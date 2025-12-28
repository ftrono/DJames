package com.ftrono.DJames.ui.navigation

import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.coroutineScope
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.ftrono.DJames.R
import com.ftrono.DJames.application.lastNavRoute
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.extraOpen
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
    preview: Boolean = false
) {
    val extraOpenState by extraOpen.observeAsState()
    val spotifyLoggedInState by spotifyLoggedIn.observeAsState()
    val checkedV3 = remember { mutableStateOf(if (preview) true else prefs.enableV3) }

    var mDisplayMenu = rememberSaveable {
        mutableStateOf(false)
    }

    val logoutDialogOn = rememberSaveable { mutableStateOf(false) }
    if (logoutDialogOn.value) {
        DialogLogout(
            context,
            logoutDialogOn,
            navController,
            extraOpenState!!
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

    // USER OPTIONS MENU:
    TopBarMenu(
        imageUrl = if (!preview) prefs.spotUserImage else "",
        iconVector = if (preview || prefs.spotUserImage == "") Icons.Outlined.Person else null,
        onClick = { mDisplayMenu.value = !mDisplayMenu.value },
    ) {
        //DROPDOWN MENU:
        OptionsMenu(
            expandedState = mDisplayMenu,
            backgroundColor = colorResource(id = R.color.dark_grey_background),
            options = {
                //1) Item: ACCOUNTS
                OptionsItem(
                    title = "Accounts",
                    iconPainter = painterResource(id = R.drawable.icon_user),
                    onClick = {
                        //Navigate:
                        val curNavRoute = NavigationItem.Accounts.route
                        if (curNavRoute == lastNavRoute && (extraOpenState!!)) {
                            navController.popBackStack()
                        } else {
                            navigateTo(navController, curNavRoute)
                        }
                        lastNavRoute = curNavRoute
                        mDisplayMenu.value = false
                    }
                )
                //2) Item: PREFERENCES
                OptionsItem(
                    title = "Preferences",
                    iconPainter = painterResource(id = R.drawable.icon_settings),
                    onClick = {
                        //Navigate:
                        val curNavRoute = NavigationItem.Settings.route
                        if (curNavRoute == lastNavRoute && (extraOpenState!!)) {
                            navController.popBackStack()
                        } else {
                            navigateTo(navController, curNavRoute)
                        }
                        lastNavRoute = curNavRoute
                        mDisplayMenu.value = false
                    }
                )
                //3) TODO: TEMP: Item: Enable / disable V3 engine:
                OptionsItem(
                    title = "Enable v3 engine",
                    iconVector = Icons.Default.Check,
                    showIcon = checkedV3.value,
                    onClick = {
                        prefs.enableV3 = !checkedV3.value
                        checkedV3.value = !checkedV3.value
                    }
                )
            }
        )
    }
}


@Composable
fun DialogLogout(
    context: Context,
    dialogOnState: MutableState<Boolean>,
    navController: NavController,
    extraOpenState: Boolean
) {
    GeneralDialog(
        dialogOnState = dialogOnState,
        backgroundColor = colorResource(id = R.color.colorPrimaryDark),
        title = "Logout",
        content = {
            Text(
                text = "You will need to login again to Spotify to play music via DJames.\n\nDo you want to log out?",
                color = colorResource(id = R.color.light_grey),
                fontSize = 14.sp
            )
        },
        dismissText = "No",
        confirmText = "Yes",
        onConfirm = {
            spotifyLoginUtils.logout(context)
            dialogOnState.value = false
        }
    )
}
