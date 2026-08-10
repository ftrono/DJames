package com.ftrono.DJames.application.screens

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ftrono.DJames.R
import com.ftrono.DJames.application.currentArtistPlaying
import com.ftrono.DJames.application.currentDate
import com.ftrono.DJames.application.currentHour
import com.ftrono.DJames.application.currentMins
import com.ftrono.DJames.application.currentSongPlaying
import com.ftrono.DJames.application.dateFormat
import com.ftrono.DJames.application.hourFormat
import com.ftrono.DJames.application.lastNavRoute
import com.ftrono.DJames.application.minsFormat
import com.ftrono.DJames.application.overlayPos
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.ui.components.RoundedSign
import com.ftrono.DJames.ui.components.StreetUIScaffold
import com.ftrono.DJames.ui.dialogs.GeneralDialog
import com.ftrono.DJames.ui.navigation.StreetUITopBar
import com.ftrono.DJames.ui.navigation.UserOptions
import com.ftrono.DJames.ui.navigation.navigateTo
import com.ftrono.DJames.ui.theme.NavigationItem
import java.time.LocalDateTime


@Preview
@Preview(heightDp = 360, widthDp = 800)
@Composable
fun ClockScreenPreview() {
    val navController = rememberNavController()
    ClockScreen(navController, preview = true)
}

@Composable
fun ClockScreen(
    navController: NavController,
    preview: Boolean = false,
) {
    //States:
    val configuration = LocalConfiguration.current
    val isLandscape by remember { mutableStateOf(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) }

    val overlayPosState by overlayPos.observeAsState()
    val currentDateState by currentDate.observeAsState()
    val currentHourState by currentHour.observeAsState()
    val currentMinsState by currentMins.observeAsState()

    StreetUIScaffold(
        modifier = Modifier
            .fillMaxSize(),
        hideLine = true,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp)
                    .background(colorResource(id = R.color.black)),
            )
        }
    ) {
        // Content:
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorResource(id = R.color.black)),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceAround
            ){
                // Info area:
                ClockInfoArea(
                    modifier = Modifier
                        .padding(
                            top=8.dp,
                            bottom=8.dp,
                            start=20.dp,
                            end=20.dp,
                        ),
                    isLandscape = true,
                    currentDateState = currentDateState!!,
                    currentHourState = currentHourState!!,
                    currentMinsState = currentMinsState!!,
                )
                // Unlock:
                UnlockButton(
                    modifier = Modifier
                        .fillMaxHeight(),
                    navController = navController,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorResource(id = R.color.black)),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Info area:
                ClockInfoArea(
                    modifier = Modifier
                        .padding(
                            top=12.dp,
                            bottom=100.dp,
                            start=20.dp,
                            end=20.dp,
                        ),
                    isLandscape = false,
                    currentDateState = currentDateState!!,
                    currentHourState = currentHourState!!,
                    currentMinsState = currentMinsState!!,
                )
                // Unlock:
                UnlockButton(
                    modifier = Modifier,
                    navController = navController,
                )
            }
        }
    }
}


@Composable
fun ClockInfoArea(
    modifier: Modifier,
    isLandscape: Boolean,
    currentDateState: String,
    currentHourState: String,
    currentMinsState: String,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // CLOCK:
        Text(
            modifier = Modifier,
            text = "${currentHourState}\n${currentMinsState}",
            color = colorResource(id = R.color.faded_grey),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            fontSize = if (isLandscape) 120.sp else 130.sp,
            lineHeight = if (isLandscape) 100.sp else 110.sp,
        )

        Column(
            modifier = Modifier
                .padding(start=20.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            //DATE:
            Text(
                modifier = Modifier
                    .padding(bottom = 12.dp),
                text = currentDateState,
                fontWeight = FontWeight.Medium,
                color = colorResource(id = R.color.faded_grey),
                fontSize = 32.sp,
                lineHeight = 32.sp,
            )

            //PLAYER INFO:
            PlayerInfo(isLandscape)
        }
    }
}


//PLAYER INFO:
@Composable
fun PlayerInfo(
    isLandscape: Boolean,
) {
    val currentSongPlayingState by currentSongPlaying.observeAsState()
    val currentArtistPlayingState by currentArtistPlaying.observeAsState()
    Card(
        modifier = Modifier
            .padding(top = 12.dp)
            .width(160.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors (
            containerColor = colorResource(id = R.color.dark_grey_background)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(
                    top=10.dp,
                    bottom=10.dp,
                    start=24.dp,
                    end=24.dp
                ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            //ICON:
            Icon(
                modifier = Modifier
                    .size(30.dp),
                painter = painterResource(id = R.drawable.icon_note),
                contentDescription = "Item image",
                tint = colorResource(id = R.color.midfaded_grey),
            )
            //SONG NAME:
            Text(
                modifier = Modifier,
                text = currentSongPlayingState!!,
                color = colorResource(id = R.color.mid_grey),
                fontSize = 18.sp,
                fontStyle = FontStyle.Italic
            )
            //ARTIST NAME:
            Text(
                modifier = Modifier,
                text = currentArtistPlayingState!!,
                lineHeight = 16.sp,
                color = colorResource(id = R.color.mid_grey),
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun UnlockButton(
    modifier: Modifier,
    navController: NavController,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RoundedSign(
            modifier = Modifier
                .padding(start = 20.dp, end = 20.dp)
                .clickable {
                    // Show full Home:
                    val curNavRoute = NavigationItem.Home.route
                    navigateTo(navController, curNavRoute)
                    lastNavRoute = curNavRoute
                },
            signSize = 80.dp,
            contentSize = 40,
            backgroundColor = colorResource(R.color.faded_grey),
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
