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
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.database.Route
import com.ftrono.DJames.application.screens.VocabularyScreen
import com.ftrono.DJames.be.samples.testRoutes
import com.ftrono.DJames.ui.components.CustomCheckbox
import com.ftrono.DJames.ui.dialogs.DialogRequestDetail
import com.ftrono.DJames.ui.dialogs.EditVocDialog
import com.ftrono.DJames.ui.components.EditVocDynamicField
import com.ftrono.DJames.ui.components.EditVocDynamicNameSection
import com.ftrono.DJames.ui.selectors.getTextFieldColors
import com.ftrono.DJames.ui.selectors.vocColorSelector
import com.ftrono.DJames.ui.selectors.vocColorSelectorLight
import com.ftrono.DJames.ui.selectors.vocIconSelector
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.ftrono.DJames.application.fulfillmentUtils
import com.ftrono.DJames.ui.components.EditVocSectionTitle


@Preview
@Preview(heightDp = 360, widthDp = 800)
@Composable
fun DialogEditRoutePreview() {
    VocabularyScreen(editPreview="route", preview=true)
}


@Composable
fun EditVocRoute(
    context: Context,
    libraryMap: MutableState<Map<String, String>>,
    keyState: MutableState<String>,
    filter: String,
    onDismiss: () -> Unit = {},
    preview: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }
    val mContext = LocalContext.current

    //Init:
    val key = keyState.value

    //Pre-populate:
    // val itemRoute = Route()
    var itemRoute = if (preview) {
        testRoutes[0]
    } else if (key != "") {
        libUtils.getRoute(keyState.value)
    } else {
        Route()
    }

    //Init aliases:
    val initAliases = itemRoute.aliases.toMutableList()
    initAliases.removeAt(0)

    //States:
    val textName = rememberSaveable { mutableStateOf(itemRoute.name) }
    val textSubtitle = rememberSaveable { mutableStateOf(libUtils.buildRouteSubtitle(itemRoute)) }
    val textAliases = rememberSaveable { mutableStateOf(initAliases.joinToString(", ")) }
    val imageUrlState = rememberSaveable { mutableStateOf("") }
    //Destination:
    val textDestAddress = rememberSaveable { mutableStateOf(itemRoute.destination.address) }
    val textDestNumber = rememberSaveable { mutableStateOf(itemRoute.destination.number) }
    val textDestPlaceName = rememberSaveable { mutableStateOf(itemRoute.destination.placeName) }
    val textDestTown = rememberSaveable { mutableStateOf(itemRoute.destination.town) }
    val textDestZip = rememberSaveable { mutableStateOf(itemRoute.destination.zip) }
    val textDestProvince = rememberSaveable { mutableStateOf(itemRoute.destination.province) }
    val textDestCountry = rememberSaveable { mutableStateOf(if (itemRoute.destination.country == "") "Italy" else itemRoute.destination.country) }
    //Via:
    val checkedVia = remember { mutableStateOf(itemRoute.via.address != "") }
    val textViaAddress = rememberSaveable { mutableStateOf(itemRoute.via.address) }
    val textViaNumber = rememberSaveable { mutableStateOf(itemRoute.via.number) }
    val textViaPlaceName = rememberSaveable { mutableStateOf(itemRoute.via.placeName) }
    val textViaTown = rememberSaveable { mutableStateOf(itemRoute.via.town) }
    val textViaZip = rememberSaveable { mutableStateOf(itemRoute.via.zip) }
    val textViaProvince = rememberSaveable { mutableStateOf(itemRoute.via.province) }
    val textViaCountry = rememberSaveable { mutableStateOf(if (itemRoute.via.country == "") "Italy" else itemRoute.via.country) }

    val requestDetailOn = rememberSaveable { mutableStateOf(false) }
    if (requestDetailOn.value) {
        DialogRequestDetail(
            dialogOnState = requestDetailOn,
            title = "Route URL",
            message = if (checkedVia.value) {
                "Please enter a valid Destination or Pass Through address for the current Route!"
            } else {
                "Please enter a valid Destination address for the current Route!"
            }
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
            showRefresh = false,
            showGo = true,
            onGo = {
                if (itemRoute.destination.town != "") {
                    utils.openLink(mContext, url = fulfillmentUtils.buildRouteUrl(itemRoute), fromService = false)
                }
            },
            onDismiss = {
                onDismiss()
            },
            onSave = {
                //CHECK & BUILD:
                //1) Validate Route:
                requestDetailOn.value = textDestAddress.value == "" ||
                    textDestTown.value == "" ||
                    checkedVia.value && (
                        textViaAddress.value == "" ||
                        textViaTown.value == ""
                    )

                if (!requestDetailOn.value && textName.value != "") {
                    //2) Update object:
                    //TODO: TEMP:
                    val aliasesList = mutableListOf(
                        utils.cleanString(textName.value).lowercase(),
                        utils.cleanString(
                            textDestPlaceName.value.trim()
                            + " " + textDestAddress.value.trim()
                            + " " + textDestNumber.value.trim()
                            + " " + textDestTown.value.trim()
                            + " " + textViaPlaceName.value.trim()
                            + " " + textViaAddress.value.trim()
                            + " " + textViaNumber.value.trim()
                            + " " + textViaTown.value.trim()
                        ).lowercase(),
                    )
                    Log.d("EditVocRoute", "$aliasesList")
                    if (textAliases.value != "") {
                        for (alias in textAliases.value.split(",")) {
                            val temp = utils.cleanString(alias).lowercase()
                            if (temp != "" && !aliasesList.contains(temp)) {
                                aliasesList.add(temp)
                            }
                        }
                    }
                    itemRoute.name = utils.capitalizeWords(textName.value).trim()
                    itemRoute.aliases = aliasesList
                    //Destination:
                    itemRoute.destination.address = utils.capitalizeWords(textDestAddress.value.trim())
                    itemRoute.destination.number = textDestNumber.value.uppercase().trim()
                    itemRoute.destination.placeName = utils.capitalizeWords(textDestPlaceName.value.trim())
                    itemRoute.destination.town = utils.capitalizeWords(textDestTown.value.trim())
                    itemRoute.destination.zip = textDestZip.value.uppercase().trim()
                    itemRoute.destination.province = textDestProvince.value.uppercase().trim()
                    itemRoute.destination.country = utils.capitalizeWords(textDestCountry.value.trim())
                    //Via:
                    itemRoute.via.address = if (!checkedVia.value) "" else utils.capitalizeWords(textViaAddress.value.trim())
                    itemRoute.via.number = if (!checkedVia.value) "" else textViaNumber.value.uppercase().trim()
                    itemRoute.via.placeName = if (!checkedVia.value) "" else utils.capitalizeWords(textViaPlaceName.value.trim())
                    itemRoute.via.town = if (!checkedVia.value) "" else utils.capitalizeWords(textViaTown.value.trim())
                    itemRoute.via.zip = if (!checkedVia.value) "" else textViaZip.value.uppercase().trim()
                    itemRoute.via.province = if (!checkedVia.value) "" else textViaProvince.value.uppercase().trim()
                    itemRoute.via.country = if (!checkedVia.value) "" else utils.capitalizeWords(textViaCountry.value.trim())


                    //3) Update / store to DB:
                    libUtils.storeRoute(context, itemRoute)

                    //4) End & close:
                    libraryMap.value = libUtils.refreshLibrary(filter)   //Refresh list
                    onDismiss()
                }
            }
        ) {
            //CONTENT:
            //ROUTE NAME:
            EditVocDynamicNameSection(
                textHeaderColor = vocColorSelectorLight(cat = filter),
                textFieldColors = getTextFieldColors(
                    colorLight = vocColorSelectorLight(cat = filter),
                    colorDark = vocColorSelector(cat = filter)
                ),
                filter = filter,
                textState = textName,
                subtitleState = textSubtitle,
                imageUrlState = imageUrlState,
                initActive = textName.value == "",
                showEditIcon = true
            )

            //ROUTE ALIASES:
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

            //DESTINATION ADDRESS:
            AddressGroup(
                title = "Destination address",
                placeNameState = textDestPlaceName,
                addressState = textDestAddress,
                numberState = textDestNumber,
                zipState = textDestZip,
                townState = textDestTown,
                provinceState = textDestProvince,
                countryState = textDestCountry
            )

            //VIA: CHECKBOX:
            CustomCheckbox(
                modifier = Modifier
                    .padding(bottom = 5.dp),
                checkedState = checkedVia,
                checkedColor = vocColorSelectorLight(cat = filter),
                textColor = colorResource(id = R.color.light_grey),
                text = "Use a Pass Through address"
            )

            if (checkedVia.value) {
                //VIA ADDRESS:
                AddressGroup(
                    title = "Pass Through address",
                    placeNameState = textViaPlaceName,
                    addressState = textViaAddress,
                    numberState = textViaNumber,
                    zipState = textViaZip,
                    townState = textViaTown,
                    provinceState = textViaProvince,
                    countryState = textViaCountry
                )
            }
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
    val filter = "route"

    //Section:
    EditVocSectionTitle(
        title = title
    )

    //PLACE NAME:
    EditVocDynamicField(
        modifier = Modifier
            .fillMaxWidth(),
        textHeaderColor = vocColorSelectorLight(cat = filter),
        textFieldColors = getTextFieldColors(
            colorLight = vocColorSelectorLight(cat = filter),
            colorDark = vocColorSelector(cat = filter)
        ),
        title = "Place / Business name",
        placeholder = "(optional)",
        textState = placeNameState
    )

    //STREET NAME:
    EditVocDynamicField(
        modifier = Modifier
            .fillMaxWidth(),
        textHeaderColor = vocColorSelectorLight(cat = filter),
        textFieldColors = getTextFieldColors(
            colorLight = vocColorSelectorLight(cat = filter),
            colorDark = vocColorSelector(cat = filter)
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
        EditVocDynamicField(
            modifier = Modifier
                .weight(0.5f)
                .padding(end = 2.dp),
            textHeaderColor = vocColorSelectorLight(cat = filter),
            textFieldColors = getTextFieldColors(
                colorLight = vocColorSelectorLight(cat = filter),
                colorDark = vocColorSelector(cat = filter)
            ),
            title = "Number",
            placeholder = "...",
            textState = numberState,
            charLimit = 5
        )

        //ZIP:
        EditVocDynamicField(
            modifier = Modifier
                .weight(0.5f)
                .padding(start = 2.dp),
            textHeaderColor = vocColorSelectorLight(cat = filter),
            textFieldColors = getTextFieldColors(
                colorLight = vocColorSelectorLight(cat = filter),
                colorDark = vocColorSelector(cat = filter)
            ),
            title = "Zip code",
            placeholder = "...",
            textState = zipState
        )

    }

    //TOWN NAME:
    EditVocDynamicField(
        modifier = Modifier
            .fillMaxWidth(),
        textHeaderColor = vocColorSelectorLight(cat = filter),
        textFieldColors = getTextFieldColors(
            colorLight = vocColorSelectorLight(cat = filter),
            colorDark = vocColorSelector(cat = filter)
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
        EditVocDynamicField(
            modifier = Modifier
                .weight(0.4f)
                .padding(end = 2.dp),
            textHeaderColor = vocColorSelectorLight(cat = filter),
            textFieldColors = getTextFieldColors(
                colorLight = vocColorSelectorLight(cat = filter),
                colorDark = vocColorSelector(cat = filter)
            ),
            title = "Province",
            placeholder = "...",
            textState = provinceState,
            charLimit = 2
        )

        //COUNTRY:
        EditVocDynamicField(
            modifier = Modifier
                .weight(0.6f)
                .padding(start = 2.dp),
            textHeaderColor = vocColorSelectorLight(cat = filter),
            textFieldColors = getTextFieldColors(
                colorLight = vocColorSelectorLight(cat = filter),
                colorDark = vocColorSelector(cat = filter)
            ),
            title = "Country",
            placeholder = "Country...",
            textState = countryState
        )

    }
}
