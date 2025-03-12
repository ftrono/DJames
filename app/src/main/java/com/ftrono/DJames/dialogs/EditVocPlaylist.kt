package com.ftrono.DJames.dialogs

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.database.ItemInfoView
import com.ftrono.DJames.database.Playlist
import com.ftrono.DJames.screen.VocabularyScreen
import com.ftrono.DJames.test_objects.testPlaylists
import com.ftrono.DJames.ui.getTextFieldColors
import com.ftrono.DJames.ui.vocColorSelector
import com.ftrono.DJames.ui.vocColorSelectorLight


@Preview
@Preview(heightDp = 360, widthDp = 800)
@Composable
fun DialogEditPlaylistPreview() {
    VocabularyScreen(editPreview="playlist", preview=true)
}


@Composable
fun EditVocPlaylist(
    mContext: Context,
    dialogOnState: MutableState<Boolean>,
    libraryMap: MutableState<Map<String, String>>,
    keyState: MutableState<String>,
    filter: String,
    preview: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }

    //Init:
    var itemPlaylist = Playlist()
    val key = keyState.value

    //Recover info:
    if (key != "") {
        itemPlaylist = if (preview) {
            testPlaylists[0]
        } else {
            libUtils.getPlaylist(keyState.value)
        }
    }

    //Init aliases:
    val initAliases = itemPlaylist.aliases.toMutableList()
    initAliases.removeAt(0)

    //States:
    val textName = rememberSaveable { mutableStateOf(itemPlaylist.name) }
    val textAliases = rememberSaveable { mutableStateOf(initAliases.joinToString(", ")) }
    val textPlayUrl = rememberSaveable { mutableStateOf(itemPlaylist.spotifyUrl) }

    val requestDetailOn = rememberSaveable { mutableStateOf(false) }
    if (requestDetailOn.value) {
        DialogRequestDetail(
            dialogOnState = requestDetailOn,
            title = "Playlist URL",
            message = "Please enter a valid URL for the current Playlist!\n\nPlease copy it from Spotify -> your playlist -> Share -> Copy link."
        )
    }

    //EDIT DIALOG:
    Dialog(
        onDismissRequest = {
            //cancelable -> true
            dialogOnState.value = false
            keyState.value = ""
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current

        //MAIN:
        EditVocDialog(
            modifier = Modifier
                .clickable {
                    focusManager.clearFocus()
                },
            filter = filter,
            onDismiss = {
                //cancelable -> true
                dialogOnState.value = false
                keyState.value = ""
            },
            onSave = {
                //CHECK & BUILD:
                //1) Validate Playlist URL:
                requestDetailOn.value = !utils.isPlaylistUrl(textPlayUrl.value.replace(" ", ""))

                if (!requestDetailOn.value && textName.value != "") {
                    //2) Update object:
                    val aliasesList = mutableListOf(textName.value.lowercase())
                    if (textAliases.value != "") {
                        for (alias in textAliases.value.split(",")) {
                            val temp = alias.lowercase().strip()
                            if (temp != "" && !aliasesList.contains(temp)) {
                                aliasesList.add(temp)
                            }
                        }
                    }
                    itemPlaylist.name = utils.capitalizeWords(textName.value)
                    itemPlaylist.aliases = aliasesList
                    itemPlaylist.spotifyUrl = textPlayUrl.value

                    //3) Update / store to DB:
                    libUtils.storePlaylist(mContext, itemPlaylist)

                    //4) End & close:
                    libraryMap.value = libUtils.refreshLibrary(filter)   //Refresh list
                    dialogOnState.value = false
                    keyState.value = ""
                }
            }
        ) {
            //CONTENT:
            //PLAYLIST NAME:
            EditVocDynamicNameSection(
                textFieldColors = getTextFieldColors(
                    colorLight = vocColorSelectorLight(cat = filter),
                    colorDark = vocColorSelector(cat = filter)
                ),
                filter = filter,
                textState = textName
            )

            //PLAYLIST ALIASES:
            EditVocDynamicField(
                textHeaderColor = vocColorSelectorLight(cat = filter),
                textFieldColors = getTextFieldColors(
                    colorLight = vocColorSelectorLight(cat = filter),
                    colorDark = vocColorSelector(cat = filter)
                ),
                title = "Aliases (separate with commas)",
                placeholder = "Write aliases here...",
                textState = textAliases
            )

            //PLAYLIST URL:
            EditVocDynamicField(
                textHeaderColor = vocColorSelectorLight(cat = filter),
                textFieldColors = getTextFieldColors(
                    colorLight = vocColorSelectorLight(cat = filter),
                    colorDark = vocColorSelector(cat = filter)
                ),
                title = "Spotify Playlist Link",
                placeholder = "Paste here the Spotify link...",
                textState = textPlayUrl
            )
        }
    }
}