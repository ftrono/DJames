package com.ftrono.DJames.application.screens

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import com.ftrono.DJames.R
import com.ftrono.DJames.application.curLibrarySize
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.application.vocHeads
import com.ftrono.DJames.application.vocSectionIdentifier
import com.ftrono.DJames.be.database.ItemInfoView
import com.ftrono.DJames.application.dialogs.EditVocArtist
import com.ftrono.DJames.application.dialogs.EditVocContact
import com.ftrono.DJames.application.dialogs.EditVocPlaylist
import com.ftrono.DJames.ui.dialogs.GeneralDialog
import com.ftrono.DJames.ui.components.HeaderWithSign
import com.ftrono.DJames.ui.components.OptionsItem
import com.ftrono.DJames.ui.components.OptionsMenu
import com.ftrono.DJames.ui.components.RoundedLetter
import com.ftrono.DJames.ui.components.SplitterCat
import com.ftrono.DJames.ui.components.StreetBackground
import com.ftrono.DJames.ui.components.VocItemCard
import com.ftrono.DJames.ui.selectors.vocColorSelector
import com.ftrono.DJames.ui.selectors.vocIconSelector
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString


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
    val nameState = rememberSaveable { mutableStateOf("") }
    val currentCatState = rememberSaveable { mutableStateOf(vocHeads[0]) }
    val libraryMap = rememberSaveable {
        mutableStateOf(libUtils.refreshLibrary(currentCatState.value, preview))
    }
    val curLibrarySizeState by curLibrarySize.observeAsState()
    val deleteVocOn = rememberSaveable { mutableStateOf(false) }
    if (deleteVocOn.value) {
        DialogDeleteVocabulary(mContext, deleteVocOn, libraryMap, keyState, nameState, filter=currentCatState.value)
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
            "artist" -> EditVocArtist(mContext, editVocOn, libraryMap, keyState, filter=editKey, preview)
            "playlist" -> EditVocPlaylist(mContext, editVocOn, libraryMap, keyState, filter=editKey, preview)
            "contact" -> EditVocContact(mContext, editVocOn, libraryMap, keyState, filter=editKey, preview)
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
                                num = curLibrarySizeState,
                                // signColor = vocColorSelector(cat = currentCatState.value)
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
                                        libraryMap = libraryMap,
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
                                        libraryMap = libraryMap,
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
                                        libraryMap = libraryMap,
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
                        libraryMap = libraryMap,
                        currentCatState = currentCatState,
                        keyState = keyState,
                        nameState = nameState,
                        editVocOn = editVocOn,
                        deleteVocOn = deleteVocOn,
                        isLandscape = isLandscape,
                        preview = preview
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
    nameState: MutableState<String>,
    key: String,
    name: String
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
                    nameState.value = name
                    mDisplayMenu.value = false
                    deleteVocOn.value = true
                }
            )
        }
    )
}


@Composable
fun VocSectionContent(
    libraryMap: MutableState<Map<String, String>>,
    currentCatState: MutableState<String>,
    keyState: MutableState<String>,
    nameState: MutableState<String>,
    editVocOn: MutableState<Boolean>,
    deleteVocOn: MutableState<Boolean>,
    isLandscape: Boolean,
    preview: Boolean = false
) {

    //CONTENT:
    if (libraryMap.value.isEmpty()) {
        //VOCABULARY EMPTY:
        Text(
            text = "${currentCatState.value.replaceFirstChar { it.uppercase() }}s library\nis empty",
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
            libraryMap.value.forEach { map ->
                if (map.key.contains(vocSectionIdentifier)) {
                    //HEADER:
                    item(
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        VocLetter(
                            letter = map.key.replace(vocSectionIdentifier, "")
                        )
                    }
                } else {
                    //ITEM:
                    item {
                        VocItem(
                            currentCatState = currentCatState,
                            keyState = keyState,
                            nameState = nameState,
                            key = map.key,
                            itemJson = map.value,
                            editVocOn = editVocOn,
                            deleteVocOn = deleteVocOn
                        )
                        if (map.key == libraryMap.value.keys.last()) Spacer(modifier = Modifier.padding(60.dp))
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
            .padding(top=4.dp)
    ) {
        //Item key:
        RoundedLetter(
            text = letter.uppercase(),
            signSize = 32.dp,
            fontSize = 16.sp,
            backgroundColor = colorResource(id = R.color.windowBackground),
            borderColor = colorResource(id = R.color.dark_grey),
            fontColor = colorResource(id = R.color.light_grey)
        )
    }
}


@Composable
fun VocItem(
    currentCatState: MutableState<String>,
    keyState: MutableState<String>,
    nameState: MutableState<String>,
    key: String,
    itemJson: String,
    editVocOn: MutableState<Boolean>,
    deleteVocOn: MutableState<Boolean>
) {
    val mDisplayMenu = rememberSaveable { mutableStateOf(false) }
    val itemInfo = Json.decodeFromString<ItemInfoView>(itemJson)
    //Init aliases:
    val itemName = itemInfo.name
    val itemAliases = itemInfo.aliases.toMutableList()
    itemAliases.removeAt(0)

    //VOC CHIPS:
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {

        //Card:
        VocItemCard(
            modifier = Modifier
                .wrapContentWidth()
                .height(60.dp),
            cardColors = CardDefaults.cardColors(
                containerColor = if (mDisplayMenu.value) {
                    colorResource(id = R.color.dark_grey)
                } else {
                    colorResource(id = R.color.dark_grey_background)
                }
            ),
            signBackgroundColor = vocColorSelector(cat = currentCatState.value),
            signBorderColor = colorResource(id = R.color.midfaded_grey),
            signIconColor = colorResource(id = R.color.light_grey),
            signIconPainter = vocIconSelector(cat = currentCatState.value),
            circle = currentCatState.value != "playlist",
            title = utils.trimString(itemName, 24),
            subtitle = if (itemAliases.isNotEmpty()) {
                    utils.trimString("\"" + itemAliases.joinToString("\", \"") + "\"", 16)
                } else if (itemInfo.phone != "") {
                    utils.trimString(itemInfo.phone, 16)
                } else "",
            onClick = {
                mDisplayMenu.value = !mDisplayMenu.value
            }
        )
        //Options menu:
        ChipOptions(mDisplayMenu, deleteVocOn, editVocOn, keyState, nameState, key=key, name=itemName)
    }
}


//DROPDOWN MENU:
@Composable
fun CatOptions(
    mContext: Context,
    libraryMap: MutableState<Map<String, String>>,
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
                    libraryMap.value = libUtils.refreshLibrary(head)
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
    libraryMap: MutableState<Map<String, String>>,
    keyState: MutableState<String>,
    nameState: MutableState<String>,
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
                    "Do you want to delete this $filter?\n\n${nameState.value}"
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
            nameState.value = ""
        },
        confirmText = "Yes",
        onConfirm = {
            if (key != "") {
                libUtils.deleteLibraryItem(mContext, filter, key)
            } else {
                //Delete all:
                libUtils.deleteLibrary(mContext, filter)
            }
            libraryMap.value = libUtils.refreshLibrary(filter)   //Refresh list
            dialogOnState.value = false
            keyState.value = ""
            nameState.value = ""
        }
    )
}


//VOC ACTIONS:
//Send:
fun sendVoc(mContext: Context, filter: String) {
    //Build cached file:
    val fileName = libUtils.buildLibraryToSend(mContext, filter)
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

