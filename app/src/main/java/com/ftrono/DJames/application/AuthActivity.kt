package com.ftrono.DJames.application

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.ftrono.DJames.R
import com.google.gson.JsonParser
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.ResponseTypeValues
import java.io.BufferedReader
import java.io.InputStreamReader


class AuthActivity: ComponentActivity() {

    private val TAG: String = AuthActivity::class.java.getSimpleName()
    private lateinit var authService: AuthorizationService
    private lateinit var authLauncher: ActivityResultLauncher<Intent>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize AuthorizationService
        authService = AuthorizationService(this)

        // Register the ActivityResultLauncher
        authLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleAuthResult(result)
        }
        startAuthorizationFlow()
    }


    private fun startAuthorizationFlow() {
        //GET SPOTIFY DEV CREDENTIALS:
        val reader = BufferedReader(InputStreamReader(this@AuthActivity.resources.openRawResource(R.raw.spotify_credentials)))
        val credJson = JsonParser.parseReader(reader).asJsonObject
        val clientId = credJson.get("spotify_client").asString

        //BUILD AUTH REQUEST:
        val authRequest = AuthorizationRequest.Builder(
            spotifyAuthConfig,
            clientId,
            ResponseTypeValues.CODE, // Spotify uses Authorization Code flow
            Uri.parse(redirectUri)
        )
            //Spotify Scopes:
            .setScopes(
                "user-read-private",   //Read access to user’s email address
                "user-read-email",   //Read access to user’s subscription details (type of user account)
                "user-read-playback-state",   //Read access to a user’s player state (devices, player state, track)
                "user-modify-playback-state",   //Write access to a user’s playback state (add to queue)
                "user-read-currently-playing",   //Read your currently playing content (track / queue)
                "streaming",   //Play content and control playback on your other devices
                "playlist-read-private",   //Read access to user's private playlists
                "playlist-read-collaborative",   //Include collaborative playlists when requesting a user's playlists
                "user-follow-modify",   //Write/delete access to the list of artists and other users that the user follows
                "user-follow-read",   //Read access to the list of artists and other users that the user follows
                "user-top-read",   //Read access to a user's top artists and tracks
                "user-read-recently-played",   //Read access to a user’s recently played tracks
                "user-library-read",   //Access saved content (tracks, albums)
                "user-library-modify"   //Manage saved content (tracks, albums)
                //"playlist-modify-private",   //Write access to a user's private playlists
                //"playlist-modify-public",   //Write access to a user's public playlists
                //"app-remote-control",   //Android only: communicate with the Spotify app on your device
            )
            .build()

        val authIntent = authService.getAuthorizationRequestIntent(authRequest)
        authLauncher.launch(authIntent) // Use the launcher instead of startActivityForResult
    }


    private fun handleAuthResult(result: ActivityResult) {
        Log.d(TAG, "AUTH REDIRECT TRIGGERED!")
        if (result.resultCode == RESULT_OK && result.data != null) {
            val response = AuthorizationResponse.fromIntent(result.data!!)
            val error = AuthorizationException.fromIntent(result.data)
            if (response != null) {
                // Authorization successful, exchange the code for a token
                exchangeAuthorizationCode(response)
            } else {
                // Handle error
                Log.w(TAG, "AUTH ERROR!")
                Log.w(TAG, error.toString())
                Toast.makeText(applicationContext, "Authentication ERROR: not logged in.", Toast.LENGTH_LONG).show()
                finish()
            }
        } else {
            Log.w(TAG, "AUTH RESULT: Result not OK or data is null!")
        }
    }


    private fun exchangeAuthorizationCode(response: AuthorizationResponse) {
        val tokenRequest = response.createTokenExchangeRequest()
        authService.performTokenRequest(tokenRequest) { tokenResponse, ex ->
            if (tokenResponse != null) {
                try {
                    spotTempToken = tokenResponse.accessToken!!
                    refrTempToken = tokenResponse.refreshToken!!
                    Log.d(TAG, "AUTH SUCCESS: Access & Refresh tokens received! Started Spotify.me main.")
                    // Log.d(TAG, "AccessToken: $spotTempToken")
                    // Log.d(TAG, "RefreshToken: $refrTempToken")
                    showLoggingIn.postValue(true)
                } catch (e: Exception) {
                    Log.w(TAG, "ERROR:  AUTH: Cannot call Spotify.me main!", e)
                }
            } else {
                // Handle token exchange failure
                Log.w(TAG, "AUTH: TokenExchangeError!", ex)
            }
        }
        finish()
    }

//    override fun onResume() {
//        super.onResume()
//        finish()
//    }

    override fun onDestroy() {
        super.onDestroy()
        authService.dispose() // Clean up AuthorizationService
    }

}