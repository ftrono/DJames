package com.ftrono.DJames.application

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ftrono.DJames.R
import java.io.File
import java.net.URLEncoder
import java.util.Random
import kotlin.streams.asSequence


class WebAuth : AppCompatActivity() {

    private val TAG: String = WebAuth::class.java.getSimpleName()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.web_view)
        supportActionBar!!.title = "Spotify Login"
        webView = findViewById<View>(R.id.webview) as WebView
        webView!!.settings.javaScriptEnabled = true

        try {
            val dataDir: String = applicationContext.getPackageManager()
                .getPackageInfo(applicationContext.getPackageName(), 0).applicationInfo.dataDir
            File("$dataDir/app_webview").deleteRecursively()
        } catch (e: Exception) {
            Log.d(TAG, "Exception: ", e)
        }

        val clientId = "f525169dff664aa192ab51d2bbeb9767"
        val redirectUriOrig ="http://localhost:8888/callback"
        val redirectUri = URLEncoder.encode(redirectUriOrig, "UTF-8")
        val state = generateRandomString(16)
        val scope = "user-read-private%20user-read-email"

        //concatenate queryParams:
        val queryParams = "response_type=code&client_id=$clientId&scope=$scope&redirect_uri=$redirectUri&state=$state"

        webView!!.webViewClient = object: WebViewClient() {

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {

                var webUrl = webView!!.url
                supportActionBar!!.subtitle = webUrl

                var urlComps = webUrl!!.split("?")
                Log.d(TAG, "$urlComps")
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
                                //LOGGED IN:
                                Log.d(TAG, "Token received!")
                                prefs.spotifyToken = token
                                //Send broadcast:
                                Intent().also { intent ->
                                    intent.setAction(ACTION_LOGGED_IN)
                                    sendBroadcast(intent)
                                }
                                Toast.makeText(
                                    applicationContext,
                                    "LOGIN SUCCESS: Welcome to DJames!",
                                    Toast.LENGTH_LONG
                                ).show()
                                finish()
                            }
                        }
                    }
                }
                super.doUpdateVisitedHistory(view, url, isReload)
            }
        }
        webView!!.loadUrl("https://accounts.spotify.com/authorize?$queryParams")
    }


    fun generateRandomString(length: Int): String {
        val source = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return Random().ints(length.toLong(), 0, source.length)
            .asSequence()
            .map(source::get)
            .joinToString("")
    }

    companion object {
        private var webView: WebView? = null
    }
}