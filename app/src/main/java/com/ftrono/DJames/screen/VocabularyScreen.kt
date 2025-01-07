package com.ftrono.DJames.screen

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
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
import com.ftrono.DJames.application.filter
import com.ftrono.DJames.application.vocDir
import com.ftrono.DJames.application.vocHeads
import com.ftrono.DJames.application.vocSectionIdentifier
import com.ftrono.DJames.dialogs.DialogEditVocabulary
import com.ftrono.DJames.dialogs.GeneralDialog
import com.ftrono.DJames.ui.HeaderWithSign
import com.ftrono.DJames.ui.OptionsItem
import com.ftrono.DJames.ui.OptionsMenu
import com.ftrono.DJames.ui.SplitterSign
import com.ftrono.DJames.ui.StreetBackground
import com.ftrono.DJames.ui.vocColorSelectorLight
import com.ftrono.DJames.ui.vocIconSelector
import com.ftrono.DJames.utilities.Utilities
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader


@Preview
@Preview(heightDp = 360, widthDp = 800)
@Composable
fun VocScreenPreview() {
    VocabularyScreen(editPreview="", preview=true)
}


@Composable
fun VocabularyScreen(
    editPreview: String = "",
    preview: Boolean = false
) {
    val configuration = LocalConfiguration.current
    val isLandscape by remember { mutableStateOf(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) }
    val mContext = LocalContext.current
    //Descriptions:
    val subtitles = mapOf(
        "artist" to "Save your favourite artists & links",
        "playlist" to "Play songs from these playlists",
        "contact" to "People to call or message"
    )
    //Statuses:
    val keyState = rememberSaveable { mutableStateOf("") }
    val currentCatState = rememberSaveable { mutableStateOf(vocHeads[0]) }
    val vocabulary = rememberSaveable {
        mutableStateOf(getVocKeys(mContext, currentCatState.value, preview))
    }
    val deleteVocOn = rememberSaveable { mutableStateOf(false) }
    if (deleteVocOn.value) {
        DialogDeleteVocabulary(mContext, deleteVocOn, vocabulary, keyState, currentCatState.value)
    }
    val editVocOn = rememberSaveable { mutableStateOf(if (editPreview == "") false else true) }
    if (editPreview != "") {
        DialogEditVocabulary(mContext, editVocOn, vocabulary, keyState, editPreview, preview)
    } else if (editVocOn.value) {
        DialogEditVocabulary(mContext, editVocOn, vocabulary, keyState, currentCatState.value, preview)
    }
    var mDisplayMainMenu = rememberSaveable {
        mutableStateOf(false)
    }


    //SCAFFOLD FOR FAB:
    Scaffold(
        floatingActionButton = {
            Column (
                horizontalAlignment = Alignment.End
            ) {

                //1) CAT OPTIONS:
                if (isLandscape) {
                    Box() {
                        FloatingActionButton(
                            containerColor = colorResource(id = R.color.dark_grey_background),
                            onClick = {
                                mDisplayMainMenu.value = !mDisplayMainMenu.value
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Add vocabulary item",
                                tint = colorResource(id = R.color.colorAccentLight)
                            )
                        }
                        CatOptions(
                            mContext = mContext,
                            vocabulary = vocabulary,
                            mDisplayMenu = mDisplayMainMenu,
                            deleteVocOn = deleteVocOn,
                            head = currentCatState.value
                        )
                    }
                }

                //2) ADD NEW ITEM:
                ExtendedFloatingActionButton(
                    containerColor = colorResource(id = R.color.colorAccent),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add vocabulary item",
                            tint = colorResource(id = R.color.light_grey)
                        )
                    },
                    text = {
                        Text(
                            text = "Add",
                            color = colorResource(id = R.color.light_grey),
                            fontSize = 16.sp
                        )
                    },
                    onClick = {
                        keyState.value = ""
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

            //BACKGROUND:
            StreetBackground(
                startDistance = 20
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .background(colorResource(id = R.color.windowBackground))
                ) {
                    //HEADER:
                    Column(
                        modifier = Modifier
                            .padding(top = if (isLandscape) 8.dp else 0.dp, bottom = 8.dp)
                            .fillMaxWidth()
                        //                .verticalScroll(rememberScrollState())
                    ) {
                        if (!isLandscape) {
                            HeaderWithSign(
                                iconRes = painterResource(id = R.drawable.sign_fork),
                                title = "Vocabulary",
                                subtitle = "Help DJames understand you"
                            ){
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
                                        contentDescription = "Add vocabulary item",
                                        tint = colorResource(id = R.color.colorAccentLight)
                                    )
                                    CatOptions(
                                        mContext = mContext,
                                        vocabulary = vocabulary,
                                        mDisplayMenu = mDisplayMainMenu,
                                        deleteVocOn = deleteVocOn,
                                        head = currentCatState.value
                                    )
                                }
                            }
                        }

                        SplitterSign(
                            currentCatState = currentCatState,
                            vocabulary = vocabulary,
                            preview = preview
                        )
                    }
                }

                //CONTENT:
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                ) {

                    //SECTION HEADER:
                    Row(
                        modifier = Modifier
                            .background(colorResource(id = R.color.windowBackground))
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {

                        //N items:
                        Text(
                            modifier = Modifier
                                .padding(start=6.dp),
                            text = if (vocabulary.value.size == 1) "1 item" else "${vocabulary.value.size} items" + "  •  ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            //textAlign = TextAlign.Center,
                            color = vocColorSelectorLight(cat = currentCatState.value)
                        )
                        //Description:
                        Text(
                            modifier = Modifier
                                .padding(end=6.dp),
                            text = "${subtitles[currentCatState.value]!!}",
                            fontSize = 14.sp,
                            //fontWeight = FontWeight.Bold,
                            //textAlign = TextAlign.Center,
                            color = colorResource(id = R.color.mid_grey)
                        )
                    }

                    //CONTENT:
                    VocSectionContent(
                        mContext = mContext,
                        vocabulary = vocabulary,
                        currentCatState = currentCatState,
                        keyState = keyState,
                        editVocOn = editVocOn,
                        deleteVocOn = deleteVocOn,
                        isLandscape = isLandscape,
                        preview = preview,
                        editPreview = editPreview
                    )
                }
            }
        }
    }
}


//DROPDOWN MENU:
@Composable
fun ChipOptions(
    mDisplayMenu: MutableState<Boolean>,
    deleteVocOn: MutableState<Boolean>,
    editVocOn: MutableState<Boolean>,
    keyState: MutableState<String>,
    key: String
) {
    //DROPDOWN MENU:
    OptionsMenu(
        expandedState = mDisplayMenu,
        backgroundColor = colorResource(id = R.color.dark_grey),
        options = {
            //1) Item: EDIT VOC ITEM
            OptionsItem(
                title = "Edit",
                iconVector = Icons.Default.Edit,
                onClick = {
                    keyState.value = key
                    mDisplayMenu.value = false
                    editVocOn.value = true
                }
            )
            //2) Item: DELETE VOC ITEM
            OptionsItem(
                title = "Delete",
                iconVector = Icons.Default.Delete,
                onClick = {
                    keyState.value = key
                    mDisplayMenu.value = false
                    deleteVocOn.value = true
                }
            )
        }
    )
}


@Composable
fun VocSectionContent(
    mContext: Context,
    vocabulary: MutableState<List<String>>,
    currentCatState: MutableState<String>,
    keyState: MutableState<String>,
    editVocOn: MutableState<Boolean>,
    deleteVocOn: MutableState<Boolean>,
    isLandscape: Boolean,
    preview: Boolean = false,
    editPreview: String = ""
) {

    //CONTENT:
    if (vocabulary.value.isEmpty()) {
        //VOCABULARY EMPTY:
        Text(
            text = "${currentCatState.value.replaceFirstChar { it.uppercase() }}s vocabulary\nis empty",
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            color = colorResource(id = R.color.mid_grey),
            modifier = Modifier
                .fillMaxSize()
                .wrapContentHeight()
                .wrapContentWidth()
        )
    } else {
        //VOCABULARY LIST:
        LazyVerticalGrid(
            modifier = Modifier
                .padding(start = 32.dp, end = 24.dp, bottom = 12.dp)
                .fillMaxSize(),
            columns = GridCells.Fixed(if (isLandscape) 3 else 2),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            //ITEMS:
            vocabulary.value.forEach { key ->
                if (key.contains(vocSectionIdentifier)) {
                    //HEADER:
                    item(
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        VocLetter(
                            letter = key.replace(vocSectionIdentifier, "")
                        )
                    }
                } else {
                    //ITEM:
                    item {
                        VocItem(
                            mContext = mContext,
                            currentCatState = currentCatState,
                            keyState = keyState,
                            head = currentCatState.value,
                            key = key,
                            editVocOn = editVocOn,
                            deleteVocOn = deleteVocOn,
                            preview = preview,
                            editPreview = editPreview
                        )
                        if (key == vocabulary.value.last()) Spacer(modifier = Modifier.padding(60.dp))
                    }
                }
            }
        }
    }
}


@Composable
fun VocLetter(
    letter: String
) {
    //TEXT LABEL:
    Column (
        modifier = Modifier
            .padding(top=4.dp, bottom=4.dp)
    ) {
        //Item key:
        Text(
            modifier = Modifier
                .padding(start = 2.dp, bottom = 2.dp)
                .wrapContentWidth()
                .wrapContentHeight(),
            color = colorResource(id = R.color.light_grey),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            text = letter.uppercase()
        )
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth(),
            color = colorResource(id = R.color.faded_grey)
        )
    }
}


@Composable
fun VocItem(
    mContext: Context,
    currentCatState: MutableState<String>,
    keyState: MutableState<String>,
    head: String,
    key: String,
    editVocOn: MutableState<Boolean>,
    deleteVocOn: MutableState<Boolean>,
    preview: Boolean = false,
    editPreview: String = ""
) {
    val utils = Utilities()

    //VOC CHIPS:
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        var mDisplayMenu = rememberSaveable {
            mutableStateOf(false)
        }
        Card(
            modifier = Modifier
                .wrapContentWidth()
                .height(56.dp)
                .clickable {
                    mDisplayMenu.value = !mDisplayMenu.value
                },
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, colorResource(id = R.color.faded_grey)),
            colors = CardDefaults.cardColors(
                containerColor = if (mDisplayMenu.value) {
                    colorResource(id = R.color.dark_grey)
                } else {
                    colorResource(id = R.color.dark_grey_background)
                }
            )
        ) {
            Row (
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ){
                //CHIP ICON:
                Icon(
                    modifier = Modifier
                        .padding(start = 4.dp, end = 4.dp)
                        .size(24.dp),
                    painter = vocIconSelector(cat = head),
                    contentDescription = head,
                    tint = vocColorSelectorLight(cat = currentCatState.value)
                )
                //TEXT LABEL:
                Column(
                    modifier = Modifier
                        .padding(start = 4.dp, end = 6.dp)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    //Item key:
                    Text(
                        modifier = Modifier
                            .wrapContentWidth()
                            .wrapContentHeight(),
                        color = colorResource(id = R.color.light_grey),
                        fontSize = 14.sp,
                        lineHeight = 16.sp,
                        maxLines = if (head == "contact") 1 else 2,
                        //fontStyle = if (key == "") null else FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        text = if (key == "") "Add $head" else utils.trimString(key, 24)
                    )
                    //Item detail:
                    if (head == "contact" && key != "") {
                        Text(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .wrapContentWidth()
                                .wrapContentHeight(),
                            color = colorResource(id = R.color.mid_grey),
                            fontSize = 12.sp,
                            lineHeight = 14.sp,
                            maxLines = 1,
                            fontStyle = FontStyle.Italic,
                            text = if (preview) {
                                "3331122333"
                            } else {
                                updateVocabulary(
                                    mContext,
                                    head
                                ).get(key).asJsonObject.get("phone").asString
                            }
                        )
                    }
                }
            }
        }
        ChipOptions(mDisplayMenu, deleteVocOn, editVocOn, keyState, key)
    }
}


//DROPDOWN MENU:
@Composable
fun CatOptions(
    mContext: Context,
    vocabulary: MutableState<List<String>>,
    mDisplayMenu: MutableState<Boolean>,
    deleteVocOn: MutableState<Boolean>,
    head: String
) {
    //DROPDOWN MENU:
    OptionsMenu(
        expandedState = mDisplayMenu,
        backgroundColor = colorResource(id = R.color.dark_grey),
        options = {
            //1) Item: REFRESH VOC CATEGORY
            OptionsItem(
                title = "Refresh",
                iconVector = Icons.Default.Refresh,
                onClick = {
                    filter.postValue(head)
                    mDisplayMenu.value = false
                    vocabulary.value = getVocKeys(mContext, head)
                    Toast.makeText(mContext, "${head.replaceFirstChar { it.uppercase() }}s vocabulary updated!", Toast.LENGTH_SHORT).show()
                }
            )
            //2) Item: SHARE VOC CATEGORY
            OptionsItem(
                title = "Share",
                iconVector = Icons.Default.Share,
                onClick = {
                    sendVoc(mContext, head)
                    mDisplayMenu.value = false
                }
            )
            //3) Item: DELETE VOC CATEGORY
            OptionsItem(
                title = "Delete all",
                iconVector = Icons.Default.Delete,
                onClick = {
                    filter.postValue(head)
                    mDisplayMenu.value = false
                    deleteVocOn.value = true
                }
            )
        }
    )
}


@Composable
fun DialogDeleteVocabulary(
    mContext: Context,
    dialogOnState: MutableState<Boolean>,
    vocabulary: MutableState<List<String>>,
    keyState: MutableState<String>,
    filter: String
) {
    val utils = Utilities()
    val key = keyState.value
    //DELETE DIALOG:
    GeneralDialog(
        dialogOnState = dialogOnState,
        backgroundColor = colorResource(id = R.color.colorPrimaryDark),
        title = if (key != "") "Delete $filter" else "Delete ${filter}s",
        content = {
            Text(
                text = if (key != "") {
                    "Do you want to delete this $filter?\n\n$key"
                } else {
                    "Do you want to delete all ${filter}s in vocabulary?"
                },
                color = colorResource(id = R.color.light_grey),
                fontSize = 14.sp
            )
        },
        dismissText = "No",
        onDismiss = {
            dialogOnState.value = false
            keyState.value = ""
        },
        confirmText = "Yes",
        onConfirm = {
            if (key != "") {
                //Delete current:
                var ret = utils.editVocFile(prevText=key)
                if (ret == 0) {
                    vocabulary.value = getVocKeys(mContext, filter)   //Refresh list
                    Toast.makeText(mContext, "Deleted!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(mContext, "ERROR: Vocabulary not updated!", Toast.LENGTH_LONG).show()
                }
            } else {
                //Delete all:
                File(vocDir, "voc_${filter}s.json").delete()
                Log.d("VocabularyScreen", "Deleted ${filter}s vocabulary.")
                vocabulary.value = getVocKeys(mContext, filter)   //Refresh list
                Toast.makeText(mContext, "${filter.replaceFirstChar { it.uppercase() }} vocabulary deleted!", Toast.LENGTH_LONG).show()
            }
            dialogOnState.value = false
            keyState.value = ""
        }
    )
}


//REFRESH:
fun updateVocabulary(mContext: Context, filter: String, preview: Boolean = false): JsonObject {
    if (preview) {
        //Mock data:
        val reader = BufferedReader(InputStreamReader(mContext.resources.openRawResource(R.raw.vocabulary_sample)))
        //val vocItems = JsonObject()   //EMPTY TEST
        val vocItems = JsonParser.parseReader(reader).asJsonObject.get(filter).asJsonObject
        return vocItems
    } else {
        //Real data:
        val utils = Utilities()
        val vocItems = utils.getVocabulary(filter=filter)
        //Log.d("Vocabulary", vocItems.toString())
        return vocItems
    }
}


//VOC ACTIONS:
//Send:
fun sendVoc(mContext: Context, filter: String) {
    //Send the current file:
    val file = File(vocDir, "voc_${filter}s.json")
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


//Voc keyset:
fun getVocKeys(mContext: Context, head: String, preview: Boolean = false): List<String> {
    val utils = Utilities()
    val vocKeys = updateVocabulary(mContext, head, preview).keySet().toList()
    val vocKeysWithHeaders = mutableListOf<String>()
    var letter = ""
    for (key in vocKeys) {
        val cur = key.first().toString()
        if (cur != letter) {
            if (utils.isLetters(cur)) {
                letter = cur
            } else {
                letter = "#"
            }
            vocKeysWithHeaders.add("$vocSectionIdentifier$letter")
        }
        vocKeysWithHeaders.add(key)
    }
    return vocKeysWithHeaders
}
