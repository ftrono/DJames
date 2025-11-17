package com.ftrono.DJames.be.agents.graph

import android.content.Context
import android.util.Log
import com.ftrono.DJames.be.agents.nodes.Node
import com.ftrono.DJames.be.agents.data.StateInfo
import com.ftrono.DJames.application.START
import com.ftrono.DJames.application.END
import com.ftrono.DJames.application.defaultReplies
import com.ftrono.DJames.application.lastRecordingName
import com.ftrono.DJames.application.lastRequestIntent
import com.ftrono.DJames.application.lastUserMessage
import com.ftrono.DJames.application.messageUtils
import com.ftrono.DJames.application.minSpeechPct
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.be.agents.data.ChatMessage
import com.ftrono.DJames.be.agents.data.SttReturn
import com.ftrono.DJames.be.models.AiReply
import com.ftrono.DJames.be.models.RecDetails


open class Graph(
    private val context: Context,
) {
    open val name: String = this::class.java.simpleName
    open var TAG = name

    var graph = mutableMapOf<String, Node>()

    // Graph utils:
    fun addNode(node: Node) {
        graph.putIfAbsent(node.name, node)
    }

    fun addNodes(nodes: MutableList<Node>) {
        for (node in nodes) {
            addNode(node)
        }
    }

    fun getNode(name: String): Node {
        try {
            return graph[name]!!
        } catch (e: Exception) {
            throw Exception("Node '$name' not found in graph!")
        }
    }

    // Abstract:
    open fun build() {
        // TODO
    }

    open fun loadMessages(): MutableList<ChatMessage> {
        // TODO
        return mutableListOf()
    }

    open fun transcribe(inAudioPath: String): SttReturn {
        // TODO
        return SttReturn()
    }

    open fun invoke(
        prevState: StateInfo,
        recDetails: RecDetails? = null,
        inMessage: String = "",
        isStart: Boolean = false,
    ): StateInfo {
        // TODO
        return StateInfo()
    }

    // CENTRALIZED:
    // Stream graph:
    fun stream(
        prevState: StateInfo,
        onRestart: String,
    ): StateInfo {

        var updState = prevState
        updState.interrupt = false   // turn off from previous run
        if (updState.next == START) updState.next = graph.keys.first()
        Log.d(TAG, "Graph streaming loop STARTED from Node: '${updState.next}'.")

        // TODO: STREAMING LOOP:
        while (updState.next != END && !updState.fail) {
            Log.d(TAG, "Streaming from Node: ${updState.next}")
            // Log.d(TAG, "State -> $updState")
            updState = getNode(updState.next).invoke(updState)
            // Human-in-the-Loop:
            if (updState.interrupt) {
                Log.d(TAG, "Interrupt requested!")
                break
            } else if (updState.next == END) {
                updState.end = true
            } else if (updState.next == START) {
                // Fresh start:
                updState = StateInfo(
                    next = onRestart,
                    messages = mutableListOf(
                        ChatMessage(
                            role = "user",
                            content = "Hi!"
                        )
                    )
                )
            }
            Log.d(TAG, "Next node -> ${updState.next}")
        }
        Log.d(TAG, "Graph streaming loop ENDED.")
        // Log.d(TAG, "Final State -> $updState")
        return updState
    }

    // Process new user message:
    fun processUserMessage(
        prevState: StateInfo,
        recDetails: RecDetails?,
        inMessage: String,
        isStart: Boolean,
    ): StateInfo {
        
        // Status vars:
        var updState = prevState
        var fromVoice = recDetails != null
        var fail = false
        var isSilence = false
        var language = ""
        Log.d(TAG, "Processing new user message...")

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
                isSilence = isSilence,
                noSave = isSilence,
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
            return updState
        }
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

}
