package com.ftrono.DJames.be.agents.nodes

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.END
import com.ftrono.DJames.be.agents.LlmAgent
import com.ftrono.DJames.be.agents.NodeType
import com.ftrono.DJames.be.agents.StateInfo
import com.ftrono.DJames.be.agents.tools.SearchContacts
import com.ftrono.DJames.be.agents.tools.SearchPlaces
import com.ftrono.DJames.be.agents.tools.SearchTracks
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
        var userPrompt = """
            ## TASK:
            You have **only one task*: to **classify** the user request into **ONE of the following literal categories**.

            ## AVAILABLE CATEGORIES:
            - "PlayerAgent" -> for any request involving music, songs, music artists, albums or podcast episodes, or Spotify in general.
            - "CallAgent" -> for any request involving calling someone.
            - "MessageAgent" -> for any request involving messaging someone.
            - "DriveAgent" -> for any request involving requesting driving directions, routes, places, navigation or maps.
            - "__END__" -> if the user wants to stop, cancel, exit or end the conversation.
            - "GuidanceAgent" -> in any other case.
        """
        var systemPrompt = buildSystemPrompt(isRouter = true)
        userPrompt = buildUserPrompt(systemPrompt, userPrompt, isRouter = true)

        val llmAgent = LlmAgent(
            context = context,
            apiKey = apiKey,
            agentName = name,
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            isRouter = true,
        )

        val llmReturn = llmAgent.invoke(llmMessages = prevState.messages)
        updState = routeRequest(context, llmReturn, updState)
        return updState
    }
}


// (LLM-based) ReAct agent node:
class PlayerAgentNode (
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
        var userPrompt = """
            ## TASK:
            Your domain is music conversations. Your task is to help the user find a song to play. 
            Any request not involving music is outside your tasks scope.
            Use the available tools provided to get the list of available songs for the artist requested by the user.
        """
        var systemPrompt = buildSystemPrompt(useJson=useJson)
        userPrompt = buildUserPrompt(systemPrompt, userPrompt, useJson=useJson)

        val llmAgent = LlmAgent(
            context = context,
            apiKey = apiKey,
            agentName = name,
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            tools = mapOf<String, Tool>(
                "searchTracks" to SearchTracks()
            ),
        )

        val llmReturn = llmAgent.invoke(llmMessages = prevState.messages)
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
        var userPrompt = """
            ## TASK:
            Your task is to help the user make a call to one of his contacts. 
            Any request not involving making a call is outside your tasks scope.
            Use the available tools provided to get the list of available contacts that the user can call.
        """
        var systemPrompt = buildSystemPrompt(useJson=useJson)
        userPrompt = buildUserPrompt(systemPrompt, userPrompt, useJson=useJson)

        val llmAgent = LlmAgent(
            context = context,
            apiKey = apiKey,
            agentName = name,
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            tools = mapOf<String, Tool>(
                "searchContacts" to SearchContacts()
            ),
        )

        val llmReturn = llmAgent.invoke(llmMessages = prevState.messages)
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
        var userPrompt = """
            ## TASK:
            Your task is to help the user send a message to one of his contacts.
            Any request not involving sending an SMS, a Whatsapp text message or a Whatsapp audio/voice message is outside your tasks scope.
            Use the available tools provided to get the list of available contacts that the user can send messages to.
        """
        var systemPrompt = buildSystemPrompt(useJson=useJson)
        userPrompt = buildUserPrompt(systemPrompt, userPrompt, useJson=useJson)

        val llmAgent = LlmAgent(
            context = context,
            apiKey = apiKey,
            agentName = name,
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            tools = mapOf<String, Tool>(
                "searchContacts" to SearchContacts()
            ),
        )

        val llmReturn = llmAgent.invoke(llmMessages = prevState.messages)
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
        var userPrompt = """
            ## TASK:
            Your domain is places, maps and driving directions. Your task is to help the user find a place to drive to.
            Any request not involving places, maps or driving directions is outside your tasks scope.
            Use the available tools provided to get the list of available places the user can go nearby.
        """
        var systemPrompt = buildSystemPrompt(useJson=useJson)
        userPrompt = buildUserPrompt(systemPrompt, userPrompt, useJson=useJson)

        val llmAgent = LlmAgent(
            context = context,
            apiKey = apiKey,
            agentName = name,
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            tools = mapOf<String, Tool>(
                "searchPlaces" to SearchPlaces()
            ),
        )

        val llmReturn = llmAgent.invoke(llmMessages = prevState.messages)
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
        var userPrompt = """
            ## TASK:
            Your only task is to provide information on your functionalities to the user.
        """
        var systemPrompt = buildSystemPrompt(useJson=useJson)
        userPrompt = buildUserPrompt(systemPrompt, userPrompt, useJson=useJson)

        val llmAgent = LlmAgent(
            context = context,
            apiKey = apiKey,
            agentName = name,
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
        )

        val llmReturn = llmAgent.invoke(llmMessages = prevState.messages)
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
