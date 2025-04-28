package com.ftrono.DJames.be.spotify

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.be.models.HttpResponse
import com.google.gson.JsonArray
import com.google.gson.JsonObject


class SpotifyCalls(private val context: Context) {
    private val TAG = SpotifyPlayer::class.java.simpleName
    private var query = SpotifyQuery(context)


    //SAVE CURRENT TRACK:
    fun saveTrackRequest(ids: JsonArray): Int {
        var url = "https://api.spotify.com/v1/me/tracks"

        //Body:
        var jsonBody = JsonObject()
        jsonBody.add("ids", ids)

        //Headers:
        var jsonHeads = JsonObject()
        jsonHeads.addProperty("Authorization", "Bearer ${prefs.spotifyToken}")
        jsonHeads.addProperty("Content-Type", "application/json")

        //PUT REQUEST:
        var response = query.querySpotify(type = "put", url = url, jsonHeads = jsonHeads, jsonBody = jsonBody)
        Log.d(TAG, "saveTrackRequest: response code: ${response.code}")
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


    //GET SPOTIFY ARTIST:
    fun getSpotifyArtist(id: String): HttpResponse {
        var url = "https://api.spotify.com/v1/artists/$id"

        //Headers:
        var jsonHeads = JsonObject()
        jsonHeads.addProperty("Authorization", "Bearer ${prefs.spotifyToken}")

        //GET REQUEST:
        var response = query.querySpotify(type = "get", url = url, jsonHeads = jsonHeads)
        Log.d(TAG, "getSpotifyArtist: response code: ${response.code}")
        return response
    }


    //GET SPOTIFY PLAYLIST:
    fun getSpotifyPlaylist(id: String, detailsOnly: Boolean): HttpResponse {
        var url = "https://api.spotify.com/v1/playlists/$id"
        if (detailsOnly) {
            url += "?fields=name%2Cimages%2Cowner%2Csnapshot_id"
        }

        //Headers:
        var jsonHeads = JsonObject()
        jsonHeads.addProperty("Authorization", "Bearer ${prefs.spotifyToken}")

        //GET REQUEST:
        var response = query.querySpotify(type = "get", url = url, jsonHeads = jsonHeads)
        Log.d(TAG, "getSpotifyPlaylist: response code: ${response.code}")
        return response
    }

}