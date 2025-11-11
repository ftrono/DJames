package com.ftrono.DJames.be.agents

import android.content.Context
import android.util.Log
import com.ftrono.DJames.R
import com.ftrono.DJames.application.defaultReplies
import com.ftrono.DJames.application.lastRecordingName
import com.ftrono.DJames.application.lastRequestIntent
import com.ftrono.DJames.application.lastUserMessage
import com.ftrono.DJames.application.messageUtils
import com.ftrono.DJames.application.minSpeechPct
import com.ftrono.DJames.application.START
import com.ftrono.DJames.application.END
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.be.agents.nodes.CallAgentNode
import com.ftrono.DJames.be.agents.nodes.DriveAgentNode
import com.ftrono.DJames.be.agents.nodes.GuidanceAgentNode
import com.ftrono.DJames.be.agents.nodes.MainRouterNode
import com.ftrono.DJames.be.agents.nodes.MessageAgentNode
import com.ftrono.DJames.be.agents.nodes.PlayerAgentNode
import com.ftrono.DJames.be.models.AiReply
import com.ftrono.DJames.be.agents.StateInfo
import com.ftrono.DJames.be.agents.graph.Graph
import com.ftrono.DJames.be.models.RecDetails
import com.google.gson.JsonParser
import java.io.BufferedReader
import java.io.InputStreamReader


// MAIN GRAPH:
class AgentsGraph(
    private val context: Context,
) {
    private val TAG = this::class.java.simpleName

    //GET LLM CREDENTIALS:
    val reader = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.env)))
    private val apiKey = JsonParser.parseReader(reader).asJsonObject.get("mistral_api_key").asString

    // Initialize:
    val graph = this.build()

    fun build(): Graph {
        // Define the graph structure:
        val graph = Graph(name="GraphV3")

        // MAIN ROUTER:
        graph.addNode(
            MainRouterNode(
                context = context,
                apiKey = apiKey,
                nextOptions = listOf(
                    "PlayerAgent",
                    "CallAgent",
                    "MessageAgent",
                    "DriveAgent",
                    "GuidanceAgent",
                    END
                ),
            )
        )

        // NODES:
        graph.addNode(
            PlayerAgentNode(
                context = context,
                apiKey = apiKey,
                useJson = true,
                onComplete = END,
                onFallback = "MainRouter",
            )
        )

        graph.addNode(
            CallAgentNode(
                context = context,
                apiKey = apiKey,
                useJson = true,
                onComplete = END,
                onFallback = "MainRouter",
            )
        )

        graph.addNode(
            MessageAgentNode(
                context = context,
                apiKey = apiKey,
                useJson = true,
                onComplete = END,
                onFallback = "MainRouter",
            )
        )

        graph.addNode(
            DriveAgentNode(
                context = context,
                apiKey = apiKey,
                useJson = true,
                onComplete = END,
                onFallback = "MainRouter",
            )
        )

        graph.addNode(
            GuidanceAgentNode(
                context = context,
                apiKey = apiKey,
                useJson = true,
                onComplete = END,
                onFallback = "MainRouter",
            )
        )

        Log.d(TAG, "Graph built!")
        return graph
    }

    fun loadMessages(): MutableList<ChatMessage> {
        // TODO: Build initial messages list:
        val msgList = mutableListOf<ChatMessage>(
            // ChatMessage(role = "user", content = inMessage)
        )
        Log.d(TAG, "Messages size: ${msgList.size} items.")
        return msgList
    }

    // (FROM VOICE) TRANSCRIBE NEW AUDIO:
    fun transcribe(inAudioPath: String): SttReturn {
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

    // Store user message:
    fun storeUserMessage(
        transcription: String,
        language: String,
        fromVoice: Boolean,
        isStart: Boolean
    ): Long {
        // Store last user message:
        lastUserMessage.text = transcription
        lastUserMessage.requestIntent = lastRequestIntent
        val lastUserMsgId = messageUtils.storeMessage(
            context = context,
            langCode = language,
            fromUser = true,
            fromVoice = fromVoice,
            isStart = isStart,
            llmMessages = mutableListOf<ChatMessage>(
                ChatMessage(role = "user", content = transcription)
            )
        )
        return lastUserMsgId
    }

    fun invoke(
        prevState: StateInfo,
        recDetails: RecDetails? = null,
        inMessage: String = "",
        isStart: Boolean = false,
    ): StateInfo {

        // Status vars:
        var updState = prevState
        var fromVoice = recDetails != null
        var fail = false
        var isSilence = false
        var language = ""
        Log.d(TAG, "Invoking Graph...")

        // Retrieve latest messages:
        if (updState.messages.isEmpty()) {
            updState.messages.addAll(
                loadMessages()   // TODO
            )
        }

        // Parse new message:
        var transcription = ""
        if (fromVoice) {
            // FROM VOICE:
            // Empty audio:
            if (recDetails!!.speechPct <= minSpeechPct) {
                isSilence = true
                fail = true
                Log.d(TAG, "Empty audio.")

            } else {
                // STT transcribe:
                val sttReturn = transcribe(recDetails.recPath)

                isSilence = sttReturn.isSilence
                fail = sttReturn.fail
                language = sttReturn.language
                transcription = sttReturn.transcription

                if (isSilence) {
                    Log.d(TAG, "Empty transcription.")
                } else {
                    Log.d(TAG, "TRANSCRIPTION: $transcription")
                }
            }
        } else {
            // FROM CHAT:
            transcription = inMessage
        }

        if (fail || isSilence) {
            // Silence case:
            return StateInfo(
                lastRecording = lastRecordingName,
                fail = true,
                noSave = isSilence,
                aiReplies = listOf(
                    AiReply(
                        langCode = prefs.queryLanguage,
                        text = if (isSilence) defaultReplies.replyFallback() else defaultReplies.replyError()
                    )
                )
            )

        } else {
            // Store user message:
            updState.lastUserMsgId = storeUserMessage(
                transcription = transcription,
                language = language,
                fromVoice = fromVoice,
                isStart = isStart,
            )
            // Append last user chat message to conversation:
            updState.messages.add(
                ChatMessage(role = "user", content = transcription)
            )

            Log.d(TAG, "Messages size (input): ${updState.messages.size} items.")

            // STREAMING LOOP:
            updState = graph.invoke(updState)
            Log.d(TAG, "Messages size (output): ${updState.messages.size} items, fail: ${updState.fail}")

            //TODO: Add store messages to DB here!

            // Returns:
            updState.end = updState.next == END
            updState.aiReplies = listOf(
                AiReply(
                    langCode = prefs.queryLanguage,
                    text = when {
                        updState.fail -> defaultReplies.replyError()
                        else -> updState.messages.last().content
                    }
                )
            )
            return updState
        }
    }
}