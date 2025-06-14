package com.ftrono.DJames.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ftrono.DJames.R
import com.ftrono.DJames.application.genderMaleState
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.spotUserImageState
import com.ftrono.DJames.application.spotifyLoggedIn
import com.ftrono.DJames.application.userNicknameState
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.application.screens.SpotifyLoginStatus
import com.ftrono.DJames.ui.selectors.getTextFieldColors


// SETTINGS UI

@Preview
@Composable
fun SettingsHeaderPreview() {
    SettingsHeader(
        backClickable = {},
        iconRes = painterResource(id = R.drawable.sign_preferences),
        title = "Preferences",
        options = {
            //SAVE BUTTON:
            Icon(
                modifier = Modifier
                    .padding(end = 18.dp)
                    .size(35.dp),
                imageVector = Icons.Default.Check,
                contentDescription = "Save",
                tint = colorResource(id = R.color.colorAccentLight)
            )
        }
    )
}


@Composable
fun SettingsHeader(
    backClickable: () -> Unit,
    iconRes: Painter,
    title: String,
    signColor: Color = colorResource(id = R.color.colorPrimary),
    options: @Composable () -> Unit
) {
    //HEADER:
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(colorResource(id = R.color.windowBackground)),
        contentAlignment = Alignment.Center
    ) {
        //HEADER CONTENT:
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            //BACK:
            Icon(
                modifier = Modifier
                    .padding(start = 12.dp, end = 4.dp)
                    .size(32.dp)
                    .clickable {
                        backClickable()
                    },
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = colorResource(id = R.color.colorAccentLight)
            )
            //MAIN HEADER SIGN:
            HeaderSign(
                modifier = Modifier
                    .padding(10.dp)
                    .wrapContentSize(align = Alignment.TopStart),
                iconRes = iconRes,
                title = title,
                signColor = signColor
            )
            //OPTIONS BUTTONS:
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ){
                options()
            }
        }
    }
}


@Composable
fun SettingsSection(
    modifier: Modifier = Modifier,
    title: String = "",
    signColor: Color? = null,
    iconPainter: Painter? = null,
    content: @Composable () -> Unit
) {
    //TITLE:
    if (title != "") {
        SectionTitle(
            modifier = modifier
                .padding(end = 8.dp, top = 16.dp, bottom = 8.dp),
            title = title,
            signColor = signColor!!,
            iconPainter = iconPainter!!
        )
    }
    CardContainer() {
        content()
    }
}


@Composable
fun CardContainer(
    modifier: Modifier = Modifier,
    internalPadding: Dp = 20.dp,
    roundedCorners: Dp = 20.dp,
    containerColor: Color = colorResource(id = R.color.dark_grey_background),
    content: @Composable () -> Unit
) {
    //CARD:
    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        border = BorderStroke(1.dp, colorResource(id = R.color.faded_grey)),
        shape = RoundedCornerShape(roundedCorners),
        colors = CardDefaults.cardColors (
            containerColor = containerColor
        )
    ) {
        //SETTINGS LIST:
        Column(
            modifier = Modifier
                .padding(internalPadding),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
        ) {
            content()
        }
    }
}


@Preview
@Composable
fun SettingsUserSectionPreview() {
    SettingsUserSection(
        modifier = Modifier
            .padding(bottom = 4.dp),
        textHeaderColor = colorResource(id = R.color.greenSignLight),
        textFieldColors = getTextFieldColors(
            colorLight = colorResource(id = R.color.greenSignLight),
            colorDark = colorResource(id = R.color.greenSign)
        ),
        preview = true
    )
}


@Composable
fun SettingsUserSection(
    modifier: Modifier = Modifier,
    textHeaderColor: Color,
    textFieldColors: TextFieldColors,
    preview: Boolean = false,
) {
    val headerText = "Write your nickname"
    val radioButtonColors = RadioButtonDefaults.colors(
        selectedColor = colorResource(id = R.color.greenSignLight),
        unselectedColor = colorResource(id = R.color.faded_grey)
    )

    val mContext = LocalContext.current
    val isActive = rememberSaveable { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val spotifyLoggedInState by spotifyLoggedIn.observeAsState()
    val userImage by spotUserImageState.observeAsState()
    val genderMale = remember { mutableStateOf(genderMaleState.value) }

    var textFieldState by remember { mutableStateOf(TextFieldValue(userNicknameState.value!!)) }
    val shownTextState = remember { mutableStateOf(if (userNicknameState.value == "") "User" else userNicknameState.value) }
    var beforeText = shownTextState.value

    fun onClicked() {
        focusRequester.requestFocus()
        keyboardController!!.show()
    }

    fun onKeyboardDone() {
        if (!preview && textFieldState.text != "") {
            prefs.userNickname = utils.cleanString(textFieldState.text)
            userNicknameState.postValue(textFieldState.text)
            shownTextState.value = textFieldState.text
            beforeText = textFieldState.text
        } else {
            shownTextState.value = beforeText
            userNicknameState.postValue(beforeText)
        }
        focusManager.clearFocus()
        keyboardController!!.hide()
    }

    SettingsSection(
        modifier = Modifier
            .padding(end=8.dp, bottom=4.dp)
    ) {

        //1) LOGIN STATUS:
        SpotifyLoginStatus(
            modifier = Modifier
                .fillMaxWidth(),
            spotifyLoggedInState!!,
            mContext
        )

        //2) USER NICKNAME & PROFILE PIC:
        if (!isActive.value) {
            Row(
                modifier = Modifier
                    .padding(bottom = 10.dp)
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
                    iconSize = 40.dp,
                    backgroundColor = colorResource(id = R.color.faded_grey),
                    borderColor = colorResource(id = R.color.midfaded_grey),
                    iconColor = colorResource(id = R.color.light_grey),
                    iconVector = Icons.Outlined.Person,
                    imageUrl = userImage!!,
                    circle = true
                )

                Column (
                    modifier = Modifier
                        .padding(start = 20.dp)
                        .clickable {
                            isActive.value = true
                            onClicked()
                        },
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {

                    //Title:
                    EditLibTitle(
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .clickable {
                                isActive.value = true
                                onClicked()
                            },
                        textHeaderColor = textHeaderColor,
                        fontSize = 16.sp,
                        title = headerText,
                    )

                    Row(
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        //Name:
                        Text(
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .clickable {
                                    isActive.value = true
                                    onClicked()
                                },
                            text = shownTextState.value!!,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(id = R.color.light_grey)
                        )

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
            }
        } else {
            //Title:
            EditLibTitle(
                textHeaderColor = textHeaderColor,
                fontSize = 16.sp,
                title = headerText,
            )

            //TextField:
            OutlinedTextField(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 20.dp)
                    .focusRequester(focusRequester),
                colors = textFieldColors,
                value = textFieldState,
                onValueChange = { newText ->
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
                        text = "Write user nickname...",
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
                textFieldState =
                    textFieldState.copy(selection = TextRange(textFieldState.text.length))
            }
        }

        //3) USER GENDER:
        Row (
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = genderMale.value!!,
                onClick = {
                    genderMale.value = !genderMale.value!!
                    genderMaleState.postValue(genderMale.value)
                    if (!preview) {
                        prefs.genderMale = genderMale.value!!
                    }
                  },
                colors = radioButtonColors
            )
            Text(
                modifier = Modifier
                    .padding(end = 30.dp),
                text = "Sir",
                fontSize = 16.sp,
                color = colorResource(id = R.color.light_grey)
            )

            RadioButton(
                selected = !genderMale.value!!,
                onClick = {
                    genderMale.value = !genderMale.value!!
                    genderMaleState.postValue(genderMale.value)
                    if (!preview) {
                        prefs.genderMale = genderMale.value!!
                    }
                  },
                colors = radioButtonColors
            )
            Text(
                modifier = Modifier
                    .padding(end = 20.dp),
                text = "Madam",
                fontSize = 16.sp,
                color = colorResource(id = R.color.light_grey)
            )
        }
    }
}

