package com.ftrono.DJames.utilities

import android.content.Context
import android.graphics.Typeface
import android.provider.Settings
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import com.ftrono.DJames.R
import com.ftrono.DJames.application.descr_main
import com.ftrono.DJames.application.descr_use
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.startButton
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.Random
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.streams.asSequence


class Utilities {
    private val TAG = Utilities::class.java.simpleName

    suspend fun makeRequest(client: OkHttpClient, request: Request): String = suspendCoroutine { continuation ->
        client.newCall(request).enqueue(object: Callback {
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response.body!!.string())
            }

            override fun onFailure(call: Call, e: IOException) {
                continuation.resume("")
                Log.d(TAG, "RESPONSE ERROR: ", e)
            }
        })
    }

    fun generateRandomString(length: Int, numOnly: Boolean = false): String {
        var source = ""
        if (numOnly) {
            source = "0123456789"
        } else {
            source = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        }

        return Random().ints(length.toLong(), 0, source.length)
            .asSequence()
            .map(source::get)
            .joinToString("")
    }

    //Set Overlay Active view in Main:
    fun setOverlayActive(context: Context): Boolean {
        try {
            if (Settings.canDrawOverlays(context)) {
                startButton!!.text = "S T O P"
                startButton!!.backgroundTintList = AppCompatResources.getColorStateList(context, R.color.colorStop)
                descr_main!!.setTextColor(AppCompatResources.getColorStateList(context, R.color.colorHeader))
                descr_main!!.setTypeface(null, Typeface.BOLD_ITALIC)
                descr_main!!.text = context.resources.getString(R.string.str_main_stop)
                if (prefs.volumeUpEnabled) {
                    descr_use!!.text = context.resources.getString(R.string.str_use_logged)
                } else {
                    descr_use!!.text = context.resources.getString(R.string.str_use_logged_no_vol)
                }
                descr_use!!.setTextColor(AppCompatResources.getColorStateList(context, R.color.light_grey))
            }
            Log.d(TAG, "SetOverlayActive()")
        } catch (e: Exception) {
            Log.d(TAG, "SetOverlayActive(): resources not available.")
        }
        return true
    }

    //Set Overlay Inactive view in Main:
    fun setOverlayInactive(context: Context): Boolean {
        try {
            startButton!!.text = "S T A R T"
            startButton!!.backgroundTintList = AppCompatResources.getColorStateList(context, R.color.colorAccent)
            descr_main!!.setTextColor(AppCompatResources.getColorStateList(context, R.color.light_grey))
            descr_main!!.setTypeface(null, Typeface.ITALIC)
            descr_main!!.text = context.resources.getString(R.string.str_main_start)
            if (prefs.volumeUpEnabled) {
                descr_use!!.text = context.resources.getString(R.string.str_use_logged)
            } else {
                descr_use!!.text = context.resources.getString(R.string.str_use_logged_no_vol)
            }
            descr_use!!.setTextColor(AppCompatResources.getColorStateList(context, R.color.mid_grey))
            Log.d(TAG, "SetOverlayInactive()")
        } catch (e: Exception) {
            Log.d(TAG, "SetOverlayInactive(): resources not available.")
        }
        return false
    }

}