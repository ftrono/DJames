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
import com.ftrono.DJames.application.messLangFull
import com.ftrono.DJames.application.messLangCodes
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.database.Contact
import com.ftrono.DJames.be.database.PhoneSet
import com.ftrono.DJames.application.screens.VocabularyScreen
import com.ftrono.DJames.be.samples.testContacts
import com.ftrono.DJames.ui.components.CustomCheckbox
import com.ftrono.DJames.ui.components.DropdownSpinner
import com.ftrono.DJames.ui.dialogs.DialogRequestDetail
import com.ftrono.DJames.ui.components.EditPhoneDynamicField
import com.ftrono.DJames.ui.dialogs.EditVocDialog
import com.ftrono.DJames.ui.components.EditVocDynamicField
import com.ftrono.DJames.ui.components.EditVocDynamicNameSection
import com.ftrono.DJames.ui.selectors.getTextFieldColors
import com.ftrono.DJames.ui.selectors.vocColorSelector
import com.ftrono.DJames.ui.selectors.vocColorSelectorLight
import com.ftrono.DJames.ui.selectors.vocIconSelector


@Preview
@Preview(heightDp = 360, widthDp = 800)
@Composable
fun DialogEditContactPreview() {
    VocabularyScreen(editPreview="contact", preview=true)
}


@Composable
fun EditVocContact(
    context: Context,
    libraryMap: MutableState<Map<String, String>>,
    keyState: MutableState<String>,
    filter: String,
    onDismiss: () -> Unit = {},
    preview: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }

    //Init:
    val key = keyState.value

    //Pre-populate:
    // val itemContact = Contact()
    val itemContact = if (preview) {
        testContacts[0]
    } else if (key != "") {
        libUtils.getContact(keyState.value)
    } else {
        Contact()
    }
    val checkedLang = remember { mutableStateOf(false) }

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
    val imageUrlState = rememberSaveable { mutableStateOf("") }
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

    //EDIT DIALOG:
    Dialog(
        onDismissRequest = {
            onDismiss()
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
            title = filter,
            headerColor = vocColorSelectorLight(cat = filter),
            headerPainter = vocIconSelector(cat = filter),
            showRefresh = false,
            onDismiss = {
                onDismiss()
            },
            onSave = {
                //CHECK & BUILD:
                //1) Validate Prefix:
                requestDetailOn.value = !utils.isGlobalPhone(
                    prefix = textPrefix.value.trim(),
                    phone = textPhone.value.trim()
                )

                if (!requestDetailOn.value && textName.value != "") {
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
                    itemContact.name = utils.capitalizeWords(textName.value).trim()
                    itemContact.aliases = aliasesList
                    //Language:
                    if (checkedLang.value) {
                        itemContact.language =
                            messLangCodes[messLangFull.indexOf(textLanguage.value)]
                    } else {
                        itemContact.language = ""
                    }
                    itemContact.defaultPhone = "personal"
                    phoneSets["personal"] = PhoneSet(
                        prefix = textPrefix.value.trim(),
                        phone = textPhone.value.trim()
                    )
                    itemContact.phoneSets = phoneSets

                    //3) Update / store to DB:
                    libUtils.storeContact(context, itemContact)

                    //4) End & close:
                    libraryMap.value = libUtils.refreshLibrary(filter)   //Refresh list
                    onDismiss()
                }
            }
        ) {
            //CONTACT NAME:
            EditVocDynamicNameSection(
                textHeaderColor = vocColorSelectorLight(cat = filter),
                textFieldColors = getTextFieldColors(
                    colorLight = vocColorSelectorLight(cat = filter),
                    colorDark = vocColorSelector(cat = filter)
                ),
                filter = filter,
                textState = textName,
                imageUrlState = imageUrlState,
                initActive = textName.value == ""
            )

            //CONTACT ALIASES:
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

            //CONTACT PHONE:
            EditPhoneDynamicField(
                textHeaderColor = vocColorSelectorLight(cat = filter),
                textFieldColors = getTextFieldColors(
                    colorLight = vocColorSelectorLight(cat = filter),
                    colorDark = vocColorSelector(cat = filter)
                ),
                title = "Main phone",
                textPrefix = textPrefix,
                textPhone = textPhone
            )

            //CONTACTS: CHECKBOX:
            CustomCheckbox(
                modifier = Modifier
                    .padding(bottom = if (checkedLang.value) 0.dp else 6.dp),
                checkedState = checkedLang,
                checkedColor = vocColorSelectorLight(cat = filter),
                textColor = if (checkedLang.value) colorResource(id = R.color.light_grey) else colorResource(
                    id = R.color.mid_grey
                ),
                text = if (checkedLang.value) {
                    "Set custom messaging language"
                } else {
                    "Set custom messaging language\n(default: ${
                        messLangFull[messLangCodes.indexOf(
                            initLanguage
                        )]
                    })"
                }
            )
            if (checkedLang.value) {
                //CONTACTS: DROPDOWN:
                DropdownSpinner(
                    context = context,
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