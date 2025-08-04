package com.ftrono.DJames.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ftrono.DJames.R
import com.ftrono.DJames.application.genders
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.spotUserImageState
import com.ftrono.DJames.application.spotifyLoggedIn
import com.ftrono.DJames.application.userNicknameState
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.ui.selectors.getTextFieldColors


// SETTINGS UI

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


@Composable
fun SpotifyLoginStatus(
    modifier: Modifier = Modifier,
    spotifyLoggedInState: Boolean,
    mContext: Context
) {
    //SPOTIFY LOGIN STATUS:
    Row (
        modifier = modifier
            .padding(bottom = 20.dp)
            .clickable {
                if (!spotifyLoggedInState) {
                    Toast
                        .makeText(
                            mContext,
                            "Log in from Settings to unlock music functions!",
                            Toast.LENGTH_LONG
                        )
                        .show()
                } else {
                    Toast
                        .makeText(
                            mContext,
                            "Logged in to Spotify as: ${prefs.spotUserName}!",
                            Toast.LENGTH_LONG
                        )
                        .show()
                }
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        //Spotify logo:
        Image(
            modifier = Modifier
                .width(30.dp)
                .height(30.dp),
            painter = painterResource(id = R.drawable.logo_spotify),
            contentDescription = "Spotify logo",
            colorFilter = if (!spotifyLoggedInState) {
                ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
            } else {
                ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1f) })
            }
        )
        //Logged in text:
        Text(
            text = if (spotifyLoggedInState) "LOGGED IN" else "NOT LOGGED IN",
            fontSize = 12.sp,
            color = colorResource(id = R.color.light_grey),
            modifier = Modifier
                .padding(start = 12.dp)
                .wrapContentWidth()
                .wrapContentHeight()
        )
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

    val mContext = LocalContext.current
    val isActive = rememberSaveable { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val spotifyLoggedInState by spotifyLoggedIn.observeAsState()
    val userImage by spotUserImageState.observeAsState()
    val genderState = rememberSaveable { mutableStateOf(if (preview) "Sir" else prefs.userGender) }

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
                    contentSize = 40,
                    backgroundColor = colorResource(id = R.color.faded_grey),
                    borderColor = colorResource(id = R.color.mid_grey),
                    contentColor = colorResource(id = R.color.light_grey),
                    borderWidth = 2.5.dp,
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

        //3) USER GENDER:
        Text(
            modifier = Modifier
                .padding(top = 8.dp),
            text = "Refer to yourself as",
            color = textHeaderColor,
            textAlign = TextAlign.Start,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        DropdownSpinner(
            context = mContext,
            parentOptions = genders,
            init = genderState.value,
            state = genderState,
            focusColorLight = colorResource(id = R.color.greenSignLight),
            focusColorDark = colorResource(id = R.color.greenSign),
            optionsBackground = colorResource(id = R.color.dark_grey),
            prefName = "userGender",
            width = 200
        )

    }
}

