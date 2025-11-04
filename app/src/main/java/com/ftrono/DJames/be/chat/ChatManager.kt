package com.ftrono.DJames.be.chat

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.defaultReplies
import com.ftrono.DJames.application.lastAiMessage
import com.ftrono.DJames.application.chatLastDispatch
import com.ftrono.DJames.application.lastRequestIntent
import com.ftrono.DJames.application.defaultChatResetTime
import com.ftrono.DJames.application.fulfillmentUtils
import com.ftrono.DJames.application.lastRecordingName
import com.ftrono.DJames.application.lastStarterId
import com.ftrono.DJames.application.messageUtils
import com.ftrono.DJames.application.queryStatus
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.agents.AgentsGraph
import com.ftrono.DJames.be.models.AiReply
import com.ftrono.DJames.be.models.DispatcherInfo
import com.ftrono.DJames.be.nlp.NLPDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


class ChatManager(private val context: Context) {
    private val TAG = this::class.java.simpleName
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val agentGraph = AgentsGraph(context)
    private val nlpDispatcher = NLPDispatcher(context, agentGraph)

    fun resetConv() {
        chatLastDispatch = DispatcherInfo()
        lastRecordingName = ""
    }

    fun processQuery(text: String, restart: Boolean = false) {
        // Reset chat if end, if forced "restart" (only if tap on oldChat action) or defaultChatResetTime is elapsed:
        Log.d(TAG, "$chatLastDispatch")
        if (
            restart
            || chatLastDispatch.end
            || chatLastDispatch.fail
            || (!chatLastDispatch.followUp && !chatLastDispatch.messageMode)
            || ((utils.getCurrentTimestamp() - lastStarterId) > defaultChatResetTime)) {
            resetConv()
        }
        //Set overlay PROCESSING color & icon:
        queryStatus.postValue("processing")
        messageUtils.resetMessage(fromUser = true)
        messageUtils.resetMessage(fromUser = false)

        // PROCESS:
        coroutineScope.launch {
            chatLastDispatch = nlpDispatcher.dispatch(text=text, prevDispatch=chatLastDispatch, fromVoice=false)
            Log.d(TAG, "LAST DISPATCH: $chatLastDispatch")

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
                // Save reply:
                lastAiMessage.text = fulfillmentUtils.joinReplies(chatLastDispatch.aiReplies)
                lastAiMessage.requestIntent = lastRequestIntent
                messageUtils.storeMessage(
                    context = context,
                    langCode = prefs.queryLanguage,
                    fromUser = false,
                    fromVoice = false
                )
            }

            // Execute:
            if (chatLastDispatch.end && chatLastDispatch.actionType != null) {
                val actionsExecutor = ActionsExecutor(context)
                newReplies = actionsExecutor.execute(chatLastDispatch)
                if (newReplies.isNotEmpty()) {
                    // Save additional AI reply as a new message:
                    messageUtils.resetMessage(fromUser = false)
                    lastAiMessage.text = fulfillmentUtils.joinReplies(newReplies)
                    lastAiMessage.requestIntent = lastRequestIntent
                    messageUtils.storeMessage(
                        context = context,
                        langCode = prefs.queryLanguage,
                        fromUser = false,
                        fromVoice = false
                    )
                }
            }

            queryStatus.postValue("ready")
        }
    }
}
