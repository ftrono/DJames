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
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.messLangFull
import com.ftrono.DJames.application.messLangCodes
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.database.Contact
import com.ftrono.DJames.database.ItemInfoView
import com.ftrono.DJames.database.PhoneSet
import com.ftrono.DJames.screen.VocabularyScreen
import com.ftrono.DJames.test_objects.testContacts
import com.ftrono.DJames.ui.DropdownSpinner
import com.ftrono.DJames.ui.getTextFieldColors
import com.ftrono.DJames.ui.vocColorSelector
import com.ftrono.DJames.ui.vocColorSelectorLight


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
    libraryMap: MutableState<Map<String, String>>,
    keyState: MutableState<String>,
    filter: String,
    preview: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }

    //Init:
    var itemContact = Contact()
    val key = keyState.value
    val checkedLang = remember { mutableStateOf(false) }

    //Recover info:
    if (key != "") {
        itemContact = if (preview) {
            testContacts[0]
        } else {
            libUtils.getContact(keyState.value)
        }
    }

    //Init aliases:
    val initAliases = itemContact.aliases.toMutableList()
    initAliases.removeAt(0)

    //Init default language:
    var initLanguage = itemContact.language
    if (initLanguage == "") {
        //No custom language:
        initLanguage = if (preview) "it" else prefs.messageLanguage
    } else {
        //There is a custom language:
        checkedLang.value = true
    }

    val phoneSets = itemContact.phoneSets
    val initPersPrefix = if (phoneSets["personal"] == null) "" else phoneSets["personal"]!!.prefix   //TODO: TEMP
    val initPersPhone = if (phoneSets["personal"] == null) "" else phoneSets["personal"]!!.phone   //TODO: TEMP

    //States:
    val textName = rememberSaveable { mutableStateOf(itemContact.name) }
    val textAliases = rememberSaveable { mutableStateOf(initAliases.joinToString(", ")) }
    val textLanguage = rememberSaveable { mutableStateOf(messLangFull[messLangCodes.indexOf(initLanguage)]) }
    val textDefaultPhone = rememberSaveable { mutableStateOf(itemContact.defaultPhone) }   //TODO
    val textPrefix = rememberSaveable { mutableStateOf(initPersPrefix) }
    val textPhone = rememberSaveable { mutableStateOf(initPersPhone) }

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

                if (!requestDetailOn.value && textName.value != "") {
                    //2) Update object:
                    val aliasesList = mutableListOf(textName.value.lowercase())
                    if (textAliases.value != "") {
                        for (alias in textAliases.value.split(",")) {
                            val temp = alias.lowercase().strip()
                            if (temp != "" && !aliasesList.contains(temp)) {
                                aliasesList.add(temp)
                            }
                        }
                    }
                    itemContact.name = utils.capitalizeWords(textName.value)
                    itemContact.aliases = aliasesList
                    //Language:
                    if (checkedLang.value) {
                        itemContact.language = messLangCodes[messLangFull.indexOf(textLanguage.value)]
                    } else {
                        itemContact.language =  ""
                    }
                    itemContact.defaultPhone = "personal"
                    phoneSets["personal"] = PhoneSet(
                        prefix = textPrefix.value,
                        phone = textPhone.value
                    )
                    itemContact.phoneSets = phoneSets

                    //3) Update / store to DB:
                    libUtils.storeContact(mContext, itemContact)

                    //4) End & close:
                    libraryMap.value = libUtils.refreshLibrary(filter)   //Refresh list
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

            //CONTACT ALIASES:
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

            //CONTACT PHONE:
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