package com.ftrono.DJames.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
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
import com.ftrono.DJames.application.lastNavRoute
import com.ftrono.DJames.application.settingsOpen
import com.ftrono.DJames.ui.HeaderWithSign
import com.ftrono.DJames.ui.StreetBackground
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

    //BACKGROUND:
    StreetBackground(
        startDistance = 48
    ) {
        //HEADER:
        HeaderWithSign(
            iconRes = painterResource(id = R.drawable.sign_info),
            title = "Guide",
            subtitle = "What you can ask"
        ) {
            //LANGUAGE SETTINGS:
            Icon(
                modifier = Modifier
                    .padding(end = 18.dp)
                    .size(35.dp)
                    .clickable {
                        //Navigate:
                        val curNavRoute = NavigationItem.Settings.route
                        if (curNavRoute == lastNavRoute && (settingsOpenState!!)) {
                            navController.popBackStack()
                        } else {
                            navigateTo(navController, curNavRoute)
                        }
                        lastNavRoute = curNavRoute
                    },
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = colorResource(id = R.color.colorAccentLight)
            )
        }

        //CONTENT:
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            //SECTIONS LIST:
            for (category in guideItems) {
                var catItem = category.asJsonObject

                ExpandableSection(
                    modifier = Modifier
                        .fillMaxWidth(),
                    cat = catItem.get("category").asString,
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
                            //REQUEST CARD:
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .padding(start = 58.dp, top = 4.dp, end = 24.dp, bottom = 12.dp)
                                    .fillMaxWidth(),
                                border = BorderStroke(1.dp, colorResource(id = R.color.mid_grey)),
                                colors = CardDefaults.cardColors(
                                    containerColor = colorResource(id = R.color.dark_grey_background)
                                )
                            ) {

                                //CARD CONTENT:
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.Top,
                                    horizontalAlignment = Alignment.Start
                                ) {

                                    //REQUEST INTRO & CONTENT:
                                    ExpandableItem(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        requestIntro = reqItem.get("intro").asString,
                                        requestSentence = reqItem.get("sentence").asString,
                                        requestDescr = reqItem.get("description").asString,
                                        first = iCat == 0 && iReq == 0   //expand first item only
                                    )
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


@Composable
fun GuideIcon(
    cat: String,
    size: Int,
    padding: Int
) {
    Icon(
        modifier = Modifier
            .padding(start = padding.dp)
            .size(size.dp),
        painter = when (cat) {
            "calls" -> {
                painterResource(id = R.drawable.sign_phone)
            }
            "messages" -> {
                painterResource(id = R.drawable.sign_message)
            }
            else -> {
                painterResource(id = R.drawable.sign_headphones)
            }
        },
        contentDescription = cat,
        tint = colorResource(id = R.color.light_grey)
    )
}


@Composable
fun ExpandableSection(
    modifier: Modifier = Modifier,
    cat: String,
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
            cat = cat,
            title = title)

        //ON EXPANSION:
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
fun ExpandableSectionTitle(
    modifier: Modifier = Modifier,
    isExpanded: Boolean,
    cat: String,
    title: String
) {
    val icon = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown
    //CARD:
    Card(
        modifier = Modifier
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = 8.dp
            )
            .fillMaxWidth()
            .wrapContentHeight(),
        border = if (isExpanded) null else BorderStroke(1.dp, colorResource(id = R.color.mid_grey)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors (
            containerColor = if (isExpanded) colorResource(id = R.color.windowBackground) else colorResource(id = R.color.dark_grey_background)
        )
    ) {
        //SECTION HEADER:
        Row(
            modifier = modifier
                .padding(start=8.dp, end=8.dp, top=12.dp, bottom=12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            //ROUNDED SIGN:
            Box (
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (cat == "calls") {
                            colorResource(id = R.color.colorPrimary)
                        } else if (cat == "messages") {
                            colorResource(id = R.color.blueSign)
                        } else {
                            colorResource(id = R.color.yellowSign)
                        }
                    )
                    .border(2.dp, colorResource(id = R.color.light_grey), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                //CAT ICON:
                GuideIcon(
                    padding = 0,
                    size = 24,
                    cat = cat
                )
            }
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
            Icon(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(32.dp),
                imageVector = icon,
                tint = colorResource(id = R.color.light_grey),
                contentDescription = "Expand / collapse"
            )
        }
    }
}


@Composable
fun ExpandableItem(
    modifier: Modifier = Modifier,
    requestIntro: String,
    requestSentence: String,
    requestDescr: String,
    first: Boolean
) {
    var itemIsExpanded = rememberSaveable { mutableStateOf(first) }

    Column(
        modifier = modifier
            .clickable { itemIsExpanded.value = !itemIsExpanded.value }
            .fillMaxWidth()
    ) {

        //REQUEST INTRO:
        ExpandableItemTitle(
            isExpanded = itemIsExpanded,
            title = requestIntro)

        //ON EXPANSION:
        AnimatedVisibility(
            modifier = Modifier
                .fillMaxWidth(),
            visible = itemIsExpanded.value
        ) {

            //EXPANDED -> REQUEST DETAILS:
            Column(
                modifier = Modifier
                    .padding(start = 24.dp, end = 24.dp, bottom = 20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {

                //DIVIDER:
                HorizontalDivider(
                    modifier = Modifier
                        .padding(bottom=4.dp),
                    color = colorResource(id = R.color.faded_grey)
                )
                //SENTENCE:
                Text(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                    text = requestSentence,
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
                    text = requestDescr,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    color = colorResource(id = R.color.mid_grey)
                )
            }
        }
    }
}


@Composable
fun ExpandableItemTitle(
    isExpanded: MutableState<Boolean>,
    title: String
) {
    val icon = if (isExpanded.value) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown
    //SECTION HEADER:
    Row(
        modifier = Modifier
            .padding(start=10.dp, end = 20.dp, top=10.dp, bottom=8.dp)
            .clickable { isExpanded.value = !isExpanded.value },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        //REQUEST INTRO:
        Text(
            modifier = Modifier
                .padding(start=16.dp)
                .weight(1f),
            text = title,
            fontSize = 14.sp,
            fontWeight = if (isExpanded.value) FontWeight.Bold else null,
            color = colorResource(id = R.color.light_grey)
        )
        //EXPAND/COLLAPSE:
        Icon(
            modifier = Modifier
                .padding(start=4.dp)
                .size(24.dp),
            imageVector = icon,
            tint = colorResource(id = R.color.light_grey),
            contentDescription = "Expand / collapse"
        )
    }
}