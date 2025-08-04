package com.ftrono.DJames.application.screens

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.ftrono.DJames.R
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ftrono.DJames.application.dialogs.DialogRequestOverlay
import com.ftrono.DJames.application.dialogs.SinglePermissionHandler
import com.ftrono.DJames.application.extraOpen
import com.ftrono.DJames.application.lastNavRoute
import com.ftrono.DJames.application.overlayActive
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.sharedLink
import com.ftrono.DJames.application.spotifyLoggedIn
import com.ftrono.DJames.application.userGender
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.ui.components.CardSign
import com.ftrono.DJames.ui.components.ChatInputField
import com.ftrono.DJames.ui.components.DriveIcon
import com.ftrono.DJames.ui.components.StreetUIScaffold
import com.ftrono.DJames.ui.navigation.StreetUITopBar
import com.ftrono.DJames.ui.navigation.UserOptions
import com.ftrono.DJames.ui.navigation.navigateTo
import com.ftrono.DJames.ui.selectors.guideColorSelectorLight
import com.ftrono.DJames.ui.selectors.guideIconSelector
import com.ftrono.DJames.ui.theme.NavigationItem
import kotlin.Boolean


@Preview
@Preview(heightDp = 360, widthDp = 800)
@Composable
fun HomeScreenPreview() {
    val navController = rememberNavController()
    HomeScreen(navController, preview = true)
}

@Composable
fun HomeScreen(
    navController: NavController,
    preview: Boolean = false
) {
    val configuration = LocalConfiguration.current
    val isLandscape by remember { mutableStateOf(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) }

    val mContext = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val spotifyLoggedInState by spotifyLoggedIn.observeAsState()
    val sharedLinkState by sharedLink.observeAsState()
    if (sharedLinkState != "") {
        val curNavRoute = NavigationItem.Library.route
        navigateTo(navController, curNavRoute)
        lastNavRoute = curNavRoute
    }

    //Overlay permission management:
    val requestOverlayOn = rememberSaveable { mutableStateOf(false) }
    if (requestOverlayOn.value) {
        DialogRequestOverlay(
            mContext = mContext,
            dialogOnState = requestOverlayOn
        )
    }
    // Mic permissions management:
    val requestPermissions = rememberSaveable { mutableStateOf(false) }
    if (requestPermissions.value) {
        SinglePermissionHandler(
            context = mContext,
            dialogOnState = requestPermissions,
            permission = Manifest.permission.RECORD_AUDIO
        )
    }

    StreetUIScaffold(
        lineDistance = 70.dp,
        topBar = {
            StreetUITopBar(
                pretitle = "",
                title = "DJames",
                subtitle = if (!spotifyLoggedInState!!) "Not logged in" else "for ${prefs.spotUserName}",
                showBack = false,
                optionButtons = {
                    UserOptions(
                        context = mContext,
                        lifecycleOwner = lifecycleOwner,
                        navController = navController,
                        preview = preview,
                    )
                }
            )
        }
    ) {
        //WRAPPER:
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // SpotifyLoginStatus(Modifier, spotifyLoggedInState!!, mContext)
            if (isLandscape) {
                //DISPLAY HORIZONTALLY:
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    LeftSide(
                        modifier = Modifier
                            .weight(0.5F),
                        context = mContext,
                        navController = navController,
                        isLandscape = isLandscape,
                        preview = preview,
                    )
                    RightSide(
                        modifier = Modifier
                            .weight(0.5F),
                        context = mContext,
                        navController = navController,
                        requestPermissions = requestPermissions,
                        requestOverlayOn = requestOverlayOn,
                        isLandscape = isLandscape,
                        preview = preview,
                    )
                }
            } else {
                //DISPLAY VERTICALLY:
                LeftSide(
                    context = mContext,
                    navController = navController,
                    isLandscape = isLandscape,
                    preview = preview,
                )
                RightSide(
                    context = mContext,
                    navController = navController,
                    requestPermissions = requestPermissions,
                    requestOverlayOn = requestOverlayOn,
                    isLandscape = isLandscape,
                    preview = preview,
                )
            }

        }
    }
}


@Composable
fun LeftSide(
    modifier: Modifier = Modifier,
    context: Context,
    navController: NavController,
    isLandscape: Boolean,
    preview: Boolean = false
) {
    val genderState by userGender.observeAsState()

    Column(
        modifier = modifier
            .padding(start=if (isLandscape) 30.dp else 0.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // DJAMES LOGO:
        Image(
            modifier = Modifier
                .width(if (isLandscape) 100.dp else 160.dp)
                .height(if (isLandscape) 100.dp else 160.dp),
            painter = painterResource(id = R.drawable.djames),
            contentDescription = "DJames logo"
        )
        // MAIN INTRO TEXT:
        Text(
            modifier = Modifier
                .padding(top=8.dp, bottom=if (!isLandscape) 20.dp else 0.dp),
            text = "Hi ${if (preview) "Sir" else genderState}! How can I help you?",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            color = colorResource(id = R.color.light_grey),
        )
        if (isLandscape) {
            // OPEN GUIDE BUTTON:
            OpenGuideButton(
                context = context,
                navController = navController,
            )
        }
    }
}


@Composable
fun RightSide(
    modifier: Modifier = Modifier,
    context: Context,
    navController: NavController,
    requestPermissions: MutableState<Boolean>,
    requestOverlayOn: MutableState<Boolean>,
    isLandscape: Boolean,
    preview: Boolean = false
) {
    val focusManager = LocalFocusManager.current
    val chatText = rememberSaveable { mutableStateOf("") }
    val overlayActiveState by overlayActive.observeAsState()

    Column(
        modifier = modifier
            .padding(end = if (isLandscape) 30.dp else 0.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // CHAT INPUT FIELD:
        ChatInputField(
            context = context,
            requestPermissions = requestPermissions,
            requestOverlayOn = requestOverlayOn,
            modifier = Modifier
                .padding(
                    start = 32.dp,
                    end = 24.dp,
                    top = 6.dp,
                    bottom = 2.dp
                )
                .imePadding()
                .fillMaxWidth(),
            textState = chatText,   //TODO: CENTRALIZE!
            placeholder = "Ask me anything...",
            enableLeftButton = false,
            onSend = { }   //TODO
        )
        // DRIVE INTRO TEXT:
        Text(
            modifier = Modifier
                .padding(top=24.dp, bottom=0.dp),
            text = if (overlayActiveState!!) "Not driving?" else "Are you driving?",
            fontSize = 16.sp,
            fontStyle = FontStyle.Italic,
            color = colorResource(id = R.color.light_grey),
        )
        // DRIVE MODE BUTTON:
        DriveModeButton(
            context = context,
            requestPermissions = requestPermissions,
            requestOverlayOn = requestOverlayOn,
            overlayActiveState = overlayActiveState!!
        )
        if (!isLandscape) {
            // OPEN GUIDE BUTTON:
            OpenGuideButton(
                context = context,
                navController = navController,
            )
        }
    }

}


@Composable
fun DriveModeButton(
    context: Context,
    requestPermissions: MutableState<Boolean>,
    requestOverlayOn: MutableState<Boolean>,
    overlayActiveState: Boolean
) {

    CardSign(
        modifier = Modifier
            .padding(top = 20.dp)
            .clickable {
                utils.startStopDriveMode(
                    context = context,
                    requestOverlayOn = requestOverlayOn,
                    requestPermissions = requestPermissions,
                    openClock = true,
                )
            },
        backgroundColor = if (overlayActiveState) {
            colorResource(id = R.color.colorStop)
        } else {
            colorResource(id = R.color.colorAccent)
        },
        borderColor = colorResource(id = R.color.mid_grey),
        borderWidth = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {

            // DRIVE ICON
            DriveIcon(
                iconSize = 36.dp,
                showForbidden = overlayActiveState
            )
            // TEXT:
            Column(
                modifier = Modifier
                    .padding(top = 4.dp, bottom = 4.dp, start = 12.dp, end = 12.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    modifier = Modifier,
                    color = colorResource(id = R.color.light_grey),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    text = if (overlayActiveState) "Stop\nDrive mode" else "Open\nDrive mode"
                )
            }
            // GO ICON:
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                tint = colorResource(R.color.light_grey),
                contentDescription = "Go"
            )
        }
    }
}


@Composable
fun OpenGuideButton(
    context: Context,
    navController: NavController,
) {
    val extraOpenState by extraOpen.observeAsState()

    CardSign(
        modifier = Modifier
            .padding(top = 28.dp)
            .clickable {
                //Navigate:
                val curNavRoute = NavigationItem.Guide.route
                if (curNavRoute == lastNavRoute && (extraOpenState!!)) {
                    navController.popBackStack()
                } else {
                    navigateTo(navController, curNavRoute)
                }
                lastNavRoute = curNavRoute
            },
        backgroundColor = colorResource(id = R.color.dark_grey),
        borderColor = colorResource(id = R.color.faded_grey),
        borderWidth = 1.5.dp,
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // MAIN:
            Column(
                modifier = Modifier
                    .padding(top = 4.dp, bottom = 4.dp, start = 8.dp, end = 8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier,
                    color = colorResource(id = R.color.light_grey),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    text = "❔ What I can do:"
                )
                Row(
                    modifier = Modifier
                        .padding(top=8.dp, bottom=2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(start=2.dp, end=2.dp),
                        painter = guideIconSelector("calls"),
                        tint = guideColorSelectorLight("calls"),
                        contentDescription = "Go"
                    )
                    Icon(
                        modifier = Modifier
                            .padding(start=2.dp, end=2.dp),
                        painter = guideIconSelector("messages"),
                        tint = guideColorSelectorLight("messages"),
                        contentDescription = "Go"
                    )
                    Icon(
                        modifier = Modifier
                            .padding(start=2.dp, end=2.dp),
                        painter = guideIconSelector("play"),
                        tint = guideColorSelectorLight("play"),
                        contentDescription = "Go"
                    )
                    Icon(
                        modifier = Modifier
                            .padding(start=2.dp, end=2.dp),
                        painter = guideIconSelector("routes"),
                        tint = guideColorSelectorLight("routes"),
                        contentDescription = "Go"
                    )
                }
            }
            // GO ICON:
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                tint = colorResource(R.color.light_grey),
                contentDescription = "Go"
            )
        }
    }
}
