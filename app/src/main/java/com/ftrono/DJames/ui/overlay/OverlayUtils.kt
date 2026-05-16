package com.ftrono.DJames.ui.overlay

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ftrono.DJames.R
import com.ftrono.DJames.application.ACTION_FINISH_MAIN
import com.ftrono.DJames.application.ACTION_SAVE_TRACK
import com.ftrono.DJames.application.ACTION_TOASTER
import com.ftrono.DJames.application.ClockActivity
import com.ftrono.DJames.application.overlayPos
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.queryStatus
import com.ftrono.DJames.application.services.VoiceQueryService
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.application.volumeUpEnabledUI
import com.ftrono.DJames.be.models.QuickAction


// Calculate toes arc (in degrees):
fun getToesPositions(
    size: Int,
    interval: Float,
    posRight: Boolean = false,
    bottomDocked: Boolean = false
): List<Float> {
    val sizeMultipliers = mapOf(
        2 to 0.5f,
        3 to 1f,
        4 to 1.5f,
        5 to 2f
    )
    val center = if (bottomDocked) 270f else if (posRight) 180f else 0f
    var lowerBound = center - interval * sizeMultipliers[size]!!
    val positions = mutableListOf<Float>()

    for (i in 0 ..< size) {
        var temp = lowerBound + i * interval
        if (temp < 0f) {
            temp = temp + 360f
        }
        positions.add(temp)
    }
    if (posRight && !bottomDocked) positions.sortDescending()
    return positions
}


fun getQuickActionOnTap(
    context: Context,
    name: String
): () -> Unit {
    val TAG = "OverlayService"
    return if (name == "speak") {
        {
            //TRIGGER VOICE REQUEST:
            try {
                context.startService(Intent(context, VoiceQueryService::class.java))
                Log.d(TAG, "OVERLAY SERVICE: VOICE QUERY SERVICE STARTED.")
            } catch (e:Exception) {
                Log.w(TAG, "ERROR: OVERLAY SERVICE: VOICE QUERY SERVICE NOT STARTED. ", e)
            }
        }
    } else if (name == "save") {
        {
            //TRIGGER SAVE TRACK:
            queryStatus.postValue("processing")
            Intent().also { intent ->
                intent.setAction(ACTION_SAVE_TRACK)
                context.sendBroadcast(intent)
            }
        }
    } else if (name == "clock") {
        {
            //Start Clock screen:
            utils.openActivity(context, ClockActivity::class.java, fromService = true)
            //End Main():
            //Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_FINISH_MAIN)
                context.sendBroadcast(intent)
            }
        }
    } else if (name == "volume") {
        {
            if (volumeUpEnabledUI.value!!) {   //THIS must not change!
                // Disable volume-up trigger temporarily:
                queryStatus.postValue("volume")
                prefs.volumeUpEnabled = false   //THIS is used by EventReceiver!
                Toast.makeText(context, "Raise volume now!", Toast.LENGTH_SHORT).show()
            }
        }
    } else if (name == "pos") {
        {
            if (overlayPos.value == "Right") overlayPos.value = "Left" else overlayPos.value = "Right"
        }
    } else {
        {
            //TRIGGER ENABLE/DISABLE USE EXPERIMENTAL SETTING:
//            val modeToTrigger = if (prefs.enableV3) "OFF" else "ON"
//            prefs.enableV3 = !prefs.enableV3
//            //TOAST -> Send broadcast:
//            Intent().also { intent ->
//                intent.setAction(ACTION_TOASTER)
//                intent.putExtra("toastText", "V3 mode: $modeToTrigger")
//                context.sendBroadcast(intent)
//            }
        }
    }
}


@Composable
fun getQuickAction(
    name: String,
    isActive: Boolean,
    colorActive: Color,
    colorInactive: Color,
    currentTimeState: String,
    overlayPosState: String,
): QuickAction {
    return if (name == "speak") {
        QuickAction(
            id = "speak",
            title = "Ask",
            content = {
                Icon(
                    modifier = Modifier
                        .size(34.dp),
                    painter = painterResource(id = R.drawable.icon_mic),
                    tint = if (isActive) colorActive else colorInactive,
                    contentDescription = "Ask"
                )
            }
        )
    } else if (name == "save") {
        QuickAction(
            id = "save",
            title = "Save",
            content = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        modifier = Modifier
                            .offset(x = 6.dp)
                            .size(28.dp),
                        painter = painterResource(R.drawable.logo_spotify),
                        tint = if (isActive) colorActive else colorInactive,
                        contentDescription = "Save track to Spotify"
                    )
                    Icon(
                        modifier = Modifier
                            .offset(x = 0.dp, y = 10.dp)
                            .size(22.dp),
                        imageVector = Icons.Default.Add,
                        tint = if (isActive) colorActive else colorInactive,
                        contentDescription = "Save track to Spotify"
                    )
                }
            },
        )
    } else if (name == "clock") {
        QuickAction(
            id = "clock",
            title = "Clock",
            content = {
                Text(
                    modifier = Modifier,
                    text = currentTimeState,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) colorActive else colorInactive,
                )
            },
        )
    } else if (name == "volume") {
        QuickAction(
            id = "volume",
            title = "Volume",
            content = {
                Icon(
                    modifier = Modifier
                        .size(34.dp),
                    painter = painterResource(R.drawable.icon_lock),
                    tint = if (isActive) colorActive else colorInactive,
                    contentDescription = "Raise Volume"
                )
            },
        )
    } else {
        QuickAction(
            id = "pos",
            title = "Move",
            content = {
                Icon(
                    modifier = Modifier
                        .size(34.dp),
                    imageVector = if (overlayPosState == "Right") Icons.AutoMirrored.Default.ArrowBack else Icons.AutoMirrored.Default.ArrowForward,
                    tint = if (isActive) colorActive else colorInactive,
                    contentDescription = "Raise Volume"
                )
            },
        )
    }
}