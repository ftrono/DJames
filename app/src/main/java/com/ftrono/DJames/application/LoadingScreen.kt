package com.ftrono.DJames.application

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ftrono.DJames.R
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.Response
import okhttp3.Callback
import okhttp3.Call
import java.io.IOException
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.launch
import com.google.gson.JsonParser


class LoadingScreen: AppCompatActivity() {

    private val TAG: String = LoadingScreen::class.java.getSimpleName()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.loading_screen)

        //GET TOKENS
        var url = "https://accounts.spotify.com/api/token"
        val authStr = "${clientId}:${clientSct}"
        val encodedStr: String = Base64.getEncoder().encodeToString(authStr.toByteArray())
        var spotToken = ""
        var refrToken = ""

        //BUILD CLIENT:
        var formBody = FormBody.Builder()
            .add("code", grantToken)
            .add("redirect_uri", redirectUriOrig)
            .add("grant_type", "authorization_code")
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
        lifecycle.coroutineScope.launch {
            var response = makeRequest(client, request)
            if (response != "") {
                try {
                    //RESPONSE RECEIVED -> TOKENS:
                    var respJSON = JsonParser.parseString(response).asJsonObject
                    spotToken = respJSON.get("access_token").toString().replace("\"", "")
                    refrToken = respJSON.get("refresh_token").toString().replace("\"", "")
                    Log.d(TAG, "AUTH SUCCESS: Access & Refresh tokens received!")

                    //Get user profile data:
                    //BUILD POST REQUEST:
                    url = "https://api.spotify.com/v1/me"
                    request = Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer $spotToken")
                        .build()

                    //GET:
                    response = makeRequest(client, request)
                    if (response != "") {
                        try {
                            //RESPONSE RECEIVED -> USER'S PROFILE DATA:
                            respJSON = JsonParser.parseString(response).asJsonObject
                            var product = respJSON.get("product").toString().replace("\"", "")
                            //User must be PREMIUM:
                            if (product == "premium" || product == "duo" || product == "family") {
                                //SUCCESS!
                                prefs.spotifyToken = spotToken
                                prefs.refreshToken = refrToken
                                prefs.userName = respJSON.get("display_name").toString().replace("\"", "")
                                //Send broadcast:
                                Intent().also { intent ->
                                    intent.setAction(ACTION_LOGGED_IN)
                                    sendBroadcast(intent)
                                }
                            } else {
                                Log.d(TAG, "USER TYPE: $product")
                                Toast.makeText(applicationContext, "ERROR: to use DJames, you need to be a Spotify Premium user! :(", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Log.d(TAG, "Profile parsing error: ", e)
                            Toast.makeText(applicationContext, "Authentication ERROR: not logged in.", Toast.LENGTH_LONG).show()
                        }
                    }

                } catch (e: Exception) {
                    Log.d(TAG, "ERROR IN RESPONSE PARSING: ", e)
                    Toast.makeText(applicationContext, "Authentication ERROR: not logged in.", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(applicationContext, "Authentication ERROR: not logged in.", Toast.LENGTH_LONG).show()
            }
            grantToken = ""
            finish()
        }

    }


    suspend fun makeRequest(client: OkHttpClient, request: Request): String = suspendCoroutine { continuation ->
        client.newCall(request).enqueue(object: Callback {
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response.body!!.string())
            }

            override fun onFailure(call: Call, e: IOException) {
                continuation.resume("")
                Log.d(TAG, "RESPONSE ERROR: ", e)
            }
        })
    }

    override fun onBackPressed() {
        return
    }



}