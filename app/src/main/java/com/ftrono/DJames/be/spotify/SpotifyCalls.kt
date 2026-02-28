package com.ftrono.DJames.be.spotify

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.spotIntroUri
import com.ftrono.DJames.be.models.HttpResponse
import com.google.gson.JsonObject


class SpotifyCalls(private val context: Context) {
    private val TAG = this::class.java.simpleName
    private var query = SpotifyQuery(context)


    //SAVE CURRENT TRACK:
    fun saveLibraryRequest(ids: List<String>, type: String): Int {
        var url = "https://api.spotify.com/v1/me/library"

        //Data:
        val uris = ids.joinToString(",") { "$spotIntroUri%3A${type}%3A${it}" }

        //Headers:
        var jsonHeads = JsonObject()
        jsonHeads.addProperty("Authorization", "Bearer ${prefs.spotifyToken}")
        jsonHeads.addProperty("Content-Type", "application/json")

        //PUT REQUEST:
        var response = query.querySpotify(type = "put", url = "$url?uris=$uris", jsonHeads = jsonHeads)
        Log.d(TAG, "saveLibraryRequest: response code: ${response.code}")
        Log.d(TAG, "saveLibraryRequest: response: ${response}")
        return response.code
    }


    //GET CURRENT PLAY QUEUE:
    fun getCurrentPlayQueue(): HttpResponse {
        var url = "https://api.spotify.com/v1/me/player/queue"

        //Headers:
        var jsonHeads = JsonObject()
        jsonHeads.addProperty("Authorization", "Bearer ${prefs.spotifyToken}")

        //GET REQUEST:
        var response = query.querySpotify(type = "get", url = url, jsonHeads = jsonHeads)
        Log.d(TAG, "getCurrentPlayQueue: response code: ${response.code}")
        return response
    }


    //GET SPOTIFY ITEM:
    fun getSpotifyItem(type: String, id: String, detailsOnly: Boolean = true): HttpResponse {
        val filter = if (type == "podcast") "show" else type
        var url = "https://api.spotify.com/v1/${filter}s/$id"
        if (type == "playlist" && detailsOnly) {
            url += "?fields=name%2Cimages%2Cowner%2Csnapshot_id"
        }

        //Headers:
        var jsonHeads = JsonObject()
        jsonHeads.addProperty("Authorization", "Bearer ${prefs.spotifyToken}")

        //GET REQUEST:
        var response = query.querySpotify(type = "get", url = url, jsonHeads = jsonHeads)
        Log.d(TAG, "getSpotifyItem: response code for get $type: ${response.code}")
        return response
    }


    //GET SPOTIFY PODCAST:
    fun getSpotifyPodcastEpisodes(id: String, limit: Int? = null): HttpResponse {
        var url = "https://api.spotify.com/v1/shows/$id/episodes"
        if (limit != null) {
            url = "$url?limit=$limit"
        }

        //Headers:
        var jsonHeads = JsonObject()
        jsonHeads.addProperty("Authorization", "Bearer ${prefs.spotifyToken}")

        //GET REQUEST:
        var response = query.querySpotify(type = "get", url = url, jsonHeads = jsonHeads)
        Log.d(TAG, "getPodcastEpisodes: response code: ${response.code}")
        return response
    }

}