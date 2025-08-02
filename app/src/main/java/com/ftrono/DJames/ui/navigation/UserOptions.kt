package com.ftrono.DJames.ui.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.coroutineScope
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.ftrono.DJames.R
import com.ftrono.DJames.application.AuthActivity
import com.ftrono.DJames.application.lastNavRoute
import com.ftrono.DJames.application.settingsOpen
import com.ftrono.DJames.application.showLoggingIn
import com.ftrono.DJames.application.spotifyLoggedIn
import com.ftrono.DJames.application.spotifyLoginUtils
import com.ftrono.DJames.ui.components.OptionsItem
import com.ftrono.DJames.ui.components.OptionsMenu
import com.ftrono.DJames.ui.dialogs.DialogLoading
import com.ftrono.DJames.ui.dialogs.GeneralDialog
import com.ftrono.DJames.ui.theme.NavigationItem


//USER OPTIONS:
@Composable
fun UserOptions(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    navController: NavController,
) {
    val settingsOpenState by settingsOpen.observeAsState()
    val spotifyLoggedInState by spotifyLoggedIn.observeAsState()

    var mDisplayMenu = rememberSaveable {
        mutableStateOf(false)
    }

    val logoutDialogOn = rememberSaveable { mutableStateOf(false) }
    if (logoutDialogOn.value) {
        DialogLogout(
            context,
            logoutDialogOn,
            navController,
            settingsOpenState!!
        )
    }

    val dialogLoggingInOn by showLoggingIn.observeAsState()
    if (dialogLoggingInOn!!) {
        DialogLoading(
            text = "Logging in to Spotify..."
        )
        spotifyLoginUtils.getSpotifyUserData(
            context = context,
            navController = navController,
            scope = lifecycleOwner.lifecycle.coroutineScope
        )
    }

    //SETTINGS BUTTON:
    Icon(
        modifier = Modifier
            .padding(end = 12.dp)
            .clickable {
                //Navigate:
                val curNavRoute = NavigationItem.Settings.route
                if (curNavRoute == lastNavRoute && (settingsOpenState!!)) {
                    navController.popBackStack()
                } else {
                    navigateTo(navController, curNavRoute)
                }
                lastNavRoute = curNavRoute
            },
        painter = painterResource(id = R.drawable.item_settings),
        contentDescription = "",
        tint = if (settingsOpenState!!) {
            colorResource(id = R.color.colorAccentLight)
        } else {
            colorResource(id = R.color.light_grey)
        }
    )

    //"MORE OPTIONS" BUTTON:
    Icon(
        modifier = Modifier
            .padding(end = 14.dp)
            .clickable { mDisplayMenu.value = !mDisplayMenu.value },
        imageVector = Icons.Default.MoreVert,
        contentDescription = "",
        tint = colorResource(id = R.color.light_grey)
    )

    //DROPDOWN MENU:
    OptionsMenu(
        expandedState = mDisplayMenu,
        backgroundColor = colorResource(id = R.color.dark_grey_background),
        options = {
            //1) Item: LOGIN/LOGOUT
            OptionsItem(
                title = if (!spotifyLoggedInState!!) "Login to Spotify" else "Logout from Spotify",
                iconPainter = painterResource(id = R.drawable.item_user),
                onClick = {
                    if (!spotifyLoggedInState!!) {
                        //Login user -> Open WebView:
                        val intent1 =
                            Intent(context, AuthActivity::class.java)
                        context.startActivity(intent1)
                    } else {
                        //LOG OUT:
                        logoutDialogOn.value = true
                    }
                    mDisplayMenu.value = false
                }
            )
            //2) Item: VOICE SETTINGS
            OptionsItem(
                title = "Voice settings",
                iconPainter = painterResource(id = R.drawable.icon_speak),
                onClick = {
                    //Set app preferences:
                    val intent1 = Intent("com.android.settings.TTS_SETTINGS")
                    context.startActivity(intent1)
                    mDisplayMenu.value = false
                }
            )
            //3) Item: PERMISSIONS
            OptionsItem(
                title = "Permissions",
                iconPainter = painterResource(id = R.drawable.item_permissions),
                onClick = {
                    //Set app preferences:
                    val intent1 = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", context.packageName, null)
                    intent1.setData(uri)
                    context.startActivity(intent1)
                    mDisplayMenu.value = false
                }
            )
        }
    )
}


@Composable
fun DialogLogout(
    context: Context,
    dialogOnState: MutableState<Boolean>,
    navController: NavController,
    settingsOpenState: Boolean
) {
    GeneralDialog(
        dialogOnState = dialogOnState,
        backgroundColor = colorResource(id = R.color.colorPrimaryDark),
        title = "Logout",
        content = {
            Text(
                text = "You will need to login again to Spotify to use DJames.\n\nDo you want to log out?",   // and you'll lose your saved library & message history
                color = colorResource(id = R.color.light_grey),
                fontSize = 14.sp
            )
        },
        dismissText = "No",
        confirmText = "Yes",
        onConfirm = {
            spotifyLoginUtils.logout(
                context,
                navController,
                settingsOpenState
            )
            dialogOnState.value = false
        }
    )
}
