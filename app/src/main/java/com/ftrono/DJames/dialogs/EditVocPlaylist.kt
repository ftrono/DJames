package com.ftrono.DJames.dialogs

import android.content.Context
import android.util.Patterns
import android.webkit.URLUtil
import android.widget.Toast
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
import com.ftrono.DJames.application.playlistUrlIntro
import com.ftrono.DJames.screen.VocabularyScreen
import com.ftrono.DJames.screen.getVocKeys
import com.ftrono.DJames.screen.updateVocabulary
import com.ftrono.DJames.ui.getTextFieldColors
import com.ftrono.DJames.ui.vocColorSelector
import com.ftrono.DJames.ui.vocColorSelectorLight
import com.ftrono.DJames.utilities.Utilities
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
    vocabulary: MutableState<List<String>>,
    keyState: MutableState<String>,
    filter: String,
    preview: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    //Init:
    var key = keyState.value
    var vocItems = updateVocabulary(mContext, filter, preview)
    var initName = key
    var initPlayUrl = ""

    //Recover info:
    if (key != "") {
        val prevDetails = vocItems.get(key).asJsonObject
        if (prevDetails.has("playlist_URL")) {
            initPlayUrl = prevDetails.get("playlist_URL").asString
        }
    }

    //States:
    var textName = rememberSaveable { mutableStateOf(initName) }
    var textPlayURL = rememberSaveable { mutableStateOf(initPlayUrl) }

    val requestDetailOn = rememberSaveable { mutableStateOf(false) }
    if (requestDetailOn.value) {
        DialogRequestDetail(
            dialogOnState = requestDetailOn,
            title = "Playlist URL",
            message = "Please enter a valid URL for the current Playlist!\n\nPlease copy it from Spotify -> your playlist -> Share -> Copy link."
        )
    }

    //EDIT DIALOG:
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
            //CHECK & UPDATE:
            var newDetails = JsonObject()
            //Get new info:
            newDetails.addProperty(
                "playlist_URL",
                textPlayURL.value.replace(" ", "")
            )
            //Edit:
            requestDetailOn.value = validateEditPlaylist(
                mContext = mContext,
                vocabulary = vocabulary,
                prevText = initName,
                newText = textName.value.lowercase(),
                newDetails = newDetails,
                filter = filter,
                vocItems = vocItems
            )
            if (!requestDetailOn.value) {
                //CLOSE THE DIALOG:
                dialogOnState.value = false
                keyState.value = ""
            }
        }
    ) {
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
            textState = textPlayURL
        )
    }
}


//Validate Edit Playlist:
fun validateEditPlaylist(
    mContext: Context,
    vocabulary: MutableState<List<String>>,
    prevText: String,
    newText: String,
    newDetails: JsonObject,
    filter: String,
    vocItems: JsonObject
): Boolean {
    //Return true -> Show DialogRequestDetail;
    //Return false -> Don't show DialogRequestDetail.

    if (newText != "") {
        var newURL = newDetails.get("playlist_URL").asString
        var urlTest = URLUtil.isValidUrl(newURL) && Patterns.WEB_URL.matcher(newURL).matches()
        if (!urlTest || !newURL.contains(playlistUrlIntro)) {
            //Request enter valid URL:
            return true
        } else {
            newURL = newURL.split("?")[0]
            newDetails.addProperty("playlist_URL", newURL)
        }

        var prevDetails = JsonObject()
        if (vocItems.has(prevText)) {
            prevDetails = vocItems.get(prevText).asJsonObject
        }
//        Log.d("EditVoc", prevDetails.toString())
//        Log.d("EditVoc", newDetails.toString())

        //Save to file:
        if (newText != prevText || newDetails != prevDetails) {
            val utils = Utilities()
            var ret = utils.editVocFile(prevText=prevText, newText=newText, newDetails=newDetails)
            if (ret == 0) {
                Toast.makeText(mContext, "Saved!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(mContext, "ERROR: Vocabulary not updated!", Toast.LENGTH_LONG).show()
            }
            vocabulary.value = getVocKeys(mContext, filter)   //Refresh list
        }
    }

    return false
}


