package com.ftrono.DJames.spotify

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.deltaSimilarity
import com.ftrono.DJames.application.ext_format
import com.ftrono.DJames.application.last_log
import com.ftrono.DJames.application.prefs
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.net.URLEncoder
import me.xdrop.fuzzywuzzy.FuzzySearch
import kotlin.math.roundToInt


class SpotifySearchArtistsPlaylists(context: Context) {
    private val TAG = SpotifySearchArtistsPlaylists::class.java.simpleName
    private var query = SpotifyQuery(context)

    //SEARCH TRACKS OR ALBUMS:
    fun searchArtistsPlaylists(searchData: JsonObject, playType: String): JsonObject {
        //vars:
        val matchName = searchData.get("text_confirmed").asString.lowercase()

        //BUILD GET REQUEST:
        var baseURL = "https://api.spotify.com/v1/search"
        val encodedMatchName: String = URLEncoder.encode(matchName, "UTF-8")

        //Tools:
        var returnJSON = JsonObject()
        var url = baseURL
        var items = JsonArray()
        var bestMatches = JsonArray()
        var bestInd = 0
        var bestScore = 0
        var bestResult = JsonObject()

        //Log:
        var logQueries = JsonArray()
        var logJSON = JsonObject()

        //Headers:
        var jsonHeads = JsonObject()
        jsonHeads.addProperty("Authorization", "Bearer ${prefs.spotifyToken}")
        Log.d(TAG, "Bearer: ${prefs.spotifyToken}")

        //FIRST REQUEST:
        //Compose query:
        url += "?q=${encodedMatchName}&type=${playType}&limit=10&market=${prefs.spotCountry}"
        Log.d(TAG, url)

        //QUERY:
        var respJSON = query.querySpotify(type = "get", url = url, jsonHeads = jsonHeads)
        var n_items = 0
        //Log:
        logJSON.addProperty("type", "searchTrackOrAlbum")
        logJSON.addProperty("url", url)
        //Check content:
        var keySet = respJSON.keySet()
        if (keySet.size == 0) {
            //Empty:
            logJSON.addProperty("n_items", n_items)
            Log.d(TAG, "ERROR: Spotify Search results not received!!")
        } else {
            //Analyse response & get index of best result:
            var temp = respJSON.getAsJsonObject("${playType}s").getAsJsonArray("items")
            var buf = JsonObject()
            for (item in temp) {
                try {
                    buf = item.asJsonObject
                    items.add(item)
                } catch (_: Exception) { }
            }

            n_items = items.size()
            logJSON.addProperty("n_items", n_items)
            //Get best score:
            if (n_items > 0) {
                bestMatches = getBestMatchingArtistPlaylist(items, matchName, playType)
                logJSON.add("spotify_matches", bestMatches)
                //Best:
                bestInd = bestMatches[0].asJsonObject.get("pos").asInt
                bestScore = bestMatches[0].asJsonObject.get("score").asInt
                bestResult = items.get(bestInd).asJsonObject
            }
        }
        //Log:
        logQueries.add(logJSON)
        last_log!!.add("spotify_queries", logQueries)
        last_log!!.addProperty("best_score", bestScore)

        //EXTRACT INFO:
        if (!bestResult.isEmpty) {
            Log.d(TAG, "BEST RESULT: INDEX $bestInd, ITEM: $bestResult")
            returnJSON.addProperty("play_type", playType)

            //ID & uri:
            val id = bestResult.get("id").asString
            returnJSON.addProperty("id", id)
            returnJSON.addProperty("uri", "spotify:$playType:$id")
            returnJSON.addProperty("spotify_URL", "${ext_format}${playType}/$id")
            var owner = ""
            if (playType == "playlist") {
                owner = bestResult.get("owner").asJsonObject.get("display_name").asString   //display_name!
            }
            returnJSON.addProperty("owner", owner)

            //Match name:
            returnJSON.add("match_name", bestResult.get("name"))

            Log.d(TAG, "Spotify $playType search results successfully processed!")
            Log.d(TAG, "returnJSON: ${returnJSON}")
        }
        return returnJSON
    }

    //Spotify: get Best Result:
    fun getBestMatchingArtistPlaylist(items: JsonArray, matchName: String, playType: String): JsonArray {
        //Analyse Spotify query result:
        //GET BEST RESULT:
        var c = 0
        var matchesArray = ArrayList<JsonObject>()
        var ids = ArrayList<String>()
        var scoresMap = mutableMapOf<Int, Int>()
        for (item in items) {
            var scoreJson = JsonObject()
            scoreJson.addProperty("pos", c)
            var currItem = item.asJsonObject
            //Key info:
            val re = Regex("[^A-Za-z0-9 ]")
            var name = re.replace(currItem.get("name").asString, "")
            scoreJson.addProperty("name", name)
            ids.add(currItem.get("id").asString)
            //Artists name:
            //calculate similarity:
            var temp_name = scoreJson.get("name").asString.lowercase()
            var nameSet = FuzzySearch.tokenSetRatio(temp_name, matchName)
            var namePartial = FuzzySearch.partialRatio(temp_name, matchName)
            var nameFull = FuzzySearch.ratio(temp_name, matchName)
            scoreJson.addProperty("nameSetSimilarity", nameSet)
            scoreJson.addProperty("namePartialSimilarity", namePartial)
            scoreJson.addProperty("nameFullSimilarity", nameFull)
            var score = listOf<Int>(nameSet, namePartial, nameFull).average().roundToInt()

            //Store:
            scoreJson.addProperty("score", score)
            scoresMap[c] = score
            matchesArray.add(scoreJson)
            Log.d(TAG, scoreJson.toString())
            c += 1
        }

        //Sort map:
        Log.d(TAG, "MAP: $scoresMap")
        val sortedScores = scoresMap.toList().sortedByDescending { it.second }.toMap()
        Log.d(TAG, "SORTED MAP: $sortedScores")
        //Exclude lower items:
        var scoreThreshold = sortedScores.values.elementAt(0) - deltaSimilarity
        if (scoreThreshold < 0) {
            scoreThreshold = 0
        }
        var bestScores = sortedScores.filter { (key, value) -> value >= scoreThreshold}
        Log.d(TAG, "FILTERED MAP: $bestScores")
        //Default best Ind is the first (max score):
        var bestInd = sortedScores.keys.elementAt(0)

        //RETURN PREPARATION:
        var bestMatches = JsonArray()
        //position 0 -> BEST:
        bestMatches.add(matchesArray[bestInd])
        //the other results:
        for (k in bestScores.keys) {
            if (k != bestInd) {
                bestMatches.add(matchesArray[k])
            }
        }
        return bestMatches
    }

}
