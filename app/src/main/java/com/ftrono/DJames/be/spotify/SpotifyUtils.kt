package com.ftrono.DJames.be.spotify

import android.content.Context
import android.content.Intent
import android.media.ToneGenerator
import android.util.Log
import android.util.Patterns
import android.webkit.URLUtil
import com.ftrono.DJames.application.ACTION_TOASTER
import com.ftrono.DJames.application.artistUrlIntro
import com.ftrono.DJames.application.currentTrackId
import com.ftrono.DJames.application.overlayStatus
import com.ftrono.DJames.application.playlistUrlIntro
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.database.Artist
import com.ftrono.DJames.be.database.Playlist
import com.google.gson.JsonArray
import com.google.gson.JsonParser


class SpotifyUtils {
    private val TAG = SpotifyUtils::class.java.simpleName


    //Validate artist URL:
    fun isArtistUrl(playUrl: String): Boolean {
        val urlTest = URLUtil.isValidUrl(playUrl) && Patterns.WEB_URL.matcher(playUrl).matches()
        //True if conditions are met:
        return (urlTest && playUrl.contains(artistUrlIntro))
    }


    //Validate playlist URL:
    fun isPlaylistUrl(playUrl: String): Boolean {
        val urlTest = URLUtil.isValidUrl(playUrl) && Patterns.WEB_URL.matcher(playUrl).matches()
        //True if conditions are met:
        return (urlTest && playUrl.contains(playlistUrlIntro))
    }


    //Trim Spotify URL:
    fun trimSpotifyUrl(playURL: String): String {
        return playURL.replace(" ", "").split("?")[0].trim()
    }


    //Get item ID:
    fun getSpotifyID(url: String): String {
        return url.split("/").last()
    }


    //Disambiguate Spotify URL:
    fun disambiguateSpotifyURL(url: String): String {
        val urlTest = URLUtil.isValidUrl(url) && Patterns.WEB_URL.matcher(url).matches()
        return if (urlTest && url.contains(artistUrlIntro)) "artist" else if (urlTest && url.contains(playlistUrlIntro)) "playlist" else ""
    }

    //Save currently playing track:
    fun saveCurrentTrack(context: Context, toneGen: ToneGenerator){
        Log.d(TAG, "SaveTrack: job start!")
        var toastText = ""
        try {
            val spotifyCalls = SpotifyCalls(context)
            val ids = JsonArray()
            ids.add(currentTrackId.split(":").last())
            Log.d(TAG, "IDS TO SAVE: $ids")
            val ret = spotifyCalls.saveTrackRequest(ids)
            //PROCESS RESPONSE:
            if (ret == 200) {
                //SUCCESS -> Play ACKNOWLEDGE tone:
                toneGen.startTone(ToneGenerator.TONE_PROP_ACK)   //ACKNOWLEDGE
                Log.d(TAG, "Saved track: $ids")
                toastText = "Current track saved in Spotify!"
            } else {
                //Play FAIL tone:
                toneGen.startTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE)   //FAIL
                Log.w(TAG, "ERROR: Cannot save track: $ids")
                toastText = "ERROR: Could not save current track in Spotify!"
            }
        } catch (e: Exception) {
            //Play FAIL tone:
            toneGen.startTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE)   //FAIL
            Log.w(TAG, "ERROR: Cannot save current track!")
            toastText = "ERROR: Could not save current track in Spotify!"
        }
        //TOAST -> Send broadcast:
        Intent().also { intent ->
            intent.setAction(ACTION_TOASTER)
            intent.putExtra("toastText", toastText)
            context.sendBroadcast(intent)
        }
        overlayStatus.postValue("ready")
        Log.d(TAG, "SaveTrack: job end!")
    }


    //Get Artist info:
    fun getArtistInfo(context: Context, url: String, initArtist: Artist = Artist(), init: Boolean): Artist {
        Log.d(TAG, "getArtistInfo: job start!")
        var toastText = ""
        var itemArtist = initArtist
        try {
            val spotifyCalls = SpotifyCalls(context)
            val resp = spotifyCalls.getSpotifyArtist(getSpotifyID(url))
            //PROCESS RESPONSE:
            if (resp.code == 200) {
                //SUCCESS -> Extract info:
                Log.d(TAG, "getArtistInfo: results received!")
                val respJson = JsonParser.parseString(resp.body).asJsonObject
                itemArtist.name = respJson.get("name").asString
                itemArtist.spotifyUrl = url
                //Genres:
                var genres = mutableListOf<String>()
                for (genre in respJson.get("genres").asJsonArray){
                    genres.add(genre.asString)
                }
                itemArtist.genres = genres
                //Image URL:
                try {
                    itemArtist.imageUrl = respJson.get("images").asJsonArray.get(0).asJsonObject.get("url").asString
                    Log.d(TAG, itemArtist.imageUrl)
                } catch (e: Exception) {
                    itemArtist.imageUrl = ""
                }
                toastText = if (!init) "Refreshed!" else "Please fill in additional information!"
            } else {
                Log.w(TAG, "ERROR: Could not extract info from Spotify!")
                toastText = if (!init) "Cannot refresh now!" else "Please fill in missing information!"
            }
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: Could not extract info from Spotify! ", e)
            toastText = if (!init) "Cannot refresh now!" else "Please fill in missing information!"
        }
        //TOAST -> Send broadcast:
        Intent().also { intent ->
                intent.setAction(ACTION_TOASTER)
                intent.putExtra("toastText", toastText)
                context.sendBroadcast(intent)
            }
        Log.d(TAG, "getArtistInfo: job end!")
        return itemArtist
    }


    //Get Playlist info:
    fun getPlaylistInfo(context: Context, url: String, initPlaylist: Playlist = Playlist(), init: Boolean): Playlist {
        Log.d(TAG, "getPlaylistInfo: job start!")
        var toastText = ""
        var itemPlaylist = initPlaylist
        try {
            val spotifyCalls = SpotifyCalls(context)
            val resp = spotifyCalls.getSpotifyPlaylist(getSpotifyID(url), detailsOnly=true)
            //PROCESS RESPONSE:
            if (resp.code == 200) {
                //SUCCESS -> Extract info:
                Log.d(TAG, "getPlaylistInfo: results received!")
                val respJson = JsonParser.parseString(resp.body).asJsonObject
                itemPlaylist.name = respJson.get("name").asString
                itemPlaylist.spotifyUrl = url
                //Owner:
                try {
                    itemPlaylist.owner = utils.cleanString(respJson.get("owner").asJsonObject.get("display_name").asString)
                } catch (e: Exception) {
                    itemPlaylist.owner = ""
                }
                //Image URL:
                try {
                    itemPlaylist.imageUrl = respJson.get("images").asJsonArray.get(0).asJsonObject.get("url").asString
                } catch (e: Exception) {
                    itemPlaylist.imageUrl = ""
                }
                toastText = if (!init) "Refreshed!" else "Please fill in additional information!"
            } else {
                Log.w(TAG, "ERROR: Could not extract info from Spotify!")
                toastText = if (!init) "Cannot refresh now!" else "Please fill in missing information!"
            }
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: Could not extract info from Spotify! ", e)
            toastText = if (!init) "Cannot refresh now!" else "Please fill in missing information!"
        }
        //TOAST -> Send broadcast:
        Intent().also { intent ->
            intent.setAction(ACTION_TOASTER)
            intent.putExtra("toastText", toastText)
            context.sendBroadcast(intent)
        }
        Log.d(TAG, "getPlaylistInfo: job end!")
        return itemPlaylist
    }

}