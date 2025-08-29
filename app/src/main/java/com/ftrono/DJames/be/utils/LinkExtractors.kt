package com.ftrono.DJames.be.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.ftrono.DJames.application.sourceToCatMap
import com.ftrono.DJames.application.spotifyUtils
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.database.LibraryItem
import com.ftrono.DJames.be.models.RawLinkPreview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup


class LinkExtractors {
    private val TAG = LinkExtractors::class.java.simpleName

    // GET LINK PREVIEW:
    suspend fun fetchRawLinkPreview(url: String): RawLinkPreview {
        val linkPreview = RawLinkPreview()
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0") // important: some sites block bots
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext linkPreview

                    val body = response.body?.string() ?: return@withContext linkPreview
                    val doc = Jsoup.parse(body)

                    // Try Open Graph first
                    val title = doc.select("meta[property=og:title]").attr("content")
                        .ifBlank { doc.title() }
                    val description = doc.select("meta[property=og:description]").attr("content")
                        .ifBlank { doc.select("meta[name=description]").attr("content") }
                    val image = doc.select("meta[property=og:image]").attr("content")
                    val pageUrl = doc.select("meta[property=og:url]").attr("content")
                        .ifBlank { url }

                    RawLinkPreview(
                        title = title,
                        description = description,
                        imageUrl = image.ifBlank { "" },
                        url = pageUrl
                    )
                }
            } catch (e: Exception) {
                Log.d(TAG, "fetchLinkPreview(): ERROR in fetching link preview: ", e)
                linkPreview
            }
        }
    }

    // Main Spotify Extractor:
    fun extractSpotifyInfo(context: Context, initLibItem: LibraryItem, new: Boolean): LibraryItem {
        var updLibItem = initLibItem
        var linkPreview = RawLinkPreview()
        var toastText = ""
        runBlocking {
            linkPreview = fetchRawLinkPreview(initLibItem.url)
            linkPreview.let {
                println("Title: ${it.title}")
                println("Description: ${it.description}")
                println("Image: ${it.imageUrl}")
                println("URL: ${it.url}")
            }
            Log.d(TAG, "Description: ${linkPreview.description}")
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
                toastText = if (new) "Link info extracted!" else "Info refreshed!"
            }
            Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
            return@runBlocking updLibItem
        }
        return updLibItem
    }

}