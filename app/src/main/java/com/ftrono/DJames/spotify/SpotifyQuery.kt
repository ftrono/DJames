package com.ftrono.DJames.spotify

import android.content.Context
import android.util.Log
import com.ftrono.DJames.R
import com.ftrono.DJames.application.client
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.utilities.Utilities.HttpResponse
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Base64


class SpotifyQuery(private val context: Context) {
    private val TAG = SpotifyQuery::class.java.simpleName

    //REFRESHER:
    fun refreshAuth(context: Context) {
        //GET SPOTIFY DEV CREDENTIALS:
        val reader = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.spotify_credentials)))
        val credJson = JsonParser.parseReader(reader).asJsonObject
        val clientId = credJson.get("spotify_client").asString
        val clientSct = credJson.get("spotify_sct").asString

        //GET TOKENS:
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
            if (response.code == 200) {
                try {
                    //RESPONSE RECEIVED -> TOKENS:
                    Log.d(TAG, "Spotify refresh response received!")
                    var respJSON = JsonParser.parseString(response.body).asJsonObject
                    //SUCCESS:
                    var keySet = respJSON.keySet()
                    Log.d(TAG, keySet.toString())
                    prefs.spotifyToken = respJSON.get("access_token").asString
                    if (respJSON.has("refresh_token")) {
                        prefs.refreshToken =
                            respJSON.get("refresh_token").asString
                    }
                    Log.d(TAG, "TOKEN REFRESH SUCCESS: new Refresh tokens received!")
                } catch (e: Exception) {
                    Log.w(TAG, "REFRESH: ERROR IN RESPONSE PARSING: ", e)
                }
            } else {
                Log.d(TAG, "Token Refresh ERROR: not refreshed.")
            }
        }
    }


    //REQUEST BUILDER:
    fun buildRequest(
        url: String,
        type: String,
        jsonHeads: JsonObject,
        jsonBody: JsonObject? = null
    ): Request {

        //Build headers:
        var keySet = jsonHeads.keySet()
        var headersBuilder = Headers.Builder()
        for (key in keySet) {
            headersBuilder.add(key, jsonHeads.get(key).asString)
        }
        var headers = headersBuilder.build()

        //Build request:
        var request: Request? = null
        if (type == "get") {
            //GET:
            request = Request.Builder()
                .url(url)
                .headers(headers)
                .build()
        } else if (type == "post") {
            //POST:
            var body = jsonBody.toString().toRequestBody()
            request = Request.Builder()
                .url(url)
                .post(body)
                .headers(headers)
                .build()
        } else {
            //PUT:
            var body = jsonBody.toString().toRequestBody()
            request = Request.Builder()
                .url(url)
                .put(body)
                .headers(headers)
                .build()
        }
        return request
    }


    //MAIN QUERY PROCESS:
    fun querySpotify(
        type: String,
        url: String,
        jsonHeads: JsonObject,
        jsonBody: JsonObject? = null
    ): HttpResponse {
        var response = HttpResponse(
            code = -1,  // -1 to indicate failure
            body = ""
        )

        //FIRST QUERY:
        var request = buildRequest(type=type, url=url, jsonHeads=jsonHeads, jsonBody=jsonBody)

        runBlocking {
            response = utils.makeRequest(client, request)
            if (response.code == 200 || response.code == 204) {
                //FIRST QUERY SUCCESS:
                Log.d(TAG, "Spotify query: first response SUCCESS!")
                return@runBlocking response

            } else if (response.code == 401) {
                //Calling Refresh:
                Log.d(TAG, "Refreshing token...")
                refreshAuth(context)

                //SECOND QUERY:
                var jsonHeads2 = jsonHeads
                jsonHeads2.addProperty("Authorization", "Bearer ${prefs.spotifyToken}")
                request = buildRequest(type=type, url=url, jsonHeads=jsonHeads, jsonBody=jsonBody)
                response = utils.makeRequest(client, request)

                if (response.code == 200 || response.code == 204) {
                    //SECOND QUERY SUCCESS:
                    Log.d(TAG, "Spotify query: second response SUCCESS!")
                    return@runBlocking response

                } else {
                    Log.d(TAG, "Spotify query: Refresh query did not work. Exiting interpreter.")
                    return@runBlocking response
                }

            } else {
                Log.d(TAG, "Spotify query: Refresh query did not work. Exiting interpreter.")
                return@runBlocking response
            }
        }
        return response
    }

}