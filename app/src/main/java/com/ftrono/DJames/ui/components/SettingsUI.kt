package com.ftrono.DJames.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ftrono.DJames.R
import com.ftrono.DJames.application.AuthActivity
import com.ftrono.DJames.application.genders
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.spotUserImageState
import com.ftrono.DJames.application.userNicknameUI
import com.ftrono.DJames.application.utils


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
            .fillMaxWidth(),
        border = BorderStroke(1.dp, colorResource(id = R.color.dark_grey)),
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
fun SpotifyLoginButton(
    modifier: Modifier = Modifier,
    context: Context,
    spotifyLoggedInState: Boolean,
    logoutDialogOn: MutableState<Boolean>,
) {
    //SPOTIFY LOGIN STATUS:
    //Logged in text:
    CardSign(
        modifier = modifier
            .padding(bottom = 20.dp)
            .clickable {
                if (!spotifyLoggedInState) {
                    //Login user -> Open WebView:
                    val intent1 = Intent(context, AuthActivity::class.java)
                    context.startActivity(intent1)
                } else {
                    //LOG OUT:
                    logoutDialogOn.value = true
                }
            },
        backgroundColor = if (spotifyLoggedInState) colorResource(R.color.faded_grey) else colorResource(R.color.colorPrimary),
        borderColor = colorResource(R.color.transparent_full),
        borderWidth = 0.dp,
    ) {
        Row (
            modifier = Modifier,
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            //Spotify logo:
            Image(
                modifier = Modifier
                    .padding(12.dp)
                    .width(20.dp)
                    .height(20.dp),
                painter = painterResource(id = R.drawable.logo_spotify),
                contentDescription = if (!spotifyLoggedInState) "Login to Spotify" else "Logout from Spotify",
                colorFilter = if (!spotifyLoggedInState) {
                    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                } else {
                    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1f) })
                }
            )
            //Text:
            Text(
                text = if (!spotifyLoggedInState) "Login to Spotify" else "Logout from Spotify",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.light_grey),
                modifier = Modifier
                    .padding(end=12.dp),
            )
        }
    }
}


@Preview
@Composable
fun SettingsUserSectionPreview() {
    val logoutDialogOn = remember { mutableStateOf(false) }
    val spotifyLoggedInState = remember { mutableStateOf(false) }
    SettingsUserSection(
        modifier = Modifier,
        spotifyLoggedInState = spotifyLoggedInState.value,
        logoutDialogOn = logoutDialogOn,
        preview = true
    )
}


@Composable
fun SettingsUserSection(
    modifier: Modifier = Modifier,
    spotifyLoggedInState: Boolean,
    logoutDialogOn: MutableState<Boolean>,
    preview: Boolean = false,
) {
    val mContext = LocalContext.current
    val textHeaderColor = colorResource(id = R.color.light_grey)
    val userImage by spotUserImageState.observeAsState()
    val userNicknameUIState by userNicknameUI.observeAsState()
    val genderState = rememberSaveable { mutableStateOf(if (preview) "Sir" else prefs.userGender) }
    val textNickName = rememberSaveable { mutableStateOf(if (preview || userNicknameUI.value!! == "") genderState.value else userNicknameUI.value!!) }

    fun onClicked() {
        // Store new nickname to prefs!:
        if (textNickName.value != "") {
            textNickName.value = utils.cleanString(textNickName.value)
            userNicknameUI.value = textNickName.value
            prefs.userNickname = textNickName.value
        } else {
            textNickName.value = userNicknameUI.value!!
        }
    }
    
    LaunchedEffect(userNicknameUIState!!) {
        if (!preview) {
            if (userNicknameUI.value!! == "") {
                textNickName.value = genderState.value
            } else {
                textNickName.value = userNicknameUI.value!!
            }
        }
    }

    SettingsSection(
        modifier = modifier
            .padding(end=8.dp, bottom=4.dp)
    ) {
        //1) LOGIN STATUS:
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpotifyLoginButton(
                modifier = Modifier,
                context = mContext,
                spotifyLoggedInState = spotifyLoggedInState!!,
                logoutDialogOn = logoutDialogOn,
            )
        }

        //Title:
        Text(
            modifier = Modifier
                .padding(top=4.dp, bottom = 8.dp),
            text = "Write a nickname for you",
            color = textHeaderColor,
            textAlign = TextAlign.Start,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        //USER NICKNAME:
        EditLibDynamicNameSection(
            filter = "user",
            textState = textNickName,
            imageUrl = userImage!!,
            preview = preview,
            onClick = { onClicked() }
        )

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

