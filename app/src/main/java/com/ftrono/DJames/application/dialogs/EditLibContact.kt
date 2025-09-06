package com.ftrono.DJames.application.dialogs

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.navigation.compose.rememberNavController
import com.ftrono.DJames.R
import com.ftrono.DJames.application.aliasFieldDescription
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.messLangFull
import com.ftrono.DJames.application.messLangCodes
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.database.PhoneSet
import com.ftrono.DJames.application.screens.LibraryScreen
import com.ftrono.DJames.be.database.LibraryItem
import com.ftrono.DJames.be.samples.testLibrary
import com.ftrono.DJames.ui.components.CustomCheckbox
import com.ftrono.DJames.ui.components.DropdownSpinner
import com.ftrono.DJames.ui.dialogs.DialogRequestDetail
import com.ftrono.DJames.ui.components.EditPhoneDynamicField
import com.ftrono.DJames.ui.dialogs.EditLibDialog
import com.ftrono.DJames.ui.components.EditLibDynamicField
import com.ftrono.DJames.ui.components.EditLibDynamicNameSection
import com.ftrono.DJames.ui.selectors.getTextFieldColors
import com.ftrono.DJames.ui.selectors.libColorSelector
import com.ftrono.DJames.ui.selectors.libColorSelectorLight


@Preview
@Preview(heightDp = 360, widthDp = 800)
@Composable
fun DialogEditContactPreview() {
    val navController = rememberNavController()
    LibraryScreen(navController, editPreview="contact", preview=true)
}


@Composable
fun EditLibContact(
    context: Context,
    snapshot: MutableState<Long>,
    idState: MutableState<Long>,
    onDismiss: () -> Unit = {},
    preview: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }

    //Init:
    val id: Long = idState.value
    val filter = "contact"

    //Pre-populate:
    // val itemContact = Contact()
    val itemContact = if (preview) {
        testLibrary.filter{ it.type == filter }[0]
    } else if (id > -1) {
        libUtils.getLibItemById(idState.value)
    } else {
        LibraryItem(
            source = filter,
            type = filter,
            phoneSet = PhoneSet(),
        )
    }
    val checkedLang = remember { mutableStateOf(false) }

    //Init aliases:
    val initAliases = itemContact.aliases.toMutableList()
    if (initAliases.isNotEmpty()) {
        initAliases.removeAt(0)
    }

    //Init default language:
    var initLanguage = itemContact.language
    if (initLanguage == "") {
        //No custom language:
        initLanguage = if (preview) "it" else prefs.messageLanguage
    } else {
        //There is a custom language:
        checkedLang.value = true
    }

    //States:
    val textName = rememberSaveable { mutableStateOf(itemContact.name) }
    val textSubtitle = rememberSaveable { mutableStateOf("") }
    val textAliases = rememberSaveable { mutableStateOf(initAliases.joinToString(", ")) }
    val imageUrlState = rememberSaveable { mutableStateOf("") }
    val textLanguage = rememberSaveable { mutableStateOf(messLangFull[messLangCodes.indexOf(initLanguage)]) }
    val textPrefix = rememberSaveable { mutableStateOf(itemContact.phoneSet!!.prefix) }
    val textPhone = rememberSaveable { mutableStateOf(itemContact.phoneSet!!.phone) }

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
            title = filter,
            cat = filter,
            showRefresh = false,
            onDismiss = {
                onDismiss()
            },
            onSave = {
                //CHECK & BUILD:
                //1) Validate Prefix:
                requestDetailOn.value = !utils.isGlobalPhone(
                    prefix = textPrefix.value.replace(" ", ""),
                    phone = textPhone.value.replace(" ", "")
                )

                if (!requestDetailOn.value && textName.value != "") {
                    //2) Update object:
                    val aliasesList = mutableListOf(utils.cleanString(textName.value).lowercase())
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
                    itemContact.lastUpdated = utils.getCurrentTimestamp()
                    //Language:
                    if (checkedLang.value) {
                        itemContact.language =
                            messLangCodes[messLangFull.indexOf(textLanguage.value)]
                    } else {
                        itemContact.language = ""
                    }
                    itemContact.phoneSet = PhoneSet(
                        prefix = textPrefix.value.replace(" ", ""),
                        phone = textPhone.value.replace(" ", "")
                    )

                    //3) Update / store to DB:
                    libUtils.storeLibItem(context, itemContact)

                    //4) End & close:
                    snapshot.value = utils.getCurrentTimestamp()   //Refresh list
                    onDismiss()
                }
            }
        ) {
            //CONTACT NAME:
            EditLibDynamicNameSection(
                filter = filter,
                textState = textName,
                subtitleState = textSubtitle,
                imageUrl = imageUrlState.value,
                preview = preview,
            )

            //CONTACT ALIASES:
            EditLibDynamicField(
                modifier = Modifier
                    .fillMaxWidth(),
                textHeaderColor = libColorSelectorLight(cat = filter),
                textFieldColors = getTextFieldColors(
                    colorLight = libColorSelectorLight(cat = filter),
                    colorDark = libColorSelector(cat = filter)
                ),
                title = "Aliases (separate with commas)",
                description = aliasFieldDescription,
                placeholder = "Write aliases here...",
                italic = true,
                textState = textAliases
            )

            //CONTACT PHONE:
            EditPhoneDynamicField(
                textHeaderColor = libColorSelectorLight(cat = filter),
                textFieldColors = getTextFieldColors(
                    colorLight = libColorSelectorLight(cat = filter),
                    colorDark = libColorSelector(cat = filter)
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
                checkedColor = libColorSelectorLight(cat = filter),
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
                    focusColorLight = libColorSelectorLight(cat = filter),
                    focusColorDark = libColorSelector(cat = filter)
                )
            } else {
                textLanguage.value = ""
            }
        }
    }
}