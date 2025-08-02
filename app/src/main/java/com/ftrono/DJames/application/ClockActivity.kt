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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.lifecycle.MutableLiveData
import com.ftrono.DJames.R
import com.ftrono.DJames.application.MainActivity
import com.ftrono.DJames.ui.dialogs.GeneralDialog
import com.ftrono.DJames.ui.components.RoundedSign
import com.ftrono.DJames.ui.theme.ClockTheme
import com.ftrono.DJames.ui.theme.black
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class ClockActivity: ComponentActivity() {

    private val TAG: String = ClockActivity::class.java.getSimpleName()

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

        clockActive.postValue(true)

        //Start personal Receiver:
        val actFilter = IntentFilter()
        actFilter.addAction(ACTION_TIME_TICK)
        actFilter.addAction(ACTION_UPDATE_PLAYER)
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
        val overlayPosState by overlayPos.observeAsState()
        val currentDateState by currentDate.observeAsState()
        val currentHourState by currentHour.observeAsState()
        val currentMinsState by currentMins.observeAsState()

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
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                //CLOSE BUTTON (HORIZONTAL - OVERLAY ON THE RIGHT):
                if (isLandscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (overlayPosState!! == "Right") {
                            CloseButton()
                            MaxiClock(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                isLandscape = isLandscape,
                                currentHourState = currentHourState!!,
                                currentMinsState = currentMinsState!!
                            )
                            ClosePlaceholder()
                        } else {
                            ClosePlaceholder()
                            MaxiClock(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                isLandscape = isLandscape,
                                currentHourState = currentHourState!!,
                                currentMinsState = currentMinsState!!
                            )
                            CloseButton()
                        }
                    }
                } else {
                    MaxiClock(
                        modifier = Modifier
                            .fillMaxWidth(),
                        isLandscape = isLandscape,
                        currentHourState = currentHourState!!,
                        currentMinsState = currentMinsState!!
                    )
                }
            }
            //PLAYER INFO:
            PlayerInfo(isLandscape)

            //CLOSE BUTTON (VERTICAL):
            if (!isLandscape) {
                CloseButton()
            }
        }

    }


    @Composable
    fun MaxiClock(
        modifier: Modifier,
        isLandscape: Boolean,
        currentHourState: String,
        currentMinsState: String
    ) {
        //MAXI CLOCK:
        Text(
            modifier = modifier,
            text = if (!isLandscape) {
                "${currentHourState}\n${currentMinsState}"
            } else {
                "${currentHourState}:${currentMinsState}"
            },
            color = colorResource(id = R.color.faded_grey),
            textAlign = TextAlign.Center,
            fontSize = if (!isLandscape) 150.sp else 140.sp,
            lineHeight = 150.sp
        )
    }


    @Composable
    fun PlayerInfo(isLandscape: Boolean) {
        //PLAYER INFO:
        val currentPlayingPrefixState by currentPlayingPrefix.observeAsState()
        val currentSongPlayingState by currentSongPlaying.observeAsState()
        val currentArtistPlayingState by currentArtistPlaying.observeAsState()

        val mContext = LocalContext.current
        val playerDialogOn = rememberSaveable { mutableStateOf(false) }
        if (playerDialogOn.value) {
            PlayerDialog(mContext, playerDialogOn)
        }

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
                RoundedSign(
                    signSize = 50.dp,
                    contentSize = 30,
                    backgroundColor = colorResource(id = R.color.black),
                    borderColor = colorResource(id = R.color.faded_grey),
                    contentColor = colorResource(id = R.color.midfaded_grey),
                    iconPainter = painterResource(id = R.drawable.sign_note)
                )
                Column(
                    modifier = Modifier
                        .wrapContentWidth()
                        .wrapContentHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    if (currentPlayingPrefixState != "") {
                        //PREFIX:
                        Text(
                            modifier = Modifier
                                .padding(start = 14.dp, bottom = 2.dp)
                                .wrapContentWidth(),
                            text = currentPlayingPrefixState!!,
                            lineHeight = 12.sp,
                            color = colorResource(id = R.color.midfaded_grey),
                            fontSize = 12.sp,
                            // fontWeight = FontWeight.Bold
                        )
                    }
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
                        lineHeight = 16.sp,
                        color = colorResource(id = R.color.mid_grey),
                        fontSize = 16.sp
                    )
                }
            }
        }
    }


    @Composable
    fun CloseButton() {
        OutlinedButton(
            modifier= Modifier
                .padding(start = 20.dp, end = 20.dp)
                .size(50.dp),
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
    fun ClosePlaceholder() {
        Box(
            modifier= Modifier
                .padding(start = 20.dp, end = 20.dp)
                .size(50.dp)
                .background(colorResource(id = R.color.black))
        )
    }


    @Composable
    fun PlayerDialog(
        context: Context,
        playerDialogOn: MutableState<Boolean>
    ) {
        GeneralDialog(
            dialogOnState = playerDialogOn,
            backgroundColor = colorResource(id = R.color.dark_grey),
            title = "To get the best DJames experience",
            content = {
                //TEXT 1:
                Text(
                    text = "Open your Spotify app, then go to \"Settings & Privacy\" -> \"Playback\" and ensure these 2 toggles are ON:",
                    modifier = Modifier.padding(bottom=8.dp),
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
                        .padding(top = 8.dp, bottom = 8.dp)
                        .fillMaxWidth()
                        .wrapContentHeight()
                )
                //TEXT 2:
                Text(
                    text = "This will enable player info and ensure continuous playback.",
                    modifier = Modifier.padding(top=8.dp),
                    color = colorResource(id = R.color.light_grey),
                    textAlign = TextAlign.Start,
                    fontSize = 14.sp
                )
            },
            dismissText = "Ok"
        )
    }


    override fun onDestroy() {
        clockActive.postValue(false)
        //unregister receivers:
        unregisterReceiver(clockActReceiver)
        acts_active.remove(TAG)
        super.onDestroy()
    }

    override fun onPause() {
        clockActive.postValue(false)
        super.onPause()
    }

    override fun onStop() {
        clockActive.postValue(false)
        super.onStop()
    }

    override fun onStart() {
        if (!overlayActive.value!!) {
            //Start Main:
            finish()
            val intent1 = Intent(applicationContext, MainActivity::class.java)
            startActivity(intent1)
        } else {
            clockActive.postValue(true)
        }
        super.onStart()
    }

    override fun onResume() {
        if (!overlayActive.value!!) {
            //Start Main:
            finish()
            val intent1 = Intent(applicationContext, MainActivity::class.java)
            startActivity(intent1)
        } else {
            clockActive.postValue(true)
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
        if (currentPlayingPrefix.value == "") {
            currentPlayingPrefix.postValue("Last song played")
        }
        currentSongPlaying.postValue(utils.trimString(songName, 28))
        currentArtistPlaying.postValue(utils.trimString(artistName, 28))
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

            //Update player:
            if (intent.action == ACTION_UPDATE_PLAYER) {
                Log.d(TAG, "CLOCK: ACTION_UPDATE_PLAYER.")
                updatePlayer()
            }

            //Finish activity:
            if (intent.action == ACTION_FINISH_CLOCK) {
                Log.d(TAG, "CLOCK: ACTION_FINISH_CLOCK.")
                finish()
                if (clockActive.value!!) {
                    //Start Main:
                    val intent1 = Intent(applicationContext, MainActivity::class.java)
                    startActivity(intent1)
                }
            }

        }
    }

}