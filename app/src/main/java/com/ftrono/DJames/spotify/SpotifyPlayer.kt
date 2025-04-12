package com.ftrono.DJames.spotify

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.ftrono.DJames.application.ACTION_LOG_REFRESH
import com.ftrono.DJames.application.ACTION_TOASTER
import com.ftrono.DJames.application.ClockActivity
import com.ftrono.DJames.application.clockActive
import com.ftrono.DJames.application.last_log
import com.ftrono.DJames.application.prefs
import com.google.gson.JsonObject
import java.net.URLEncoder


class SpotifyPlayer (private val context: Context) {

    private val TAG = SpotifyPlayer::class.java.simpleName
    private var query = SpotifyQuery(context)
    private var extThread: Thread? = null


    //PLAY INTERNALLY OR EXTERNALLY:
    fun spotifyPlay(playInfo: JsonObject): Int {
        val spotifyUrl = playInfo.get("spotify_URL").asString
        val playType = playInfo.get("play_type").asString
        //TRIAL 1:
        //Try requested context:
        val clockWasActive = clockActive.value!!
        var sessionState = playInternally(playInfo, useAlbum=false)
        Log.d(TAG, "(FIRST) SESSION STATE: ${sessionState}")

        if (sessionState == 200 || sessionState == 204) {
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

                    if (sessionState == 200 || sessionState == 204) {
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
                    Log.d(TAG, "ExtThread start!")
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
                        val clockIntent = Intent(context, ClockActivity::class.java)
                        clockIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        clockIntent.putExtra("fromwhere", "ser")
                        context.startActivity(clockIntent)
                    }
                    Log.d(TAG, "ExtThread end!")
                }
            } catch (e: InterruptedException) {
                Log.w(TAG, "Interrupted: exception.", e)
            }
        }
        //start thread:
        extThread!!.start()
    }


    //PLAY INTERNALLY:
    fun playInternally(resultJSON: JsonObject, useAlbum: Boolean = false): Int {
        var url = "https://api.spotify.com/v1/me/player/play"
        var playType = resultJSON.get("play_type").asString

        //Body:
        var jsonBody = JsonObject()
        var offset = JsonObject()   //Context

        // Use requested context:
        if (playType == "track" && useAlbum) {
            jsonBody.addProperty("context_uri", resultJSON.get("album_uri").asString)
        } else {
            jsonBody.addProperty("context_uri", resultJSON.get("context_uri").asString)
        }

        //Start playing from:
        if (playType != "artist") {
            if (playType == "track") {
                offset.addProperty("uri", resultJSON.get("uri").asString)   //song uri
            } else {
                offset.addProperty("position", 0)   //beginning
            }
            jsonBody.add("offset", offset)
        }
        jsonBody.addProperty("position_ms", 0)

        //Headers:
        var jsonHeads = JsonObject()
        jsonHeads.addProperty("Authorization", "Bearer ${prefs.spotifyToken}")
        jsonHeads.addProperty("Content-Type", "application/json")

        //PUT REQUEST:
        var response = query.querySpotify(type = "put", url = url, jsonHeads = jsonHeads, jsonBody = jsonBody)
        Log.d(TAG, "PLAY INTERNALLY: ${response.code}!")
        return response.code
    }

    
    //GET PLAYBACK STATE:
    fun getPlaybackState(): Int {
        var url = "https://api.spotify.com/v1/me/player"

        //Headers:
        var jsonHeads = JsonObject()
        jsonHeads.addProperty("Authorization", "Bearer ${prefs.spotifyToken}")

        //GET REQUEST:
        var response = query.querySpotify(type = "get", url = url, jsonHeads = jsonHeads)
        Log.d(TAG, "PLAYBACK STATE: ${response.code}")
        return response.code
    }

}