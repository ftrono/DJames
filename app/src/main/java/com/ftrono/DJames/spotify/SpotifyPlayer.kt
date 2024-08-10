package com.ftrono.DJames.spotify

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.ftrono.DJames.application.ACTION_LOG_REFRESH
import com.ftrono.DJames.application.ACTION_TOASTER
import com.ftrono.DJames.application.FakeLockScreen
import com.ftrono.DJames.application.client
import com.ftrono.DJames.application.clock_active
import com.ftrono.DJames.application.last_log
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.utilities.Utilities
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.Headers
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder


class SpotifyPlayer (private val context: Context) {

    private val TAG = SpotifyPlayer::class.java.simpleName
    private var extThread: Thread? = null


    //PLAY INTERNALLY OR EXTERNALLY:
    fun spotifyPlay(playInfo: JsonObject): Int {
        var spotifyUrl = playInfo.get("spotify_URL").asString
        var playType = playInfo.get("play_type").asString
        //TRIAL 1:
        //Try requested context:
        val clockWasActive = clock_active
        var sessionState = playInternally(playInfo, useAlbum=false)
        Log.d(TAG, "(FIRST) SESSION STATE: ${sessionState}")

        if (sessionState == 0) {
            if (playInfo.get("context_type").asString == "album") {
                //If context was "album" -> terminate:
                return 0

            } else {
                //CUSTOM CONTEXT:
                //Wait 1 sec:
                Thread.sleep(1000)
                //CHECK 204:
                var playerState = getPlaybackState()
                Log.d(TAG, "PLAYBACK STATE: $playerState")

                if (playerState == 200) {
                    //200: OK -> Terminate:
                    return 0

                } else {
                    //204: WRONG CONTEXT or 400-on:
                    //TRIAL 2:
                    //Use album as context:
                    sessionState = playInternally(playInfo, useAlbum = true)
                    Log.d(TAG, "(SECOND) SESSION STATE: ${sessionState}")

                    //Update log:
                    last_log!!.addProperty("context_error", true)

                    //Send broadcast:
                    Intent().also { intent ->
                        intent.setAction(ACTION_LOG_REFRESH)
                        context.sendBroadcast(intent)
                    }

                    if (sessionState == 0) {
                        //OK -> Terminate:
                        return 0

                    } else {
                        //400-on: OPEN EXTERNALLY WITH CONTEXT = ALBUM:
                        //Open externally:
                        if (playType != "track") {
                            openExternally(spotifyUrl, clockWasActive)
                        } else {
                            var contextUri =
                                playInfo.get("album_uri").asString
                            val encodedContextUri: String =
                                URLEncoder.encode(contextUri, "UTF-8")
                            openExternally("$spotifyUrl?context=$encodedContextUri", clockWasActive)
                        }
                        return -1
                    }
                }
            }

        } else {
            //400-on: OPEN EXTERNALLY WITH CONTEXT = ALBUM:
            //Open externally:
            if (playType != "track") {
                openExternally(spotifyUrl, clockWasActive)
            } else {
                var contextUri = playInfo.get("album_uri").asString
                val encodedContextUri: String =
                    URLEncoder.encode(contextUri, "UTF-8")
                openExternally("$spotifyUrl?context=$encodedContextUri", clockWasActive)
            }
            //Update log:
            last_log!!.addProperty("play_externally", true)

            //Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_LOG_REFRESH)
                context.sendBroadcast(intent)
            }
            return -1
        }
    }


    //PLAY EXTERNALLY:
    private fun openExternally(spotifyToOpen: String, clockWasActive: Boolean) {
        //prepare thread:
        extThread = Thread {
            try {
                synchronized(this) {
                    //Open query result in Spotify:
                    val intentSpotify = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(spotifyToOpen)
                    )
                    intentSpotify.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intentSpotify.putExtra("fromwhere", "ser")
                    context.startActivity(intentSpotify)

                    if (clockWasActive && prefs.clockRedirectEnabled) {
                        //TOAST -> Send broadcast:
                        Intent().also { intent ->
                            intent.setAction(ACTION_TOASTER)
                            intent.putExtra("toastText", "Going back to Clock in ${prefs.clockTimeout} seconds...")
                            context.sendBroadcast(intent)
                        }
                        //Clock redirect:
                        Thread.sleep((prefs.clockTimeout.toLong() - 1) * 1000)   //default: 10000
                        //Launch Clock:
                        val clockIntent = Intent(context, FakeLockScreen::class.java)
                        clockIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        clockIntent.putExtra("fromwhere", "ser")
                        context.startActivity(clockIntent)
                    }
                }
            } catch (e: InterruptedException) {
                Log.d(TAG, "Interrupted: exception.", e)
            }
        }
        //start thread:
        extThread!!.start()
    }


    //PLAY INTERNALLY:
    fun playInternally(resultJSON: JsonObject, useAlbum: Boolean = false): Int {
        var ret = -1
        var utils = Utilities()
        //BUILD PUT REQUEST:
        var url = "https://api.spotify.com/v1/me/player/play"
        var playType = resultJSON.get("play_type").asString
        var offset = JsonObject()  //Context
        if (playType == "track") {
            //Start playing from the song:
            offset.addProperty("uri", resultJSON.get("uri").asString)
        } else {
            //Start playing from the beginning:
            offset.addProperty("position", 0)
        }

        var jsonBody = JsonObject()
        if (useAlbum && playType == "track") {
            //use album context:
            jsonBody.addProperty("context_uri", resultJSON.get("album_uri").asString)
        } else {
            //Use requested context:
            jsonBody.addProperty("context_uri", resultJSON.get("context_uri").asString)
        }
        jsonBody.add("offset", offset)
        jsonBody.addProperty("position_ms", 0)

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
                            var query = SpotifyQuery()
                            query.refreshAuth()

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
                    Log.d(TAG, "PlayInternally(): Response is not a JSON -> OK")
                }

                //CHECK RESPONSE:
                if (response == "") {
                    Log.d(TAG, "PLAY INTERNALLY: request sent.")
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

    
    //GET PLAYBACK STATE:
    fun getPlaybackState(): Int {
        var utils = Utilities()
        var ret = -1
        //BUILD GET REQUEST:
        var url = "https://api.spotify.com/v1/me/player"

        var headers = Headers.Builder()
            .add("Authorization", "Bearer ${prefs.spotifyToken}")
            .build()

        var request = Request.Builder()
            .url(url)
            .headers(headers)
            .build()

        try {
            runBlocking {
                var response = utils.makeRequest(client, request)
                Log.d(TAG, response.toString())
                if (response == "") {
                    //204: WRONG CONTEXT:
                    Log.d(TAG, "PLAYBACK STATE: 204")
                    ret = 204
                } else {
                    try {
                        var respJSON = JsonParser.parseString(response).asJsonObject
                        if (respJSON.has("item")) {
                            if (respJSON.get("item").toString() != "null") {
                                //200: CONTEXT OK:
                                Log.d(TAG, "PLAYBACK STATE: 200")
                                ret = 200
                            } else {
                                Log.d(TAG, "PLAYBACK STATE: Error response")
                            }
                        } else {
                            Log.d(TAG, "PLAYBACK STATE: Error response")
                        }
                    } catch (e:Exception) {
                        Log.d(TAG, "PLAYBACK STATE: Could not parse response. Error: ", e)
                    }
                }
            }
            return ret
        } catch (e: Exception) {
            Log.d(TAG, "PLAYBACK STATE ERROR: ", e)
            return -1
        }
    }

}