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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontStyle
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
import com.ftrono.DJames.ui.selectors.guideColorSelector
import com.ftrono.DJames.ui.selectors.guideColorSelectorLight
import com.ftrono.DJames.ui.selectors.guideIconSelector
import com.ftrono.DJames.ui.selectors.libColorSelector
import com.ftrono.DJames.ui.selectors.libColorSelectorLight
import com.ftrono.DJames.ui.selectors.libIconSelector


// STREET UI LANGUAGE COMPONENTS
@Composable
fun StreetUIScaffold(
    modifier: Modifier = Modifier,
    hideLine: Boolean = false,
    lineDistance: Dp,
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
fun CardSign(
    modifier: Modifier,
    roundedCorners: Dp = 20.dp,
    backgroundColor: Color,
    borderColor: Color,
    borderWidth: Dp,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(roundedCorners),
        border = BorderStroke(borderWidth, borderColor),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        content()
    }
}


@Composable
fun ZebraSign(
    modifier: Modifier,
    isLight: Boolean = false,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .background(colorResource(if (isLight) R.color.light_grey else R.color.windowBackground)),
    ) {
        content()
    }
}


@Composable
fun ClickableSignContent(
    innerPadding: Dp = 8.dp,
    isLight: Boolean = false,
    content: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .padding(innerPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // CONTENT:
        Column(
            modifier = Modifier
                .padding(end = innerPadding)
                .weight(1F),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            content()
        }
        // GO ICON:
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            tint = colorResource(if (isLight) R.color.black else R.color.light_grey),
            contentDescription = "Go"
        )
    }
}



@Composable
fun ClickableCardSign(
    modifier: Modifier = Modifier,
    roundedCorners: Dp = 20.dp,
    backgroundColor: Color = colorResource(id = R.color.dark_grey),
    borderColor: Color = colorResource(id = R.color.faded_grey),
    borderWidth: Dp = 1.5.dp,
    innerPadding: Dp = 8.dp,
    onClick: () -> Unit = {},
    content: @Composable () -> Unit = {},
) {
    CardSign(
        modifier = modifier
            .clickable {
                onClick()
            },
        roundedCorners = roundedCorners,
        backgroundColor = backgroundColor,
        borderColor = borderColor,
        borderWidth = borderWidth,
    ) {
        ClickableSignContent(
            innerPadding = innerPadding,
            content = content,
        )
    }
}


@Composable
fun ClickableZebraSign(
    modifier: Modifier = Modifier,
    isLight: Boolean = false,
    innerPadding: Dp = 8.dp,
    onClick: () -> Unit = {},
    content: @Composable () -> Unit = {},
) {
    ZebraSign (
        modifier = modifier
            .clickable {
                onClick()
            },
        isLight = isLight,
    ) {
        ClickableSignContent(
            innerPadding = innerPadding,
            isLight = isLight,
            content = content,
        )
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
            contentDescription = "Item image"
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
    borderColor: Color,
    fontColor: Color,
) {
    //ROUNDED SIGN:
    Card(
        modifier = Modifier,
        border = BorderStroke(1.5.dp, borderColor),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors (
            containerColor = backgroundColor
        )
    ) {
        //LETTER:
        Text(
            modifier = Modifier
                .padding(top=4.dp, bottom=4.dp, start=8.dp, end=8.dp),
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
        subtitle = "Subtitle",
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
    subtitle: String = "",
    signColor: Color,
    iconPainter: Painter,
    arrowColor: Color = colorResource(id = R.color.light_grey),
    expandable: Boolean = false,
    isExpanded: Boolean = true
) {

    //CARD:
    Card(
        modifier = modifier
            .fillMaxWidth(),
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
            subtitle=subtitle,
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
    subtitle: String = "",
    signColor: Color,
    iconPainter: Painter? = null,
    iconVector: ImageVector? = null,
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
            contentSize = 20,
            backgroundColor = signColor,
            borderColor = colorResource(id = R.color.mid_grey),
            contentColor = colorResource(id = R.color.light_grey),
            iconPainter = iconPainter,
            iconVector = iconVector,
        )
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f),
        ) {
            //CAT TITLE:
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.light_grey)
            )
            //SUBTITLE:
            if (subtitle != "") {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = colorResource(id = R.color.mid_grey)
                )
            }
        }
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
            .height(70.dp)
            .width(180.dp),
        cardColors = CardDefaults.cardColors(
            containerColor = colorResource(id = R.color.dark_grey_background)
        ),
        source = "spotify",
        type = "artist",
        title = "Item name",
        subtitle = "subtitle",
    )
}


@Composable
fun LibItemCard(
    modifier: Modifier = Modifier,
    cardColors: CardColors,
    source: String,
    type: String,
    title: String,
    subtitle: String = "",
    imageUrl: String = "",
    isCollection: Boolean = false,
    onClick: () -> Unit = {}
) {
    val isMultiline = rememberSaveable { mutableStateOf(false) }
    val cardBorderColor = colorResource(id = R.color.dark_grey)
    val signBackgroundColor = if (isCollection) colorResource(R.color.violetSign) else libColorSelector(cat = type)
    val signBorderColor = colorResource(id = R.color.midfaded_grey)
    val signIconColor = colorResource(id = R.color.light_grey)
    val signIconPainter = if (isCollection) null else libIconSelector(cat = type)
    val circle = type == "artist" || source == "contact"

    Card(
        modifier = modifier
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, cardBorderColor),
        colors = cardColors
    ) {

        // ROW: INFO + SIGN:
        Row (
            modifier = Modifier
                .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ){

            // INFO COLUMN:
            Column(
                modifier = Modifier
                    .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                    .fillMaxHeight()   // Vertical
                    .weight(1F),   // Horizontal
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                if (source == "spotify") {
                    //CAT ROW:
                    Row(
                        modifier = Modifier
                            .padding(bottom = 2.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        //SOURCE ICON:
                        Icon(
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(12.dp),
                            painter = libIconSelector(source),
                            contentDescription = type,
                            tint = libColorSelector(source)
                        )
                        //CAT ICON:
                        Icon(
                            modifier = Modifier
                                .padding(end = 2.dp)
                                .size(12.dp),
                            painter = libIconSelector(type),
                            contentDescription = type,
                            tint = libColorSelectorLight(type)
                        )
                        //CAT NAME:
                        Text(
                            modifier = Modifier
                                .padding(end = 4.dp),
                            color = libColorSelectorLight(cat = type),
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            // fontWeight = FontWeight.Bold,
                            text = utils.capitalizeWords((type)),
                        )
                    }
                }

                //ITEM INFO:
                //Item name:
                Text(
                    modifier = Modifier,
                    color = colorResource(id = R.color.light_grey),
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    text = title,
                    fontWeight = FontWeight.Bold,
                    onTextLayout = { textLayoutResult ->
                        isMultiline.value = textLayoutResult.lineCount > if (source == "spotify") 1 else 2
                    }
                )
                //Item detail:
                if (subtitle != "" && !isMultiline.value) {
                    Text(
                        modifier = Modifier
                            .padding(top = 2.dp),
                        color = colorResource(id = R.color.mid_grey),
                        fontSize = 10.sp,
                        maxLines = 1,
                        lineHeight = 12.sp,
                        fontStyle = FontStyle.Italic,
                        text = subtitle
                    )
                }

            }

            //SIGN: ITEM ARTWORK / ICON:
            RoundedSign(
                modifier = Modifier
                    .padding(end=4.dp),
                signSize = 46.dp,
                contentSize = 20,
                backgroundColor = signBackgroundColor,
                borderColor = signBorderColor,
                borderWidth = 1.5.dp,
                contentColor = signIconColor,
                iconPainter = signIconPainter,
                iconVector = if (isCollection) Icons.Default.Favorite else null,
                imageUrl = imageUrl,
                circle = circle
            )
        }
    }
}