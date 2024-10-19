package com.ftrono.DJames.screen

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
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
import com.ftrono.DJames.application.headOrder
import com.ftrono.DJames.application.vocDir
import com.ftrono.DJames.application.vocEditPreview
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterialApi::class)
@Composable
fun VocabularyScreen(editPreview: Boolean = false, preview: Boolean = false) {
    val mContext = LocalContext.current

    var keyState = rememberSaveable {
        mutableStateOf("")
    }

    var iCat = 0

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
                    text = "🎧  My Vocabulary",
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
                    text = "Help DJames understand you",
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
        }
        Column (
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(id = R.color.windowBackground))
                .verticalScroll(rememberScrollState())
        ) {
            //SECTIONS:
            for (head in headOrder) {
                var vocabulary = rememberSaveable {
                    mutableStateOf(listOf<String>(""))
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
                var sectionIsExpanded = rememberSaveable {
                    mutableStateOf(
                        if (preview) {
                            if (iCat == 0) true else false
                        } else false
                    )
                }
                if (sectionIsExpanded.value) {
                    vocabulary.value = getVocKeys(mContext, head, preview)
                }

                ExpandableVocSection(
                    mContext = mContext,
                    vocabulary = vocabulary,
                    deleteVocOn = deleteVocOn,
                    modifier = Modifier
                        .fillMaxWidth(),
                    sectionIsExpanded = sectionIsExpanded,
                    head = head,
                    title = "My ${head.replaceFirstChar { it.uppercase() }}s",
                    subtitle = if (head == "artist") {
                        "Favourite or hard-to-spell names"
                    } else if (head == "playlist") {
                        "For playing songs within"
                    } else (
                        "For calls and messages"
                    )
                ) {
                    ContextualFlowRow(
                        modifier = Modifier
                            .padding(start = 52.dp, end = 24.dp, bottom = 12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalArrangement = Arrangement.Center,
                        itemCount = getVocKeys(mContext, head, preview).size
                    ) {
                        index ->
                        //VOC CHIPS:
                        val key = vocabulary.value[index]
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .wrapContentSize()
                        ) {
                            var mDisplayMenu = rememberSaveable {
                                mutableStateOf(false)
                            }

                            Chip(
                                colors = ChipDefaults.chipColors(
                                    backgroundColor = if (key == "") {
                                        colorResource(id = R.color.colorAccent)
                                    } else if (mDisplayMenu.value) {
                                        colorResource(id = R.color.colorPrimary)
                                    } else {
                                        colorResource(id = R.color.dark_grey_background)
                                    },
                                    contentColor = colorResource(id = R.color.light_grey),
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
                                            padding = 2,
                                            size = 20,
                                            filter = head
                                        )
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
                            ) {
                                Text(
                                    modifier = Modifier
                                        .wrapContentWidth()
                                        .wrapContentHeight(),
                                    color = if (mDisplayMenu.value) colorResource(id = R.color.colorAccentLight) else colorResource(id = R.color.light_grey),
                                    fontSize = 14.sp,
                                    fontStyle = if (key == "") null else FontStyle.Italic,
                                    text = if (key == "") "ADD" else key
                                )
                            }
                            ChipOptions(mDisplayMenu, deleteVocOn, editVocOn, keyState, key)
                        }
                    }
                }
                iCat ++
            }
        }
    }

}


@Composable
fun VocIcon(
    filter: String,
    size: Int,
    padding: Int
) {
    Icon(
        modifier = Modifier
            .padding(start = padding.dp)
            .size(size.dp),
        painter = when (filter) {
            "artist" -> {
                painterResource(id = R.drawable.chip_note)
            }
            "playlist" -> {
                painterResource(id = R.drawable.chip_headphones)
            }
            else -> {
                painterResource(id = R.drawable.chip_phone)
            }
        },
        contentDescription = filter,
        tint = colorResource(id = R.color.colorAccentLight)
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


@Composable
fun ExpandableVocSection(
    mContext: Context,
    vocabulary: MutableState<List<String>>,
    deleteVocOn: MutableState<Boolean>,
    modifier: Modifier = Modifier,
    head: String,
    title: String,
    subtitle: String,
    sectionIsExpanded: MutableState<Boolean>,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .clickable { sectionIsExpanded.value = !sectionIsExpanded.value }
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
            content()
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
    //CARD:
    Card(
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
            containerColor = if (isExpanded) colorResource(id = R.color.windowBackground) else colorResource(id = R.color.dark_grey_background)
        )
    ) {
        //SECTION HEADER:
        Row(
            modifier = modifier
                .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            //CAT ICON:
            VocIcon(
                padding = 8,
                size = 32,
                filter = head
            )
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
                    color = colorResource(id = R.color.mid_grey)
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
                Toast.makeText(mContext, "Vocabulary updated!", Toast.LENGTH_SHORT).show()
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
            containerColor = colorResource(id = R.color.dark_grey),
            title = {
                Text(
                    text = if (key != "") "Delete $filter" else "Delete ${filter}s",
                    color = colorResource(id = R.color.light_grey)
                ) },
            text = {
                Text(
                    text = if (key != "") {
                        "Do you want to delete this $filter?\n\n$key"
                    } else {
                        "Do you want to delete all ${filter}s in vocabulary?"
                    },
                    color = colorResource(id = R.color.mid_grey)
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
                    fontSize = 14.sp,
                    color = colorResource(id = R.color.colorAccentLight)
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
