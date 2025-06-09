package com.ftrono.DJames.be.database

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import com.ftrono.DJames.application.ACTION_LOG_REFRESH
import com.ftrono.DJames.application.appVersion
import com.ftrono.DJames.application.lastLog
import com.ftrono.DJames.application.logDir
import com.ftrono.DJames.application.utils
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class HistoryUtils {
    private val TAG = HistoryUtils::class.java.simpleName


    //Open new log:
    fun openLog() {
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        lastLog = HistoryLog()
        lastLog.keyInfo.datetime = now
        lastLog.keyInfo.appVersion = appVersion
    }


    //Close last open log:
    fun saveLog(context: Context) {
        if (lastLog.keyInfo.vocScore == 0 && lastLog.keyInfo.bestScore == 0) {
            Log.d(TAG, "No scores: Log not saved!")
        } else {
            try {
                lastLog.keyInfo.intentName = lastLog.nlpQueries.first().intentName
                var now = last_log!!.get("datetime").asString
                var logFile = File(logDir, "$now.json")
                logFile.writeText(last_log.toString())
                //Send broadcast:
                Intent().also { intent ->
                    intent.setAction(ACTION_LOG_REFRESH)
                    context.sendBroadcast(intent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "ERROR: Log not saved!", e)
            }
        }
    }


    //CARD ACTIONS:
    //Send:
    fun sendLog(mContext: Context, filename: String) {
        //Send the current file:
        val file = File(logDir, filename)
        val uriToFile = FileProvider.getUriForFile(mContext, "com.ftrono.DJames.provider", file)
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uriToFile)
            type = "image/jpeg"
        }
        var chooserIntent = Intent.createChooser(sendIntent, null)
        chooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        chooserIntent.putExtra("fromwhere", "ser")
        startActivity(mContext, chooserIntent, null)
    }


    //Open:
    fun viewLog(mContext: Context, filename: String) {
        try {
            // Get URI and MIME type of file
            val file = File(logDir, filename)
            val uri = FileProvider.getUriForFile(mContext, "com.ftrono.DJames.provider", file)
            val mime = mContext.contentResolver.getType(uri)

            // Open file with user selected app
            val intent1 = Intent()
            intent1.setAction(Intent.ACTION_VIEW)
            intent1.setDataAndType(uri, mime)
            intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent1.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent1.putExtra("fromwhere", "ser")
            startActivity(mContext, intent1, null)
        } catch (e: Exception) {
            Log.d("HistoryScreen", "ViewLog(): viewer app not found!")
            Toast.makeText(mContext, "No app to open the selected file!", Toast.LENGTH_LONG).show()
        }
    }

}