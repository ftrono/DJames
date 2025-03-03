package com.ftrono.DJames.dialogs

import android.content.Context
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.database.Artist
import com.ftrono.DJames.database.PlayLink
import com.ftrono.DJames.screen.VocabularyScreen
import com.ftrono.DJames.database.ItemInfoView
import com.ftrono.DJames.test_objects.testArtists
import com.ftrono.DJames.ui.DropdownSpinner
import com.ftrono.DJames.ui.getTextFieldColors
import com.ftrono.DJames.ui.vocColorSelector
import com.ftrono.DJames.ui.vocColorSelectorLight


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
    var itemArtist = Artist()
    val key = keyState.value

    //Recover info:
    if (key != "") {
        itemArtist = if (preview) {
            testArtists[0]
        } else {
            libUtils.getArtist(keyState.value)
        }
    }

    //Init aliases:
    val initAliases = itemArtist.aliases.toMutableList()
    initAliases.removeAt(0)
    val playLinks = itemArtist.playLinks
    val initPlayThisIsUrl = if (playLinks["spotify_this_is"] == null) "" else playLinks["spotify_this_is"]!!.spotifyUrl   //TODO: TEMP

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

                if (!requestDetailArtistOn.value && !requestDetailPlaylistOn.value && textName.value != "") {
                    //2) Update object:
                    val thisIsName = "this is ${textName.value}"
                    val aliasesList = mutableListOf(textName.value.lowercase())
                    if (textAliases.value != "") {
                        for (alias in textAliases.value.split(",")) {
                            val temp = alias.lowercase().strip()
                            if (temp != "" && !aliasesList.contains(temp)) {
                                aliasesList.add(temp)
                            }
                        }
                    }
                    itemArtist.name = utils.capitalizeWords(textName.value)
                    itemArtist.aliases = aliasesList
                    itemArtist.spotifyUrl = textArtistUrl.value.replace(" ", "").split("?")[0]
                    itemArtist.defaultPlay = if (textPlayThisIsUrl.value == "") "artist" else playOptionsValToKeys[textDefaultPlay.value]!!
                    //TODO: add more playlists:
                    playLinks["spotify_this_is"] = PlayLink(
                        name = thisIsName,
                        owner = "Spotify",
                        spotifyUrl = textPlayThisIsUrl.value.replace(" ", "").split("?")[0]
                    )
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

            //ARTIST ALIASES:
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
                title = "Aliases (separate with commas)",
                placeholder = "Write $filter name...",
                textState = textAliases
            )

            //ARTIST URL:
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
                title = "Spotify: Artist URL",
                placeholder = "Paste here the Spotify link...",
                textState = textArtistUrl
            )

            //PLAYLIST URL:
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
                title = "Spotify: 'This is' playlist URL",
                placeholder = "Paste here the Spotify link...",
                textState = textPlayThisIsUrl
            )

            //DEFAULT PLAY: DROPDOWN:
            Text(
                text = "Play by default",
                color = vocColorSelectorLight(cat = filter),
                textAlign = TextAlign.Start,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            DropdownSpinner(
                mContext = mContext,
                parentOptions = playOptionsValToKeys.keys.toList(),
                init = initDefaultPlay,
                state = textDefaultPlay,
                focusColorLight = vocColorSelectorLight(cat = filter),
                focusColorDark = vocColorSelector(cat = filter)
            )

        }
    }
}
