package com.ftrono.DJames.be.spotify

import android.content.Context
import android.net.Uri
import android.util.Log
import com.ftrono.DJames.application.deltaSimilarity
import com.ftrono.DJames.application.minThreshold
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.spotifyParsers
import com.ftrono.DJames.application.spotifyQueryLimit
import com.ftrono.DJames.be.database.ScoreSet
import com.ftrono.DJames.be.database.SpotifyMatchModel
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
        val url = baseURL + "?q=${encodedQuery}&type=${playType}&limit=$spotifyQueryLimit&market=${prefs.spotCountry}"

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
    ): MutableList<SpotifyMatchModel> {
        Log.d(TAG, "Scoring ${items.size()} Spotify results for query: type '$playType', matchName: '$matchName', detailName: '$detailName'...")
        val allMatches = mutableListOf<SpotifyMatchModel>()
        val idsToCheck = mutableListOf<String>()
        for (item in items) {
            try {
                val curMatch = SpotifyMatchModel()
                val curJson = item.asJsonObject

                // Extract key info:
                curMatch.playable = spotifyParsers.extractPlayableFromJson(playType, curJson)
                if (playType == "track" && curMatch.playable.track!!.album!!.type == "album") {
                    curMatch.isAlbum = true
                }
                var stringToMatch = curJson.get("name").asString
                var detailToMatch = when (playType) {
                    "track" -> {
                        curMatch.playable.track!!.artists.joinToString(", ", "", "") { it.name }
                    }

                    "album" -> {
                        curMatch.playable.album!!.artists.joinToString(", ", "", "") { it.name }
                    }

                    "episode" -> {
                        curMatch.playable.episode!!.podcast!!.name
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
                    var scoreSet = ScoreSet()
                    scoreSet.nameSetSimilarity = FuzzySearch.tokenSetRatio(stringToMatch, matchName)
                    scoreSet.namePartialSimilarity = FuzzySearch.partialRatio(stringToMatch, matchName)
                    scoreSet.nameFullSimilarity = FuzzySearch.ratio(stringToMatch, matchName)
                    curMatch.scoreSet = scoreSet
                    curMatch.score = listOf<Int>(
                        scoreSet.nameSetSimilarity,
                        scoreSet.namePartialSimilarity,
                        scoreSet.nameFullSimilarity
                    ).average().roundToInt()

                } else {
                    // B) Match name and detail separately:
                    // Clean string (remove regex):
                    stringToMatch = re.replace(stringToMatch.lowercase(), "")
                    detailToMatch = re.replace(detailToMatch.lowercase(), "")
                    var scoreSet = ScoreSet()
                    scoreSet.nameSetSimilarity = FuzzySearch.tokenSetRatio(stringToMatch, matchName)
                    scoreSet.namePartialSimilarity = FuzzySearch.partialRatio(stringToMatch, matchName)
                    scoreSet.nameFullSimilarity = FuzzySearch.ratio(stringToMatch, matchName)
                    scoreSet.detailSetSimilarity = FuzzySearch.tokenSetRatio(detailToMatch, detailName)
                    scoreSet.detailPartialSimilarity = FuzzySearch.partialRatio(detailToMatch, detailName)
                    curMatch.scoreSet = scoreSet
                    curMatch.score = listOf<Int>(
                        scoreSet.nameSetSimilarity,
                        scoreSet.namePartialSimilarity,
                        scoreSet.nameFullSimilarity,
                        scoreSet.detailSetSimilarity,
                        scoreSet.detailPartialSimilarity
                    ).average().roundToInt()
                }

                // Filter & store:
                if (curMatch.score >= minThreshold) {
                    allMatches.add(curMatch)
                    idsToCheck.add(curMatch.playable.id)
                }

            } catch (e: Exception) {
                Log.w(TAG, "Error: Spotify match item skipped. ", e)
            }
        }

        //Check saved:
        if (playType == "track" && idsToCheck.isNotEmpty()) {
            val savedIds = checkSaved(idsToCheck)
            Log.d(TAG, "Check saved: $savedIds")
            // Add isSaved information:
            savedIds.takeIf { it.isNotEmpty() }?.forEachIndexed { i, isSaved ->
                allMatches[i].isSaved = isSaved
            }
        }

        //Sort matches by score:
        return allMatches.sortedByDescending { it.score }.toMutableList()
    }


    // Get saved tracks info:
    fun checkSaved(ids: MutableList<String>): List<Boolean> {
        var savedList = listOf<Boolean>()
        var url = "https://api.spotify.com/v1/me/tracks/contains?ids="

        //Headers:
        val jsonHeads = JsonObject()
        jsonHeads.addProperty("Authorization", "Bearer ${prefs.spotifyToken}")

        //Query params:
        url += ids.joinToString(",", "", "")
        Log.d(TAG, url)

        val response = query.querySpotify(type = "get", url = url, jsonHeads = jsonHeads)
        if (response.body != "") {
            savedList = JsonParser.parseString(response.body).asJsonArray.map { it.asBoolean }
        }
        return savedList
    }


    //Spotify: (legacy) rescore results:
    fun legacyRescoreResults(
        playType: String,
        origMatches: MutableList<SpotifyMatchModel>
    ): MutableList<SpotifyMatchModel> {
        //Exclude lower score items:
        var scoreThreshold = origMatches.elementAt(0).score - deltaSimilarity
        scoreThreshold = if (scoreThreshold < 0) 0 else scoreThreshold

        // Key is original allMatches index, Value is score:
        var sortedMatches = origMatches.filter { it.score >= scoreThreshold}.toMutableList()

        // Sort full score items: priority+similarity if track, else similarity:
        if (playType == "track") {
            // Calculate priority-aware combined scores:
            val scoredMatches = sortedMatches.map {
                // Priority rules (0–3):
                val priority = when {
                    it.isSaved && it.isAlbum -> 3
                    it.isSaved -> 2
                    it.isAlbum -> 1
                    else -> 0
                }
                // Balance similarity and priority:
                val combinedScore = it.score + priority * 100  // tweakable weights
                Pair(it, combinedScore)
            }
            // Sort by combined score, highest first:
            sortedMatches = scoredMatches.sortedByDescending { it.second }.map { it.first }.toMutableList()
        }
        return sortedMatches
    }

}
