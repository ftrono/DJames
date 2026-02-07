package com.ftrono.DJames.be.agents

import android.content.Context
import android.util.Log
import com.ftrono.DJames.R
import com.ftrono.DJames.application.defaultReplies
import com.ftrono.DJames.application.START
import com.ftrono.DJames.application.END
import com.ftrono.DJames.application.defaultChatWait
import com.ftrono.DJames.application.messageUtils
import com.ftrono.DJames.application.minSpeechPct
import com.ftrono.DJames.application.mistralSttModel
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
import com.ftrono.DJames.be.database.Attachments
import com.ftrono.DJames.be.models.RecDetails
import com.google.gson.JsonParser
import java.io.BufferedReader
import java.io.InputStreamReader


// LLM GRAPH:
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

    // Process new user message:
    override fun processUserMessage(
        prevState: StateInfo,
        recDetails: RecDetails?,
        inMessage: String,
    ): StateInfo {

        // Status vars:
        var updState = prevState
        var fromVoice = recDetails != null
        updState.fromVoice = fromVoice
        var isStart = prevState.next == START
        var language = "en"
        Log.d(TAG, "Processing new user message...")

        // Parse new message:
        // LLM version: transcribe only if voice:
        var transcription = ""
        if (fromVoice) {
            // FROM VOICE:
            updState.lastRecording = recDetails!!.recName
            updState.noSave = false

            if (recDetails.speechPct <= minSpeechPct) {
                // Empty audio:
                Log.d(TAG, "Empty audio.")
                updState.isSilence = true
                updState.fail = true
                updState.noSave = true

            } else if (prevState.messageMode && prevState.messageType == "voice") {
                //Whatsapp audio message -> no STT!
                Log.d(TAG, "Message followup: audio message.")
                transcription = "(private message)"

            } else {
                // (LLM) STT transcribe:
                Log.d(TAG, "STT activated")
                val sttAgent = LlmAgent(
                    context = context,
                    apiKey = apiKey,
                    model = mistralSttModel,
                    agentName = "Transcriber",
                )
                val sttReturn = sttAgent.transcribe(
                    audioPath = recDetails.recPath
                )

                // Process:
                transcription = sttReturn.transcription
                updState.isSilence = sttReturn.isSilence
                updState.fail = sttReturn.fail
                language = sttReturn.language

                if (updState.isSilence) {
                    updState.noSave = true
                    Log.d(TAG, "Empty transcription.")
                } else {
                    Log.d(TAG, "TRANSCRIPTION: $transcription")
                }
            }
        } else {
            // FROM CHAT:
            transcription = inMessage
            Thread.sleep(defaultChatWait)   // Typing delay
        }

        if (!updState.fail && !updState.isSilence) {
            // STATE: Append last user message (original) to conversation:
            updState.messages.add(
                ChatMessage(role = "user", content = transcription)
            )
            // DB: Store user message (anonymized):
            val storedText = if (updState.messageMode) "(private message)" else transcription
            val attachments = Attachments()
            attachments.llmChatMessages = mutableListOf<ChatMessage>(
                ChatMessage(role = "user", content = storedText)
            )
            updState.lastUserMsgId = messageUtils.storeMessage(
                context = context,
                langCode = "en",
                fromUser = true,
                fromVoice = fromVoice,
                isStart = isStart,
                text = storedText,
                intent = updState.intentName,
                attachments = attachments,
            )
        }
        return updState
    }

    // MAIN: INVOKE:
    override fun invoke(
        prevState: StateInfo,
        recDetails: RecDetails?,
        inMessage: String,
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
        )

        Log.d(TAG, "Messages size (input): ${updState.messages.size} items.")

        // STREAMING LOOP:
        updState = stream(updState, onRestart = "GuidanceAgent", onResume = "MainRouter")
        Log.d(TAG, "Messages size (output): ${updState.messages.size} items, fail: ${updState.fail}")

        //TODO: Add store messages to DB here!

        // Returns:
        updState.aiReplies = listOf(
            AiReply(
                langCode = prefs.queryLanguage,
                text = when {
                    updState.isSilence -> defaultReplies.replyFallback()   // Fallback
                    updState.fail -> defaultReplies.replyError()   // Error
                    (updState.next == END && (
                            updState.messages.isEmpty() || updState.messages.last().role != "assistant"
                        )
                    ) -> defaultReplies.replyNevermind()   // Nevermind
                    else -> updState.messages.last().content   // Actual reply
                }
            )
        )
        return updState
    }
}