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
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.screen.VocabularyScreen
import com.ftrono.DJames.screen.getVocItem
import com.ftrono.DJames.screen.getVocKeys
import com.ftrono.DJames.ui.getTextFieldColors
import com.ftrono.DJames.ui.vocColorSelector
import com.ftrono.DJames.ui.vocColorSelectorLight
import com.google.gson.JsonObject


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
    vocKeys: MutableState<List<String>>,
    keyState: MutableState<String>,
    filter: String,
    preview: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }

    //Prev:
    val prevKey = keyState.value
    var prevItem = JsonObject()

    //Init:
    var key = keyState.value
    var initName = key
    var initPlayUrl = ""

    //Recover info:
    if (key != "") {
        //TODO: extract useful data:
        prevItem = getVocItem(mContext, filter, keyState.value, preview)
        initName = prevItem.get("name").asString
        initPlayUrl = prevItem.get("playlist_URL").asString
    }

    //States:
    var textName = rememberSaveable { mutableStateOf(initName) }
    var textPlayUrl = rememberSaveable { mutableStateOf(initPlayUrl) }

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

                if (!requestDetailOn.value) {
                    //2) Build object:
                    var newItem = JsonObject()
                    newItem.addProperty("name", textName.value)
                    newItem.addProperty("playlist_URL", textPlayUrl.value.replace(" ", "").split("?")[0])

                    //3) Store:
                    if (newItem != prevItem) {
                        utils.writeLibraryItem(
                            context = mContext,
                            filter = filter,
                            key = textName.value,
                            item = newItem,
                            prevKey = prevKey
                        )
                    }

                    //4) End & close:
                    vocKeys.value = getVocKeys(mContext, filter)   //Refresh list
                    dialogOnState.value = false
                    keyState.value = ""
                }
            }
        ) {
            //CONTENT:
            //PLAYLIST NAME:
            EditVocTextField(
                modifier = Modifier
                    .focusRequester(focusRequester),
                onKeyboardDone = {
                    focusManager.clearFocus()
                    keyboardController!!.hide()
                },
                textHeaderColor = vocColorSelectorLight(cat = filter),
                textFieldColors = getTextFieldColors(
                    colorLight = vocColorSelectorLight(cat = filter),
                    colorDark = vocColorSelector(cat = filter)
                ),
                title = "Name",
                placeholder = "Write $filter name...",
                textState = textName
            )

            //ARTIST: PLAYLIST URL:
            EditVocTextField(
                modifier = Modifier
                    .focusRequester(focusRequester),
                onKeyboardDone = {
                    focusManager.clearFocus()
                    keyboardController!!.hide()
                },
                textHeaderColor = vocColorSelectorLight(cat = filter),
                textFieldColors = getTextFieldColors(
                    colorLight = vocColorSelectorLight(cat = filter),
                    colorDark = vocColorSelector(cat = filter)
                ),
                title = if (filter == "artist") "'This is' playlist URL" else "Playlist URL",
                placeholder = "Paste here the Spotify link...",
                textState = textPlayUrl
            )
        }
    }
}