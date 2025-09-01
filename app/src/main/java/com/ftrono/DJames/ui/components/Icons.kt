package com.ftrono.DJames.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ftrono.DJames.R


@Composable
fun DriveIcon(
    iconSize: Dp = 48.dp,
    lineWidth: Dp = 2.dp,
    showForbidden: Boolean
) {
    Box() {
        Icon(
            modifier = Modifier
                .size(iconSize),
            painter = painterResource(R.drawable.icon_car),
            contentDescription = if (showForbidden) "Close Drive mode" else "Open Drive mode",
            tint = colorResource(R.color.light_grey)
        )
        if (showForbidden) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val strokeWidthPx = lineWidth.toPx()
                drawLine(
                    color = Color.White,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height),
                    strokeWidth = strokeWidthPx
                )
            }
        }
    }
}

@Composable
fun VolumeUpIcon(
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    lineWidth: Dp = 2.dp,
    showForbidden: Boolean
) {
    Box() {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier
                    .offset(x = 4.dp)
                    .size(iconSize - 6.dp),
                painter = painterResource(R.drawable.arrow_up),
                tint = colorResource(id = R.color.light_grey),
                contentDescription = "Raise Volume"
            )
            Icon(
                modifier = Modifier
                    .size(iconSize),
                painter = painterResource(R.drawable.icon_volume),
                tint = colorResource(id = R.color.light_grey),
                contentDescription = "Raise Volume"
            )
        }
        if (showForbidden) {
            Canvas(modifier = Modifier
                .offset(x = 4.dp)
                .matchParentSize()
            ) {
                val strokeWidthPx = lineWidth.toPx()
                drawLine(
                    color = Color.White,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height),
                    strokeWidth = strokeWidthPx
                )
            }
        }
    }
}

