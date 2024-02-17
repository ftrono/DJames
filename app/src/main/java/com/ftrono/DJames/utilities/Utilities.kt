package com.ftrono.DJames.utilities

import android.util.Log
import com.ftrono.DJames.application.deltaSimilarity
import com.google.gson.JsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.Random
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.streams.asSequence
import me.xdrop.fuzzywuzzy.FuzzySearch


class Utilities {
    private val TAG = Utilities::class.java.simpleName

    //OkHTTP: make HTTP request:
    suspend fun makeRequest(client: OkHttpClient, request: Request): String = suspendCoroutine { continuation ->
        client.newCall(request).enqueue(object: Callback {
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response.body!!.string())
            }

            override fun onFailure(call: Call, e: IOException) {
                continuation.resume("")
                Log.d(TAG, "RESPONSE ERROR: ", e)
            }
        })
    }


    //ID creator:
    fun generateRandomString(length: Int, numOnly: Boolean = false): String {
        var source = ""
        if (numOnly) {
            source = "0123456789"
        } else {
            source = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        }

        return Random().ints(length.toLong(), 0, source.length)
            .asSequence()
            .map(source::get)
            .joinToString("")
    }

    //Spotify: get Best Result:
    fun getBestResult(respJSON: JsonObject, matchName: String, artistName: String): JsonObject {
        var bestResult = JsonObject()
        //Analyse Spotify query result:
        var tracks = respJSON.getAsJsonObject("tracks")
        var items = tracks.getAsJsonArray("items")

        //GET BEST RESULT:
        var c = 0
        var matchesArray = ArrayList<JsonObject>()
        var scoresMap = mutableMapOf<Int, Int>()
        for (item in items) {
            var scoreJson = JsonObject()
            var currItem = item.asJsonObject
            //Key info:
            scoreJson.addProperty("name", currItem.get("name").asString)
            scoreJson.addProperty("albumType", currItem.get("album").asJsonObject.get("album_type").asString)
            //Artists name:
            var foundArtists = ArrayList<String>()
            var artists = currItem.getAsJsonArray("artists")
            for (artist in artists) {
                foundArtists.add(artist.asJsonObject.get("name").asString)
            }
            scoreJson.addProperty("artists", foundArtists.joinToString(", ", "", ""))
            //calculate similarity:
            scoreJson.addProperty("nameSimilarity", FuzzySearch.tokenSetRatio(scoreJson.get("name").asString, matchName))
            scoreJson.addProperty("artistSimilarity", FuzzySearch.tokenSetRatio(scoreJson.get("artists").asString, artistName))
            var score = scoreJson.get("nameSimilarity").asInt + scoreJson.get("artistSimilarity").asInt
            scoreJson.addProperty("score", score)
            scoresMap[c] = score
            matchesArray.add(scoreJson)
            Log.d(TAG, scoreJson.toString())
            c += 1
        }
        Log.d(TAG, "MAP: $scoresMap")
        //Sort map:
        val sortedScores = scoresMap.toList().sortedByDescending { it.second }.toMap()
        Log.d(TAG, "SORTED MAP: $sortedScores")
        var bestK = sortedScores.keys.elementAt(0)
        //Exclude lower items:
        var scoreThreshold = sortedScores.values.elementAt(0) - deltaSimilarity
        if (scoreThreshold < 0) {
            scoreThreshold = 0
        }
        var bestScores = sortedScores.filter { (key, value) -> value >= scoreThreshold}
        Log.d(TAG, "FILTERED MAP: $bestScores")
        //Get album (if present):
        for (k in bestScores.keys) {
            var result = matchesArray[k]
            if (result.get("albumType").asString == "album") {
                bestResult = items.get(k).asJsonObject
                Log.d(TAG, "BEST RESULT: INDEX $k, ITEM: $bestResult")
                break
            }
        }
        //else: return bestK:
        if (bestResult.isEmpty()) {
            bestResult = items.get(bestK).asJsonObject
            Log.d(TAG, "BEST RESULT: INDEX $bestK, ITEM: $bestResult")
        }
        return bestResult
    }

}