package com.ftrono.DJames.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ftrono.DJames.R


@Composable
fun StreetBackground(
    startDistance: Int,
    pageContent:  @Composable() (ColumnScope.() -> Unit) = {}
) {
    //Page container:
    Box (
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.windowBackground))
    ) {
        //Street line canvas:
        StreetLine (
            modifier = Modifier
                .padding(start = startDistance.dp)
                .matchParentSize()
                .width(20.dp)
        )
        //Content container:
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            pageContent()
        }
    }
}


@Composable
fun StreetLine(
    modifier: Modifier
) {
    Canvas(
        modifier = modifier
    ) {
        drawLine(
            color = Color.Gray,
            start = Offset(0f, 0f),
            end = Offset(0f, size.height),
            strokeWidth = 20f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(160f, 80f), 0f)
        )
    }
}


@Composable
fun HeaderWithSign(
    iconRes: Painter,
    title: String,
    subtitle: String? = null,
    num: Int? = null,
    optionButtons: @Composable() (RowScope.() -> Unit) = {}
) {
    //HEADER:
    Row(
        modifier = Modifier
            .padding(bottom = 6.dp)
            .fillMaxWidth()
            .wrapContentHeight()
            .background(colorResource(id = R.color.windowBackground)),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        //Street sign:
        HeaderSign(
            modifier = Modifier
                .padding(10.dp)
                .weight(1f)
                .wrapContentSize(align = Alignment.TopStart),
            iconRes = iconRes,
            title = title,
            subtitle = subtitle,
            num = num
        )
        //OPTIONS BUTTONS:
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            optionButtons()
        }
    }
}


@Composable
fun HeaderSign(
    modifier: Modifier,
    iconRes: Painter,
    title: String,
    subtitle: String? = null,
    num: Int? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(2.dp, colorResource(id = R.color.mid_grey)),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(id = R.color.colorPrimary)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            //Sign icon:
            Icon(
                modifier = Modifier
                    .size(50.dp),
                painter = iconRes,
                contentDescription = "header",
                tint = colorResource(id = R.color.light_grey)
            )
            //Headers text:
            Column(
                modifier = Modifier
                    .padding(start = 8.dp, end = 30.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.light_grey),
                    modifier = Modifier
                        .wrapContentWidth()
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(id = R.color.mid_grey),
                        modifier = Modifier
                            .wrapContentWidth()
                    )
                }
            }
            if (num != null) {
                //N items:
                Text(
                    text = "$num",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.light_grey),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .wrapContentWidth()
                )
            }
        }
    }
}