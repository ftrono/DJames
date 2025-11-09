package com.ftrono.DJames.be.agents

import android.content.Context
import android.util.Log
import com.ftrono.DJames.R
import com.ftrono.DJames.application.defaultReplies
import com.ftrono.DJames.application.lastRecordingName
import com.ftrono.DJames.application.lastRequestIntent
import com.ftrono.DJames.application.lastUserMessage
import com.ftrono.DJames.application.lastUserMessageId
import com.ftrono.DJames.application.messageUtils
import com.ftrono.DJames.application.minSpeechPct
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.be.agents.nodes.CallAgentNode
import com.ftrono.DJames.be.agents.nodes.DriveAgentNode
import com.ftrono.DJames.be.agents.nodes.GuidanceAgentNode
import com.ftrono.DJames.be.agents.nodes.MainRouterNode
import com.ftrono.DJames.be.agents.nodes.MessageAgentNode
import com.ftrono.DJames.be.agents.nodes.PlayerAgentNode
import com.ftrono.DJames.be.models.AiReply
import com.ftrono.DJames.be.agents.StateInfo
import com.ftrono.DJames.be.models.RecDetails
import com.google.gson.JsonParser
import org.bsc.langgraph4j.CompiledGraph
import org.bsc.langgraph4j.StateGraph
import org.bsc.langgraph4j.StateGraph.END
import org.bsc.langgraph4j.StateGraph.START
import org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async
import org.bsc.langgraph4j.action.AsyncNodeAction.node_async
import org.bsc.langgraph4j.state.AgentStateFactory
import org.bsc.langgraph4j.utils.EdgeMappings
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
    val stateGraph = this.build()
    var messages: MutableList<ChatMessage?> = this.loadMessages()

    fun build(): StateGraph<StateMap?> {
        // Define the graph structure:
        val stateGraph = StateGraph<StateMap?>(
            StateMap.SCHEMA,
            AgentStateFactory { initData: MutableMap<String?, Any?>? ->
                StateMap(
                    initData!!
                )
            })
            // Nodes:
            .addNode("MainRouter", node_async(MainRouterNode(context, apiKey)))
            .addNode("PlayerAgent", node_async(PlayerAgentNode(context, apiKey)))
            .addNode("CallAgent", node_async(CallAgentNode(context, apiKey)))
            .addNode("MessageAgent", node_async(MessageAgentNode(context, apiKey)))
            .addNode("DriveAgent", node_async(DriveAgentNode(context, apiKey)))
            .addNode("GuidanceAgent", node_async(GuidanceAgentNode(context, apiKey)))
            // Edges:
            .addEdge(START, "MainRouter")
            .addConditionalEdges(
                "MainRouter",
                edge_async { state ->
                    state!!.next()
                },
                EdgeMappings.builder()
                    .to("PlayerAgent")
                    .to("CallAgent")
                    .to("MessageAgent")
                    .to("DriveAgent")
                    .to("GuidanceAgent")
                    .toEND(END)
                    .build()
            )
            // End:
            .addEdge("PlayerAgent", END)
            .addEdge("CallAgent", END)
            .addEdge("MessageAgent", END)
            .addEdge("DriveAgent", END)
            .addEdge("GuidanceAgent", END)

        Log.d(TAG, "Graph built!")
        return stateGraph
    }

    fun loadMessages(): MutableList<ChatMessage?> {
        // TODO: Build initial messages list:
        val msgList = mutableListOf<ChatMessage?>(
            // ChatMessage(role = "user", content = inMessage)
        )
        Log.d(TAG, "Messages size: ${msgList.size} items.")
        return msgList
    }

    fun compile(stateGraph: StateGraph<StateMap?>): CompiledGraph<StateMap?> {
        return stateGraph.compile()
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
        lastUserMessageId = messageUtils.storeMessage(
            context = context,
            langCode = language,
            fromUser = true,
            fromVoice = fromVoice,
            isStart = isStart,
            llmMessages = mutableListOf<ChatMessage>(
                ChatMessage(role = "user", content = transcription)
            )
        )
        return lastUserMessageId
    }

    fun invoke(
        prevState: StateInfo,
        recDetails: RecDetails? = null,
        inMessage: String = "",
        isStart: Boolean = false,
    ): StateInfo {
        // Build & compile the graph:
        val compiledGraph = this.compile(stateGraph)
        Log.d(TAG, "Graph compiled!")

        // Status vars:
        var fromVoice = recDetails != null
        var fail = false
        var isSilence = false
        var isEnd = false
        var next = ""
        var language = ""

        // TODO: Retrieve latest messages

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
            storeUserMessage(
                transcription = transcription,
                language = language,
                fromVoice = fromVoice,
                isStart = isStart,
            )
            // Append last user chat message to conversation:
            messages.add(
                ChatMessage(role = "user", content = transcription)
            )

            Log.d(TAG, "Messages size (input): ${messages.size} items.")
            var outMessage = ""

            // STREAMING LOOP:
            // The `stream` method returns an AsyncGenerator. Results are in the final state after execution:
            for (event in compiledGraph.stream(
                // Input:
                mutableMapOf<String?, Any?>(
                    StateMap.MESSAGES to messages
                )
            )) {
                // Output:
                Log.d(TAG, "Current node: ${event.node()}")
                messages = event.state()!!.messages()
                fail = event.state()!!.fail()
                next = event.state()!!.next()
                isEnd = next == END
                language = event.state()!!.language()
                try {
                    outMessage = messages.last()!!.content.replace("* ", "- ")
                } catch (e: Exception) {
                    Log.w(TAG, "Cannot recover outMessage!")
                    fail = true
                    break
                }
                if (fail) break
            }
            Log.d(TAG, "Messages size (output): ${messages.size} items, fail: $fail.")

            //TODO: Add store messages to DB here!

            return StateInfo(
                lastRecording = lastRecordingName,
                fail = fail,
                end = isEnd,
                aiReplies = listOf(
                    AiReply(
                        langCode = prefs.queryLanguage,
                        text = when {
                            fail -> defaultReplies.replyError()
                            isEnd -> defaultReplies.replyNevermind()
                            else -> outMessage
                        }
                    )
                )
            )
        }
    }
}