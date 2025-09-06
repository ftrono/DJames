package com.ftrono.DJames.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ftrono.DJames.R


@Preview
@Composable
fun DriveIcon(
    iconSize: Dp = 48.dp,
    lineWidth: Dp = 2.dp,
    showForbidden: Boolean = false
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

