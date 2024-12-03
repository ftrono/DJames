package com.ftrono.DJames.screen

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
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
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import com.ftrono.DJames.R
import com.ftrono.DJames.application.filter
import com.ftrono.DJames.application.vocDir
import com.ftrono.DJames.application.vocHeads
import com.ftrono.DJames.dialogs.DialogEditVocabulary
import com.ftrono.DJames.ui.HeaderWithSign
import com.ftrono.DJames.ui.OptionsItem
import com.ftrono.DJames.ui.OptionsMenu
import com.ftrono.DJames.ui.StreetBackground
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
fun VocabularyScreen(editPreview: String = "", preview: Boolean = false) {
    val mContext = LocalContext.current
    val utils = Utilities()
    //Descriptions:
    val subtitles = mapOf(
        "artist" to "Favourites or hard-to-spell",
        "playlist" to "Play songs from this list",
        "contact" to "For calls & messages"
    )
    //Statuses:
    var keyState = rememberSaveable { mutableStateOf("") }
    var catState = rememberSaveable { mutableStateOf(vocHeads[0]) }
    val expandedStates = remember {
        mutableStateMapOf(*vocHeads.map { it to false }.toTypedArray())
    }

    //BACKGROUND:
    StreetBackground(
        startDistance = 48
    ) {
        //HEADER:
        HeaderWithSign(
            iconRes = painterResource(id = R.drawable.sign_fork),
            title = "My Vocabulary",
            subtitle = "Help DJames understand you")

        //CONTENT:
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            //Check expanded states:
            utils.updateStatesMap(expandedStates, target=catState.value)
            //SECTIONS:
            for (vocHead in vocHeads) {
                ExpandableVocSection(
                    mContext = mContext,
                    modifier = Modifier
                        .fillMaxWidth(),
                    keyState = keyState,
                    catState = catState,
                    expandedStates = expandedStates,
                    head = vocHead,
                    title = "My ${vocHead}s",
                    subtitle = subtitles[vocHead]!!,
                    preview = preview,
                    editPreview = editPreview
                )
            }
        }
    }
}


@Composable
fun VocIcon(
    filter: String,
    size: Int,
    padding: Int,
    bigger: Boolean = false
) {
    Icon(
        modifier = Modifier
            .padding(start = padding.dp)
            .size(size.dp),
        painter = when (filter) {
            "artist" -> {
                painterResource(id = R.drawable.sign_note)
            }
            "playlist" -> {
                painterResource(id = R.drawable.sign_headphones)
            }
            else -> {
                painterResource(id = R.drawable.sign_phone)
            }
        },
        contentDescription = filter,
        tint = if (bigger) colorResource(id = R.color.light_grey) else colorResource(id = R.color.colorAccentLight)
    )
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


@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterialApi::class)
@Composable
fun ExpandableVocSection(
    mContext: Context,
    modifier: Modifier = Modifier,
    keyState: MutableState<String>,
    catState: MutableState<String>,
    expandedStates: SnapshotStateMap<String, Boolean>,
    head: String,
    title: String,
    subtitle: String,
    preview: Boolean = false,
    editPreview: String = ""
) {
    val utils = Utilities()

    val vocabulary = rememberSaveable {
        mutableStateOf(getVocKeys(mContext, head, preview))
    }

    val deleteVocOn = rememberSaveable { mutableStateOf(false) }
    if (deleteVocOn.value) {
        DialogDeleteVocabulary(mContext, deleteVocOn, vocabulary, keyState, head)
    }

    val editVocOn = rememberSaveable { mutableStateOf(if (editPreview == "") false else true) }
    if (editPreview != "") {
        DialogEditVocabulary(mContext, editVocOn, vocabulary, keyState, editPreview, preview)
    } else if (editVocOn.value) {
        DialogEditVocabulary(mContext, editVocOn, vocabulary, keyState, head, preview)
    }

    utils.updateStatesMap(expandedStates, target=catState.value)

    Column(
        modifier = modifier
            .clickable {
                //Update global catState:
                if (catState.value == head) {
                    catState.value = ""
                } else {
                    catState.value = head
                }
                utils.updateStatesMap(expandedStates, target=catState.value)
            }
            .fillMaxWidth()
    ) {
        //SECTION:
        ExpandableVocSectionTitle(
            mContext = mContext,
            vocabulary = vocabulary,
            deleteVocOn = deleteVocOn,
            isExpanded = expandedStates[head]!!,
            head = head,
            title = title,
            subtitle = subtitle)

        AnimatedVisibility(
            modifier = Modifier
                .fillMaxWidth(),

            visible = expandedStates[head]!!
        ) {
            LazyHorizontalGrid(
                modifier = Modifier
                    .padding(start = 52.dp, end = 24.dp, bottom = 12.dp)
                    .fillMaxWidth()
                    .height(180.dp),
                rows = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(vocabulary.value) { index, key ->
                    //VOC CHIPS:
                    Box(
                        modifier = Modifier
                            .width(150.dp)
                            .height(50.dp)
                    ) {
                        var mDisplayMenu = rememberSaveable {
                            mutableStateOf(false)
                        }

                        AssistChip(
                            modifier = Modifier
                                .padding(4.dp)
                                .fillMaxSize(),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, colorResource(id = R.color.mid_grey)),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (key == "") {
                                    colorResource(id = R.color.colorAccent)
                                } else if (mDisplayMenu.value) {
                                    colorResource(id = R.color.dark_grey)
                                } else {
                                    colorResource(id = R.color.dark_grey_background)
                                },
                                labelColor = colorResource(id = R.color.light_grey),
                                leadingIconContentColor = colorResource(id = R.color.mid_grey)
                            ),
                            leadingIcon = {
                                if (key == "") {
                                    //ADD NEW:
                                    Icon(
                                        modifier = Modifier
                                            .padding(start = 2.dp)
                                            .size(20.dp),
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "ADD NEW",
                                        tint = colorResource(id = R.color.light_grey)
                                    )
                                } else {
                                    //CHIP ICON:
                                    VocIcon(
                                        padding = 4,
                                        size = 20,
                                        filter = head
                                    )
                                }
                            },
                            label = {
                                Column {
                                    //Item key:
                                    Text(
                                        modifier = Modifier
                                            .wrapContentWidth()
                                            .wrapContentHeight(),
                                        color = colorResource(id = R.color.light_grey),
                                        fontSize = 14.sp,
                                        lineHeight = 16.sp,
                                        maxLines = if (head == "contact") 1 else 2,
                                        fontStyle = if (key == "") null else FontStyle.Italic,
                                        fontWeight = if (key == "") FontWeight.Bold else null,
                                        text = if (key == "") "Add $head" else utils.trimString(key, 16)
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
                                                updateVocabulary(mContext, head).get(key).asJsonObject.get("phone").asString
                                            }
                                        )
                                    }
                                }
                            },
                            onClick = {
                                filter.postValue(head)
                                if (key == "") {
                                    editVocOn.value = true
                                } else {
                                    mDisplayMenu.value = !mDisplayMenu.value
                                }
                            }
                        )
                        ChipOptions(mDisplayMenu, deleteVocOn, editVocOn, keyState, key)
                    }
                }
            }
        }
    }
}


@Composable
fun ExpandableVocSectionTitle(
    mContext: Context,
    vocabulary: MutableState<List<String>>,
    deleteVocOn: MutableState<Boolean>,
    modifier: Modifier = Modifier,
    isExpanded: Boolean,
    head: String,
    title: String,
    subtitle: String
) {
    var mDisplayMenu = rememberSaveable {
        mutableStateOf(false)
    }
    val icon = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown
    val vocSize = vocabulary.value.size - 1
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
                .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            //ROUNDED SIGN:
            Box (
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (head == "artist") {
                            colorResource(id = R.color.blueSign)
                        } else if (head == "playlist") {
                            colorResource(id = R.color.yellowSign)
                        } else {
                            colorResource(id = R.color.colorPrimary)
                        }
                    )
                    .border(2.dp, colorResource(id = R.color.light_grey), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                //CAT ICON:
                VocIcon(
                    padding = 0,
                    size = 24,
                    filter = head,
                    bigger = true
                )
            }

            //TITLE:
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
            ) {
                //Title:
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.light_grey)
                )
                //Subtitle:
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.mid_grey)
                )
                //Count:
                Text(
                    text = if (vocSize == 1) "1 item" else "$vocSize items",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.colorAccentLight)
                )
            }
            //"MORE OPTIONS" BUTTON:
            Box() {
                Icon(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clickable { mDisplayMenu.value = !mDisplayMenu.value },
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "",
                    tint = colorResource(id = R.color.light_grey)
                )
                CatOptions(
                    mContext = mContext,
                    vocabulary = vocabulary,
                    mDisplayMenu = mDisplayMenu,
                    deleteVocOn = deleteVocOn,
                    head = head
                )
            }

            //EXPAND/COLLAPSE:
            Icon(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(32.dp),
                imageVector = icon,
                tint = colorResource(id = R.color.light_grey),
                contentDescription = "Expand / collapse"
            )
        }
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
    if (dialogOnState.value) {
        AlertDialog(
            onDismissRequest = {
                //cancelable -> true:
                dialogOnState.value = false
                keyState.value = ""
            },
            containerColor = colorResource(id = R.color.colorPrimaryOld),
            title = {
                Text(
                    text = if (key != "") "Delete $filter" else "Delete ${filter}s",
                    color = colorResource(id = R.color.light_grey),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                ) },
            text = {
                Text(
                    text = if (key != "") {
                        "Do you want to delete this $filter?\n\n$key"
                    } else {
                        "Do you want to delete all ${filter}s in vocabulary?"
                    },
                    color = colorResource(id = R.color.light_grey)
                ) },
            dismissButton = {
                Text(
                    modifier = Modifier
                        .padding(end = 20.dp)
                        .clickable {
                            dialogOnState.value = false
                            keyState.value = ""
                        },
                    text = "No",
                    fontSize = 16.sp,
                    color = colorResource(id = R.color.light_grey)
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
                        },
                    text = "Yes",
                    fontSize = 16.sp,
                    color = colorResource(id = R.color.light_grey)
                )
            }
        )
    }
}


//REFRESH:
fun updateVocabulary(mContext: Context, filter: String, preview: Boolean = false): JsonObject {
    if (preview) {
        //Mock data:
        val reader = BufferedReader(InputStreamReader(mContext.resources.openRawResource(R.raw.vocabulary_sample)))
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
    val vocKeys = listOf("") + updateVocabulary(mContext, head, preview).keySet().toList()
    return vocKeys
}
