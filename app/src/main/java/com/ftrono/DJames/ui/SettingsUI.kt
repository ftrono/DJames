package com.ftrono.DJames.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ftrono.DJames.R
import com.ftrono.DJames.screen.openLog


// SETTINGS UI

@Preview
@Composable
fun SettingsHeaderPreview() {
    SettingsHeader(
        backClickable = {},
        options = {
            //SAVE BUTTON:
            Icon(
                modifier = Modifier
                    .padding(end = 18.dp)
                    .size(35.dp),
                imageVector = Icons.Default.Check,
                contentDescription = "Save",
                tint = colorResource(id = R.color.colorAccentLight)
            )
        }
    )
}


@Composable
fun SettingsHeader(
    backClickable: () -> Unit,
    options: @Composable () -> Unit
) {
    //HEADER:
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(colorResource(id = R.color.windowBackground)),
        contentAlignment = Alignment.Center
    ) {
        //HEADER CONTENT:
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    //GRADIENT:
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to colorResource(id = R.color.transparent_full),
                            0.3f to colorResource(id = R.color.transparent_full),
                            1f to colorResource(id = R.color.windowBackground)
                        )
                    )
                ),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            //BACK:
            Icon(
                modifier = Modifier
                    .padding(start = 12.dp, end = 4.dp)
                    .size(32.dp)
                    .clickable {
                        backClickable()
                    },
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = colorResource(id = R.color.colorAccentLight)
            )
            //MAIN HEADER SIGN:
            HeaderSign(
                modifier = Modifier
                    .padding(10.dp)
                    .wrapContentSize(align = Alignment.TopStart),
                iconRes = painterResource(id = R.drawable.sign_preferences),
                title = "Preferences"
            )
            //OPTIONS BUTTONS:
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ){
                options()
            }
        }
    }
}


@Composable
fun SettingsSection(
    modifier: Modifier = Modifier,
    title: String,
    signColor: Color,
    iconPainter: Painter,
    content: @Composable () -> Unit
) {
    //TITLE:
    SectionTitle(
        modifier = modifier
            .padding(end=8.dp, top=16.dp, bottom=8.dp),
        title = title,
        signColor = signColor,
        iconPainter = iconPainter
    )
    //CARD:
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        border = BorderStroke(1.dp, colorResource(id = R.color.faded_grey)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors (
            containerColor = colorResource(id = R.color.dark_grey_background)
        )
    ) {
        //SETTINGS LIST:
        Column(
            modifier = Modifier
                .padding(20.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
        ) {
            content()
        }
    }
}

