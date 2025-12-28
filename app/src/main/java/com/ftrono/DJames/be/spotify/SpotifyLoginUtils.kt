package com.ftrono.DJames.be.spotify

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.navigation.NavController
import com.ftrono.DJames.application.ACTION_TOASTER
import com.ftrono.DJames.application.lastNavRoute
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.defaultHttpTimeout
import com.ftrono.DJames.application.refrTempToken
import com.ftrono.DJames.application.showLoggingIn
import com.ftrono.DJames.application.spotTempToken
import com.ftrono.DJames.application.spotUserImageState
import com.ftrono.DJames.application.spotifyLoggedIn
import com.ftrono.DJames.application.spotUserName
import com.ftrono.DJames.application.userNicknameUI
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.utils.HttpClient
import com.ftrono.DJames.ui.navigation.navigateTo
import com.ftrono.DJames.ui.theme.NavigationItem
import com.google.gson.JsonParser
import kotlinx.coroutines.launch
import okhttp3.Request
import java.lang.Thread.sleep


class SpotifyLoginUtils {
    private val TAG = this::class.java.simpleName

    fun getSpotifyUserData(
        context: Context,
        navController: NavController,
        scope: LifecycleCoroutineScope
    ) {
        //Get user profile data:
        //BUILD GET REQUEST:
        val httpClient = HttpClient()
        val client = httpClient.getClient(defaultHttpTimeout)
        val url = "https://api.spotify.com/v1/me"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $spotTempToken")
            .build()

        //GET:
        scope.launch {
            Log.d(TAG, "Performing Spotify.me request...")
            val meResponse = httpClient.makeRequest(client, request)
            if (meResponse.code == 200) {
                Log.d(TAG, "Spotify.me: answer received!")
                try {
                    //RESPONSE RECEIVED -> USER'S PROFILE DATA:
                    val respJSON = JsonParser.parseString(meResponse.body).asJsonObject
                    var product = respJSON.get("product").asString
                    //User must be PREMIUM:
                    if (product == "premium" || product == "duo" || product == "family") {
                        //Log.d(TAG, response)
                        //SUCCESS!
                        prefs.spotifyToken = spotTempToken
                        prefs.spotRefreshToken = refrTempToken
                        prefs.spotUserName = respJSON.get("display_name").asString
                        spotUserName.postValue(respJSON.get("display_name").asString)
                        prefs.spotUserId = respJSON.get("id").asString
                        prefs.spotUserEMail = respJSON.get("email").asString
                        prefs.spotCountry = respJSON.get("country").asString
                        //Spotify profile image:
                        try {
                            val images = respJSON.getAsJsonArray("images")
                            val firstImage = images.get(0).asJsonObject
                            prefs.spotUserImage = firstImage.get("url").asString
                        } catch (e: Exception) {
                            Log.w(TAG, "Unable to retrieve user image: ", e)
                            prefs.spotUserImage = ""
                        }
                        //Generate NLP user ID:
                        prefs.nlpUserId = utils.generateRandomString(30, numOnly = true)

                        sleep(1000)
                        Log.d(TAG, "Spotify.me: success! User is enabled.")
                        Toast.makeText(context, "SUCCESS: DJames is now LOGGED IN to your Spotify!", Toast.LENGTH_LONG).show()
                        spotifyLoggedIn.postValue(true)
                    } else {
                        Log.w(TAG, "Spotify.me: PROBLEM -> user not enabled! USER TYPE: $product")
                        Toast.makeText(context, "ERROR: to use DJames, you need to be a Spotify Premium user! :(", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Profile parsing error: ", e)
                    Toast.makeText(context, "Authentication ERROR: not logged in.", Toast.LENGTH_LONG).show()
                }
            }
            spotTempToken = ""
            refrTempToken = ""
            //States:
            showLoggingIn.postValue(false)
            spotUserImageState.postValue(prefs.spotUserImage)
            //TOAST -> Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_TOASTER)
                intent.putExtra("toastText", "Logged in to Spotify!")
                context.sendBroadcast(intent)
            }
            //Navigate to Accounts:
            val curNavRoute = NavigationItem.Accounts.route
            navigateTo(navController, curNavRoute)
            lastNavRoute = curNavRoute
        }
    }


    //LOGOUT:
    fun logout(context: Context) {
        //Delete tokens & user details:
        spotifyLoggedIn.postValue(false)
        prefs.spotifyToken = ""
        prefs.spotRefreshToken = ""
        prefs.spotUserId = ""
        prefs.spotUserName = ""
        prefs.spotUserEMail = ""
        prefs.spotUserImage = ""
        prefs.spotCountry = ""
        spotUserName.postValue("")
        spotUserImageState.postValue("")
        //utils.deleteUserCache(context)
        Toast.makeText(context, "Djames is now LOGGED OUT from your Spotify.", Toast.LENGTH_LONG).show()
    }


}