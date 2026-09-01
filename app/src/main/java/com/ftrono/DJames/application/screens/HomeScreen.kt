package com.ftrono.DJames.application.screens

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.ftrono.DJames.R
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ftrono.DJames.application.currentCat
import com.ftrono.DJames.application.dialogs.DialogRequestOverlay
import com.ftrono.DJames.application.dialogs.SinglePermissionHandler
import com.ftrono.DJames.application.lastAiMessageText
import com.ftrono.DJames.application.lastNavRoute
import com.ftrono.DJames.application.lastSnapshot
import com.ftrono.DJames.application.lastUserMessageText
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.queryStatus
import com.ftrono.DJames.application.sharedLink
import com.ftrono.DJames.application.spotifyLoggedIn
import com.ftrono.DJames.application.userGender
import com.ftrono.DJames.application.spotUserName
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.timeOfDay
import com.ftrono.DJames.application.userNicknameUI
import com.ftrono.DJames.ui.components.ExpandableCard
import com.ftrono.DJames.ui.components.ExtServiceLoginButton
import com.ftrono.DJames.ui.components.InfoBox
import com.ftrono.DJames.ui.components.LibItemCard
import com.ftrono.DJames.ui.components.RoundedSign
import com.ftrono.DJames.ui.components.StreetUIScaffold
import com.ftrono.DJames.ui.navigation.StreetUITopBar
import com.ftrono.DJames.ui.navigation.UserOptions
import com.ftrono.DJames.ui.navigation.navigateTo
import com.ftrono.DJames.ui.selectors.colorSelector
import com.ftrono.DJames.ui.selectors.iconSelector
import com.ftrono.DJames.ui.theme.NavigationItem
import kotlin.Boolean


@Preview
@Preview(heightDp = 360, widthDp = 800)
@Composable
fun HomeScreenPreview() {
    val navController = rememberNavController()
    HomeScreen(
        navController = navController,
        preview = true,
        previewConv = false,
    )
}

@Composable
fun HomeScreen(
    navController: NavController,
    preview: Boolean = false,
    previewConv: Boolean = false,
) {
    val configuration = LocalConfiguration.current
    val isLandscape by remember { mutableStateOf(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) }
    val mContext = LocalContext.current
    val focusManager = LocalFocusManager.current

    //States:
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

    val leftAreaWidth = 200.dp

    StreetUIScaffold(
        modifier = Modifier
            .clickable(
                // This makes the rest of the screen clear focus on tap
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                focusManager.clearFocus()
            },
        lineDistance = if (isLandscape) leftAreaWidth + 36.dp + 22.dp else 20.dp,
        topBar = {
            StreetUITopBar(
                pretitle = "",
                title = stringResource(R.string.app_title),
                subtitle = "",
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
                .padding(
                    start = 36.dp, end = 20.dp
                )
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLandscape) {
                //DISPLAY HORIZONTALLY:
                Row(
                    modifier = Modifier,
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IntroArea(
                        context = mContext,
                        navController = navController,
                        modifier = Modifier
                            .padding(
                                top = 12.dp, bottom = 12.dp
                            )
                            .width(leftAreaWidth)
                            .fillMaxHeight(),
                        isLandscape = true,
                        spotifyLoggedInState = spotifyLoggedInState!!,
                    )

                    //Street line canvas:
                    Spacer(
                        modifier = Modifier
                            .padding(start = 12.dp, end = 12.dp)
                            .height(20.dp)
                            .width(20.dp)
                    )

                    Column(
                        modifier = Modifier,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        //Conversational section:
                        ConversationalArea(
                            isLandscape = true,
                            preview=previewConv,
                        )

                        // Functional area title:
                        FunctionalArea(
                            context = mContext,
                            navController = navController,
                            isLandscape = true,
                            spotifyLoggedInState = spotifyLoggedInState!!,
                            preview = preview,
                        )
                    }
                }
            } else {
                //DISPLAY VERTICALLY:
                IntroArea(
                    context = mContext,
                    navController = navController,
                    modifier = Modifier
                        .padding(
                            top = 12.dp, bottom = 12.dp
                        )
                        .fillMaxWidth(),
                    isLandscape = false,
                    spotifyLoggedInState = spotifyLoggedInState!!,
                )

                //Conversational section:
                ConversationalArea(
                    isLandscape = false,
                    preview=previewConv,
                )

                // Functional area title:
                FunctionalArea(
                    context = mContext,
                    navController = navController,
                    isLandscape = false,
                    spotifyLoggedInState = spotifyLoggedInState!!,
                    preview = preview,
                )
            }
        }
    }
}


// DJAMES LOGO:
@Composable
fun DJamesLogo(
    context: Context,
    modifier: Modifier,
    spotifyLoggedInState: Boolean,
) {
    val spotNameState by spotUserName.observeAsState()
    // DJames logo:
    Image(
        modifier = modifier
            .clickable {
                val toastText = if (!spotifyLoggedInState) {
                    "Log in from Accounts to unlock music functions!"
                } else {
                    "Logged in to Spotify as: $spotNameState!"
                }
                Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
            },
        painter = painterResource(id = R.drawable.djames),
        contentDescription = "DJames logo"
    )
}


@Composable
fun HomeIntroText(
    navController: NavController,
) {
    val userNameState by userNicknameUI.observeAsState()
    val genderState by userGender.observeAsState()
    val timeOfDayState by timeOfDay.observeAsState()

    Text(
        text = "Good $timeOfDayState,",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = colorResource(id = R.color.light_grey),
    )
    Row(
        modifier = Modifier
            .clickable {
                //Navigate:
                val curNavRoute = NavigationItem.Accounts.route
                navigateTo(navController, curNavRoute)
                lastNavRoute = curNavRoute
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            modifier = Modifier
                .padding(end = 8.dp)
                .size(24.dp),
            contentDescription = "Edit",
            imageVector = Icons.Outlined.Edit,
            tint = colorResource(R.color.mid_grey)
        )
        Text(
            text = if (userNameState == "") genderState!! else prefs.userNickname,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(id = R.color.light_grey),
        )
    }
}

// INTRO AREA:
@Composable
fun IntroArea(
    context: Context,
    navController: NavController,
    modifier: Modifier,
    spotifyLoggedInState: Boolean,
    isLandscape: Boolean,
    preview: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center,
    ) {
        if (isLandscape) {
            // DJames logo:
            DJamesLogo(
                context = context,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .size(60.dp),
                spotifyLoggedInState = spotifyLoggedInState
            )
            // Intro text:
            HomeIntroText(navController)

        } else {
            // DJames row:
            Row(
                modifier = Modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                // Intro text:
                Column(
                    modifier = Modifier
                        .weight(1F),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center,
                ) {
                    HomeIntroText(navController)
                }
                // DJames logo:
                DJamesLogo(
                    context = context,
                    modifier = Modifier
                        .size(70.dp),
                    spotifyLoggedInState = spotifyLoggedInState,
                )
            }
        }

        // Usage tip:
        InfoBox(
            modifier = Modifier
                .padding(top=12.dp, bottom=12.dp),
            backgroundColor = colorResource(R.color.dark_grey_background),
            iconPainter = painterResource(R.drawable.icon_lamp),
        ) {
            Text(
                text = buildAnnotatedString {
                    append("Ask me to ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("play music")
                    }
                    append(", ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("navigate")
                    }
                    append(" to a place, ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("call or message")
                    }
                    append(" your contacts")
                },
                fontSize = 15.sp,
                lineHeight = 15.sp,
                color = colorResource(id = R.color.light_grey),
            )
        }
    }
}


// Conversational section:
@Composable
fun ConversationalArea(
    isLandscape: Boolean,
    preview: Boolean = false,
) {
    val queryState by queryStatus.observeAsState()
    val lastUserMsgState by lastUserMessageText.observeAsState()
    val lastAiMsgState by lastAiMessageText.observeAsState()

    if (preview || queryState == "busy" || queryState == "processing") {
        InfoBox(
            modifier = Modifier
                .padding(
                    top = if (isLandscape) 12.dp else 0.dp,
                    bottom = 12.dp,
                ),
            backgroundColor = colorResource(R.color.brownSignDark),
            iconPainter = painterResource(R.drawable.icon_speak_2),
        ) {
            // Intro:
            if (lastUserMsgState!! != "") {
                Text(
                    modifier = Modifier
                        .padding(top = 12.dp),
                    text = "\"${lastUserMsgState!!}\"",
                    color = colorResource(id = R.color.light_grey),
                    fontSize = 14.sp,
                    lineHeight = 14.sp,
                )
            }
            // Content:
            Text(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 12.dp),
                text = "\"${lastAiMsgState!!}\"",
                fontStyle = FontStyle.Italic,
                color = colorResource(id = R.color.light_grey),
                fontSize = 18.sp,
                lineHeight = 18.sp,
            )
        }
    }
}


// FUNCTIONAL AREA:
@Composable
fun FunctionalArea(
    context: Context,
    navController: NavController,
    spotifyLoggedInState: Boolean,
    isLandscape: Boolean,
    preview: Boolean = false,
) {
    // States:
    val catsStateItems = listOf("spotify", "place", "contact")
    val expandedStates = remember {
        mutableStateMapOf(*catsStateItems.map { it to false }.toTypedArray())
    }
    val currentExpanded = rememberSaveable { mutableStateOf(catsStateItems[0]) }

    Column(
        modifier = Modifier
            .padding(
                top = 12.dp, bottom = 12.dp
            )
            .fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center,
    ) {
        // TITLE:
        Row(
            modifier = Modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            // Title:
            Text(
                modifier = Modifier
                    .weight(1F),
                text = "Library & activity",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.light_grey),
            )
            // Search:
            RoundedSign(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clickable {
                        // TODO: Search
                    },
                signSize = 48.dp,
                contentSize = 24,
                backgroundColor = colorResource(R.color.dark_grey),
                borderColor = colorResource(R.color.transparent_full),
                contentColor = colorResource(R.color.light_grey),
                borderWidth = 2.5.dp,
                iconVector = Icons.Outlined.Search,
            )
            // Chat history:
            RoundedSign(
                modifier = Modifier
                    .clickable {
                        //Navigate:
                        val curNavRoute = NavigationItem.Messages.route
                        navigateTo(navController, curNavRoute)
                        lastNavRoute = curNavRoute
                    },
                signSize = 48.dp,
                contentSize = 24,
                backgroundColor = colorResource(R.color.dark_grey),
                borderColor = colorResource(R.color.transparent_full),
                contentColor = colorResource(R.color.light_grey),
                borderWidth = 2.5.dp,
                iconPainter = painterResource(R.drawable.icon_message),
            )
        }

        // TODO: Show here search results vs sections
        // SECTIONS:
        for (cat in catsStateItems) {
            ContentSection(
                context = context,
                cat = cat,
                navController = navController,
                iconPainter = iconSelector(cat),
                backgroundColor = colorSelector(cat),
                isLandscape = isLandscape,
                expandedStates = expandedStates,
                currentExpanded = currentExpanded,
                spotifyLoggedInState = spotifyLoggedInState,
                preview = preview,
            )
        }
    }
}


@Composable
fun ContentSection(
    context: Context,
    cat: String,
    navController: NavController,
    iconPainter: Painter,
    backgroundColor: Color,
    isLandscape: Boolean,
    expandedStates: SnapshotStateMap<String, Boolean>,
    currentExpanded: MutableState<String>,
    spotifyLoggedInState: Boolean,
    preview: Boolean = false,
) {
    val snapshot by lastSnapshot.observeAsState()
    val columns = if (isLandscape) 6 else 4
    val spacing = 6.dp

    // RECENT ITEMS:
    var recentItems = libUtils.getAll(
        cat = cat,
        subcat = "",
        limit = columns + 2,
        preview = preview,
    )

    // When snapshot changes, reload data:
    LaunchedEffect(snapshot) {
        recentItems = libUtils.getAll(
            cat = cat,
            subcat = "",
            limit = columns + 2,
            preview = preview,
        )
    }

    // CARD:
    ExpandableCard(
        modifier = Modifier
            .padding(top = 12.dp, bottom = 12.dp),
        id = cat,
        title = utils.capitalizeWords(if (cat == "spotify") cat else "${cat}s"),
        backgroundColor = backgroundColor,
        iconPainter = iconPainter,
        expandedStates = expandedStates,
        currentExpanded = currentExpanded,
        useCustomCornerButton = true,
        cornerButton = {
            Icon(
                modifier = Modifier
                    .size(28.dp),
                imageVector = Icons.AutoMirrored.Default.ArrowForward,
                tint = colorResource(id = R.color.light_grey),
                contentDescription = "Open section"
            )
        },
        useCustomOnClick = true,
        onClick = {
            // Set current cat:
            currentCat.postValue(cat)
            //Navigate:
            val curNavRoute = NavigationItem.Library.route
            navigateTo(navController, curNavRoute)
            lastNavRoute = curNavRoute
        },
    ) {
        // Intro row:
        Row(
            modifier = Modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                modifier = Modifier
                    .weight(1F),
                text = "Saved items",
//                text = if (recentItems.isEmpty()) {
//                    "No recent activity"
//                } else if (cat == "spotify") {
//                    "Recently listened"
//                } else {
//                    "Recently used"
//                },
                fontSize = 14.sp,
                // fontWeight = FontWeight.Light,
                color = colorResource(id = R.color.light_grey),
            )
            if (cat == "spotify") {
                // Connect / Disconnect:
                ExtServiceLoginButton(
                    modifier = Modifier,
                    backgroundColor = colorResource(R.color.faded_grey),
                    loggedInState = spotifyLoggedInState,
                    label = "Account",
                    showIcon = false,
                    onClick = {
                        val curNavRoute = NavigationItem.Accounts.route
                        navigateTo(navController, curNavRoute)
                        lastNavRoute = curNavRoute
                    }
                )
            }
        }

        // Content:
        if (recentItems.isNotEmpty()) {
            // RECENT LIST:
            LazyHorizontalGrid(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                rows = GridCells.Fixed(1),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalArrangement = Arrangement.spacedBy(
                    space = spacing,
                    alignment = Alignment.Top
                )
            ) {
                //ITEMS:
                recentItems.forEach { item ->
                    item {
                        //Card:
                        LibItemCard(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(80.dp),
                            item = item,
                            trimChars = 16,
                            selected = false,
                            preview = preview,
                            onClick = {
                                // OPEN LINK:
                                if (item.source == "contact") {
                                    val contactPhone =
                                        "${item.phoneSet!!.prefix}${item.phoneSet!!.phone}"
                                    utils.makeCall(
                                        context,
                                        contactPhone = contactPhone,
                                        fromService = false
                                    )
                                } else {
                                    utils.openLink(context, url = item.url, fromService = false)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}