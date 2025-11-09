package com.ftrono.DJames.be.agents.nodes

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.defaultReplies
import com.ftrono.DJames.application.lastRequestIntent
import com.ftrono.DJames.application.lastUserMessage
import com.ftrono.DJames.application.lastUserMessageId
import com.ftrono.DJames.application.messageUtils
import com.ftrono.DJames.application.nlp_queryText
import com.ftrono.DJames.be.agents.ChatMessage
import com.ftrono.DJames.be.agents.LlmAgent
import com.ftrono.DJames.be.agents.tools.SearchContacts
import com.ftrono.DJames.be.agents.tools.SearchPlaces
import com.ftrono.DJames.be.agents.tools.SearchTracks
import com.ftrono.DJames.be.agents.StateMap
import com.ftrono.DJames.be.agents.tools.Tool
import org.bsc.langgraph4j.action.NodeAction


// (LLM-based) Router node:
class MainRouterNode (
    private val context: Context,
    private val apiKey: String,
) : NodeAction<StateMap?> {

    private val TAG = this::class.java.simpleName
    val name: String = TAG.replace("Node", "")

    override fun apply(state: StateMap?): MutableMap<String?, Any?> {
        Log.d(TAG, "$name activated")
        val prompt = """
            You're a Router agent in a conversational graph. 
            
            ## TASK:
            You have **only one task*: to **classify** the user request into **ONE of the following literal categories**.

            ## AVAILABLE CATEGORIES:
            - "PlayerAgent" -> for any request involving music, songs, music artists, albums or podcast episodes, or Spotify in general.
            - "CallAgent" -> for any request involving calling someone.
            - "MessageAgent" -> for any request involving messaging someone.
            - "DriveAgent" -> for any request involving requesting driving directions, routes, places, navigation or maps.
            - "__END__" -> if the user wants to stop, cancel, exit or end the conversation.
            - "GuidanceAgent" -> in any other case.

           ## IMPORTANT: 
           - **You must NOT answer to the user question**: another agent will take care of that.
           - **Ignore all conversational context and previous replies.**
           - **Strictly reply with only ONE of these classification categories and NOTHING ELSE**. Don't use quotes.
        """
        var inMessages = state!!.messages()
        Log.d(TAG, "Current messages: $inMessages")

        val llmAgent = LlmAgent(
            context = context,
            apiKey = apiKey,
            agentName = name,
            basePrompt = prompt,
            isRouter = true,
        )

        val llmReturn = llmAgent.invoke(llmMessages = inMessages)
        lastRequestIntent = llmReturn.next   // TODO

        // Update last user message:
        messageUtils.updateMessage(
            context = context,
            id = state.lastUserMessageId(),
            requestIntent = llmReturn.next,
        )

        return mutableMapOf<String?, Any?>(
            StateMap.Companion.MESSAGES to llmReturn.messages,
            StateMap.Companion.FAIL to llmReturn.fail,
            StateMap.Companion.NEXT to llmReturn.next,
        )
    }
}


// (LLM-based) ReAct agent node:
class PlayerAgentNode (
    private val context: Context,
    private val apiKey: String,
) : NodeAction<StateMap?> {

    private val TAG = this::class.java.simpleName
    val name: String = TAG.replace("Node", "")

    override fun apply(state: StateMap?): MutableMap<String?, Any?> {
        Log.d(TAG, "$name activated")
        val prompt = """
            You are DJames, a smart driving assistant and personal virtual DJ! 
            You speak like an English personal chauffeur. You are helpful and gentle. 
            Use the available tools provided to get the list of available songs for the artist requested by the user.
            **Don't use markdown and always reply with short answers to the user.**
        """
        var inMessages = state!!.messages()
        Log.d(TAG, "Current messages: $inMessages")

        val llmAgent = LlmAgent(
            context = context,
            apiKey = apiKey,
            agentName = name,
            basePrompt = prompt,
            tools = mapOf<String, Tool>(
                "searchTracks" to SearchTracks()
            ),
        )

        val llmReturn = llmAgent.invoke(llmMessages = inMessages)

        return mutableMapOf<String?, Any?>(
            StateMap.Companion.MESSAGES to llmReturn.messages,
            StateMap.Companion.FAIL to llmReturn.fail,
        )
    }
}


// (LLM-based) ReAct agent node:
class CallAgentNode (
    private val context: Context,
    private val apiKey: String,
) : NodeAction<StateMap?> {

    private val TAG = this::class.java.simpleName
    val name: String = TAG.replace("Node", "")

    override fun apply(state: StateMap?): MutableMap<String?, Any?> {
        Log.d(TAG, "$name activated")
        val prompt = """
            You are DJames, a smart driving assistant and personal virtual DJ! 
            You speak like an English personal chauffeur. You are helpful and gentle. 
            Use the available tools provided to get the list of available contacts that the user can call.
            **Don't use markdown and always reply with short answers to the user.**
        """
        var inMessages = state!!.messages()
        Log.d(TAG, "Current messages: $inMessages")

        val llmAgent = LlmAgent(
            context = context,
            apiKey = apiKey,
            agentName = name,
            basePrompt = prompt,
            tools = mapOf<String, Tool>(
                "searchContacts" to SearchContacts()
            ),
        )

        val llmReturn = llmAgent.invoke(llmMessages = inMessages)

        return mutableMapOf<String?, Any?>(
            StateMap.Companion.MESSAGES to llmReturn.messages,
            StateMap.Companion.FAIL to llmReturn.fail,
        )
    }
}


// (LLM-based) ReAct agent node:
class MessageAgentNode (
    private val context: Context,
    private val apiKey: String,
) : NodeAction<StateMap?> {

    private val TAG = this::class.java.simpleName
    val name: String = TAG.replace("Node", "")

    override fun apply(state: StateMap?): MutableMap<String?, Any?> {
        Log.d(TAG, "$name activated")
        val prompt = """
            You are DJames, a smart driving assistant and personal virtual DJ! 
            You speak like an English personal chauffeur. You are helpful and gentle. 
            Use the available tools provided to get the list of available contacts that the user can send messages to.
            **Don't use markdown and always reply with short answers to the user.**
        """
        var inMessages = state!!.messages()
        Log.d(TAG, "Current messages: $inMessages")

        val llmAgent = LlmAgent(
            context = context,
            apiKey = apiKey,
            agentName = name,
            basePrompt = prompt,
            tools = mapOf<String, Tool>(
                "searchContacts" to SearchContacts()
            ),
        )

        val llmReturn = llmAgent.invoke(llmMessages = inMessages)

        return mutableMapOf<String?, Any?>(
            StateMap.Companion.MESSAGES to llmReturn.messages,
            StateMap.Companion.FAIL to llmReturn.fail,
        )
    }
}


// (LLM-based) ReAct agent node:
class DriveAgentNode (
    private val context: Context,
    private val apiKey: String,
) : NodeAction<StateMap?> {

    private val TAG = this::class.java.simpleName
    val name: String = TAG.replace("Node", "")

    override fun apply(state: StateMap?): MutableMap<String?, Any?> {
        Log.d(TAG, "$name activated")
        val prompt = """
            You are DJames, a smart driving assistant and personal virtual DJ! 
            You speak like an English personal chauffeur. You are helpful and gentle. 
            Use the available tools provided to get the list of available places the user can go nearby.
            **Don't use markdown and always reply with short answers to the user.**
        """
        var inMessages = state!!.messages()
        Log.d(TAG, "Current messages: $inMessages")

        val llmAgent = LlmAgent(
            context = context,
            apiKey = apiKey,
            agentName = name,
            basePrompt = prompt,
            tools = mapOf<String, Tool>(
                "searchPlaces" to SearchPlaces()
            ),
        )

        val llmReturn = llmAgent.invoke(llmMessages = inMessages)

        return mutableMapOf<String?, Any?>(
            StateMap.Companion.MESSAGES to llmReturn.messages,
            StateMap.Companion.FAIL to llmReturn.fail,
        )
    }
}


// (LLM-based) ReAct agent node:
class GuidanceAgentNode (
    private val context: Context,
    private val apiKey: String,
) : NodeAction<StateMap?> {

    private val TAG = this::class.java.simpleName
    val name: String = TAG.replace("Node", "")

    override fun apply(state: StateMap?): MutableMap<String?, Any?> {
        Log.d(TAG, "$name activated")
        val prompt = """
            You are DJames, a smart driving assistant and personal virtual DJ! 
            You speak like an English personal chauffeur. You are helpful and gentle. 
            Your only task is to provide information on your functionalities to the user.
            **Don't use markdown and always reply with short answers to the user.**
        """
        var inMessages = state!!.messages()
        Log.d(TAG, "Current messages: $inMessages")

        val llmAgent = LlmAgent(
            context = context,
            apiKey = apiKey,
            agentName = name,
            basePrompt = prompt,
        )

        val llmReturn = llmAgent.invoke(llmMessages = inMessages)

        return mutableMapOf<String?, Any?>(
            StateMap.Companion.MESSAGES to llmReturn.messages,
            StateMap.Companion.FAIL to llmReturn.fail,
        )
    }
}
