package com.ftrono.DJames.dialogs

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
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
import com.ftrono.DJames.R
import com.ftrono.DJames.ui.RoundedSign
import com.ftrono.DJames.ui.getTextFieldColors
import com.ftrono.DJames.ui.vocColorSelector
import com.ftrono.DJames.ui.vocColorSelectorLight
import com.ftrono.DJames.ui.vocIconSelector


//EDIT VOC COMPONENTS:

@Preview
@Composable
fun DialogRequestDetailPreview() {
    DialogRequestDetail(
        dialogOnState = remember {
            mutableStateOf(true)
        }
    )
}


//Request missing detail:
@Composable
fun DialogRequestDetail(
    dialogOnState: MutableState<Boolean>,
    title: String = "Title",
    message: String = "Custom message"
) {
    //REQUEST DETAIL DIALOG:
    GeneralDialog(
        dialogOnState = dialogOnState,
        backgroundColor = colorResource(id = R.color.colorPrimaryDark),
        title = title,
        content = {
            Text(
                text = message,
                color = colorResource(id = R.color.light_grey),
                fontSize = 14.sp
            )
        },
        dismissText = "Ok"
    )
}


@Preview
@Composable
fun DialogEditVocPreview() {
    Dialog (
        onDismissRequest = {}
    ) {
        EditVocDialog(filter = "artist")
    }
}


//Main Container: EditVocDialog:
@Composable
fun EditVocDialog(
    modifier: Modifier = Modifier,
    filter: String,
    onDismiss: () -> Unit = {},
    onSave: () -> Unit = {},
    content: @Composable () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isLandscape by remember { mutableStateOf(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) }

    //EDIT DIALOG CONTAINER:
    Card(
        modifier = modifier
            .padding(
                top = 30.dp,
                bottom = 30.dp,
                start = if (isLandscape) 80.dp else 40.dp,
                end = if (isLandscape) 80.dp else 40.dp
            )
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(20.dp),
        //border = BorderStroke(2.dp, colorResource(id = R.color.faded_grey)),
        colors = CardDefaults.cardColors (
            containerColor = colorResource(id = R.color.dark_grey_background)
        )
    ) {

        Column(
            modifier = Modifier
                .padding(20.dp)
                .wrapContentWidth()
                .wrapContentHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {

            //TITLE:
            EditVocHeader(
                filter = filter,
                onCancel = { onDismiss() },
                onSave = { onSave() }
            )

            //CONTENT:
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .wrapContentWidth()
                    .wrapContentHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
            ) {

                content()

                //END PADDING:
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                )
            }
        }
    }
}


@Preview
@Composable
fun EditVocHeaderPreview() {
    EditVocHeader(
        filter = "artist",
        onCancel = {},
        onSave = {}
    )
}


//EditVoc Header:
@Composable
fun EditVocHeader(
    filter: String,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    //HEADER:
    Box(
        modifier = Modifier
            .padding(bottom = 12.dp)
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.Center
    ) {
        //HEADER CONTENT:
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            //ICON:
            Icon(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(36.dp),
                painter = vocIconSelector(cat = filter),
                contentDescription = filter,
                tint = vocColorSelectorLight(cat = filter)
            )
            //TITLE:
            Text(
                text = "${filter.replaceFirstChar { it.uppercase() }}",
                modifier = Modifier
                    .padding(8.dp)
                    .weight(1f),
                color = colorResource(id = R.color.light_grey),
                textAlign = TextAlign.Start,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            //BACK BUTTON:
            Icon(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(35.dp)
                    .clickable {
                        onCancel()
                    },
                imageVector = Icons.Filled.Close,
                contentDescription = "Cancel",
                tint = vocColorSelectorLight(cat = filter)
            )
            //SAVE BUTTON:
            Icon(
                modifier = Modifier
                    .size(35.dp)
                    .clickable {
                        onSave()
                    },
                imageVector = Icons.Default.Check,
                contentDescription = "Save",
                tint = vocColorSelectorLight(cat = filter)
            )
        }
    }
}


//Text Field with title:
@Composable
fun EditVocTextField(
    modifier: Modifier = Modifier,
    textHeaderColor: Color = colorResource(id = R.color.light_grey),
    textFieldColors: TextFieldColors,
    title: String,
    placeholder: String,
    textState: MutableState<String>,
    showButton: Boolean = false,
    onKeyboardDone: () -> Unit = {}
) {
    Column (
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {

        //Title:
        Text(
            text = title,
            color = textHeaderColor,
            textAlign = TextAlign.Start,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )

        Row (
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(top = 8.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            //Text Field:
            OutlinedTextField(
                modifier = modifier
                    .weight(1f),
                colors = textFieldColors,
                value = textState.value,
                onValueChange = { newText ->
                    textState.value = newText.trimStart { it == '0' }
                },
                textStyle = TextStyle(
                    fontSize = 16.sp
                ),
//                maxLines = 1,
//                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        onKeyboardDone()
                    }
                ),
                placeholder = {
                    Text(
                        text = placeholder,
                        fontSize = 16.sp,
                        fontStyle = FontStyle.Italic
                    )
                }
            )

            //Button:
            if (showButton) {
                RoundedSign(
                    modifier = modifier
                        .padding(start = 8.dp)
                        .clickable { onKeyboardDone() },
                    signSize = 40.dp,
                    iconSize = 20.dp,
                    backgroundColor = textFieldColors.textSelectionColors.backgroundColor,
                    borderColor = textFieldColors.textSelectionColors.backgroundColor,
                    iconColor = colorResource(id = R.color.light_grey),
                    iconVector = Icons.Default.Done
                )
            }
        }
    }
}

@Preview
@Composable
fun EditVocDynamicFieldPreview() {
    val textName = rememberSaveable { mutableStateOf("sample text") }
    val textHeaderColor = vocColorSelectorLight(cat = "artist")
    val textFieldColors = getTextFieldColors(
        colorLight = vocColorSelectorLight(cat = "artist"),
        colorDark = vocColorSelector(cat = "artist")
    )
    EditVocDynamicField(
        textHeaderColor = textHeaderColor,
        textFieldColors = textFieldColors,
        disabledText = textName.value,
        title = "Artist Name",
        placeholder = "Write name here...",
        textState = textName,
        useChips = true
    )
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditVocDynamicField(
    modifier: Modifier = Modifier,
    textHeaderColor: Color = colorResource(id = R.color.light_grey),
    textFieldColors: TextFieldColors,
    disabledText: String,
    disabledTextColor: Color = colorResource(id = R.color.mid_grey),
    title: String,
    placeholder: String,
    textState: MutableState<String>,
    useChips: Boolean = false,
    onClicked: () -> Unit = {},
    onKeyboardDone: () -> Unit = {}
) {
    val isActive = rememberSaveable { mutableStateOf(false) }
    if (!isActive.value) {

        Column (
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {

            //Title:
            Text(
                modifier = Modifier
                    .clickable {
                        isActive.value = true
                        onClicked()
                   },
                text = if (useChips && !isActive.value) title.slice(0..<title.indexOf("(", ignoreCase = true)) else title,
                color = textHeaderColor,
                textAlign = TextAlign.Start,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(top = 8.dp, bottom = 20.dp)
                    .clickable {
                        isActive.value = true
                        onClicked()
                    },
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                //Edit icon:
                Icon(
                    modifier = modifier
                        .padding(end = 8.dp)
                        .size(20.dp),
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = disabledTextColor
                )

                //Value:
                if (useChips && textState.value != "") {
                    //Chips row:
                    FlowRow(
                        modifier = modifier
                            .weight(1f),
                        horizontalArrangement = Arrangement.Start,
                        verticalArrangement = Arrangement.Center
                    ) {

                        for (alias in textState.value.split(",")) {
                            AssistChip(
                                modifier = Modifier
                                    .padding(end=4.dp),
                                onClick = {
                                    isActive.value = true
                                    onClicked()
                                },
                                label = { 
                                    Text(
                                        text=alias,
                                        color = colorResource(id = R.color.light_grey),
                                        fontSize = 14.sp,
                                        fontStyle = FontStyle.Italic
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = textFieldColors.textSelectionColors.backgroundColor,
                                    labelColor = colorResource(id = R.color.light_grey)
                                ),
                                border = null
                            )
                        }
                    }

                } else {
                    //Text:
                    Text(
                        modifier = modifier
                            .weight(1f),
                        text = disabledText,
                        color = disabledTextColor,
                        textAlign = TextAlign.Start,
                        fontSize = 16.sp,
                        fontStyle = FontStyle.Italic,
                    )
                }
            }
        }

    } else {
        //TextField:
        EditVocTextField(
            modifier = modifier,
            textHeaderColor = textHeaderColor,
            textFieldColors = textFieldColors,
            title = title,
            placeholder = placeholder,
            textState = textState,
            showButton = true,
            onKeyboardDone = {
                onKeyboardDone()
                isActive.value = false
            },
        )
    }
}


@Composable
fun EditPhoneDynamicField(
    modifier: Modifier = Modifier,
    textHeaderColor: Color = colorResource(id = R.color.light_grey),
    textFieldColors: TextFieldColors,
    disabledText: String,
    disabledTextColor: Color = colorResource(id = R.color.mid_grey),
    title: String,
    textPrefix: MutableState<String>,
    textPhone: MutableState<String>,
    onClicked: () -> Unit = {},
    onKeyboardDone: () -> Unit = {}
) {
    val isActive = rememberSaveable { mutableStateOf(false) }
    Column (
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        //Title:
        Text(
            modifier = Modifier
                .clickable {
                    isActive.value = true
                    onClicked()
                },
            text = title,
            color = textHeaderColor,
            textAlign = TextAlign.Start,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )

        if (!isActive.value) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(top = 8.dp, bottom = 20.dp)
                    .clickable {
                        isActive.value = true
                        onClicked()
                    },
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                //Edit icon:
                Icon(
                    modifier = modifier
                        .padding(end = 8.dp)
                        .size(20.dp),
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = disabledTextColor
                )

                //Value:
                Text(
                    modifier = modifier
                        .weight(1f),
                    text = disabledText,
                    color = disabledTextColor,
                    textAlign = TextAlign.Start,
                    fontSize = 16.sp,
                    fontStyle = FontStyle.Italic,
                )
            }

        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(top = 8.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {

                //PREFIX:
                OutlinedTextField(
                    modifier = modifier
                        .width(60.dp),
                    colors = textFieldColors,
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
                    modifier = modifier
                        .weight(1f),
                    colors = textFieldColors,
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
                            onKeyboardDone()
                            isActive.value = false
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

                //Button:
                RoundedSign(
                    modifier = modifier
                        .padding(start = 8.dp)
                        .clickable {
                            onKeyboardDone()
                            isActive.value = false
                        },
                    signSize = 40.dp,
                    iconSize = 20.dp,
                    backgroundColor = textFieldColors.textSelectionColors.backgroundColor,
                    borderColor = textFieldColors.textSelectionColors.backgroundColor,
                    iconColor = colorResource(id = R.color.light_grey),
                    iconVector = Icons.Default.Done
                )
            }
        }
    }

}


