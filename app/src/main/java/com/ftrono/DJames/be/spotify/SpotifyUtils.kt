package com.ftrono.DJames.be.spotify

import android.content.Context
import android.content.Intent
import android.media.ToneGenerator
import android.util.Log
import android.util.Patterns
import android.webkit.URLUtil
import android.widget.Toast
import androidx.compose.runtime.MutableState
import com.ftrono.DJames.application.ACTION_TOASTER
import com.ftrono.DJames.application.addLinkOn
import com.ftrono.DJames.application.artistUrlIntro
import com.ftrono.DJames.application.currentTrackId
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.overlayStatus
import com.ftrono.DJames.application.playlistUrlIntro
import com.ftrono.DJames.application.sharedLink
import com.ftrono.DJames.application.spotifyUtils
import com.ftrono.DJames.be.database.Artist
import com.ftrono.DJames.be.database.Playlist
import com.google.gson.JsonArray
import com.google.gson.JsonParser


class SpotifyUtils {
    private val TAG = SpotifyUtils::class.java.simpleName

    //Extract URL from share message:
    fun extractUrl(text: String): String {
        var url = ""
        Log.d(TAG, "Extracting URL from message: $text")
        try {
            if (text.contains("https://")) {
                url = text.split("https://")[1].trim()
                url = "https://" + url.split(" ")[0]
                url = trimSpotifyUrl(url.trim())
                Log.d(TAG, "Extracted URL: $url")
            } else {
                Log.w(TAG, "Cannot extract URL from text!")
            }
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: Cannot extract URL from text. ", e)
        }
        return url
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
        return if (
            urlTest && url.contains(artistUrlIntro)
            ) "artist"
        else if (
            urlTest && url.contains(playlistUrlIntro)
            ) "playlist"
        else ""
    }


    //Disambiguate and Open EditVocDialog:
    fun checkAndEditVoc(
        context: Context,
        keyState: MutableState<String>,
        addLinkState: MutableState<String>,
        currentCatState: MutableState<String>,
        editVocOn: MutableState<Boolean>,
        loadingDialogOn: MutableState<Boolean>
    ) {
        loadingDialogOn.value = true
        var urlToCheck = if (sharedLink.value != "") sharedLink.value!!.trim() else (addLinkState.value)
        val goto = spotifyUtils.disambiguateSpotifyURL(urlToCheck)
        if (goto != "") {
            addLinkOn.postValue(false)
            currentCatState.value = goto
            val urlMap = libUtils.getUrlMap(goto)
            val foundKey = urlMap.getOrDefault(urlToCheck, "")
            if (foundKey != "") {
                keyState.value = foundKey
                loadingDialogOn.value = false
            } else {
                addLinkState.value = urlToCheck
            }
            editVocOn.value = true
        } else {
            loadingDialogOn.value = false
            Toast.makeText(context, "Invalid Spotify Artist or Playlist link!", Toast.LENGTH_SHORT).show()
        }
        sharedLink.postValue("")
    }


    //Get currently playing item:
    fun getCurrentlyPlayingItem(context: Context){
        Log.d(TAG, "getCurrentlyPlayingItem: job start!")
        try {
            val spotifyCalls = SpotifyCalls(context)
            val resp = spotifyCalls.getCurrentPlayQueue()
            //PROCESS RESPONSE:
            if (resp.code == 200) {
                //SUCCESS -> Extract info:
                Log.d(TAG, "getCurrentlyPlayingItem: results received!")
                val respJson = JsonParser.parseString(resp.body).asJsonObject
                try {
                    val playType = respJson.get("currently_playing").asJsonObject.get("type").asString
                    //TODO: Check item "currently_playing.type", it can be "track" or "episode"

                } catch (e: Exception) {
                    Log.w(TAG, "getCurrentlyPlayingItem: Nothing playing now!")
                }
            } else {
                Log.w(TAG, "ERROR: Cannot check current play queue!")
            }
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: Cannot check current play queue!")
        }
        Log.d(TAG, "getCurrentlyPlayingItem: job end!")
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
        //TOAST:
        Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
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
                    itemPlaylist.owner = respJson.get("owner").asJsonObject.get("display_name").asString
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
        //TOAST:
        Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
        Log.d(TAG, "getPlaylistInfo: job end!")
        return itemPlaylist
    }

}