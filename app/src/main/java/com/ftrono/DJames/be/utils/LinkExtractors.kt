package com.ftrono.DJames.be.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.ftrono.DJames.application.defaultHttpTimeout
import com.ftrono.DJames.application.sourceToCatMap
import com.ftrono.DJames.application.spotifyUtils
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.database.ExtractedItem
import com.ftrono.DJames.be.database.LibraryItem
import com.ftrono.DJames.be.models.RawLinkPreview
import com.ftrono.DJames.be.spotify.SpotifyCalls
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup


class LinkExtractors {
    private val TAG = LinkExtractors::class.java.simpleName

    // GET LINK PREVIEW:
    suspend fun fetchRawLinkPreview(url: String): RawLinkPreview {
        val linkPreview = RawLinkPreview()
        return withContext(Dispatchers.IO) {
            try {
                // Build request:
                val httpClient = HttpClient()
                val client = httpClient.getClient(defaultHttpTimeout)
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0") // important: some sites block bots
                    .build()

                // Query HTTP:
                var response = httpClient.makeRequest(client, request)
                if (response.code == 200) {
                    val doc = Jsoup.parse(response.body)

                    // Try Open Graph first
                    val title = doc.select("meta[property=og:title]").attr("content")
                        .ifBlank { doc.title() }
                    val description = doc.select("meta[property=og:description]").attr("content")
                        .ifBlank { doc.select("meta[name=description]").attr("content") }
                    val image = doc.select("meta[property=og:image]").attr("content")
                    val pageUrl = doc.select("meta[property=og:url]").attr("content")
                        .ifBlank { url }

                    // Return:
                    RawLinkPreview(
                        title = title,
                        description = description,
                        imageUrl = image.ifBlank { "" },
                        url = pageUrl
                    )
                } else {
                    linkPreview
                }

            } catch (e: Exception) {
                Log.d(TAG, "fetchLinkPreview(): ERROR in fetching link preview: ", e)
                linkPreview
            }
        }
    }

    // Main: Spotify Link Extractor (via HTTP link preview parsing):
    fun extractSpotifyInfoFromHTTP(context: Context, initLibItem: LibraryItem, new: Boolean): ExtractedItem {
        var returnItem = ExtractedItem()
        var updLibItem = initLibItem
        var linkPreview = RawLinkPreview()
        var toastText = ""
        runBlocking {
            linkPreview = fetchRawLinkPreview(initLibItem.url)
            Log.d(TAG, "Title: ${linkPreview.title} - Description: ${linkPreview.description}")
            val descrSplits = linkPreview.description.split(" · ")

            // Extract type:
            val albumTypes = listOf("album", "single", "ep", "compilation")
            var albumType = "n/a"
            for (split in descrSplits) {
                val splitOk = split.lowercase()
                if (splitOk == "song") {
                    updLibItem.type = "track"
                    break
                } else if (splitOk in sourceToCatMap["spotify"]!!) {
                    updLibItem.type = splitOk
                    break
                } else if (splitOk in albumTypes) {
                    updLibItem.type = "album"
                    albumType = utils.capitalizeWords(splitOk)
                    break
                }
            }
            if (linkPreview.title == "" || updLibItem.type == "") {
                Log.w(TAG, "ERROR: Could not extract info from Spotify!")
                toastText = if (new) "Cannot extract link info!" else "Cannot refresh info now!"
                Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
                return@runBlocking initLibItem

            } else {
                returnItem.response = 200
                updLibItem.name = linkPreview.title.split(" - $albumType by")[0]
                updLibItem.detail = if (descrSplits.size > 1) {
                    if (updLibItem.type == "album" || updLibItem.type == "track") {
                        // Pos 0 -> artist name:
                        descrSplits[0]
                    } else if (updLibItem.type == "playlist" || updLibItem.type == "podcast") {
                        // Pos 1 -> playlist owner or publisher:
                        descrSplits[1]
                    }  else {
                        initLibItem.detail
                    }
                } else {
                    initLibItem.detail
                }
                updLibItem.imageUrl = if (linkPreview.imageUrl != "") linkPreview.imageUrl else initLibItem.imageUrl
                // toastText = if (new) "Link info extracted!" else "Info refreshed!"
            }
            returnItem.libItem = updLibItem
            if (toastText != "") {
                Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
            }
            return@runBlocking returnItem
        }
        return returnItem
    }


    // Main: Spotify Link Extractor (via Spotify Web API):
    fun extractSpotifyInfoFromAPI(context: Context, initItem: LibraryItem, new: Boolean): ExtractedItem {
        Log.d(TAG, "extractSpotifyInfoFromAPI: job start!")
        var toastText = ""
        var returnItem = ExtractedItem()
        var itemSpotify = initItem
        val filter = itemSpotify.type
        try {
            val spotifyCalls = SpotifyCalls(context)
            val resp = spotifyCalls.getSpotifyItem(type=filter, id=spotifyUtils.getSpotifyID(itemSpotify.url))
            //PROCESS RESPONSE:
            if (resp.code == 200) {
                returnItem.response = resp.code
                //SUCCESS -> Extract info:
                Log.d(TAG, "extractSpotifyInfoFromAPI: results received!")
                val respJson = JsonParser.parseString(resp.body).asJsonObject
                itemSpotify.name = respJson.get("name").asString
                itemSpotify.url = itemSpotify.url
                //Image URL:
                try {
                    itemSpotify.imageUrl = if (filter == "track") {
                        respJson.get("album").asJsonObject.get("images").asJsonArray.get(0).asJsonObject.get("url").asString
                    } else {
                        respJson.get("images").asJsonArray.get(0).asJsonObject.get("url").asString
                    }
                    Log.d(TAG, itemSpotify.imageUrl)
                } catch (e: Exception) {
                    itemSpotify.imageUrl = ""
                }
                //Detail:
                try {
                    itemSpotify.detail = when (filter) {
                        "track" -> {
                            respJson.get("artists").asJsonArray.joinToString(", ") {
                                it.asJsonObject.get("name").asString
                            }
                        }
                        "album" -> {
                            respJson.get("artists").asJsonArray.joinToString(", ") {
                                it.asJsonObject.get("name").asString
                            }
                        }
                        "artist" -> {
                            respJson.get("genres").asJsonArray.joinToString(", ") {
                                it.asString
                            }
                        }
                        "playlist" -> respJson.get("owner").asJsonObject.get("display_name").asString
                        "podcast" -> respJson.get("publisher").asString
                        "episode" -> respJson.get("show").asJsonObject.get("name").asString
                        else -> ""
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "ERROR: Cannot extract item.detail info! ", e)
                    itemSpotify.detail = ""
                }
                // toastText = if (!new) "Refreshed!" else "Please fill in additional information!"
            } else {
                Log.w(TAG, "ERROR: Could not extract info from Spotify!")
                toastText = if (!new) "Cannot refresh now!" else "Please fill in missing information!"
            }
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: Could not extract info from Spotify! ", e)
            toastText = if (!new) "Cannot refresh now!" else "Please fill in missing information!"
        }
        //TOAST:
        if (toastText != "") {
            Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
        }
        Log.d(TAG, "extractSpotifyInfoFromAPI: job end!")
        returnItem.libItem = itemSpotify
        return returnItem
    }

}