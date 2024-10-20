package com.ftrono.DJames.application

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ftrono.DJames.R
import okhttp3.Request
import okhttp3.Headers
import java.util.Base64
import androidx.lifecycle.coroutineScope
import com.ftrono.DJames.utilities.Utilities
import kotlinx.coroutines.launch
import com.google.gson.JsonParser
import okhttp3.FormBody
import java.io.BufferedReader
import java.io.InputStreamReader


class LoadingScreen: AppCompatActivity() {

    private val TAG: String = LoadingScreen::class.java.getSimpleName()
    private var utils = Utilities()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.loading_screen)

        //GET SPOTIFY DEV CREDENTIALS:
        val reader = BufferedReader(InputStreamReader(applicationContext.resources.openRawResource(R.raw.spotify_credentials)))
        val credJson = JsonParser.parseReader(reader).asJsonObject
        val clientId = credJson.get("spotify_client").asString
        val clientSct = credJson.get("spotify_sct").asString

        //GET TOKENS:
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
            var response = utils.makeRequest(client, request)
            if (response != "") {
                try {
                    //RESPONSE RECEIVED -> TOKENS:
                    var respJSON = JsonParser.parseString(response).asJsonObject
                    if (!respJSON.has("error")) {
                        spotToken = respJSON.get("access_token").asString
                        refrToken = respJSON.get("refresh_token").asString
                        Log.d(TAG, "AUTH SUCCESS: Access & Refresh tokens received!")

                        //Get user profile data:
                        //BUILD GET REQUEST:
                        url = "https://api.spotify.com/v1/me"
                        request = Request.Builder()
                            .url(url)
                            .header("Authorization", "Bearer $spotToken")
                            .build()

                        //GET:
                        response = utils.makeRequest(client, request)
                        if (response != "") {
                            try {
                                //RESPONSE RECEIVED -> USER'S PROFILE DATA:
                                respJSON = JsonParser.parseString(response).asJsonObject
                                var product = respJSON.get("product").asString
                                //User must be PREMIUM:
                                if (product == "premium" || product == "duo" || product == "family") {
                                    //Log.d(TAG, response)
                                    //SUCCESS!
                                    prefs.spotifyToken = spotToken
                                    prefs.refreshToken = refrToken
                                    prefs.spotUserId = respJSON.get("id").asString
                                    prefs.spotUserName = respJSON.get("display_name").asString
                                    prefs.spotUserEMail = respJSON.get("email").asString
                                    prefs.spotCountry = respJSON.get("country").asString
                                    try {
                                        var images = respJSON.getAsJsonArray("images")
                                        var firstImage = images.get(0).asJsonObject
                                        prefs.spotUserImage = firstImage.get("url").asString
                                    } catch (e: Exception) {
                                        Log.w(TAG, "Unable to retrieve user image: ", e)
                                        prefs.spotUserImage = ""
                                    }

                                    //Generate NLP user ID:
                                    prefs.nlpUserId = utils.generateRandomString(30, numOnly = true)

                                    //Send broadcasts:
                                    Intent().also { intent ->
                                        intent.setAction(ACTION_MAIN_LOGGED_IN)
                                        sendBroadcast(intent)
                                    }
                                } else {
                                    Log.d(TAG, "USER TYPE: $product")
                                    Toast.makeText(
                                        applicationContext,
                                        "ERROR: to use DJames, you need to be a Spotify Premium user! :(",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Profile parsing error: ", e)
                                Toast.makeText(
                                    applicationContext,
                                    "Authentication ERROR: not logged in.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } else {
                        Log.d(TAG, "ERROR IN RESPONSE PARSING: ${response}")
                        Toast.makeText(applicationContext, "Authentication ERROR: not logged in.", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "ERROR IN RESPONSE PARSING: ", e)
                    Toast.makeText(applicationContext, "Authentication ERROR: not logged in.", Toast.LENGTH_LONG).show()
                }
            } else {
                Log.d(TAG, "EMPTY RESPONSE!")
                Toast.makeText(applicationContext, "Authentication ERROR: not logged in.", Toast.LENGTH_LONG).show()
            }
            grantToken = ""
            finish()
        }

    }

    override fun onBackPressed() {
        return
    }

}