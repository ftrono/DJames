package com.ftrono.DJames.application.dialogs

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.sp
import com.ftrono.DJames.R
import com.ftrono.DJames.application.overlayPermissionDescription
import com.ftrono.DJames.application.permissionDescriptions
import com.ftrono.DJames.application.permsRequested
import com.ftrono.DJames.application.runtimePermissions
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.ui.dialogs.GeneralDialog


@Composable
fun MultiPermissionsHandler(
    context: Context,
    startIndex: Int = 0
) {
    var showDialog = rememberSaveable { mutableStateOf(false) }
    var currentPermissionIndex by rememberSaveable { mutableStateOf(startIndex) }
    val currentPermission = runtimePermissions.getOrNull(currentPermissionIndex)

    // OnResult -> activate system permission request popup:
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && currentPermission != null) {
            Log.d("PermissionHandler", "Granted permission: $currentPermission")
        }
        showDialog.value = false
        currentPermissionIndex++
    }

    // Trigger the permission request loop:
    LaunchedEffect(currentPermissionIndex) {
        val perm = currentPermission
        if (perm != null) {
            if (!utils.checkPermission(context, perm)) {
                showDialog.value = true
            } else {
                Log.d("PermissionHandler", "Granted permission: $currentPermission")
                currentPermissionIndex++
            }
        } else {
            permsRequested.postValue(true)
        }
    }

    // INTRO REQUEST:
    if (showDialog.value && currentPermission != null) {
        GeneralDialog(
            dialogOnState = showDialog,
            backgroundColor = colorResource(id = R.color.colorPrimaryDark),
            title = "Permission Required",
            content = {
                Text(
                    permissionDescriptions[currentPermission] ?: "Permission required.",
                    color = colorResource(id = R.color.light_grey),
                    fontSize = 14.sp
                )
              },
            confirmText = "Yes",
            onConfirm = {
                launcher.launch(currentPermission)
            },
            dismissText = "Not now",
            onDismiss = {
                showDialog.value = false
                currentPermissionIndex++
            }
        )
    }
}


@Composable
fun SinglePermissionHandler(
    context: Context,
    dialogOnState: MutableState<Boolean>,
    permission: String
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("PermissionHandler", "Granted permission: $permission")
        }
        dialogOnState.value = false
    }

    LaunchedEffect(Unit) {
        if (utils.checkPermission(context, permission)) {
            dialogOnState.value = false
        }
    }

    if (dialogOnState.value) {
        GeneralDialog(
            dialogOnState = dialogOnState,
            backgroundColor = colorResource(id = R.color.colorPrimaryDark),
            title = "Permission Required",
            content = {
                Text(
                    permissionDescriptions[permission] ?: "Permission required.",
                    color = colorResource(id = R.color.light_grey),
                    fontSize = 14.sp
                )
            },
            confirmText = "Yes",
            onConfirm = {
                launcher.launch(permission)
            },
            dismissText = "Not now",
            onDismiss = {
                dialogOnState.value = false
            }
        )
    }
}

@Composable
fun DialogRequestOverlay(
    mContext: Context,
    dialogOnState: MutableState<Boolean>,
) {
    GeneralDialog(
        dialogOnState = dialogOnState,
        backgroundColor = colorResource(id = R.color.colorPrimaryDark),
        title = "Overlay permission",
        content = {
            Text(
                text = overlayPermissionDescription,
                color = colorResource(id = R.color.light_grey),
                fontSize = 14.sp
            )
        },
        dismissText = "Not now",
        confirmText = "Yes",
        onConfirm = {
            dialogOnState.value = false
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${mContext.packageName}")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            mContext.startActivity(intent)
        },
        onDismiss = {
            dialogOnState.value = false
        }
    )
}
