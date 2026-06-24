package com.ftrono.DJames.application.screens

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.ftrono.DJames.R
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ftrono.DJames.application.curNavId
import com.ftrono.DJames.application.dialogs.DialogRequestOverlay
import com.ftrono.DJames.application.dialogs.SinglePermissionHandler
import com.ftrono.DJames.application.guidePosPlaceholder
import com.ftrono.DJames.application.lastAiMessageText
import com.ftrono.DJames.application.lastNavRoute
import com.ftrono.DJames.application.lastUserMessageText
import com.ftrono.DJames.application.overlayActive
import com.ftrono.DJames.application.overlayPos
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.queryStatus
import com.ftrono.DJames.application.sharedLink
import com.ftrono.DJames.application.spotifyLoggedIn
import com.ftrono.DJames.application.userGender
import com.ftrono.DJames.application.spotUserName
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.application.isVolumeUpPreferenceSet
import com.ftrono.DJames.be.agents.chat.ChatManager
import com.ftrono.DJames.be.collections.guideTexts
import com.ftrono.DJames.be.models.SelectorItem
import com.ftrono.DJames.ui.components.CardSign
import com.ftrono.DJames.ui.components.StreetLine
import com.ftrono.DJames.ui.components.StreetUIScaffold
import com.ftrono.DJames.ui.navigation.SharedViewModel
import com.ftrono.DJames.ui.navigation.SplitterSign
import com.ftrono.DJames.ui.navigation.StreetUITopBar
import com.ftrono.DJames.ui.navigation.UserOptions
import com.ftrono.DJames.ui.navigation.navigateTo
import com.ftrono.DJames.ui.selectors.colorSelector
import com.ftrono.DJames.ui.selectors.iconSelector
import com.ftrono.DJames.ui.theme.NavigationItem
import kotlin.Boolean
import kotlin.math.absoluteValue
import kotlin.math.roundToInt


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

    val focusManager = LocalFocusManager.current
    val spotifyLoggedInState by spotifyLoggedIn.observeAsState()
    val queryState by queryStatus.observeAsState()
    val guideItemState = rememberSaveable { mutableStateOf("info") }

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
        hideLine = isLandscape,
        lineDistance = 70.dp,
        topBar = {
            StreetUITopBar(
                pretitle = "",
                title = stringResource(R.string.app_name),
                subtitle = if (!spotifyLoggedInState!!) "Not logged in" else "for ${prefs.spotUserName}",
                showBack = false,
                optionButtons = {
                    UserOptions(
                        context = mContext,
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
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLandscape) {
                //DISPLAY HORIZONTALLY:
                Row(
                    modifier = Modifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column(
                        modifier = Modifier,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        SplitterDriveMode(
                            context = mContext,
                            requestPermissions = requestPermissions,
                            requestOverlayOn = requestOverlayOn,
                        )
                        LogoHome(
                            context = mContext,
                            isLandscape = true,
                            queryState = queryState!!,
                            spotifyLoggedInState = spotifyLoggedInState!!,
                        )
                    }
                    //Street line canvas:
                    StreetLine(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .fillMaxHeight()
                            .width(20.dp)
                    )
                    GuideViewer(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        isLandscape = true,
                        itemState = guideItemState,
                        queryState = queryState!!,
                    )
                }
            } else {
                //DISPLAY VERTICALLY:
                SplitterDriveMode(
                    context = mContext,
                    requestPermissions = requestPermissions,
                    requestOverlayOn = requestOverlayOn,
                )
                LogoHome(
                    context = mContext,
                    isLandscape = false,
                    queryState = queryState!!,
                    spotifyLoggedInState = spotifyLoggedInState!!,
                )
                GuideViewer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    isLandscape = false,
                    itemState = guideItemState,
                    queryState = queryState!!,
                )
            }
        }
    }
}


// SPLITTER DRIVE MODE:
@Composable
fun SplitterDriveMode(
    context: Context,
    requestPermissions: MutableState<Boolean>,
    requestOverlayOn: MutableState<Boolean>,
) {
    val driveModeState = rememberSaveable() { mutableStateOf("mobile") }

    val items = mutableListOf(
        SelectorItem(
            id = "mobile",
            useCustomClick = true,
            onClick = {}
        ),
        SelectorItem(
            id = "car",
            useCustomClick = true,
            onClick = {
                // Open Drive mode:
                driveModeState.value = "mobile"
                utils.startStopDriveMode(
                    context = context,
                    requestOverlayOn = requestOverlayOn,
                    requestPermissions = requestPermissions,
                    openClock = true,
                    startOnly = true,
                )
            }
        )
    )

    // SPLITTER SIGN:
    SplitterSign(
        modifier = Modifier
            .padding(top=24.dp, bottom=8.dp),
        currentItemState = driveModeState,
        items = items,
    )
}


// DJAMES LOGO:
@Composable
fun LogoHome(
    context: Context,
    spotifyLoggedInState: Boolean,
    isLandscape: Boolean,
    queryState: String,
    preview: Boolean = false,
) {
    val overlayActiveState by overlayActive.observeAsState()
    val volumeUpEnabledState by isVolumeUpPreferenceSet.observeAsState()
    val userNameState by spotUserName.observeAsState()
    val genderState by userGender.observeAsState()

    // Pulsating animation:
    val baseSize = if (isLandscape) 200F else 250F
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val animatedSize by if (queryState == "busy" || queryState == "processing") {
        infiniteTransition.animateFloat(
            initialValue = baseSize,
            targetValue = baseSize + 10F,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = ""
        )
    } else {
        rememberUpdatedState(baseSize)
    }

    // CONTENT:
    Box(
        modifier = Modifier
            .padding(top = 12.dp, bottom = 12.dp, start = 32.dp, end = 32.dp)
            .size((baseSize + 20F).dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(animatedSize.roundToInt().dp)
                .clip(CircleShape)
                // .border(BorderStroke(1.dp, colorResource(id = R.color.dark_grey)), CircleShape)
                .background(
                    brush = if (queryState == "busy") {
                        SolidColor(colorResource(R.color.transparent_busy))
                    } else if (queryState == "processing") {
                        SolidColor(colorResource(R.color.dark_grey))
                    } else {
                        Brush.radialGradient(
                            radius = if (isLandscape) 300f else 380f,
                            colors = listOf(
                                colorResource(R.color.colorPrimary), // center
                                colorResource(R.color.transparent_full)   // outer
                            )
                        )
                    },
                    shape = CircleShape
                ),
        )
        Column(
            modifier = Modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // DJames logo:
            Image(
                modifier = Modifier
                    .size(if (isLandscape) 90.dp else 130.dp)
                    .clickable {
                        var toastText = if (overlayActiveState!! && volumeUpEnabledState!!) {
                            "Use the OVERLAY or VOLUME UP / SHUTTER button to speak!"
                        } else if (overlayActiveState!!) {
                            "Use the OVERLAY button to speak!"
                        } else if (!spotifyLoggedInState) {
                            "Log in from Accounts to unlock music functions!"
                        } else {
                            "Logged in to Spotify as: $userNameState!"
                        }
                        Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
                    },
                painter = painterResource(id = R.drawable.djames),
                contentDescription = "DJames logo"
            )

            // Intro text:
            val introText = if (queryState == "busy") {
                "Speak now!"
            } else if (queryState == "processing") {
                "Thinking..."
            } else {
                "Hi ${if (preview) "Sir" else genderState}, I'm DJames,\nyour driving\nassistant!"
            }
            Text(
                modifier = Modifier,
                text = introText,
                fontSize = if (isLandscape) 16.sp else 20.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                color = if (queryState == "busy") {
                    colorResource(R.color.white)
                } else colorResource(id = R.color.light_grey),
                textAlign = TextAlign.Center,
                lineHeight = if (isLandscape) 16.sp else 20.sp,
            )
        }
    }
}


// GUIDE VIEWER:
@Composable
fun GuideViewer(
    modifier: Modifier = Modifier,
    itemState: MutableState<String>,
    isLandscape: Boolean,
    queryState: String,
) {
    val lastUserMsgState by lastUserMessageText.observeAsState()
    val lastAiMsgState by lastAiMessageText.observeAsState()

    val items = mutableListOf(
        SelectorItem(
            id = "info",
            disableGray = true,
            useCustomClick = true,
            onClick = {}
        ),
        SelectorItem(
            id = "music",
            useImage = true,
            useCustomClick = true,
            onClick = {}
        ),
        SelectorItem(
            id = "phone",
            disableGray = true,
            useCustomClick = true,
            onClick = {}
        ),
        SelectorItem(
            id = "messages",
            disableGray = true,
            useCustomClick = true,
            onClick = {}
        ),
        SelectorItem(
            id = "gmaps",
            useImage = true,
            useCustomClick = true,
            onClick = {}
        ),
    )

    // SPLITTER SIGN:
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SplitterSign(
            modifier = Modifier
                .padding(top=24.dp),
            currentItemState = itemState,
            items = items,
            disabled = (queryState == "busy" || queryState == "processing"),
        )

        if (queryState != "busy" && queryState != "processing") {
            // CARDS CAROUSEL:
            GuideCardsCarousel(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 20.dp)
                    .fillMaxWidth()
                    .weight(1f),
                items = items,
                itemState = itemState,
                isLandscape = isLandscape,
            )
        } else {
            // CONTAINER:
            CardSign(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 20.dp, start = 32.dp, end = 32.dp)
                    .fillMaxWidth()
                    .weight(1f),
                borderColor = colorResource(id = R.color.dark_grey),
                borderWidth = 4.dp,
                backgroundColor = colorResource(R.color.dark_grey_background),
            ) {
                Column(
                    modifier = Modifier
                        .padding(start = 20.dp, end = 20.dp)
                        .weight(1F)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Intro:
                    if (lastUserMsgState!! != "") {
                        Text(
                            modifier = Modifier
                                .padding(top = 12.dp),
                            text = lastUserMsgState!!,
                            textAlign = TextAlign.Center,
                            color = colorResource(id = R.color.light_grey),
                            fontSize = 14.sp,
                            lineHeight = 14.sp,
                        )
                    }
                    // Content:
                    Text(
                        modifier = Modifier
                            .padding(top = 12.dp, bottom = 12.dp),
                        text = lastAiMsgState!!,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        color = colorResource(id = R.color.light_grey),
                        fontSize = 18.sp,
                        lineHeight = 18.sp,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GuideCardsCarousel(
    modifier: Modifier = Modifier,
    items: MutableList<SelectorItem>,
    itemState: MutableState<String>,
    isLandscape: Boolean,
) {
    // States:
    val overlayPosState by overlayPos.observeAsState()
    val loggedInState by spotifyLoggedIn.observeAsState()
    val pagerState = rememberPagerState { items.size }
    val guideIds = items.map{it.id}

    // On page swipe:
    LaunchedEffect(pagerState.currentPage) {
        itemState.value = items[pagerState.currentPage].id
    }
    // On guide icon tap:
    LaunchedEffect(itemState.value) {
        val newPage = guideIds.indexOf(itemState.value)
        if ((newPage - pagerState.currentPage).absoluteValue == 1) {
            pagerState.animateScrollToPage(newPage)
        } else {
            pagerState.scrollToPage(newPage)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        pageSpacing = 10.dp,
        contentPadding = PaddingValues(horizontal = 30.dp)
    ) { page ->
        val curItem = items[page].id
        val guideText = guideTexts[curItem]!!
        val guideIntro = guideText.intro
        val guideOutro = if (guideText.outro.contains(guidePosPlaceholder)) {
            when {
                (isLandscape && overlayPosState!! == "Right") -> {
                    guideText.outro.replace(guidePosPlaceholder, "on the right")
                }

                (isLandscape) -> {
                    guideText.outro.replace(guidePosPlaceholder, "on the left")
                }

                else -> {
                    guideText.outro.replace(guidePosPlaceholder, "below")
                }
            }
        } else guideText.outro

        // CONTAINER:
        CardSign (
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val pageOffset = pagerState
                        .getOffsetDistanceInPages(page)
                        .absoluteValue
                    lerp(
                        start = 75.dp,
                        stop = 100.dp,
                        fraction = 1f - pageOffset.coerceIn(0f, 1f)
                    ).also { scale ->
                        scaleY = scale / 100.dp
                    }
                }
                .fillMaxHeight(),
            borderColor = colorResource(id = R.color.mid_grey),
            borderWidth = 4.dp,
            backgroundColor = colorSelector(curItem),
        ) {
            Row(
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp)
                    .fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    modifier = Modifier
                        .size(52.dp),
                    painter = iconSelector(curItem),
                    contentDescription = "Item image",
                    tint = colorResource(R.color.light_grey)
                )

                Column(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .weight(1F)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Intro:
                    if (guideIntro != "") {
                        Text(
                            modifier = Modifier
                                .padding(top = 12.dp),
                            text = guideIntro,
                            textAlign = TextAlign.Center,
                            color = colorResource(id = R.color.light_grey),
                            fontSize = 14.sp,
                            lineHeight = 14.sp,
                        )
                    }
                    // Content:
                    Text(
                        modifier = Modifier
                            .padding(top = 12.dp, bottom = 12.dp),
                        text = guideText.content,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        color = colorResource(id = R.color.light_grey),
                        fontSize = 18.sp,
                        lineHeight = 18.sp,
                    )
                    // Outro:
                    Text(
                        modifier = Modifier
                            .padding(bottom = 12.dp),
                        text = if (!loggedInState!! && curItem == "music") {
                            "$guideOutro\nLog in from Accounts to unlock music functions!"
                        } else guideOutro,
                        textAlign = TextAlign.Center,
                        color = colorResource(id = R.color.light_grey),
                        fontSize = 14.sp,
                        lineHeight = 14.sp,
                    )
                }
            }
        }
    }
}