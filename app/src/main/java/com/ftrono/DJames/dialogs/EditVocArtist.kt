package com.ftrono.DJames.dialogs

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ftrono.DJames.R
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.database.Artist
import com.ftrono.DJames.database.PlayLink
import com.ftrono.DJames.screen.VocabularyScreen
import com.ftrono.DJames.test_objects.testArtists
import com.ftrono.DJames.ui.DropdownSpinner
import com.ftrono.DJames.ui.SectionTitle
import com.ftrono.DJames.ui.getTextFieldColors
import com.ftrono.DJames.ui.vocColorSelector
import com.ftrono.DJames.ui.vocColorSelectorLight
import com.ftrono.DJames.ui.vocIconSelector


@Preview
@Preview(heightDp = 360, widthDp = 800)
@Composable
fun DialogEditArtistPreview() {
    VocabularyScreen(editPreview="artist", preview=true)
}


@Composable
fun EditVocArtist(
    mContext: Context,
    dialogOnState: MutableState<Boolean>,
    libraryMap: MutableState<Map<String, String>>,
    keyState: MutableState<String>,
    filter: String,
    preview: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }

    //Init:
    val key = keyState.value

    //Pre-populate:
    // val itemArtist = Artist()
    val itemArtist = if (preview) {
        testArtists[0]
    } else if (key != "") {
        libUtils.getArtist(keyState.value)
    } else {
        Artist()
    }

    //Init aliases:
    val initAliases = itemArtist.aliases.toMutableList()
    initAliases.removeAt(0)
    val playLinks = itemArtist.playLinks
    val initPlayThisIsUrl = if (playLinks["spotify_this_is"] == null) "" else playLinks["spotify_this_is"]!!.spotifyUrl   //TODO: TEMP
    val initPlayRadioUrl = if (playLinks["spotify_radio"] == null) "" else playLinks["spotify_radio"]!!.spotifyUrl
    val initPlayMixUrl = if (playLinks["spotify_mix"] == null) "" else playLinks["spotify_mix"]!!.spotifyUrl
    val initPlayCustomUrl = if (playLinks["custom"] == null) "" else playLinks["custom"]!!.spotifyUrl

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
    val textAliases = rememberSaveable { mutableStateOf(initAliases.joinToString(", ")) }
    val textDefaultPlay = rememberSaveable { mutableStateOf(initDefaultPlay) }
    val textArtistUrl = rememberSaveable { mutableStateOf(itemArtist.spotifyUrl) }
    val textPlayThisIsUrl = rememberSaveable { mutableStateOf(initPlayThisIsUrl) }
    val textPlayRadioUrl = rememberSaveable { mutableStateOf(initPlayRadioUrl) }
    val textPlayMixUrl = rememberSaveable { mutableStateOf(initPlayMixUrl) }
    val textPlayCustomUrl = rememberSaveable { mutableStateOf(initPlayCustomUrl) }

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
                //1) Validate Artist URL & Playlist URL:
                requestDetailArtistOn.value = !utils.isArtistUrl(textArtistUrl.value.replace(" ", ""))
                if (textPlayThisIsUrl.value != "") {
                    requestDetailPlaylistOn.value = !utils.isPlaylistUrl(textPlayThisIsUrl.value.replace(" ", ""))
                }
                if (textPlayRadioUrl.value != "") {
                    requestDetailPlaylistOn.value = !utils.isPlaylistUrl(textPlayRadioUrl.value.replace(" ", ""))
                }
                if (textPlayMixUrl.value != "") {
                    requestDetailPlaylistOn.value = !utils.isPlaylistUrl(textPlayMixUrl.value.replace(" ", ""))
                }
                if (textPlayCustomUrl.value != "") {
                    requestDetailPlaylistOn.value = !utils.isPlaylistUrl(textPlayCustomUrl.value.replace(" ", ""))
                }

                if (!requestDetailArtistOn.value && !requestDetailPlaylistOn.value && textName.value != "") {
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
                    itemArtist.name = utils.capitalizeWords(textName.value).trim()
                    itemArtist.aliases = aliasesList
                    itemArtist.spotifyUrl = textArtistUrl.value.replace(" ", "").split("?")[0]
                    itemArtist.defaultPlay = if (textPlayThisIsUrl.value == "") "artist" else playOptionsValToKeys[textDefaultPlay.value]!!
                    //PlayLinks:
                    if (textPlayThisIsUrl.value.trim() != "") {
                        playLinks["spotify_this_is"] = PlayLink(
                            name = "This is ${textName.value}",
                            owner = "Spotify",
                            spotifyUrl = textPlayThisIsUrl.value.replace(" ", "").split("?")[0]
                        )
                    }

                    if (textPlayRadioUrl.value.trim() != "") {
                        playLinks["spotify_radio"] = PlayLink(
                            name = "${textName.value} Radio",
                            owner = "Spotify",
                            spotifyUrl = textPlayRadioUrl.value.replace(" ", "").split("?")[0]
                        )
                    }

                    if (textPlayMixUrl.value.trim() != "") {
                        playLinks["spotify_mix"] = PlayLink(
                            name = "${textName.value} Mix",
                            owner = "Spotify",
                            spotifyUrl = textPlayMixUrl.value.replace(" ", "").split("?")[0].trim()
                        )
                    }

                    if (textPlayCustomUrl.value.trim() != "") {
                        playLinks["custom"] = PlayLink(
                            name = "${textName.value} Custom",
                            owner = "",
                            spotifyUrl = textPlayCustomUrl.value.replace(" ", "")
                                .split("?")[0].trim()
                        )
                    }

                    itemArtist.playLinks = playLinks

                    //3) Update / store to DB:
                    libUtils.storeArtist(mContext, itemArtist)

                    //4) End & close:
                    libraryMap.value = libUtils.refreshLibrary(filter)   //Refresh list
                    dialogOnState.value = false
                    keyState.value = ""
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
                initActive = textName.value == ""
            )

            //ARTIST ALIASES:
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

            //Section:
            SectionTitle(
                modifier = Modifier
                    .padding(top=4.dp, bottom=12.dp),
                title = "Main links",
                signColor = vocColorSelector(cat = filter),
                iconPainter = painterResource(id = R.drawable.logo_spotify)
            )

            //ARTIST URL:
            EditVocDynamicField(
                textHeaderColor = vocColorSelectorLight(cat = filter),
                textFieldColors = getTextFieldColors(
                    colorLight = vocColorSelectorLight(cat = filter),
                    colorDark = vocColorSelector(cat = filter)
                ),
                title = "Spotify Profile Link",
                placeholder = "Paste Spotify link...",
                textState = textArtistUrl
            )

            //SPOTIFY "THIS IS":
            EditVocDynamicField(
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
                mContext = mContext,
                parentOptions = playOptionsValToKeys.keys.toList(),
                init = initDefaultPlay,
                state = textDefaultPlay,
                focusColorLight = vocColorSelectorLight(cat = filter),
                focusColorDark = vocColorSelector(cat = filter)
            )

            //Section:
            SectionTitle(
                modifier = Modifier
                    .padding(top=4.dp, bottom=12.dp),
                title = "Extra links",
                signColor = vocColorSelector(cat = filter),
                iconPainter = painterResource(id = R.drawable.sign_headphones)
            )

            //SPOTIFY "RADIO":
            EditVocDynamicField(
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
                textHeaderColor = vocColorSelectorLight(cat = filter),
                textFieldColors = getTextFieldColors(
                    colorLight = vocColorSelectorLight(cat = filter),
                    colorDark = vocColorSelector(cat = filter)
                ),
                title = "Spotify \"Artist Mix\" Link",
                placeholder = "Paste Spotify link...",
                textState = textPlayMixUrl
            )

            //CUSTOM PLAYLIST:
            EditVocDynamicField(
                textHeaderColor = vocColorSelectorLight(cat = filter),
                textFieldColors = getTextFieldColors(
                    colorLight = vocColorSelectorLight(cat = filter),
                    colorDark = vocColorSelector(cat = filter)
                ),
                title = "Custom Playlist Link",
                placeholder = "Paste Spotify link...",
                textState = textPlayCustomUrl
            )

        }
    }
}
