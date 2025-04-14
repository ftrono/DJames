package com.ftrono.DJames.application.dialogs

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ftrono.DJames.R
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.database.Playlist
import com.ftrono.DJames.application.screens.VocabularyScreen
import com.ftrono.DJames.application.spotifyUtils
import com.ftrono.DJames.be.samples.testPlaylists
import com.ftrono.DJames.ui.components.CustomCheckbox
import com.ftrono.DJames.ui.dialogs.DialogRequestDetail
import com.ftrono.DJames.ui.dialogs.EditVocDialog
import com.ftrono.DJames.ui.components.EditVocDynamicField
import com.ftrono.DJames.ui.components.EditVocDynamicNameSection
import com.ftrono.DJames.ui.selectors.getTextFieldColors
import com.ftrono.DJames.ui.selectors.vocColorSelector
import com.ftrono.DJames.ui.selectors.vocColorSelectorLight
import com.ftrono.DJames.ui.selectors.vocIconSelector


@Preview
@Preview(heightDp = 360, widthDp = 800)
@Composable
fun DialogEditPlaylistPreview() {
    VocabularyScreen(editPreview="playlist", preview=true)
}


@Composable
fun EditVocPlaylist(
    context: Context,
    libraryMap: MutableState<Map<String, String>>,
    keyState: MutableState<String>,
    initLinkState: MutableState<String>,
    filter: String,
    onDismiss: () -> Unit = {},
    preview: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }

    //Init:
    val key = keyState.value

    //Pre-populate:
    // val itemPlaylist = Playlist()
    var itemPlaylist = if (preview) {
        testPlaylists[0]
    } else if (key != "") {
        libUtils.getPlaylist(keyState.value)
    } else {
        Playlist()
    }

    if (initLinkState.value != "") {
        itemPlaylist = spotifyUtils.getPlaylistInfo(context, initLinkState.value, itemPlaylist, init=true)
    }

    //Init aliases:
    val initAliases = itemPlaylist.aliases.toMutableList()
    initAliases.removeAt(0)

    //States:
    val textName = rememberSaveable { mutableStateOf(itemPlaylist.name) }
    val textAliases = rememberSaveable { mutableStateOf(initAliases.joinToString(", ")) }
    val imageUrlState = rememberSaveable { mutableStateOf(itemPlaylist.imageUrl) }
    val textPlayUrl = rememberSaveable { mutableStateOf(if (initLinkState.value != "") initLinkState.value else itemPlaylist.spotifyUrl) }
    val checkedSpotify = remember { mutableStateOf(itemPlaylist.owner.lowercase() == "spotify") }

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
            onDismiss()
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
            title = filter,
            headerColor = vocColorSelectorLight(cat = filter),
            headerPainter = vocIconSelector(cat = filter),
            showRefresh = true,
            onRefresh = {
                itemPlaylist = spotifyUtils.getPlaylistInfo(context, textPlayUrl.value, itemPlaylist, init=false)
                textName.value = itemPlaylist.name
                imageUrlState.value = itemPlaylist.imageUrl
            },
            onDismiss = {
                onDismiss()
            },
            onSave = {
                //CHECK & BUILD:
                //1) Validate Playlist URL:
                requestDetailOn.value = !spotifyUtils.isPlaylistUrl(textPlayUrl.value.replace(" ", ""))

                if (!requestDetailOn.value && textName.value != "") {
                    //2) Update object:
                    val aliasesList = mutableListOf(textName.value.lowercase())
                    if (textAliases.value != "") {
                        for (alias in textAliases.value.split(",")) {
                            val temp = alias.lowercase().trim()
                            if (temp != "" && !aliasesList.contains(temp)) {
                                aliasesList.add(temp)
                            }
                        }
                    }
                    itemPlaylist.name = utils.capitalizeWords(textName.value).trim()
                    itemPlaylist.aliases = aliasesList
                    itemPlaylist.imageUrl = imageUrlState.value
                    itemPlaylist.spotifyUrl = spotifyUtils.trimSpotifyUrl(textPlayUrl.value)
                    //TODO: TEMP:
                    itemPlaylist.owner =
                        if (checkedSpotify.value) "Spotify" else if (itemPlaylist.owner == "Spotify") "" else itemPlaylist.owner

                    //3) Update / store to DB:
                    libUtils.storePlaylist(context, itemPlaylist)

                    //4) End & close:
                    libraryMap.value = libUtils.refreshLibrary(filter)   //Refresh list
                    onDismiss()
                }
            }
        ) {
            //CONTENT:
            //PLAYLIST NAME:
            EditVocDynamicNameSection(
                textHeaderColor = vocColorSelectorLight(cat = filter),
                textFieldColors = getTextFieldColors(
                    colorLight = vocColorSelectorLight(cat = filter),
                    colorDark = vocColorSelector(cat = filter)
                ),
                filter = filter,
                textState = textName,
                imageUrlState = imageUrlState,
                initActive = textName.value == ""
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
                italic = true,
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
                placeholder = "Paste Spotify link...",
                textState = textPlayUrl
            )

            //OWNER: CHECKBOX:
            CustomCheckbox(
                modifier = Modifier
                    .padding(bottom = 5.dp),
                checkedState = checkedSpotify,
                checkedColor = vocColorSelectorLight(cat = filter),
                textColor = colorResource(id = R.color.light_grey),
                text = "By Spotify"
            )
        }
    }
}