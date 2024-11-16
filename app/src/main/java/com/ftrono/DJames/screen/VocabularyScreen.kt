package com.ftrono.DJames.screen

import android.content.Context
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
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.ftrono.DJames.R
import com.ftrono.DJames.application.filter
import com.ftrono.DJames.application.vocDir
import com.ftrono.DJames.application.vocEditPreview
import com.ftrono.DJames.ui.HeaderWithSign
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
    VocabularyScreen(editPreview=false, preview=true)
}


@Composable
fun VocabularyScreen(editPreview: Boolean = false, preview: Boolean = false) {
    val mContext = LocalContext.current

    //Statuses:
    var keyState = rememberSaveable { mutableStateOf("") }
    var artistsExpanded = rememberSaveable { mutableStateOf(true) }
    var playlistsExpanded = rememberSaveable { mutableStateOf(false) }
    var contactsExpanded = rememberSaveable { mutableStateOf(false) }

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
            //SECTIONS:
            //1) ARTISTS:
            ExpandableVocSection(
                mContext = mContext,
                modifier = Modifier
                    .fillMaxWidth(),
                keyState = keyState,
                sectionIsExpanded = artistsExpanded,
                otherIsExpanded1 = playlistsExpanded,
                otherIsExpanded2 = contactsExpanded,
                head = "artist",
                title = "My Artists",
                subtitle = "Favourites or hard-to-spell",
                preview = preview,
                editPreview = editPreview
            )

            //2) PLAYLISTS:
            ExpandableVocSection(
                mContext = mContext,
                modifier = Modifier
                    .fillMaxWidth(),
                keyState = keyState,
                sectionIsExpanded = playlistsExpanded,
                otherIsExpanded1 = artistsExpanded,
                otherIsExpanded2 = contactsExpanded,
                head = "playlist",
                title = "My Playlists",
                subtitle = "Play songs from this list",
                preview = preview,
                editPreview = editPreview
            )

            //3) CONTACTS:
            ExpandableVocSection(
                mContext = mContext,
                modifier = Modifier
                    .fillMaxWidth(),
                keyState = keyState,
                sectionIsExpanded = contactsExpanded,
                otherIsExpanded1 = playlistsExpanded,
                otherIsExpanded2 = artistsExpanded,
                head = "contact",
                title = "My Contacts",
                subtitle = "For calls & messages",
                preview = preview,
                editPreview = editPreview
            )
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
fun ChipOptions(mDisplayMenu: MutableState<Boolean>, deleteVocOn: MutableState<Boolean>, editVocOn: MutableState<Boolean>, keyState: MutableState<String>, key: String) {
    //DROPDOWN MENU:
    DropdownMenu(
        modifier = Modifier
            .background(colorResource(id = R.color.dark_grey)),
        shape = RoundedCornerShape(20.dp),
        //border = BorderStroke(1.dp, colorResource(id = R.color.mid_grey)),
        expanded = mDisplayMenu.value,
        onDismissRequest = {
            keyState.value = ""
            mDisplayMenu.value = false
        }
    ) {

        //1) Item: EDIT VOC ITEM
        DropdownMenuItem(
            text = {
                Text(
                    text = "Edit",
                    color = colorResource(id = R.color.light_grey),
                    fontSize = 16.sp
                )},
            leadingIcon = {
                Icon(
                    Icons.Default.Edit,
                    "Edit item",
                    tint = colorResource(id = R.color.mid_grey)
                )
            },
            onClick = {
                keyState.value = key
                mDisplayMenu.value = false
                editVocOn.value = true
            }
        )

        //2) Item: DELETE VOC ITEM
        DropdownMenuItem(
            text = {
                Text(
                    text = "Delete",
                    color = colorResource(id = R.color.light_grey),
                    fontSize = 16.sp
                )},
            leadingIcon = {
                Icon(
                    Icons.Default.Delete,
                    "Delete item",
                    tint = colorResource(id = R.color.mid_grey)
                )
            },
            onClick = {
                keyState.value = key
                mDisplayMenu.value = false
                deleteVocOn.value = true
            }
        )
    }
}


@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterialApi::class)
@Composable
fun ExpandableVocSection(
    mContext: Context,
    modifier: Modifier = Modifier,
    head: String,
    title: String,
    subtitle: String,
    sectionIsExpanded: MutableState<Boolean>,
    otherIsExpanded1: MutableState<Boolean>,
    otherIsExpanded2: MutableState<Boolean>,
    keyState: MutableState<String>,
    preview: Boolean = false,
    editPreview: Boolean = false
) {
    val utils = Utilities()

    val vocabulary = rememberSaveable {
        mutableStateOf(getVocKeys(mContext, head, preview))
    }

    val deleteVocOn = rememberSaveable { mutableStateOf(false) }
    if (deleteVocOn.value) {
        DialogDeleteVocabulary(mContext, deleteVocOn, vocabulary, keyState, head)
    }

    val editVocOn = rememberSaveable { mutableStateOf(editPreview) }
    if (editPreview) {
        DialogEditVocabulary(mContext, editVocOn, vocabulary, keyState, vocEditPreview, preview)   //TODO: change in App.kt
    } else if (editVocOn.value) {
        DialogEditVocabulary(mContext, editVocOn, vocabulary, keyState, head, preview)
    }

    if (sectionIsExpanded.value) {
        otherIsExpanded1.value = false
        otherIsExpanded2.value = false
    }

    Column(
        modifier = modifier
            .clickable {
                sectionIsExpanded.value = !sectionIsExpanded.value
                if (sectionIsExpanded.value) {
                    otherIsExpanded1.value = false
                    otherIsExpanded2.value = false
                }
            }
            .fillMaxWidth()
    ) {
        //SECTION:
        ExpandableVocSectionTitle(
            mContext = mContext,
            vocabulary = vocabulary,
            deleteVocOn = deleteVocOn,
            isExpanded = sectionIsExpanded.value,
            head = head,
            title = title,
            subtitle = subtitle)

        AnimatedVisibility(
            modifier = Modifier
                .fillMaxWidth(),

            visible = sectionIsExpanded.value
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
    DropdownMenu(
        modifier = Modifier
            .background(colorResource(id = R.color.dark_grey)),
        shape = RoundedCornerShape(20.dp),
        //border = BorderStroke(1.dp, colorResource(id = R.color.mid_grey)),
        expanded = mDisplayMenu.value,
        onDismissRequest = {
            mDisplayMenu.value = false
        }
    ) {

        //1) Item: REFRESH VOC CATEGORY
        DropdownMenuItem(
            text = {
                Text(
                    text = "Refresh",
                    color = colorResource(id = R.color.light_grey),
                    fontSize = 16.sp
                )},
            leadingIcon = {
                Icon(
                    Icons.Default.Refresh,
                    "Refresh",
                    tint = colorResource(id = R.color.mid_grey)
                )
            },
            onClick = {
                filter.postValue(head)
                mDisplayMenu.value = false
                vocabulary.value = getVocKeys(mContext, head)
                Toast.makeText(mContext, "${head.replaceFirstChar { it.uppercase() }}s vocabulary updated!", Toast.LENGTH_SHORT).show()
            }
        )

        //2) Item: DELETE VOC CATEGORY
        DropdownMenuItem(
            text = {
                Text(
                    text = "Delete all",
                    color = colorResource(id = R.color.light_grey),
                    fontSize = 16.sp
                )},
            leadingIcon = {
                Icon(
                    Icons.Default.Delete,
                    "Delete all",
                    tint = colorResource(id = R.color.mid_grey)
                )
            },
            onClick = {
                filter.postValue(head)
                mDisplayMenu.value = false
                deleteVocOn.value = true
            }
        )
    }
}


@Composable
fun DialogDeleteVocabulary(mContext: Context, dialogOnState: MutableState<Boolean>, vocabulary: MutableState<List<String>>, keyState: MutableState<String>, filter: String) {
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


//Voc keyset:
fun getVocKeys(mContext: Context, head: String, preview: Boolean = false): List<String> {
    val vocKeys = listOf("") + updateVocabulary(mContext, head, preview).keySet().toList()
    return vocKeys
}
