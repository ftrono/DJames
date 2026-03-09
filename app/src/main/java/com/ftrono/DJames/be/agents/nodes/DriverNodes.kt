package com.ftrono.DJames.be.agents.nodes

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.END
import com.ftrono.DJames.application.mistralLlmModelMedium
import com.ftrono.DJames.be.agents.data.ChatMessage
import com.ftrono.DJames.be.agents.llm.LlmAgent
import com.ftrono.DJames.be.agents.data.StateInfo
import com.ftrono.DJames.be.agents.data.handoffDescription
import com.ftrono.DJames.be.agents.tools.*
import com.ftrono.DJames.be.agents.fulfillment.GenericFulfillment


// (LLM-based) ReAct agent node:
class DriverAgentNode (
    private val context: Context,
    private val apiKey: String,
    override val onComplete: String,
    override val onFallback: String,
) : Node() {

    override val TAG = this::class.java.simpleName
    override val name: String = TAG.replace("Node", "")
    val model = mistralLlmModelMedium

    override fun invoke(prevState: StateInfo): StateInfo {
        Log.d(TAG, "${name} activated")
        var updState = prevState

        // Build prompt:
        val corePrompt = """
            ## TASK:
            You're in charge of every request regarding getting driving directions, navigation routes, places or address search. You are connected to Google Maps and you can find and show the user the navigation route to a place or address they ask.
            Consider the context in the conversation and **use the available tools** to search for a place / address and show the user the navigation route **before replying** to them.
            
            **General rules**: 
            - You need to understand which place or address the user is asking from the context of the conversation.
            - FIRST THING: always call the "tool_retrieve" tool to retrieve the Google Maps ID for the place name or address the user wants to go to (take the info as it is from the user message). 
            - THEN: Only **after** you use "tool_retrieve", use the "tool_go" tool with that Google Maps ID to show the navigation route on the user's screen.
            - If no place is mentioned in the conversation or the user is requesting if you can find or navigate to a place / address / route without giving you a place, ask the user directly to give you the name of the place or the address to navigate to.
            
            ## TOOLS:
            You can use the following tools:
                * **tool_handoff**: use this tool if either: 
                    (i) the user is asking to end, stop or restart the conversation; 
                    (ii) the user is requesting guidance or info about your capabilities; 
                    (iii) in **any case* the user makes a request outside your tasks scope.
                * **tool_retrieve**: get from Google Maps the Google Maps ID of the requested place / address. **Always use this tool to get the Google Maps ID BEFORE calling "tool_go"!**
                * **tool_go**: finally show to the user the navigation route with the retrieved Google Maps ID. Use this tool only **AFTER you retrieved the Google Maps ID from 'tool_retrieve'**.
            
            ## FURTHER INFO:
            - If the user's request is unclear, ask for clarification before proceeding with any tools. 
            - If the user's request includes additional information that is not relevant to the task, focus on the primary request and ignore the additional information.
            - **Reply in the same language in which the user is speaking!** 
            - **Always follow the indications you receive from the tools!**
        """.trimIndent()

        val inMessages = prepareInMessages(
            origMessages = prevState.messages,
            corePrompt = corePrompt,
        )

        val llmAgent = LlmAgent(
            context = context,
            apiKey = apiKey,
            model = model,
            agentName = name,
            onComplete = onComplete,
            onFallback = onFallback,
            tools = mapOf<String, Tool>(
                ToolHandoff(handoffDescription).name to ToolHandoff(handoffDescription),
                ToolRetrievePlaces().name to ToolRetrievePlaces(),
                ToolGo(context).name to ToolGo(context),
            ),
        )

        val llmReturn = llmAgent.invoke(
            llmMessages = inMessages,
            attachments = updState.attachments
        )
        updState.attachments = llmReturn.attachments
        updState.actionType = llmReturn.actionType
        updState = updateStateFromNode(updState, llmReturn)
        return updState
    }
}


// (Intent-based) Fulfillment node:
class DriverIntentNode (
    private val context: Context,
    override val onComplete: String = "",
    override val onFallback: String = "",
) : Node() {

    override val TAG = this::class.java.simpleName
    override val name: String = TAG.replace("Node", "")

    override fun invoke(prevState: StateInfo): StateInfo {
        Log.d(TAG, "$name activated")
        var updState = prevState

        // Fork:
        var fulfillment = GenericFulfillment(context)
        updState = if (updState.isStart) fulfillment.driveRequest1(updState) else fulfillment.driveRequest2(updState)
        updState.next = if (updState.isStart) name else END

        // Update messages:
        if (updState.aiReplies.isNotEmpty()) {
            updState.messages.add(
                ChatMessage(
                    role = "assistant",
                    content = updState.aiReplies.joinToString(" ") { it.text },
                )
            )
        }
        return updState
    }
}