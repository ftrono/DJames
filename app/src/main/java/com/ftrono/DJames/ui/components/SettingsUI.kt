package com.ftrono.DJames.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ftrono.DJames.R


// SETTINGS UI

@Composable
fun SettingsSection(
    modifier: Modifier = Modifier,
    title: String = "",
    subtitle: String = "",
    signColor: Color? = null,
    iconPainter: Painter? = null,
    iconVector: ImageVector? = null,
    content: @Composable () -> Unit
) {
    //TITLE:
    if (title != "") {
        SectionTitle(
            modifier = modifier
                .padding(end = 8.dp, top = 16.dp, bottom = 8.dp),
            title = title,
            subtitle = subtitle,
            signColor = signColor!!,
            iconPainter = iconPainter,
            iconVector = iconVector,
        )
    }
    CardContainer() {
        content()
    }
}


@Composable
fun CardContainer(
    modifier: Modifier = Modifier,
    internalPadding: Dp = 20.dp,
    roundedCorners: Dp = 20.dp,
    containerColor: Color = colorResource(id = R.color.dark_grey_background),
    content: @Composable () -> Unit
) {
    //CARD:
    Card(
        modifier = modifier
            .fillMaxWidth(),
        border = BorderStroke(1.dp, colorResource(id = R.color.dark_grey)),
        shape = RoundedCornerShape(roundedCorners),
        colors = CardDefaults.cardColors (
            containerColor = containerColor
        )
    ) {
        //SETTINGS LIST:
        Column(
            modifier = Modifier
                .padding(internalPadding),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
        ) {
            content()
        }
    }
}


@Composable
fun ExtServiceLoginButton(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    loggedInState: Boolean,
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
            //Spotify logo:
            Icon(
                modifier = Modifier
                    .padding(12.dp)
                    .size(20.dp),
                imageVector = if (loggedInState) Icons.Default.Close else Icons.AutoMirrored.Filled.ExitToApp,
                tint = colorResource(R.color.light_grey),
                contentDescription = if (loggedInState) "Disconnect" else "Connect",
            )
            //Text:
            Text(
                text = if (loggedInState) "Disconnect" else "Connect",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.light_grey),
                modifier = Modifier
                    .padding(end=12.dp),
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
                .padding(end=8.dp)
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
