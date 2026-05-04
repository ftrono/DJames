package com.ftrono.DJames.ui.navigation

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.navigation.NavController
import com.ftrono.DJames.ui.theme.NavigationItem
import com.ftrono.DJames.R
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ftrono.DJames.application.lastNavRoute
import com.ftrono.DJames.application.extraOpen
import com.ftrono.DJames.application.libCats
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.overlayActive
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.models.SelectorItem
import com.ftrono.DJames.ui.components.RoundedSign
import com.ftrono.DJames.ui.selectors.libColorSelectorLight
import com.ftrono.DJames.ui.selectors.libIconSelector


@Composable
fun TopBarMenu(
    backgroundColor: Color? = null,
    contentColor: Color = colorResource(id = R.color.light_grey),
    borderColor: Color = colorResource(id = R.color.mid_grey),
    imageRes: Painter? = null,
    iconPainter: Painter? = null,
    iconVector: ImageVector? = null,
    contentText: String = "",
    imageUrl: String = "",
    moreOnly: Boolean = false,
    onClick: () -> Unit = {},
    optionsMenu: @Composable () -> Unit = {}
) {
    Box() {
        Row(
            modifier = Modifier
                .padding(end = 12.dp)
                .clickable {
                    onClick()
                },
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (moreOnly) {
                // MORE ICON:
                Icon(
                    modifier = Modifier
                        .size(28.dp),
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = colorResource(R.color.light_grey)
                )
            } else {
                // ROUNDED SIGN ICON:
                RoundedSign(
                    modifier = Modifier,
                    signSize = 48.dp,
                    contentSize = if (iconPainter != null) 20 else 24,
                    backgroundColor = backgroundColor ?: colorResource(R.color.dark_grey),
                    borderColor = borderColor,
                    contentColor = contentColor,
                    borderWidth = 2.5.dp,
                    contentText = contentText,
                    imageUrl = imageUrl,
                    imageRes = imageRes,
                    iconPainter = iconPainter,
                    iconVector = iconVector,
                )
                // MORE ICON:
                Icon(
                    modifier = Modifier
                        .size(28.dp),
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = colorResource(R.color.light_grey)
                )
            }
        }
        // OPTIONS MENU:
        optionsMenu()
    }
}


@Preview
@Composable
fun TopBarPreview() {
    StreetUITopBar(
        pretitle = "",
        title = "DJames",
        subtitle = "for user_name",
        showBack = true,
        optionButtons = {
            //TODO
            TopBarMenu(
                backgroundColor = colorResource(R.color.blueSign),
                contentText = "20"
            )
        }
    )
}


//TOP APP BAR:
@Composable
fun StreetUITopBar(
    pretitle: String = "",
    title: String,
    subtitle: String = "",
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    optionButtons: @Composable() (RowScope.() -> Unit) = {}
) {
    //HEADER:
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(65.dp)
            .background(colorResource(id = R.color.windowBackground)),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        //BACK:
        if (showBack) {
            Icon(
                modifier = Modifier
                    .padding(start = 18.dp)
                    .size(30.dp)
                    .clickable {
                        onBack()
                    },
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = colorResource(id = R.color.light_grey)
            )
        }

        //HEADERS TEXT:
        Column(
            modifier = Modifier
                .padding(start = 18.dp, end = 30.dp)
                .weight(1F)
        ) {
            if (pretitle != "") {
                Text(
                    text = pretitle,
                    fontSize = 14.sp,
                    // fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.light_grey),
                )
            }
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.light_grey),
            )
            if (subtitle != "") {
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    // fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.light_grey),
                )
            }
        }

        //OPTIONS BUTTONS:
        optionButtons()
    }
}


@Preview(widthDp = 500)
@Composable
fun TopSplitterBarPreview() {
    // Load splitter cats:
    val libSplitterItems = mutableListOf<SelectorItem>()
    for (cat in libCats) {
        libSplitterItems.add(
            SelectorItem(
                id = cat,
                title = if (cat == "spotify") "Spotify links" else "${utils.capitalizeWords(cat)}s",
                iconPainter = libIconSelector(cat),
                color = libColorSelectorLight(cat),
            )
        )
    }

    val currentCatState = rememberSaveable { mutableStateOf(libCats[0]) }
    TopSplitterBar(
        currentItemState = currentCatState,
        items = libSplitterItems,
        showBack = true,
        optionButtons = {
            //TODO
            TopBarMenu(
                backgroundColor = colorResource(R.color.blueSign),
                contentText = "20"
            )
        }
    )
}



//TOP SPLITTER BAR:
@Composable
fun TopSplitterBar(
    currentItemState: MutableState<String>,
    items: MutableList<SelectorItem>,
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    onNavClick: () -> Unit = {},
    optionButtons: @Composable() (RowScope.() -> Unit) = {}
) {
    //HEADER:
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(65.dp)
            .background(colorResource(id = R.color.windowBackground)),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        //BACK:
        if (showBack) {
            Icon(
                modifier = Modifier
                    .padding(start = 18.dp)
                    .size(30.dp)
                    .clickable {
                        onBack()
                    },
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = colorResource(id = R.color.light_grey)
            )
        }

        //SPLITTER SIGN (bigger weight with margins):
        Row(
            modifier = Modifier
                .weight(1F),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            SplitterSign(
                currentItemState = currentItemState,
                items = items,
                onNavClick = onNavClick,
            )
        }

        //OPTIONS BUTTONS:
        optionButtons()
    }
}

@Composable
fun SplitterSign(
    modifier: Modifier = Modifier,
    currentItemState: MutableState<String>,
    items: MutableList<SelectorItem>,
    iconSize: Dp = 22.dp,
    disabled: Boolean = false,
    onNavClick: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isLandscape by remember { mutableStateOf(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) }

    //BUTTONS:
    Card(
        modifier = modifier,
        border = BorderStroke(2.dp, colorResource(id = R.color.dark_grey)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors (
            containerColor = colorResource(id = R.color.dark_grey_background)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(top = 4.dp, bottom = 4.dp, start = 12.dp, end = 12.dp)
                .scrollable(rememberScrollState(), orientation = Orientation.Horizontal),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            for (item in items) {
                SplitterCat(
                    item = item,
                    selected = currentItemState.value == item.id,
                    isLandscape = isLandscape,
                    iconSize = iconSize,
                    disabled = disabled,
                    onNavClick = {
                        if (!disabled) {
                            currentItemState.value = item.id
                            if (item.useCustomClick) item.onClick() else {
                                onNavClick()
                            }
                        }
                    }
                )
                //DIVIDERS:
                if (item.id != items.last().id) {
                    VerticalDivider(
                        modifier = Modifier
                            .padding(start = 4.dp, end = 4.dp)
                            .height(30.dp),
                        thickness = 2.dp,
                        color = colorResource(id = R.color.dark_grey)
                    )
                }
            }
        }
    }
}


@Composable
fun SplitterCat(
    item: SelectorItem,
    selected: Boolean,
    isLandscape: Boolean = false,
    iconSize: Dp = 22.dp,
    disabled: Boolean = false,
    onNavClick: () -> Unit = {}
){
    Row(
        modifier = Modifier
            .padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 4.dp)   // Before: 8.dp
            .clickable {
                onNavClick()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            //Sign icon:
            if (!disabled && item.useImage) {
                Image(
                    modifier = Modifier
                        .padding(
                            start = if (isLandscape && selected) 4.dp else 6.dp,
                            end = if (isLandscape && selected) 4.dp else 6.dp
                        )
                        .size(if (selected) (iconSize + 4.dp) else iconSize),
                    painter = item.iconPainter!!,
                    contentDescription = item.title,
                )
            } else {
                Icon(
                    modifier = Modifier
                        .padding(
                            start = if (isLandscape && selected) 4.dp else 6.dp,
                            end = if (isLandscape && selected) 4.dp else 6.dp
                        )
                        .size(if (selected) (iconSize + 4.dp) else iconSize),
                    painter = item.iconPainter!!,
                    contentDescription = item.title,
                    tint = if (disabled) {
                            colorResource(id = R.color.mid_grey)
                        } else if (selected || item.disableGray) {
                            item.color!!
                        } else {
                            colorResource(id = R.color.light_grey)
                        }
                )
            }

            if (selected) {
                HorizontalDivider(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .width(30.dp),
                    thickness = 2.dp,
                    color = item.color!!
                )
            }
        }
        //Title:
        if (isLandscape && selected && item.title != "") {
            Text(
                modifier = Modifier
                    .padding(start = 4.dp, end = 6.dp),
                text = item.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = item.color!!,
                maxLines = 1
            )
        }
    }
}


// FILTERS ROW:
@Composable
fun FiltersRow(
    snapshot: MutableState<Long>,
    currentCatState: MutableState<String>,
    currentSubCatState: MutableState<String>,
    preview: Boolean = false,
) {
    var filters = if (currentCatState.value == "spotify") libUtils.getSubcats(currentCatState.value, preview) else listOf()

    // When snapshot changes, reload data
    LaunchedEffect(snapshot.value) {
        filters = if (currentCatState.value == "spotify") libUtils.getSubcats(currentCatState.value, preview) else listOf()
    }

    if (filters.size > 1) {
        Row(
            modifier = Modifier
                .padding(start = 32.dp, end=24.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "ALL":
            AssistChip(
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, colorResource(R.color.dark_grey)),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (currentSubCatState.value == "") {
                        colorResource(R.color.midfaded_grey)
                    } else {
                        colorResource(R.color.windowBackground)
                    }
                ),
                label = {
                    Text(
                        text = "All",
                        fontSize = 12.sp,
                        // fontWeight = FontWeight.Bold,
                        color = colorResource(id = R.color.light_grey)
                    )
                },
                onClick = {
                    currentSubCatState.value = ""
                    snapshot.value = utils.getCurrentTimestamp()   //Refresh list
                }
            )

            //FILTERS:
            Row(
                modifier = Modifier
                    .weight(1F)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (filt in filters) {
                    AssistChip(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, colorResource(R.color.dark_grey)),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (currentSubCatState.value == filt) {
                                colorResource(R.color.midfaded_grey)
                            } else {
                                colorResource(R.color.windowBackground)
                            }
                        ),
                        label = {
                            Text(
                                text = utils.capitalizeWords(filt + "s"),
                                fontSize = 12.sp,
                                // fontWeight = FontWeight.Bold,
                                color = colorResource(id = R.color.light_grey)
                            )
                        },
                        onClick = {
                            currentSubCatState.value = filt
                            snapshot.value = utils.getCurrentTimestamp()   //Refresh list
                        }
                    )
                }
            }
        }
    }
}


@Preview
@Composable
fun MainNavBarPreview1() {
    val navController = rememberNavController()
    val clickCounterState by remember { mutableStateOf(0) }
    val navItemLeft = NavigationItem.Library
    val navItemRight = NavigationItem.Messages
    val items = mutableListOf(
        SelectorItem(
            id = navItemLeft.route,
            title = navItemLeft.title,
            iconPainter = painterResource(navItemLeft.icon),
        ),
        SelectorItem(
            id = navItemRight.route,
            title = navItemRight.title,
            iconPainter = painterResource(navItemRight.icon),
        )
    )

    MainNavBar(
        navController = navController,
        clickCounterState = clickCounterState,
        isLandscape = false,
        items = items,
        preview = true,
    )
}


@Preview(heightDp = 360, widthDp = 100)
@Composable
fun MainNavBarPreview2() {
    val navController = rememberNavController()
    val clickCounterState by remember { mutableStateOf(0) }
    val items = mutableListOf(
        SelectorItem(
            id = "restart",
            title = "Restart",
            iconVector = Icons.Default.Refresh,
        ),
        SelectorItem(
            id = "cancel",
            title = "Cancel",
            iconVector = Icons.Default.Close,
        )
    )

    MainNavBar(
        navController = navController,
        clickCounterState = clickCounterState,
        isLandscape = true,
        items = items,
        preview = true,
    )
}


@Composable
fun NavSideItem(
    modifier: Modifier,
    navController: NavController,
    item: SelectorItem,
) {
    // States:
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val extraOpenState by extraOpen.observeAsState()

    // Colours:
    val itemColor = colorResource(R.color.mid_grey)
    val selectedColor = colorResource(R.color.greenSignLight)

    Column(
        modifier = modifier
            .size(52.dp)
            .clickable {
                if (item.useCustomClick) {
                    //Navigate:
                    val curNavRoute = item.id
                    if (curNavRoute == lastNavRoute && (extraOpenState!!)) {
                        navController.popBackStack()
                    } else {
                        navigateTo(navController, curNavRoute)
                    }
                    lastNavRoute = curNavRoute
                } else {
                    // Custom:
                    item.onClick()
                }
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (item.iconVector != null) {
            Icon(
                modifier = Modifier
                    .size(28.dp),
                imageVector = item.iconVector!!,
                contentDescription = item.title,
                tint = if (item.useCustomClick && currentRoute == item.id) selectedColor else itemColor,
            )
        } else {
            Icon(
                modifier = Modifier
                    .size(28.dp),
                painter = item.iconPainter!!,
                contentDescription = item.title,
                tint = if (item.useCustomClick && currentRoute == item.id) selectedColor else itemColor,
            )
        }
        Text(
            modifier = Modifier
                .padding(top=4.dp),
            text = item.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (item.useCustomClick && currentRoute == item.id) selectedColor else itemColor,
            textAlign = TextAlign.Center,
        )
    }
}


@Composable
fun NavBarContent(
    navController: NavController,
    clickCounterState: Int,
    isLandscape: Boolean,
    backgroundColor: Color,
    items: MutableList<SelectorItem>,
) {
    if (clickCounterState == 0) {
        // LEFT ITEM:
        NavSideItem(
            modifier = Modifier
                .padding(
                    top = if (isLandscape) 14.dp else 0.dp,
                    bottom = if (isLandscape) 14.dp else 0.dp,
                    start = if (isLandscape) 0.dp else 14.dp,
                    end = if (isLandscape) 0.dp else 14.dp,
                ),
            navController = navController,
            item = items[0],
        )

        // Center button (placeholder):
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .width(if (isLandscape) 10.dp else 100.dp)
                .height(if (isLandscape) 100.dp else 10.dp)
                .background(backgroundColor)
            // .border(width = 2.dp, color = colorResource(R.color.midfaded_grey), shape = CircleShape),
        )

        // RIGHT ITEM:
        NavSideItem(
            modifier = Modifier
                .padding(
                    top = if (isLandscape) 14.dp else 0.dp,
                    bottom = if (isLandscape) 14.dp else 0.dp,
                    start = if (isLandscape) 0.dp else 14.dp,
                    end = if (isLandscape) 0.dp else 14.dp,
                ),
            navController = navController,
            item = items[1],
        )
    }
}


@Composable
fun MainNavBar(
    navController: NavController,
    clickCounterState: Int,
    isLandscape: Boolean,
    items: MutableList<SelectorItem>,
    preview: Boolean = false,
    onClickCenter: () -> Unit = {},
) {
    // States:
    val overlayActiveState by overlayActive.observeAsState()

    // Colours:
    val backgroundColor = colorResource(R.color.dark_grey_background)

    // Background:
    Box(
        modifier = if (isLandscape) {
            Modifier
                .fillMaxHeight()
                .background(colorResource(R.color.windowBackground))
        } else {
            Modifier
                .fillMaxWidth()
                .background(colorResource(R.color.windowBackground))
            },
        contentAlignment = Alignment.Center,
    ) {
        // Selector:
        Card(
            modifier = if (isLandscape) {
                Modifier
                    .padding(top = 24.dp, bottom = 24.dp, start = 0.dp, end = 0.dp)
                    .fillMaxHeight()
            } else {
                Modifier
                    .padding(top = 0.dp, bottom = 0.dp, start = 32.dp, end = 32.dp)
                    .fillMaxWidth()
            },
            border = BorderStroke(2.dp, colorResource(id = R.color.dark_grey)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor,
            )
        ) {
            if (isLandscape) {
                // LANDSCAPE MODE:
                Column(
                    modifier = Modifier
                        .padding(top = 14.dp, bottom = 14.dp, start = 8.dp, end = 8.dp)
                        .width(52.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NavBarContent(
                        navController = navController,
                        clickCounterState = clickCounterState,
                        isLandscape = true,
                        backgroundColor = backgroundColor,
                        items = items,
                    )
                }

            } else {
                // PORTRAIT MODE:
                Row(
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 8.dp, start = 14.dp, end = 14.dp)
                        .height(52.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    NavBarContent(
                        navController = navController,
                        clickCounterState = clickCounterState,
                        isLandscape = false,
                        backgroundColor = backgroundColor,
                        items = items,
                    )
                }
            }
        }
        // Center button (placeholder):
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .size(100.dp)
                .background(if (overlayActiveState!!) {
                        colorResource(R.color.colorStop)
                    } else {
                        colorResource(R.color.colorPrimary)
                    })
                .clickable { onClickCenter() },
            contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier
                        .size(50.dp),
                    painter = painterResource(R.drawable.icon_touch),
                    contentDescription = "Cancel",
                    tint = colorResource(R.color.light_grey),
                )
            }
    }
}