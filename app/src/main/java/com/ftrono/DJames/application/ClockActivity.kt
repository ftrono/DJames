package com.ftrono.DJames.application

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.lifecycle.MutableLiveData
import com.ftrono.DJames.R
import com.ftrono.DJames.ui.theme.ClockTheme
import com.ftrono.DJames.ui.theme.black
import com.ftrono.DJames.utilities.Utilities
import com.google.gson.JsonObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class ClockActivity: ComponentActivity() {

    private val TAG: String = ClockActivity::class.java.getSimpleName()
    private val utils = Utilities()

    //Parameters:
    private val dateFormat = DateTimeFormatter.ofPattern("E, dd MMM")
    private val hourFormat = DateTimeFormatter.ofPattern("HH")
    private val minsFormat = DateTimeFormatter.ofPattern("mm")

    //Status:
    private var currentDate = MutableLiveData<String>("Mon 1 Jan")
    private var currentHour = MutableLiveData<String>("00")
    private var currentMins = MutableLiveData<String>("00")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        acts_active.add(TAG)

        enableEdgeToEdge(
            //For safe padding:
            statusBarStyle = SystemBarStyle.auto(black.toArgb(), black.toArgb()),
            navigationBarStyle = SystemBarStyle.auto(black.toArgb(), black.toArgb())
        )
        setContent {
            ClockTheme {
                //Background:
                Surface (
                    modifier = Modifier.fillMaxSize(),
                    color = black
                ) {
                    ClockScreen()
                }
            }
        }

        clock_active = true
        //Send broadcast:
        Intent().also { intent ->
            intent.setAction(ACTION_CLOCK_OPENED)
            sendBroadcast(intent)
        }

        //Start personal Receiver:
        val actFilter = IntentFilter()
        actFilter.addAction(ACTION_TIME_TICK)
        actFilter.addAction(SPOTIFY_METADATA_CHANGED)
        actFilter.addAction(ACTION_FINISH_CLOCK)

        //register all the broadcast dynamically in onCreate() so they get activated when app is open and remain in background:
        registerReceiver(clockActReceiver, actFilter, RECEIVER_EXPORTED)
        Log.d(TAG, "ClockActReceiver started.")

        //Start clock:
        updateDateClock()
    }


    @Preview
    @Preview(heightDp = 360, widthDp = 800)
    @Composable
    fun ClockScreen() {
        //States:
        val configuration = LocalConfiguration.current
        val isLandscape by remember { mutableStateOf(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) }
        val currentDateState by currentDate.observeAsState()
        val currentHourState by currentHour.observeAsState()
        val currentMinsState by currentMins.observeAsState()
        val currentSongPlayingState by currentSongPlaying.observeAsState()
        val currentArtistPlayingState by currentArtistPlaying.observeAsState()
        val currentAlbumPlayingState by currentAlbumPlaying.observeAsState()

        val mContext = LocalContext.current
        val playerDialogOn = rememberSaveable { mutableStateOf(false) }
        if (playerDialogOn.value) {
            PlayerDialog(mContext, playerDialogOn)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .background(colorResource(id = R.color.black)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //DATE:
            Text(
                modifier = Modifier
                    .padding(bottom = if (!isLandscape) 20.dp else 10.dp)
                    .fillMaxWidth(),
                text = currentDateState!!,
                color = colorResource(id = R.color.faded_grey),
                textAlign = TextAlign.Center,
                fontSize = 22.sp
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                //CLOSE BUTTON (HORIZONTAL):
                if (isLandscape) {
                    CloseButton(true)
                }
                //MAXI CLOCK:
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(0f),
                    text = if (!isLandscape) {
                        "${currentHourState!!}\n${currentMinsState!!}"
                    } else {
                        "${currentHourState!!}:${currentMinsState!!}"
                    },
                    color = colorResource(id = R.color.faded_grey),
                    textAlign = TextAlign.Center,
                    fontSize = if (!isLandscape) 150.sp else 140.sp,
                    lineHeight = 150.sp
                )
            }

            //PLAYER INFO:
            Card(
                onClick = { playerDialogOn.value = true },
                modifier = Modifier
                    .padding(
                        top = if (!isLandscape) 20.dp else 10.dp,
                        bottom = if (!isLandscape) 40.dp else 0.dp
                    )
                    .wrapContentSize(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors (
                    containerColor = colorResource(id = R.color.dark_grey_background)
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(
                            top=10.dp,
                            bottom=10.dp,
                            start=24.dp,
                            end=24.dp
                        ),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    //ARTWORK ICON:
                    Image(
                        modifier = Modifier
                            .width(50.dp)
                            .height(50.dp)
                            .zIndex(1f),
                        painter = painterResource(id = R.drawable.artwork_clock),
                        contentDescription = "Album art"
                    )
                    Column(
                        modifier = Modifier
                            .wrapContentWidth()
                            .wrapContentHeight(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start
                    ) {
                        //SONG NAME:
                        Text(
                            modifier = Modifier
                                .padding(start = 14.dp)
                                .wrapContentWidth(),
                            text = currentSongPlayingState!!,
                            color = colorResource(id = R.color.mid_grey),
                            fontSize = 18.sp,
                            fontStyle = FontStyle.Italic
                        )
                        //ARTIST NAME:
                        Text(
                            modifier = Modifier
                                .padding(start = 14.dp)
                                .wrapContentWidth(),
                            text = currentArtistPlayingState!!,
                            color = colorResource(id = R.color.mid_grey),
                            fontSize = 14.sp
                        )
                        //ALBUM NAME:
                        Text(
                            modifier = Modifier
                                .padding(start = 14.dp)
                                .offset(y = -(2.dp))
                                .wrapContentWidth(),
                            text = currentAlbumPlayingState!!,
                            color = colorResource(id = R.color.mid_grey),
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }
            //CLOSE BUTTON (VERTICAL):
            if (!isLandscape) {
                CloseButton(false)
            }
        }

    }


    @Composable
    fun CloseButton(isLandscape: Boolean) {
        OutlinedButton(
            modifier= Modifier
                .padding(start = if (isLandscape) 20.dp else 0.dp)
                .size(50.dp)
                .zIndex(1f),  //avoid the oval shape
            shape = CircleShape,
            border= BorderStroke(2.dp, colorResource(id = R.color.faded_grey)),
            contentPadding = PaddingValues(0.dp),  //avoid the little icon
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colorResource(id = R.color.faded_grey)),
            onClick = {
                //Start Main:
                val intent1 = Intent(this, MainActivity::class.java)
                startActivity(intent1)
                finish()
            }
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "content description")
        }
    }


    @Composable
    fun PlayerDialog(context: Context, playerDialogOn: MutableState<Boolean>) {
        Dialog(onDismissRequest = { playerDialogOn.value = false }) {
            //CONTAINER:
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors (
                    containerColor = colorResource(id = R.color.dark_grey)
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .wrapContentWidth()
                        .wrapContentHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start,
                ) {
                    //TITLE:
                    Text(
                        text = "To get the best DJames experience",
                        modifier = Modifier.padding(8.dp),
                        color = colorResource(id = R.color.light_grey),
                        textAlign = TextAlign.Start,
                        fontSize = 22.sp
                    )
                    //TEXT 1:
                    Text(
                        text = "Open your Spotify app, then go to \"Settings & Privacy\" -> \"Playback\" and ensure these 2 toggles are ON:",
                        modifier = Modifier.padding(8.dp),
                        color = colorResource(id = R.color.light_grey),
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp
                    )
                    //IMAGE:
                    Image(
                        painter = painterResource(id = R.drawable.spotify_settings),
                        contentDescription = "Spotify player settings",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth()
                            .wrapContentHeight()
                    )
                    //TEXT 2:
                    Text(
                        text = "This will enable player info and ensure continuous playback.",
                        modifier = Modifier.padding(8.dp),
                        color = colorResource(id = R.color.light_grey),
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp
                    )
                    //BUTTONS ROW:
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(top = 8.dp, end = 12.dp)
                                .clickable { playerDialogOn.value = false },
                            color = colorResource(id = R.color.colorAccentLight),
                            text = "Ok",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
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
        if (!overlay_active) {
            //Start Main:
            finish()
            val intent1 = Intent(applicationContext, MainActivity::class.java)
            startActivity(intent1)
        } else {
            clock_active = true
            //Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_CLOCK_OPENED)
                sendBroadcast(intent)
            }
        }
        super.onStart()
    }

    override fun onResume() {
        if (!overlay_active) {
            //Start Main:
            finish()
            val intent1 = Intent(applicationContext, MainActivity::class.java)
            startActivity(intent1)
        } else {
            clock_active = true
            //Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_CLOCK_OPENED)
                sendBroadcast(intent)
            }
        }
        super.onResume()
    }

    override fun onBackPressed() {
        finish()
        //Start Main:
        val intent1 = Intent(this, MainActivity::class.java)
        startActivity(intent1)
    }

    fun updateDateClock() {
        var now = LocalDateTime.now()
        currentDate.postValue(now.format(dateFormat))
        currentHour.postValue(now.format(hourFormat))
        currentMins.postValue(now.format(minsFormat))
    }

    fun updatePlayer() {
        //Populate player info:
        currentSongPlaying.postValue(utils.trimString(songName))
        currentArtistPlaying.postValue(utils.trimString(artistName))
        currentAlbumPlaying.postValue(utils.trimString(contextName))
    }


    //PERSONAL RECEIVER:
    var clockActReceiver = object: BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            //Update clock (every minute):
            if (intent!!.action == ACTION_TIME_TICK) {
                updateDateClock()
                if (!enablePlayerInfo) {
                    enablePlayerInfo = true
                    if (songName != "") {
                        updatePlayer()
                    }
                }
            }

            //Spotify Metadata Changed:
            if (intent.action == SPOTIFY_METADATA_CHANGED) {
                Log.d(TAG, "CLOCK: SPOTIFY_METADATA_CHANGED.")
                try {
                    //Get new track data:
                    val id = intent.getStringExtra("id")
                    val intentSongName = intent.getStringExtra("track")
                    val intentArtistName = intent.getStringExtra("artist")
                    val intentAlbumName = intent.getStringExtra("album")

                    //If new track:
                    if (intentSongName != songName || intentArtistName != artistName || intentAlbumName != contextName) {
                        //Update currently_playing JSON:
                        currently_playing = JsonObject()
                        currently_playing!!.addProperty("id", id)
                        currently_playing!!.addProperty("uri", "$uri_format$id")
                        currently_playing!!.addProperty("spotify_URL", "${ext_format}track/$id")
                        currently_playing!!.addProperty("song_name", intentSongName)
                        currently_playing!!.addProperty("artist_name", intentArtistName)
                        currently_playing!!.addProperty("album_name", intentAlbumName)

                        //Update info:
                        songName = intentSongName!!
                        artistName = intentArtistName!!
                        contextName = intentAlbumName!!

                        //Update player:
                        if (enablePlayerInfo) {
                            updatePlayer()
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "CLOCK: SPOTIFY_METADATA_CHANGED: resources not available.")
                }
            }

            //Finish activity:
            if (intent.action == ACTION_FINISH_CLOCK) {
                Log.d(TAG, "CLOCK: ACTION_FINISH_CLOCK.")
                finish()
                if (clock_active) {
                    //Start Main:
                    val intent1 = Intent(applicationContext, MainActivity::class.java)
                    startActivity(intent1)
                }
            }

        }
    }

}