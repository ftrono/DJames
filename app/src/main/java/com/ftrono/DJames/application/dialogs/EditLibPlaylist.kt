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
import com.ftrono.DJames.application.screens.LibraryScreen
import com.ftrono.DJames.application.spotifyUtils
import com.ftrono.DJames.be.samples.testPlaylists
import com.ftrono.DJames.ui.components.CustomCheckbox
import com.ftrono.DJames.ui.dialogs.DialogRequestDetail
import com.ftrono.DJames.ui.dialogs.EditLibDialog
import com.ftrono.DJames.ui.components.EditLibDynamicField
import com.ftrono.DJames.ui.components.EditLibDynamicNameSection
import com.ftrono.DJames.ui.selectors.getTextFieldColors
import com.ftrono.DJames.ui.selectors.libColorSelector
import com.ftrono.DJames.ui.selectors.libColorSelectorLight
import com.ftrono.DJames.ui.selectors.libIconSelector
import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth


@Preview
@Preview(heightDp = 360, widthDp = 800)
@Composable
fun DialogEditPlaylistPreview() {
    LibraryScreen(editPreview="playlist", preview=true)
}


@Composable
fun EditLibPlaylist(
    context: Context,
    libraryItems: MutableState<List<String>>,
    idState: MutableState<Long>,
    initLinkState: MutableState<String>,
    loadingDialogOn: MutableState<Boolean>,
    filter: String,
    onDismiss: () -> Unit = {},
    preview: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }

    //Init:
    val id: Long = idState.value

    //Pre-populate:
    // val itemPlaylist = Playlist()
    var itemPlaylist = if (preview) {
        testPlaylists[0]
    } else if (id > -1) {
        libUtils.getPlaylist(idState.value)
    } else {
        Playlist()
    }

    if (initLinkState.value != "") {
        itemPlaylist = spotifyUtils.getPlaylistInfo(context, initLinkState.value, itemPlaylist, init=true)
        loadingDialogOn.value = false
    }

    //Init aliases:
    val initAliases = itemPlaylist.aliases.toMutableList()
    initAliases.removeAt(0)

    //States:
    val textName = rememberSaveable { mutableStateOf(itemPlaylist.name) }
    val textOwner = rememberSaveable { mutableStateOf("by " + itemPlaylist.owner) }
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
            usePlatformDefaultWidth = true
        )
    ) {
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current

        //MAIN:
        EditLibDialog(
            modifier = Modifier
                .clickable {
                    focusManager.clearFocus()
                },
            title = filter,
            headerColor = libColorSelectorLight(cat = filter),
            headerPainter = libIconSelector(cat = filter),
            showRefresh = true,
            onRefresh = {
                itemPlaylist = spotifyUtils.getPlaylistInfo(context, textPlayUrl.value, itemPlaylist, init=false)
                textName.value = itemPlaylist.name
                textOwner.value = "by " + itemPlaylist.owner
                imageUrlState.value = itemPlaylist.imageUrl
            },
            onDismiss = {
                onDismiss()
            },
            onSave = {
                //CHECK & BUILD:
                //1) Validate Playlist URL:
                requestDetailOn.value = spotifyUtils.disambiguateSpotifyURL(textPlayUrl.value.replace(" ", "")) != "playlist"

                if (!requestDetailOn.value && textName.value != "") {
                    //2) Update object:
                    val aliasesList = mutableListOf(utils.cleanString(textName.value).lowercase())
                    Log.d("EditLibPlaylist", "$aliasesList")
                    if (textAliases.value != "") {
                        for (alias in textAliases.value.split(",")) {
                            val temp = utils.cleanString(alias).lowercase()
                            if (temp != "" && !aliasesList.contains(temp)) {
                                aliasesList.add(temp)
                            }
                        }
                    }
                    itemPlaylist.name = textName.value.trim()   //utils.capitalizeWords(textName.value).trim()
                    itemPlaylist.aliases = aliasesList
                    itemPlaylist.imageUrl = imageUrlState.value
                    itemPlaylist.spotifyUrl = spotifyUtils.trimSpotifyUrl(textPlayUrl.value)
                    //TODO: TEMP:
                    itemPlaylist.owner =
                        if (checkedSpotify.value) "Spotify" else if (itemPlaylist.owner == "Spotify") "" else itemPlaylist.owner

                    //3) Update / store to DB:
                    libUtils.storePlaylist(context, itemPlaylist)

                    //4) End & close:
                    libraryItems.value = libUtils.refreshLibrary(filter)   //Refresh list
                    onDismiss()
                }
            }
        ) {
            //CONTENT:
            //PLAYLIST NAME:
            EditLibDynamicNameSection(
                textHeaderColor = libColorSelectorLight(cat = filter),
                textFieldColors = getTextFieldColors(
                    colorLight = libColorSelectorLight(cat = filter),
                    colorDark = libColorSelector(cat = filter)
                ),
                filter = filter,
                textState = textName,
                subtitleState = textOwner,
                imageUrlState = imageUrlState,
                initActive = textName.value == "",
                showEditIcon = textName.value == ""
            )

            //PLAYLIST ALIASES:
            EditLibDynamicField(
                modifier = Modifier
                    .fillMaxWidth(),
                textHeaderColor = libColorSelectorLight(cat = filter),
                textFieldColors = getTextFieldColors(
                    colorLight = libColorSelectorLight(cat = filter),
                    colorDark = libColorSelector(cat = filter)
                ),
                title = "Aliases (separate with commas)",
                placeholder = "Write aliases here...",
                italic = true,
                textState = textAliases
            )

            //PLAYLIST URL:
            EditLibDynamicField(
                modifier = Modifier
                    .fillMaxWidth(),
                textHeaderColor = libColorSelectorLight(cat = filter),
                textFieldColors = getTextFieldColors(
                    colorLight = libColorSelectorLight(cat = filter),
                    colorDark = libColorSelector(cat = filter)
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
                checkedColor = libColorSelectorLight(cat = filter),
                textColor = colorResource(id = R.color.light_grey),
                text = "By Spotify",
                onClickExtra = {
                    if (checkedSpotify.value) {
                        textOwner.value = "by Spotify"
                    } else {
                        textOwner.value = ""
                    }
                }
            )
        }
    }
}