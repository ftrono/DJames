package com.ftrono.DJames.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ftrono.DJames.R
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.database.LibraryItem
import com.ftrono.DJames.ui.selectors.colorSelector
import com.ftrono.DJames.ui.selectors.colorSelectorDark
import com.ftrono.DJames.ui.selectors.colorSelectorLight
import com.ftrono.DJames.ui.selectors.colorSelectorMid
import com.ftrono.DJames.ui.selectors.iconSelector
import com.ftrono.DJames.ui.theme.midfaded_grey


// STREET UI LANGUAGE COMPONENTS
@Composable
fun StreetUIScaffold(
    modifier: Modifier = Modifier,
    hideLine: Boolean = false,
    lineDistance: Dp = 20.dp,
    topBar: @Composable () -> Unit = {},
    fab: @Composable () -> Unit = {},
    pageContent:  @Composable () (ColumnScope.() -> Unit) = {},
) {
    // Scaffold:
    Scaffold (
        modifier = modifier
            .fillMaxSize(),
        topBar = topBar,
        floatingActionButton = fab,
        contentColor = colorResource(R.color.windowBackground)
    ) {
        //Page container:
        Box (
            modifier = modifier
                .padding(it)
                .background(colorResource(id = R.color.windowBackground))
        ) {
            if (!hideLine) {
                //Street line canvas:
                StreetLine(
                    modifier = Modifier
                        .padding(start = lineDistance)
                        .matchParentSize()
                        .width(20.dp)
                )
            }
            //Content container:
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                pageContent()
            }
        }
    }
}


@Composable
fun StreetLine(
    modifier: Modifier,
    isHorizontal: Boolean = false,
) {
    Canvas(
        modifier = modifier
    ) {
        drawLine(
            color = midfaded_grey,
            start = Offset(x = 0f, y = 0f),
            end = Offset(
                x = if (isHorizontal) size.width else 0f,
                y = if (isHorizontal) 0f else size.height,
            ),
            strokeWidth = 20f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(160f, 80f), 0f)
        )
    }
}


@Composable
fun CardSign(
    modifier: Modifier,
    roundedCorners: Dp = 20.dp,
    backgroundColor: Color,
    borderColor: Color? = null,
    borderWidth: Dp? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(roundedCorners),
        border = if (borderColor != null && borderWidth != null) BorderStroke(borderWidth, borderColor) else null,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        content()
    }
}


@Composable
fun RoundedSign(
    modifier: Modifier = Modifier,
    signSize: Dp,
    contentSize: Int,
    backgroundColor: Color,
    borderColor: Color,
    contentColor: Color,
    borderWidth: Dp = 1.5.dp,
    imageRes: Painter? = null,
    iconPainter: Painter? = null,
    iconVector: ImageVector? = null,
    contentText: String = "",
    imageUrl: String = "",
    circle: Boolean = true,
    clickable: Boolean = false,
    onClick: () -> Unit = {},
) {
    //ROUNDED SIGN:
    if (imageRes != null) {
        Image(
            modifier = Modifier
                .size(signSize)
                .clip(if (circle) CircleShape else RoundedCornerShape(4.dp))
                .border(
                    borderWidth,
                    borderColor,
                    if (circle) CircleShape else RoundedCornerShape(4.dp)
                ),
            painter = imageRes,
            contentDescription = "Item image",
            contentScale = ContentScale.Crop,
        )
    } else if (imageUrl != "") {
        AsyncImage(
            modifier = modifier
                .size(signSize)
                .clip(if (circle) CircleShape else RoundedCornerShape(4.dp))
                .border(
                    borderWidth,
                    borderColor,
                    if (circle) CircleShape else RoundedCornerShape(4.dp)
                ),
            model = imageUrl,
            contentDescription = "Item image",
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = if (clickable) {
                    modifier
                        .size(signSize)
                        .clip(if (circle) CircleShape else RoundedCornerShape(4.dp))
                        .background(backgroundColor)
                        .border(
                            borderWidth,
                            borderColor,
                            if (circle) CircleShape else RoundedCornerShape(4.dp)
                        )
                        .clickable { onClick() }
                } else {
                    modifier
                        .size(signSize)
                        .clip(if (circle) CircleShape else RoundedCornerShape(4.dp))
                        .background(backgroundColor)
                        .border(
                            borderWidth,
                            borderColor,
                            if (circle) CircleShape else RoundedCornerShape(4.dp)
                        )
                },
            contentAlignment = Alignment.Center
        ) {
            //CAT ICON:
            if (contentText != "") {
                //N items:
                Text(
                    modifier = Modifier,
                    text = contentText,
                    fontSize = if (contentText.length < 3) contentSize.sp else (contentSize-7).sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = contentColor
                )
            } else if (iconVector != null) {
                Icon(
                    modifier = Modifier
                        .size(contentSize.dp),
                    imageVector = iconVector,
                    contentDescription = "Item image",
                    tint = contentColor
                )
            } else {
                Icon(
                    modifier = Modifier
                        .size(contentSize.dp),
                    painter = iconPainter!!,
                    contentDescription = "Item image",
                    tint = contentColor
                )
            }
        }
    }
}


@Composable
fun LetterStarter(
    text: String,
    fontSize: TextUnit,
    backgroundColor: Color,
    borderColor: Color? = null,
    fontColor: Color,
) {
    //ROUNDED SIGN:
    Card(
        modifier = Modifier,
        border = if (borderColor == null) null else BorderStroke(1.5.dp, borderColor),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors (
            containerColor = backgroundColor
        )
    ) {
        Row(
            modifier = Modifier
                .padding(top=4.dp, bottom=4.dp, start=8.dp, end=8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // SIGN:
            Icon(
                modifier = Modifier
                    .padding(end = 2.dp)
                    .size(12.dp),
                painter = painterResource(R.drawable.arrow_down),
                tint = colorResource(R.color.black),
                contentDescription = text
            )
            //LETTER:
            Text(
                modifier = Modifier
                    .padding(start = 2.dp),
                text = text,
                color = fontColor,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
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
    showIcon: Boolean = true,
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
                    tint = if (showIcon) colorResource(id = R.color.mid_grey) else colorResource(id = R.color.transparent_full),
                )
            } else {
                Icon(
                    painter = iconPainter!!,
                    contentDescription = title,
                    tint = if (showIcon) colorResource(id = R.color.mid_grey) else colorResource(id = R.color.transparent_full),
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
    val currentCatState = remember { mutableStateOf("artist") }
    LibItemCard(
        modifier = Modifier
            .height(140.dp)
            .width(140.dp),
        item = LibraryItem(
            source = "spotify",
            type = "playlist",
            name = "Item name",
        ),
        preview = true,
        previewDetail = "detail",
    )
}


@Composable
fun LibItemCard(
    modifier: Modifier = Modifier,
    item: LibraryItem,
    trimChars: Int = 20,
    selected: Boolean = false,
    preview: Boolean = false,
    previewDetail: String = "",
    onClick: () -> Unit = {}
) {
    // Item details:
    val isCollection = item.id == -2L
    val imageUrl = if (preview) "" else item.imageUrl
    val title = utils.trimString(item.name, trimChars)

    val detail = if (previewDetail != "") {
        previewDetail
    } else if (item.source == "place" && item.address != null && item.address!!.town != "") {
        utils.trimString(
             item.address!!.town,
            8
        )
    } else if (item.source == "contact" && item.phoneSet != null && item.phoneSet!!.phone != "") {
        item.phoneSet!!.phone
    } else ""

    val isMultiline = rememberSaveable { mutableStateOf(false) }
    val circle = item.type == "artist" || item.source == "contact"

    // Initials:
    var initials = ""
    if (!isCollection && imageUrl == "") {
        try {
            initials = title
                .lowercase()
                .replace(" & ", " ")
                .replace(" and ", " ")
                .split(" ")
                .joinToString("") { it.first().toString() }
        } catch (e: Exception) { }
        initials = if (initials.length < 2 && title.length >= 2) {
            title.slice(0..1).uppercase()
        } else {
            initials.slice(0..1).uppercase()
        }
    }

    // Resources:
    val cardBorderColor = colorResource(id = R.color.transparent_full)
    val signBackgroundColor = if (isCollection) {
        colorResource(R.color.violetSign)
    } else colorSelectorDark(cat = item.type)
    val signBorderColor = colorResource(id = R.color.transparent_full)   // midfaded_grey
    val signIconColor = colorResource(id = R.color.light_grey)
    val signIconPainter = if (!isCollection && initials == "") iconSelector(cat = item.type) else null

    Card(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, cardBorderColor),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                colorSelectorMid(item.source)
            } else {
                colorResource(id = R.color.transparent_full)
            }
        ),
    ) {
        // ROW: INFO + SIGN:
        Column(
            modifier = Modifier
                .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                //SIGN: ITEM ARTWORK / ICON:
                RoundedSign(
                    modifier = Modifier,
                    signSize = 60.dp,
                    contentSize = 20,
                    backgroundColor = signBackgroundColor,
                    borderColor = signBorderColor,
                    borderWidth = 1.5.dp,
                    contentColor = signIconColor,
                    iconPainter = signIconPainter,
                    contentText = initials,
                    iconVector = if (isCollection) Icons.Default.Favorite else null,
                    imageUrl = imageUrl,
                    circle = circle
                )
                Card(
                    modifier = Modifier,
                    shape = RoundedCornerShape(2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.source == "spotify") {
                            colorSelector(item.type)
                        } else {
                            colorSelectorLight(item.source)
                        }
                    ),
                ) {
                    //CAT ICON:
                    Icon(
                        modifier = Modifier
                            .padding(2.dp)
                            .size(12.dp),
                        painter = if (item.source == "spotify") iconSelector(item.type) else iconSelector(item.source),
                        contentDescription = item.type,
                        tint = colorResource(R.color.white)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(start=4.dp, end=4.dp, top=4.dp, bottom=2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                //ITEM INFO:
                //Item detail (place):
                if (detail != "" && item.source == "place") {
                    Text(
                        modifier = Modifier,
                        color = colorResource(id = R.color.light_grey),
                        fontSize = 10.sp,
                        maxLines = 1,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Light,
                        textAlign = TextAlign.Center,
                        text = detail,
                    )
                }

                //Item name:
                Text(
                    modifier = Modifier,
                    color = colorResource(id = R.color.light_grey),
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    maxLines = 2,
                    text = title,
                    textAlign = TextAlign.Center,
                    onTextLayout = { textLayoutResult ->
                        isMultiline.value =
                            textLayoutResult.lineCount > 1
                    }
                )

                //Item detail (contact):
                if (!isMultiline.value) {
                    Text(
                        modifier = Modifier,
                        color = colorResource(id = R.color.light_grey),
                        fontSize = 10.sp,
                        maxLines = 1,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Light,
                        textAlign = TextAlign.Center,
                        text = if (item.source == "place") "" else detail,
                    )
                }

            }
        }
    }
}