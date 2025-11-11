package com.ftrono.DJames.be.chat

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.defaultReplies
import com.ftrono.DJames.application.lastAiMessage
import com.ftrono.DJames.application.chatLastState
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
import com.ftrono.DJames.be.agents.StateInfo
import com.ftrono.DJames.be.fulfillment.NLPDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ChatManager(private val context: Context) {
    private val TAG = this::class.java.simpleName
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var agentsGraph: AgentsGraph
    private lateinit var nlpDispatcher: NLPDispatcher
    private var isStart = false

    fun init() {
        agentsGraph = AgentsGraph(context)
        nlpDispatcher = NLPDispatcher(context)
    }

    fun resetConv() {
        isStart = true
        chatLastState = StateInfo()
        lastRecordingName = ""
    }

    fun processQuery(text: String, restart: Boolean = false) {
        // Reset chat if end, if forced "restart" (only if tap on oldChat action) or defaultChatResetTime is elapsed:
        Log.d(TAG, "$chatLastState")
        if (
            restart
            || chatLastState.end
            || chatLastState.fail
            || ((utils.getCurrentTimestamp() - lastStarterId) > defaultChatResetTime)
        ) {
            resetConv()
        }
        //Set overlay PROCESSING color & icon:
        queryStatus.postValue("processing")
        messageUtils.resetMessage(fromUser = true)
        messageUtils.resetMessage(fromUser = false)

        // PROCESS:
        coroutineScope.launch {
            withContext(coroutineScope.coroutineContext) {
                chatLastState = if (prefs.enableV3) {
                    agentsGraph.invoke(
                        inMessage = text,
                        prevState = chatLastState,
                        isStart = isStart
                    )
                } else {
                    nlpDispatcher.dispatch(
                        text = text,
                        prevState = chatLastState,
                        isStart = isStart,
                        fromVoice = false
                    )
                }
                Log.d(TAG, "LAST DISPATCH: $chatLastState")

                var newReplies = listOf<AiReply>()
                isStart = false   // Enable FollowUp

                if (chatLastState.fail && chatLastState.aiReplies.isEmpty()) {
                    // Default fail replies:
                    newReplies = listOf(
                        AiReply(
                            langCode = prefs.queryLanguage,
                            text = defaultReplies.replyError()
                        )
                    )
                }

                // Store message:
                if (chatLastState.aiReplies.isNotEmpty()) {
                    // Save reply:
                    lastAiMessage.text = fulfillmentUtils.joinReplies(chatLastState.aiReplies)
                    lastAiMessage.requestIntent = lastRequestIntent
                    messageUtils.storeMessage(
                        context = context,
                        langCode = prefs.queryLanguage,
                        fromUser = false,
                        fromVoice = false
                    )
                }

                // Execute:
                if (chatLastState.end && chatLastState.actionType != null) {
                    val actionsExecutor = ActionsExecutor(context)
                    newReplies = actionsExecutor.execute(chatLastState)
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
            }
            queryStatus.postValue("ready")
        }
    }
}
