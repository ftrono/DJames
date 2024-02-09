package com.ftrono.DJames.api

import android.util.Log
import com.ftrono.DJames.application.prefs
import com.google.gson.JsonObject
import java.net.URLEncoder
import com.google.gson.JsonPrimitive


class SpotifySearch() {
    private val TAG = SpotifySearch::class.java.simpleName
    private var query = SpotifyQuery()

    //GENERIC SPOTIFY SEARCH -> PLAY WITHIN ALBUM:
    fun genericSearch(results: Array<String>): JsonObject {
        //BUILD GET REQUEST:
        var baseURL = "https://api.spotify.com/v1/search"
        var returnJSON = JsonObject()

        //Query params:
        var queryParams = results.joinToString("&")
        val encodedParams: String = URLEncoder.encode(queryParams, "UTF-8")
        var url = baseURL + "?q=" + encodedParams + "&type=track"
        Log.d(TAG, url)

        //Headers:
        var jsonHeads = JsonObject()
        jsonHeads.addProperty("Authorization", "Bearer ${prefs.spotifyToken}")

        //REQUEST:
        var respJSON = query.querySpotify(type="get", url=url, jsonHeads=jsonHeads)

        var keySet = respJSON.keySet()
        if (keySet.size == 0) {
            Log.d(TAG, "ERROR: Spotify Search results not received!!")
            Log.d(TAG, "returnJSON: ${returnJSON}")
            return returnJSON
        } else {
            //Analyse response:
            var tracks = respJSON.getAsJsonObject("tracks")
            var items = tracks.getAsJsonArray("items")
            //Log.d(TAG, "Items: ${items}")

            //FROM FIRST RESULT:
            var firstResult = items.get(0).asJsonObject

            //Uri:
            var uri = firstResult.get("uri").asString
            returnJSON.add("uri", JsonPrimitive(uri))
            Log.d(TAG, "uri: ${uri}")

            //Spotify URL:
            var extUrls = firstResult.getAsJsonObject("external_urls")
            var spotifyURL = extUrls.get("spotify").asString
            returnJSON.add("spotify_URL", JsonPrimitive(spotifyURL))
            Log.d(TAG, "spotify_URL: ${spotifyURL}")

            //Track name:
            returnJSON.add("song_name", firstResult.get("name"))

            //Artist name:
            var artists = firstResult.getAsJsonArray("artists")
            var firstArtist = artists.get(0).asJsonObject
            returnJSON.add("artist_name", firstArtist.get("name"))

            //CONTEXT:
            //Album name:
            var album = firstResult.get("album").asJsonObject
            returnJSON.add("context_type", JsonPrimitive("Album"))
            returnJSON.add("context_name", album.get("name"))
            returnJSON.add("context_uri", album.get("uri"))
            returnJSON.add(
                "track_number",
                JsonPrimitive(firstResult.get("track_number").asInt - 1)
            )   //offset = track_number - 1

            //Artwork:
            if (album.has("images")) {
                try {
                    var images = album.getAsJsonArray("images")
                    var firstImage = images.get(0).asJsonObject
                    returnJSON.add("artwork", firstImage.get("url"))
                } catch (e: Exception) {
                    Log.d(TAG, "Unable to retrieve album artwork: ", e)
                }
            }

            Log.d(TAG, "Spotify Search results successfully processed!")
            Log.d(TAG, "returnJSON: ${returnJSON}")
            return returnJSON
        }
    }
}