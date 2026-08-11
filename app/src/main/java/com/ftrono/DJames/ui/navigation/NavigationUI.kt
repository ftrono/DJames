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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ftrono.DJames.application.clockActive
import com.ftrono.DJames.application.lastNavRoute
import com.ftrono.DJames.application.extraOpen
import com.ftrono.DJames.application.libCats
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.overlayActive
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.models.SelectorItem
import com.ftrono.DJames.ui.components.RoundedSign
import com.ftrono.DJames.ui.selectors.iconSelector
import com.ftrono.DJames.ui.selectors.colorSelectorLight
import com.ftrono.DJames.ui.selectors.iconSelector


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
    val clockActiveState by clockActive.observeAsState()

    //HEADER:
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(65.dp)
            .background(
                colorResource(if (clockActiveState!!) R.color.black else R.color.windowBackground)
            ),
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
                fontSize = if (pretitle == "" && subtitle == "") 20.sp else 24.sp,
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
                iconPainter = iconSelector(cat),
                color = colorSelectorLight(cat),
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
                    painter = iconSelector(item.id),
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
                    painter = iconSelector(item.id),
                    contentDescription = item.title,
                    tint = if (disabled) {
                            colorResource(id = R.color.mid_grey)
                        } else if (selected || item.disableGray) {
                            colorSelectorLight(item.id)
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
                    color = if (disabled) colorResource(id = R.color.mid_grey) else colorSelectorLight(item.id)
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
                color = colorSelectorLight(item.id),
                maxLines = 1
            )
        }
    }
}


// FILTERS ROW:
@Composable
fun FiltersRow(
    snapshot: MutableState<Long>,
    currentCat: String,
    currentSubCatState: MutableState<String>,
    preview: Boolean = false,
) {
    var filters = libUtils.getSubcats(currentCat, preview)

    // When snapshot changes, reload data
    LaunchedEffect(snapshot.value) {
        filters = libUtils.getSubcats(currentCat, preview)
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
    val clickCounterState by remember { mutableStateOf(0) }
    MainNavBar(
        clickCounterState = clickCounterState,
        isLandscape = false,
        preview = true,
    )
}


@Preview(heightDp = 360, widthDp = 100)
@Composable
fun MainNavBarPreview2() {
    val clickCounterState by remember { mutableStateOf(0) }
    MainNavBar(
        clickCounterState = clickCounterState,
        isLandscape = true,
        preview = true,
    )
}


@Composable
fun StartButton(
    overlayActiveState: Boolean,
    onClickCenter: () -> Unit = {},
) {
    if (overlayActiveState) {
        // Placeholder:
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .size(100.dp)
                .background(colorResource(R.color.windowBackground)),
        )
    } else {
        // Button:
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .size(100.dp)
                .background(
                    colorResource(R.color.faded_grey)
                )
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


@Composable
fun MainNavBar(
    clickCounterState: Int,
    isLandscape: Boolean,
    preview: Boolean = false,
    previewClock: Boolean = false,
    onClickCenter: () -> Unit = {},
) {
    val overlayActiveState by overlayActive.observeAsState()
    val clockActiveState by clockActive.observeAsState()

    // Background:
    Box(
        modifier = if (isLandscape) {
            Modifier
                .fillMaxHeight()
                .width(100.dp)
                .background(
                    colorResource(if (clockActiveState!! || previewClock) R.color.black else R.color.windowBackground)
                )
        } else {
            Modifier
                .fillMaxWidth()
                .height(170.dp)
                .background(
                    colorResource(if (clockActiveState!! || previewClock) R.color.black else R.color.windowBackground)
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (isLandscape) Arrangement.Center else Arrangement.Bottom,
        ) {
            if (clickCounterState == 0 && (!isLandscape || !overlayActiveState!!)) {
                // Text:
                Text(
                    modifier = Modifier
                        .padding(top=12.dp, bottom=12.dp),
                    text = if (!overlayActiveState!!) {
                        "Tap to start"
                    } else {
                        "Keep screen on for voice commands\n" +
                        "Tap or Volume Up to speak"
                    },
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    color = colorResource(id = R.color.mid_grey),
                )
            }
            // Button:
            StartButton(
                overlayActiveState=overlayActiveState!!,
                onClickCenter=onClickCenter,
            )
        }
    }
}