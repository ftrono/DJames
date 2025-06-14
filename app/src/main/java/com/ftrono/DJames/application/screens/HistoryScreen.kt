package com.ftrono.DJames.application.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ftrono.DJames.R
import com.ftrono.DJames.application.historyItems
import com.ftrono.DJames.application.logUtils
import com.ftrono.DJames.application.midThreshold
import com.ftrono.DJames.application.playThreshold
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.database.HistoryLog
import com.ftrono.DJames.be.database.LogViewInfo
import com.ftrono.DJames.ui.dialogs.GeneralDialog
import com.ftrono.DJames.ui.components.HeaderWithSign
import com.ftrono.DJames.ui.components.OptionsItem
import com.ftrono.DJames.ui.components.OptionsMenu
import com.ftrono.DJames.ui.components.StreetBackground
import com.ftrono.DJames.ui.selectors.historyColorSelectorLight
import com.ftrono.DJames.ui.selectors.historyIconSelector
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json


@Preview
@Preview(heightDp = 360, widthDp = 800)
@Composable
fun HistoryScreenPreview() {
    HistoryScreen(preview=true)
}

@Composable
fun HistoryScreen(preview: Boolean = false) {
//    val configuration = LocalConfiguration.current
//    val isLandscape by remember { mutableStateOf(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) }
    val mContext = LocalContext.current
    val historyItemsState by historyItems.observeAsState()
    historyItems.postValue(logUtils.refreshHistory(preview))

    val mDisplayMainMenu = rememberSaveable {
        mutableStateOf(false)
    }

    val deleteAllOn = rememberSaveable { mutableStateOf(false) }
    if (deleteAllOn.value) {
        DialogDeleteHistory(mContext, deleteAllOn)
    }

    //BACKGROUND:
    StreetBackground(
        startDistance = 20
    ) {
        //HEADER:
        HeaderWithSign(
            iconRes = painterResource(id = R.drawable.sign_history),
            title = "History",
            subtitle = "Last 30 days",
            num = historyItemsState!!.size
        ) {
            Box() {
                //1) CAT OPTIONS:
                Icon(
                    modifier = Modifier
                        .padding(end = 18.dp)
                        .size(35.dp)
                        .clickable {
                            mDisplayMainMenu.value = !mDisplayMainMenu.value
                        },
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Add library item",
                    tint = colorResource(id = R.color.colorAccentLight)
                )
                HistoryOptions(
                    mDisplayMenu = mDisplayMainMenu,
                    deleteAllOn = deleteAllOn,
                )
            }
        }

        //CONTENT:
        if (historyItemsState!!.isEmpty()) {
            //HISTORY EMPTY:
            Text(
                text = "History is empty",
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                color = colorResource(id = R.color.mid_grey),
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentHeight()
                    .wrapContentWidth()
            )
        } else {
            //HISTORY LIST:
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                state = rememberLazyListState()
            ) {
                itemsIndexed(
                    historyItemsState!!
                ) { index, item ->
                    //HISTORY CARD:
                    HistoryCard(
                        itemJson = item
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryCard(
    itemJson: String
) {
    //INFO:
    val mContext = LocalContext.current
    val logItem = Json.decodeFromString<HistoryLog>(itemJson)
    val viewInfo = getLogViewInfo(logItem)

    var mDisplayMenu = rememberSaveable {
        mutableStateOf(false)
    }

    val deleteLogOn = rememberSaveable { mutableStateOf(false) }
    if (deleteLogOn.value) {
        DialogDeleteHistory(mContext, deleteLogOn, logItem.id, logItem.datetime)
    }

    //CARD:
    Card(
        onClick = {
            // Open Log file via external app:
            val filename = logUtils.prepareLogFile(mContext, logItem.id)
            logUtils.openLogViaApp(mContext, filename)
          },
        modifier = Modifier
            .padding(
                start = 32.dp,
                end = 24.dp,
                top = 8.dp,
                bottom = 8.dp
            )
            .fillMaxWidth()
            .wrapContentHeight(),
        border = BorderStroke(1.dp, colorResource(id = R.color.faded_grey)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors (
            containerColor = colorResource(id = R.color.dark_grey_background)
        )
    ) {
        Column (
            modifier = Modifier
                .padding(10.dp)
        ) {
            //INTRO ROW:
            Row(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                //CAT ICON:
                Icon(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(16.dp),
                    painter = historyIconSelector(cat = viewInfo.intentName),
                    contentDescription = viewInfo.intentName,
                    tint = historyColorSelectorLight(cat = viewInfo.intentName)
                )
                //CAT NAME:
                Text(
                    modifier = Modifier
                        .padding(start = 4.dp),
                    color = historyColorSelectorLight(cat = viewInfo.intentName),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    text = "${viewInfo.intentName}  •  "
                )
                //INTRO TEXT & DATETIME:
                Text(
                    modifier = Modifier
                        .weight(1f),
                    color = colorResource(id = R.color.mid_grey),
                    fontSize = 12.sp,
                    text = viewInfo.head
                )
                //"MORE OPTIONS" BUTTON:
                Box() {
                    Icon(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clickable { mDisplayMenu.value = !mDisplayMenu.value },
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "",
                        tint = colorResource(id = R.color.light_grey)
                    )
                    HistoryItemOptions(
                        mContext = mContext,
                        mDisplayMenu = mDisplayMenu,
                        deleteLogOn = deleteLogOn,
                        id = viewInfo.id)
                }
            }
            //MAIN TEXT:
            Text(
                modifier = Modifier
                    .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 10.dp)
                    .wrapContentWidth()
                    .wrapContentHeight(),
                color = colorResource(id = R.color.light_grey),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                lineHeight = 16.sp,
                text = viewInfo.main
            )
            //DETAIL TEXT:
            Text(
                modifier = Modifier
                    .padding(start = 12.dp, bottom = 8.dp)
                    .wrapContentWidth()
                    .wrapContentHeight(),
                color = colorResource(id = R.color.mid_grey),
                fontSize = 12.sp,
                lineHeight = 14.sp,
                text = viewInfo.detail
            )
        }
    }
}


//DROPDOWN MENU:
@Composable
fun HistoryOptions(
    mDisplayMenu: MutableState<Boolean>,
    deleteAllOn: MutableState<Boolean>
) {
    //DROPDOWN MENU:
    OptionsMenu(
        expandedState = mDisplayMenu,
        backgroundColor = colorResource(id = R.color.dark_grey),
        options = {
            //1) Item: DELETE HISTORY
            OptionsItem(
                title = "Delete history",
                iconVector = Icons.Default.Delete,
                onClick = {
                    mDisplayMenu.value = false
                    deleteAllOn.value = true
                }
            )
        }
    )
}


//DROPDOWN MENU:
@Composable
fun HistoryItemOptions(
    mContext: Context,
    mDisplayMenu: MutableState<Boolean>,
    deleteLogOn: MutableState<Boolean>,
    id: Long
) {
    //DROPDOWN MENU:
    OptionsMenu(
        expandedState = mDisplayMenu,
        backgroundColor = colorResource(id = R.color.dark_grey),
        options = {
            //1) Item: VIEW LOG
            OptionsItem(
                title = "View",
                iconVector = Icons.Default.Search,
                onClick = {
                    val filename = logUtils.prepareLogFile(mContext, id)
                    logUtils.openLogViaApp(mContext, filename)
                    mDisplayMenu.value = false
                }
            )
            //2) Item: SHARE LOG
            OptionsItem(
                title = "Share",
                iconVector = Icons.Default.Share,
                onClick = {
                    //Prepare & send cached file:
                    val filename = logUtils.prepareLogFile(mContext, id)
                    utils.sendCachedFile(mContext, filename)
                    mDisplayMenu.value = false
                }
            )
            //3) Item: DELETE LOG
            OptionsItem(
                title = "Delete",
                iconVector = Icons.Default.Delete,
                onClick = {
                    mDisplayMenu.value = false
                    deleteLogOn.value = true
                }
            )
        }
    )
}


@Composable
fun DialogDeleteHistory(
    mContext: Context,
    dialogOnState: MutableState<Boolean>,
    id: Long? = null,
    datetime: String = "",
) {

    //DELETE DIALOG:
    GeneralDialog(
        dialogOnState = dialogOnState,
        backgroundColor = colorResource(id = R.color.colorPrimaryDark),
        title = if (id != null) "Delete log" else "Delete history",
        content = {
            Text(
                text = if (id != null) {
                    "Do you want to delete this log item?\n\n$datetime"
                } else {
                    "Do you want to delete all history logs?"
                },
                color = colorResource(id = R.color.light_grey),
                fontSize = 14.sp
            )
        },
        dismissText = "No",
        confirmText = "Yes",
        onConfirm = {
            if (id != null) {
                //Delete current:
                logUtils.deleteLogItem(mContext, id)
            } else {
                //Delete all:
                logUtils.deleteHistory(mContext)
            }
            historyItems.postValue(logUtils.refreshHistory())   //Refresh list
            dialogOnState.value = false
        }
    )
}


//BUILD LOG VIEW INFO:
fun getLogViewInfo(logItem: HistoryLog): LogViewInfo {
    val trimLength = 40
    val intentName = logItem.keyInfo.intentName
    var viewInfo = LogViewInfo(
        id = logItem.id,
        intentName = intentName
    )

    //Request scoring:
    var itemScore = if (intentName.contains("Play") && logItem.keyInfo.bestScore > 0) {
            if (!logItem.keyInfo.contextError && logItem.keyInfo.bestScore >= playThreshold) {
                "🟢"
            } else {
                "🟡"
            }
        } else if (logItem.keyInfo.libScore > midThreshold || logItem.keyInfo.libScore == 0) {
            "🟢"
        } else {
            "🟡"
        }

    //Build info:
    viewInfo.head = "${logItem.datetime.slice(0..< (logItem.datetime.length-3))}  $itemScore"

    //Main text:
    val queryText = logItem.keyInfo.queryText
    viewInfo.main = if (intentName.contains("Play") && !queryText.contains("play ")) {
        "play: $queryText"
    } else if (intentName.contains("Drive")) {
        "drive: $queryText"
    } else {
        queryText
    }

    //Detail text:
    var detailText = ""
    if (intentName.contains("Call") || intentName.contains("Message")) {
        //Calls & Messages:
        val itemInfo = logItem.usable
        detailText = "Contact:  ${itemInfo.name}"

    } else if (intentName.contains("Drive")) {
        //Drive:
        val itemInfo = logItem.usable
        if (itemInfo.detail == "") {
            detailText = "Route:  ${itemInfo.name}"
        } else {
            detailText = "Route:  ${itemInfo.name}\nDetail:  ${itemInfo.detail}"
        }

    } else if (intentName.contains("Play")) {
        //Play requests:
        val playable = logItem.spotifyPlay

        if (playable.type == "podcast" || playable.type == "episode") {
            //Podcast:
            var podcastName = utils.capitalizeWords(playable.contextName)
            var episodeName = utils.capitalizeWords(playable.name)
            var episodeDate = utils.capitalizeWords(playable.releaseDate)
            detailText = "Podcast:  $podcastName\nEpisode:  ($episodeDate) $episodeName"

        } else if (playable.type == "playlist" || playable.type == "collection") {
            //Playlist / artist playlist / collection:
            detailText = "Playlist:  ${utils.capitalizeWords(playable.name)}"

        } else if (playable.type == "artist") {
            //Artist:
            detailText = "Artist:  ${utils.capitalizeWords(playable.name)}"

        } else if (playable.type == "album") {
            //Album:
            var matchName = utils.trimString(playable.name, trimLength)
            var artistName =
                utils.trimString(playable.artistsNames.joinToString(", "), trimLength)
            if (playable.albumType != "album") {
                detailText =
                    "Album:  $matchName  (${utils.capitalizeWords(playable.albumType)})\nArtist:  $artistName"
            } else {
                detailText = "Album:  $matchName\nArtist:  $artistName"
            }

        } else {
            //Track:
            var matchName = utils.trimString(playable.name, trimLength)
            var artistName =
                utils.trimString(playable.artistsNames.joinToString(", "), trimLength)

            //Context:
            var contextType = playable.contextType
            var contextName = ""
            if (contextType == "Playlist" && !logItem.keyInfo.contextError && !logItem.keyInfo.playedExternally) {
                //Use Playlist:
                contextName = playable.contextName
            } else {
                //Default to Album type:
                contextType = utils.capitalizeWords(playable.albumType)
                contextName = playable.albumName
            }
            var contextFull = "$contextName  ($contextType)"
            if (logItem.keyInfo.playedExternally) {
                contextFull = "$contextFull [EXT]"
            }
            detailText = "Track:  $matchName\nArtist:  $artistName\nContext:  $contextFull"
        }

    } else {
        detailText = "(No info)"
    }

    //Store:
    viewInfo.detail = detailText
    return viewInfo
}
