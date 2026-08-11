package com.ftrono.DJames.application.screens

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.ftrono.DJames.application.extraOpen
import com.ftrono.DJames.application.lastNavRoute
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.queryStatus
import com.ftrono.DJames.application.sharedLink
import com.ftrono.DJames.application.spotifyLoggedIn
import com.ftrono.DJames.application.userGender
import com.ftrono.DJames.application.spotUserName
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.userNicknameUI
import com.ftrono.DJames.ui.components.CardSign
import com.ftrono.DJames.ui.components.ExtServiceLoginButton
import com.ftrono.DJames.ui.components.LibItemCard
import com.ftrono.DJames.ui.components.RoundedSign
import com.ftrono.DJames.ui.components.StreetLine
import com.ftrono.DJames.ui.components.StreetUIScaffold
import com.ftrono.DJames.ui.navigation.StreetUITopBar
import com.ftrono.DJames.ui.navigation.UserOptions
import com.ftrono.DJames.ui.navigation.navigateTo
import com.ftrono.DJames.ui.selectors.colorSelectorHome
import com.ftrono.DJames.ui.selectors.iconSelector
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
    val focusManager = LocalFocusManager.current
    val spotifyLoggedInState by spotifyLoggedIn.observeAsState()
    val queryState by queryStatus.observeAsState()
    val extraOpenState by extraOpen.observeAsState()


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
        hideLine = isLandscape,
        lineDistance = 20.dp,
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
                            .width(200.dp)
                            .fillMaxHeight(),
                        isLandscape = true,
                        spotifyLoggedInState = spotifyLoggedInState!!,
                    )
                    //Street line canvas:
                    StreetLine(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .fillMaxHeight()
                            .width(20.dp)
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

    Text(
        text = "Good ${utils.getTimeOfDay()},",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = colorResource(id = R.color.light_grey),
    )
    Row(
        modifier = Modifier
            .background(color = colorResource(R.color.windowBackground))
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
                    .padding(bottom=8.dp)
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
        CardSign (
            modifier = Modifier
                .padding(top=12.dp, bottom=12.dp),
            backgroundColor = colorResource(R.color.light_grey),
            roundedCorners = 14.dp,
        ) {
            Row(
                modifier = Modifier
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    modifier = Modifier
                        .size(28.dp),
                    contentDescription = "Usage tip",
                    painter = painterResource(R.drawable.icon_lamp),
                    tint = colorResource(R.color.black)
                )
                Text(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(1F),
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
                    color = colorResource(id = R.color.black),
                )
            }
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
                backgroundColor = colorSelectorHome(cat),
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
    utils.updateStatesMap(expandedStates, target=currentExpanded.value)
    val columns = if (isLandscape) 6 else 4
    val spacing = 6.dp

    // CARD:
    CardSign (
        modifier = Modifier
            .padding(top = 12.dp, bottom = 12.dp)
            .clickable {
                //Update global currentExpanded:
                if (currentExpanded.value == cat) {
                    currentExpanded.value = ""
                } else {
                    currentExpanded.value = cat
                }
                utils.updateStatesMap(expandedStates, target = currentExpanded.value)
            },
        backgroundColor = backgroundColor,
        roundedCorners = 14.dp,
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // TITLE:
            Row(
                modifier = Modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                // Section title:
                Icon(
                    modifier = Modifier
                        .size(20.dp),
                    contentDescription = utils.capitalizeWords(cat),
                    painter = iconPainter,
                    tint = colorResource(R.color.light_grey)
                )
                Text(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(1F),
                    text = utils.capitalizeWords(if (cat == "spotify") cat else "${cat}s"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.light_grey),
                )
                if (currentExpanded.value == cat) {
                    Text(
                        modifier = Modifier
                            .clickable {
                                // Set current cat:
                                currentCat.postValue(cat)
                                //Navigate:
                                val curNavRoute = NavigationItem.Library.route
                                navigateTo(navController, curNavRoute)
                                lastNavRoute = curNavRoute
                            },
                        text = "View saved >",
                        fontSize = 14.sp,
                        color = colorResource(id = R.color.light_grey),
                    )
                } else {
                    Icon(
                        modifier = Modifier
                            .size(20.dp),
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        tint = colorResource(id = R.color.light_grey),
                        contentDescription = "Expand / collapse"
                    )
                }
            }

            // ON EXPANSION:
            AnimatedVisibility(
                modifier = Modifier
                    .fillMaxWidth(),
                visible = expandedStates[cat]!!
            ) {
                // RECENT ITEMS:
                Column(
                    modifier = Modifier
                        .padding(top=16.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center,
                ) {
                    val recentItems = libUtils.getAll(
                        cat=cat,
                        subcat="",
                        limit=columns+2,
                        preview=preview,
                    )

                    // Intro row:
                    Row(
                        modifier = Modifier,
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            modifier = Modifier
                                .weight(1F),
                            text = if (recentItems.isEmpty()) {
                                "No recent activity"
                            } else if (cat == "spotify") {
                                "Recently listened"
                            } else {
                                "Recently used"
                            },
                            fontSize = 14.sp,
                            color = colorResource(id = R.color.light_grey),
                        )
                        if (cat == "spotify") {
                            // Connect / Disconnect:
                            ExtServiceLoginButton(
                                modifier = Modifier,
                                backgroundColor = colorResource(R.color.faded_grey),
                                loggedInState = spotifyLoggedInState,
                                label = "Manage",
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
                        LazyHorizontalGrid (
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            rows = GridCells.Fixed(1),
                            horizontalArrangement = Arrangement.spacedBy(spacing),
                            verticalArrangement = Arrangement.spacedBy(spacing)
                        ) {
                            //ITEMS:
                            recentItems.forEach { item ->
                                item {
                                    //Card:
                                    LibItemCard(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(80.dp),
                                        cardColors = CardDefaults.cardColors(
                                            containerColor = colorResource(id = R.color.transparent_full)
                                        ),
                                        source = item.source,
                                        type = item.type,
                                        title = utils.trimString(item.name, 20),
                                        subtitle = utils.trimString(libUtils.getDetail(item), 16),
                                        imageUrl = if (preview) "" else item.imageUrl,
                                        isCollection = item.id == -2L,
                                        fromHome = true,
                                        onClick = {
                                            // OPEN LINK:
                                            if (item.source == "contact") {
                                                val contactPhone = "${item.phoneSet!!.prefix}${item.phoneSet!!.phone}"
                                                utils.makeCall(context, contactPhone = contactPhone, fromService = false)
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
        }
    }
}