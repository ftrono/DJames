package com.ftrono.DJames.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ftrono.DJames.R
import com.ftrono.DJames.application.libHeads
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.navItems
import com.ftrono.DJames.ui.selectors.libColorSelectorLight
import com.ftrono.DJames.ui.selectors.libIconSelector

@Preview
@Preview(heightDp = 360, widthDp = 800)
@Composable
fun MainNavigatorPreview() {
    val configuration = LocalConfiguration.current
    val isLandscape by remember { mutableStateOf(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) }
    val selected = "home"

    MainNavigator() {
        navItems.forEach { navItem ->
            NavigatorCat(
                selected = navItem.route == selected,
                label = navItem.title,
                iconPainter = painterResource(navItem.icon)
            )
            //DIVIDERS:
            if (navItem != navItems.last()) {
                if (isLandscape) {
                    HorizontalDivider(
                        modifier = Modifier
                            .padding(top = 4.dp, bottom = 4.dp)
                            .width(50.dp)
                            .wrapContentHeight(),
                        thickness = 2.dp,
                        color = colorResource(id = R.color.faded_grey)
                    )
                } else {
                    VerticalDivider(
                        modifier = Modifier
                            .padding(start = 4.dp, end = 4.dp)
                            .height(40.dp)
                            .wrapContentWidth(),
                        thickness = 2.dp,
                        color = colorResource(id = R.color.faded_grey)
                    )
                }
            }
        }
    }
}


@Composable
fun MainNavigator(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isLandscape by remember { mutableStateOf(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) }

    //CONTAINER:
    Box(
        modifier = modifier
            .background(colorResource(R.color.windowBackground)),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            border = BorderStroke(2.dp, colorResource(id = R.color.faded_grey)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorResource(id = R.color.dark_grey_background)
            )
        ) {
            // BUTTONS:
            if (isLandscape) {
                Column(
                    modifier = Modifier
                        .padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    content()
                }
            } else {
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp, bottom = 4.dp, start = 12.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    content()
                }
            }
        }
    }
}


@Composable
fun NavigatorCat(
    selected: Boolean,
    label: String,
    iconPainter: Painter,
    onClick: () -> Unit = {}
){

    Card(
        modifier = Modifier
            .wrapContentHeight()
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) colorResource(id = R.color.colorAccent) else colorResource(id = R.color.transparent_full)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)
                .wrapContentWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            //Sign icon:
            Icon(
                modifier = Modifier
                    .padding(
                        top = 6.dp,
                        bottom = 6.dp,
                        start = 10.dp,
                        end = 10.dp
                    )
                    .size(if (selected) 32.dp else 24.dp),
                painter = iconPainter,
                contentDescription = "category",
                tint = colorResource(id = R.color.light_grey)
            )
            //Label:
//            if (selected) {
//                Text(
//                    modifier = Modifier
//                        .padding(start = 6.dp, end = 6.dp),
//                    text = label,
//                    fontSize = 12.sp,
//                    fontWeight = FontWeight.Bold,
//                    color = colorResource(id = R.color.light_grey),
//                    maxLines = 1
//                )
//            }
        }
    }
}