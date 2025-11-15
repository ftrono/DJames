package com.ftrono.DJames.be.agents.nodes

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.END
import com.ftrono.DJames.be.agents.ChatMessage
import com.ftrono.DJames.be.agents.LlmAgent
import com.ftrono.DJames.be.agents.NodeType
import com.ftrono.DJames.be.agents.StateInfo
import com.ftrono.DJames.be.agents.tools.SearchContacts
import com.ftrono.DJames.be.agents.tools.*
import com.ftrono.DJames.be.agents.tools.Tool


// (LLM-based) Router node:
class MainRouterNode (
    private val context: Context,
    private val apiKey: String,
    override val nextOptions: List<String> = listOf(),
) : Node() {

    override val TAG = this::class.java.simpleName
    override val name: String = TAG.replace("Node", "")
    override val type: NodeType = NodeType.ROUTER

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
            - "MessageAgent" -> for any request involving messaging someone.
            - "DriveAgent" -> for any request involving requesting driving directions, routes, places, navigation or maps.
            - "__END__" -> if the user wants to stop, cancel, exit or end the conversation.
            - "GuidanceAgent" -> in any other case.
        """.trimIndent()
        var inMessages = addPromptsToMessages(
            origMessages = prevState.messages,
            corePrompt = corePrompt,
            isRouter = true,
        )

        val llmAgent = LlmAgent(
            context = context,
            apiKey = apiKey,
            agentName = name,
            isRouter = true,
        )

        val llmReturn = llmAgent.invoke(llmMessages = inMessages)
        updState = routeRequest(context, llmReturn, updState)
        return updState
    }
}


// (LLM-based) ReAct agent node:
class PlayerAgentNode (
    private val context: Context,
    private val apiKey: String,
    override val onComplete: String = END,
    override val onFallback: String = "",
) : Node() {

    override val TAG = this::class.java.simpleName
    override val name: String = TAG.replace("Node", "")

    override fun invoke(prevState: StateInfo): StateInfo {
        Log.d(TAG, "$name activated")
        var updState = prevState

        // Build prompt:
        var corePrompt = """
            ## TASK:
            Help the user find music or podcasts they want to play. You are connected to Spotify and you can play songs, artists, albums, playlists, podcast episodes or their liked songs collection. Consider the context in the conversation and **use the available tools** to search and play the item the user is requesting **before replying** to them.
            
            **General rules**: 
            - You need to understand what the user wants to play from the context of the conversation, and retrieve the needed Spotify ID from 'tool_retrieve', in order to play it via 'tool_play'.
            
            ## TOOLS:
            You can use the following tools:
                * **tool_handoff**: use this tool if the user either: 
                    (i) wants to end/stop the conversation; 
                    (ii) is requesting guidance or info about your capabilities; 
                    (iii) in **any case* the user makes a request outside your tasks scope.
                * **tool_retrieve**: get the Spotify ID of the requested item to play, if it exists in the knowledge base. **Always use this tool to retrieve the Spotify ID** for songs, artists, albums, playlists, podcast episodes or liked songs collection from your knowledge base before playing them!
                * **tool_play**: play the requested item in Spotify. Use this tool only **AFTER you retrieved from 'tool_retrieve' the Spotify ID** for the specific item to play.
            
            ## FURTHER INFO:
            - If the user's request is unclear, ask for clarification before proceeding with any tools. 
            - If the user's request includes additional information that is not relevant to the task, focus on the primary request and ignore the additional information.
            - **Always follow the indications you receive from the tools!**
        """.trimIndent()
        var inMessages = addPromptsToMessages(
            origMessages = prevState.messages,
            corePrompt = corePrompt,
        )

        val llmAgent = LlmAgent(
            context = context,
            apiKey = apiKey,
            agentName = name,
            handoffTo = onFallback,
            tools = mapOf<String, Tool>(
                ToolHandoff().name to ToolHandoff(),
                ToolRetrievePlayer().name to ToolRetrievePlayer(),
                ToolPlay().name to ToolPlay(),
            ),
        )

        val llmReturn = llmAgent.invoke(llmMessages = inMessages)
        if (!llmReturn.fail) {
            when (llmReturn.next) {
                END -> {
                    updState.messages.addAll(llmReturn.messages)
                    updState.next = llmReturn.next
                }
                onFallback -> {
                    updState.next = llmReturn.next
                }
                else -> {
                    updState.interrupt = true
                    updState.messages.addAll(llmReturn.messages)
                    updState.next = name
                }
            }
        }

        // Extend:
        updState.fail = llmReturn.fail
        return updState
    }
}


// (LLM-based) ReAct agent node:
class CallAgentNode (
    private val context: Context,
    private val apiKey: String,
    override val useJson: Boolean = false,
    override val onComplete: String = END,
    override val onFallback: String = "",
) : Node() {

    override val TAG = this::class.java.simpleName
    override val name: String = TAG.replace("Node", "")

    override fun invoke(prevState: StateInfo): StateInfo {
        Log.d(TAG, "$name activated")
        var updState = prevState

        // Build prompt:
        var corePrompt = """
            ## TASK:
            Your task is to help the user make a call to one of his contacts. 
            Any request not involving making a call is outside your tasks scope.
            Use the available tools provided to get the list of available contacts that the user can call.
        """.trimIndent()
        var inMessages = addPromptsToMessages(
            origMessages = prevState.messages,
            corePrompt = corePrompt,
        )

        val llmAgent = LlmAgent(
            context = context,
            apiKey = apiKey,
            agentName = name,
            handoffTo = "MainRouter",
            tools = mapOf<String, Tool>(
                "search_contacts" to SearchContacts()
            ),
        )

        val llmReturn = llmAgent.invoke(llmMessages = inMessages)
        if (useJson && !llmReturn.fail) {
            updState = parseJson(llmReturn, updState)
        } else {
            updState.messages.addAll(llmReturn.messages)
        }

        // Extend:
        updState.fail = llmReturn.fail
        return updState
    }
}


// (LLM-based) ReAct agent node:
class MessageAgentNode (
    private val context: Context,
    private val apiKey: String,
    override val useJson: Boolean = false,
    override val onComplete: String = END,
    override val onFallback: String = "",
) : Node() {

    override val TAG = this::class.java.simpleName
    override val name: String = TAG.replace("Node", "")

    override fun invoke(prevState: StateInfo): StateInfo {
        Log.d(TAG, "$name activated")
        var updState = prevState

        // Build prompt:
        var corePrompt = """
            ## TASK:
            Your task is to help the user send a message to one of his contacts.
            Any request not involving sending an SMS, a Whatsapp text message or a Whatsapp audio/voice message is outside your tasks scope.
            Use the available tools provided to get the list of available contacts that the user can send messages to.
        """.trimIndent()
        var inMessages = addPromptsToMessages(
            origMessages = prevState.messages,
            corePrompt = corePrompt,
        )

        val llmAgent = LlmAgent(
            context = context,
            apiKey = apiKey,
            agentName = name,
            handoffTo = "MainRouter",
            tools = mapOf<String, Tool>(
                "search_contacts" to SearchContacts()
            ),
        )

        val llmReturn = llmAgent.invoke(llmMessages = inMessages)
        if (useJson && !llmReturn.fail) {
            updState = parseJson(llmReturn, updState)
        } else {
            updState.messages.addAll(llmReturn.messages)
        }

        // Extend:
        updState.fail = llmReturn.fail
        return updState
    }
}


// (LLM-based) ReAct agent node:
class DriveAgentNode (
    private val context: Context,
    private val apiKey: String,
    override val useJson: Boolean = false,
    override val onComplete: String = END,
    override val onFallback: String = "",
) : Node() {

    override val TAG = this::class.java.simpleName
    override val name: String = TAG.replace("Node", "")

    override fun invoke(prevState: StateInfo): StateInfo {
        Log.d(TAG, "$name activated")
        var updState = prevState

        // Build prompt:
        var corePrompt = """
            ## TASK:
            Your domain is places, maps and driving directions. Your task is to help the user find a place to drive to.
            Any request not involving places, maps or driving directions is outside your tasks scope.
            Use the available tools provided to get the list of available places the user can go nearby.
        """.trimIndent()
        var inMessages = addPromptsToMessages(
            origMessages = prevState.messages,
            corePrompt = corePrompt,
        )

        val llmAgent = LlmAgent(
            context = context,
            apiKey = apiKey,
            agentName = name,
            handoffTo = "MainRouter",
            tools = mapOf<String, Tool>(
                "search_places" to SearchPlaces()
            ),
        )

        val llmReturn = llmAgent.invoke(llmMessages = inMessages)
        if (useJson && !llmReturn.fail) {
            updState = parseJson(llmReturn, updState)
        } else {
            updState.messages.addAll(llmReturn.messages)
        }

        // Extend:
        updState.fail = llmReturn.fail
        return updState
    }
}


// (LLM-based) ReAct agent node:
class GuidanceAgentNode (
    private val context: Context,
    private val apiKey: String,
    override val useJson: Boolean = false,
    override val onComplete: String = END,
    override val onFallback: String = "",
) : Node() {

    override val TAG = this::class.java.simpleName
    override val name: String = TAG.replace("Node", "")

    override fun invoke(prevState: StateInfo): StateInfo {
        Log.d(TAG, "$name activated")
        var updState = prevState

        // Build prompt:
        var corePrompt = """
            ## TASK:
            Your only task is to provide information on your functionalities to the user.
        """.trimIndent()
        var inMessages = addPromptsToMessages(
            origMessages = prevState.messages,
            corePrompt = corePrompt,
        )

        val llmAgent = LlmAgent(
            context = context,
            apiKey = apiKey,
            agentName = name,
            handoffTo = "MainRouter",
        )

        val llmReturn = llmAgent.invoke(llmMessages = inMessages)
        if (useJson && !llmReturn.fail) {
            updState = parseJson(llmReturn, updState)
        } else {
            updState.messages.addAll(llmReturn.messages)
        }

        // Extend:
        updState.fail = llmReturn.fail
        return updState
    }
}
