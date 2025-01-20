package com.ftrono.DJames.dialogs

import android.content.Context
import android.telephony.PhoneNumberUtils
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
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
import com.ftrono.DJames.R
import com.ftrono.DJames.application.messLangCaps
import com.ftrono.DJames.application.messLangCodes
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.screen.VocabularyScreen
import com.ftrono.DJames.screen.getVocKeys
import com.ftrono.DJames.screen.updateVocabulary
import com.ftrono.DJames.ui.DropdownSpinner
import com.ftrono.DJames.ui.getTextFieldColors
import com.ftrono.DJames.ui.vocColorSelector
import com.ftrono.DJames.ui.vocColorSelectorLight
import com.ftrono.DJames.utilities.Utilities
import com.google.gson.JsonObject


@Preview
@Preview(heightDp = 360, widthDp = 800)
@Composable
fun DialogEditContactPreview() {
    VocabularyScreen(editPreview="contact", preview=true)
}


@Composable
fun EditVocContact(
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
    var prevLangCode = ""
    var initLanguageCaps = if (preview) "Italian" else ""
    var defaultLanguageCaps = if (preview) "Italian" else messLangCaps[messLangCodes.indexOf(prefs.messageLanguage)]
    var initPrefix = "+39"
    var initPhone = ""

    var checkedLang by remember { mutableStateOf(false) }

    //Recover info:
    if (key != "") {
        val prevDetails = vocItems.get(key).asJsonObject
        if (prevDetails.has("contact_language")) {
            prevLangCode = prevDetails.get("contact_language").asString
            initLanguageCaps = messLangCaps[messLangCodes.indexOf(prevLangCode)]
            checkedLang = true
        }
        initPrefix = prevDetails.get("prefix").asString
        initPhone = prevDetails.get("phone").asString
    }

    //States:
    var textName = rememberSaveable { mutableStateOf(initName) }
    var textPrefix = rememberSaveable { mutableStateOf(initPrefix) }
    var textPhone = rememberSaveable { mutableStateOf(initPhone) }
    var textLanguageState = rememberSaveable { mutableStateOf(initLanguageCaps) }

    val requestDetailOn = rememberSaveable { mutableStateOf(false) }
    if (requestDetailOn.value) {
        DialogRequestDetail(
            dialogOnState = requestDetailOn,
            title = "Contact Phone Number",
            message = "Please enter a valid phone number for the current Contact!\n\nPlease include the international prefix at the beginning (i.e. \"+39\", \"+44\", ...)."
        )
    }

    val checkBoxColors = CheckboxDefaults.colors(
        checkedColor = vocColorSelectorLight(cat = filter),
        uncheckedColor = colorResource(id = R.color.mid_grey),
        checkmarkColor = colorResource(id = R.color.dark_grey_background)
    )

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
            if (textLanguageState.value != "") {
                val newLangCode =
                    messLangCodes[messLangCaps.indexOf(textLanguageState.value)]
                newDetails.addProperty("contact_language", newLangCode)
            }
            newDetails.addProperty("prefix", textPrefix.value.replace(" ", ""))
            newDetails.addProperty("phone", textPhone.value.replace(" ", ""))

            //Edit:
            requestDetailOn.value = validateEditContact(
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
        //CONTACT NAME:
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

        //CONTACTS: TEXT FIELD 2:
        Text(
            text = "Main phone",
            color = vocColorSelectorLight(cat = filter),
            textAlign = TextAlign.Start,
            fontSize = 16.sp,
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
                    .padding(top = 8.dp, bottom = 10.dp)
                    .width(60.dp)
                    .wrapContentHeight(),
                colors = getTextFieldColors(
                    colorLight = vocColorSelectorLight(cat = filter),
                    colorDark = vocColorSelector(cat = filter)
                ),
                value = textPrefix.value,
                onValueChange = { newText ->
                    textPrefix.value = newText.trimStart { it == '0' }
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
                    .padding(top = 8.dp, bottom = 10.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .focusRequester(focusRequester),
                colors = getTextFieldColors(
                    colorLight = vocColorSelectorLight(cat = filter),
                    colorDark = vocColorSelector(cat = filter)
                ),
                value = textPhone.value,
                onValueChange = { newText ->
                    textPhone.value = newText.trimStart { it == '0' }
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

        //CONTACTS: CHECKBOX:
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                modifier = Modifier
                    .padding(bottom = if (checkedLang) 0.dp else 6.dp)
                    .offset(x = -(12.dp)),
                checked = checkedLang,
                onCheckedChange = { checkedLang = it },
                colors = checkBoxColors
            )
            Text(
                modifier = Modifier
                    .padding(bottom = if (checkedLang) 0.dp else 6.dp)
                    .offset(x = -(12.dp))
                    .clickable { checkedLang = !checkedLang },
                text = if (checkedLang) {
                    "Set custom messaging language"
                } else {
                    "Set custom messaging language\n(default: ${defaultLanguageCaps})"
                },
                fontSize = 14.sp,
                lineHeight = 16.sp,
                color = if (checkedLang) colorResource(id = R.color.light_grey) else colorResource(
                    id = R.color.mid_grey
                )
            )
        }
        if (checkedLang) {
            //CONTACTS: DROPDOWN:
            val initCaps =
                if (prevLangCode == "") defaultLanguageCaps else messLangCaps[messLangCodes.indexOf(
                    prevLangCode
                )]
            textLanguageState.value = initCaps
            DropdownSpinner(
                mContext = mContext,
                parentOptions = messLangCaps,
                init = initCaps,
                state = textLanguageState,
                focusColorLight = vocColorSelectorLight(cat = filter),
                focusColorDark = vocColorSelector(cat = filter)
            )
        } else {
            textLanguageState.value = ""
        }
    }
}


//Validate Edit Contact:
fun validateEditContact(
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
        var newPrefix = newDetails.get("prefix").asString
        var newPhone = newDetails.get("phone").asString
        var newLang = ""
        try {
            newLang = newDetails.get("contact_language").asString   //TODO
        } catch (e: Exception) {
            newLang = ""
        }
        var phoneTest = PhoneNumberUtils.isGlobalPhoneNumber(newPhone)
        //Request valid detail (i.e. no URL, no phone number, no international prefix in phone number):
        if (!phoneTest || (!newPrefix.contains("+") && newPrefix.length != 3) || (newPhone.length != 10 && newPhone.length != 11)) {
            //(Phone numbers length is 10 digits (Italy) or 11 digits (UK), + international prefix (3 digits))
            return true
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
