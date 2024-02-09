package com.ftrono.DJames.api

import com.google.gson.JsonObject
import android.util.Log
import com.ftrono.DJames.application.client
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.utils
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.runBlocking
import okhttp3.Headers
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody


class SpotifyInterpreter {
    private val TAG = SpotifyInterpreter::class.java.simpleName

    fun dispatchCall(resultsNLP: Array<String>): JsonObject {
    //DISPATCH SPOTIFY CALLS ACCORDING TO NLP RESULTS:
        var search = SpotifySearch()
        var returnJSON = search.genericSearch(resultsNLP)
        return returnJSON
    }

    fun playInternally(resultJSON: JsonObject): Int {
        var ret = -1
        //BUILD PUT REQUEST:
        var url = "https://api.spotify.com/v1/me/player/play"
        var uris = JsonArray()
        uris.add(JsonPrimitive(resultJSON.get("uri").asString))

        //Offset:
        var offset = JsonObject()
        offset.add("position", resultJSON.get("track_number"))

        var jsonBody = JsonObject()
        //jsonBody.add("uris", uris)
        jsonBody.add("context_uri", resultJSON.get("context_uri"))
        jsonBody.add("offset", offset)
        jsonBody.add("position_ms", JsonPrimitive(0))

        var body = jsonBody.toString().toRequestBody()

        var headers = Headers.Builder()
            .add("Authorization", "Bearer ${prefs.spotifyToken}")
            .add("Content-Type", "application/json")
            .build()

        var request = Request.Builder()
            .url(url)
            .put(body)
            .headers(headers)
            .build()

        try {
            runBlocking {
                var response = utils.makeRequest(client, request)
                if (response == "") {
                    Log.d(TAG, "PLAY INTERNALLY SUCCESS!")
                    ret = 0
                } else {
                    Log.d(TAG, "COULD NOT PLAY INTERNALLY.")
                    try {
                        var respJSON = JsonParser.parseString(response).asJsonObject
                        if (respJSON.has("error")) {
                            Log.d(TAG, "PLAY INTERNALLY: Error response: ${response}")
                        }
                    } catch (e:Exception) {
                        Log.d(TAG, "PLAY INTERNALLY: Could not parse response. Error: ", e)
                    }
                    ret = -1
                }
            }
            return ret
        } catch (e: Exception) {
            Log.d(TAG, "PLAY INTERNALLY ERROR: ", e)
            return -1
        }
    }

}