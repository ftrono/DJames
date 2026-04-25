package com.ftrono.DJames.be.database

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import com.ftrono.DJames.BuildConfig
import com.ftrono.DJames.application.ACTION_MESSAGES_REFRESH
import com.ftrono.DJames.application.appVersion
import com.ftrono.DJames.application.datetimeExportFormat
import com.ftrono.DJames.application.datetimeFullFormat
import com.ftrono.DJames.application.lastStarterId
import com.ftrono.DJames.application.maxHistoryDays
import com.ftrono.DJames.application.messageBox
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.collections.testMessages
import io.objectbox.query.QueryBuilder
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File


class MessageUtils {
    private val TAG = this::class.java.simpleName

    //GET ALL:
    //Get List of Message IDs:
    fun refreshMessages(preview: Boolean = false): List<Long> {
        var messagesIds = listOf<Long>()
        try {
            //Load message IDs:
            if (preview) {
                messagesIds = testMessages.sortedByDescending { it.timestamp }.map { it.id }
            } else {
                // TODO (TEMP): Cannot sort in ObjectBox directly in PropertyQuery!
                // SO: First map ids to timestamps, then sort and finally extract sorted ids only:
                val ids = messageBox!!.query()
                    .build()
                    .property(Message_.id)
                    .findLongs()
                    .toList()
                val timestamps = messageBox!!.query()
                    .build()
                    .property(Message_.timestamp)
                    .findLongs()
                    .toList()
                val idsMap = ids.zip(timestamps).toMap()
                messagesIds = idsMap.toList().sortedByDescending { it.second }.toMap().keys.toList()
            }
            return messagesIds

        } catch (e: Exception) {
            Log.w(TAG, "ERROR: cannot refresh Messages! ", e)
            return messagesIds
        }
    }


    //GET MESSAGES:
    //Get single message by ID:
    fun getMessageById(id: Long, preview: Boolean = false): Message {
        return if (preview) {
            testMessages.filter { it.id == id }[0]
        } else {
            messageBox!!.get(id)
        }
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
    //Store last open message to DB:
    fun storeMessage(
        context: Context,
        langCode: String,
        fromUser: Boolean = false,
        fromVoice: Boolean = false,
        isStart: Boolean = false,
        text: String,
        intent: String = "",
        actionType: ActionType? = null,
        attachments: Attachments = Attachments(),
    ): Long {
        var key = -1L
        try {
            if (text == "") {
                // Empty message -> nothing to save:
                Log.w(TAG, "Empty Message: not saved!")
            } else {
                var message = Message(
                    id = 0,
                    appVersion = appVersion,
                    fromUser = fromUser,
                    timestamp = utils.getCurrentTimestamp(),
                    text = text,
                    fromVoice = fromVoice,
                    langCode = langCode,
                    requestIntent = intent,
                    actionType = actionType,
                    attachments = attachments
                )
                if (isStart) {
                    // NEW CONVERSATION:
                    lastStarterId = message.timestamp
                    message.isStart = true
                }
                message.starterId = lastStarterId
                // Store message:
                key = messageBox!!.put(message)
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
        return key
    }


    //Store last open message to DB:
    fun updateMessage(
        context: Context,
        id: Long,
        langCode: String = "",
        requestIntent: String = "",
        isStart: Boolean = false,
    ) {
        try {
            val message = messageBox!!.get(id)
            message.langCode = if (langCode != "") langCode else message.langCode
            message.requestIntent =
                if (requestIntent != "") requestIntent else message.requestIntent
            if (isStart) {
                // NEW CONVERSATION:
                lastStarterId = message.timestamp
                message.isStart = true
            }
            messageBox!!.put(message)
            Log.d(TAG, "Message item $id updated!")
            //Send broadcast:
            Intent().also { intent ->
                intent.setAction(ACTION_MESSAGES_REFRESH)
                context.sendBroadcast(intent)
            }
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: Message item $id not updated!", e)
        }
    }


    //DB DELETE:
    //Update Starter ID for conversations whose first message has been deleted:
    fun updateLeftovers(
        starterIds: List<Long>,
    ) {
        try {
            // Get full list of starterIDs:
            for (starterId in starterIds) {
                // Check how many messages are in the conversation:
                val items = messageBox!!.query(Message_.starterId.equal(starterId)).build().find()
                if (items.isNotEmpty()) {
                    // New first message must have isStart = true:
                    val msg = items[0]
                    msg.isStart = true
                    messageBox!!.put(msg)
                    Log.d(TAG, "Leftovers with starterId $starterId updated!")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ERROR: cannot update leftovers!", e)
        }
    }


    //Delete Messages (+ leftovers):
    fun deleteMessageItems(context: Context, ids: List<Long>) {
        try {
            // Delete:
            var itemsToDelete = messageBox!!.get(ids)
            messageBox!!.remove(itemsToDelete)
            Log.d(TAG, "Deleted Message items with IDs: $ids!")
            // Also update leftovers:
            val refStarterIds = itemsToDelete.map { item -> item.starterId }.toMutableList().distinct()
            updateLeftovers(refStarterIds)
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
            var itemsToDelete = messageBox!!
                .query(Message_.starterId.equal(starterId))
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
            val daysInMillis = maxHistoryDays * 24 * 60 * 60 * 1000
            val thresholdTimestamp = System.currentTimeMillis() - daysInMillis

            // Filter items with id (timestamp) before threshold:
            val itemsToDelete = messageBox!!.query()
                .less(Message_.timestamp, thresholdTimestamp)
                .build()
                .find()

            // Bulk delete
            messageBox!!.remove(itemsToDelete)
            // Also update leftovers:
            val refStarterIds = itemsToDelete.map { item -> item.starterId }.toMutableList().distinct()
            updateLeftovers(refStarterIds)
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
                single.datetime = utils.convertTimestamp(single.timestamp, datetimeFullFormat)
                val filename = "log_${utils.convertTimestamp(single.timestamp, datetimeExportFormat)}.json"
                val cachedFile = File(context.cacheDir, filename)
                cachedFile.writeText(Json.encodeToString(single))
                return filename
            } else if (starterId > 0) {
                // Prepare conversation:
                val conv = getMessagesByStarterId(starterId).toMutableList()
                if (conv.size > 0) {
                    var starterTimestamp = 0L
                    for (mess in conv) {
                        mess.datetime = utils.convertTimestamp(mess.timestamp, datetimeFullFormat)
                        starterTimestamp = mess.timestamp
                    }
                    val filename =
                        "log_${utils.convertTimestamp(starterTimestamp, datetimeExportFormat)}.json"
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
            val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", file)
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


    //BUILD MESSAGE VIEW INFO:
    fun buildExtraDetails(message: Message): String {
        var detailText = ""
        val trimLength = 40
        val intentName = message.requestIntent
        val attachments = message.attachments

        if (message.actionType == ActionType.WA_VOICE && intentName.contains("Message")) {
            detailText = "Type:  Whatsapp Voice"

        } else if (attachments.usable != null) {
            if (intentName.contains("Call") || intentName.contains("Message")) {
                //Calls & Messages:
                val itemInfo = attachments.usable!!
                val msgType = when (message.actionType) {
                    ActionType.WA_TEXT -> "Whatsapp Text"
                    ActionType.SMS -> "SMS"
                    else -> ""
                }
                detailText = if (msgType == "") "" else "Type:  $msgType\n"
                detailText += if (itemInfo.name == "") "" else "Contact:  ${itemInfo.name}"
                if (itemInfo.phoneSet != null) {
                    detailText += "\nPhone:  ${itemInfo.phoneSet!!.prefix} ${itemInfo.phoneSet!!.phone}"
                }

            } else if (intentName.contains("Drive")) {
                //Drive:
                val itemInfo = attachments.usable!!
                if (itemInfo.detail == "") {
                    detailText = if (itemInfo.name == "") "" else "Place:  ${itemInfo.name}"
                } else {
                    detailText =
                        if (itemInfo.name == "") "" else "Place:  ${itemInfo.name}\nDetail:  ${itemInfo.detail}"
                }
            }

            //Add confidence:
            if (attachments.usable!!.matchScore > 0 && detailText.trim() != "") {
                detailText += "\nMatch:  ${attachments.usable!!.matchScore}%"
            }

        } else if (intentName.contains("Play") && attachments.spotifyPlay != null) {
            //Play requests:
            val playable = attachments.spotifyPlay!!

            if (playable.type == "episode" && playable.episode != null) {
                //Podcast:
                var podcastName = utils.capitalizeWords(playable.episode!!.podcast!!.name)
                detailText = if (podcastName == "") "" else "Podcast:  $podcastName"
                var episodeName = utils.capitalizeWords(playable.episode!!.name)
                var episodeDate = utils.capitalizeWords(playable.episode!!.releaseDate)
                detailText += if (episodeName == "") "" else "\nEpisode:  ($episodeDate) \"$episodeName\""

            } else if (playable.type == "playlist" && playable.playlist != null) {
                //Playlist / collection:
                detailText = if (playable.playlist!!.name == "") "" else "Playlist:  ${
                    utils.capitalizeWords(playable.playlist!!.name)
                }"

            } else if (playable.type == "artist" && playable.artist != null) {
                //Artist:
                detailText =
                    if (playable.artist!!.name == "") "" else "Artist:  ${utils.capitalizeWords(playable.artist!!.name)}"

            } else if (playable.type == "album" && playable.album != null) {
                //Album:
                var matchName = utils.trimString(playable.album!!.name, trimLength)
                if (matchName != "") {
                    var artistName =
                        utils.trimString(playable.album!!.artists.joinToString(", ") { it.name }, trimLength)
                    if (playable.album!!.type != "album") {
                        detailText =
                            "Album:  $matchName  (${utils.capitalizeWords(playable.album!!.type)})\nArtist:  $artistName"
                    } else {
                        detailText = "Album:  $matchName\nArtist:  $artistName"
                    }
                }

            } else if (playable.type == "track" && playable.track != null) {
                //Track:
                var matchName = utils.trimString(playable.track!!.name, trimLength)
                if (matchName != "") {
                    var artistName =
                        utils.trimString(playable.track!!.artists.joinToString(", ") { it.name }, trimLength)

                    //Context:
                    var contextType = if (playable.track!!.context == null) "" else playable.track!!.context!!.type
                    var contextName = ""
                    if (playable.track!!.context != null) {
                        //Use Context name:
                        contextName = playable.track!!.context!!.name
                    } else if (playable.track!!.album != null) {
                        //Default to Album type:
                        contextType = utils.capitalizeWords(playable.track!!.album!!.type)
                        contextName = playable.track!!.album!!.name
                    }
                    var contextFull = "$contextName  ($contextType)"
                    detailText = "Track:  $matchName\nArtist:  $artistName\nContext:  $contextFull"
                }
            }

            //Add confidence:
            if (playable.matchScore > 0 && detailText.trim() != "") {
                detailText += "\nMatch:  ${playable.matchScore}%"
            }
        }
        return detailText
    }


    //TODO: TEMP (use only when needed):
    fun updateExistingMessages() {
        var messages = messageBox!!.query().order(Message_.timestamp, QueryBuilder.DESCENDING).build().find()
        for (msg in messages) {
            //TODO
            break
        }
    }

}
