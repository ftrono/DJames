package com.ftrono.DJames.application

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.ftrono.DJames.R
import android.webkit.WebSettings
import android.webkit.CookieManager
import android.widget.Toast
import com.ftrono.DJames.utilities.Utilities


class WebAuth : AppCompatActivity() {

    private val TAG: String = WebAuth::class.java.getSimpleName()
    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.web_view)
        supportActionBar!!.title = "Spotify Login"

        //Load WebView:
        webView = findViewById<View>(R.id.webview) as WebView
        webView!!.settings.javaScriptEnabled = true
        webView!!.settings.setCacheMode(WebSettings.LOAD_NO_CACHE)

        //Spotify Scopes:
        val scopes = arrayOf(
            "user-read-private",   //Read access to user’s email address
            "user-read-email",   //Read access to user’s subscription details (type of user account)
            "user-read-playback-state",   //Read access to a user’s player state (devices, player state, track)
            "user-modify-playback-state",   //Write access to a user’s playback state (add to queue)
            "user-read-currently-playing",   //Read your currently playing content (track / queue)
            "app-remote-control",   //Android only: communicate with the Spotify app on your device
            "streaming",   //Play content and control playback on your other devices
            "playlist-read-private",   //Read access to user's private playlists
            "playlist-read-collaborative",   //Include collaborative playlists when requesting a user's playlists
            "playlist-modify-private",   //Write access to a user's private playlists
            "playlist-modify-public",   //Write access to a user's public playlists
            "user-follow-modify",   //Write/delete access to the list of artists and other users that the user follows
            "user-follow-read",   //Read access to the list of artists and other users that the user follows
            "user-top-read",   //Read access to a user's top artists and tracks
            "user-read-recently-played",   //Read access to a user’s recently played tracks
            "user-library-read",   //Access saved content (tracks, albums)
            "user-library-modify"   //Manage saved content (tracks, albums)
        )

        val scope = scopes.joinToString("%20", "", "")

        //Prepare auth request:
        var utils = Utilities()
        val state = utils.generateRandomString(16)

        //concatenate queryParams:
        val queryParams = "response_type=code&client_id=$clientId&scope=$scope&redirect_uri=$redirectUri&state=$state"

        //URL params extraction:
        webView!!.webViewClient = object: WebViewClient() {

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {

                var webUrl = webView!!.url
                supportActionBar!!.subtitle = webUrl

                var urlComps = webUrl!!.split("?")
                //IF CALLBACK -> get Token from callback URL:
                if (urlComps[0] == redirectUriOrig) {
                    //URL form: code=<token>&state=<state>
                    var params = urlComps[1].split("=")
                    if (params[0] == "code") {
                        //Check "state":
                        var state2 = params[2]
                        if (state2 == state) {
                            //Get token:
                            var token = params[1].split("&")[0]
                            if (token != "") {
                                //GRANT TOKEN RECEIVED:
                                Log.d(TAG, "Grant token received!")
                                grantToken = token.replace("\"", "")
                                webView!!.loadUrl("")
                                finish()
                                //Start loading screen:
                                val intent1 = Intent(applicationContext, LoadingScreen::class.java)
                                startActivity(intent1)
                            } else {
                                Log.d(TAG, "Authentication ERROR: no token received.")
                                Toast.makeText(applicationContext, "Authentication ERROR. Please try again!", Toast.LENGTH_LONG).show()
                                finish()
                            }
                        } else {
                            Log.d(TAG, "Authentication ERROR: state mismatch.")
                            Toast.makeText(applicationContext, "Authentication ERROR. Please try again!", Toast.LENGTH_LONG).show()
                            finish()
                        }
                    } else if (params[0] == "error") {
                        //Get error message:
                        var errorMessage = params[1].split("&")[0]
                        Log.d(TAG, "Authentication ERROR: $errorMessage")
                        Toast.makeText(applicationContext, "Authentication ERROR. Please try again!", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
                super.doUpdateVisitedHistory(view, url, isReload)
            }
        }
        webView!!.loadUrl("https://accounts.spotify.com/authorize?$queryParams")
    }


    override fun onDestroy() {
        super.onDestroy()
        webView!!.clearCache(true)
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        webView = null
    }

}