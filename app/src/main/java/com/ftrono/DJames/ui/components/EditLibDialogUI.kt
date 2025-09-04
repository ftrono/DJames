package com.ftrono.DJames.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ftrono.DJames.R
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.ui.selectors.getTextFieldColors
import com.ftrono.DJames.ui.selectors.libColorSelector
import com.ftrono.DJames.ui.selectors.libColorSelectorLight
import com.ftrono.DJames.ui.selectors.libIconSelector


//EDIT LIB COMPONENTS:
@Composable
fun EditLibTitle(
    modifier: Modifier = Modifier,
    textHeaderColor: Color = colorResource(id = R.color.light_grey),
    fontSize: TextUnit = 16.sp,
    title: String,
    onClicked: () -> Unit = {},
) {
    //Title:
    Text(
        modifier = modifier
            .clickable {
                onClicked()
            },
        text = title,
        color = textHeaderColor,
        fontSize = fontSize,
        //fontWeight = FontWeight.Bold,
    )
}


//COMPOSITIONS:
@Preview
@Composable
fun EditLibDynamicNameSectionPreview() {
    val textState = remember { mutableStateOf("Ginetto") }
    val subtitleState = remember { mutableStateOf("Spotify") }
    val imageUrlState = remember { mutableStateOf("") }
    EditLibDynamicNameSection(
        modifier = Modifier,
        filter = "artist",
        textState = textState,
        subtitleState = subtitleState,
        imageUrl = imageUrlState.value,
        preview = true
    )
}


@Composable
fun EditLibDynamicNameSection(
    modifier: Modifier = Modifier,
    filter: String,
    textState: MutableState<String>,
    subtitleState: MutableState<String>? = null,
    imageUrl: String,
    disabled: Boolean = false,
    isCollection: Boolean = false,
    preview: Boolean = false,
    onClick: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isActive by interactionSource.collectIsFocusedAsState()

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val placeholder = "${utils.capitalizeWords(filter)} name"
    val textFieldColors = getTextFieldColors(
        colorLight = libColorSelectorLight(cat = if (filter == "user") "contact" else filter),
        colorDark = libColorSelector(cat = if (filter == "user") "contact" else filter),
        fullyTransparent = true,
    )

    fun onClicked() {
        focusRequester.requestFocus()
        keyboardController!!.show()
    }

    fun onKeyboardDone() {
        focusManager.clearFocus()
        keyboardController!!.hide()
        onClick()
    }

    Row(
        modifier = Modifier
            .padding(bottom = 20.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {

        //CHIP ICON:
        RoundedSign(
            modifier = Modifier
                .focusRequester(focusRequester)
                .clickable {
                    onClicked()
                },
            signSize = 70.dp,
            contentSize = 40,
            backgroundColor = if (isCollection) {
                colorResource(R.color.violetSign)
            } else if (filter == "user") {
                colorResource(id = R.color.faded_grey)
            } else {
                libColorSelector(
                    cat = filter
                )
            },
            borderColor = colorResource(id = R.color.midfaded_grey),
            contentColor = colorResource(id = R.color.light_grey),
            borderWidth = 2.0.dp,
            iconVector = if (isCollection) Icons.Default.Favorite else if (filter == "user") Icons.Outlined.Person else null,
            iconPainter = if (isCollection || filter == "user") null else libIconSelector(cat = filter),
            imageUrl = if (preview || isCollection) "" else imageUrl,
            circle = filter == "artist" || filter == "contact" || filter == "user"
        )

        Column(
            modifier = modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {

            //Text Field:
            OutlinedTextField(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .focusRequester(focusRequester),
                enabled = !disabled,
                colors = textFieldColors,
                value = textState.value,
                interactionSource = interactionSource,
                onValueChange = { newText ->
                    textState.value = newText
                },
                textStyle = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = if (isActive) null else FontWeight.Bold,
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
                        fontSize = 20.sp,
                        fontStyle = FontStyle.Italic,
                        color = colorResource(R.color.light_grey)
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
                            contentSize = 20,
                            backgroundColor = textFieldColors.textSelectionColors.backgroundColor,
                            borderColor = textFieldColors.textSelectionColors.backgroundColor,
                            contentColor = colorResource(id = R.color.light_grey),
                            iconVector = Icons.Default.Done
                        )
                    } else {
                        if (!disabled) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit"
                            )
                        }
                    }

                }
            )

            if (subtitleState != null && subtitleState.value != "" && !isActive) {
                //Subtitle:
                Text(
                    modifier = Modifier
                        .padding(start=23.dp),
                    text = subtitleState.value,
                    fontSize = 16.sp,
                    fontStyle = FontStyle.Italic,
                    color = colorResource(id = R.color.mid_grey)
                )
            }
        }
    }
}

@Preview
@Composable
fun EditLibDynamicFieldPreview() {
    val textName = rememberSaveable { mutableStateOf("sample text") }
    val textHeaderColor = libColorSelectorLight(cat = "artist")
    val textFieldColors = getTextFieldColors(
        colorLight = libColorSelectorLight(cat = "artist"),
        colorDark = libColorSelector(cat = "artist")
    )

    EditLibDynamicField(
        modifier = Modifier
            .fillMaxWidth(),
        textHeaderColor = textHeaderColor,
        textFieldColors = textFieldColors,
        title = "Artist Name",
        placeholder = "Write name here...",
        textState = textName
    )
}


@Composable
fun EditLibDynamicField(
    modifier: Modifier = Modifier,
    textHeaderColor: Color = colorResource(id = R.color.light_grey),
    textFieldColors: TextFieldColors,
    title: String,
    description: String = "",
    placeholder: String,
    italic: Boolean = false,
    textState: MutableState<String>,
    charLimit: Int = 0,
    disabled: Boolean = false,
    onClick: () -> Unit = {},
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
        onClick()
    }

    Column (
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {

        //Title:
        EditLibTitle(
            textHeaderColor = textHeaderColor,
            fontSize = 16.sp,
            title = if (!isActive && title.contains("(")) title.slice(
                0..<title.indexOf(
                    "(",
                    ignoreCase = true
                )
            ) else title,
        )

        //Description:
        if (description != "") {
            Text(
                modifier = modifier
                    .padding(top = 6.dp, bottom = 6.dp)
                    .clickable {
                        onClicked()
                    },
                text = description,
                color = colorResource(R.color.light_grey),
                fontSize = 14.sp,
            )
        }

        //Text Field:
        OutlinedTextField(
            modifier = Modifier
                .padding(top = 8.dp, bottom = 20.dp)
                .focusRequester(focusRequester),
            enabled = !disabled,
            colors = textFieldColors,
            value = textState.value,
            interactionSource = interactionSource,
            onValueChange = { newText ->
                textState.value = if (charLimit > 0 && newText.length >= charLimit) newText.slice(0..<charLimit) else newText
            },
            textStyle = TextStyle(
                fontSize = 16.sp,
                fontStyle = if (italic) FontStyle.Italic else null,
                fontWeight = if (italic) FontWeight.Bold else null
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
                        contentSize = 20,
                        backgroundColor = textFieldColors.textSelectionColors.backgroundColor,
                        borderColor = textFieldColors.textSelectionColors.backgroundColor,
                        contentColor = colorResource(id = R.color.light_grey),
                        iconVector = Icons.Default.Done
                    )
                } else {
                    if (!disabled) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit"
                        )
                    }
                }

            }
        )
    }
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
    EditLibTitle(
        textHeaderColor = textHeaderColor,
        fontSize = fontSize,
        title = title,
    )

    //Text Field with Button:
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
                        contentSize = 20,
                        backgroundColor = textFieldColors.textSelectionColors.backgroundColor,
                        borderColor = textFieldColors.textSelectionColors.backgroundColor,
                        contentColor = colorResource(id = R.color.light_grey),
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
fun EditLibSectionTitlePreview() {
    EditLibSectionTitle(
        title = "DESTINATION ADDRESS"
    )
}


@Composable
fun EditLibSectionTitle(
    title: String
) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 16.dp),
        text = title.uppercase(),
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        color = colorResource(id = R.color.light_grey)
    )
}