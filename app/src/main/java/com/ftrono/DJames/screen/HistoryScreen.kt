package com.ftrono.DJames.screen

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import com.ftrono.DJames.R
import com.ftrono.DJames.application.filter
import com.ftrono.DJames.application.historySize
import com.ftrono.DJames.application.logDir
import com.ftrono.DJames.application.midThreshold
import com.ftrono.DJames.application.playThreshold
import com.ftrono.DJames.utilities.Utilities
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

    var historyLogs by rememberSaveable {
        mutableStateOf(updateHistory(mContext, preview))
    }
    var historyItems = JsonParser.parseString(historyLogs).asJsonArray.toList()

    val historySizeState by historySize.observeAsState()
    historySize.postValue(historyItems.size)

    val deleteAllOn = rememberSaveable { mutableStateOf(false) }
    if (deleteAllOn.value) {
        DialogDeleteHistory(mContext, deleteAllOn)
    } else {
        historyLogs = updateHistory(mContext, preview)
        historyItems = JsonParser.parseString(historyLogs).asJsonArray.toList()
        historySize.postValue(historyItems.size)
    }

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
                    text = "🕑  History",
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
                    text = if (historySizeState == 1) "1 request (last 30 days)" else "$historySizeState requests (last 30 days)",
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
            //OPTIONS BUTTONS:
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ){
                //REFRESH BUTTON:
                IconButton(
                    onClick = {
                        historyLogs = updateHistory(mContext)
                        historyItems = JsonParser.parseString(historyLogs).asJsonArray.toList()
                        historySize.postValue(historyItems.size)
                        Toast.makeText(mContext, "History updated!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(35.dp),
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = colorResource(id = R.color.colorAccentLight)
                    )
                }
                //DELETE BUTTON:
                IconButton(
                    modifier = Modifier
                        .padding(end=12.dp),
                    onClick = {
                        deleteAllOn.value = true
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(35.dp),
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete All",
                        tint = colorResource(id = R.color.colorAccentLight)
                    )
                }
            }
        }
        if (historySizeState == 0) {
            //HISTORY EMPTY:
            Text(
                text = "History is empty!\n\n🕑",
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                fontStyle = FontStyle.Italic,
                color = colorResource(id = R.color.mid_grey),
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentHeight()
                    .wrapContentWidth()
            )
        } else {
            //HISTORY LIST:
            LazyColumn (
                modifier = Modifier
                    .fillMaxSize(),
                state = rememberLazyListState()
            ) {
                itemsIndexed(
                    JsonParser.parseString(historyLogs).asJsonArray.toList()
                ) { index, item ->
                    //HISTORY CARD:
                    HistoryCard(item = item.asJsonObject)
                }
            }
        }

    }

}

@Composable
fun HistoryCard(item: JsonObject) {
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

    //CARD:
    Card(
        onClick = { openLog(mContext, filename) },
        modifier = Modifier
            .padding(
                start=20.dp,
                end=20.dp,
                top=8.dp,
                bottom=8.dp
            )
            .fillMaxWidth()
            .wrapContentHeight(),
        //border = BorderStroke(1.dp, colorResource(id = R.color.dark_grey)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors (
            containerColor = colorResource(id = R.color.dark_grey_background)
        )
    ) {
        Column (
            modifier = Modifier
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                //INTRO & DATETIME:
                Text(
                    modifier = Modifier
                        .padding(start = 12.dp, top=2.dp)
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .weight(1f),
                    color = colorResource(id = R.color.mid_grey),
                    fontSize = 12.sp,
                    text = textIntro
                )
                //"MORE OPTIONS" BUTTON:
                Box() {
                    Icon(
                        modifier = Modifier
                            .padding(end=4.dp)
                            .clickable { mDisplayMenu.value = !mDisplayMenu.value },
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "",
                        tint = colorResource(id = R.color.light_grey)
                    )
                    HistoryOptions(
                        mContext = mContext,
                        mDisplayMenu = mDisplayMenu,
                        deleteLogOn = deleteLogOn,
                        filename = filename)
                }
            }
            //REQUEST TEXT:
            Text(
                modifier = Modifier
                    .padding(start = 12.dp, top=4.dp, bottom = 10.dp)
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
                color = colorResource(id = R.color.colorAccentLight),
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
    mContext: Context,
    mDisplayMenu: MutableState<Boolean>,
    deleteLogOn: MutableState<Boolean>,
    filename: String
) {
    //DROPDOWN MENU:
    DropdownMenu(
        modifier = Modifier
            .background(colorResource(id = R.color.dark_grey)),
        shape = RoundedCornerShape(20.dp),
        expanded = mDisplayMenu.value,
        onDismissRequest = {
            mDisplayMenu.value = false
        }
    ) {

        //1) Item: SHARE LOG
        DropdownMenuItem(
            text = {
                Text(
                    text = "Share",
                    color = colorResource(id = R.color.light_grey),
                    fontSize = 16.sp
                )},
            leadingIcon = {
                Icon(
                    Icons.Default.Share,
                    "Share",
                    tint = colorResource(id = R.color.mid_grey)
                )
            },
            onClick = {
                sendLog(mContext, filename)
                mDisplayMenu.value = false
            }
        )

        //2) Item: DELETE LOG
        DropdownMenuItem(
            text = {
                Text(
                    text = "Delete log",
                    color = colorResource(id = R.color.light_grey),
                    fontSize = 16.sp
                )},
            leadingIcon = {
                Icon(
                    Icons.Default.Delete,
                    "Delete log",
                    tint = colorResource(id = R.color.mid_grey)
                )
            },
            onClick = {
                mDisplayMenu.value = false
                deleteLogOn.value = true
            }
        )
    }
}


@Composable
fun DialogDeleteHistory(mContext: Context, dialogOnState: MutableState<Boolean>, filename: String = "") {
    //DELETE DIALOG:
    if (dialogOnState.value) {
        AlertDialog(
            onDismissRequest = {
                //cancelable -> true:
                dialogOnState.value = false
            },
            containerColor = colorResource(id = R.color.dark_grey),
            title = {
                Text(
                    text = if (filename != "") "Delete log" else "Delete history",
                    color = colorResource(id = R.color.light_grey)
                ) },
            text = {
                Text(
                    text = if (filename != "") {
                        "Do you want to delete this log item?\n\n$filename"
                    } else {
                        "Do you want to delete all history logs?"
                    },
                    color = colorResource(id = R.color.mid_grey)
                ) },
            dismissButton = {
                Text(
                    modifier = Modifier
                        .padding(end=20.dp)
                        .clickable { dialogOnState.value = false },
                    text = "No",
                    fontSize = 14.sp,
                    color = colorResource(id = R.color.colorAccentLight)
                )
            },
            confirmButton = {
                Text(
                    modifier = Modifier
                        .clickable {
                            dialogOnState.value = false
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
                            historySize.postValue(historySize.value!! - 1)   //Refresh list
                            Toast.makeText(mContext, toastText, Toast.LENGTH_LONG).show()
                        },
                    text = "Yes",
                    fontSize = 14.sp,
                    color = colorResource(id = R.color.colorAccentLight)
                )
            }
        )
    }
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

    //Intent icon:
    val intentIcon = if (intentName == "CallRequest") {
        "📞"
    } else if (intentName == "MessageRequest") {
        "💬"
    } else if (intentName.contains("Play")) {
        "🎧"
    } else {
        "❔"
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
    val textIntro = "$intentIcon  $itemScore  $datetime  •  $intentName"
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

        var utils = Utilities()
        if (playType == "playlist") {
            //Playlist / artist / collection:
            var matchName = item.get("spotify_play").asJsonObject.get("context_name").asString.split(" ").map { it.lowercase().capitalize(
                Locale.getDefault()) }.joinToString(" ")
            textExtra = "Playlist:  $matchName"


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
fun updateHistory(mContext: Context, preview: Boolean = false): String {
    if (preview) {
        //Mock data:
        val reader = BufferedReader(InputStreamReader(mContext.resources.openRawResource(R.raw.history_sample)))
        val logItems = JsonParser.parseReader(reader).asJsonArray
        return logItems.toString()
    } else {
        //Real data:
        val utils = Utilities()
        val logItems = utils.getLogArray()
        //Log.d("Items", logItems.toString())
        return logItems.toString()
    }
}
