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
import com.ftrono.DJames.application.episodeUrlIntro
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.overlayStatus
import com.ftrono.DJames.application.playlistUrlIntro
import com.ftrono.DJames.application.sharedLink
import com.ftrono.DJames.application.showUrlIntro
import com.ftrono.DJames.application.spotifyUtils
import com.ftrono.DJames.application.trackUrlIntro
import com.ftrono.DJames.be.database.Artist
import com.ftrono.DJames.be.database.Playlist
import com.ftrono.DJames.be.database.Podcast
import com.ftrono.DJames.be.database.SpotifyPlayable
import com.ftrono.DJames.be.models.HttpResponse
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


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
        else if (
            urlTest && url.contains(showUrlIntro)
        ) "podcast"
        else if (
            urlTest && url.contains(trackUrlIntro)
        ) "track"
        else if (
            urlTest && url.contains(episodeUrlIntro)
        ) "episode"
        else ""
    }


    //Disambiguate and Open EditLibDialog:
    fun checkAndEditLib(
        context: Context,
        idState: MutableState<Long>,
        addLinkState: MutableState<String>,
        currentCatState: MutableState<String>,
        editLibOn: MutableState<Boolean>,
        loadingDialogOn: MutableState<Boolean>
    ) {
        loadingDialogOn.value = true
        var urlToCheck = if (sharedLink.value != "") sharedLink.value!!.trim() else (addLinkState.value)
        var goto = spotifyUtils.disambiguateSpotifyURL(urlToCheck)
        if (goto != "") {
            addLinkOn.postValue(false)
            //Check extract parent:
            if (goto == "track" || goto == "episode") {
                val parentId = getParentIdFromChildUrl(context, goto, urlToCheck)
                if (parentId == "") {
                    loadingDialogOn.value = false
                    Toast.makeText(context, "Invalid Spotify Artist, Playlist or Podcast link!", Toast.LENGTH_SHORT).show()
                    urlToCheck = ""
                } else if (goto == "track") {
                    goto = "artist"
                    urlToCheck = artistUrlIntro + parentId
                } else {
                    goto = "podcast"
                    urlToCheck = showUrlIntro + parentId
                }
                Log.d(TAG, urlToCheck)
            }
            if (urlToCheck != "") {
                //Go to right Edit Lib dialog:
                currentCatState.value = goto
                val urlMap = libUtils.getUrlMap(goto)
                val foundId = urlMap.getOrDefault(urlToCheck, -1L)
                if (foundId > -1) {
                    idState.value = foundId
                    loadingDialogOn.value = false
                } else {
                    addLinkState.value = urlToCheck
                }
                editLibOn.value = true
            }
        } else {
            loadingDialogOn.value = false
            Toast.makeText(context, "Invalid Spotify Artist, Playlist or Podcast link!", Toast.LENGTH_SHORT).show()
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


    //Get Parent ID from Child URL:
    fun getParentIdFromChildUrl(context: Context, childCat: String, url: String): String {
        Log.d(TAG, "getParentIdFromChildUrl: job start!")
        var childId = ""
        var parentCat = ""
        //Extract:
        try {
            val spotifyCalls = SpotifyCalls(context)
            var resp = HttpResponse(
                code = -1,  // -1 to indicate failure
                body = ""
            )
            //Select parent:
            if (childCat == "track") {
                parentCat = "artist"
                resp = spotifyCalls.getSpotifyTrack(getSpotifyID(url))
            } else if (childCat == "episode") {
                parentCat = "show"
                resp = spotifyCalls.getSpotifyEpisode(getSpotifyID(url))
            }
            //PROCESS RESPONSE:
            if (resp.code == 200) {
                //SUCCESS -> Extract Parent ID:
                Log.d(TAG, "getParentIdFromChildUrl: results received!")
                val respJson = JsonParser.parseString(resp.body).asJsonObject
                if (parentCat == "artist") {
                    // Track -> Artist:
                    childId = respJson.get("artists").asJsonArray.get(0).asJsonObject.get("id").asString   //take 1st artist by default
                } else {
                    // Episode -> Show:
                    childId = respJson.get("show").asJsonObject.get("id").asString
                }
            } else {
                Log.w(TAG, "ERROR: Could not extract $parentCat from $childCat!")
            }
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: Could not extract $parentCat from $childCat! ", e)
        }
        Log.d(TAG, "getParentIdFromChildUrl: job end!")
        return childId
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


    //Get Podcast info:
    fun getPodcastInfo(context: Context, url: String, initPodcast: Podcast = Podcast(), init: Boolean): Podcast {
        Log.d(TAG, "getPodcastInfo: job start!")
        var toastText = ""
        var itemPodcast = initPodcast
        try {
            val spotifyCalls = SpotifyCalls(context)
            val resp = spotifyCalls.getSpotifyPodcast(getSpotifyID(url))
            //PROCESS RESPONSE:
            if (resp.code == 200) {
                //SUCCESS -> Extract info:
                Log.d(TAG, "getPodcastInfo: results received!")
                val respJson = JsonParser.parseString(resp.body).asJsonObject
                itemPodcast.name = respJson.get("name").asString
                itemPodcast.spotifyUrl = url
                itemPodcast.publisher = respJson.get("publisher").asString
                itemPodcast.description = respJson.get("description").asString
                //Languages:
                var itemLanguages = mutableListOf<String>()
                for (obj in respJson.get("languages").asJsonArray) {
                    itemLanguages.add(obj.asString)
                }
                itemPodcast.languages = itemLanguages
                //Image URL:
                try {
                    itemPodcast.imageUrl = respJson.get("images").asJsonArray.get(0).asJsonObject.get("url").asString
                } catch (e: Exception) {
                    itemPodcast.imageUrl = ""
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
        Log.d(TAG, "getPodcastInfo: job end!")
        return itemPodcast
    }


    //Get Podcast info:
    fun getPodcastEpisodes(context: Context, spotifyId: String, latestOnly: Boolean = true): List<SpotifyPlayable> {
        Log.d(TAG, "getPodcastEpisodes: job start!")
        var episodes = mutableListOf<SpotifyPlayable>()
        try {
            val spotifyCalls = SpotifyCalls(context)
            val resp = spotifyCalls.getSpotifyPodcastEpisodes(spotifyId, limit = if (latestOnly) 1 else null)
            //PROCESS RESPONSE:
            if (resp.code == 200) {
                //SUCCESS -> Extract info:
                Log.d(TAG, "getPodcastEpisodes: results received!")
                val respJson = JsonParser.parseString(resp.body).asJsonObject
                val items = respJson.get("items").asJsonArray
                if (items.isEmpty) {
                    Log.w(TAG, "ERROR: Could not get Podcast Episodes from Spotify!")
                } else {
                    for (item in items) {
                        val itemJson = item.asJsonObject
                        var fullyPlayed = false
                        var resumePositionMs = 0
                        //ResumePoint info:
                        try {
                            var itemResume = itemJson.get("resume_point").asJsonObject
                            fullyPlayed = itemResume.get("fully_played").asBoolean
                            if (!fullyPlayed) {
                                resumePositionMs = itemResume.get("resume_position_ms").asInt
                            }
                        } catch (e: Exception) {
                            Log.d(TAG, "No ResumePoint info in current Episode!")
                        }
                        //Languages:
                        var itemLanguages = mutableListOf<String>()
                        for (obj in itemJson.get("languages").asJsonArray) {
                            itemLanguages.add(obj.asString)
                        }
                        episodes.add(
                            SpotifyPlayable(
                                type = "episode",
                                id = itemJson.get("id").asString,
                                name = itemJson.get("name").asString,
                                releaseDate = try {
                                    itemJson.get("release_date").asString
                                } catch (e: Exception) {
                                    ""
                                },
                                languages = itemLanguages,
                                fullyPlayed = fullyPlayed,
                                resumePositionMs = resumePositionMs,
                            )
                        )
                    }
                }

            } else {
                Log.w(TAG, "ERROR: Could not get Podcast Episodes from Spotify!")
            }
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: Could not get Podcast Episodes from Spotify! ", e)
        }
        //TOAST:
        Log.d(TAG, "getPodcastEpisodes: job end!")
        return episodes
    }


}