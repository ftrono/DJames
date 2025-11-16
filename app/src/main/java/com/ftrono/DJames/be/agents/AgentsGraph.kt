package com.ftrono.DJames.be.agents

import android.content.Context
import android.util.Log
import com.ftrono.DJames.R
import com.ftrono.DJames.application.defaultReplies
import com.ftrono.DJames.application.START
import com.ftrono.DJames.application.END
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.be.agents.nodes.CallAgentNode
import com.ftrono.DJames.be.agents.nodes.DriverAgentNode
import com.ftrono.DJames.be.agents.nodes.GuidanceAgentNode
import com.ftrono.DJames.be.agents.nodes.MainRouterNode
import com.ftrono.DJames.be.agents.nodes.MessageAgentNode
import com.ftrono.DJames.be.agents.nodes.PlayerAgentNode
import com.ftrono.DJames.be.models.AiReply
import com.ftrono.DJames.be.agents.data.StateInfo
import com.ftrono.DJames.be.agents.graph.Graph
import com.ftrono.DJames.be.agents.llm.LlmAgent
import com.ftrono.DJames.be.agents.data.ChatMessage
import com.ftrono.DJames.be.agents.data.SttReturn
import com.ftrono.DJames.be.models.RecDetails
import com.google.gson.JsonParser
import java.io.BufferedReader
import java.io.InputStreamReader


// MAIN GRAPH:
class AgentsGraph(
    private val context: Context,
): Graph(context) {

    override val name = this::class.java.simpleName
    override var TAG = name

    //GET LLM CREDENTIALS:
    val reader = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.env)))
    private val apiKey = JsonParser.parseReader(reader).asJsonObject.get("mistral_api_key").asString

    override fun build() {
        // Define the graph structure:
        addNodes(
            nodes = mutableListOf(
                // MAIN ROUTER:
                MainRouterNode(
                    context = context,
                    apiKey = apiKey,
                    nextOptions = listOf(
                        "PlayerAgent",
                        "CallAgent",
                        "MessageAgent",
                        "DriverAgent",
                        "GuidanceAgent",
                        START,
                        END
                    ),
                ),

                // NODES:
                PlayerAgentNode(
                    context = context,
                    apiKey = apiKey,
                    onComplete = END,
                    onFallback = "MainRouter",
                ),

                CallAgentNode(
                    context = context,
                    apiKey = apiKey,
                    onComplete = END,
                    onFallback = "MainRouter",
                ),

                MessageAgentNode(
                    context = context,
                    apiKey = apiKey,
                    onComplete = END,
                    onFallback = "MainRouter",
                ),

                DriverAgentNode(
                    context = context,
                    apiKey = apiKey,
                    onComplete = END,
                    onFallback = "MainRouter",
                ),

                GuidanceAgentNode(
                    context = context,
                    apiKey = apiKey,
                    onComplete = END,
                    onFallback = "MainRouter",
                )
            )
        )
        Log.d(TAG, "Graph built!")
    }

    // State / DB:
    // Load latest message to State:
    override fun loadMessages(): MutableList<ChatMessage> {
        // TODO: Build initial messages list:
        val msgList = mutableListOf<ChatMessage>(
            // ChatMessage(role = "user", content = inMessage)
        )
        Log.d(TAG, "Messages size: ${msgList.size} items.")
        return msgList
    }

    // (FROM VOICE) TRANSCRIBE NEW AUDIO:
    override fun transcribe(inAudioPath: String): SttReturn {
        Log.d(TAG, "STT activated")
        val sttAgent = LlmAgent(
            context = context,
            apiKey = apiKey,
            agentName = "Transcriber",
        )
        val sttReturn = sttAgent.transcribe(
            audioPath = inAudioPath
        )
        return sttReturn
    }

    override fun invoke(
        prevState: StateInfo,
        recDetails: RecDetails?,
        inMessage: String,
        isStart: Boolean,
    ): StateInfo {

        // Status vars:
        var updState = prevState
        Log.d(TAG, "Invoking Graph...")

        // Retrieve latest messages:
        if (updState.messages.isEmpty()) {
            updState.messages.addAll(
                loadMessages()   // TODO
            )
        }

        // Process new message:
        updState = processUserMessage(
            prevState = prevState,
            recDetails = recDetails,
            inMessage = inMessage,
            isStart = isStart,
        )

        Log.d(TAG, "Messages size (input): ${updState.messages.size} items.")

        // STREAMING LOOP:
        updState = stream(updState, onRestart = "GuidanceAgent")
        Log.d(TAG, "Messages size (output): ${updState.messages.size} items, fail: ${updState.fail}")

        //TODO: Add store messages to DB here!

        // Returns:
        updState.end = updState.next == END
        updState.aiReplies = listOf(
            AiReply(
                langCode = prefs.queryLanguage,
                text = when {
                    updState.isSilence -> defaultReplies.replyFallback()   // Fallback
                    updState.fail -> defaultReplies.replyError()   // Error
                    (updState.next == END && updState.messages.isEmpty()) -> defaultReplies.replyNevermind()   // Nevermind
                    else -> updState.messages.last().content   // Actual reply
                }
            )
        )
        return updState
    }
}