package com.ftrono.DJames.be.chat

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.END
import com.ftrono.DJames.application.defaultReplies
import com.ftrono.DJames.application.defaultChatResetTime
import com.ftrono.DJames.application.fulfillmentUtils
import com.ftrono.DJames.application.lastStarterId
import com.ftrono.DJames.application.messageUtils
import com.ftrono.DJames.application.queryStatus
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.agents.AgentsGraph
import com.ftrono.DJames.be.agents.IntentsGraph
import com.ftrono.DJames.be.models.AiReply
import com.ftrono.DJames.be.agents.data.StateInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ChatManager(private val context: Context) {
    private val TAG = this::class.java.simpleName
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var agentsGraph: AgentsGraph
    private lateinit var intentsGraph: IntentsGraph

    private var lastState = StateInfo()   // Reset

    fun init() {
        // Agents:
        agentsGraph = AgentsGraph(context)
        agentsGraph.build()
        // (Compat) Intents:
        intentsGraph = IntentsGraph(context)
        intentsGraph.build()
    }

    fun processQuery(text: String, restart: Boolean = false) {
        // Reset chat if end, if forced "restart" (only if tap on oldChat action) or defaultChatResetTime is elapsed:
        Log.d(TAG, "$lastState")
        if (
            restart
            || lastState.next == END
            || lastState.fail
            || ((utils.getCurrentTimestamp() - lastStarterId) > defaultChatResetTime)
        ) {
            lastState = StateInfo()   // Reset
        }
        //Set overlay PROCESSING color & icon:
        queryStatus.postValue("processing")

        // PROCESS:
        coroutineScope.launch {
            withContext(coroutineScope.coroutineContext) {
                lastState = if (prefs.enableV3) {
                    agentsGraph.invoke(
                        inMessage = text,
                        prevState = lastState,
                    )
                } else {
                    intentsGraph.invoke(
                        inMessage = text,
                        prevState = lastState,
                    )
                }

                // Execute action:
                var newReplies = listOf<AiReply>()
                if (lastState.next == END && lastState.actionType != null) {
                    val actionsExecutor = ActionsExecutor(context)
                    newReplies = actionsExecutor.execute(lastState)   // Updated reply
                    if (newReplies.isNotEmpty()) lastState.aiReplies = newReplies
                    // Reset:
                    if (lastState.messageMode) {
                        lastState.messageMode = false
                        lastState.attachments.usable = null
                        lastState.next = END
                    }
                }

                // Save reply:
                if (!lastState.noSave) {
                    messageUtils.storeMessage(
                        context = context,
                        langCode = prefs.queryLanguage,
                        fromUser = false,
                        fromVoice = true,
                        text = lastState.aiReplies.joinToString(" ") { it.text },
                        intent = lastState.intentName,
                        actionType = lastState.actionType,
                        attachments = lastState.attachments,
                    )
                }
            }
            queryStatus.postValue("ready")
        }
    }
}
