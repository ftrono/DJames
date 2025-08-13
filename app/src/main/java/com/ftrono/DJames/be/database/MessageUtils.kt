package com.ftrono.DJames.be.database

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import com.ftrono.DJames.application.ACTION_MESSAGES_REFRESH
import com.ftrono.DJames.application.appVersion
import com.ftrono.DJames.application.chatReset
import com.ftrono.DJames.application.voiceConvStarted
import com.ftrono.DJames.application.datetimeExportFormat
import com.ftrono.DJames.application.datetimeFullFormat
import com.ftrono.DJames.application.lastAiMessage
import com.ftrono.DJames.application.lastStarter
import com.ftrono.DJames.application.lastUserMessage
import com.ftrono.DJames.application.messageBox
import com.ftrono.DJames.application.messagesPageSize
import com.ftrono.DJames.be.samples.testMessages
import io.objectbox.query.QueryBuilder
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MessageUtils {
    private val TAG = MessageUtils::class.java.simpleName

    //UTILS:
    fun getCurrentTimestamp(): Long {
        return System.currentTimeMillis()
    }

    fun convertTimestamp(timestamp: Long, formatStr: String): String {
        val datetimeStr = SimpleDateFormat(formatStr, Locale.getDefault()).format(Date(timestamp))
        return datetimeStr
    }

    //GET ALL:
    //Get List of Message items:
    fun refreshMessages(offset: Long = 0L, preview: Boolean = false): List<String> {
        //1) Load messages:
        var messages = listOf<String>()
        try {
            messages = if (preview) {
                testMessages.sortedByDescending { it.timestamp }
            } else {
                messageBox!!.query().order(Message_.timestamp, QueryBuilder.DESCENDING).build().find(offset, messagesPageSize)
            }.map { item ->
                // Remove useless attachments:
                item.attachments.nlpQueries = mutableListOf()
                item.attachments.nlpExtractor = ExtractorInfo()
                item.attachments.spotifyQueries = mutableListOf()
                // Cast value to String to allow storing into MutableState:
                Json.encodeToString( item )
            }

            //2) Update Messages size (IMPORTANT - for signs):
//            curMessagesSize.postValue(
//                if (preview) testMessages.size else messageBox!!.query(Message_.type.notEqual("starter")).build().count().toInt()
//            )
            return messages

        } catch (e: Exception) {
            Log.w(TAG, "ERROR: cannot refresh Messages! ", e)
            return messages
        }
    }


    //GET MESSAGES:
    //Get single message by ID:
    fun getMessageById(id: Long): Message {
        return messageBox!!.get(id)
    }

    //Get entire items by starterId:
    fun getMessagesByStarterId(starterId: Long): List<Message> {
        try {
            return messageBox!!.query().equal(Message_.starterId, starterId).order(Message_.timestamp).build().find()
        } catch (e: Exception) {
            Log.w(TAG, "getMessagesByStarterId(): ERROR: ", e)
            return listOf()
        }
    }

    //Get message IDs with a given starterId:
    fun getMessageIDsByStarterId(starterId: Long): List<Long> {
        try {
            return messageBox!!.query().equal(Message_.starterId, starterId).order(Message_.timestamp).build().property(Message_.id).findLongs().toList()
        } catch (e: Exception) {
            Log.w(TAG, "getMessageIDsByStarterId(): ERROR: ", e)
            return listOf()
        }
    }


    //INSERT NEW:
    //Create Starter:
    fun createStarter() {
        val now = getCurrentTimestamp()-1
        lastStarter = Message(
            timestamp = now,
            appVersion = appVersion,
            type = "starter",
            starterId = now
        )
    }

    // Create new Message:
    fun createMessage(fromUser: Boolean = false, isStart: Boolean = false) {
        val now = getCurrentTimestamp()
        if (isStart) createStarter()
        if (fromUser) {
            lastUserMessage = Message(
                id = 0,
                timestamp = now,
                appVersion = appVersion,
                type = "user",
                starterId = lastStarter.timestamp,
            )
        } else {
            lastAiMessage = Message(
                id = 0,
                timestamp = now,
                appVersion = appVersion,
                type = "ai",
                starterId = lastStarter.timestamp,
            )
        }

    }


    //Store last open log to DB:
    fun storeMessage(context: Context, fromUser: Boolean = false, fromVoice: Boolean = false) {
        try {
            val message = if (fromUser) lastUserMessage else lastAiMessage
            if (!fromUser) {
                message.timestamp = getCurrentTimestamp()
            }
            if (message.text == "") {
                // Empty message -> nothing to save:
                Log.w(TAG, "Empty Message: not saved!")
            } else if (fromVoice && !fromUser && !voiceConvStarted) {
                // (Voice only) System Intro message - conversation not started until the user makes its first voice request:
                Log.w(TAG, "Conversation not started: skipped saving AI message!")
            } else {
                // STARTER:
                if (fromVoice && fromUser && !voiceConvStarted) {
                    // (Voice only) User is making its first voice request -> start new conversation now:
                    voiceConvStarted = true   // conv started
                    messageBox!!.put(lastStarter)
                    Log.d(TAG, "Voice conversation started!")
                } else if (fromUser && chatReset) {
                    // (Chat only) User is making its first chat request -> start new conversation now:
                    chatReset = false   // conv started
                    messageBox!!.put(lastStarter)
                    Log.d(TAG, "Chat conversation started!")
                }
                // CONTENT: Actually store message:
                messageBox!!.put(message)
                Log.d(TAG, "Message item ${message.id} saved!")
                //Send broadcast:
                Intent().also { intent ->
                    intent.setAction(ACTION_MESSAGES_REFRESH)
                    context.sendBroadcast(intent)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: Message item not saved!", e)
        }
    }

    //DB DELETE:
    //Delete empty starters (starters whose conversation has been fully deleted):
    fun deleteLeftovers(
        starterIds: List<Long>,
    ) {
        try {
            // Get full list of starterIDs:
            for (starterId in starterIds) {
                // Check how many messages are in the conversation:
                val items = messageBox!!.query(Message_.starterId.equal(starterId)).build().find()
                if (items.size == 1) {
                    // If 1 -> delete the leftover:
                    messageBox!!.remove(items)
                    Log.d(TAG, "Leftovers with starterId $starterId deleted!")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: cannot delete leftovers!", e)
        }
    }


    //Delete Messages (+ leftovers):
    fun deleteMessageItems(context: Context, ids: List<Long>) {
        try {
            // Delete:
            var itemsToDelete = messageBox!!.get(ids)
            messageBox!!.remove(itemsToDelete)
            Log.d(TAG, "Deleted Message items with IDs: $ids!")
            // Also delete leftovers:
            val refStarterIds = itemsToDelete.map { item -> item.starterId }.toMutableList().distinct()
            deleteLeftovers(refStarterIds)
            // Toast:
            if (itemsToDelete.size == 1) {
                Toast.makeText(context, "Message deleted!", Toast.LENGTH_LONG).show()
            } else if (itemsToDelete.size > 1) {
                Toast.makeText(context, "Messages deleted!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "ERROR: cannot delete!", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "ERROR: cannot delete!", Toast.LENGTH_LONG).show()
            Log.w(TAG, "ERROR in deleting Messages with IDs: $ids. ", e)
        }
    }


    //Delete Conversation:
    fun deleteConversation(context: Context, starterId: Long = 0) {
        try {
            var itemsToDelete = messageBox!!.query()
                .equal(Message_.starterId, starterId)
                .build()
                .find()
            messageBox!!.remove(itemsToDelete)
            Log.d(TAG, "Deleted Message items with starterId: $starterId!")
            Log.d(TAG, "$itemsToDelete.size")
            if (itemsToDelete.size == 1) {
                Toast.makeText(context, "Message deleted!", Toast.LENGTH_LONG).show()
            } else if (itemsToDelete.size > 1) {
                Toast.makeText(context, "Messages deleted!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "ERROR: cannot delete!", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "ERROR: cannot delete!", Toast.LENGTH_LONG).show()
            Log.w(TAG, "ERROR in deleting Messages with starterId: $starterId. ", e)
        }
    }


    //Delete all:
    fun deleteAllMessages(context: Context) {
        try {
            messageBox!!.removeAll()
            Log.d(TAG, "All messages deleted!")
            Toast.makeText(context, "All messages deleted!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.w(TAG, "ERROR in deleting all messages. ", e)
            Toast.makeText(context, "ERROR in deleting all messages!", Toast.LENGTH_LONG).show()
        }
    }


    //Delete older items (cleaning):
    fun deleteOldMessages() {
        try {
            // Go back 30 days from now (in timestamp):
            val daysInMillis = 30L * 24 * 60 * 60 * 1000
            val thresholdTimestamp = System.currentTimeMillis() - daysInMillis

            // Filter items with id (timestamp) before threshold:
            val itemsToDelete = messageBox!!.query()
                .less(Message_.timestamp, thresholdTimestamp)
                .build()
                .find()

            // Bulk delete
            messageBox!!.remove(itemsToDelete)
            // Also delete leftovers:
            val refStarterIds = itemsToDelete.map { item -> item.starterId }.toMutableList().distinct()
            deleteLeftovers(refStarterIds)
            Log.d(TAG, "Messages cleaned: deleted ${itemsToDelete.size} older messages!")
        } catch (e: Exception) {
            Log.w(TAG, "ERROR in cleaning messages. ", e)
        }
    }


    //Prepare cached Messages Log file to send:
    fun prepareLogFile(context: Context, id: Long = 0, starterId: Long = 0): String {
        try {
            if (id > 0) {
                // Prepare single message:
                val single = getMessageById(id)
                single.datetime = convertTimestamp(single.timestamp, datetimeFullFormat)
                val filename = "log_${convertTimestamp(single.timestamp, datetimeExportFormat)}.json"
                val cachedFile = File(context.cacheDir, filename)
                cachedFile.writeText(Json.encodeToString(single))
                return filename
            } else if (starterId > 0) {
                // Prepare conversation:
                val conv = getMessagesByStarterId(starterId).toMutableList()
                if (conv.size > 0) {
                    var starterTimestamp = 0L
                    for (mess in conv) {
                        mess.datetime = convertTimestamp(mess.timestamp, datetimeFullFormat)
                        starterTimestamp = mess.timestamp
                    }
                    val filename =
                        "log_${convertTimestamp(starterTimestamp, datetimeExportFormat)}.json"
                    val cachedFile = File(context.cacheDir, filename)
                    cachedFile.writeText(Json.encodeToString(conv))
                    return filename
                } else {
                    Log.d(TAG, "prepareLogFile(): empty conversation!")
                    return ""
                }
            } else {
                Log.d(TAG, "prepareLogFile(): no Id or starterId!")
                return ""
            }
        } catch (e: Exception) {
            Log.d(TAG, "ERROR: Cannot prepare Message Log to send. ", e)
            return ""
        }
    }


    //Open Messages Log file in external app:
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
            Log.d("MessagesScreen", "openLogViaApp(): viewer app not found!")
            Toast.makeText(context, "No app to open the selected file!", Toast.LENGTH_LONG).show()
        }
    }

}
