package com.ftrono.DJames.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ftrono.DJames.R
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.ui.RoundedSign
import com.ftrono.DJames.ui.getTextFieldColors
import com.ftrono.DJames.ui.vocColorSelector
import com.ftrono.DJames.ui.vocColorSelectorLight
import com.ftrono.DJames.ui.vocIconSelector


//EDIT VOC COMPONENTS:
@Composable
fun EditVocTitle(
    modifier: Modifier = Modifier,
    textHeaderColor: Color = colorResource(id = R.color.light_grey),
    fontSize: TextUnit = 16.sp,
    title: String,
    onClicked: () -> Unit = {},
) {
    //Title:
    Text(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onClicked()
            },
        text = title,
        color = textHeaderColor,
        textAlign = TextAlign.Start,
        fontSize = fontSize,
        //fontWeight = FontWeight.Bold,
    )
}


//COMPOSITIONS:

@Preview
@Composable
fun EditVocDynamicFieldPreview() {
    val textName = rememberSaveable { mutableStateOf("sample text") }
    val textHeaderColor = vocColorSelectorLight(cat = "artist")
    val textFieldColors = getTextFieldColors(
        colorLight = vocColorSelectorLight(cat = "artist"),
        colorDark = vocColorSelector(cat = "artist")
    )
    Column (
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        EditVocDynamicField(
            textHeaderColor = textHeaderColor,
            textFieldColors = textFieldColors,
            title = "Artist Name",
            placeholder = "Write name here...",
            textState = textName
        )
    }
}


@Composable
fun EditVocDynamicField(
    modifier: Modifier = Modifier,
    textHeaderColor: Color = colorResource(id = R.color.light_grey),
    textFieldColors: TextFieldColors,
    title: String,
    placeholder: String,
    fontSize: TextUnit = 16.sp,
    textState: MutableState<String>
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isActive by interactionSource.collectIsFocusedAsState()

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun onClicked() {
        focusRequester.requestFocus()
        keyboardController!!.show()
    }

    fun onKeyboardDone() {
        focusManager.clearFocus()
        keyboardController!!.hide()
    }

    //Title:
    EditVocTitle(
        textHeaderColor = textHeaderColor,
        fontSize = fontSize,
        title = if (!isActive && title.contains("(")) title.slice(0..<title.indexOf("(", ignoreCase = true)) else title,
    )

    //Text Field:
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 20.dp)
            .focusRequester(focusRequester),
        colors = textFieldColors,
        value = textState.value,
        interactionSource = interactionSource,
        onValueChange = { newText ->
            textState.value = newText
        },
        textStyle = TextStyle(
            fontSize = 16.sp
        ),
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
        },
        trailingIcon = {
            if (isActive) {
                //Button:
                RoundedSign(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .focusRequester(focusRequester)
                        .clickable {
                            onKeyboardDone()
                        },
                    signSize = 36.dp,
                    iconSize = 20.dp,
                    backgroundColor = textFieldColors.textSelectionColors.backgroundColor,
                    borderColor = textFieldColors.textSelectionColors.backgroundColor,
                    iconColor = colorResource(id = R.color.light_grey),
                    iconVector = Icons.Default.Done
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit"
                )
            }

        }
    )
}


@Composable
fun EditPhoneDynamicField(
    modifier: Modifier = Modifier,
    textHeaderColor: Color = colorResource(id = R.color.light_grey),
    textFieldColors: TextFieldColors,
    title: String,
    textPrefix: MutableState<String>,
    textPhone: MutableState<String>,
    fontSize: TextUnit = 16.sp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isActive by interactionSource.collectIsFocusedAsState()

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun onClicked() {
        focusRequester.requestFocus()
        keyboardController!!.show()
    }

    fun onKeyboardDone() {
        focusManager.clearFocus()
        keyboardController!!.hide()
    }

    //Title:
    EditVocTitle(
        textHeaderColor = textHeaderColor,
        fontSize = fontSize,
        title = title,
    )

    //Text Field with Button:
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
            modifier = Modifier
                .width(60.dp)
                .focusRequester(focusRequester),
            colors = textFieldColors,
            value = textPrefix.value,
            onValueChange = { newText ->
                textPrefix.value = newText.trimStart { it == '0' }
            },
            textStyle = TextStyle(
                fontSize = fontSize,
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
                    fontSize = fontSize,
                    fontStyle = FontStyle.Italic
                )
            },
        )

        //SUFFIX:
        OutlinedTextField(
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            interactionSource = interactionSource,
            colors = textFieldColors,
            value = textPhone.value,
            onValueChange = { newText ->
                textPhone.value = newText.trimStart { it == '0' }
            },
            textStyle = TextStyle(
                fontSize = fontSize
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
                }
            ),
            placeholder = {
                Text(
                    text = "Phone number...",
                    fontSize = fontSize,
                    fontStyle = FontStyle.Italic
                )
            },
            trailingIcon = {
                if (isActive) {
                    //Button:
                    RoundedSign(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .focusRequester(focusRequester)
                            .clickable {
                                onKeyboardDone()
                            },
                        signSize = 36.dp,
                        iconSize = 20.dp,
                        backgroundColor = textFieldColors.textSelectionColors.backgroundColor,
                        borderColor = textFieldColors.textSelectionColors.backgroundColor,
                        iconColor = colorResource(id = R.color.light_grey),
                        iconVector = Icons.Default.Done
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit"
                    )
                }

            }
        )
    }
}


@Preview
@Composable
fun EditVocDynamicNameSectionPreview() {
    val textName = rememberSaveable { mutableStateOf("sample text") }
    val textFieldColors = getTextFieldColors(
        colorLight = vocColorSelectorLight(cat = "artist"),
        colorDark = vocColorSelector(cat = "artist")
    )
    EditVocDynamicNameSection(
        modifier = Modifier.fillMaxWidth(),
        textFieldColors = textFieldColors,
        filter = "artist",
        textState = textName
    )
}


@Composable
fun EditVocDynamicNameSection(
    modifier: Modifier = Modifier,
    textFieldColors: TextFieldColors,
    filter: String,
    textState: MutableState<String>
) {
    val isActive = rememberSaveable { mutableStateOf(false) }
    var textFieldState by remember { mutableStateOf(TextFieldValue(textState.value)) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun onClicked() {
        focusRequester.requestFocus()
        keyboardController!!.show()
    }

    fun onKeyboardDone() {
        focusManager.clearFocus()
        keyboardController!!.hide()
    }

    Row (
        modifier = Modifier
            .padding(bottom = 20.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ){

        if (!isActive.value) {
            //CHIP ICON:
            RoundedSign(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .clickable {
                        isActive.value = true
                        onClicked()
                    },
                signSize = 70.dp,
                iconSize = 40.dp,
                backgroundColor = vocColorSelector(cat = filter),
                borderColor = colorResource(id = R.color.midfaded_grey),
                iconColor = colorResource(id = R.color.light_grey),
                iconPainter = vocIconSelector(cat = filter),
                circle = filter != "playlist"
            )

            //Name:
            Text(
                modifier = Modifier
                    .padding(start = 20.dp)
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .clickable {
                        isActive.value = true
                        onClicked()
                    },
                text = if (textState.value == "") "${utils.capitalizeWords(filter)} Name" else textState.value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.light_grey)
            )

            if (textState.value == "") {
                //Edit icon:
                Icon(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(30.dp)
                        .focusRequester(focusRequester)
                        .clickable {
                            isActive.value = true
                            onClicked()
                        },
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = colorResource(id = R.color.mid_grey)
                )
            }

        } else {
            //TextField:
            OutlinedTextField(
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                colors = textFieldColors,
                value = textFieldState,
                onValueChange = { newText ->
                    val corr = newText.text
                    textState.value = corr
                    textFieldState = newText
                },
                textStyle = TextStyle(
                    fontSize = 20.sp
                ),
                keyboardOptions = KeyboardOptions(
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
                        text = "Write $filter name...",
                        fontSize = 16.sp,
                        fontStyle = FontStyle.Italic
                    )
                },
                trailingIcon = {
                    //Button:
                    RoundedSign(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clickable {
                                onKeyboardDone()
                                isActive.value = false
                            },
                        signSize = 36.dp,
                        iconSize = 20.dp,
                        backgroundColor = textFieldColors.textSelectionColors.backgroundColor,
                        borderColor = textFieldColors.textSelectionColors.backgroundColor,
                        iconColor = colorResource(id = R.color.light_grey),
                        iconVector = Icons.Default.Done
                    )
                }
            )
            LaunchedEffect(Unit) {
                onClicked()
                textFieldState = textFieldState.copy(selection = TextRange(textFieldState.text.length))
            }
        }
    }
}