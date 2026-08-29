package com.ftrono.DJames.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ftrono.DJames.R
import com.ftrono.DJames.application.utils


// SETTINGS UI
@Composable
fun StaticCard(
    modifier: Modifier,
    roundedCorners: Dp = 14.dp,
    title: String,
    backgroundColor: Color,
    iconPainter: Painter? = null,
    iconVector: ImageVector? = null,
    fromExpandable: Boolean = false,
    cornerButton: @Composable () -> Unit = {},
    content: @Composable () -> Unit = {}
) {
    // CARD:
    CardSign (
        modifier = modifier,
        backgroundColor = backgroundColor,
        roundedCorners = roundedCorners,
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
                val iconSize = 28.dp
                val iconColor = colorResource(R.color.light_grey)

                if (iconVector != null) {
                    Icon(
                        modifier = Modifier
                            .size(iconSize),
                        contentDescription = title,
                        imageVector = iconVector,
                        tint = iconColor
                    )
                } else {
                    Icon(
                        modifier = Modifier
                            .size(iconSize),
                        contentDescription = title,
                        painter = iconPainter!!,
                        tint = iconColor
                    )
                }
                Text(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(1F),
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorResource(id = R.color.light_grey),
                )
                cornerButton()
            }
            if (fromExpandable) {
                content()
            } else {
                Column(
                    modifier = Modifier
                        .padding(top=16.dp, bottom=2.dp, start=2.dp, end=2.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center,
                ) {
                    content()
                }
            }
        }
    }
}


@Composable
fun ExpandableCard(
    modifier: Modifier,
    roundedCorners: Dp = 14.dp,
    id: String,
    title: String,
    backgroundColor: Color,
    iconPainter: Painter? = null,
    iconVector: ImageVector? = null,
    expandedStates: SnapshotStateMap<String, Boolean>,
    currentExpanded: MutableState<String>,
    useCustomCornerButton: Boolean = false,
    cornerButton: @Composable () -> Unit = {},
    useCustomOnClick: Boolean = false,
    onClick: () -> Unit = {},
    content: @Composable () -> Unit = {}
) {
    utils.updateStatesMap(expandedStates, target = currentExpanded.value)
    // CARD:
    StaticCard(
        modifier = modifier
            .clickable {
                if (currentExpanded.value == id) {
                    if (useCustomOnClick) {
                        // Custom:
                        onClick()
                    } else {
                        // Collapse:
                        currentExpanded.value = ""
                        utils.updateStatesMap(expandedStates, target = currentExpanded.value)
                    }
                } else {
                    // Expand:
                    currentExpanded.value = id
                    utils.updateStatesMap(expandedStates, target = currentExpanded.value)
                }
            },
        roundedCorners = roundedCorners,
        title = title,
        backgroundColor = backgroundColor,
        iconPainter = iconPainter,
        iconVector = iconVector,
        fromExpandable = true,
        cornerButton = {
            val iconSize = 28.dp
            if (currentExpanded.value == id) {
                if (useCustomCornerButton) {
                    cornerButton()
                } else {
                    Icon(
                        modifier = Modifier
                            .size(iconSize),
                        painter = painterResource(R.drawable.arrow_up),
                        tint = colorResource(id = R.color.light_grey),
                        contentDescription = "Expand / collapse"
                    )
                }
            } else {
                Icon(
                    modifier = Modifier
                        .size(iconSize),
                    painter = painterResource(R.drawable.arrow_down),
                    tint = colorResource(id = R.color.light_grey),
                    contentDescription = "Expand / collapse"
                )
            }
        }
    ) {
        // ON EXPANSION:
        AnimatedVisibility(
            modifier = Modifier
                .fillMaxWidth(),
            visible = expandedStates[id]!!
        ) {
            Column(
                modifier = Modifier
                    .padding(top=16.dp, bottom=2.dp, start=2.dp, end=2.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center,
            ) {
                content()
            }
        }
    }
}


@Composable
fun ExtServiceLoginButton(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    loggedInState: Boolean,
    label: String = "",
    showIcon: Boolean = true,
    onClick: () -> Unit = {}
) {
    //SPOTIFY LOGIN STATUS:
    //Logged in text:
    CardSign(
        modifier = modifier
            .clickable {
                onClick()
            },
        backgroundColor = if (loggedInState) colorResource(R.color.faded_grey) else backgroundColor,
        borderColor = colorResource(R.color.transparent_full),
        borderWidth = 0.dp,
    ) {
        Row (
            modifier = Modifier,
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showIcon) {
                //Icon:
                Icon(
                    modifier = Modifier
                        .padding(12.dp)
                        .size(20.dp),
                    imageVector = if (loggedInState) Icons.Default.Close else Icons.AutoMirrored.Filled.ExitToApp,
                    tint = colorResource(R.color.light_grey),
                    contentDescription = if (label != "" && loggedInState) label else if (loggedInState) "Disconnect" else "Connect",
                )
            }
            //Text:
            Text(
                modifier = Modifier
                    .padding(
                        top = if (showIcon) 0.dp else 4.dp,
                        bottom = if (showIcon) 0.dp else 4.dp,
                        start = if (showIcon) 0.dp else 12.dp,
                        end = 12.dp
                    ),
                text = if (label != "" && loggedInState) label else if (loggedInState) "Disconnect" else "Connect",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.light_grey),
            )
        }
    }
}


@Composable
fun ExtServiceAccountItem(
    modifier: Modifier = Modifier,
    name: String,
    backgroundColor: Color,
    iconPainter: Painter,
    loggedInState: Boolean,
    userNameState: String,
    onClick: () -> Unit = {},
) {
    // EXT SERVICE:
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo:
        Image(
            modifier = Modifier
                .padding(end = 8.dp)
                .size(28.dp),
            painter = iconPainter,
            contentDescription = name,
            colorFilter = if (loggedInState) {
                ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1f) })
            } else {
                ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
            }
        )
        // Labels:
        Column () {
            Text(
                text = name,
                color = colorResource(id = R.color.light_grey),
                textAlign = TextAlign.Start,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (loggedInState) userNameState else "Not connected",
                color = colorResource(id = R.color.light_grey),
                textAlign = TextAlign.Start,
                fontSize = 10.sp,
            )
        }
        Spacer(Modifier.weight(1f))
        // Connect / Disconnect:
        ExtServiceLoginButton(
            modifier = Modifier,
            backgroundColor = backgroundColor,
            loggedInState = loggedInState,
            onClick = {
                onClick()
            }
        )
    }
}
