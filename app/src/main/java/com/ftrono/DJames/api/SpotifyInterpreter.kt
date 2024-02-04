package com.ftrono.DJames.api

import android.util.Log
import com.ftrono.DJames.application.client
import com.ftrono.DJames.application.clientId
import com.ftrono.DJames.application.clientSct
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.utils
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.runBlocking
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.Base64


class SpotifyInterpreter() {
    private val TAG = SpotifyInterpreter::class.java.simpleName

    fun refreshAuth() {
        //GET TOKENS
        var url = "https://accounts.spotify.com/api/token"
        val authStr = "$clientId:$clientSct"
        val encodedStr: String = Base64.getEncoder().encodeToString(authStr.toByteArray())

        //BUILD CLIENT:
        var formBody = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", prefs.refreshToken)
            .build()
        var headers = Headers.Builder()
            .add("content-type", "application/x-www-form-urlencoded")
            .add("Authorization", "Basic $encodedStr")
            .build()
        var request = Request.Builder()
            .url(url)
            .post(formBody)
            .headers(headers)
            .build()

        //CALL POST REQUEST:
        runBlocking {
            var response = utils.makeRequest(client, request)
            if (response != "") {
                try {
                    //RESPONSE RECEIVED -> TOKENS:
                    Log.d(TAG, "Spotify refresh response received!")
                    var respJSON = JsonParser.parseString(response).asJsonObject
                    var keySet = respJSON.keySet()
                    Log.d(TAG, keySet.toString())
                    prefs.spotifyToken = respJSON.get("access_token").asString
                    if (respJSON.has("refresh_token")) {
                        prefs.refreshToken =
                            respJSON.get("refresh_token").asString
                    }
                    Log.d(TAG, "TOKEN REFRESH SUCCESS: new Refresh tokens received!")

                } catch (e: Exception) {
                    Log.d(TAG, "REFRESH: ERROR IN RESPONSE PARSING: ", e)
                }
            } else {
                Log.d(TAG, "Token Refresh ERROR: not refreshed.")
            }
        }
    }

    fun querySpotify(results: Array<String>): JsonObject {
        //BUILD GET REQUEST:
        var baseURL = "https://api.spotify.com/v1/search"
        var returnJSON = JsonObject()

        //Query params:
        var queryParams = results.joinToString("&")
        val encodedParams: String = URLEncoder.encode(queryParams, "UTF-8")
        var url = baseURL + "?q=" + encodedParams + "&type=track"
        Log.d(TAG, url)

        var request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${prefs.spotifyToken}")
            .build()

        //GET:
        runBlocking {
            var response = utils.makeRequest(client, request)
            if (response != "") {
                //RESPONSE RECEIVED:
                Log.d(TAG, "First Search answer received. Analysing...")
                var respJSON = JsonParser.parseString(response).asJsonObject
                Log.d(TAG, respJSON.toString())

                //IF ERROR:
                if (respJSON.has("error")) {
                    var errorJSON = respJSON.get("error").asJsonObject
                    var status = errorJSON.get("status").asString.toInt()
                    Log.d(TAG, "First Search answer: received error ${status}.")
                    //401 -> token expired!
                    if (status == 401) {
                        Log.d(TAG, "Refreshing token...")
                        refreshAuth()
                        //Retry request:
                        request = Request.Builder()
                            .url(url)
                            .header("Authorization", "Bearer ${prefs.spotifyToken}")
                            .build()
                        response = utils.makeRequest(client, request)
                        if (response != "") {
                            //RESPONSE RECEIVED:
                            Log.d(TAG, "Second Search answer received. Analysing...")
                            respJSON = JsonParser.parseString(response).asJsonObject
                            Log.d(TAG, respJSON.toString())
                            //IF ERROR AGAIN:
                            if (respJSON.has("error")) {
                                Log.d(TAG, "Refresh query did not work. Exiting interpreter.")
                                return@runBlocking returnJSON
                            }
                        } else {
                            Log.d(TAG, "Empty refresh query. Exiting interpreter.")
                            return@runBlocking returnJSON
                        }
                    } else {
                        Log.d(TAG, "Query error. Exiting interpreter.")
                        return@runBlocking returnJSON
                    }
                }

                //SEARCH QUERY SUCCESS -> EXTRACT RESULTS:
                Log.d(TAG, "Spotify Search results received!")
                var tracks = respJSON.getAsJsonObject("tracks")
                var items = tracks.getAsJsonArray("items")
                Log.d(TAG, "Items: ${items}")

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
                returnJSON.add("song_name", JsonPrimitive(firstResult.get("name").asString))

                //Artist name:
                var artists = firstResult.getAsJsonArray("artists")
                var firstArtist = artists.get(0).asJsonObject
                returnJSON.add("artist_name", JsonPrimitive(firstArtist.get("name").asString))

                //Artist name:
                var album = firstResult.get("album").asJsonObject
                returnJSON.add("context_name", JsonPrimitive("Album: ${album.get("name").asString}"))

                Log.d(TAG, "Spotify Search results successfully processed!")
            } else {
                Log.d(TAG, "GET TRACK: EMPTY RESPONSE!")
            }
        }
        Log.d(TAG, "returnJSON: ${returnJSON}")
        return returnJSON
    }


    fun playInternally(resultJSON: JsonObject): Int {
        var ret = -1
        //BUILD PUT REQUEST:
        var url = "https://api.spotify.com/v1/me/player/play"
        var uris = JsonArray()
        uris.add(JsonPrimitive(resultJSON.get("uri").asString))

        //Offset:
        var position = 0
        var offset = JsonObject()
        offset.add("position", JsonPrimitive(position))

        var jsonBody = JsonObject()
        jsonBody.add("uris", uris)
        //jsonBody.add("offset", offset)
        jsonBody.add("position_ms", JsonPrimitive(0))

        var body = jsonBody.toString().toRequestBody()

        var headers = Headers.Builder()
            .add("Authorization", "Bearer ${prefs.spotifyToken}")
            .add("Content-Type", "application/json")
            .build()

        var request = Request.Builder()
            .url(url)
            .put(body)
            .headers(headers)
            .build()

        try {
            runBlocking {
                Log.d(TAG, request.toString())
                var response = utils.makeRequest(client, request)
                if (response == "") {
                    Log.d(TAG, "PLAY INTERNALLY SUCCESS!")
                    ret = 0
                } else {
                    Log.d(TAG, "PLAY INTERNALLY ERROR. Response: ${response}")
                    ret = -1
                }
            }
            return ret
        } catch (e: Exception) {
            Log.d(TAG, "PLAY INTERNALLY ERROR: ", e)
            return -1
        }
    }


    fun dispatchCall(resultsNLP: Array<String>): JsonObject {
        //DISPATCH SPOTIFY CALLS ACCORDING TO NLP RESULTS:
        var returnJSON = querySpotify(resultsNLP)
        return returnJSON
    }

}