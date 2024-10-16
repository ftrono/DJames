package com.ftrono.DJames.screen

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ftrono.DJames.R
import com.ftrono.DJames.application.spotifyLoggedIn
import com.ftrono.DJames.application.vocDir
import com.ftrono.DJames.application.vocabularySize
import com.ftrono.DJames.ui.theme.MyDJamesItem
import com.ftrono.DJames.utilities.Utilities
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader


@Preview
@Preview(heightDp = 360, widthDp = 800)
@Composable
fun VocabularyScreenPreview() {
    val navController = rememberNavController()
    VocabularyScreen(navController, "contact", MyDJamesItem.Playlists, editPreview=false, preview=true)
}

@Composable
fun VocabularyScreen(navController: NavController, filter: String, myDJamesItem: MyDJamesItem, editPreview: Boolean = false, preview: Boolean = false) {
    val mContext = LocalContext.current
    val spotifyLoggedInState by spotifyLoggedIn.observeAsState()

    var textNoData = ""
    if (filter == "contact") {
        textNoData =
            "Your contacts vocabulary is empty!\n\nLet DJames know your favourite contacts'\nnames and phone numbers by\nwriting them here.\n\n✏️"
    } else if (filter == "playlist") {
        textNoData =
            "Your playlists vocabulary is empty!\n\nLet DJames know your playlists by\nwriting their names & links here.\n\n✏️"
    } else {
        textNoData =
            "Your ${filter}s vocabulary is empty!\n\nHelp DJames understand your\n lesser known or hard-to-spell $filter names\nby writing them here.\n\n✏️"
    }

    var vocabulary by rememberSaveable {
        mutableStateOf(updateVocabulary(mContext, filter, preview))
    }
    var vocItems = JsonParser.parseString(vocabulary).asJsonObject
    var listItems = vocItems.keySet().toList()
    val vocabularySizeState by vocabularySize.observeAsState()
    vocabularySize.postValue(vocItems.size())

    //ALL:
    val deleteAllOn = rememberSaveable { mutableStateOf(false) }
    if (deleteAllOn.value) {
        DialogDeleteVocabulary(mContext, deleteAllOn, filter)
    } else {
        vocabulary = updateVocabulary(mContext, filter, preview)
        vocItems = JsonParser.parseString(vocabulary).asJsonObject
        listItems = vocItems.keySet().toList()
        vocabularySize.postValue(vocItems.size())
    }

    val editVocOn = rememberSaveable { mutableStateOf(editPreview) }
    if (editVocOn.value || editPreview) {
        DialogEditVocabulary(mContext, editVocOn, filter, vocItems, key="", preview)
    } else {
        vocabulary = updateVocabulary(mContext, filter, preview)
        vocItems = JsonParser.parseString(vocabulary).asJsonObject
        listItems = vocItems.keySet().toList()
        vocabularySize.postValue(vocItems.size())
    }

    Scaffold(
        floatingActionButton = {
            if (spotifyLoggedInState!!) {
                ExtendedFloatingActionButton(
                    containerColor = colorResource(id = R.color.colorAccent),
                    icon = {
                        Icon(
                            Icons.Default.Add,
                            "Add vocabulary item",
                            tint = colorResource(id = R.color.light_grey)
                        ) },
                    text = {
                        Text(
                            text = "Add",
                            color = colorResource(id = R.color.light_grey),
                            fontSize = 16.sp
                        ) },
                    onClick = {
                        editVocOn.value = true
                    }
                )
            }
        }
    ) {
        Box(
            modifier = Modifier
                .padding(it)
        ) {
            Column(
                modifier = Modifier
                    .padding(it)
                    .fillMaxSize()
                    .background(colorResource(id = R.color.windowBackground))
            ) {
                //HEADER:
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(colorResource(id = R.color.windowBackground)),
                    contentAlignment = Alignment.Center
                ) {
                    //BG_IMAGE:
                    Image(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(0f),
                        contentScale = ContentScale.Crop,
                        painter = painterResource(myDJamesItem.background),
                        contentDescription = "DJames logo"
                    )
                    //HEADER CONTENT:
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                //GRADIENT:
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.0f to colorResource(id = R.color.transparent_full),
                                        0.3f to colorResource(id = R.color.transparent_full),
                                        1f to colorResource(id = R.color.windowBackground)
                                    )
                                )
                            ),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        //BACK:
                        IconButton(
                            onClick = {
                                navController.popBackStack()
                            }
                        ) {
                            Icon(
                                modifier = Modifier
                                    .offset(x = (6.dp))
                                    .size(32.dp),
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = colorResource(id = R.color.colorAccentLight)
                            )
                        }
                        //TEXT HEADERS:
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = if (filter == "artist") {
                                    "Your hard-to-spell artists"
                                } else if (filter == "playlist") {
                                    "Your favourite playlists"
                                } else if (filter == "contact") {
                                    "Your favourite contacts"
                                } else {
                                    ""
                                },
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorResource(id = R.color.light_grey),
                                modifier = Modifier
                                    .padding(
                                        start = 6.dp,
                                        top = 14.dp
                                    )
                                    .wrapContentWidth()
                            )
                            Text(
                                text = if (vocabularySizeState == 1) "1 item" else "$vocabularySizeState items",
                                fontSize = 14.sp,
                                fontStyle = FontStyle.Italic,
                                color = colorResource(id = R.color.colorAccentLight),
                                modifier = Modifier
                                    .padding(
                                        start = 6.dp,
                                        bottom = 20.dp
                                    )
                                    .wrapContentWidth()
                            )
                        }
                    }
                    //OPTIONS BUTTONS:
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        //REFRESH BUTTON:
                        IconButton(
                            onClick = {
                                vocabulary = updateVocabulary(mContext, filter)
                                vocItems = JsonParser.parseString(vocabulary).asJsonObject
                                listItems = vocItems.keySet().toList()
                                vocabularySize.postValue(vocItems.size())
                                Toast.makeText(mContext, "Vocabulary updated!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                modifier = Modifier
                                    .offset(x = (6.dp))
                                    .size(32.dp),
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = colorResource(id = R.color.colorAccentLight)
                            )
                        }
                        //DELETE BUTTON:
                        IconButton(
                            modifier = Modifier
                                .padding(end = 12.dp),
                            onClick = {
                                deleteAllOn.value = true
                            }
                        ) {
                            Icon(
                                modifier = Modifier.size(32.dp),
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete All",
                                tint = colorResource(id = R.color.colorAccentLight)
                            )
                        }
                    }
                }
                if (vocabularySizeState == 0) {
                    //VOCABULARY EMPTY:
                    Text(
                        text = textNoData,
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
                    //VOCABULARY LIST:
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        itemsIndexed(
                            listItems
                        ) { index, item ->
                            //HISTORY CARD:
                            VocabularyCard(item, filter, myDJamesItem, vocItems, preview)
                            if (index == listItems.lastIndex) Spacer(modifier = Modifier.padding(40.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VocabularyCard(key: String, filter: String, myDJamesItem: MyDJamesItem, vocItems: JsonObject, preview: Boolean = false) {
    val mContext = LocalContext.current

    val editVocOn = rememberSaveable { mutableStateOf(false) }
    if (editVocOn.value) {
        DialogEditVocabulary(mContext, editVocOn, filter, vocItems, key, preview)
    }

    val deleteVocOn = rememberSaveable { mutableStateOf(false) }
    if (deleteVocOn.value) {
        DialogDeleteVocabulary(mContext, deleteVocOn, filter, key)
    }
    //CARD:
    Card(
        onClick = {
            editVocOn.value = true
        },
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .wrapContentHeight(),
        //border = BorderStroke(1.dp, colorResource(id = R.color.dark_grey)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors (
            containerColor = colorResource(id = R.color.windowBackground)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ){
            //VOC TEXT:
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ){
                Image(
                    modifier = Modifier
                        .padding(start = 8.dp, bottom = 8.dp)
                        .size(35.dp),
                    painter = painterResource(id = myDJamesItem.listIcon),
                    contentDescription = "Vocabulary"
                )
                Text(
                    modifier = Modifier
                        .padding(start = 12.dp, bottom = 8.dp)
                        .wrapContentWidth()
                        .wrapContentHeight(),
                    color = colorResource(id = R.color.light_grey),
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    text = key
                )
                }
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ){
                //EDIT BUTTON:
                IconButton(
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(30.dp),
                    onClick = {
                        editVocOn.value = true
                    }) {
                    Icon(
                        modifier = Modifier.size(27.dp),
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit item",
                        tint = colorResource(id = R.color.mid_grey)
                    )
                }
                //DELETE BUTTON:
                IconButton(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(30.dp),
                    onClick = {
                        deleteVocOn.value = true
                    }) {
                    Icon(
                        modifier = Modifier.size(27.dp),
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete item",
                        tint = colorResource(id = R.color.mid_grey)
                    )
                }
            }
        }
    }
}


@Composable
fun DialogDeleteVocabulary(mContext: Context, dialogOnState: MutableState<Boolean>, filter: String, key: String = "") {
    val utils = Utilities()
    //DELETE DIALOG:
    if (dialogOnState.value) {
        AlertDialog(
            onDismissRequest = {
                //cancelable -> true
                dialogOnState.value = false
            },
            containerColor = colorResource(id = R.color.dark_grey),
            title = {
                Text(
                    text = if (key != "") "Delete item" else "Delete $filter vocabulary",
                    color = colorResource(id = R.color.light_grey)
                ) },
            text = {
                Text(
                    text = if (key != "") {
                        "Do you want to delete this $filter?\n\n$key"
                    } else {
                        "Do you want to delete all $filter in vocabulary?"
                    },
                    color = colorResource(id = R.color.mid_grey)
                ) },
            dismissButton = {
                Text(
                    modifier = Modifier
                        .padding(end = 20.dp)
                        .clickable {
                            dialogOnState.value = false
                        },
                    text = "No",
                    fontSize = 14.sp,
                    color = colorResource(id = R.color.colorAccentLight)
                )
            },
            confirmButton = {
                Text(
                    modifier = Modifier
                        .clickable {
                            if (key != "") {
                                //Delete current:
                                var ret = utils.editVocFile(prevText=key)
                                if (ret == 0) {
                                    vocabularySize.postValue(vocabularySize.value!! - 1)   //Refresh list
                                    Toast.makeText(mContext, "Deleted!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(mContext, "ERROR: Vocabulary not updated!", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                //Delete all:
                                File(vocDir, "voc_${filter}s.json").delete()
                                Log.d("VocabularyScreen", "Deleted ${filter}s vocabulary.")
                                vocabularySize.postValue(0)   //Refresh list
                                Toast.makeText(mContext, "${filter.replaceFirstChar { it.uppercase() }} vocabulary deleted!", Toast.LENGTH_LONG).show()
                            }
                            dialogOnState.value = false
                        },
                    text = "Yes",
                    fontSize = 14.sp,
                    color = colorResource(id = R.color.colorAccentLight)
                )
            }
        )
    }
}


//REFRESH:
fun updateVocabulary(mContext: Context, filter: String, preview: Boolean = false): String {
    if (preview) {
        //Mock data:
        val reader = BufferedReader(InputStreamReader(mContext.resources.openRawResource(R.raw.vocabulary_sample)))
        val vocItems = JsonParser.parseReader(reader).asJsonObject.get(filter).asJsonObject
        return vocItems.toString()
    } else {
        //Real data:
        val utils = Utilities()
        val vocItems = utils.getVocabulary(filter=filter)
        Log.d("Vocabulary", vocItems.toString())
        return vocItems.toString()
    }
}
