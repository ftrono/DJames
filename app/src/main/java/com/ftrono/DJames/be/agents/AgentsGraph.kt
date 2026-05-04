package com.ftrono.DJames.be.agents

import android.content.Context
import android.util.Log
import com.ftrono.DJames.R
import com.ftrono.DJames.application.defaultReplies
import com.ftrono.DJames.application.START
import com.ftrono.DJames.application.END
import com.ftrono.DJames.application.defaultChatWait
import com.ftrono.DJames.application.lastAiMessageText
import com.ftrono.DJames.application.lastUserMessageText
import com.ftrono.DJames.application.lastStarterId
import com.ftrono.DJames.application.messageUtils
import com.ftrono.DJames.application.minSpeechPct
import com.ftrono.DJames.application.mistralLlmModelSmall
import com.ftrono.DJames.application.mistralSttModel
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.be.agents.nodes.CallAgentNode
import com.ftrono.DJames.be.agents.nodes.DriverAgentNode
import com.ftrono.DJames.be.agents.nodes.GuidanceAgentNode
import com.ftrono.DJames.be.agents.nodes.MainRouterNode
import com.ftrono.DJames.be.agents.nodes.MessageTextAgentNode
import com.ftrono.DJames.be.agents.nodes.PlayerAgentNode
import com.ftrono.DJames.kaigraph.StateInfo
import com.ftrono.DJames.kaigraph.Graph
import com.ftrono.DJames.kaigraph.LlmAgent
import com.ftrono.DJames.kaigraph.ChatMessage
import com.ftrono.DJames.kaigraph.FinalizerReply
import com.ftrono.DJames.be.agents.nodes.MessageRouterNode
import com.ftrono.DJames.be.agents.nodes.MessageVoiceAgentNode
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
                // LEVEL 0 - MAIN ROUTER:
                MainRouterNode(
                    context = context,
                    apiKey = apiKey,
                    nextOptions = listOf(
                        "GuidanceAgent",
                        "DriverAgent",
                        "PlayerAgent",
                        "CallAgent",
                        "MessageRouter",
                        START,
                        END
                    ),
                ),

                // LEVEL 1 - NODES:
                GuidanceAgentNode(
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

                // LEVEL 1 - MESSAGE ROUTER:
                MessageRouterNode(
                    context = context,
                    apiKey = apiKey,
                    nextOptions = listOf(
                        "MessageVoiceAgent",
                        "MessageTextAgent",
                        "MainRouter",
                    ),
                ),

                // LEVEL 2 - MESSAGE NODES:
                MessageTextAgentNode(
                    context = context,
                    apiKey = apiKey,
                    onComplete = END,
                    onFallback = "MainRouter",
                ),

                MessageVoiceAgentNode(
                    context = context,
                    apiKey = apiKey,
                    onComplete = END,
                    onFallback = END,   // Deterministic node
                ),
            )
        )
        Log.d(TAG, "Graph built!")
    }

    // State / DB:
    // Load latest message to State:
    override fun loadMessages(): MutableList<ChatMessage> {
        val msgList = messageUtils.getMessagesByStarterId(lastStarterId)
        val chatMsgList = mutableListOf<ChatMessage>()
        for (msg in msgList) {
            chatMsgList.add(
                ChatMessage(
                    role = if (msg.fromUser) "user" else "assistant",
                    content = msg.text
                )
            )
        }
        Log.d(TAG, "Messages size: ${msgList.size} items.")
        return chatMsgList
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
        if (isStart) lastStarterId = 0L
        Log.d(TAG, "Processing new user message...")

        // Parse new message:
        // LLM version: transcribe only if voice and not voice message:
        var transcription = ""
        if (updState.voiceMessageMode) {
            transcription = "(voice message recorded)"
            Log.d(TAG, "Voice message recorded - no STT transcription.")

        } else if (fromVoice) {
            // FROM VOICE:
            updState.lastRecording = recDetails!!.recName
            updState.noSave = false

            if (recDetails.speechPct <= minSpeechPct) {
                // Empty audio:
                Log.d(TAG, "Empty audio.")
                updState.isSilence = true
                updState.fail = true
                updState.noSave = true

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
                updState.reqLangCode = if (sttReturn.language != "") sttReturn.language else prefs.queryLanguage
                Log.d(TAG, "TRANSCR. LANGUAGE: '${updState.reqLangCode}'")

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
            if (fromVoice) {
                lastUserMessageText.postValue(transcription)
                lastAiMessageText.postValue("...")
            }
            updState.messages.add(
                ChatMessage(role = "user", content = transcription)
            )
            // DB: Store user message:
            updState.lastUserMsgId = messageUtils.storeMessage(
                context = context,
                langCode = updState.reqLangCode,
                fromUser = true,
                fromVoice = fromVoice,
                isStart = isStart,
                text = transcription,
                agentName = updState.agentName,
                attachments = Attachments(),
            )
        }
        return updState
    }

    // Extract finalization info from last agent's reply:
    fun finalize(finalReply: String): Boolean {
        try {
            Log.d(TAG, "Invoking FinalizerAgent...")
            // Get out followUp info (true / false):
            val finalizerAgent = LlmAgent(
                context = context,
                apiKey = apiKey,
                model = mistralLlmModelSmall,
                agentName = "Structured",
            )
            val inMessages = mutableListOf<ChatMessage>(
                ChatMessage(
                    role = "system",
                    content = promptFinalizer
                ),
                ChatMessage(
                    role = "user",
                    content = finalReply
                )
            )

            val encodedReply = finalizerAgent.invoke(
                llmMessages = inMessages,
                attachments = Attachments()
            )

            val decodedReply = finalizerAgent.decodeJson<FinalizerReply>(encodedReply.messages[0].content)
            return decodedReply.followUp

        } catch (e: Exception) {
            Log.w(TAG, "ERROR in finalize(): ", e)
            return false
        }
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
        if (prevState.next == START) {
            updState.messages = loadMessages()
        }

        // Process new message:
        updState = processUserMessage(
            prevState = prevState,
            recDetails = recDetails,
            inMessage = inMessage,
        )

        Log.d(TAG, "Messages size (input): ${updState.messages.size} items.")

        // STREAMING LOOP:
        updState = stream(updState, routerNodes = listOf("MainRouter", "MessageRouter"))
        Log.d(TAG, "Messages size (output): ${updState.messages.size} items, fail: ${updState.fail}")

        // Final reply:
        updState.fullReply = when {
            updState.isSilence -> defaultReplies.replyFallback()   // Fallback
            updState.fail -> defaultReplies.replyError()   // Error
            (updState.next == END && (
                updState.messages.isEmpty() || updState.messages.last().role != "assistant"
            )) -> defaultReplies.replyNevermind()   // Nevermind
            else -> updState.messages.last().content   // Actual reply
        }

        // Safety check:
        if (updState.fullReply == "") {
            updState.fullReply = defaultReplies.replyError()
            updState.fail = true
            updState.interrupt = false
            updState.next = END
        }

        // Finalize process:
        if (updState.fail) {
            // Fail:
            updState.voiceMessageMode = false
            updState.interrupt = false
            updState.next = END

        } else {
            // Check with Finalizer agent:
            val followUp = if (updState.fullReply.contains("?")) true else {
                finalize(updState.fullReply.replace("*", ""))
            }
            if (followUp) {
                // Follow-up -> interrupt:
                updState.next = updState.agentName
                updState.interrupt = true

            } else {
                // END:
                updState.next = END
                updState.interrupt = false
                updState.voiceMessageMode = false
            }
        }
        return updState
    }
}