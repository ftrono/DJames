package com.ftrono.DJames.application.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ftrono.DJames.R
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.collections.GuideCategory
import com.ftrono.DJames.be.collections.fullGuide
import com.ftrono.DJames.ui.components.GeneralSectionHeader
import com.ftrono.DJames.ui.components.StreetUIScaffold
import com.ftrono.DJames.ui.navigation.StreetUITopBar
import com.ftrono.DJames.ui.navigation.UserOptions
import com.ftrono.DJames.ui.selectors.guideColorSelector
import com.ftrono.DJames.ui.selectors.guideColorSelectorLight
import com.ftrono.DJames.ui.selectors.guideIconSelector


@Preview
@Preview(heightDp = 360, widthDp = 800)
@Composable
fun GuideScreenPreview() {
    val navController = rememberNavController()
    GuideScreen(navController, preview = true)
}

@Composable
fun GuideScreen(
    navController: NavController,
    preview: Boolean = false
) {
    val mContext = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var guideStateItems = getGuideStateItems(fullGuide)
    val expandedStates = remember {
        mutableStateMapOf(*guideStateItems.map { it to false }.toTypedArray())
    }
    val currentExpanded = rememberSaveable { mutableStateOf(guideStateItems[0]) }

    // SCREEN:
    StreetUIScaffold(
        lineDistance = 20.dp,
        topBar = {
            StreetUITopBar(
                pretitle = "",
                title = "Guide",
                subtitle = "What you can ask",
                showBack = true,
                onBack = { navController.popBackStack() },   //TODO
                optionButtons = {
                    UserOptions(
                        context = mContext,
                        lifecycleOwner = lifecycleOwner,
                        navController = navController,
                        preview = preview,
                    )
                }
            )
        }
    ){
        //CONTENT:
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            //SECTIONS LIST:
            for (catItem in fullGuide) {
                var cat = catItem.category

                //CATEGORY SECTION:
                ExpandableGuideSection(
                    modifier = Modifier
                        .fillMaxWidth(),
                    cat = cat,
                    title = catItem.header
                ) {

                    //REQUESTS:
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.Start
                    ) {

                        for (reqItem in catItem.requests) {
                            //REQUEST CARD:
                            ExpandableGuideItem(
                                cat = cat,
                                requestIntro = reqItem.intro,
                                requestSentence = reqItem.sentence,
                                requestDescr = reqItem.description,
                                expandedStates = expandedStates,
                                currentExpanded = currentExpanded
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun ExpandableGuideSection(
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
        GeneralSectionHeader(
            modifier = Modifier
                .padding(
                    start = 26.dp,
                    end = 20.dp,
                    top = 8.dp,
                    bottom = 8.dp
                ),
            title = title,
            signColor = guideColorSelector(cat = cat),
            iconPainter = guideIconSelector(cat = cat),
            arrowColor = guideColorSelectorLight(cat = cat),
            expandable = true,
            isExpanded = sectionIsExpanded
        )

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
fun ExpandableGuideItem(
    cat: String,
    requestIntro: String,
    requestSentence: String,
    requestDescr: String,
    expandedStates: SnapshotStateMap<String, Boolean>,
    currentExpanded: MutableState<String>
) {
    utils.updateStatesMap(expandedStates, target=currentExpanded.value)
    val itemStateName = "$cat - $requestIntro"

    //CARD:
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .padding(start = 32.dp, top = 4.dp, end = 24.dp, bottom = 12.dp)
            .fillMaxWidth()
            .clickable {
                //Update global currentExpanded:
                if (currentExpanded.value == itemStateName) {
                    currentExpanded.value = ""
                } else {
                    currentExpanded.value = itemStateName
                }
                utils.updateStatesMap(expandedStates, target = currentExpanded.value)
            },
        border = BorderStroke(1.dp, colorResource(id = R.color.dark_grey)),
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

            //REQUEST INTRO:
            ExpandableGuideItemTitle(
                isExpanded = expandedStates[itemStateName]!!,
                cat = cat,
                title = requestIntro
            )

            //ON EXPANSION:
            AnimatedVisibility(
                modifier = Modifier
                    .fillMaxWidth(),
                visible = expandedStates[itemStateName]!!
            ) {

                //EXPANDED -> REQUEST DETAILS:
                Column(
                    modifier = Modifier
                        .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start
                ) {

                    //DIVIDER:
                    HorizontalDivider(
                        modifier = Modifier
                            .padding(bottom = 4.dp),
                        color = colorResource(id = R.color.dark_grey)
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
}


@Composable
fun ExpandableGuideItemTitle(
    isExpanded: Boolean,
    cat: String,
    title: String
) {
    val icon = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown

    //SECTION HEADER:
    Row(
        modifier = Modifier
            .padding(start=10.dp, end = 20.dp, top=10.dp, bottom=8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        //CAT ICON:
        Icon(
            modifier = Modifier
                .padding(start = 10.dp)
                .size(16.dp),
            painter = guideIconSelector(cat = cat),
            contentDescription = cat,
            tint = guideColorSelectorLight(cat = cat)
        )
        //REQUEST INTRO:
        Text(
            modifier = Modifier
                .padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 2.dp)
                .weight(1f),
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isExpanded) guideColorSelectorLight(cat = cat) else colorResource(id = R.color.light_grey)
        )
        //EXPAND/COLLAPSE:
        Icon(
            modifier = Modifier
                .padding(start = 4.dp)
                .size(24.dp),
            imageVector = icon,
            tint = guideColorSelectorLight(cat = cat),
            contentDescription = "Expand / collapse"
        )
    }
}


// GET GUIDE:
// Get list of Guide items for state:
fun getGuideStateItems(fullGuide: List<GuideCategory>): List<String> {
    var guideItems = mutableListOf<String>()
    for (item in fullGuide) {
        var cat = item.category
        var requests = item.requests
        for (req in requests) {
            var intro = req.intro
            guideItems.add("$cat - $intro")
        }
    }
    return guideItems
}
