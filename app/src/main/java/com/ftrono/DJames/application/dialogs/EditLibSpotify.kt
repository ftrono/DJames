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
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.ftrono.DJames.application.aliasFieldDescription
import com.ftrono.DJames.application.sharedLink
import com.ftrono.DJames.application.spotIntroUrl
import com.ftrono.DJames.be.database.LibraryItem
import com.ftrono.DJames.be.collections.defaultCollection
import com.ftrono.DJames.be.collections.testLibrary
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString


@Preview
@Preview(heightDp = 360, widthDp = 800)
@Composable
fun DialogEditSpotifyPreview() {
    val navController = rememberNavController()
    LibraryScreen(navController, editPreview="spotify", preview=true)
}


@Composable
fun EditLibSpotify(
    context: Context,
    snapshot: MutableState<Long>,
    extractedItemState: MutableState<String>,
    idState: MutableState<Long>,
    onDismiss: () -> Unit = {},
    preview: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }
    val mContext = LocalContext.current
    val sharedLinkState by sharedLink.observeAsState()
    val isDefault = idState.value == -2L   //Default

    //Init:
    val id: Long = idState.value
    var itemSpotify = LibraryItem(
        source = "spotify",
    )

    //Pre-populate:
    if (idState.value == -2L) {
        itemSpotify = defaultCollection
    } else if (extractedItemState.value != "") {
        itemSpotify = Json.decodeFromString<LibraryItem>(extractedItemState.value)
        extractedItemState.value = ""
    } else if (preview) {
        val filter = "artist"
        itemSpotify = testLibrary.filter{ it.type == filter && it.source == "spotify" }[0]
    } else if (id > -1) {
        itemSpotify = libUtils.getLibItemById(idState.value)
    }

    //Init aliases:
    val initAliases = itemSpotify.aliases.toMutableList()
    if (initAliases.isNotEmpty()) {
        initAliases.removeAt(0)
    }

    //States:
    val textType = rememberSaveable { mutableStateOf(itemSpotify.type) }
    val textName = rememberSaveable { mutableStateOf(itemSpotify.name) }
    val textDetail = rememberSaveable { mutableStateOf(itemSpotify.detail) }
    val textAliases = rememberSaveable { mutableStateOf(initAliases.joinToString(", ")) }
    val imageUrlState = rememberSaveable { mutableStateOf(itemSpotify.imageUrl) }
    val textPlayUrl = rememberSaveable { mutableStateOf(if (sharedLinkState != "") sharedLinkState!! else itemSpotify.url) }

    val requestDetailOn = rememberSaveable { mutableStateOf(false) }
    if (requestDetailOn.value) {
        DialogRequestDetail(
            dialogOnState = requestDetailOn,
            title = "Spotify URL",
            message = "Please enter a valid Spotify URL!\n\nPlease copy it from Spotify -> item page -> Share -> Copy link."
        )
    }

    LaunchedEffect(textType) {
        if (textType.value == "") textType.value = "spotify"
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
                .clickable(
                    // This makes the rest of the screen clear focus on tap
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    focusManager.clearFocus()
                },
            title = if (textType.value == "spotify") "${textType.value} link" else utils.capitalizeWords(textType.value),
            cat = "spotify",
            subcat = if (textType.value == "spotify") "" else textType.value,
            showRefresh = !isDefault,
            onRefresh = {
                Toast.makeText(mContext, "Refreshing info...", Toast.LENGTH_LONG).show()
                textPlayUrl.value = spotifyUtils.trimSpotifyUrl(textPlayUrl.value)
                itemSpotify.url = textPlayUrl.value
                itemSpotify = spotifyUtils.callLinkExtractor(mContext, itemSpotify, new=false)
                textType.value = itemSpotify.type
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
                sharedLink.postValue("")
                requestDetailOn.value = !textPlayUrl.value.replace(" ", "").contains(spotIntroUrl)

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
                    itemSpotify.type = textType.value
                    itemSpotify.name = textName.value.trim()
                    itemSpotify.aliases = aliasesList
                    itemSpotify.lastUpdated = utils.getCurrentTimestamp()
                    itemSpotify.detail = textDetail.value
                    itemSpotify.imageUrl = imageUrlState.value
                    itemSpotify.url = spotifyUtils.trimSpotifyUrl(textPlayUrl.value)

                    //3) Update / store to DB:
                    libUtils.storeLibItem(context, itemSpotify)

                    //4) End & close:
                    snapshot.value = utils.getCurrentTimestamp()   //Refresh list
                    onDismiss()
                }
            }
        ) {
            //CONTENT:
            //SPOTIFY NAME:
            EditLibDynamicNameSection(
                filter = if (isDefault) "collection" else textType.value,
                textState = textName,
                subtitleState = textDetail,
                imageUrl = imageUrlState.value,
                disabled = isDefault,
                preview = preview,
            )

            //SPOTIFY ALIASES:
            EditLibDynamicField(
                modifier = Modifier
                    .fillMaxWidth(),
                textHeaderColor = libColorSelectorLight(cat = textType.value),
                textFieldColors = getTextFieldColors(
                    colorLight = libColorSelectorLight(cat = textType.value),
                    colorDark = libColorSelector(cat = textType.value)
                ),
                title = "Aliases (separate with commas)",
                description = aliasFieldDescription,
                placeholder = "Write aliases here...",
                italic = true,
                textState = textAliases
            )

            //SPOTIFY URL:
            EditLibDynamicField(
                modifier = Modifier
                    .fillMaxWidth(),
                textHeaderColor = libColorSelectorLight(cat = textType.value),
                textFieldColors = getTextFieldColors(
                    colorLight = libColorSelectorLight(cat = textType.value),
                    colorDark = libColorSelector(cat = textType.value)
                ),
                title = "Spotify Link",
                placeholder = "Paste Spotify link...",
                textState = textPlayUrl,
                disabled = isDefault
            )
        }
    }
}