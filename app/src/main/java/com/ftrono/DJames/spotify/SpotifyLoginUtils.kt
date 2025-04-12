package com.ftrono.DJames.spotify

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.navigation.NavController
import com.ftrono.DJames.application.ACTION_TOASTER
import com.ftrono.DJames.application.client
import com.ftrono.DJames.application.genderMaleState
import com.ftrono.DJames.application.lastNavRoute
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.refrTempToken
import com.ftrono.DJames.application.showLoggingIn
import com.ftrono.DJames.application.spotTempToken
import com.ftrono.DJames.application.spotUserImageState
import com.ftrono.DJames.application.spotifyLoggedIn
import com.ftrono.DJames.application.userNicknameState
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.ui.navigateTo
import com.ftrono.DJames.ui.theme.NavigationItem
import com.google.gson.JsonParser
import kotlinx.coroutines.launch
import okhttp3.Request
import java.lang.Thread.sleep


class SpotifyLoginUtils {
    private val TAG = SpotifyLoginUtils::class.java.simpleName

    fun getSpotifyUserData(
        mContext: Context,
        navController: NavController,
        scope: LifecycleCoroutineScope
    ) {
        //Get user profile data:
        //BUILD GET REQUEST:
        val url = "https://api.spotify.com/v1/me"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $spotTempToken")
            .build()

        //GET:
        scope.launch {
            Log.d(TAG, "Performing Spotify.me request...")
            val meResponse = utils.makeRequest(client, request)
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
                        prefs.refreshToken = refrTempToken
                        prefs.spotUserName = respJSON.get("display_name").asString
                        prefs.spotUserId = respJSON.get("id").asString
                        prefs.spotUserEMail = respJSON.get("email").asString
                        prefs.spotCountry = respJSON.get("country").asString
                        prefs.userNickname = utils.cleanString(respJSON.get("display_name").asString)
                        prefs.genderMale = true
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
                        Toast.makeText(mContext, "SUCCESS: DJames is now LOGGED IN to your Spotify!", Toast.LENGTH_LONG).show()
                        spotifyLoggedIn.postValue(true)
                    } else {
                        Log.w(TAG, "Spotify.me: PROBLEM -> user not enabled! USER TYPE: $product")
                        Toast.makeText(mContext, "ERROR: to use DJames, you need to be a Spotify Premium user! :(", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Profile parsing error: ", e)
                    Toast.makeText(mContext, "Authentication ERROR: not logged in.", Toast.LENGTH_LONG).show()
                }
            }
            spotTempToken = ""
            refrTempToken = ""
            //States:
            showLoggingIn.postValue(false)
            genderMaleState.postValue(true)
            spotUserImageState.postValue(prefs.spotUserImage)
            userNicknameState.postValue(prefs.userNickname)
            //TOAST -> Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_TOASTER)
                intent.putExtra("toastText", "Logged in! Please pick a nickname for you!")
                mContext.sendBroadcast(intent)
            }
            //Navigate to Settings:
            val curNavRoute = NavigationItem.Settings.route
            navigateTo(navController, curNavRoute)
            lastNavRoute = curNavRoute
        }
    }


    //LOGOUT:
    fun logout(
        context: Context,
        navController: NavController,
        settingsOpenState: Boolean
    ) {
        //Delete tokens & user details:
        spotifyLoggedIn.postValue(false)
        prefs.spotifyToken = ""
        prefs.refreshToken = ""
        prefs.spotUserId = ""
        prefs.spotUserName = ""
        prefs.spotUserEMail = ""
        prefs.spotUserImage = ""
        prefs.spotCountry = ""
        prefs.userNickname = ""
        prefs.genderMale = true
        prefs.nlpUserId = utils.generateRandomString(12)
        genderMaleState.postValue(true)
        spotUserImageState.postValue("")
        userNicknameState.postValue("")
        //utils.deleteUserCache()
        Toast.makeText(context, "Djames is now LOGGED OUT from your Spotify.", Toast.LENGTH_LONG).show()
        //Navigate to Home:
        val curNavRoute = NavigationItem.Home.route
        if (curNavRoute == lastNavRoute && (settingsOpenState)) {
            navController.popBackStack()
        } else {
            navigateTo(navController, curNavRoute)
        }
        lastNavRoute = curNavRoute
    }


}