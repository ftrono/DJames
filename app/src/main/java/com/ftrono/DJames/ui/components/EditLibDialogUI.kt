package com.ftrono.DJames.ui.components

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
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


@Preview
@Composable
fun EditLibDynamicNameSectionPreview() {
    val textName = rememberSaveable { mutableStateOf("") }
    val subtitle = rememberSaveable { mutableStateOf("Alternative Rock") }
    val imageUrlState = rememberSaveable { mutableStateOf("") }
    val textHeaderColor = libColorSelectorLight(cat = "artist")
    val textFieldColors = getTextFieldColors(
        colorLight = libColorSelectorLight(cat = "artist"),
        colorDark = libColorSelector(cat = "artist")
    )

    EditLibDynamicNameSection(
        modifier = Modifier.fillMaxWidth(),
        textHeaderColor = textHeaderColor,
        textFieldColors = textFieldColors,
        filter = "artist",
        textState = textName,
        subtitleState = subtitle,
        imageUrlState = imageUrlState,
        initActive = false,
        showEditIcon = true,
    )
}


@Composable
fun EditLibDynamicNameSection(
    modifier: Modifier = Modifier,
    textHeaderColor: Color,
    textFieldColors: TextFieldColors,
    filter: String,
    textState: MutableState<String>,
    subtitleState: MutableState<String>? = null,
    imageUrlState: MutableState<String>,
    initActive: Boolean = false,
    showEditIcon: Boolean = false,
    isCollection: Boolean = false,
    preview: Boolean = false,
) {
    val isActive = rememberSaveable { mutableStateOf(initActive) }
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

    Column (
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {

        if (!isActive.value || isCollection) {
            Row(
                modifier = Modifier
                    .padding(bottom = 20.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                //CHIP ICON:
                RoundedSign(
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .clickable {
                            isActive.value = true
                            onClicked()
                        },
                    signSize = 70.dp,
                    contentSize = 40,
                    backgroundColor = if (isCollection) colorResource(R.color.violetSign) else libColorSelector(cat = filter),
                    borderColor = colorResource(id = R.color.midfaded_grey),
                    contentColor = colorResource(id = R.color.light_grey),
                    borderWidth = 2.0.dp,
                    iconVector = if (isCollection) Icons.Default.Favorite else null,
                    iconPainter = if (isCollection) null else libIconSelector(cat = filter),
                    imageUrl = if (preview || isCollection) "" else imageUrlState.value,
                    circle = filter == "artist" || filter == "contact"
                )

                Column(
                    modifier = Modifier
                        .padding(start = 20.dp)
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .clickable {
                            isActive.value = true
                            onClicked()
                        },
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    //Name:
                    Text(
                        text = if (textState.value == "") "${if (filter == "spotify") "Spotify Link" else utils.capitalizeWords(filter)} Name" else textState.value,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(id = R.color.light_grey)
                    )
                    if (subtitleState != null && subtitleState.value != "") {
                        //Subtitle:
                        Text(
                            modifier = Modifier
                                .padding(top = 4.dp),
                            text = subtitleState.value,
                            fontSize = 16.sp,
                            fontStyle = FontStyle.Italic,
                            color = colorResource(id = R.color.mid_grey)
                        )
                    }

                }

                if (showEditIcon && !isCollection) {
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
            }
        } else {
            //Title:
            EditLibTitle(
                textHeaderColor = textHeaderColor,
                fontSize = 16.sp,
                title = "Name",
            )

            //TextField:
            OutlinedTextField(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 20.dp)
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
                        text = "Write ${if (filter == "spotify") "link" else filter} name...",
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
                        contentSize = 20,
                        backgroundColor = textFieldColors.textSelectionColors.backgroundColor,
                        borderColor = textFieldColors.textSelectionColors.backgroundColor,
                        contentColor = colorResource(id = R.color.light_grey),
                        iconVector = Icons.Default.Done
                    )
                }
            )
            LaunchedEffect(Unit) {
                onClicked()
                textFieldState =
                    textFieldState.copy(selection = TextRange(textFieldState.text.length))
            }
        }
    }
}