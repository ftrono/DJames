package com.ftrono.DJames.spotify

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.client
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.utils
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.Headers
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody


class SpotifyVarious(private val context: Context) {
    private val TAG = SpotifyPlayer::class.java.simpleName

    //SAVE CURRENT TRACK:
    fun saveTrackRequest(ids: JsonArray): Int {
        var ret = -1
        //BUILD PUT REQUEST:
        var url = "https://api.spotify.com/v1/me/tracks"

        var jsonBody = JsonObject()
        jsonBody.add("ids", ids)
        var body = jsonBody.toString().toRequestBody()

        //FIRST PUT REQUEST:
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
                //Log.d(TAG, response)
                try {
                    //Check if error 401:
                    var respJSON = JsonParser.parseString(response).asJsonObject
                    if (respJSON.has("error")) {
                        var errorJSON = respJSON.get("error").asJsonObject
                        var status = errorJSON.get("status").asString.toInt()
                        Log.d(TAG, "First Search answer: received error ${status}.")

                        //401 -> token expired!
                        if (status == 401) {
                            //Calling Refresh:
                            Log.d(TAG, "Refreshing token...")
                            var query = SpotifyQuery(context)
                            query.refreshAuth(context)

                            //SECOND PUT REQUEST:
                            headers = Headers.Builder()
                                .add("Authorization", "Bearer ${prefs.spotifyToken}")
                                .add("Content-Type", "application/json")
                                .build()

                            request = Request.Builder()
                                .url(url)
                                .put(body)
                                .headers(headers)
                                .build()

                            response = utils.makeRequest(client, request)
                            //Log.d(TAG, response)
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "saveTrackRequest(): Response is not a JSON -> OK")
                }

                //CHECK RESPONSE:
                if (response == "") {
                    Log.d(TAG, "saveTrackRequest: request sent.")
                    ret = 0
                } else {
                    Log.d(TAG, "COULD NOT SAVE TRACK.")
                    try {
                        var respJSON = JsonParser.parseString(response).asJsonObject
                        if (respJSON.has("error")) {
                            Log.d(TAG, "saveTrackRequest: Error response: ${response}")
                        }
                    } catch (e:Exception) {
                        Log.w(TAG, "saveTrackRequest: Could not parse response. Error: ", e)
                    }
                    ret = -1
                }
            }
            return ret
        } catch (e: Exception) {
            Log.w(TAG, "saveTrackRequest ERROR: ", e)
            return -1
        }
    }

}