package com.ftrono.DJames.application.dialogs

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.application.screens.LibraryScreen
import com.ftrono.DJames.application.spotifyUtils
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
import androidx.navigation.compose.rememberNavController
import com.ftrono.DJames.be.database.LibraryItem
import com.ftrono.DJames.be.samples.testLibrary


@Preview
@Preview(heightDp = 360, widthDp = 800)
@Composable
fun DialogEditSpotifyPreview() {
    val navController = rememberNavController()
    LibraryScreen(navController, editPreview="artist", preview=true)
}


@Composable
fun EditLibSpotify(
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
    var itemSpotify = if (preview) {
        testLibrary.filter{ it.type == filter && it.source == "spotify" }[0]
    } else if (id > -1) {
        libUtils.getLibItemById(idState.value)
    } else {
        LibraryItem(
            source = "spotify",
            type = filter,
        )
    }

    if (initLinkState.value != "") {
        itemSpotify = spotifyUtils.getSpotifyInfo(context, filter, initLinkState.value, itemSpotify, init=true)   //TODO
        loadingDialogOn.value = false
    }

    //Init aliases:
    val initAliases = itemSpotify.aliases.toMutableList()
    initAliases.removeAt(0)

    //States:
    val textName = rememberSaveable { mutableStateOf(itemSpotify.name) }
    val textDetail = rememberSaveable { mutableStateOf(itemSpotify.detail) }
    val textAliases = rememberSaveable { mutableStateOf(initAliases.joinToString(", ")) }
    val imageUrlState = rememberSaveable { mutableStateOf(itemSpotify.imageUrl) }
    val textPlayUrl = rememberSaveable { mutableStateOf(if (initLinkState.value != "") initLinkState.value else itemSpotify.url) }

    val requestDetailOn = rememberSaveable { mutableStateOf(false) }
    if (requestDetailOn.value) {
        DialogRequestDetail(
            dialogOnState = requestDetailOn,
            title = "${utils.capitalizeWords(filter)} URL",
            message = "Please enter a valid URL for the current ${utils.capitalizeWords(filter)}!\n\nPlease copy it from Spotify -> $filter page -> Share -> Copy link."
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
                itemSpotify = spotifyUtils.getSpotifyInfo(context, filter, textPlayUrl.value, itemSpotify, init=false)   // TODO
                textName.value = itemSpotify.name
                textDetail.value = itemSpotify.detail
                imageUrlState.value = itemSpotify.imageUrl
            },
            onDismiss = {
                onDismiss()
            },
            onSave = {
                //CHECK & BUILD:
                //1) Validate Spotify URL:
                requestDetailOn.value = spotifyUtils.disambiguateSpotifyURL(textPlayUrl.value.replace(" ", "")) != filter

                if (!requestDetailOn.value && textName.value != "") {
                    //2) Update object:
                    val aliasesList = mutableListOf(utils.cleanString(textName.value).lowercase())
                    Log.d("EditLibSpotify", "$aliasesList")
                    if (textAliases.value != "") {
                        for (alias in textAliases.value.split(",")) {
                            val temp = utils.cleanString(alias).lowercase()
                            if (temp != "" && !aliasesList.contains(temp)) {
                                aliasesList.add(temp)
                            }
                        }
                    }
                    itemSpotify.name = textName.value.trim()
                    itemSpotify.aliases = aliasesList
                    itemSpotify.imageUrl = imageUrlState.value
                    itemSpotify.url = spotifyUtils.trimSpotifyUrl(textPlayUrl.value)

                    //3) Update / store to DB:
                    libUtils.storeLibItem(context, itemSpotify)

                    //4) End & close:
                    libraryItems.value = libUtils.refreshLibrary(filter)   //Refresh list
                    onDismiss()
                }
            }
        ) {
            //CONTENT:
            //SPOTIFY NAME:
            EditLibDynamicNameSection(
                textHeaderColor = libColorSelectorLight(cat = filter),
                textFieldColors = getTextFieldColors(
                    colorLight = libColorSelectorLight(cat = filter),
                    colorDark = libColorSelector(cat = filter)
                ),
                filter = filter,
                textState = textName,
                subtitleState = textDetail,
                imageUrlState = imageUrlState,
                initActive = textName.value == "",
                showEditIcon = textName.value == ""
            )

            //SPOTIFY ALIASES:
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

            //SPOTIFY URL:
            EditLibDynamicField(
                modifier = Modifier
                    .fillMaxWidth(),
                textHeaderColor = libColorSelectorLight(cat = filter),
                textFieldColors = getTextFieldColors(
                    colorLight = libColorSelectorLight(cat = filter),
                    colorDark = libColorSelector(cat = filter)
                ),
                title = "Spotify Link",
                placeholder = "Paste Spotify link...",
                textState = textPlayUrl
            )
        }
    }
}