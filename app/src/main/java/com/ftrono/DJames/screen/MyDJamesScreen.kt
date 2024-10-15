package com.ftrono.DJames.screen

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ftrono.DJames.R
import com.ftrono.DJames.ui.navigateTo
import com.ftrono.DJames.ui.theme.MyDJamesItem
import com.ftrono.DJames.utilities.Utilities


@Preview
@Preview(heightDp = 360, widthDp = 800)
@Composable
fun MyDJamesPreview() {
    val navController = rememberNavController()
    MyDJamesScreen(navController)
}


@Composable
fun MyDJamesScreen(navController: NavController) {
    Column (
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.windowBackground))
    ) {
        //SCREEN HEADER:
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(colorResource(id = R.color.windowBackground)),
            contentAlignment = Alignment.CenterStart
        ) {
            //Header:
            Text(
                text = "🎧  My DJames",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.light_grey),
                modifier = Modifier
                    .padding(
                        start = 10.dp,
                        top = 20.dp,
                        bottom = 10.dp
                    )
                    .wrapContentWidth()
            )
        }
        //CONTENT:
        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            //VOCABULARY:
            //Headers:
            Text(
                text = "Vocabulary",
                fontSize = 20.sp,
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
                text = "Help DJames understand you:",
                fontSize = 14.sp,
                color = colorResource(id = R.color.mid_grey),
                modifier = Modifier
                    .padding(
                        start = 10.dp,
                        bottom = 8.dp
                    )
                    .wrapContentWidth()
            )
            //CARDS:
            DJamesCardsRow(
                navController,
                listOf(
                    MyDJamesItem.Artists,
                    MyDJamesItem.Playlists
                )
            )
            DJamesCardsRow(
                navController,
                listOf(
                    MyDJamesItem.Contacts,
                    MyDJamesItem.Places
                )
            )

            //FOR YOU:
            //Header:
            Text(
                text = "For you",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.light_grey),
                modifier = Modifier
                    .padding(
                        start = 10.dp,
                        top = 30.dp,
                        bottom = 8.dp
                    )
                    .wrapContentWidth()
            )
            //Cards:
            DJamesCardsRow(
                navController,
                listOf(
                    MyDJamesItem.Parking,
                    MyDJamesItem.News
                )
            )
        }
    }
}


@Composable
fun DJamesCardsRow(navController: NavController, items: List<MyDJamesItem>) {
    val mContext = LocalContext.current
    val utils = Utilities()

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ){
        for (item in items) {
            //ARTISTS:
            Card(
                modifier = Modifier
                    .padding(12.dp)
                    .weight(0.5f)
                    .height(120.dp)
                    .shadow(
                        elevation = 2.dp,
                        spotColor = colorResource(id = R.color.colorAccentLight),
                        shape = RoundedCornerShape(10.dp)
                    ),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(id = R.color.dark_grey_background)
                ),
                onClick = {
                    if (item.route in listOf("artist", "playlist", "contact")) {
                        navigateTo(navController, item.route, inner=true)
                    } else {
                        Toast.makeText(mContext, "Not ready yet!", Toast.LENGTH_LONG).show()
                    }
                }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    //BG_IMAGE:
                    Image(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(0f),
                        contentScale = ContentScale.Crop,
                        painter = painterResource(item.background),
                        contentDescription = "DJames logo"
                    )
                    //ICONS & DESCRIPTION:
                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        //Icon:
                        Icon(
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .size(30.dp),
                            painter = painterResource(id = item.icon),
                            contentDescription = "",
                            tint = colorResource(id = R.color.colorAccentLight)
                        )
                        //Item title (category):
                        Text(
                            text = item.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(id = R.color.light_grey),
                            modifier = Modifier
                                .wrapContentWidth()
                        )
                        //Item subtitle (no. items):
                        Text(
                            text = utils.updateItemsCount(item.route, "item"),
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 18.sp,
                            color = colorResource(id = R.color.light_grey),
                            modifier = Modifier
                                .wrapContentWidth()
                        )
                    }
                }
            }
        }
    }
}
