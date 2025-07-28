package com.ftrono.DJames.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ftrono.DJames.R
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.libHeads
import com.ftrono.DJames.ui.selectors.guideColorSelector
import com.ftrono.DJames.ui.selectors.guideColorSelectorLight
import com.ftrono.DJames.ui.selectors.guideIconSelector
import com.ftrono.DJames.ui.selectors.libColorSelector
import com.ftrono.DJames.ui.selectors.libColorSelectorLight
import com.ftrono.DJames.ui.selectors.libIconSelector


// STREET UI LANGUAGE COMPONENTS

@Composable
fun StreetBackground(
    modifier: Modifier = Modifier,
    startDistance: Int,
    pageContent:  @Composable() (ColumnScope.() -> Unit) = {},
) {
    //Page container:
    Box (
        modifier = modifier
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
        iconPainter = painterResource(id = R.drawable.sign_fork),
        title = "Section Title",
        subtitle = "Section subtitle",
        num = 1
    )
}


@Composable
fun HeaderWithSign(
    modifier: Modifier = Modifier,
    iconPainter: Painter? = null,
    iconVector: ImageVector? = null,
    onIconClick: () -> Unit = {},
    pretitle: String? = null,
    title: String,
    subtitle: String? = null,
    num: Int? = null,
    signColor: Color = colorResource(id = R.color.colorPrimary),
    optionButtons: @Composable() (RowScope.() -> Unit) = {}
) {
    //HEADER:
    Row(
        modifier = modifier
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
            iconPainter = iconPainter,
            iconVector = iconVector,
            onIconClick = { onIconClick() },
            pretitle = pretitle,
            title = title,
            subtitle = subtitle,
            num = num,
            signColor = signColor
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
    iconPainter: Painter? = null,
    iconVector: ImageVector? = null,
    onIconClick: () -> Unit = {},
    pretitle: String? = null,
    title: String,
    subtitle: String? = null,
    num: Int? = null,
    signColor: Color = colorResource(id = R.color.colorPrimary)
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(2.dp, colorResource(id = R.color.mid_grey)),
        colors = CardDefaults.cardColors(
            containerColor = signColor
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            //Sign icon:
            if (iconVector != null) {
                Icon(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onIconClick() },
                    imageVector = iconVector,
                    contentDescription = "header",
                    tint = colorResource(id = R.color.light_grey)
                )
            } else {
                Icon(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onIconClick() },
                    painter = iconPainter!!,
                    contentDescription = "header",
                    tint = colorResource(id = R.color.light_grey)
                )
            }
            //Headers text:
            Column(
                modifier = Modifier
                    .padding(start = 8.dp, end = 30.dp)
            ) {
                if (pretitle != null) {
                    Text(
                        text = pretitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(id = R.color.light_grey),
                    )
                }
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.light_grey),
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(id = R.color.light_grey),
                    )
                }
            }
            if (num != null) {
                //N items:
                Text(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .wrapContentWidth(),
                    text = "$num",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.light_grey)
                )
            }
        }
    }
}


@Preview
@Composable
fun SplitterSignPreview() {
    val currentCatState = rememberSaveable { mutableStateOf(libHeads[0]) }
    val libraryItems = rememberSaveable {
        mutableStateOf(libUtils.refreshLibrary(currentCatState.value, true))
    }

    SplitterSign(
        libraryItems = libraryItems,
        currentCatState = currentCatState,
        curLibrarySizeState = 0,
        preview = true
    )
}


@Composable
fun SplitterSign(
    currentCatState: MutableState<String>,
    libraryItems: MutableState<List<String>>,
    curLibrarySizeState: Int,
    preview: Boolean
) {
    val configuration = LocalConfiguration.current
    val isLandscape by remember { mutableStateOf(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) }

    //BUTTONS:
    Card(
        modifier = Modifier
            .wrapContentWidth()
            .wrapContentHeight(),
        border = BorderStroke(2.dp, colorResource(id = R.color.faded_grey)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors (
            containerColor = colorResource(id = R.color.dark_grey_background)
        )
    ) {
        Row (
            modifier = Modifier
                .padding(top=4.dp, bottom=4.dp, start=12.dp, end=12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            for (head in libHeads) {
                SplitterCat(
                    currentCatState = currentCatState,
                    libraryItems = libraryItems,
                    head = head,
                    title = "${head.replaceFirstChar { it.uppercase() }}s",
                    selected = currentCatState.value == head,
                    isLandscape = isLandscape,
                    num = if (isLandscape && currentCatState.value == head) curLibrarySizeState else null,
                    preview = preview
                )
                //DIVIDERS:
                if (head != libHeads.last()) {
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
}


@Composable
fun SplitterCat(
    currentCatState: MutableState<String>,
    libraryItems: MutableState<List<String>>,
    head: String,
    title: String,
    selected: Boolean,
    num: Int? = null,
    isLandscape: Boolean = false,
    preview: Boolean = false
){
    Row(
        modifier = Modifier
            .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)
            .wrapContentWidth()
            .clickable {
                currentCatState.value = head
                libraryItems.value = libUtils.refreshLibrary(currentCatState.value, preview)
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        //Sign icon:
        Icon(
            modifier = Modifier
                .padding(
                    start = if (isLandscape && selected) 4.dp else 6.dp,
                    end = if (isLandscape && selected) 4.dp else 6.dp
                )
                .size(if (selected) 26.dp else 18.dp),
            painter = libIconSelector(head),
            contentDescription = "category",
            tint = if (selected) libColorSelectorLight(head) else colorResource(id = R.color.light_grey)
        )
        //Title:
        if (isLandscape && selected) {
            Text(
                modifier = Modifier
                    .padding(start = 4.dp, end = 6.dp),
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = libColorSelectorLight(head),
                maxLines = 1
            )
            if (num != null) {
                //Number of items:
                Text(
                    modifier = Modifier
                        .padding(start = 4.dp, end = 4.dp)
                        .wrapContentWidth(),
                    text = num.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = libColorSelectorLight(head),
                    maxLines = 1
                )
            }
        }
    }
}


@Composable
fun RoundedSign(
    modifier: Modifier = Modifier,
    signSize: Dp,
    iconSize: Dp,
    backgroundColor: Color,
    borderColor: Color,
    iconColor: Color,
    iconPainter: Painter? = null,
    iconVector: ImageVector? = null,
    imageUrl: String = "",
    circle: Boolean = true
) {
    //ROUNDED SIGN:
    if (imageUrl == "") {
        Box(
            modifier = modifier
                .size(signSize)
                .clip(if (circle) CircleShape else RoundedCornerShape(4.dp))
                .background(backgroundColor)
                .border(1.5.dp, borderColor, if (circle) CircleShape else RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            //CAT ICON:
            if (iconVector != null) {
                Icon(
                    modifier = Modifier
                        .size(iconSize),
                    imageVector = iconVector,
                    contentDescription = "Item image",
                    tint = iconColor
                )
            } else {
                Icon(
                    modifier = Modifier
                        .size(iconSize),
                    painter = iconPainter!!,
                    contentDescription = "Item image",
                    tint = iconColor
                )
            }
        }
    } else {
        AsyncImage(
            modifier = modifier
                .size(signSize)
                .clip(if (circle) CircleShape else RoundedCornerShape(4.dp))
                .border(1.5.dp, borderColor, if (circle) CircleShape else RoundedCornerShape(4.dp)),
            model = imageUrl,
            contentDescription = "Item image"
        )
    }
}

@Composable
fun RoundedLetter(
    text: String,
    signSize: Dp,
    fontSize: TextUnit,
    backgroundColor: Color,
    borderColor: Color,
    fontColor: Color,
) {
    //ROUNDED SIGN:
    Box (
        modifier = Modifier
            .size(signSize)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.5.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        //LETTER:
        Text(
            text = text,
            color = fontColor,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold
        )
    }
}


@Preview
@Composable
fun GeneralSectionClosedPreview() {
    GeneralSectionHeader(
        title = "Title",
        signColor = guideColorSelector(cat = "songs"),
        iconPainter = guideIconSelector(cat = "songs"),
        arrowColor = guideColorSelectorLight(cat = "songs"),
        expandable = true,
        isExpanded = false
    )
}


@Preview
@Composable
fun GeneralSectionOpenPreview() {
    GeneralSectionHeader(
        title = "Title",
        signColor = guideColorSelector(cat = "songs"),
        iconPainter = guideIconSelector(cat = "songs"),
        arrowColor = guideColorSelectorLight(cat = "songs"),
        expandable = true,
        isExpanded = true
    )
}


@Composable
fun GeneralSectionHeader(
    modifier: Modifier = Modifier,
    title: String,
    signColor: Color,
    iconPainter: Painter,
    arrowColor: Color = colorResource(id = R.color.light_grey),
    expandable: Boolean = false,
    isExpanded: Boolean = true
) {

    //CARD:
    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        border = if (isExpanded) null else BorderStroke(1.dp, colorResource(id = R.color.faded_grey)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors (
            containerColor = if (isExpanded) colorResource(id = R.color.transparent_bg) else colorResource(id = R.color.dark_grey_background)
        )
    ) {
        SectionTitle(
            modifier = Modifier
                .padding(start=6.dp, end=8.dp, top=12.dp, bottom=12.dp),
            title=title,
            signColor = signColor,
            iconPainter = iconPainter,
            arrowColor = arrowColor,
            expandable = expandable,
            isExpanded = isExpanded
        )
    }
}


@Composable
fun SectionTitle(
    modifier: Modifier = Modifier,
    title: String,
    signColor: Color,
    iconPainter: Painter,
    arrowColor: Color = colorResource(id = R.color.light_grey),
    expandable: Boolean = false,
    isExpanded: Boolean = true
) {
    val icon = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown

    //SECTION HEADER:
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        //ROUNDED SIGN:
        RoundedSign(
            signSize = 40.dp,
            iconSize = 20.dp,
            backgroundColor = signColor,
            borderColor = colorResource(id = R.color.mid_grey),
            iconColor = colorResource(id = R.color.light_grey),
            iconPainter = iconPainter
        )
        //CAT TITLE:
        Text(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f),
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(id = R.color.light_grey)
        )
        //EXPAND/COLLAPSE:
        if (expandable) {
            Icon(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(32.dp),
                imageVector = icon,
                tint = arrowColor,
                contentDescription = "Expand / collapse"
            )
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


@Preview
@Composable
fun LibItemCardPreview() {
    LibItemCard(
        modifier = Modifier
            .wrapContentWidth()
            .height(60.dp),
        cardColors = CardDefaults.cardColors(
            containerColor = colorResource(id = R.color.dark_grey)
        ),
        signBackgroundColor = libColorSelector(cat = "artist"),
        signBorderColor = colorResource(id = R.color.midfaded_grey),
        signIconColor = colorResource(id = R.color.light_grey),
        signIconPainter = libIconSelector(cat = "artist"),
        title = "Item name",
        subtitle = "subtitle",
    )
}


@Composable
fun LibItemCard(
    modifier: Modifier = Modifier,
    cardColors: CardColors,
    signBackgroundColor: Color,
    signBorderColor: Color,
    signIconColor: Color,
    signIconPainter: Painter,
    circle: Boolean = true,
    title: String,
    subtitle: String = "",
    imageUrl: String = "",
    onClick: () -> Unit = {}
) {
    val isMultiline = rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = modifier
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, colorResource(id = R.color.faded_grey)),
        colors = cardColors
    ) {

        Row (
            modifier = Modifier
                .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ){
            //CHIP ICON:
            RoundedSign(
                modifier = Modifier
                    .padding(end = 4.dp),
                signSize = 34.dp,
                iconSize = 20.dp,
                backgroundColor = signBackgroundColor,
                borderColor = signBorderColor,
                iconColor = signIconColor,
                iconPainter = signIconPainter,
                imageUrl = imageUrl,
                circle = circle
            )
            //TEXT LABEL:
            Column(
                modifier = Modifier
                    .padding(start = 4.dp, end = 6.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                //Item key:
                Text(
                    modifier = Modifier
                        .wrapContentWidth()
                        .wrapContentHeight(),
                    color = colorResource(id = R.color.light_grey),
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    text = title,
                    fontWeight = FontWeight.Bold,
                    //fontStyle = FontStyle.Italic,
                    onTextLayout = { textLayoutResult ->
                        isMultiline.value = textLayoutResult.lineCount > 1
                    }
                )
                //Item detail:
                if (subtitle != "" && !isMultiline.value) {
                    Text(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .wrapContentWidth()
                            .wrapContentHeight(),
                        color = colorResource(id = R.color.mid_grey),
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        maxLines = 1,
                        fontStyle = FontStyle.Italic,
                        text = subtitle
                    )
                }
            }
        }
    }
}