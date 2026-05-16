package com.ftrono.DJames.be.spotify

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.ftrono.DJames.application.ACTION_MESSAGES_REFRESH
import com.ftrono.DJames.application.ACTION_TOASTER
import com.ftrono.DJames.application.ClockActivity
import com.ftrono.DJames.application.clockActive
import com.ftrono.DJames.application.spotIntroUrl
import com.ftrono.DJames.application.spotCollectionIntUri
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.spotCollectionUrl
import com.ftrono.DJames.application.spotIntroUri
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.database.SpotifyPlayable
import com.google.gson.JsonArray
import com.google.gson.JsonObject


class SpotifyPlayer (private val context: Context) {

    private val TAG = this::class.java.simpleName
    private var query = SpotifyQuery(context)
    private var extThread: Thread? = null


    //PLAY INTERNALLY OR EXTERNALLY:
    fun spotifyPlay(playable: SpotifyPlayable): Int {
        //Build external URL:
        val playUrl = if (playable.id == "collection") {
            spotCollectionUrl
        } else {
            "$spotIntroUrl/${playable.type}/${playable.id}"
        }
        //TRIAL 1:
        //Try requested context:
        val clockWasActive = clockActive.value!!
        var sessionState = playInternally(playable, useAlbum=false)
        Log.d(TAG, "(FIRST) SESSION STATE: ${sessionState}")

        if (sessionState == 200 || sessionState == 204) {
            if (playable.track == null || playable.track!!.context == null) {
                //If no track or no specific context requested for track -> terminate:
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
                    sessionState = playInternally(playable, useAlbum = true)
                    Log.d(TAG, "(SECOND) SESSION STATE: ${sessionState}")

                    //Send broadcast:
                    Intent().also { intent ->
                        intent.setAction(ACTION_MESSAGES_REFRESH)
                        context.sendBroadcast(intent)
                    }

                    if (sessionState == 200 || sessionState == 204) {
                        //OK -> Terminate:
                        return 0

                    } else {
                        //400-on: OPEN EXTERNALLY WITH CONTEXT = ALBUM:
                        //Open externally:
                        if (playable.type != "track") {
                            openExternally(playUrl, clockWasActive)
                        } else {
                            var contextUri = "$spotIntroUri:album:${playable.track!!.album!!.id}"
                            val encodedContextUri = Uri.encode(contextUri)
                            openExternally("$playUrl?context=$encodedContextUri", clockWasActive)
                        }
                        return -1
                    }
                }
            }

        } else {
            //400-on: OPEN EXTERNALLY WITH CONTEXT = ALBUM:
            //Open externally:
            if (playable.type != "track") {
                openExternally(playUrl, clockWasActive)
            } else {
                var contextUri = "$spotIntroUri:album:${playable.track!!.album!!.id}"
                val encodedContextUri = Uri.encode(contextUri)
                openExternally("$playUrl?context=$encodedContextUri", clockWasActive)
            }

            //Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_MESSAGES_REFRESH)
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
                    utils.openLink(context, url = spotifyToOpen, fromService = true)

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
                        utils.openActivity(context, ClockActivity::class.java, fromService=true)
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
    fun playInternally(playable: SpotifyPlayable, useAlbum: Boolean = false): Int {
        var url = "https://api.spotify.com/v1/me/player/play"

        //Body:
        var jsonBody = JsonObject()
        var offset = JsonObject()   //Context

        //Use uri vs context_uri:
        if (playable.type == "episode" && playable.episode != null) {
            // EPISODE (NOTE: Podcast must always be played as an episode!):
            val uris = JsonArray()
            uris.add("$spotIntroUri:episode:${playable.id}")
            jsonBody.add("uris", uris)
            jsonBody.addProperty("position_ms", playable.episode!!.resumePositionMs)

        } else {
            if (playable.type == "track" && playable.track != null) {
                // TRACK CONTEXT:
                // Default -> album:
                var contextUri = "$spotIntroUri:album:${playable.track!!.album!!.id}"

                // Else -> Requested context:
                if (!useAlbum && playable.track!!.context != null) {
                    // Use requested track context:
                    if (playable.track!!.context!!.id == "collection") {
                        // Liked Songs collection:
                        contextUri = spotCollectionIntUri.replace("replaceUserId", prefs.spotUserId)

                    } else if (playable.track!!.context!!.id != "") {
                        // Playlist:
                        contextUri = "$spotIntroUri:playlist:${playable.track!!.context!!.id}"
                    }
                }
                jsonBody.addProperty("context_uri", contextUri)

            } else if (playable.id == "collection") {
                // COLLECTION -> Collection uri as context:
                jsonBody.addProperty("context_uri", spotCollectionIntUri.replace("replaceUserId", prefs.spotUserId))

            } else {
                // Use main uri as context:
                jsonBody.addProperty("context_uri", "$spotIntroUri:${playable.type}:${playable.id}")
            }

            //Start playing from:
            if (playable.type != "artist") {
                if (playable.type == "track") {
                    offset.addProperty("uri", "$spotIntroUri:${playable.type}:${playable.id}")   //track uri
                } else {
                    offset.addProperty("position", 0)   //beginning
                }
                jsonBody.add("offset", offset)
            }
            jsonBody.addProperty("position_ms", 0)
        }

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