package com.ftrono.DJames.screen

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
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
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
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
import com.ftrono.DJames.application.curLibrarySize
import com.ftrono.DJames.application.spotifyLoggedIn
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.application.vocHeads
import com.ftrono.DJames.application.vocSectionIdentifier
import com.ftrono.DJames.dialogs.EditVocArtist
import com.ftrono.DJames.dialogs.EditVocContact
import com.ftrono.DJames.dialogs.EditVocPlaylist
import com.ftrono.DJames.dialogs.GeneralDialog
import com.ftrono.DJames.ui.HeaderWithSign
import com.ftrono.DJames.ui.OptionsItem
import com.ftrono.DJames.ui.OptionsMenu
import com.ftrono.DJames.ui.SplitterCat
import com.ftrono.DJames.ui.StreetBackground
import com.ftrono.DJames.ui.vocColorSelectorLight
import com.ftrono.DJames.ui.vocIconSelector
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
        "artist" to "My artists to play",
        "playlist" to "Play songs from...",
        "contact" to "Call or message..."
    )
    //Statuses:
    val keyState = rememberSaveable { mutableStateOf("") }
    val currentCatState = rememberSaveable { mutableStateOf(vocHeads[0]) }
    val vocKeys = rememberSaveable {
        mutableStateOf(getVocKeys(mContext, currentCatState.value, preview))
    }
    val curLibrarySizeState by curLibrarySize.observeAsState()
    val deleteVocOn = rememberSaveable { mutableStateOf(false) }
    if (deleteVocOn.value) {
        DialogDeleteVocabulary(mContext, deleteVocOn, vocKeys, keyState, currentCatState.value)
    }

    val editVocOn = rememberSaveable { mutableStateOf(if (editPreview == "") false else true) }

    var editKey = ""
    if (editPreview != "") {
        editKey = editPreview
    } else if (editVocOn.value) {
        editKey = currentCatState.value
    }

    if (editVocOn.value) {
        when (editKey) {
            "artist" -> EditVocArtist(mContext, editVocOn, vocKeys, keyState, filter=editKey, preview)
            "playlist" -> EditVocPlaylist(mContext, editVocOn, vocKeys, keyState, filter=editKey, preview)
            "contact" -> EditVocContact(mContext, editVocOn, vocKeys, keyState, filter=editKey, preview)
        }
    }

    var mDisplayMainMenu = rememberSaveable {
        mutableStateOf(false)
    }


    //SCAFFOLD FOR FAB:
    Scaffold(
        floatingActionButton = {
            //FAB -> ADD NEW ITEM:
            ExtendedFloatingActionButton(
                containerColor = colorResource(id = R.color.colorAccent),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add library item",
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
                                title = "Library",
                                subtitle = subtitles[currentCatState.value]!!,
                                num = curLibrarySizeState
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
                                        contentDescription = "Add library item",
                                        tint = colorResource(id = R.color.colorAccentLight)
                                    )
                                    CatOptions(
                                        mContext = mContext,
                                        vocKeys = vocKeys,
                                        mDisplayMenu = mDisplayMainMenu,
                                        deleteVocOn = deleteVocOn,
                                        head = currentCatState.value
                                    )
                                }
                            }
                        }

                        //CAT SELECTORS:
                        Row (
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Row (
                                modifier = Modifier
                                    .weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                //BUTTONS:
                                for (head in vocHeads) {
                                    SplitterCat(
                                        currentCatState = currentCatState,
                                        vocabulary = vocKeys,
                                        head = head,
                                        title = if (!isLandscape && head == "artist") "Artists  " else "${head.replaceFirstChar { it.uppercase() }}s",
                                        selected = currentCatState.value == head,
                                        num = if (isLandscape && currentCatState.value == head) curLibrarySizeState else null,
                                        preview = preview
                                    )
                                    //DIVIDERS:
                                    if (head != vocHeads.last()) {
                                        VerticalDivider(
                                            modifier = Modifier
                                                .padding(start = 4.dp, end = 4.dp)
                                                .height(30.dp)
                                                .wrapContentWidth(),
                                            thickness = 2.dp,
                                            color = colorResource(id = R.color.faded_grey)
                                        )
                                    }
                                }
                            }
                            //CAT OPTIONS:
                            if (isLandscape) {
                                Box() {
                                    Icon(
                                        modifier = Modifier
                                            .padding(end = 18.dp)
                                            .size(30.dp)
                                            .clickable {
                                                mDisplayMainMenu.value = !mDisplayMainMenu.value
                                            },
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Library options",
                                        tint = colorResource(id = R.color.colorAccentLight)
                                    )

                                    CatOptions(
                                        mContext = mContext,
                                        vocKeys = vocKeys,
                                        mDisplayMenu = mDisplayMainMenu,
                                        deleteVocOn = deleteVocOn,
                                        head = currentCatState.value
                                    )
                                }
                            }
                        }

                    }
                }

                //CONTENT:
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                ) {

                    //CONTENT:
                    VocSectionContent(
                        mContext = mContext,
                        vocKeys = vocKeys,
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
    vocKeys: MutableState<List<String>>,
    currentCatState: MutableState<String>,
    keyState: MutableState<String>,
    editVocOn: MutableState<Boolean>,
    deleteVocOn: MutableState<Boolean>,
    isLandscape: Boolean,
    preview: Boolean = false,
    editPreview: String = ""
) {

    //CONTENT:
    if (vocKeys.value.isEmpty()) {
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
            vocKeys.value.forEach { key ->
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
                        if (key == vocKeys.value.last()) Spacer(modifier = Modifier.padding(60.dp))
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
    //TODO: extract useful data:
    val curItem = getVocItem(mContext, currentCatState.value, key, preview)

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
                        text = utils.trimString(key, 24)
                        //fontWeight = FontWeight.Bold,
                        //fontStyle = FontStyle.Italic,
                    )
                    //Item detail:
                    if (head == "contact") {
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
                                try {
                                    curItem.get("main").asJsonObject.get("phone").asString
                                } catch (e: Exception) {
                                    ""
                                }
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
    vocKeys: MutableState<List<String>>,
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
                    mDisplayMenu.value = false
                    vocKeys.value = getVocKeys(mContext, head)
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
    vocKeys: MutableState<List<String>>,
    keyState: MutableState<String>,
    filter: String
) {
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
                utils.deleteLibraryItem(mContext, filter, key)
            } else {
                //Delete all:
                utils.deleteLibrary(mContext, filter)
            }
            vocKeys.value = getVocKeys(mContext, filter)   //Refresh list
            dialogOnState.value = false
            keyState.value = ""
        }
    )
}


//VOC ACTIONS:
//Send:
fun sendVoc(mContext: Context, filter: String) {
    //Build cached file:
    val fileName = utils.buildLibraryToSend(mContext, filter)
    if (fileName != "") {
        //Get cached file:
        val file = File(mContext.cacheDir, fileName)
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
    } else (
        Toast.makeText(mContext, "ERROR: cannot prepare consolidated Library file for ${filter}s to send!", Toast.LENGTH_LONG).show()
    )
}


//Voc keyset:
fun getVocKeys(mContext: Context, filter: String, preview: Boolean = false, addHeaders: Boolean = true): List<String> {
    var vocKeys = listOf<String>()

    //1) Load library:
    if (preview) {
        var reader: BufferedReader? = null
        //Mock data:
        if (filter == "artist") {
            reader = BufferedReader(InputStreamReader(mContext.resources.openRawResource(R.raw.library_artists)))
        } else if (filter == "playlist") {
            reader = BufferedReader(InputStreamReader(mContext.resources.openRawResource(R.raw.library_playlists)))
        } else {
            reader = BufferedReader(InputStreamReader(mContext.resources.openRawResource(R.raw.library_contacts)))
        }
        //val vocItems = listOf<String>()   //EMPTY TEST
        vocKeys = JsonParser.parseReader(reader).asJsonObject.keySet().toList()
    } else {
        //Real data:
        vocKeys = utils.getLibraryKeys(filter=filter)
        //Log.d("Library", vocKeys.toString())
    }
    //Update Library size:
    curLibrarySize.postValue(vocKeys.size)

    //2) Add letters headers:
    if (addHeaders) {
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
    } else {
        return vocKeys
    }
}


//GET:
fun getVocItem(mContext: Context, filter: String, key: String, preview: Boolean = false): JsonObject {
    if (preview) {
        var reader: BufferedReader? = null
        //Mock data:
        if (filter == "artist") {
            reader = BufferedReader(InputStreamReader(mContext.resources.openRawResource(R.raw.library_artists)))
        } else if (filter == "playlist") {
            reader = BufferedReader(InputStreamReader(mContext.resources.openRawResource(R.raw.library_playlists)))
        } else {
            reader = BufferedReader(InputStreamReader(mContext.resources.openRawResource(R.raw.library_contacts)))
        }
        //Mock data:
        val vocItem = JsonParser.parseReader(reader).asJsonObject.get(key).asJsonObject
        return vocItem
    } else {
        //Real data:
        val vocItem = utils.getLibraryItem(filter, key)
        //Log.d("Items", vocItem.toString())
        return vocItem
    }
}