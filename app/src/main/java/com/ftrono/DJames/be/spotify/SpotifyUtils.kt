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
import com.ftrono.DJames.application.albumUrlIntro
import com.ftrono.DJames.application.artistUrlIntro
import com.ftrono.DJames.application.currentTrackId
import com.ftrono.DJames.application.episodeUrlIntro
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.queryStatus
import com.ftrono.DJames.application.playlistUrlIntro
import com.ftrono.DJames.application.sharedLink
import com.ftrono.DJames.application.showUrlIntro
import com.ftrono.DJames.application.spotifyParsers
import com.ftrono.DJames.application.spotifyUtils
import com.ftrono.DJames.application.trackUrlIntro
import com.ftrono.DJames.be.database.LibraryItem
import com.ftrono.DJames.be.database.SpotifyEpisode
import com.ftrono.DJames.be.database.SpotifyPlayable
import com.ftrono.DJames.be.utils.LinkExtractors
import com.google.gson.JsonParser
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString


class SpotifyUtils {
    private val TAG = this::class.java.simpleName

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
            urlTest && url.contains(albumUrlIntro)
        ) "album"
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


    //Get Parent ID from Child URL:
    fun getParentIdFromChildUrl(context: Context, childCat: String, url: String): String {
        Log.d(TAG, "getParentIdFromChildUrl: job start!")
        var childId = ""
        var parentCat = ""
        //Extract:
        try {
            val spotifyCalls = SpotifyCalls(context)
            var resp = spotifyCalls.getSpotifyItem(type=childCat, id=getSpotifyID(url))
            //Select parent:
            if (childCat == "track") {
                parentCat = "artist"
            } else if (childCat == "episode") {
                parentCat = "show"
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


    //Disambiguate and Open EditLibDialog:
    fun checkLinkAndExtract(
        context: Context,
        idState: MutableState<Long>,
        currentCatState: MutableState<String>,
        currentSubCatState: MutableState<String>,
        addLinkOnState: MutableState<Boolean>,
        editLibOn: MutableState<Boolean>,
        extractedItemState: MutableState<String>,
        useParent: Boolean = false
    ) {
        var urlToCheck = trimSpotifyUrl(sharedLink.value!!)
        // sharedLink.postValue(urlToCheck)
        var goto = spotifyUtils.disambiguateSpotifyURL(urlToCheck)
        if (goto != "") {
            addLinkOnState.value = false
            //Check extract parent:
            if (useParent && (goto == "track" || goto == "episode")) {
                val parentId = getParentIdFromChildUrl(context, goto, urlToCheck)
                if (parentId == "") {
                    Toast.makeText(context, "Invalid Spotify link!", Toast.LENGTH_SHORT).show()
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
                currentCatState.value = "spotify"
                currentSubCatState.value = ""

                val foundId = libUtils.getLibIDWithUrl(urlToCheck)
                if (foundId > -1) {
                    // Link exists in DB!
                    idState.value = foundId
                } else {
                    // New link -> Extract info!
                    var itemSpotify = LibraryItem(
                        source = "spotify",
                        type = goto,
                        url = urlToCheck
                    )
                    // Main -> use Spotify API:
                    itemSpotify = callLinkExtractor(context, itemSpotify, new=true)
                    extractedItemState.value = Json.encodeToString<LibraryItem>(itemSpotify)
                    sharedLink.postValue("")
                }
                Log.d(TAG, "CHECK & EDIT URL: foundID: $foundId, useParent: $useParent, goto: $goto, URL: $urlToCheck")
                addLinkOnState.value = false
                editLibOn.value = true
            }
        } else {
            Toast.makeText(context, "Invalid Spotify link!", Toast.LENGTH_SHORT).show()
        }
    }


    fun callLinkExtractor(context: Context, initItem: LibraryItem, new: Boolean = false): LibraryItem {
        // Main -> use Spotify API:
        val linkExtractor = LinkExtractors()
        var itemSpotify = initItem
        var extractedItem = linkExtractor.extractSpotifyInfoFromAPI(context, itemSpotify, new)
        Log.d(TAG, "First link extraction: $itemSpotify")
        if (extractedItem.response != 200) {
            // Backup -> use link preview:
            extractedItem = linkExtractor.extractSpotifyInfoFromHTTP(context, itemSpotify, new)
            Log.d(TAG, "Second link extraction: $itemSpotify")
        }
        itemSpotify = extractedItem.libItem
        if (itemSpotify.type == "playlist" && itemSpotify.detail == "") {
            itemSpotify.detail = "Spotify"
        }
        return itemSpotify
    }


    //Get currently playing item:
    fun getCurrentlyPlayingItem(context: Context): SpotifyPlayable {
        Log.d(TAG, "getCurrentlyPlayingItem: job start!")
        var playable = SpotifyPlayable()
        try {
            val spotifyCalls = SpotifyCalls(context)
            val resp = spotifyCalls.getCurrentPlayQueue()
            //PROCESS RESPONSE:
            if (resp.code == 200) {
                //SUCCESS -> Extract info:
                Log.d(TAG, "getCurrentlyPlayingItem: results received!")
                val respJson = JsonParser.parseString(resp.body).asJsonObject
                try {
                    val curPlaying = respJson.get("currently_playing").asJsonObject
                    val playType = curPlaying.get("type").asString
                    //TODO: Check item "currently_playing.type", it can be "track" or "episode"
                    if (playType != "track" && playType != "episode") {
                        Log.w(TAG, "ERROR: getCurrentlyPlayingItem: item not a track or an episode!")
                        return playable
                    } else {
                        playable.type = playType
                        playable.id = curPlaying.get("id").asString
                        if (playType == "track") {
                            // TRACK:
                            playable.track = spotifyParsers.extractTrack(curPlaying)
                        } else {
                            // EPISODE:
                            playable.episode = spotifyParsers.extractEpisodeFromPodcast(curPlaying)
                        }
                    }
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
        return playable
    }


    //Save currently playing track:
    fun saveCurrentTrack(context: Context, toneGen: ToneGenerator){
        Log.d(TAG, "SaveTrack: job start!")
        var toastText = ""
        try {
            val spotifyCalls = SpotifyCalls(context)
            val ids = listOf(currentTrackId.split(":").last())
            Log.d(TAG, "IDS TO SAVE: $ids")
            val ret = spotifyCalls.saveLibraryRequest(ids, type="track")
            //PROCESS RESPONSE:
            if (ret == 200) {
                //SUCCESS -> Play ACKNOWLEDGE tone:
                toneGen.startTone(ToneGenerator.TONE_PROP_ACK)   //ACKNOWLEDGE
                Log.d(TAG, "Saved track: $ids")
                toastText = "Current track SAVED in Spotify!"
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
        queryStatus.postValue("ready")
        Log.d(TAG, "SaveTrack: job end!")
    }


    //Get Podcast episodes:
    fun getPodcastEpisodes(context: Context, podcastId: String, podcastName: String, latestOnly: Boolean = true): List<SpotifyEpisode> {
        Log.d(TAG, "getPodcastEpisodes: job start!")
        var episodes = mutableListOf<SpotifyEpisode>()
        try {
            val spotifyCalls = SpotifyCalls(context)
            val resp = spotifyCalls.getSpotifyPodcastEpisodes(podcastId, limit = if (latestOnly) 1 else null)
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
                        episodes.add(spotifyParsers.extractEpisodeFromLibItem(item.asJsonObject, podcastId, podcastName))
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