package com.ftrono.DJames.screen

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
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
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import com.ftrono.DJames.R
import com.ftrono.DJames.application.historyKeys
import com.ftrono.DJames.application.logDir
import com.ftrono.DJames.application.midThreshold
import com.ftrono.DJames.application.playThreshold
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.dialogs.GeneralDialog
import com.ftrono.DJames.ui.HeaderWithSign
import com.ftrono.DJames.ui.OptionsItem
import com.ftrono.DJames.ui.OptionsMenu
import com.ftrono.DJames.ui.StreetBackground
import com.ftrono.DJames.ui.historyColorSelectorLight
import com.ftrono.DJames.ui.historyIconSelector
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.Locale


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
    val historyKeysState by historyKeys.observeAsState()
    historyKeys.postValue(updateHistory(mContext, preview))

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
            num = historyKeysState!!.size
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
        if (historyKeysState!!.isEmpty()) {
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
                    historyKeysState!!
                ) { index, item ->
                    //HISTORY CARD:
                    HistoryCard(
                        item = getHistoryItem(mContext, item, preview)
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryCard(
    item: JsonObject
) {
    //INFO:
    val mContext = LocalContext.current
    val itemInfo = getHistoryItemInfo(item, mContext)
    val filename = itemInfo.get("filename").asString
    val textIntro = itemInfo.get("textIntro").asString
    val textMain = itemInfo.get("textMain").asString
    val textExtra = itemInfo.get("textExtra").asString

    var mDisplayMenu = rememberSaveable {
        mutableStateOf(false)
    }

    val deleteLogOn = rememberSaveable { mutableStateOf(false) }
    if (deleteLogOn.value) {
        DialogDeleteHistory(mContext, deleteLogOn, filename)
    }

    //Intent name:
    val intentName = if (item.has("intent_name")) {
        item.get("intent_name").asString
    } else {
        "Unknown"
    }

    //CARD:
    Card(
        onClick = { openLog(mContext, filename) },
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
                    painter = historyIconSelector(cat = intentName),
                    contentDescription = intentName,
                    tint = historyColorSelectorLight(cat = intentName)
                )
                //CAT NAME:
                Text(
                    modifier = Modifier
                        .padding(start = 4.dp),
                    color = historyColorSelectorLight(cat = intentName),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    text = "${intentName}  •  "
                )
                //INTRO & DATETIME:
                Text(
                    modifier = Modifier
                        .weight(1f),
                    color = colorResource(id = R.color.mid_grey),
                    fontSize = 12.sp,
                    text = textIntro
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
                        filename = filename)
                }
            }
            //REQUEST TEXT:
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
                text = textMain
            )
            //EXTRA INFO:
            Text(
                modifier = Modifier
                    .padding(start = 12.dp, bottom = 8.dp)
                    .wrapContentWidth()
                    .wrapContentHeight(),
                color = colorResource(id = R.color.mid_grey),
                fontSize = 12.sp,
                lineHeight = 14.sp,
                text = textExtra
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
    filename: String
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
                    openLog(mContext, filename)
                    mDisplayMenu.value = false
                }
            )
            //2) Item: SHARE LOG
            OptionsItem(
                title = "Share",
                iconVector = Icons.Default.Share,
                onClick = {
                    sendLog(mContext, filename)
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
    filename: String = ""
) {

    //DELETE DIALOG:
    GeneralDialog(
        dialogOnState = dialogOnState,
        backgroundColor = colorResource(id = R.color.colorPrimaryDark),
        title = if (filename != "") "Delete log" else "Delete history",
        content = {
            Text(
                text = if (filename != "") {
                    "Do you want to delete this log item?\n\n$filename"
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
            var toastText = ""
            if (filename != "") {
                //Delete current:
                File(logDir, filename).delete()
                Log.d("HistoryScreen", "Deleted file: $filename")
                toastText = "Log deleted!"
            } else {
                //Delete all:
                logDir!!.deleteRecursively()
                Log.d("HistoryScreen", "Deleted ALL logs.")
                toastText = "History deleted!"
            }
            historyKeys.postValue(updateHistory(mContext))   //Refresh list
            Toast.makeText(mContext, toastText, Toast.LENGTH_LONG).show()
            dialogOnState.value = false
        }
    )
}


//GET ITEM INFO:
fun getHistoryItemInfo(item: JsonObject, context: Context): JsonObject {
    var itemInfo = JsonObject()
    val trimLength = 40

    //Datetime:
    val datetime = item.get("datetime").asString

    //Intent name:
    val intentName = if (item.has("intent_name")) {
        item.get("intent_name").asString
    } else {
        "Unknown"
    }

    //Album type:
    var albumType = try {
        item.get("spotify_play").asJsonObject.get("album_type").asString.replaceFirstChar { it.uppercase() }
    } catch (e: Exception) {
        "Album"
    }

    //Context error:
    val context_error = try {
        item.get("context_error").asBoolean
    } catch (e: Exception) {
        false
    }

    //Request scoring:
    var itemScore = ""
    try {
        itemScore = if (item.has("voc_score")) {
            if (item.get("voc_score").asInt > midThreshold) {
                "🟢"
            } else {
                "🟡"
            }
        } else {
            if (!context_error && item.get("best_score").asInt >= playThreshold) {
                "🟢"
            } else {
                "🟡"
            }
        }
    } catch (e: Exception) {
        Log.d("HistoryScreen", "No score info in log item: $datetime")
    }

    //Build info:
    val queryText = item.get("nlp").asJsonObject.get("query_text").asString
    val textIntro = "${datetime.slice(0..< (datetime.length-3))}  $itemScore"
    val textMain = if (intentName.contains("Play") && !queryText.contains("play ")) {
        "play: $queryText"
    } else {
        queryText
    }

    //Extra text:
    var textExtra = ""

    if (intentName.contains("Call") || intentName.contains("Message")) {
        //Calls & Messages:
        var contacted = item.get("contact_extractor").asJsonObject.get("contact_confirmed").asString.replaceFirstChar { it.uppercase() }
        textExtra = "Contact:  $contacted"

    } else if (intentName.contains("Play")) {
        //Play requests:
        var playType = try {
            item.get("spotify_play").asJsonObject.get("play_type").asString
        } catch (e: Exception) {
            ""
        }

        if (playType == "playlist") {
            //Playlist / artist playlist / collection:
            var matchName = item.get("spotify_play").asJsonObject.get("context_name").asString.split(" ").map { it.lowercase().capitalize(
                Locale.getDefault()) }.joinToString(" ")
            textExtra = "Playlist:  $matchName"

        } else if (playType == "artist") {
            //Artist:
            var matchName = item.get("spotify_play").asJsonObject.get("context_name").asString.split(" ").map { it.lowercase().capitalize(
                Locale.getDefault()) }.joinToString(" ")
            textExtra = "Artist:  $matchName"


        } else if (playType == "album") {
            //Album:
            var matchName = utils.trimString(item.get("spotify_play").asJsonObject.get("match_name").asString, trimLength)
            var artistName = utils.trimString(item.get("spotify_play").asJsonObject.get("artist_name").asString, trimLength)
            if (albumType != "Album") {
                textExtra = "Album:  $matchName  ($albumType)\nArtist:  $artistName"
            } else {
                textExtra = "Album:  $matchName\nArtist:  $artistName"
            }

        } else {
            //Track:
            //Log.d("History", item.toString())
            var matchName = utils.trimString(item.get("spotify_play").asJsonObject.get("match_name").asString, trimLength)
            var artistName = utils.trimString(item.get("spotify_play").asJsonObject.get("artist_name").asString, trimLength)

            //Context:
            var play_externally = try {
                item.get("play_externally").asBoolean
            } catch (e: Exception) {
                false
            }
            var contextType = item.get("spotify_play").asJsonObject.get("context_type").asString.replaceFirstChar { it.uppercase() }
            var contextName = ""
            if (contextType == "Playlist" && !context_error && !play_externally) {
                //Use Playlist:
                contextName = item.get("spotify_play").asJsonObject.get("context_name").asString
            } else {
                //Default to Album type:
                contextType = albumType
                contextName = item.get("spotify_play").asJsonObject.get("album_name").asString
            }
            contextName = utils.trimString(contextName.split(" ").joinToString(" ") { it.replaceFirstChar(Char::titlecase) })
            var contextFull = "$contextName  ($contextType)"
            if (play_externally) {
                contextFull = "$contextFull [EXT]"
            }
            textExtra = "Track:  $matchName\nArtist:  $artistName\nContext:  $contextFull"
        }

    } else {
        textExtra = "(No info)"
    }


    //Store to JSON:
    itemInfo.addProperty("filename", "$datetime.json")
    itemInfo.addProperty("textIntro", textIntro)
    itemInfo.addProperty("textMain", textMain)
    itemInfo.addProperty("textExtra", textExtra)

    return itemInfo
}


//CARD ACTIONS:
//Send:
fun sendLog(mContext: Context, filename: String) {
    //Send the current file:
    val file = File(logDir, filename)
    val uriToFile = FileProvider.getUriForFile(mContext, "com.ftrono.DJames.provider", file)
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, uriToFile)
        type = "image/jpeg"
    }
    var chooserIntent = Intent.createChooser(sendIntent, null)
    chooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    chooserIntent.putExtra("fromwhere", "ser")
    startActivity(mContext, chooserIntent, null)
}


//Open:
fun openLog(mContext: Context, filename: String) {
    try {
        // Get URI and MIME type of file
        val file = File(logDir, filename)
        val uri = FileProvider.getUriForFile(mContext, "com.ftrono.DJames.provider", file)
        val mime = mContext.contentResolver.getType(uri)

        // Open file with user selected app
        val intent1 = Intent()
        intent1.setAction(Intent.ACTION_VIEW)
        intent1.setDataAndType(uri, mime)
        intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent1.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent1.putExtra("fromwhere", "ser")
        startActivity(mContext, intent1, null)
    } catch (e: Exception) {
        Log.d("HistoryScreen", "OpenLogFile(): viewer app not found!")
        Toast.makeText(mContext, "No app to open the selected file!", Toast.LENGTH_LONG).show()
    }
}


//REFRESH:
fun updateHistory(mContext: Context, preview: Boolean = false): List<String> {
    if (preview) {
        //Mock data:
        val reader = BufferedReader(InputStreamReader(mContext.resources.openRawResource(R.raw.history_sample)))
        val logItems = JsonParser.parseReader(reader).asJsonObject
        val logKeys = logItems.keySet().toList()
        return logKeys
    } else {
        //Real data:
        val logKeys = utils.getLogKeys()
        //Log.d("Items", logKeys.toString())
        return logKeys
    }
}

//GET:
fun getHistoryItem(mContext: Context, key: String, preview: Boolean = false): JsonObject {
    if (preview) {
        //Mock data:
        val reader = BufferedReader(InputStreamReader(mContext.resources.openRawResource(R.raw.history_sample)))
        val logItems = JsonParser.parseReader(reader).asJsonObject
        val logItem = logItems.get(key).asJsonObject
        return logItem
    } else {
        //Real data:
        val logItem = utils.getLogItem(key)
        //Log.d("Item", logItem.toString())
        return logItem
    }
}

