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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.application.screens.LibraryScreen
import com.ftrono.DJames.ui.dialogs.DialogRequestDetail
import com.ftrono.DJames.ui.dialogs.EditLibDialog
import com.ftrono.DJames.ui.components.EditLibDynamicField
import com.ftrono.DJames.ui.components.EditLibDynamicNameSection
import com.ftrono.DJames.ui.selectors.getTextFieldColors
import com.ftrono.DJames.ui.selectors.libColorSelector
import com.ftrono.DJames.ui.selectors.libColorSelectorLight
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.ftrono.DJames.application.aliasFieldDescription
import com.ftrono.DJames.be.database.Address
import com.ftrono.DJames.be.database.LibraryItem
import com.ftrono.DJames.be.samples.testLibrary
import com.ftrono.DJames.ui.components.EditLibSectionTitle


@Preview
@Preview(heightDp = 360, widthDp = 800)
@Composable
fun DialogEditPlacePreview() {
    val navController = rememberNavController()
    LibraryScreen(navController, editPreview="place", preview=true)
}


@Composable
fun EditLibPlace(
    context: Context,
    snapshot: MutableState<Long>,
    idState: MutableState<Long>,
    onDismiss: () -> Unit = {},
    preview: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }
    val mContext = LocalContext.current

    //Init:
    val id: Long = idState.value
    val filter = "place"

    //Pre-populate:
    var itemPlace = if (preview) {
        testLibrary.filter{ it.type == filter }[0]
    } else if (id > -1) {
        libUtils.getLibItemById(idState.value)
    } else {
        LibraryItem(
            source = filter,
            type = filter,
            address = Address(),
        )
    }

    //Init aliases:
    val initAliases = itemPlace.aliases.toMutableList()
    if (initAliases.isNotEmpty()) {
        initAliases.removeAt(0)
    }

    //States:
    val textName = rememberSaveable { mutableStateOf(itemPlace.name) }
    val textSubtitle = rememberSaveable { mutableStateOf(itemPlace.address!!.town) }
    val textAliases = rememberSaveable { mutableStateOf(initAliases.joinToString(", ")) }
    val imageUrlState = rememberSaveable { mutableStateOf("") }
    //Address:
    val textDestAddress = rememberSaveable { mutableStateOf(itemPlace.address!!.street) }
    val textDestNumber = rememberSaveable { mutableStateOf(itemPlace.address!!.number) }
    val textDestPlaceName = rememberSaveable { mutableStateOf(itemPlace.address!!.placeName) }
    val textDestTown = rememberSaveable { mutableStateOf(itemPlace.address!!.town) }
    val textDestZip = rememberSaveable { mutableStateOf(itemPlace.address!!.zip) }
    val textDestProvince = rememberSaveable { mutableStateOf(itemPlace.address!!.province) }
    val textDestCountry = rememberSaveable { mutableStateOf(if (itemPlace.address!!.country == "") "Italy" else itemPlace.address!!.country) }

    val requestDetailOn = rememberSaveable { mutableStateOf(false) }
    if (requestDetailOn.value) {
        DialogRequestDetail(
            dialogOnState = requestDetailOn,
            title = "Address",
            message = "Please enter a valid address for the current place!",
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
                .clickable {
                    focusManager.clearFocus()
                },
            title = filter,
            cat = filter,
            showRefresh = false,
            showGo = true,
            onGo = {
                if (itemPlace.address != null) {
                    utils.openLink(mContext, url = libUtils.buildPlaceUrlFromLibraryItem(itemPlace.address), fromService = false)
                }
            },
            onDismiss = {
                onDismiss()
            },
            onSave = {
                //CHECK & BUILD:
                //1) Validate Place:
                requestDetailOn.value =
                    textDestAddress.value == "" ||
                    textDestTown.value == ""

                if (!requestDetailOn.value && textName.value != "") {
                    //2) Update object:
                    val aliasesList = mutableListOf(
                        utils.cleanString(textName.value).lowercase()
                    )
                    Log.d("EditLibPlace", "$aliasesList")
                    if (textAliases.value != "") {
                        for (alias in textAliases.value.split(",")) {
                            val temp = utils.cleanString(alias).lowercase()
                            if (temp != "" && !aliasesList.contains(temp)) {
                                aliasesList.add(temp)
                            }
                        }
                    }
                    itemPlace.name = utils.capitalizeWords(textName.value).trim()
                    itemPlace.aliases = aliasesList
                    //Address:
                    itemPlace.address = Address(
                        street = utils.capitalizeWords(textDestAddress.value.trim()),
                        number = textDestNumber.value.uppercase().trim(),
                        placeName = utils.capitalizeWords(textDestPlaceName.value.trim()),
                        town = utils.capitalizeWords(textDestTown.value.trim()),
                        zip = textDestZip.value.uppercase().trim(),
                        province = textDestProvince.value.uppercase().trim(),
                        country = utils.capitalizeWords(textDestCountry.value.trim()),
                    )


                    //3) Update / store to DB:
                    libUtils.storeLibItem(context, itemPlace)

                    //4) End & close:
                    snapshot.value = utils.getCurrentTimestamp()   //Refresh list
                    onDismiss()
                }
            }
        ) {
            //CONTENT:
            //PLACE NAME:
            EditLibDynamicNameSection(
                filter = filter,
                textState = textName,
                subtitleState = textSubtitle,
                imageUrl = imageUrlState.value,
                preview = preview,
            )

            //PLACE ALIASES:
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

            //ADDRESS:
            AddressGroup(
                title = "Address",
                placeNameState = textDestPlaceName,
                addressState = textDestAddress,
                numberState = textDestNumber,
                zipState = textDestZip,
                townState = textDestTown,
                provinceState = textDestProvince,
                countryState = textDestCountry
            )
        }
    }
}


@Composable
fun AddressGroup(
    title: String,
    placeNameState: MutableState<String>,
    addressState: MutableState<String>,
    numberState: MutableState<String>,
    zipState: MutableState<String>,
    townState: MutableState<String>,
    provinceState: MutableState<String>,
    countryState: MutableState<String>,
) {
    val filter = "place"

    //Section:
    EditLibSectionTitle(
        title = title
    )

    //PLACE NAME:
    EditLibDynamicField(
        modifier = Modifier
            .fillMaxWidth(),
        textHeaderColor = libColorSelectorLight(cat = filter),
        textFieldColors = getTextFieldColors(
            colorLight = libColorSelectorLight(cat = filter),
            colorDark = libColorSelector(cat = filter)
        ),
        title = "Place / Business name",
        placeholder = "(optional)",
        textState = placeNameState
    )

    //STREET NAME:
    EditLibDynamicField(
        modifier = Modifier
            .fillMaxWidth(),
        textHeaderColor = libColorSelectorLight(cat = filter),
        textFieldColors = getTextFieldColors(
            colorLight = libColorSelectorLight(cat = filter),
            colorDark = libColorSelector(cat = filter)
        ),
        title = "Street name",
        placeholder = "Write street...",
        textState = addressState
    )

    Row (
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        //NUMBER:
        EditLibDynamicField(
            modifier = Modifier
                .weight(0.5f)
                .padding(end = 2.dp),
            textHeaderColor = libColorSelectorLight(cat = filter),
            textFieldColors = getTextFieldColors(
                colorLight = libColorSelectorLight(cat = filter),
                colorDark = libColorSelector(cat = filter)
            ),
            title = "Number",
            placeholder = "...",
            textState = numberState,
            charLimit = 5
        )

        //ZIP:
        EditLibDynamicField(
            modifier = Modifier
                .weight(0.5f)
                .padding(start = 2.dp),
            textHeaderColor = libColorSelectorLight(cat = filter),
            textFieldColors = getTextFieldColors(
                colorLight = libColorSelectorLight(cat = filter),
                colorDark = libColorSelector(cat = filter)
            ),
            title = "Zip code",
            placeholder = "...",
            textState = zipState
        )

    }

    //TOWN NAME:
    EditLibDynamicField(
        modifier = Modifier
            .fillMaxWidth(),
        textHeaderColor = libColorSelectorLight(cat = filter),
        textFieldColors = getTextFieldColors(
            colorLight = libColorSelectorLight(cat = filter),
            colorDark = libColorSelector(cat = filter)
        ),
        title = "Town name",
        placeholder = "Write town...",
        textState = townState
    )

    Row (
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        //PROVINCE:
        EditLibDynamicField(
            modifier = Modifier
                .weight(0.4f)
                .padding(end = 2.dp),
            textHeaderColor = libColorSelectorLight(cat = filter),
            textFieldColors = getTextFieldColors(
                colorLight = libColorSelectorLight(cat = filter),
                colorDark = libColorSelector(cat = filter)
            ),
            title = "Province",
            placeholder = "...",
            textState = provinceState,
            charLimit = 2
        )

        //COUNTRY:
        EditLibDynamicField(
            modifier = Modifier
                .weight(0.6f)
                .padding(start = 2.dp),
            textHeaderColor = libColorSelectorLight(cat = filter),
            textFieldColors = getTextFieldColors(
                colorLight = libColorSelectorLight(cat = filter),
                colorDark = libColorSelector(cat = filter)
            ),
            title = "Country",
            placeholder = "Country...",
            textState = countryState
        )

    }
}
