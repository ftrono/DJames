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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.text.style.TextAlign
import com.ftrono.DJames.R
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ftrono.DJames.application.chatInputPlaceholder
import com.ftrono.DJames.application.curNavId
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
import com.ftrono.DJames.application.volumeUpEnabledUI
import com.ftrono.DJames.be.chat.ChatManager
import com.ftrono.DJames.ui.components.CardSign
import com.ftrono.DJames.ui.components.ChatInputField
import com.ftrono.DJames.ui.components.DriveIcon
import com.ftrono.DJames.ui.components.StreetUIScaffold
import com.ftrono.DJames.ui.navigation.SharedViewModel
import com.ftrono.DJames.ui.navigation.StreetUITopBar
import com.ftrono.DJames.ui.navigation.UserOptions
import com.ftrono.DJames.ui.navigation.navigateTo
import com.ftrono.DJames.ui.selectors.guideIconSelector
import com.ftrono.DJames.ui.theme.NavigationItem
import kotlin.Boolean


@Preview
@Preview(heightDp = 360, widthDp = 800)
@Composable
fun HomeScreenPreview() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val chatManager = ChatManager(context)
    chatManager.init()
    val sharedViewModel: SharedViewModel = viewModel()
    HomeScreen(navController, chatManager, sharedViewModel, preview = true)
}

@Composable
fun HomeScreen(
    navController: NavController,
    chatManager: ChatManager,
    sharedViewModel: SharedViewModel,
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
    if (sharedLinkState != "" && curNavId != 0) {
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
                Row(
                    modifier = Modifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    LogoHome(
                        context = mContext,
                        modifier = Modifier
                            .padding(bottom = 10.dp, end = 20.dp)
                            .width(90.dp)
                            .height(90.dp),
                        spotifyLoggedInState = spotifyLoggedInState!!,
                    )
                    HomeIntroText(
                        modifier = Modifier
                            .padding(bottom = 10.dp),
                        isLandscape = isLandscape,
                        preview = preview,
                    )
                    VerticalDivider(
                        modifier = Modifier
                            .height(100.dp)
                            .padding(start = 32.dp, end = 32.dp, bottom = 10.dp),
                        color = colorResource(id = R.color.faded_grey)
                    )
                    MainGuideInfo(
                        modifier = Modifier
                            .padding(bottom = 10.dp),
                        navController = navController,
                        isLandscape = isLandscape,
                    )
                }
                // CHAT INPUT FIELD:
                HomeChatWrapper(
                    context = mContext,
                    requestPermissions = requestPermissions,
                    requestOverlayOn = requestOverlayOn,
                    navController = navController,
                    chatManager = chatManager,
                    sharedViewModel = sharedViewModel,
                )
                // BUTTONS ROW:
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    DriveModeButton(
                        modifier = Modifier.padding(end=20.dp),
                        context = mContext,
                        requestPermissions = requestPermissions,
                        requestOverlayOn = requestOverlayOn,
                        isLandscape = isLandscape,
                        overlayActiveState = overlayActiveState!!
                    )
                }
            } else {
                //DISPLAY VERTICALLY:
                LogoHome(
                    context = mContext,
                    modifier = Modifier
                        .padding(bottom = 20.dp)
                        .width(160.dp)
                        .height(160.dp),
                    spotifyLoggedInState = spotifyLoggedInState!!,
                )
                HomeIntroText(
                    modifier = Modifier,
                    isLandscape = isLandscape,
                    preview = preview,
                )
                HorizontalDivider(
                    modifier = Modifier
                        .width(190.dp)
                        .padding(top=18.dp, bottom = 18.dp),
                    color = colorResource(id = R.color.faded_grey)
                )
                MainGuideInfo(
                    modifier = Modifier
                        .padding(bottom=20.dp),
                    navController = navController,
                    isLandscape = isLandscape,
                )
                HomeChatWrapper(
                    context = mContext,
                    requestPermissions = requestPermissions,
                    requestOverlayOn = requestOverlayOn,
                    navController = navController,
                    chatManager = chatManager,
                    sharedViewModel = sharedViewModel,
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
    val volumeUpEnabledState by volumeUpEnabledUI.observeAsState()

    Image(
        modifier = modifier
            .clickable {
                var toastText = if (overlayActiveState!! && volumeUpEnabledState!!) {
                    "Use the OVERLAY or VOLUME UP / SHUTTER button to speak!"
                } else if (overlayActiveState!!) {
                    "Use the OVERLAY button to speak!"
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
        text = "Hi ${if (preview) "Sir" else genderState}! I'm DJames,\nyour car assistant!",
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        fontStyle = FontStyle.Italic,
        color = colorResource(id = R.color.light_grey),
        textAlign = if (isLandscape) TextAlign.Start else TextAlign.Center,
        lineHeight = 22.sp,
    )
}


@Composable
fun MainGuideInfo(
    modifier: Modifier = Modifier,
    navController: NavController,
    isLandscape: Boolean,
) {
    val extraOpenState by extraOpen.observeAsState()
    
    Column(
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
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = if (isLandscape) Alignment.Start else Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier,
            text = "Ask me to:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(id = R.color.light_grey),
            textAlign = if (isLandscape) TextAlign.Start else TextAlign.Center,
        )
        Text(
            modifier = Modifier
                .padding(top=6.dp),
            text = "Play music via Spotify,\nget driving directions,\ncall or message your contacts!",   //TOOD: add "or YouTube"
            fontSize = 14.sp,
            color = colorResource(id = R.color.light_grey),
            textAlign = if (isLandscape) TextAlign.Start else TextAlign.Center,
            lineHeight = 16.sp,
        )
        // Icons:
        GuideIconsRow(
            modifier = Modifier
                .padding(top=12.dp),
        )
    }
}


// CHAT INPUT FIELD:
@Composable
fun HomeChatWrapper(
    context: Context,
    requestPermissions: MutableState<Boolean>,
    requestOverlayOn: MutableState<Boolean>,
    navController: NavController,
    chatManager: ChatManager,
    sharedViewModel: SharedViewModel
) {

    ChatInputField(
        context = context,
        sharedViewModel = sharedViewModel,
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
        placeholder = chatInputPlaceholder,
        enableLeftButton = false,
        onSend = {
            val curText = sharedViewModel.text.trim()
            if (curText != "") {
                navigateTo(navController, NavigationItem.Messages.route)
                chatManager.processQuery(curText)
                sharedViewModel.text = ""
            }
        }
    )
}


@Composable
fun DriveModeContent(
    context: Context,
    requestPermissions: MutableState<Boolean>,
    requestOverlayOn: MutableState<Boolean>,
    isLandscape: Boolean,
    overlayActiveState: Boolean
) {
    val spacer = remember { mutableStateOf( if (isLandscape) " " else "\n" ) }

    // DRIVE INTRO TEXT:
    Text(
        modifier = Modifier
            .padding(end = if (isLandscape) 20.dp else 0.dp),
        text = if (overlayActiveState) "Not driving?" else "Are you driving?",
        fontSize = 14.sp,
        color = colorResource(id = R.color.light_grey),
    )

    CardSign(
        modifier = Modifier
            .padding(top = if (isLandscape) 0.dp else 10.dp)
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
                    text = if (overlayActiveState) "Close${spacer.value}Drive mode" else "Open${spacer.value}Drive mode",
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
fun DriveModeButton(
    modifier: Modifier = Modifier,
    context: Context,
    requestPermissions: MutableState<Boolean>,
    requestOverlayOn: MutableState<Boolean>,
    isLandscape: Boolean,
    overlayActiveState: Boolean
) {

    if (isLandscape) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DriveModeContent(
                context = context,
                requestPermissions = requestPermissions,
                requestOverlayOn = requestOverlayOn,
                isLandscape = isLandscape,
                overlayActiveState = overlayActiveState,
            )
        }
    } else {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DriveModeContent(
                context = context,
                requestPermissions = requestPermissions,
                requestOverlayOn = requestOverlayOn,
                isLandscape = isLandscape,
                overlayActiveState = overlayActiveState,
            )
        }
    }

}

@Composable
fun GuideIconsRow(
    modifier: Modifier = Modifier,
    iconSize: Dp = 20.dp,
    iconSpacing: Dp = 4.dp,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Image(
            modifier = Modifier
                .padding(start = iconSpacing, end = iconSpacing)
                .size(iconSize),
            painter = painterResource(R.drawable.logo_spotify),
            contentDescription = "Spotify logo"
        )
//        Image(
//            modifier = Modifier
//                .padding(start = iconSpacing, end = iconSpacing)
//                .size(iconSize),
//            painter = painterResource(R.drawable.logo_youtube),
//            contentDescription = "YouTube logo"
//        )
        Image(
            modifier = Modifier
                .padding(start = iconSpacing, end = iconSpacing)
                .size(iconSize),
            painter = painterResource(R.drawable.logo_gmaps),
            contentDescription = "GMaps logo"
        )
        Icon(
            modifier = Modifier
                .padding(start = iconSpacing, end = iconSpacing)
                .size(iconSize),
            painter = guideIconSelector("calls"),
            tint = colorResource(R.color.colorAccentMid),
            contentDescription = "Call icon"
        )
        Icon(
            modifier = Modifier
                .padding(start = iconSpacing, end = iconSpacing)
                .size(iconSize),
            painter = guideIconSelector("messages"),
            tint = colorResource(R.color.blueSignMid),
            contentDescription = "Message icon"
        )
        Image(
            modifier = Modifier
                .padding(start = iconSpacing, end = iconSpacing)
                .size(iconSize),
            painter = painterResource(R.drawable.logo_whatsapp),
            contentDescription = "Whatsapp icon"
        )
        // GO ICON:
        Icon(
            modifier = Modifier
                .padding(start=6.dp),
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            tint = colorResource(R.color.light_grey),
            contentDescription = "Go"
        )
    }
}