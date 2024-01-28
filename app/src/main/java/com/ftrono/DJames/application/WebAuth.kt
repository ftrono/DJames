package com.ftrono.DJames.application

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.ftrono.DJames.R
import java.util.Random
import kotlin.streams.asSequence
import android.webkit.WebSettings
import android.webkit.CookieManager
import android.widget.Toast


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

        //Clean ALL previously stored cookies & cache:
        webView!!.clearCache(true)
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()

        //Prepare auth request:
        val state = generateRandomString(16)

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
                                prefs.grantToken = token
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


    fun generateRandomString(length: Int): String {
        val source = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return Random().ints(length.toLong(), 0, source.length)
            .asSequence()
            .map(source::get)
            .joinToString("")
    }

}