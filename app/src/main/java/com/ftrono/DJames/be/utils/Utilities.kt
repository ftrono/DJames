package com.ftrono.DJames.be.utils

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.telephony.PhoneNumberUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.ftrono.DJames.BuildConfig
import com.ftrono.DJames.application.ACTION_MAKE_CALL
import com.ftrono.DJames.application.ClockActivity
import com.ftrono.DJames.application.libCats
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.messageUtils
import com.ftrono.DJames.application.overlayActive
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.recDir
import com.ftrono.DJames.application.services.OverlayService
import com.ftrono.DJames.be.models.languageNamesMap
import com.ftrono.DJames.be.models.languageWordsMap
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random
import kotlin.streams.asSequence
import androidx.core.content.edit
import com.ftrono.DJames.application.userGender
import com.ftrono.DJames.application.userNicknameUI


class Utilities {
    private val TAG = this::class.java.simpleName

    //Timestamps:
    fun getCurrentTimestamp(): Long {
        return System.currentTimeMillis()
    }

    fun convertTimestamp(timestamp: Long, formatStr: String): String {
        val datetimeStr = SimpleDateFormat(formatStr, Locale.getDefault()).format(Date(timestamp))
        return datetimeStr
    }

    //Check service running:
    fun isMyServiceRunning(serviceClass: Class<*>, context: Context): Boolean {
        val manager = context.getSystemService(AppCompatActivity.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    //Check Manifest permission:
    fun checkPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    //ID creator:
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

    //Capitalize each word in a sentence:
    fun capitalizeWords(text: String, delimiter: String = " "): String {
        return text.split(delimiter).joinToString(delimiter) { it.lowercase().replaceFirstChar(Char::titlecaseChar) }
    }

    //Clean string keeping only alphanumeric characters:
    fun cleanString(text: String, emojiOnly: Boolean = false, capitalize: Boolean = true): String {
        //val re = Regex("[^A-Za-z0-9 ]")
        var re: Regex? = null
        if (emojiOnly) {
            re = Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+")
        } else {
            re = Regex("[\\p{P}\\p{S}]|[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+")
        }
        val cleaned = re.replace(text, " ").replace(Regex("\\s+"), " ").trim()
        return if (capitalize) capitalizeWords(cleaned).trim() else cleaned.trim()
    }

    //Check if string is made of alphabetic characters:
    fun isLetters(string: String): Boolean {
        return string.all { it.isLetter() }
    }

    //Validate contact phone (i.e. no phone number, no international prefix in phone number):
    fun isGlobalPhone(prefix: String, phone: String): Boolean {
        val phoneTest = PhoneNumberUtils.isGlobalPhoneNumber(phone)
        //True if conditions are met:
        //(Phone numbers length is 10 digits (Italy) or 11 digits (UK), + international prefix (3 digits)):
        return (phoneTest && (prefix.contains("+") && prefix.length == 3) && (phone.length == 10 || phone.length == 11))
    }

    //Trim strings:
    fun trimString(textOrig: String, maxLength: Int = 30): String {
        var textTrimmed = textOrig
        if (textOrig.length > (maxLength+3)) {
            textTrimmed = textOrig.slice(0..(maxLength)) + "..."
        }
        return textTrimmed
    }

    //Make call:
    fun makeCall(context: Context, contactPhone: String, fromService: Boolean = false) {
        if (fromService) {
            Intent().also { intent ->
                intent.setAction(ACTION_MAKE_CALL)
                intent.putExtra("toCall", "tel:$contactPhone")
                context.sendBroadcast(intent)
            }
        } else {
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$contactPhone")
            }
            context.startActivity(callIntent)
        }
    }

    //Open link externally:
    fun openLink(context: Context, url: String, fromService: Boolean = false) {
        //Intent view:
        Log.d(TAG, "Opening URL: $url")
        val intent = Intent(
            Intent.ACTION_VIEW,
            url.toUri()
        )
        if (fromService) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("fromwhere", "ser")
        }
        context.startActivity(intent)
    }

    //UI update:
    fun updateStatesMap(statesMap: SnapshotStateMap<String, Boolean>, target: String): SnapshotStateMap<String, Boolean> {
        for (k in statesMap.keys) {
            if (k == target) {
                statesMap[k] = true
            } else {
                statesMap[k] = false
            }
        }
        return statesMap
    }

    //From a detected language name, get the supported language code:
    fun getLanguageCode(language: String, default: String = ""): String {
        var reqLangCode = default
        if (languageWordsMap.contains(language.trim())) {
            reqLangCode = languageWordsMap[language.trim()]!!
            Log.d(TAG, "REQUESTED LANGUAGE: $reqLangCode")
        } else {
            Log.d(TAG, "No requested language found in the voice request.")
        }
        return reqLangCode
    }

    //Get the corresponding language name in preferred language for the detected language code:
    fun getLanguageName(languageCode: String): String {
        return languageNamesMap[prefs.queryLanguage]!![languageCode]!!
    }

    //Start/Stop DRIVE Mode:
    fun startStopDriveMode(
        context: Context,
        requestOverlayOn: MutableState<Boolean>,
        requestPermissions: MutableState<Boolean>,
        openClock: Boolean = false,
    ) {
        if (overlayActive.value == false) {
            if (!Settings.canDrawOverlays(context)) {
                // REQUEST OVERLAY PERMISSION:
                requestOverlayOn.value = true
                overlayActive.postValue(false)

            } else if (!checkPermission(context, Manifest.permission.RECORD_AUDIO)) {
                Log.d("Home", "${checkPermission(context, Manifest.permission.RECORD_AUDIO)}")
                // REQUEST MISSING PERMISSIONS:
                requestPermissions.value = true

            } else {
                //START DRIVE MODE:
                requestOverlayOn.value = false
                overlayActive.postValue(true)
                //Overlay service:
                if (!isMyServiceRunning(OverlayService::class.java, context)) {
                    var intentOS = Intent(context, OverlayService::class.java)
                    context.startService(intentOS)
                    if (openClock) {
                        if (prefs.volumeUpEnabled) {
                            Toast.makeText(
                                context,
                                "Use the OVERLAY or VOLUME UP / SHUTTER button to speak!",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Use the OVERLAY button to speak!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                //Start Clock screen:
                if (openClock) {
                    val intent1 = Intent(context, ClockActivity::class.java)
                    context.startActivity(intent1)
                }
            }
        } else {
            //STOP DRIVE MODE:
            overlayActive.postValue(false)
            if (isMyServiceRunning(OverlayService::class.java, context)) {
                context.stopService(Intent(context, OverlayService::class.java))
            }
        }
    }

    //FILE MANAGEMENT:
    //Send a cached file:
    fun sendCachedFile(context: Context, filename: String) {
        if (filename != "") {
            //Get cached file:
            val file = File(context.cacheDir, filename)
            val uriToFile = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", file)
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uriToFile)
                type = "image/jpeg"
            }
            var chooserIntent = Intent.createChooser(sendIntent, null)
            chooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            chooserIntent.putExtra("fromwhere", "ser")
            ContextCompat.startActivity(context, chooserIntent, null)
        } else {
            Toast.makeText(context, "ERROR: cannot send the requested file!", Toast.LENGTH_LONG).show()
        }
    }


    // PREFS EXPORT / IMPORT:
    fun exportSharedPreferences(context: Context): String {
        val shPrefs: SharedPreferences =
            context.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)

        val allEntries = shPrefs.all
        val json = JSONObject()

        for ((key, value) in allEntries) {
            when (value) {
                is Boolean, is Int, is Long, is Float, is String -> {
                    json.put(key, value)
                }
            }
        }

        return json.toString()
    }

    fun importSharedPreferences(
        context: Context,
        jsonString: String
    ) {
        val shPrefs = context.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
        shPrefs.edit {
            val json = JSONObject(jsonString)

            json.keys().forEach { key ->
                when (val value = json.get(key)) {
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putFloat(key, value.toFloat())
                    is String -> putString(key, value)
                }
            }

        }

        // Profile info:
        userNicknameUI.postValue(prefs.userNickname)
        userGender.postValue(prefs.userGender)
    }


    //CLEANING:
    //Clean cached recordings:
    fun cleanRecordingsCache(context: Context) {
        try {
            //Delete all:
            recDir!!.deleteRecursively()
            //Re-init recordings directory:
            recDir = File(context.cacheDir, "recordings")
            recDir!!.mkdirs()
            Log.d(TAG, "Recordings cache deleted.")
        } catch (e: Exception) {
            Log.w(TAG, "Recordings cache not deleted.")
        }
    }

    //On Logout: delete user Library & Messages files:
    fun deleteUserData(context: Context) {
        try {
            messageUtils.deleteAllMessages(context)
            for (cat in libCats) {
                libUtils.deleteLibrary(context, cat)
            }
        } catch (e: Exception) {
            Log.w(TAG, "User data not completely deleted.")
        }
    }

}