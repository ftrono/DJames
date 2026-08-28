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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
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
import coil3.compose.AsyncImage
import com.ftrono.DJames.R
import com.ftrono.DJames.ui.components.RoundedSign
import com.ftrono.DJames.ui.navigation.MainNavBar
import com.ftrono.DJames.ui.theme.ClockTheme
import com.ftrono.DJames.ui.theme.black
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class ClockActivity: ComponentActivity() {

    private val TAG: String = ClockActivity::class.java.getSimpleName()

    //Parameters:
    private val dayFormat = DateTimeFormatter.ofPattern("E,")
    private val dateFormat = DateTimeFormatter.ofPattern("dd MMM")
    private val hourFormat = DateTimeFormatter.ofPattern("HH")
    private val minsFormat = DateTimeFormatter.ofPattern("mm")

    //Status:
    private var currentDay = MutableLiveData<String>("Mon,")
    private var currentDate = MutableLiveData<String>("1 Jan")
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
        val mContext = LocalContext.current
        val configuration = LocalConfiguration.current
        val isLandscape by remember { mutableStateOf(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) }

        val overlayActiveState by overlayActive.observeAsState()
        val overlayPosState by overlayPos.observeAsState()
        val clickCounterState by clickCounter.observeAsState()
        val currentDayState by currentDay.observeAsState()
        val currentDateState by currentDate.observeAsState()
        val currentHourState by currentHour.observeAsState()
        val currentMinsState by currentMins.observeAsState()
        val currentPlayerColorState by currentPlayerColor.observeAsState()

        Row(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            horizontalArrangement = Arrangement.Center
        ) {
            //SIDE NAV BAR (LEFT):
            if (isLandscape && overlayPosState == "Left") {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(100.dp)
                        .background(colorResource(id = R.color.black)),
                )
            }
            Scaffold(
                modifier = Modifier
                    .fillMaxSize(),
                topBar = {
                    if (!isLandscape) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(65.dp)
                                .background(colorResource(id = R.color.black)),
                        )
                    }
                },
                bottomBar = {
                    if (!isLandscape) {
                        if (overlayActiveState!!) {
                            MainNavBar(
                                clickCounterState = clickCounterState!!,
                                isLandscape = false,
                                onClickCenter = { }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .background(
                                        colorResource(R.color.black)
                                    )
                            )
                        }
                    }
                },
            ) {
                Box(
                    modifier = Modifier
                        .padding(it)
                )
                {
                    // Content:
                    if (isLandscape) {
                        // LANDSCAPE:
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(colorResource(id = R.color.black)),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Info area:
                            ClockInfoArea(
                                context = mContext,
                                modifier = Modifier
                                    .padding(
                                        top = 8.dp,
                                        bottom = 8.dp,
                                        start = 20.dp,
                                        end = 20.dp,
                                    ),
                                currentDayState = currentDayState!!,
                                currentDateState = currentDateState!!,
                                currentHourState = currentHourState!!,
                                currentMinsState = currentMinsState!!,
                                currentPlayerColorState = currentPlayerColorState!!,
                            )
                            // Unlock:
                            UnlockButton(
                                context = mContext,
                                modifier = Modifier
                                    .padding(start=60.dp),
                                currentPlayerColorState = currentPlayerColorState!!,
                            )
                        }

                    } else {
                        // PORTRAIT:
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(colorResource(id = R.color.black)),
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Info area:
                            ClockInfoArea(
                                context = mContext,
                                modifier = Modifier
                                    .padding(
                                        top = 12.dp,
                                        bottom = 100.dp,
                                        start = 20.dp,
                                        end = 20.dp,
                                    ),
                                currentDayState = currentDayState!!,
                                currentDateState = currentDateState!!,
                                currentHourState = currentHourState!!,
                                currentMinsState = currentMinsState!!,
                                currentPlayerColorState = currentPlayerColorState!!,
                            )
                            // Unlock:
                            UnlockButton(
                                context = mContext,
                                modifier = Modifier,
                                currentPlayerColorState = currentPlayerColorState!!,
                            )
                        }
                    }
                }
            }
            //SIDE NAV BAR (RIGHT):
            if (isLandscape && overlayPosState == "Right") {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(100.dp)
                        .background(colorResource(id = R.color.black)),
                )
            }
        }
    }


    @Composable
    fun ClockInfoArea(
        context: Context,
        modifier: Modifier,
        currentDayState: String,
        currentDateState: String,
        currentHourState: String,
        currentMinsState: String,
        currentPlayerColorState: String,
    ) {
        val colorRGB = utils.hslToColor(currentPlayerColorState)

        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // CLOCK:
            Text(
                modifier = Modifier,
                text = "${currentHourState}\n${currentMinsState}",
                color = colorRGB,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                fontSize = 140.sp,
                lineHeight = 120.sp,
            )

            Column(
                modifier = Modifier
                    .padding(start=20.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                //DAY:
                Text(
                    modifier = Modifier,
                    text = currentDayState,
                    fontWeight = FontWeight.Medium,
                    color = colorRGB,
                    fontSize = 32.sp,
                    lineHeight = 32.sp,
                )
                //DATE:
                Text(
                    modifier = Modifier
                        .padding(bottom = 12.dp),
                    text = currentDateState,
                    fontWeight = FontWeight.Medium,
                    color = colorRGB,
                    fontSize = 32.sp,
                    lineHeight = 32.sp,
                )

                //PLAYER INFO:
                PlayerInfo(context)
            }
        }
    }


    //PLAYER INFO:
    @Composable
    fun PlayerInfo(
        context: Context,
    ) {
        val currentSongPlayingState by currentSongPlaying.observeAsState()
        val currentArtistPlayingState by currentArtistPlaying.observeAsState()
        val currentImage by currentPlayerImage.observeAsState()

        Card(
            modifier = Modifier
                .padding(top = 12.dp)
                .width(160.dp)
                .height(120.dp)
                .clickable {
                    // Open Spotify context URL:
                    if (lastPlaybackInfo.contextUrl != "") {
                        utils.openLink(
                            context = context,
                            url = lastPlaybackInfo.contextUrl
                        )
                    }
                },
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(0.5.dp, colorResource(id = R.color.dark_grey_background)),
        ) {
            val imagePresent = (currentImage != null && currentImage != "")
            val darkenValue = 0.3f

            Box(
                modifier = if (imagePresent) {
                    Modifier
                        .fillMaxSize()
                } else {
                    Modifier
                        .fillMaxSize()
                        .background(colorResource(id = R.color.dark_grey_background))
                }
            ) {
                if (imagePresent) {
                    AsyncImage(
                        modifier = Modifier
                            .fillMaxSize(),
                        model = currentImage,
                        contentDescription = "Artwork",
                        contentScale = ContentScale.Crop,
                        colorFilter = ColorFilter.colorMatrix(
                            ColorMatrix().apply {
                                setToScale(
                                    redScale = darkenValue,
                                    greenScale = darkenValue,
                                    blueScale = darkenValue,
                                    alphaScale = 1f
                                )
                            }
                        )
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    //ICON:
                    Icon(
                        modifier = Modifier
                            .padding(bottom=6.dp)
                            .size(22.dp),
                        painter = painterResource(id = R.drawable.logo_spotify),
                        contentDescription = "Item image",
                        tint = colorResource(id = R.color.midfaded_grey),
                    )
                    //SONG NAME:
                    Text(
                        modifier = Modifier
                            .padding(bottom = 2.dp),
                        text = currentSongPlayingState!!,
                        color = colorResource(id = R.color.mid_grey),
                        fontSize = 16.sp,
                        lineHeight = 16.sp,
                        fontStyle = FontStyle.Italic
                    )
                    //ARTIST NAME:
                    Text(
                        modifier = Modifier,
                        text = currentArtistPlayingState!!,
                        fontSize = 14.sp,
                        lineHeight = 14.sp,
                        color = colorResource(id = R.color.mid_grey)
                    )
                }
            }
        }
    }

    @Composable
    fun UnlockButton(
        context: Context,
        modifier: Modifier,
        currentPlayerColorState: String,
    ) {
        val colorRGB = utils.hslToColor(currentPlayerColorState)

        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RoundedSign(
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp)
                    .clickable {
                        // Go to Home:
                        finish()
                        mainActive.postValue(true)
                        utils.openActivity(context, MainActivity::class.java)
                    },
                signSize = 80.dp,
                contentSize = 40,
                backgroundColor = colorRGB,
                borderColor = colorResource(id = R.color.transparent_full),
                contentColor = colorResource(id = R.color.mid_grey),
                iconPainter = painterResource(R.drawable.icon_lock),
            )
            Text(
                modifier = Modifier
                    .padding(top=12.dp),
                text = "Unlock",
                color = colorResource(id = R.color.midfaded_grey),
                fontSize = 18.sp
            )
        }
    }


    override fun onDestroy() {
        clockActive.postValue(false)
        // Unregister receivers:
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
            utils.openActivity(this, MainActivity::class.java)
        } else {
            clockActive.postValue(true)
        }
        super.onStart()
    }

    override fun onResume() {
        if (!overlayActive.value!!) {
            //Start Main:
            finish()
            utils.openActivity(this, MainActivity::class.java)
        } else {
            clockActive.postValue(true)
        }
        super.onResume()
    }

    override fun onBackPressed() {
        finish()
        //Start Main:
        mainActive.postValue(true)
        utils.openActivity(this, MainActivity::class.java)
    }

    fun updateDateClock() {
        var now = LocalDateTime.now()
        currentDay.postValue(now.format(dayFormat))
        currentDate.postValue(now.format(dateFormat))
        currentHour.postValue(now.format(hourFormat))
        currentMins.postValue(now.format(minsFormat))
    }


    //PERSONAL RECEIVER:
    var clockActReceiver = object: BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            //Update clock (every minute):
            if (intent!!.action == ACTION_TIME_TICK) {
                updateDateClock()
            }

            //Finish activity:
            if (intent.action == ACTION_FINISH_CLOCK) {
                Log.d(TAG, "CLOCK: ACTION_FINISH_CLOCK.")
                finish()
                if (clockActive.value!!) {
                    //Start Main:
                    utils.openActivity(applicationContext, MainActivity::class.java)
                }
            }
        }
    }

}