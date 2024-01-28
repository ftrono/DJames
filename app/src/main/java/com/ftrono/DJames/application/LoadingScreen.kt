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

        val auth_token = ""
        val refresh_token = ""

        //GET TOKENS
//        val JSON = "application/json".toMediaType()
//        val bodyStr = "{'code': '${prefs.grantToken}', 'redirect_uri': '$redirectUri', 'grant_type': 'authorization_code'}"
        val url = "https://accounts.spotify.com/api/token"
        val authStr = "${clientId}:${clientSct}"
        val encodedStr: String = Base64.getEncoder().encodeToString(authStr.toByteArray())

        //BUILD POST REQUEST:
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .callTimeout(5, TimeUnit.SECONDS)
            .build()
        val formBody = FormBody.Builder()
            .add("code", prefs.grantToken)
            .add("redirect_uri", redirectUriOrig)
            .add("grant_type", "authorization_code")
            .build()
        val headers = Headers.Builder()
            .add("content-type", "application/x-www-form-urlencoded")
            .add("Authorization", "Basic $encodedStr")
            .build()
        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .headers(headers)
            .build()

        //CALL POST REQUEST:
        lifecycle.coroutineScope.launch {
            val response = makeRequest(client, request)
            Log.d(TAG, "RESPONSE BODY: $response")
            if (response != "") {
                //RESPONSE RECEIVED:
                try {
                    //TOKENS RECEIVED:
                    val respJSON = JsonParser.parseString(response).asJsonObject
                    prefs.spotifyToken = respJSON.get("access_token").toString()
                    prefs.refreshToken = respJSON.get("refresh_token").toString()
                    Log.d(TAG, "SUCCESS: Access & Refresh tokens received!")

                    //Send broadcast:
                    Intent().also { intent ->
                        intent.setAction(ACTION_LOGGED_IN)
                        sendBroadcast(intent)
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "ERROR IN RESPONSE PARSING: ", e)
                    Toast.makeText(applicationContext, "Authentication ERROR: not logged in.", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(applicationContext, "Authentication ERROR: not logged in.", Toast.LENGTH_LONG).show()
            }
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