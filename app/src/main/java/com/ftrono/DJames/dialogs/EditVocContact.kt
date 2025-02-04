package com.ftrono.DJames.dialogs

import android.content.Context
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.ftrono.DJames.R
import com.ftrono.DJames.application.messLangFull
import com.ftrono.DJames.application.messLangCodes
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.screen.VocabularyScreen
import com.ftrono.DJames.screen.getVocItem
import com.ftrono.DJames.screen.getVocKeys
import com.ftrono.DJames.ui.DropdownSpinner
import com.ftrono.DJames.ui.getTextFieldColors
import com.ftrono.DJames.ui.vocColorSelector
import com.ftrono.DJames.ui.vocColorSelectorLight
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
    vocKeys: MutableState<List<String>>,
    keyState: MutableState<String>,
    filter: String,
    preview: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }

    //Prev:
    val prevKey = keyState.value
    var prevItem = JsonObject()

    //Init:
    var key = keyState.value
    var initName = key
    var initLanguage = if (preview) "it" else prefs.messageLanguage
    var initMain = JsonObject()
    var initPrefix = "+39"
    var initPhone = ""
    var checkedLang = remember { mutableStateOf(false) }

    //Recover info:
    if (key != "") {
        //TODO: extract useful data:
        prevItem = getVocItem(mContext, filter, keyState.value, preview)
        initName = prevItem.get("name").asString
        initLanguage = prevItem.get("language").asString
        if (initLanguage == "") {
            //No custom language:
            initLanguage = if (preview) "it" else prefs.messageLanguage
        } else {
            //There is a custom language:
            checkedLang.value = true
        }
        initMain = prevItem.get("main").asJsonObject
        initPrefix = initMain.get("prefix").asString
        initPhone = initMain.get("phone").asString
    }

    //States:
    var textName = rememberSaveable { mutableStateOf(initName) }
    var textPrefix = rememberSaveable { mutableStateOf(initPrefix) }
    var textPhone = rememberSaveable { mutableStateOf(initPhone) }
    var textLanguage = rememberSaveable { mutableStateOf(messLangFull[messLangCodes.indexOf(initLanguage)]) }

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
                //1) Validate Prefix:
                requestDetailOn.value = !utils.isGlobalPhone(prefix = textPrefix.value, phone = textPhone.value)

                if (!requestDetailOn.value) {
                    //2) Build object:
                    var newItem = JsonObject()
                    newItem.addProperty("name", textName.value)
                    //Language:
                    if (checkedLang.value) {
                        newItem.addProperty("language", messLangCodes[messLangFull.indexOf(textLanguage.value)])
                    } else {
                        newItem.addProperty("language", "")
                    }
                    //Main:
                    val main = JsonObject()
                    main.addProperty("prefix", textPrefix.value)
                    main.addProperty("phone", textPhone.value)
                    //Container:
                    newItem.add("main", main)

                    //3) Store:
                    if (newItem != prevItem) {
                        utils.writeLibraryItem(
                            context = mContext,
                            filter = filter,
                            key = textName.value,
                            item = newItem,
                            prevKey = prevKey
                        )
                    }

                    //4) End & close:
                    vocKeys.value = getVocKeys(mContext, filter)   //Refresh list
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
                        .padding(bottom = if (checkedLang.value) 0.dp else 6.dp)
                        .offset(x = -(12.dp)),
                    checked = checkedLang.value,
                    onCheckedChange = { checkedLang.value = it },
                    colors = checkBoxColors
                )
                Text(
                    modifier = Modifier
                        .padding(bottom = if (checkedLang.value) 0.dp else 6.dp)
                        .offset(x = -(12.dp))
                        .clickable { checkedLang.value = !checkedLang.value },
                    text = if (checkedLang.value) {
                        "Set custom messaging language"
                    } else {
                        "Set custom messaging language\n(default: ${messLangFull[messLangCodes.indexOf(initLanguage)]})"
                    },
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    color = if (checkedLang.value) colorResource(id = R.color.light_grey) else colorResource(
                        id = R.color.mid_grey
                    )
                )
            }
            if (checkedLang.value) {
                //CONTACTS: DROPDOWN:
                DropdownSpinner(
                    mContext = mContext,
                    parentOptions = messLangFull,
                    init = messLangFull[messLangCodes.indexOf(initLanguage)],
                    state = textLanguage,
                    focusColorLight = vocColorSelectorLight(cat = filter),
                    focusColorDark = vocColorSelector(cat = filter)
                )
            } else {
                textLanguage.value = ""
            }
        }
    }
}