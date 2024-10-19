package com.ftrono.DJames.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Card
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ftrono.DJames.R
import com.ftrono.DJames.application.lastNavRoute
import com.ftrono.DJames.application.settingsOpen
import com.ftrono.DJames.ui.navigateTo
import com.ftrono.DJames.ui.theme.NavigationItem
import com.ftrono.DJames.utilities.Utilities

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

    Column (
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.windowBackground))
    ) {
        //HEADER:
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(colorResource(id = R.color.windowBackground)),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            //TEXT HEADERS:
            Column(
                modifier = Modifier
                    .weight(1f),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "❔  Guide",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.light_grey),
                    modifier = Modifier
                        .padding(
                            start = 10.dp,
                            top = 14.dp
                        )
                        .wrapContentWidth()
                )
                Text(
                    text = "What you can ask:",
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    color = colorResource(id = R.color.mid_grey),
                    modifier = Modifier
                        .padding(
                            start = 53.dp,
                            bottom = 10.dp
                        )
                        .wrapContentWidth()
                )
            }
            //LANGUAGE SETTINGS:
            Button(
                modifier = Modifier
                    .padding(end = 20.dp)
                    .wrapContentWidth()
                    .height(36.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.colorPrimary),
                    contentColor = colorResource(id = R.color.light_grey)
                ),
                content = {
                    Text(
                        modifier = Modifier
                            .wrapContentWidth()
                            .wrapContentHeight(),
                        color = colorResource(id = R.color.light_grey),
                        text = "LANGUAGES",
                        fontSize = 12.sp
                    )
                },
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
            )
        }
        Column (
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(id = R.color.windowBackground))
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
                            iReq ++
                        }
                    }
                }
                iCat ++
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
        colors = ChipDefaults.chipColors(
            backgroundColor = if (isExpanded.value) colorResource(id = R.color.dark_grey) else colorResource(id = R.color.dark_grey_background),
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
            fontSize = 14.sp,
            fontStyle = FontStyle.Italic,
            color = colorResource(id = R.color.light_grey)
        )
    }
}