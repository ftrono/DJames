package com.ftrono.DJames.application.dialogs

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ftrono.DJames.R
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.database.Artist
import com.ftrono.DJames.be.database.PlayLink
import com.ftrono.DJames.application.screens.VocabularyScreen
import com.ftrono.DJames.application.spotifyUtils
import com.ftrono.DJames.be.samples.testArtists
import com.ftrono.DJames.ui.components.DropdownSpinner
import com.ftrono.DJames.ui.components.SectionTitle
import com.ftrono.DJames.ui.dialogs.DialogRequestDetail
import com.ftrono.DJames.ui.dialogs.EditVocDialog
import com.ftrono.DJames.ui.components.EditVocDynamicField
import com.ftrono.DJames.ui.components.EditVocDynamicNameSection
import com.ftrono.DJames.ui.components.EditVocSectionTitle
import com.ftrono.DJames.ui.components.EditVocTitle
import com.ftrono.DJames.ui.selectors.getTextFieldColors
import com.ftrono.DJames.ui.selectors.vocColorSelector
import com.ftrono.DJames.ui.selectors.vocColorSelectorLight
import com.ftrono.DJames.ui.selectors.vocIconSelector


@Preview
@Preview(heightDp = 360, widthDp = 800)
@Composable
fun DialogEditArtistPreview() {
    VocabularyScreen(editPreview="artist", preview=true)
}


@Composable
fun EditVocArtist(
    context: Context,
    libraryMap: MutableState<Map<String, String>>,
    keyState: MutableState<String>,
    initLinkState: MutableState<String>,
    loadingDialogOn: MutableState<Boolean>,
    filter: String,
    onDismiss: () -> Unit = {},
    preview: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }

    //Init:
    val key = keyState.value

    //Pre-populate:
    // val itemArtist = Artist()
    var itemArtist = if (preview) {
        testArtists[0]
    } else if (key != "") {
        libUtils.getArtist(keyState.value)
    } else {
        Artist()
    }

    if (initLinkState.value != "") {
        itemArtist = spotifyUtils.getArtistInfo(context, initLinkState.value, itemArtist, init=true)
        loadingDialogOn.value = false
    }

    //Init aliases:
    val initAliases = itemArtist.aliases.toMutableList()
    initAliases.removeAt(0)
    val playLinks = itemArtist.playLinks
    val initPlayThisIsUrl = if (playLinks["spotify_this_is"] == null) "" else playLinks["spotify_this_is"]!!.spotifyUrl   //TODO: TEMP
    val initPlayRadioUrl = if (playLinks["spotify_radio"] == null) "" else playLinks["spotify_radio"]!!.spotifyUrl
    val initPlayMixUrl = if (playLinks["spotify_mix"] == null) "" else playLinks["spotify_mix"]!!.spotifyUrl

    //TODO: EXTEND!
    var playOptionsKeysToVal = mutableMapOf<String, String>(
        "artist" to "Artist Top Tracks",
        "spotify_this_is" to "Spotify \"This Is\" Playlist"
    )
    var playOptionsValToKeys = mutableMapOf<String, String>(
        "Artist Top Tracks" to "artist",
        "Spotify \"This Is\" Playlist" to "spotify_this_is"
    )
    val initDefaultPlay = playOptionsKeysToVal[itemArtist.defaultPlay]!!

    //States:
    val textName = rememberSaveable { mutableStateOf(itemArtist.name) }
    val textGenres = rememberSaveable { mutableStateOf(itemArtist.genres.joinToString(", ")) }
    val textAliases = rememberSaveable { mutableStateOf(initAliases.joinToString(", ")) }
    val imageUrlState = rememberSaveable { mutableStateOf(itemArtist.imageUrl) }
    val textDefaultPlay = rememberSaveable { mutableStateOf(initDefaultPlay) }
    val textArtistUrl = rememberSaveable { mutableStateOf(if (initLinkState.value != "") initLinkState.value else itemArtist.spotifyUrl) }
    val textPlayThisIsUrl = rememberSaveable { mutableStateOf(initPlayThisIsUrl) }
    val textPlayRadioUrl = rememberSaveable { mutableStateOf(initPlayRadioUrl) }
    val textPlayMixUrl = rememberSaveable { mutableStateOf(initPlayMixUrl) }

    val requestDetailArtistOn = rememberSaveable { mutableStateOf(false) }
    val requestDetailPlaylistOn = rememberSaveable { mutableStateOf(false) }

    if (requestDetailArtistOn.value) {
        DialogRequestDetail(
            dialogOnState = requestDetailArtistOn,
            title = "Artist URL",
            message = "Please enter a valid URL for the current Artist!\n\nPlease copy it from Spotify -> artist profile -> Share -> Copy link."
        )
    }

    if (requestDetailPlaylistOn.value) {
        DialogRequestDetail(
            dialogOnState = requestDetailPlaylistOn,
            title = "Playlist URL",
            message = "Please enter a valid URL for the current Playlist!\n\nPlease copy it from Spotify -> playlist page -> Share -> Copy link."
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
                itemArtist = spotifyUtils.getArtistInfo(context, textArtistUrl.value, itemArtist, init=false)
                textName.value = itemArtist.name
                textGenres.value = itemArtist.genres.joinToString(", ")
                imageUrlState.value = itemArtist.imageUrl
            },
            onDismiss = {
                onDismiss()
            },
            onSave = {
                //CHECK & BUILD:
                //1) Validate Artist URL & Playlist URL:
                requestDetailArtistOn.value =
                    spotifyUtils.disambiguateSpotifyURL(textArtistUrl.value.replace(" ", "")) != "artist"
                if (textPlayThisIsUrl.value != "") {
                    requestDetailPlaylistOn.value =
                        spotifyUtils.disambiguateSpotifyURL(textPlayThisIsUrl.value.replace(" ", "")) != "playlist"
                }
                if (textPlayRadioUrl.value != "") {
                    requestDetailPlaylistOn.value =
                        spotifyUtils.disambiguateSpotifyURL(textPlayRadioUrl.value.replace(" ", "")) != "playlist"
                }
                if (textPlayMixUrl.value != "") {
                    requestDetailPlaylistOn.value =
                        spotifyUtils.disambiguateSpotifyURL(textPlayMixUrl.value.replace(" ", "")) != "playlist"
                }

                if (!requestDetailArtistOn.value && !requestDetailPlaylistOn.value && textName.value != "") {
                    //2) Update object:
                    val aliasesList = mutableListOf(utils.cleanString(textName.value).lowercase())
                    if (textAliases.value != "") {
                        for (alias in textAliases.value.split(",")) {
                            val temp = utils.cleanString(alias).lowercase()
                            if (temp != "" && !aliasesList.contains(temp)) {
                                aliasesList.add(temp)
                            }
                        }
                    }
                    itemArtist.name = textName.value.trim()   //utils.capitalizeWords(textName.value).trim()
                    itemArtist.aliases = aliasesList
                    itemArtist.imageUrl = imageUrlState.value
                    itemArtist.spotifyUrl = spotifyUtils.trimSpotifyUrl(textArtistUrl.value)
                    itemArtist.defaultPlay =
                        if (textPlayThisIsUrl.value == "") "artist" else playOptionsValToKeys[textDefaultPlay.value]!!
                    //PlayLinks:
                    val newPlayLinks = mutableMapOf(
                        "spotify_this_is" to PlayLink(
                            name = "",
                            owner = "Spotify",
                            spotifyUrl = ""
                        )
                    )
                    if (textPlayThisIsUrl.value.trim() != "") {
                        newPlayLinks["spotify_this_is"] = PlayLink(
                            name = "This is ${textName.value}",
                            owner = "Spotify",
                            spotifyUrl = spotifyUtils.trimSpotifyUrl(textPlayThisIsUrl.value)
                        )
                    }

                    if (textPlayRadioUrl.value.trim() != "") {
                        newPlayLinks["spotify_radio"] = PlayLink(
                            name = "${textName.value} Radio",
                            owner = "Spotify",
                            spotifyUrl = spotifyUtils.trimSpotifyUrl(textPlayRadioUrl.value)
                        )
                    }

                    if (textPlayMixUrl.value.trim() != "") {
                        newPlayLinks["spotify_mix"] = PlayLink(
                            name = "${textName.value} Mix",
                            owner = "Spotify",
                            spotifyUrl = spotifyUtils.trimSpotifyUrl(textPlayMixUrl.value)
                        )
                    }

                    itemArtist.playLinks = newPlayLinks

                    //3) Update / store to DB:
                    libUtils.storeArtist(context, itemArtist)

                    //4) End & close:
                    libraryMap.value = libUtils.refreshLibrary(filter)   //Refresh list
                    onDismiss()
                }
            }
        ) {
            //CONTENT:
            //ARTIST NAME:
            EditVocDynamicNameSection(
                textHeaderColor = vocColorSelectorLight(cat = filter),
                textFieldColors = getTextFieldColors(
                    colorLight = vocColorSelectorLight(cat = filter),
                    colorDark = vocColorSelector(cat = filter)
                ),
                filter = filter,
                textState = textName,
                subtitleState = textGenres,
                imageUrlState = imageUrlState,
                initActive = textName.value == "",
                showEditIcon = textName.value == ""
            )

            //ARTIST ALIASES:
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

            //ARTIST URL:
            EditVocDynamicField(
                modifier = Modifier
                    .fillMaxWidth(),
                textHeaderColor = vocColorSelectorLight(cat = filter),
                textFieldColors = getTextFieldColors(
                    colorLight = vocColorSelectorLight(cat = filter),
                    colorDark = vocColorSelector(cat = filter)
                ),
                title = "Spotify Profile Link",
                placeholder = "Paste Spotify link...",
                textState = textArtistUrl
            )

            //Section:
            EditVocSectionTitle(
                title = "Spotify \"This is\""
            )

            //SPOTIFY "THIS IS":
            EditVocDynamicField(
                modifier = Modifier
                    .fillMaxWidth(),
                textHeaderColor = vocColorSelectorLight(cat = filter),
                textFieldColors = getTextFieldColors(
                    colorLight = vocColorSelectorLight(cat = filter),
                    colorDark = vocColorSelector(cat = filter)
                ),
                title = "Spotify \"This is\" Playlist Link",
                placeholder = "Paste Spotify link...",
                textState = textPlayThisIsUrl
            )

            //DEFAULT PLAY: DROPDOWN:
            EditVocTitle(
                textHeaderColor = vocColorSelectorLight(cat = filter),
                title = "Play by default",
            )

            DropdownSpinner(
                context = context,
                parentOptions = playOptionsValToKeys.keys.toList(),
                init = initDefaultPlay,
                state = textDefaultPlay,
                focusColorLight = vocColorSelectorLight(cat = filter),
                focusColorDark = vocColorSelector(cat = filter)
            )

            //Section:
            EditVocSectionTitle(
                title = "Spotify Radio & Mix"
            )


            //SPOTIFY "RADIO":
            EditVocDynamicField(
                modifier = Modifier
                    .fillMaxWidth(),
                textHeaderColor = vocColorSelectorLight(cat = filter),
                textFieldColors = getTextFieldColors(
                    colorLight = vocColorSelectorLight(cat = filter),
                    colorDark = vocColorSelector(cat = filter)
                ),
                title = "Spotify \"Artist Radio\" Link",
                placeholder = "Paste Spotify link...",
                textState = textPlayRadioUrl
            )

            //SPOTIFY "MIX":
            EditVocDynamicField(
                modifier = Modifier
                    .fillMaxWidth(),
                textHeaderColor = vocColorSelectorLight(cat = filter),
                textFieldColors = getTextFieldColors(
                    colorLight = vocColorSelectorLight(cat = filter),
                    colorDark = vocColorSelector(cat = filter)
                ),
                title = "Spotify \"Artist Mix\" Link",
                placeholder = "Paste Spotify link...",
                textState = textPlayMixUrl
            )

        }
    }
}
