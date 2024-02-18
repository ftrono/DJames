package com.ftrono.DJames.application

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import com.ftrono.DJames.R
import com.google.gson.JsonObject
import com.squareup.picasso.Picasso
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt


class FakeLockScreen: AppCompatActivity() {

    private val TAG: String = FakeLockScreen::class.java.getSimpleName()

    //Views:
    private var dateView: TextView? = null
    private var clockView: TextView? = null
    private var exitButton: View? = null

    //Parameters:
    private var density: Float = 0F
    private var now: LocalDateTime? = null
    private val dateFormat = DateTimeFormatter.ofPattern("E, dd MMM")
    private val hourFormat = DateTimeFormatter.ofPattern("HH")
    private val minsFormat = DateTimeFormatter.ofPattern("mm")
    private var clockSeparator: String = "\n"

    //Player views:
    private var artworkView: ImageView? = null   //eventReceiver(new song)
    private var songView: TextView? = null   //eventReceiver(new song)
    private var artistView: TextView? = null   //eventReceiver(new song)
    private var contextView: TextView? = null   //eventReceiver(new song)


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.fake_lock_screen)
        acts_active.add(TAG)

        clock_active = true
        //Send broadcast:
        Intent().also { intent ->
            intent.setAction(ACTION_CLOCK_OPENED)
            sendBroadcast(intent)
        }

        //Load views:
        dateView = findViewById<TextView>(R.id.text_date)
        clockView = findViewById<TextView>(R.id.text_clock)
        exitButton = findViewById<View>(R.id.exit_button)
        songView = findViewById<TextView>(R.id.song_name)
        artistView = findViewById<TextView>(R.id.artist_name)
        contextView = findViewById<TextView>(R.id.context_name)
        artworkView = findViewById<ImageView>(R.id.artwork)

        //Start personal Receiver:
        val actFilter = IntentFilter()
        actFilter.addAction(ACTION_TIME_TICK)
        actFilter.addAction(ACTION_NEW_SONG)
        actFilter.addAction(SPOTIFY_METADATA_CHANGED)

        //register all the broadcast dynamically in onCreate() so they get activated when app is open and remain in background:
        registerReceiver(clockActReceiver, actFilter, RECEIVER_EXPORTED)
        Log.d(TAG, "ClockActReceiver started.")

        //Start clock:
        updateDateClock()

        //PLAYER INFO AREA:
        if (songName == "") {
            //Show hints:
            songView!!.text = "Don't turn off the screen!"
            artistView!!.text = "Keep this Clock Screen on to save battery"
            contextView!!.text = "(unless you're using Maps)"
        } else {
            //Currently playing:
            songView!!.text = songName
            artistView!!.text = artistName
            contextView!!.text = contextName
            if (artwork != "") {
                Picasso.get().load(artwork)
                    .into(artworkView)
            } else {
                Picasso.get().load(R.drawable.artwork_icon)
                    .into(artworkView)
            }
        }

        //Screen density:
        density = applicationContext.resources.displayMetrics.density

        //Check initial orientation:
        var config = getResources().getConfiguration()
        if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
            //Vertical:
            clockSeparator = "\n"
        }
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //Horizontal:
            clockSeparator = ":"
            //Move exit button to the LEFT:
            exitButton!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                rightToRight = ConstraintLayout.LayoutParams.UNSET   //clear
                topToBottom = ConstraintLayout.LayoutParams.UNSET   //clear
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                setMargins((50*density).roundToInt(),0,0,0)
            }
            //Fix constraint for last textView:
            artworkView!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                bottomToTop = ConstraintLayout.LayoutParams.UNSET   //clear
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            }
            //Fix Clock text size:
            clockView!!.textSize = 140F
        }

        //Hide status bar:
        val mainContainer = findViewById<ConstraintLayout>(R.id.fake_lock_container)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, mainContainer).let { controller ->
            controller.hide(WindowInsetsCompat.Type.statusBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        //Exit buttons listeners:
        exitButton!!.setOnClickListener(View.OnClickListener {
            finish()
            //Start Main:
            val intent1 = Intent(this, MainActivity::class.java)
            startActivity(intent1)
        })
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setVerticalView()
        }
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setHorizontalView()
        }
    }

    override fun onDestroy() {
        clock_active = false
        //Send broadcast:
        Intent().also { intent ->
            intent.setAction(ACTION_CLOCK_CLOSED)
            sendBroadcast(intent)
        }
        //unregister receivers:
        unregisterReceiver(clockActReceiver)
        //empty views:
        artworkView = null
        songView = null
        artistView = null
        contextView = null
        acts_active.remove(TAG)
        super.onDestroy()
    }

    override fun onPause() {
        clock_active = false
        //Send broadcast:
        Intent().also { intent ->
            intent.setAction(ACTION_CLOCK_CLOSED)
            sendBroadcast(intent)
        }
        super.onPause()
    }

    override fun onStop() {
        clock_active = false
        //Send broadcast:
        Intent().also { intent ->
            intent.setAction(ACTION_CLOCK_CLOSED)
            sendBroadcast(intent)
        }
        super.onStop()
    }

    override fun onStart() {
        clock_active = true
        //Send broadcast:
        Intent().also { intent ->
            intent.setAction(ACTION_CLOCK_OPENED)
            sendBroadcast(intent)
        }
        super.onStart()
    }

    override fun onResume() {
        clock_active = true
        //Send broadcast:
        Intent().also { intent ->
            intent.setAction(ACTION_CLOCK_OPENED)
            sendBroadcast(intent)
        }
        super.onResume()
    }

    override fun onBackPressed() {
        finish()
        //Start Main:
        val intent1 = Intent(this, MainActivity::class.java)
        startActivity(intent1)
    }

    fun setVerticalView() {
        //Vertical:
        clockSeparator = "\n"
        clockView!!.text = now!!.format(hourFormat) + clockSeparator + now!!.format(minsFormat)
        //Move exit button to the BOTTOM:
        exitButton!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
            topToTop = ConstraintLayout.LayoutParams.UNSET   //clear
            rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
            topToBottom = R.id.artwork
            setMargins(0,(40*density).roundToInt(),0,0)
        }
        //Fix constraint for last textView:
        artworkView!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
            bottomToBottom = ConstraintLayout.LayoutParams.UNSET   //clear
            bottomToTop = R.id.exit_button
        }
        //Fix Clock text size:
        clockView!!.textSize = 150F
    }

    fun setHorizontalView() {
        //Horizontal:
        clockSeparator = ":"
        clockView!!.text = now!!.format(hourFormat) + clockSeparator + now!!.format(minsFormat)
        //Move exit button to the LEFT:
        exitButton!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
            rightToRight = ConstraintLayout.LayoutParams.UNSET   //clear
            topToBottom = ConstraintLayout.LayoutParams.UNSET   //clear
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            setMargins((50*density).roundToInt(),0,0,0)
        }
        //Fix constraint for last textView:
        artworkView!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
            bottomToTop = ConstraintLayout.LayoutParams.UNSET   //clear
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
        //Fix Clock text size:
        clockView!!.textSize = 140F
    }

    fun updateDateClock() {
        now = LocalDateTime.now()
        dateView!!.text = now!!.format(dateFormat)
        clockView!!.text = now!!.format(hourFormat) + clockSeparator + now!!.format(minsFormat)
    }

    fun updatePlayer() {
        //Song name:
        songName = currently_playing!!.get("song_name").asString
        if (songName.length > 30) {
            songName = songName.slice(0..30) + "..."
        }
        //Artist name:
        artistName = currently_playing!!.get("artist_name").asString
        if (artistName.length > 30) {
            artistName = artistName.slice(0..30) + "..."
        }
        //Context name:
        contextName = currently_playing!!.get("album_name").asString
        if (contextName.length > 30) {
            contextName = contextName.slice(0..30) + "..."
        }
        //Artwork:
        if (currently_playing!!.has("artwork")) {
            artwork = currently_playing!!.get("artwork").asString
        } else {
            artwork = ""
        }

        //Populate player info:
        songView!!.text = songName
        artistView!!.text = artistName
        contextView!!.text = contextName
        if (artwork != "") {
            Picasso.get().load(artwork)
                .into(artworkView)
        } else {
            Picasso.get().load(R.drawable.artwork_icon)
                .into(artworkView)
        }
    }


    //PERSONAL RECEIVER:
    var clockActReceiver = object: BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            //Update clock (every minute):
            if (intent!!.action == ACTION_TIME_TICK) {
                updateDateClock()
            }

            //Action New Song manually triggered:
            if (intent.action == ACTION_NEW_SONG) {
                Log.d(TAG, "CLOCK: ACTION_NEW_SONG.")

                try {
                    updatePlayer()
                } catch (e: Exception) {
                    Log.d(TAG, "CLOCK: ACTION_NEW_SONG: resources not available.")
                }
            }

            //Spotify Metadata Changed:
            if (intent.action == SPOTIFY_METADATA_CHANGED) {
                Log.d(TAG, "CLOCK: SPOTIFY_METADATA_CHANGED.")

                try {
                    //get new track data:
                    val id = intent.getStringExtra("id")
                    val intentSongName = intent.getStringExtra("track")
                    val intentArtistName = intent.getStringExtra("artist")
                    val intentAlbumName = intent.getStringExtra("album")

                    if (intentSongName != currently_playing!!.get("song_name").asString ||
                        intentArtistName != currently_playing!!.get("artist_name").asString ||
                        intentAlbumName != currently_playing!!.get("album_name").asString) {
                        //Update currently_playing JSON:
                        currently_playing = JsonObject()
                        currently_playing!!.addProperty("id", id)
                        currently_playing!!.addProperty("uri", "$uri_format$id")
                        currently_playing!!.addProperty("spotify_URL", "$ext_format$id")
                        currently_playing!!.addProperty("song_name", intentSongName)
                        currently_playing!!.addProperty("artist_name", intentArtistName)
                        currently_playing!!.addProperty("album_name", intentAlbumName)
                        //Update player:
                        updatePlayer()
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "CLOCK: SPOTIFY_METADATA_CHANGED: resources not available.")
                }
            }
        }
    }

}