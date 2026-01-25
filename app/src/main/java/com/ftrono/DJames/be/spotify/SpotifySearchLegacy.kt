package com.ftrono.DJames.be.spotify

import android.content.Context
import android.net.Uri
import android.util.Log
import com.ftrono.DJames.application.deltaSimilarity
import com.ftrono.DJames.application.searchThreshold
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.spotifyParsers
import com.ftrono.DJames.application.spotifyQueryLimit
import com.ftrono.DJames.be.database.ExtractorInfo
import com.ftrono.DJames.be.database.SpotifyMatchModel
import com.ftrono.DJames.be.database.SpotifyPlayable
import com.ftrono.DJames.be.database.SpotifyQueryModel
import com.ftrono.DJames.be.database.SpotifyResults
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import me.xdrop.fuzzywuzzy.FuzzySearch
import kotlin.math.roundToInt


class SpotifySearchLegacy(private val context: Context) {
    private val TAG = this::class.java.simpleName
    private var query = SpotifyQuery(context)

    //SEARCH PLAYABLE:
    fun searchPlayable(searchData: ExtractorInfo): SpotifyResults {
        //Get context:
        var spotResults = SpotifyResults()
        var playType = searchData.playType
        var matchName = searchData.matchExtracted.lowercase()
        var artistName = searchData.artistConfirmed.lowercase()

        //BUILD GET REQUEST:
        var baseURL = "https://api.spotify.com/v1/search"
        val encodedMatchName: String = Uri.encode(matchName)

        //Check if LIVE to be preferred or not:
        var live = false
        for (tok in matchName.split(" ")) {
            if (tok.lowercase() == "live") {
                live = true
                break
            }
        }

        //Tools:
        var url = baseURL
        var items = JsonArray()
        var items2 = JsonArray()
        var spotifyQueries = mutableListOf<SpotifyQueryModel>()
        var bestMatches = mutableListOf<SpotifyMatchModel>()
        var bestScore = 0
        var bestResult = SpotifyPlayable()

        //Extract query params:
        var qParams = mutableListOf<String>()
        if (artistName != "") {
            qParams.add("${playType}:${matchName}")
            qParams.add("artist:${artistName}")
        }

        //Headers:
        var jsonHeads = JsonObject()
        jsonHeads.addProperty("Authorization", "Bearer ${prefs.spotifyToken}")

        //FIRST REQUEST:
        //Compose query:
        var url1 = url
        if (qParams.isNotEmpty()) {
            var queryParams = qParams.joinToString("&", prefix = "&")
            val encodedParams: String =
                Uri.encode(queryParams).replace("%26", "%20").replace("%3A", ":")
            url1 += "?q=${encodedMatchName}${encodedParams}&type=${playType}&limit=$spotifyQueryLimit&market=${prefs.spotCountry}"
        } else {
            url1 += "?q=${encodedMatchName}&type=${playType}&limit=$spotifyQueryLimit&market=${prefs.spotCountry}"
        }
        Log.d(TAG, url1)

        //First query (uses Params):
        var response = query.querySpotify(type = "get", url = url1, jsonHeads = jsonHeads)
        if (response.body != "") {
            var respJSON = JsonParser.parseString(response.body).asJsonObject
            var n_items = 0
            //Log:
            var curQuery = SpotifyQueryModel()
            curQuery.type = playType
            curQuery.url = url1
            //Check content:
            var keySet = respJSON.keySet()
            if (keySet.size == 0) {
                //Empty:
                curQuery.numItems = n_items
                Log.d(TAG, "ERROR: Spotify Search results not received!!")
            } else {
                //Analyse response & get index of best result:
                items = respJSON.getAsJsonObject("${playType}s").getAsJsonArray("items")
                n_items = items.size()
                curQuery.numItems = n_items
                //Get best score:
                if (n_items > 0) {
                    bestMatches = getBestMatches(items, playType, matchName, artistName, live)
                    curQuery.spotifyMatches = bestMatches
                    //Best:
                    bestScore = bestMatches[0].score
                    bestResult = bestMatches[0].playable
                }
            }
            //Log:
            spotifyQueries.add(curQuery)
        }

        //SECOND REQUEST:
        var url2 = url
        if (bestScore <= searchThreshold || items.isEmpty()) {
            //Compose query:
            if (artistName != "") {
                val encodedArtistName: String = Uri.encode(artistName)
                url2 += "?q=${encodedMatchName}+by+${encodedArtistName}&type=${playType}&limit=$spotifyQueryLimit&market=${prefs.spotCountry}"
            } else {
                url2 += "?q=${encodedMatchName}&type=${playType}&limit=$spotifyQueryLimit&market=${prefs.spotCountry}"
            }

            //Second query:
            if (url2 != url1) {
                Log.d(TAG, url2)
                response = query.querySpotify(type = "get", url = url2, jsonHeads = jsonHeads)
                if (response.body != "") {
                    var respJSON = JsonParser.parseString(response.body).asJsonObject
                    var n_items = 0
                    //Log:
                    var curQuery = SpotifyQueryModel()
                    curQuery.type = "searchTrackOrAlbum"
                    curQuery.url = url2
                    //Check content:
                    var keySet = respJSON.keySet()
                    if (keySet.size == 0) {
                        //Empty:
                        curQuery.numItems = n_items
                        Log.d(TAG, "ERROR: Spotify Search results not received!!")
                    } else {
                        //Analyse response & get index of best result:
                        items2 = respJSON.getAsJsonObject("${playType}s").getAsJsonArray("items")
                        n_items = items2.size()
                        curQuery.numItems = n_items
                        //Get best score:
                        if (n_items > 0) {
                            bestMatches = getBestMatches(items2, playType, matchName, artistName, live)
                            curQuery.spotifyMatches = bestMatches
                            //Best:
                            var bestScore2 = bestMatches[0].score
                            if (bestScore2 > bestScore) {
                                //Overwrite global best:
                                bestScore = bestScore2
                                bestResult = bestMatches[0].playable
                            }
                        }
                    }
                    //Log:
                    spotifyQueries.add(curQuery)
                }
            }
        }
        spotResults.bestResult = bestResult
        spotResults.spotifyQueries = spotifyQueries
        spotResults.matchScore = bestScore
        Log.d(TAG, "BEST RESULT: $bestResult")
        return spotResults
    }


    //GENERIC SPOTIFY SEARCH -> PLAY WITHIN ALBUM:
    fun checkSaved(ids: MutableList<String>): List<Boolean> {
        var savedList = listOf<Boolean>()
        //BUILD GET REQUEST:
        var url = "https://api.spotify.com/v1/me/tracks/contains?ids="

        //Headers:
        var jsonHeads = JsonObject()
        jsonHeads.addProperty("Authorization", "Bearer ${prefs.spotifyToken}")

        //Query params:
        url += ids.joinToString(",", "", "")
        Log.d(TAG, url)

        var response = query.querySpotify(type = "get", url = url, jsonHeads = jsonHeads)
        if (response.body != "") {
            savedList = JsonParser.parseString(response.body).asJsonArray.map { it.asBoolean }
        }
        return savedList
    }

    //Spotify: get Best Result:
    fun getBestMatches(items: JsonArray, playType: String, matchName: String, detailName: String, live: Boolean): MutableList<SpotifyMatchModel> {
        var c = 0
        var allMatches = mutableListOf<SpotifyMatchModel>()
        var ids = mutableListOf<String>()
        var scoresMap = mutableMapOf<Int, Int>()
        for (item in items) {
            try {
                var curMatch = SpotifyMatchModel()
                curMatch.pos = c
                var curJson = item.asJsonObject

                // Extract key info:
                curMatch.playable = spotifyParsers.extractPlayableFromJson(playType, curJson)
                ids.add(curMatch.playable.id)
                if (playType == "track" && curMatch.playable.track!!.album!!.type == "album") {
                    curMatch.isAlbum = true
                }
                var stringToMatch = curJson.get("name").asString
                var detailToMatch = when (playType) {
                    "track" -> {
                        curMatch.playable.track!!.artists.joinToString(", ", "", "") { it.name }
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
                var score = 0
                if (detailName == "") {
                    // A) Match name & detail together:
                    // Clean string (remove regex):
                    stringToMatch = re.replace(stringToMatch, "") +
                            " " + re.replace(detailToMatch, "")
                    stringToMatch = stringToMatch.lowercase()
                    curMatch.nameSetSimilarity = FuzzySearch.tokenSetRatio(stringToMatch, matchName)
                    curMatch.namePartialSimilarity =
                        FuzzySearch.partialRatio(stringToMatch, matchName)
                    curMatch.nameFullSimilarity = FuzzySearch.ratio(stringToMatch, matchName)
                    score = listOf<Int>(
                        curMatch.nameSetSimilarity,
                        curMatch.namePartialSimilarity,
                        curMatch.nameFullSimilarity
                    ).average().roundToInt()

                } else {
                    // B) Match name and detail separately:
                    // Clean string (remove regex):
                    stringToMatch = re.replace(stringToMatch.lowercase(), "")
                    detailToMatch = re.replace(detailToMatch.lowercase(), "")
                    curMatch.nameSetSimilarity = FuzzySearch.tokenSetRatio(stringToMatch, matchName)
                    curMatch.namePartialSimilarity = FuzzySearch.partialRatio(stringToMatch, matchName)
                    curMatch.nameFullSimilarity = FuzzySearch.ratio(stringToMatch, matchName)
                    curMatch.detailSetSimilarity =
                        FuzzySearch.tokenSetRatio(detailToMatch, detailName)
                    curMatch.detailPartialSimilarity =
                        FuzzySearch.partialRatio(detailToMatch, detailName)
                    score = listOf<Int>(
                        curMatch.nameSetSimilarity,
                        curMatch.namePartialSimilarity,
                        curMatch.nameFullSimilarity,
                        curMatch.detailSetSimilarity,
                        curMatch.detailPartialSimilarity
                    ).average().roundToInt()
                }

                //Check if live:
                if (playType == "track" || playType == "album") {
                    for (tok in stringToMatch.split(" ")) {
                        if (!live && tok.lowercase() == "live") {
                            score -= deltaSimilarity
                            break
                        }
                    }
                }

                // Store score:
                curMatch.score = score
                scoresMap[c] = score
                allMatches.add(curMatch)
                // Log.d(TAG, curMatch.toString())

            } catch (e: Exception) {
                Log.w(TAG, "Error: Spotify match item skipped. ", e)
            }
            c += 1
        }

        //Check saved:
        if (playType == "track") {
            var savedIds = checkSaved(ids)
            Log.d(TAG, "Check saved: $savedIds")
            // Add isSaved information:
            savedIds.takeIf { it.isNotEmpty() }?.forEachIndexed { i, isSaved ->
                allMatches[i].isSaved = isSaved
                allMatches[i].playable.track!!.saved = isSaved
            }
        }

        //Sort map:
        Log.d(TAG, "MAP: $scoresMap")
        var sortedMatches = allMatches.sortedByDescending { it.score }

        //Exclude lower score items:
        var scoreThreshold = sortedMatches.elementAt(0).score - deltaSimilarity
        scoreThreshold = if (scoreThreshold < 0) 0 else scoreThreshold

        // Key is original allMatches index, Value is score:
        var bestMatches = sortedMatches.filter { it.score >= scoreThreshold}.toMutableList()

        // Sort full score items: priority+similarity if track, else similarity:
        if (playType == "track") {
            // Calculate priority-aware combined scores:
            val scoredMatches = bestMatches.map {
                // Priority rules (0–3):
                val priority = when {
                    it.playable.track!!.saved && it.playable.track!!.album!!.type == "album" -> 3
                    it.playable.track!!.saved -> 2
                    it.playable.track!!.album!!.type == "album" -> 1
                    else -> 0
                }
                // Balance similarity and priority:
                val combinedScore = it.score + priority * 100  // tweak weight here
                Pair(it, combinedScore)
            }
            // Sort by combined score, highest first:
            bestMatches = scoredMatches.sortedByDescending { it.second }.map { it.first }.toMutableList()
        }
        return bestMatches
    }

}
