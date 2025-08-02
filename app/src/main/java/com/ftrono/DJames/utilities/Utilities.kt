package com.ftrono.DJames.utilities

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.telephony.PhoneNumberUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import com.ftrono.DJames.application.*
import com.ftrono.DJames.be.models.HttpResponse
import androidx.core.net.toUri
import com.ftrono.DJames.application.services.OverlayService
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.util.Random
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.streams.asSequence
import com.ftrono.DJames.be.models.languageNamesMap
import com.ftrono.DJames.be.models.languageWordsMap


class Utilities {
    private val TAG = Utilities::class.java.simpleName

    //OkHTTP: make HTTP request:
    suspend fun makeRequest(client: OkHttpClient, request: Request): HttpResponse = suspendCoroutine { continuation ->
        client.newCall(request).enqueue(object: Callback {
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(
                    HttpResponse(
                        code = response.code,
                        body = response.body!!.string()
                    )
                )
            }

            override fun onFailure(call: Call, e: IOException) {
                continuation.resume(
                    HttpResponse(
                        code = -1,  // -1 to indicate failure
                        body = ""
                    )
                )
                Log.d("TAG", "RESPONSE ERROR: ", e)
            }
        })
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
    fun cleanString(text: String, emojiOnly: Boolean = false): String {
        //val re = Regex("[^A-Za-z0-9 ]")
        var re: Regex? = null
        if (emojiOnly) {
            re = Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+")
        } else {
            re = Regex("[\\p{P}\\p{S}]|[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+")
        }
        val cleaned = re.replace(text, " ").replace(Regex("\\s+"), " ").trim()
        return capitalizeWords(cleaned).trim()
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


    //Get stdev:
    fun getStDev(numbers: List<Int>): Int {
        val mean = numbers.average()
        val variance = numbers.map { (it - mean).pow(2) }.average()
        return sqrt(variance).roundToInt()
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
        if (languageWordsMap.contains(language)) {
            reqLangCode = languageWordsMap[language]!!
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
            val uriToFile = FileProvider.getUriForFile(context, "com.ftrono.DJames.provider", file)
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uriToFile)
                type = "image/jpeg"
            }
            var chooserIntent = Intent.createChooser(sendIntent, null)
            chooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            chooserIntent.putExtra("fromwhere", "ser")
            startActivity(context, chooserIntent, null)
        } else {
            Toast.makeText(context, "ERROR: cannot send the requested file!", Toast.LENGTH_LONG).show()
        }
    }


    //CLEANING:
    //Clean cached recordings:
    fun cleanOlderRecs(context: Context) {
        try {
            File(context.cacheDir, "$recFileName.mp3").delete()
            Log.d(TAG, "Recording mp3 deleted.")
        } catch (e: Exception) {
            Log.w(TAG, "Recording mp3 not deleted.")
        }
        try {
            File(context.cacheDir, "$recFileName.flac").delete()
            Log.d(TAG, "Recording flac deleted.")
        } catch (e: Exception) {
            Log.w(TAG, "Recording flac not deleted.")
        }
    }


    //On Logout: delete user cache files:
    fun deleteUserCache(context: Context) {
        try {
            messageUtils.deleteAllMessages(context)
            Log.d(TAG, "Deleted ALL message history.")
            libUtils.deleteLibrary(context, "artists")
            libUtils.deleteLibrary(context, "playlists")
            libUtils.deleteLibrary(context, "contacts")
            Log.d(TAG, "User library deleted.")
        } catch (e: Exception) {
            Log.w(TAG, "User cache not completely deleted.")
        }
    }

}