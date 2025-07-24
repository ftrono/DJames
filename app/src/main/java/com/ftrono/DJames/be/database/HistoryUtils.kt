package com.ftrono.DJames.be.database

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import com.ftrono.DJames.application.ACTION_LOG_REFRESH
import com.ftrono.DJames.application.appVersion
import com.ftrono.DJames.application.curHistorySize
import com.ftrono.DJames.application.fulfillmentUtils
import com.ftrono.DJames.application.historyBox
import com.ftrono.DJames.application.lastLog
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.be.samples.testHistory
import io.objectbox.query.QueryBuilder
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class HistoryUtils {
    private val TAG = HistoryUtils::class.java.simpleName

    //GET ALL:
    //Get List of HistoryLog items:
    fun refreshHistory(preview: Boolean = false): List<String> {
        //1) Load history:
        var history = listOf<String>()
        try {
            history = if (preview) {
                testHistory
            } else {
                historyBox!!.query().order(HistoryLog_.datetime, QueryBuilder.DESCENDING).build().find()
            }.map { item ->
                //Cast value to String to allow storing into MutableState:
                Json.encodeToString(
                    HistoryLog(
                        id = item.id,
                        datetime = item.datetime,
                        keyInfo = item.keyInfo,
                        usable = item.usable,
                        spotifyPlay = item.spotifyPlay
                    )
                )
            }

            //2) Update History size (IMPORTANT - for signs):
            curHistorySize.postValue(history.size)
            return history

        } catch (e: Exception) {
            Log.w(TAG, "ERROR: cannot refresh History! ", e)
            curHistorySize.postValue(0)
            return history
        }
    }


    //GET SINGLE:
    //Get entire single item:
    fun getFullLog(id: Long): HistoryLog {
        return historyBox!!.get(id)
    }


    //Get single item, only key info:
    fun getKeyLogInfo(id: Long): HistoryLog {
        val item = historyBox!!.get(id)
        return HistoryLog(
            datetime = item.datetime,
            keyInfo = item.keyInfo,
            usable = item.usable,
            spotifyPlay = item.spotifyPlay
        )
    }


    // INSERT NEW:
    //Open new log:
    fun openLog() {
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        lastLog = HistoryLog()
        lastLog.datetime = now
        lastLog.appVersion = appVersion
        fulfillmentUtils.saveLogMessage(
            type = "ai",
            text = "Tell me, ${prefs.userGender}!"
        )
    }


    //Store last open log to DB:
    fun storeLog(context: Context) {
        try {
            lastLog.keyInfo.intentName = lastLog.nlpQueries.first().intentName
            lastLog.keyInfo.queryText = lastLog.nlpQueries.last().queryText
            Log.d(TAG, "CURRENT LOG: $lastLog")
            if (lastLog.keyInfo.intentName != "" && lastLog.keyInfo.queryText != "") {
                historyBox!!.put(lastLog)
                Log.d(TAG, "HistoryLog item ${lastLog.id} saved!")
                //Send broadcast:
                Intent().also { intent ->
                    intent.setAction(ACTION_LOG_REFRESH)
                    context.sendBroadcast(intent)
                }
            } else {
                lastLog = HistoryLog()
                Log.w(TAG, "Empty HistoryLog: Discarded!")
            }
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: HistoryLog not saved!", e)
        }
    }

    //UTILS:
    fun getHistorySize(): Long {
        return historyBox!!.count()
    }

    //DB DELETE:
    //Delete single item:
    fun deleteLogItem(context: Context, id: Long) {
        try {
            historyBox!!.remove(id)
            Log.d(TAG, "Deleted Log item $id!")
            Toast.makeText(context, "Log deleted!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "ERROR in deleting this log!", Toast.LENGTH_LONG).show()
            Log.w(TAG, "ERROR in deleting Log item: $id. ", e)
        }
    }

    //Delete all:
    fun deleteHistory(context: Context) {
        try {
            historyBox!!.removeAll()
            Log.d(TAG, "History deleted!")
            Toast.makeText(context, "History deleted!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.w(TAG, "ERROR in deleting history. ", e)
            Toast.makeText(context, "ERROR in deleting history!", Toast.LENGTH_LONG).show()
        }
    }


    //Delete older items (cleaning):
    fun deleteOldLogs() {
        try {
            // Define formatter for parsing datetime strings
            val thresholdDate = LocalDateTime.now().minusDays(30)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

            // Load all objects (you can optimize this if you know a rough range)
            val allItems = historyBox!!.all

            // Filter items with datetime before today
            val itemsToDelete = allItems.filter { item ->
                try {
                    val itemDateTime = LocalDateTime.parse(item.datetime, formatter)
                    itemDateTime.isBefore(thresholdDate)
                } catch (e: Exception) {
                    false // skip invalid formats
                }
            }

            // Bulk delete
            historyBox!!.remove(itemsToDelete)
            Log.d(TAG, "History cleaned: deleted ${itemsToDelete.size} older logs!")
        } catch (e: Exception) {
            Log.w(TAG, "ERROR in cleaning history. ", e)
        }
        }


    //Prepare cached HistoryLog file to send:
    fun prepareLogFile(context: Context, id: Long): String {
        try {
            val logItem = getFullLog(id)
            val filename = "log_${logItem.datetime.replace(":", "_")}.json"
            val cachedFile = File(context.cacheDir, filename)
            cachedFile.writeText(Json.encodeToString(logItem))
            return filename
        } catch (e: Exception) {
            Log.d(TAG, "ERROR: Cannot prepare Log to send. ", e)
            return ""
        }
    }


    //Open HistoryLog in external app:
    fun openLogViaApp(context: Context, filename: String) {
        try {
            // Get URI and MIME type of file
            val file = File(context.cacheDir, filename)
            val uri = FileProvider.getUriForFile(context, "com.ftrono.DJames.provider", file)
            val mime = context.contentResolver.getType(uri)

            // Open file with user selected app
            val intent1 = Intent()
            intent1.setAction(Intent.ACTION_VIEW)
            intent1.setDataAndType(uri, mime)
            intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent1.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent1.putExtra("fromwhere", "ser")
            startActivity(context, intent1, null)
        } catch (e: Exception) {
            Log.d("HistoryScreen", "openLogViaApp(): viewer app not found!")
            Toast.makeText(context, "No app to open the selected file!", Toast.LENGTH_LONG).show()
        }
    }

}