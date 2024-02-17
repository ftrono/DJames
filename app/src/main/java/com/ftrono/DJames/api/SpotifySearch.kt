package com.ftrono.DJames.api

import android.util.Log
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.utilities.Utilities
import com.google.gson.JsonObject
import java.net.URLEncoder
import com.google.gson.JsonPrimitive


class SpotifySearch() {
    private val TAG = SpotifySearch::class.java.simpleName
    private var query = SpotifyQuery()
    private var utils = Utilities()

    //GENERIC SPOTIFY SEARCH -> PLAY WITHIN ALBUM:
    fun genericSearch(type: String, matchName: String, artistName: String): JsonObject {
        //BUILD GET REQUEST:
        var baseURL = "https://api.spotify.com/v1/search"
        var returnJSON = JsonObject()
        var url = baseURL

        //Extract query params:
        var qParams = ArrayList<String>()
        qParams.add("${type}:${matchName}")
        if (artistName != "") {
            qParams.add("artist:${artistName}")
        }

        //Query params:
        if (qParams.isNotEmpty()) {
            var queryParams = qParams.joinToString("&", prefix = "&")
            val encodedParams: String = URLEncoder.encode(queryParams, "UTF-8")
            url += "?q=${matchName}${encodedParams}&type=${type}&market=IT"
        } else {
            url += "?q=${matchName}&type=${type}&market=IT"
        }

        Log.d(TAG, url)

        //Headers:
        var jsonHeads = JsonObject()
        jsonHeads.addProperty("Authorization", "Bearer ${prefs.spotifyToken}")

        //REQUEST:
        var respJSON = query.querySpotify(type="get", url=url, jsonHeads=jsonHeads)

        var keySet = respJSON.keySet()
        if (keySet.size == 0) {
            Log.d(TAG, "ERROR: Spotify Search results not received!!")
            //Log.d(TAG, "returnJSON: ${returnJSON}")
            return returnJSON
        } else {
            //Analyse response & get index of best result:
            var bestResult = utils.getBestResult(respJSON, matchName, artistName)

            //Uri:
            var uri = bestResult.get("uri").asString
            returnJSON.add("uri", JsonPrimitive(uri))
            Log.d(TAG, "uri: ${uri}")

            //Spotify URL:
            var extUrls = bestResult.getAsJsonObject("external_urls")
            var spotifyURL = extUrls.get("spotify").asString
            returnJSON.add("spotify_URL", JsonPrimitive(spotifyURL))
            Log.d(TAG, "spotify_URL: ${spotifyURL}")

            //Track name:
            returnJSON.add("song_name", bestResult.get("name"))

            //Artist name:
            var artists = bestResult.getAsJsonArray("artists")
            var firstArtist = artists.get(0).asJsonObject
            returnJSON.add("artist_name", firstArtist.get("name"))

            //CONTEXT:
            //Album name:
            var album = bestResult.get("album").asJsonObject
            returnJSON.add("album_name", album.get("name"))
            returnJSON.add("album_uri", album.get("uri"))

            //(TEMP) Context -> album:
            returnJSON.add("context_type", JsonPrimitive("Album"))
            returnJSON.add("context_uri", album.get("uri"))
            returnJSON.add("context_name", album.get("name"))

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