package com.ftrono.DJames.spotify

import android.content.Context
import android.util.Log
import com.ftrono.DJames.R
import com.ftrono.DJames.application.client
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.utilities.Utilities
import com.google.gson.JsonArray
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
    private var utils = Utilities()

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
            if (response != "") {
                try {
                    //RESPONSE RECEIVED -> TOKENS:
                    Log.d(TAG, "Spotify refresh response received!")
                    var respJSON = JsonParser.parseString(response).asJsonObject
                    if (!respJSON.has("error")) {
                        //SUCCESS:
                        var keySet = respJSON.keySet()
                        Log.d(TAG, keySet.toString())
                        prefs.spotifyToken = respJSON.get("access_token").asString
                        if (respJSON.has("refresh_token")) {
                            prefs.refreshToken =
                                respJSON.get("refresh_token").asString
                        }
                        Log.d(TAG, "TOKEN REFRESH SUCCESS: new Refresh tokens received!")
                    } else {
                        //ERROR RESPONSE:
                        Log.d(TAG, "REFRESH: RESPONSE ERROR: ${response}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "REFRESH: ERROR IN RESPONSE PARSING: ", e)
                }
            } else {
                Log.d(TAG, "Token Refresh ERROR: not refreshed.")
            }
        }
    }


    //REQUEST BUILDER:
    fun buildRequest(url: String, type: String, jsonHeads: JsonObject, jsonBody: JsonObject? = null, formBody: FormBody? = null): Request {
        var request: Request? = null

        //Build headers:
        var keySet = jsonHeads.keySet()
        var headersBuilder = Headers.Builder()
        for (key in keySet) {
            headersBuilder.add(key, jsonHeads.get(key).asString)
        }
        var headers = headersBuilder.build()

        if (type == "get") {
            //GET:
            request = Request.Builder()
                .url(url)
                .headers(headers)
                .build()
        } else if (type == "post") {
            if (formBody == null) {
                //POST WITH JSON BODY:
                var body = jsonBody.toString().toRequestBody()
                request = Request.Builder()
                    .url(url)
                    .post(body)
                    .headers(headers)
                    .build()
            } else {
                //POST WITH FORMBODY:
                var body = formBody
                request = Request.Builder()
                    .url(url)
                    .post(body)
                    .headers(headers)
                    .build()
            }
        } else {
            if (formBody == null) {
                //PUT WITH JSON BODY:
                var body = jsonBody.toString().toRequestBody()
                request = Request.Builder()
                    .url(url)
                    .put(body)
                    .headers(headers)
                    .build()
            } else {
                //PUT WITH FORMBODY:
                var body = formBody
                request = Request.Builder()
                    .url(url)
                    .put(body)
                    .headers(headers)
                    .build()
            }
        }
        return request
    }


    //MAIN QUERY PROCESS (JSON OUTPUT):
    fun querySpotify(type: String, url: String, jsonHeads: JsonObject, jsonBody: JsonObject? = null, formBody: FormBody? = null): JsonObject {
        /*RETURN:
            - empty JSON if fail
            - respJSON if success
         */
        var returnJSON = JsonObject()
        //FIRST GET REQUEST:
        var request = buildRequest(type=type, url=url, jsonHeads=jsonHeads, jsonBody=jsonBody, formBody=formBody)

        runBlocking {
            var response = utils.makeRequest(client, request)
            if (response != "") {
                //RESPONSE RECEIVED:
                Log.d(TAG, "First Search answer received. Analysing...")
                var respJSON = JsonParser.parseString(response).asJsonObject
                //Log.d(TAG, response)

                //IF ERROR:
                if (respJSON.has("error")) {
                    var errorJSON = respJSON.get("error").asJsonObject
                    var status = errorJSON.get("status").asString.toInt()
                    Log.d(TAG, "First Search answer: received error ${status}.")

                    //401 -> token expired!
                    if (status == 401) {
                        //Calling Refresh:
                        Log.d(TAG, "Refreshing token...")
                        refreshAuth(context)

                        //SECOND GET REQUEST:
                        var jsonHeads2 = jsonHeads
                        jsonHeads2.addProperty("Authorization", "Bearer ${prefs.spotifyToken}")
                        request = buildRequest(type=type, url=url, jsonHeads=jsonHeads, jsonBody=jsonBody, formBody=formBody)
                        response = utils.makeRequest(client, request)

                        if (response != "") {
                            //RESPONSE RECEIVED:
                            Log.d(TAG, "Second Search answer received. Analysing...")
                            respJSON = JsonParser.parseString(response).asJsonObject
                            //Log.d(TAG, respJSON.toString())

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
                returnJSON = respJSON
                return@runBlocking returnJSON

            } else {
                Log.d(TAG, "QUERY: EMPTY RESPONSE!")
                return@runBlocking returnJSON
            }
        }
        return returnJSON
    }


    //MAIN QUERY PROCESS (ARRAY OUTPUT):
    fun querySpotifyArray(type: String, url: String, jsonHeads: JsonObject, jsonBody: JsonObject? = null, formBody: FormBody? = null): JsonArray {
        /*RETURN:
            - empty JsonArray if fail
            - respArray if success
         */
        var returnArray = JsonArray()
        //FIRST GET REQUEST:
        var request = buildRequest(type=type, url=url, jsonHeads=jsonHeads, jsonBody=jsonBody, formBody=formBody)

        runBlocking {
            var response = utils.makeRequest(client, request)
            if (response != "") {
                //RESPONSE RECEIVED:
                Log.d(TAG, "First Search answer received. Analysing...")
                try {
                    var respArray = JsonParser.parseString(response).asJsonArray
                    //SEARCH QUERY SUCCESS -> EXTRACT RESULTS:
                    Log.d(TAG, "Spotify Search results received!")
                    returnArray = respArray
                    return@runBlocking returnArray
                } catch (e: Exception) {
                    //IF ERROR:
                    var respJSON = JsonParser.parseString(response).asJsonObject
                    var errorJSON = respJSON.get("error").asJsonObject
                    var status = errorJSON.get("status").asString.toInt()
                    Log.w(TAG, "First Search answer: received error ${status}.")

                    //401 -> token expired!
                    if (status == 401) {
                        //Calling Refresh:
                        Log.d(TAG, "Refreshing token...")
                        refreshAuth(context)

                        //SECOND GET REQUEST:
                        var jsonHeads2 = jsonHeads
                        jsonHeads2.addProperty("Authorization", "Bearer ${prefs.spotifyToken}")
                        request = buildRequest(type=type, url=url, jsonHeads=jsonHeads, jsonBody=jsonBody, formBody=formBody)
                        response = utils.makeRequest(client, request)

                        if (response != "") {
                            //RESPONSE RECEIVED:
                            Log.d(TAG, "Second Search answer received. Analysing...")
                            try {
                                var respArray = JsonParser.parseString(response).asJsonArray
                                //SEARCH QUERY SUCCESS -> EXTRACT RESULTS:
                                Log.d(TAG, "Spotify Search results received!")
                                returnArray = respArray
                                return@runBlocking returnArray
                            } catch (e: Exception) {
                                //IF ERROR AGAIN:
                                respJSON = JsonParser.parseString(response).asJsonObject
                                Log.d(TAG, respJSON.toString())
                                Log.w(TAG, "Refresh query did not work. Exiting interpreter.")
                                return@runBlocking returnArray
                            }
                        } else {
                            Log.d(TAG, "Empty refresh query. Exiting interpreter.")
                            return@runBlocking returnArray
                        }
                    } else {
                        Log.d(TAG, "Query error. Exiting interpreter.")
                        return@runBlocking returnArray
                    }
                }
            } else {
                Log.d(TAG, "QUERY: EMPTY RESPONSE!")
                return@runBlocking returnArray
            }
        }
        return returnArray
    }

}