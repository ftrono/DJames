package com.ftrono.DJames.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Card
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ftrono.DJames.R
import com.ftrono.DJames.application.historySize
import com.ftrono.DJames.application.lastNavRoute
import com.ftrono.DJames.application.settingsOpen
import com.ftrono.DJames.ui.navigateTo
import com.ftrono.DJames.ui.theme.NavigationItem
import com.ftrono.DJames.utilities.Utilities
import com.google.gson.JsonParser

@Preview
@Preview(heightDp = 360, widthDp = 800)
@Composable
fun GuideScreenPreview() {
    val navController = rememberNavController()
    GuideScreen(navController)
}

@Composable
fun GuideScreen(navController: NavController) {
    val mContext = LocalContext.current
    val settingsOpenState by settingsOpen.observeAsState()
    val utils = Utilities()
    var guideItems = utils.getGuideArray(mContext)
    var iCat = 0
    var iReq = 0

    Box (
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.windowBackground))
    ) {
        //BG LINE:
        Canvas(
            modifier = Modifier
                .padding(start = 48.dp)
                .matchParentSize()
                .width(20.dp)
        ) {
            drawLine(
                color = Color.Gray,
                start = Offset(0f, 0f),
                end = Offset(0f, size.height),
                strokeWidth = 20f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(160f, 80f), 0f)
            )
        }

        //MAIN:
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                Card(
                    modifier = Modifier
                        .padding(10.dp)
                        .weight(1f)
                        .wrapContentSize(align = Alignment.TopStart),
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
                            painter = painterResource(id = R.drawable.sign_info),
                            contentDescription = "header",
                            tint = colorResource(id = R.color.light_grey)
                        )
                        //Headers text:
                        Column(
                            modifier = Modifier
                                .padding(start = 8.dp, end = 30.dp)
                        ) {
                            Text(
                                text = "Guide",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorResource(id = R.color.light_grey),
                                modifier = Modifier
                                    .wrapContentWidth()
                            )
                            Text(
                                text = "What you can ask",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorResource(id = R.color.mid_grey),
                                modifier = Modifier
                                    .wrapContentWidth()
                            )
                        }
                    }
                }
                //LANGUAGE SETTINGS:
                IconButton(
                    modifier = Modifier
                        .padding(end = 12.dp),
                    onClick = {
                        //Navigate:
                        val curNavRoute = NavigationItem.Settings.route
                        if (curNavRoute == lastNavRoute && (settingsOpenState!!)) {
                            navController.popBackStack()
                        } else {
                            navigateTo(navController, curNavRoute)
                        }
                        lastNavRoute = curNavRoute
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(35.dp),
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = colorResource(id = R.color.colorAccentLight)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                //SECTIONS:
                for (category in guideItems) {
                    var catItem = category.asJsonObject
                    ExpandableSection(
                        modifier = Modifier
                            .fillMaxWidth(),
                        title = catItem.get("header").asString
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.Start
                        ) {
                            //ITEMS:
                            for (request in catItem.get("requests").asJsonArray) {
                                var reqItem = request.asJsonObject
                                ExpandableItem(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    title = reqItem.get("intro").asString,
                                    first = iCat == 0 && iReq == 0   //expand first item only
                                ) {
                                    Card(
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier
                                            .padding(start = 52.dp, end = 24.dp, bottom = 12.dp)
                                            .fillMaxWidth(),
                                        border = BorderStroke(1.dp, colorResource(id = R.color.mid_grey)),
                                        colors = CardDefaults.cardColors(
                                            containerColor = colorResource(id = R.color.dark_grey_background)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .padding(20.dp)
                                                .fillMaxWidth(),
                                            verticalArrangement = Arrangement.Top,
                                            horizontalAlignment = Alignment.Start
                                        ) {
                                            //SENTENCE:
                                            Text(
                                                modifier = Modifier
                                                    .fillMaxWidth(),
                                                text = reqItem.get("sentence").asString,
                                                fontSize = 18.sp,
                                                lineHeight = 22.sp,
                                                fontStyle = FontStyle.Italic,
                                                color = colorResource(id = R.color.light_grey)
                                            )
                                            //DESCR:
                                            Text(
                                                modifier = Modifier
                                                    .padding(top = 12.dp)
                                                    .fillMaxWidth(),
                                                text = reqItem.get("description").asString,
                                                fontSize = 14.sp,
                                                lineHeight = 18.sp,
                                                color = colorResource(id = R.color.mid_grey)
                                            )
                                        }
                                    }
                                }
                                iReq++
                            }
                        }
                    }
                    iCat++
                }
            }
        }
    }
}

@Composable
fun ExpandableSection(
    modifier: Modifier = Modifier,
    title: String,
    content: @Composable () -> Unit
) {
    var sectionIsExpanded by rememberSaveable { mutableStateOf(true) }   //TODO
    Column(
        modifier = modifier
            .clickable { sectionIsExpanded = !sectionIsExpanded }
            .background(colorResource(id = R.color.windowBackground))
            .fillMaxWidth()
    ) {
        //SECTION:
        ExpandableSectionTitle(
            isExpanded = sectionIsExpanded,
            title = title)

        AnimatedVisibility(
            modifier = Modifier
                .fillMaxWidth(),
            visible = sectionIsExpanded
        ) {
            content()
        }
    }
}


@Composable
fun ExpandableItem(
    modifier: Modifier = Modifier,
    title: String,
    first: Boolean,
    content: @Composable () -> Unit
) {
    var itemIsExpanded = rememberSaveable { mutableStateOf(first) }
    Column(
        modifier = modifier
            .clickable { itemIsExpanded.value = !itemIsExpanded.value }
            .fillMaxWidth()
    ) {
        //SECTION:
        ExpandableItemTitle(
            isExpanded = itemIsExpanded,
            title = title)

        AnimatedVisibility(
            modifier = Modifier
                .fillMaxWidth(),
            visible = itemIsExpanded.value
        ) {
            content()
        }
    }
}


@Composable
fun ExpandableSectionTitle(modifier: Modifier = Modifier, isExpanded: Boolean, title: String) {
    val icon = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown
    //SECTION HEADER:
    Row(
        modifier = modifier
            .padding(start=8.dp, end=8.dp, top=12.dp, bottom=8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f),
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(id = R.color.light_grey)
        )
        Icon(
            modifier = Modifier
                .padding(end = 12.dp)
                .size(32.dp),
            imageVector = icon,
            tint = colorResource(id = R.color.light_grey),
            contentDescription = "Expand / collapse"
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ExpandableItemTitle(modifier: Modifier = Modifier, isExpanded: MutableState<Boolean>, title: String) {
    val icon = if (isExpanded.value) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown
    //SECTION HEADER:
    Chip(
        modifier = modifier
            .padding(start=50.dp),
        shape = RoundedCornerShape(14.dp),
        //border = BorderStroke(1.dp, colorResource(id = R.color.mid_grey)),
        colors = ChipDefaults.chipColors(
            backgroundColor = if (isExpanded.value) colorResource(id = R.color.windowBackground) else colorResource(id = R.color.windowBackground),
            contentColor = colorResource(id = R.color.light_grey),
            leadingIconContentColor = colorResource(id = R.color.mid_grey)
        ),
        leadingIcon = {
            Icon(
                modifier = Modifier
                    .size(24.dp),
                imageVector = icon,
                tint = colorResource(id = R.color.colorAccentLight),
                contentDescription = "Expand / collapse"
            )
        },
        onClick = {
            isExpanded.value = !isExpanded.value
        }
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isExpanded.value) colorResource(id = R.color.light_grey) else colorResource(id = R.color.mid_grey)
        )
    }
}