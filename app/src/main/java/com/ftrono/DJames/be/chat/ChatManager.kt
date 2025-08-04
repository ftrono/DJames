package com.ftrono.DJames.be.chat

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.chatConvStarted
import com.ftrono.DJames.application.defaultReplies
import com.ftrono.DJames.application.chatFollowUp
import com.ftrono.DJames.application.lastAiMessage
import com.ftrono.DJames.application.chatLastDispatch
import com.ftrono.DJames.application.lastRequestIntent
import com.ftrono.DJames.application.chatMessageMode
import com.ftrono.DJames.application.chatReset
import com.ftrono.DJames.application.defaultChatResetTime
import com.ftrono.DJames.application.lastStarter
import com.ftrono.DJames.application.messageUtils
import com.ftrono.DJames.application.overlayStatus
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.be.models.AiReply
import com.ftrono.DJames.be.models.DispatcherInfo
import com.ftrono.DJames.be.nlp.NLPDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


class ChatManager(private val context: Context) {
    private val TAG = ChatManager::class.java.simpleName
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun resetConv() {
        chatConvStarted = false
        chatFollowUp = false
        chatMessageMode = false
        chatLastDispatch = DispatcherInfo()
        chatReset = false
    }

    fun processQuery(text: String) {
        // Start new conversation if defaultChatResetTime is elapsed:
        Log.d(TAG, "SUBTR: ${(messageUtils.getCurrentTimestamp() - lastStarter.starterId)}")
        Log.d(TAG, "DEF: ${defaultChatResetTime}")
        if (chatReset) {
            resetConv()
        } else {
            chatConvStarted = (messageUtils.getCurrentTimestamp() - lastStarter.starterId) < defaultChatResetTime
            if (!chatConvStarted) resetConv()
        }
        messageUtils.createMessage(fromUser = true, isStart = !chatConvStarted)
        //Set overlay PROCESSING color & icon:
        overlayStatus.postValue("processing")
        messageUtils.createMessage(fromUser = false)

        // PROCESS:
        coroutineScope.launch {
            var nlpDispatcher = NLPDispatcher(context)
            chatLastDispatch = nlpDispatcher.dispatch(text=text, prevDispatch=chatLastDispatch, followUp=chatFollowUp, messageMode=chatMessageMode)
            Log.d(TAG, "LAST DISPATCH: $chatLastDispatch")
            chatMessageMode = chatLastDispatch.messageMode
            chatFollowUp = chatLastDispatch.followUp

            var newReplies = listOf<AiReply>()

            if (chatLastDispatch.fail && chatLastDispatch.aiReplies.isEmpty()) {
                // Default fail replies:
                newReplies = listOf(
                    AiReply(
                        langCode = prefs.queryLanguage,
                        text = defaultReplies.replyError()
                    )
                )
            }

            // Store message:
            if (chatLastDispatch.aiReplies.isNotEmpty()) {
                // Join replies:
                var fullText = ""
                for (reply in chatLastDispatch.aiReplies) {
                    fullText = fullText + reply.text
                }
                // Save reply:
                lastAiMessage.text = fullText
                Log.d(TAG, "FULL TEXT: $fullText")
                lastAiMessage.langCode = prefs.queryLanguage   //TODO
                lastAiMessage.requestIntent = lastRequestIntent
                messageUtils.storeMessage(context, fromUser = false, fromVoice = false)
            }

            overlayStatus.postValue("ready")

//            // Execute:
//            if (chatLastDispatch.actionType != null) {
//                val actionsExecutor = ActionsExecutor(context)
//                actionsExecutor.execute(chatLastDispatch)
//            }

            // Reset:
            if (chatLastDispatch.end) {
                resetConv()
                chatReset = true
            }
        }
    }
}