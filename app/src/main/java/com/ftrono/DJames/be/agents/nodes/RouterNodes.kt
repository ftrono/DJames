package com.ftrono.DJames.be.agents.nodes

import android.Manifest
import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.END
import com.ftrono.DJames.application.mistralLlmModelSmall
import com.ftrono.DJames.be.agents.llm.LlmAgent
import com.ftrono.DJames.be.agents.data.NodeType
import com.ftrono.DJames.be.agents.data.StateInfo


// (LLM-based) Router node:
class MainRouterNode (
    private val context: Context,
    private val apiKey: String,
    override val nextOptions: List<String> = listOf(),
) : Node() {

    override val TAG = this::class.java.simpleName
    override val name: String = TAG.replace("Node", "")
    override val type: NodeType = NodeType.ROUTER
    val model = mistralLlmModelSmall

    override fun invoke(prevState: StateInfo): StateInfo {
        Log.d(TAG, "$name activated")
        var updState = prevState

        // Build prompt:
        var corePrompt = """
            ## TASK:
            You have **only one task*: to **classify** the user request into **ONE of the following literal categories**.

            ## AVAILABLE CATEGORIES:
            - "PlayerAgent" -> for any request involving music, songs, music artists, albums or podcast episodes, or Spotify in general.
            - "CallAgent" -> for any request involving calling someone.
            - "MessageRouter" -> for any request involving messaging someone.
            - "DriverAgent" -> for any request involving requesting driving directions, routes, places, navigation or maps.
            - "__START__" -> if the user wants to restart the conversation.
            - "__END__" -> if the user wants to stop, exit or end the conversation.
            - "GuidanceAgent" -> in any other case.
        """.trimIndent()
        var inMessages = prepareInMessages(
            origMessages = prevState.messages,
            corePrompt = corePrompt,
            isRouter = true,
        )

        val llmAgent = LlmAgent(
            context = context,
            apiKey = apiKey,
            model = model,
            agentName = name,
            isRouter = true,
        )

        val llmReturn = llmAgent.invoke(
            llmMessages = inMessages,
            attachments = updState.attachments
        )

        // Reset:
        updState.messageMode = false
        updState.messageType = ""
        updState.actionType = null

        // Update:
        updState.attachments = llmReturn.attachments
        updState.actionType = llmReturn.actionType
        updState = updateStateFromRouter(context, llmReturn, updState, updateIntent=true)
        return updState
    }
}


// (Intent-based) Router node:
class IntentRouterNode (
    private val context: Context,
    override val nextOptions: List<String> = listOf(),
) : Node() {

    override val TAG = this::class.java.simpleName
    override val name: String = TAG.replace("Node", "")
    override val type: NodeType = NodeType.ROUTER

    override fun invoke(prevState: StateInfo): StateInfo {
        Log.d(TAG, "$name activated")
        var updState = prevState
        // Route:
        updState.next = when {
            (updState.intentName == "CallRequest") -> "CallIntent"
            (updState.intentName == "MessageRequest") -> "MessageIntent"
            (updState.intentName == "DriveRequest") -> "DriverIntent"
            (updState.intentName.contains("Play")) -> "PlayerIntent"
            (updState.intentName == "Cancel") -> END
            else -> {
                updState.fail = true
                updState.isSilence = true   // trigger Fallback reply
                END
            }
        }

        return updState
    }
}