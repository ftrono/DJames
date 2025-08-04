package com.ftrono.DJames.application.screens

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.ftrono.DJames.application.chatText
import com.ftrono.DJames.application.dialogs.DialogRequestOverlay
import com.ftrono.DJames.application.dialogs.SinglePermissionHandler
import com.ftrono.DJames.application.extraOpen
import com.ftrono.DJames.application.lastNavRoute
import com.ftrono.DJames.application.overlayActive
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.sharedLink
import com.ftrono.DJames.application.spotifyLoggedIn
import com.ftrono.DJames.application.userGender
import com.ftrono.DJames.application.spotUserName
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.application.volumeUpEnabled
import com.ftrono.DJames.be.chat.ChatManager
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

    val focusManager = LocalFocusManager.current
    val overlayActiveState by overlayActive.observeAsState()
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
        modifier = Modifier
            .clickable(
                // This makes the rest of the screen clear focus on tap
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                focusManager.clearFocus()
            },
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
            if (isLandscape) {
                //DISPLAY HORIZONTALLY:
                HomeIntroText(
                    modifier = Modifier
                        .padding(bottom=20.dp),
                    isLandscape = isLandscape,
                    preview = preview,
                )
                // CHAT INPUT FIELD:
                HomeChatWrapper(
                    context = mContext,
                    requestPermissions = requestPermissions,
                    requestOverlayOn = requestOverlayOn,
                    navController = navController,
                )
                // BUTTONS ROW:
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start=62.dp, top=24.dp)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {

                    LogoHome(
                        context = mContext,
                        modifier = Modifier
                            .width(100.dp)
                            .height(100.dp),
                        spotifyLoggedInState = spotifyLoggedInState!!,
                    )
                    DriveModeButton(
                        modifier = Modifier,
                        context = mContext,
                        requestPermissions = requestPermissions,
                        requestOverlayOn = requestOverlayOn,
                        isLandscape = isLandscape,
                        overlayActiveState = overlayActiveState!!
                    )
                    OpenGuideButton(
                        modifier = Modifier,
                        navController = navController,
                    )
                }
            } else {
                //DISPLAY VERTICALLY:
                LogoHome(
                    context = mContext,
                    modifier = Modifier
                        .padding(bottom=20.dp)
                        .width(160.dp)
                        .height(160.dp),
                    spotifyLoggedInState = spotifyLoggedInState!!,
                )
                HomeIntroText(
                    modifier = Modifier
                        .padding(bottom=20.dp),
                    isLandscape = isLandscape,
                    preview = preview,
                )
                HomeChatWrapper(
                    context = mContext,
                    requestPermissions = requestPermissions,
                    requestOverlayOn = requestOverlayOn,
                    navController = navController,
                )
                DriveModeButton(
                    modifier = Modifier
                        .padding(top=28.dp),
                    context = mContext,
                    requestPermissions = requestPermissions,
                    requestOverlayOn = requestOverlayOn,
                    isLandscape = isLandscape,
                    overlayActiveState = overlayActiveState!!
                )
                OpenGuideButton(
                    modifier = Modifier
                        .padding(top=28.dp),
                    navController = navController,
                )
            }

        }
    }
}


// DJAMES LOGO:
@Composable
fun LogoHome(
    context: Context,
    modifier: Modifier,
    spotifyLoggedInState: Boolean,
) {
    val overlayActiveState by overlayActive.observeAsState()
    val userNameState by spotUserName.observeAsState()
    val volumeUpEnabledState by volumeUpEnabled.observeAsState()

    Image(
        modifier = modifier
            .clickable {
                var toastText = if (overlayActiveState!! && volumeUpEnabledState!!) {
                    "Use the FLOATING button, VOLUME UP, or a remote SHUTTER button!"
                } else if (overlayActiveState!!) {
                    "Use the FLOATING button to record a voice request!"
                } else if (!spotifyLoggedInState) {
                    "Log in from Settings to unlock music functions!"
                } else {
                    "Logged in to Spotify as: $userNameState!"
                }
                Toast.makeText(context, toastText, Toast.LENGTH_LONG) .show()
            },
        painter = painterResource(id = R.drawable.djames),
        contentDescription = "DJames logo"
    )
}


// MAIN INTRO TEXT:
@Composable
fun HomeIntroText(
    modifier: Modifier = Modifier,
    isLandscape: Boolean,
    preview: Boolean = false,
) {
    val genderState by userGender.observeAsState()

    Text(
        modifier = modifier,
        text = "Hi ${if (preview) "Sir" else genderState}! How can I help you?",
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        fontStyle = FontStyle.Italic,
        color = colorResource(id = R.color.light_grey),
    )
}


// CHAT INPUT FIELD:
@Composable
fun HomeChatWrapper(
    context: Context,
    requestPermissions: MutableState<Boolean>,
    requestOverlayOn: MutableState<Boolean>,
    navController: NavController
) {
    val chatManager = ChatManager(context)

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
        placeholder = "Ask me anything...",
        enableLeftButton = false,
        onSend = {
            if (chatText.value!!.trim() != "") {
                navigateTo(navController, NavigationItem.Messages.route)
                val curText = chatText.value!!.trim()
                chatManager.processQuery(curText)
                chatText.postValue("")
            }
        }
    )
}


@Composable
fun DriveModeButton(
    modifier: Modifier = Modifier,
    context: Context,
    requestPermissions: MutableState<Boolean>,
    requestOverlayOn: MutableState<Boolean>,
    isLandscape: Boolean,
    overlayActiveState: Boolean
) {

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // DRIVE INTRO TEXT:
        Text(
            modifier = Modifier,
            text = if (overlayActiveState) "Not driving?" else "Are you driving?",
            fontSize = 16.sp,
            fontStyle = FontStyle.Italic,
            color = colorResource(id = R.color.light_grey),
        )

        CardSign(
            modifier = Modifier
                .padding(top = 10.dp)
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
}


@Composable
fun OpenGuideButton(
    modifier: Modifier = Modifier,
    navController: NavController,
) {
    val extraOpenState by extraOpen.observeAsState()

    CardSign(
        modifier = modifier
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
                    .padding(top = 4.dp, bottom = 4.dp, start = 12.dp, end = 8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // INTRO:
                Text(
                    modifier = Modifier,
                    color = colorResource(id = R.color.light_grey),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    text = "❔ What I can do:"
                )
                // BUTTONS ROW:
                Row(
                    modifier = Modifier
                        .padding(top=8.dp, bottom=4.dp),
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
                        painter = guideIconSelector("car"),
                        tint = guideColorSelectorLight("car"),
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
