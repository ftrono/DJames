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
import com.ftrono.DJames.be.database.Podcast
import com.ftrono.DJames.application.screens.VocabularyScreen
import com.ftrono.DJames.application.spotifyUtils
import com.ftrono.DJames.be.samples.testPodcasts
import com.ftrono.DJames.ui.dialogs.DialogRequestDetail
import com.ftrono.DJames.ui.dialogs.EditVocDialog
import com.ftrono.DJames.ui.components.EditVocDynamicField
import com.ftrono.DJames.ui.components.EditVocDynamicNameSection
import com.ftrono.DJames.ui.selectors.getTextFieldColors
import com.ftrono.DJames.ui.selectors.vocColorSelector
import com.ftrono.DJames.ui.selectors.vocColorSelectorLight
import com.ftrono.DJames.ui.selectors.vocIconSelector
import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
import com.ftrono.DJames.ui.components.EditVocTitle


@Preview
@Preview(heightDp = 360, widthDp = 800)
@Composable
fun DialogEditPodcastPreview() {
    VocabularyScreen(editPreview="podcast", preview=true)
}


@Composable
fun EditVocPodcast(
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
    // val itemPodcast = Podcast()
    var itemPodcast = if (preview) {
        testPodcasts[0]
    } else if (id > -1) {
        libUtils.getPodcast(idState.value)
    } else {
        Podcast()
    }

    if (initLinkState.value != "") {
        itemPodcast = spotifyUtils.getPodcastInfo(context, initLinkState.value, itemPodcast, init=true)
        loadingDialogOn.value = false
    }

    //Init aliases:
    val initAliases = itemPodcast.aliases.toMutableList()
    initAliases.removeAt(0)

    //States:
    val textName = rememberSaveable { mutableStateOf(itemPodcast.name) }
    val textAliases = rememberSaveable { mutableStateOf(initAliases.joinToString(", ")) }
    val textPublisher = rememberSaveable { mutableStateOf(itemPodcast.publisher) }
    val textLanguages = rememberSaveable { mutableStateOf(itemPodcast.languages.joinToString(", ")) }
    val textDescription = rememberSaveable { mutableStateOf(itemPodcast.description) }
    val imageUrlState = rememberSaveable { mutableStateOf(itemPodcast.imageUrl) }
    val textPlayUrl = rememberSaveable { mutableStateOf(if (initLinkState.value != "") initLinkState.value else itemPodcast.spotifyUrl) }

    val requestDetailOn = rememberSaveable { mutableStateOf(false) }
    if (requestDetailOn.value) {
        DialogRequestDetail(
            dialogOnState = requestDetailOn,
            title = "Podcast URL",
            message = "Please enter a valid URL for the current Podcast!\n\nPlease copy it from Spotify -> your podcast -> Share -> Copy link."
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
                itemPodcast = spotifyUtils.getPodcastInfo(context, textPlayUrl.value, itemPodcast, init=false)
                textName.value = itemPodcast.name
                textPublisher.value = itemPodcast.publisher
                textDescription.value = itemPodcast.description
                textLanguages.value = itemPodcast.languages.joinToString(", ")
                imageUrlState.value = itemPodcast.imageUrl
                // itemPodcast.languages is automatically overwritten
            },
            onDismiss = {
                onDismiss()
            },
            onSave = {
                //CHECK & BUILD:
                //1) Validate Podcast URL:
                requestDetailOn.value = spotifyUtils.disambiguateSpotifyURL(textPlayUrl.value.replace(" ", "")) != "podcast"

                if (!requestDetailOn.value && textName.value != "") {
                    //2) Update object:
                    val aliasesList = mutableListOf(utils.cleanString(textName.value).lowercase())
                    Log.d("EditVocPodcast", "$aliasesList")
                    if (textAliases.value != "") {
                        for (alias in textAliases.value.split(",")) {
                            val temp = utils.cleanString(alias).lowercase()
                            if (temp != "" && !aliasesList.contains(temp)) {
                                aliasesList.add(temp)
                            }
                        }
                    }
                    itemPodcast.name = textName.value.trim()   //utils.capitalizeWords(textName.value).trim()
                    itemPodcast.aliases = aliasesList
                    itemPodcast.publisher = textPublisher.value.trim()
                    itemPodcast.description = textDescription.value.trim()
                    itemPodcast.name = textName.value.trim()
                    itemPodcast.imageUrl = imageUrlState.value
                    itemPodcast.spotifyUrl = spotifyUtils.trimSpotifyUrl(textPlayUrl.value)
                    // itemPodcast.languages is automatically overwritten

                    //3) Update / store to DB:
                    libUtils.storePodcast(context, itemPodcast)

                    //4) End & close:
                    libraryItems.value = libUtils.refreshLibrary(filter)   //Refresh list
                    onDismiss()
                }
            }
        ) {
            //CONTENT:
            //PODCAST NAME:
            EditVocDynamicNameSection(
                textHeaderColor = vocColorSelectorLight(cat = filter),
                textFieldColors = getTextFieldColors(
                    colorLight = vocColorSelectorLight(cat = filter),
                    colorDark = vocColorSelector(cat = filter)
                ),
                filter = filter,
                textState = textName,
                subtitleState = textPublisher,
                imageUrlState = imageUrlState,
                initActive = textName.value == "",
                showEditIcon = textName.value == ""
            )

            //PODCAST ALIASES:
            EditVocDynamicField(
                modifier = Modifier
                    .fillMaxWidth(),
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

            //PODCAST URL:
            EditVocDynamicField(
                modifier = Modifier
                    .fillMaxWidth(),
                textHeaderColor = vocColorSelectorLight(cat = filter),
                textFieldColors = getTextFieldColors(
                    colorLight = vocColorSelectorLight(cat = filter),
                    colorDark = vocColorSelector(cat = filter)
                ),
                title = "Spotify Podcast Link",
                placeholder = "Paste Spotify link...",
                textState = textPlayUrl
            )

            //PODCAST DESCRIPTION:
            if (textDescription.value != "") {
                EditVocTitle(
                    textHeaderColor = vocColorSelectorLight(cat = filter),
                    fontSize = 16.sp,
                    title = "Description",
                )
                Text(
                    modifier = Modifier
                        .padding(top = 6.dp),
                    text = textDescription.value,
                    fontSize = 16.sp,
                    color = colorResource(id = R.color.mid_grey)
                )
            }
            
        }
    }
}