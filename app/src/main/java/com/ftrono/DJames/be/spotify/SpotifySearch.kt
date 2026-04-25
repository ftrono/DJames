package com.ftrono.DJames.be.spotify

import android.content.Context
import android.net.Uri
import android.util.Log
import com.ftrono.DJames.application.deltaSimilarity
import com.ftrono.DJames.application.minThreshold
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.spotifyParsers
import com.ftrono.DJames.application.spotifyQueryLimit
import com.ftrono.DJames.be.database.SpotifyPlayable
import com.ftrono.DJames.be.database.SpotifyQueryModel
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import me.xdrop.fuzzywuzzy.FuzzySearch
import kotlin.math.roundToInt


class SpotifySearch(private val context: Context) {
    private val TAG = this::class.java.simpleName
    private var query = SpotifyQuery(context)

    //SEARCH PLAYABLE:
    fun searchPlayable(playType: String, matchName: String, detailName: String = ""): SpotifyQueryModel {
        // Build query:
        // Log.d(TAG, "Querying SpotifySearch with: type '$playType', matchName: '$matchName', detailName: '$detailName'...")
        val baseURL = "https://api.spotify.com/v1/search"
        val encodedQuery = Uri.encode(if (detailName != "") "$matchName $detailName" else matchName)
        val url = baseURL + "?q=${encodedQuery}&type=${if (playType == "podcast") "show" else playType}&limit=$spotifyQueryLimit"

        // Headers:
        val jsonHeads = JsonObject()
        jsonHeads.addProperty("Authorization", "Bearer ${prefs.spotifyToken}")

        // Query:
        val curQuery = SpotifyQueryModel()
        val response = query.querySpotify(type = "get", url = url, jsonHeads = jsonHeads)
        if (response.body != "") {
            val respJSON = JsonParser.parseString(response.body).asJsonObject
            curQuery.type = playType
            curQuery.url = url
            // Check content:
            if (!respJSON.keySet().contains("${playType}s")) {
                // Empty:
                Log.d(TAG, "ERROR: Spotify Search results not received!!")
            } else {
                // Analyse response & get index of best result:
                val items = respJSON.getAsJsonObject("${playType}s").getAsJsonArray("items")
                curQuery.numItems = items.size()
                // Score & filter results:
                if (!items.isEmpty) {
                    curQuery.spotifyMatches = scoreAndFilter(items, playType, matchName.lowercase(), detailName.lowercase())
                }
            }
        }
        return curQuery
    }


    //Spotify: score & filter results:
    fun scoreAndFilter(
        items: JsonArray,
        playType: String,
        matchName: String,
        detailName: String = ""
    ): MutableList<SpotifyPlayable> {
        Log.d(TAG, "Scoring ${items.size()} Spotify results for query: type '$playType', matchName: '$matchName', detailName: '$detailName'...")
        val allMatches = mutableListOf<SpotifyPlayable>()
        val idsToCheck = mutableListOf<String>()
        for (item in items) {
            try {
                var curMatch = SpotifyPlayable()
                val curJson = item.asJsonObject

                // Extract key info:
                curMatch = spotifyParsers.extractPlayableFromJson(playType, curJson)
                var stringToMatch = curJson.get("name").asString
                var detailToMatch = when (playType) {
                    "track" -> {
                        curMatch.track!!.artists.joinToString(", ", "", "") { it.name }
                    }

                    "album" -> {
                        curMatch.album!!.artists.joinToString(", ", "", "") { it.name }
                    }

                    "episode" -> {
                        curMatch.episode!!.podcast!!.name
                    }

                    else -> ""
                }

                // Clean strings regex:
                // \p{L} = any Unicode letter (so you keep accents like é, ñ, ü)
                // \p{N} = any Unicode digit
                val re = Regex("[^\\p{L}\\p{N} ]")

                // Calculate similarity:
                if (detailName == "") {
                    // A) Match name & detail together:
                    // Clean string (remove regex):
                    stringToMatch = re.replace(stringToMatch, "") +
                            " " + re.replace(detailToMatch, "")
                    stringToMatch = stringToMatch.lowercase()
                    val nameSetSimilarity = FuzzySearch.tokenSetRatio(stringToMatch, matchName)
                    val namePartialSimilarity = FuzzySearch.partialRatio(stringToMatch, matchName)
                    val nameFullSimilarity = FuzzySearch.ratio(stringToMatch, matchName)
                    curMatch.matchScore = listOf<Int>(
                        nameSetSimilarity,
                        namePartialSimilarity,
                        nameFullSimilarity
                    ).average().roundToInt()

                } else {
                    // B) Match name and detail separately:
                    // Clean string (remove regex):
                    stringToMatch = re.replace(stringToMatch.lowercase(), "")
                    detailToMatch = re.replace(detailToMatch.lowercase(), "")
                    val nameSetSimilarity = FuzzySearch.tokenSetRatio(stringToMatch, matchName)
                    val namePartialSimilarity = FuzzySearch.partialRatio(stringToMatch, matchName)
                    val nameFullSimilarity = FuzzySearch.ratio(stringToMatch, matchName)
                    val detailSetSimilarity = FuzzySearch.tokenSetRatio(detailToMatch, detailName)
                    val detailPartialSimilarity = FuzzySearch.partialRatio(detailToMatch, detailName)
                    curMatch.matchScore = listOf<Int>(
                        nameSetSimilarity,
                        namePartialSimilarity,
                        nameFullSimilarity,
                        detailSetSimilarity,
                        detailPartialSimilarity
                    ).average().roundToInt()
                }

                // Filter & store:
                if (curMatch.matchScore >= minThreshold) {
                    allMatches.add(curMatch)
                    idsToCheck.add(curMatch.id)
                }

            } catch (e: Exception) {
                Log.w(TAG, "Error: Spotify match item skipped. ", e)
            }
        }

        //Check saved:
        if (idsToCheck.isNotEmpty()) {
            val spotifyCalls = SpotifyCalls(context)
            val savedIds = spotifyCalls.checkLibraryRequest(idsToCheck, playType)
            Log.d(TAG, "Check saved: $savedIds")
            // Add isSaved information:
            savedIds.takeIf { it.isNotEmpty() }?.forEachIndexed { i, isSaved ->
                allMatches[i].saved = isSaved
            }
        }

        //Sort matches by score:
        return allMatches.sortedByDescending { it.matchScore }.toMutableList()
    }


    //Spotify: rescore results:
    fun rescoreResults(
        playType: String,
        origMatches: MutableList<SpotifyPlayable>
    ): MutableList<SpotifyPlayable> {
        //Exclude lower score items:
        var scoreThreshold = origMatches.elementAt(0).matchScore - deltaSimilarity
        scoreThreshold = if (scoreThreshold < 0) 0 else scoreThreshold

        // Key is original allMatches index, Value is score:
        var sortedMatches = origMatches.filter { it.matchScore >= scoreThreshold}.toMutableList()

        // Sort full score items: priority+similarity:
        val scoredMatches = sortedMatches.map {
            // Priority rules (0–3):
            val priority = when (playType) {
                "track" -> {
                    when {
                        it.saved && it.track!!.album!!.type == "album" -> 3
                        it.saved -> 2
                        it.track!!.album!!.type == "album" -> 1
                        else -> 0
                    }
                }
                "album" -> {
                    when {
                        it.saved && it.album!!.type == "album" -> 3
                        it.saved -> 2
                        it.album!!.type == "album" -> 1
                        else -> 0
                    }
                }
                else -> {
                    when {
                        it.saved -> 1
                        else -> 0
                    }
                }
            }
            // Balance similarity and priority:
            val combinedScore = it.matchScore + priority * 100  // tweakable weights
            Pair(it, combinedScore)
        }
        // Sort by combined score, highest first:
        sortedMatches = scoredMatches.sortedByDescending { it.second }.map { it.first }.toMutableList()
        // Deduplicate:
        if (playType == "track") {
            sortedMatches = sortedMatches.distinctBy { it.track!!.name to it.track!!.artists.joinToString { art -> art.name } }.toMutableList()
        } else if (playType == "album") {
            sortedMatches = sortedMatches.distinctBy { it.album!!.name to it.album!!.artists.joinToString { art -> art.name } }.toMutableList()
        }
        return sortedMatches
    }

}
