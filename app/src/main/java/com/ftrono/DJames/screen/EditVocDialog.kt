package com.ftrono.DJames.screen

import android.content.Context
import android.telephony.PhoneNumberUtils
import android.util.Patterns
import android.webkit.URLUtil
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.compose.rememberNavController
import com.ftrono.DJames.R
import com.ftrono.DJames.application.messLangCaps
import com.ftrono.DJames.application.messLangCodes
import com.ftrono.DJames.application.playlistUrlIntro
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.vocabularySize
import com.ftrono.DJames.ui.theme.MyDJamesItem
import com.ftrono.DJames.utilities.Utilities
import com.google.gson.JsonObject


@Preview
@Preview(heightDp = 360, widthDp = 800)
@Composable
fun DialogEditPreview() {
    val navController = rememberNavController()
    VocabularyScreen(navController, "contact", MyDJamesItem.Playlists, editPreview=true, preview=true)
}

@Composable
fun DialogRequestDetail(mContext: Context, dialogOnState: MutableState<Boolean>, filter: String) {
    //REQUEST DETAIL DIALOG:
    if (dialogOnState.value) {
        AlertDialog(
            onDismissRequest = {
                //cancelable -> true
                dialogOnState.value = false
            },
            containerColor = colorResource(id = R.color.dark_grey),
            title = {
                Text(
                    text = if (filter == "contact") "Contact Phone Number" else "Playlist URL",
                    color = colorResource(id = R.color.light_grey)
                ) },
            text = {
                Text(
                    text = if (filter == "contact") {
                        "Please enter a valid phone number for the current Contact!\n\nPlease include the international prefix at the beginning (i.e. \"+39\", \"+44\", ...)."
                    } else {
                        "Please enter a valid URL for the current Playlist!\n\nPlease copy it from Spotify -> your playlist -> Share -> Copy link."
                    },   // and you'll lose your saved vocabulary & history
                    color = colorResource(id = R.color.mid_grey)
                ) },
            confirmButton = {
                Text(
                    modifier = Modifier
                        .clickable {
                            dialogOnState.value = false
                        },
                    text = "Ok",
                    fontSize = 14.sp,
                    color = colorResource(id = R.color.colorAccentLight)
                )
            }
        )
    }
}


@Composable
fun DialogEditVocabulary(mContext: Context, dialogOnState: MutableState<Boolean>, filter: String, vocItems: JsonObject, key: String = "", preview: Boolean = false) {
    //Init:
    var initName = key
    var initPlayUrl = ""
    var initLanguage = if (preview) "it" else messLangCaps[messLangCodes.indexOf(prefs.messageLanguage)]
    var initPrefix = "+39"
    var initPhone = ""

    //Recover info:
    if (key != "") {
        val prevDetails = vocItems.get(key).asJsonObject
        if (filter == "playlist") {
            initPlayUrl = prevDetails.get("playlist_URL").asString
        } else if (filter == "contact") {
            if (prevDetails.has("contact_language")) {
                initLanguage = prevDetails.get("contact_language").asString.lowercase()
            }
            initPrefix = prevDetails.get("prefix").asString
            initPhone = prevDetails.get("phone").asString
        }
    }

    //States:
    var textName by rememberSaveable { mutableStateOf(initName) }
    var textPlayURL by rememberSaveable { mutableStateOf(initPlayUrl) }
    var textPrefix by rememberSaveable { mutableStateOf(initPrefix) }
    var textPhone by rememberSaveable { mutableStateOf(initPhone) }
    var textLanguageState = rememberSaveable { mutableStateOf(initLanguage) }

    val focusRequester = remember { FocusRequester() }

    val requestDetailOn = rememberSaveable { mutableStateOf(false) }
    if (requestDetailOn.value) {
        DialogRequestDetail(mContext, requestDetailOn, filter)
    }

    //TextField colors:
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colorResource(id = R.color.colorAccentLight),
        unfocusedBorderColor = colorResource(id = R.color.mid_grey),
        focusedTextColor = colorResource(id = R.color.light_grey),
        unfocusedTextColor = colorResource(id = R.color.light_grey),
        focusedPlaceholderColor = colorResource(id = R.color.mid_grey),
        unfocusedPlaceholderColor = colorResource(id = R.color.mid_grey),
        cursorColor = colorResource(id = R.color.colorAccentLight),
        selectionColors = TextSelectionColors(
            handleColor = colorResource(id = R.color.colorAccent),
            backgroundColor = colorResource(id = R.color.transparent_green)
        )
    )

    //EDIT DIALOG:
    Dialog(
        onDismissRequest = {
            //cancelable -> true
            dialogOnState.value = false
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current

        //CONTAINER:
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clickable {
                    focusManager.clearFocus()
                },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors (
                containerColor = colorResource(id = R.color.dark_grey)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .wrapContentWidth()
                    .wrapContentHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
            ) {
                //TITLE:
                Text(
                    text = "✏️  ${filter.replaceFirstChar { it.uppercase() }}",
                    modifier = Modifier.padding(8.dp),
                    color = colorResource(id = R.color.light_grey),
                    textAlign = TextAlign.Start,
                    fontSize = 22.sp
                )

                //COMMON: TEXT FIELD 1:
                Text(
                    text = "Name",
                    modifier = Modifier.padding(top=12.dp),
                    color = colorResource(id = R.color.colorAccentLight),
                    textAlign = TextAlign.Start,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                OutlinedTextField(
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 20.dp)
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .focusRequester(focusRequester),
                    colors = textFieldColors,
                    value = textName,
                    onValueChange = { newText ->
                        textName = newText.trimStart { it == '0' }
                    },
                    textStyle = TextStyle(
                        fontSize = 16.sp
                    ),
                    maxLines = 1,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    ),
                    placeholder = {
                        Text(
                            text = "Write name here...",
                            fontSize = 16.sp,
                            fontStyle = FontStyle.Italic
                        )
                    },
                )

                if (filter == "playlist") {
                    //PLAYLIST: TEXT FIELD 2:
                    Text(
                        text = "Playlist URL",
                        color = colorResource(id = R.color.colorAccentLight),
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    OutlinedTextField(
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 20.dp)
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .focusRequester(focusRequester),
                        colors = textFieldColors,
                        value = textPlayURL,
                        onValueChange = { newText ->
                            textPlayURL = newText.trimStart { it == '0' }
                        },
                        textStyle = TextStyle(
                            fontSize = 16.sp
                        ),
                        singleLine = true,
                        maxLines = 1,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                        ),
                        placeholder = {
                            Text(
                                text = "Paste here the Spotify link...",
                                fontSize = 16.sp,
                                fontStyle = FontStyle.Italic
                            )
                        },
                    )

                } else if (filter == "contact") {
                    //CONTACTS: TEXT FIELD 2:
                    Text(
                        text = "Preferred messaging language",
                        color = colorResource(id = R.color.colorAccentLight),
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    DropdownSpinner(mContext, messLangCaps, init=initLanguage, state=textLanguageState)

                    //CONTACTS: TEXT FIELD 3:
                    Text(
                        text = "Main phone",
                        color = colorResource(id = R.color.colorAccentLight),
                        textAlign = TextAlign.Start,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        //PREFIX:
                        OutlinedTextField(
                            modifier = Modifier
                                .padding(top = 8.dp, bottom = 20.dp)
                                .width(60.dp)
                                .wrapContentHeight(),
                            colors = textFieldColors,
                            value = textPrefix,
                            onValueChange = { newText ->
                                textPrefix = newText.trimStart { it == '0' }
                            },
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            singleLine = true,
                            maxLines = 1,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Next
                            ),
                            placeholder = {
                                Text(
                                    text = "+39",
                                    fontSize = 16.sp,
                                    fontStyle = FontStyle.Italic
                                )
                            },
                        )
                        //SUFFIX:
                        OutlinedTextField(
                            modifier = Modifier
                                .padding(top = 8.dp, bottom = 20.dp)
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .focusRequester(focusRequester),
                            colors = textFieldColors,
                            value = textPhone,
                            onValueChange = { newText ->
                                textPhone = newText.trimStart { it == '0' }
                            },
                            textStyle = TextStyle(
                                fontSize = 16.sp
                            ),
                            singleLine = true,
                            maxLines = 1,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }
                            ),
                            placeholder = {
                                Text(
                                    text = "Phone number...",
                                    fontSize = 16.sp,
                                    fontStyle = FontStyle.Italic
                                )
                            },
                        )
                    }
                }
                //BUTTONS ROW:
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    //CANCEL:
                    Text(
                        modifier = Modifier
                            .padding(top = 8.dp, end = 20.dp)
                            .clickable {
                                dialogOnState.value = false
                            },
                        color = colorResource(id = R.color.colorAccentLight),
                        text = "Cancel",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    //SAVE:
                    Text(
                        modifier = Modifier
                            .padding(top = 8.dp, end = 8.dp)
                            .clickable {
                                //CHECK & UPDATE:
                                var newDetails = JsonObject()
                                //Get new info:
                                if (filter == "playlist") {
                                    newDetails.addProperty("playlist_URL", textPlayURL.replace(" ", ""))
                                } else if (filter == "contact") {
                                    if (textLanguageState.value != initLanguage) {
                                        newDetails.addProperty("contact_language", textLanguageState.value)
                                    }
                                    newDetails.addProperty("prefix", textPrefix.replace(" ", ""))
                                    newDetails.addProperty("phone", textPhone.replace(" ", ""))
                                }
                                //Edit:
                                requestDetailOn.value = editVocItemAndShow(mContext=mContext, prevText=initName, newText=textName.lowercase(), newDetails=newDetails, filter=filter, vocItems=vocItems)
                                if (!requestDetailOn.value) {
                                    //CLOSE THE DIALOG:
                                    dialogOnState.value = false
                                }
                            },
                        color = colorResource(id = R.color.colorAccentLight),
                        text = "Save",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


//Edit Voc item:
fun editVocItemAndShow(mContext: Context, prevText: String, newText: String, newDetails: JsonObject, filter: String, vocItems: JsonObject): Boolean {
    //Return true -> Show DialogRequestDetail;
    //Return false -> Don't show DialogRequestDetail.
    if (newText != "") {
        if (filter == "playlist") {
            var newURL = newDetails.get("playlist_URL").asString
            var urlTest = URLUtil.isValidUrl(newURL) && Patterns.WEB_URL.matcher(newURL).matches()
            if (!urlTest || !newURL.contains(playlistUrlIntro)) {
                //Request enter valid URL:
                return true
            } else {
                newURL = newURL.split("?")[0]
                newDetails.addProperty("playlist_URL", newURL)
            }
        } else if (filter == "contact") {
            var newPrefix = newDetails.get("prefix").asString
            var newPhone = newDetails.get("phone").asString
            var newLang = newDetails.get("contact_language").asString   //TODO
            var phoneTest = PhoneNumberUtils.isGlobalPhoneNumber(newPhone)
            //Request valid detail (i.e. no URL, no phone number, no international prefix in phone number):
            if (!phoneTest || (!newPrefix.contains("+") && newPrefix.length != 3) || (newPhone.length != 10 && newPhone.length != 11)) {
                //(Phone numbers length is 10 digits (Italy) or 11 digits (UK), + international prefix (3 digits))
                return true
            }
        }
        var prevDetails = JsonObject()
        if (vocItems.has(prevText)) {
            prevDetails = vocItems.get(prevText).asJsonObject
        }
        //Save to file:
        if (newText != prevText || newDetails != prevDetails) {
            val utils = Utilities()
            var ret = utils.editVocFile(prevText=prevText, newText=newText, newDetails=newDetails)
            if (ret == 0) {
                Toast.makeText(mContext, "Saved!", Toast.LENGTH_LONG).show()
                vocabularySize.postValue(vocabularySize.value!! + 1)   //Refresh list
                if (prevText != "") {
                    vocabularySize.postValue(vocabularySize.value!! - 1)   //Refresh list
                }
            } else {
                Toast.makeText(mContext, "ERROR: Vocabulary not updated!", Toast.LENGTH_LONG).show()
            }
        }
    }
    return false
}
