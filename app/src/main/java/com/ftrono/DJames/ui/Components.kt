package com.ftrono.DJames.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ftrono.DJames.R
import com.ftrono.DJames.application.vocHeads
import com.ftrono.DJames.screen.getVocKeys


// STREET UI LANGUAGE COMPONENTS

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


@Preview
@Composable
fun HeaderPreview() {
    HeaderWithSign(
        iconRes = painterResource(id = R.drawable.sign_fork),
        title = "Section Title",
        subtitle = "Section subtitle",
        num = 1
    )
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
                    .size(40.dp),
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
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.light_grey),
                    modifier = Modifier
                        .wrapContentWidth()
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
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
                    fontSize = 24.sp,
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


@Preview
@Composable
fun SplitterPreview() {
    val mContext = LocalContext.current
    val currentCatState = rememberSaveable { mutableStateOf(vocHeads[0]) }
    val vocabulary = rememberSaveable {
        mutableStateOf(getVocKeys(mContext, currentCatState.value, true))
    }
    SplitterSign(
        currentCatState = currentCatState,
        vocabulary = vocabulary,
        preview = true
    )
}


@Composable
fun SplitterSign(
    currentCatState: MutableState<String>,
    vocabulary: MutableState<List<String>>,
    preview: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        //ITEM 1:
        for (head in vocHeads) {
            SplitterCat(
                modifier = Modifier
                    .weight(0.3f),
                currentCatState = currentCatState,
                vocabulary = vocabulary,
                head = head,
                title = "${head.replaceFirstChar { it.uppercase() }}s",
                selected = currentCatState.value == head,
                preview = preview
            )
            if (head != vocHeads.last()) {
                VerticalDivider(
                    modifier = Modifier
                        .padding(start = 4.dp, end = 4.dp)
                        .height(30.dp)
                        .wrapContentWidth(),
                    thickness = 2.dp,
                    color = colorResource(id = R.color.faded_grey)
                )
            }

        }

    }
}


@Composable
fun SplitterCat(
    modifier: Modifier,
    currentCatState: MutableState<String>,
    vocabulary: MutableState<List<String>>,
    head: String,
    title: String,
    selected: Boolean,
    preview: Boolean = false
){
    val mContext = LocalContext.current
    Card(
        modifier = modifier
            .padding(start=6.dp, end=6.dp)
            .fillMaxWidth()
            .clickable {
                currentCatState.value = head
                vocabulary.value = getVocKeys(mContext, currentCatState.value, preview)
            },
        shape = RoundedCornerShape(14.dp),
        border = if (selected) BorderStroke(1.5.dp, colorResource(id = R.color.mid_grey)) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                vocColorSelector(cat = currentCatState.value)
            } else {
                colorResource(id = R.color.transparent_full)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .wrapContentSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            //Sign icon:
            Icon(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(20.dp),
                painter = vocIconSelector(head),
                contentDescription = "category",
                tint = colorResource(id = R.color.light_grey)
            )
            //Category text:
            Column(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .wrapContentSize()
            ) {
                //Title:
                Text(
                    modifier = Modifier
                        .wrapContentWidth(),
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.light_grey),
                    maxLines = 1
                )
            }
        }
    }
}


//OPTIONS DROPDOWN MENU:
@Composable
fun OptionsMenu(
    expandedState: MutableState<Boolean>,
    backgroundColor: Color,
    options:  @Composable() (ColumnScope.() -> Unit) = {}
) {
    //DROPDOWN MENU:
    DropdownMenu(
        modifier = Modifier
            .background(backgroundColor),
        shape = RoundedCornerShape(20.dp),
        expanded = expandedState.value,
        onDismissRequest = {
            expandedState.value = false
        }
    ) {
        options()
    }
}


@Composable
fun OptionsItem(
    title: String,
    iconVector: ImageVector? = null,
    iconPainter: Painter? = null,
    onClick: () -> Unit = {}
) {
    DropdownMenuItem(
        text = {
            Text(
                text = title,
                color = colorResource(id = R.color.light_grey),
                fontSize = 16.sp
            )},
        leadingIcon = {
            if (iconVector != null) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = title,
                    tint = colorResource(id = R.color.mid_grey)
                )
            } else {
                Icon(
                    painter = iconPainter!!,
                    contentDescription = title,
                    tint = colorResource(id = R.color.mid_grey)
                )
            }
        },
        onClick = {
            onClick()
        }
    )
}