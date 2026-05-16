package com.ftrono.DJames.be.agents.nodes

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.mistralLlmModelSmall
import com.ftrono.DJames.kaigraph.LlmAgent
import com.ftrono.DJames.kaigraph.NodeType
import com.ftrono.DJames.kaigraph.StateInfo
import com.ftrono.DJames.kaigraph.Node


// Router node:
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
            - "PlayerAgent" -> for any request involving playing or finding music, songs, music artists, albums or podcast episodes, or Spotify in general.
            - "CallAgent" -> for any request involving making phone calls and calling people.
            - "MessageRouter" -> for any request involving sending messages (text or voice/audio, SMS or Whatsapp).
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
        updState.voiceMessageMode = false
        updState.actionType = null

        // Update:
        updState = updateStateFromRouter(context, llmReturn, updState)
        return updState
    }
}