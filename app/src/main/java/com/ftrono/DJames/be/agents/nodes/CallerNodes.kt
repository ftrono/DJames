package com.ftrono.DJames.be.agents.nodes

import android.Manifest
import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.END
import com.ftrono.DJames.application.defaultReplies
import com.ftrono.DJames.application.fulfillmentUtils
import com.ftrono.DJames.application.mistralLlmModelMedium
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.agents.data.ChatMessage
import com.ftrono.DJames.be.agents.data.LlmReturn
import com.ftrono.DJames.be.agents.llm.LlmAgent
import com.ftrono.DJames.be.agents.data.StateInfo
import com.ftrono.DJames.be.agents.data.handoffDescription
import com.ftrono.DJames.be.agents.tools.*
import com.ftrono.DJames.be.agents.fulfillment.GenericFulfillment


// (LLM-based) ReAct agent node:
class CallAgentNode (
    private val context: Context,
    private val apiKey: String,
    override val onComplete: String,
    override val onFallback: String,
) : Node() {

    override val TAG = this::class.java.simpleName
    override val name: String = TAG.replace("Node", "")
    val model = mistralLlmModelMedium

    override fun invoke(prevState: StateInfo): StateInfo {
        Log.d(TAG, "$name activated")
        var updState = prevState
        var llmReturn: LlmReturn? = null

        if (!utils.checkPermission(context, Manifest.permission.CALL_PHONE)) {
            // NO PERMISSION:
            llmReturn = LlmReturn(
                fail = true,
                next = END,
                messages = mutableListOf(
                    ChatMessage(
                        role = "assistant",
                        content = defaultReplies.replyNoPermission(),
                    )
                ),
            )

        } else {
            // Build prompt:
            val corePrompt = """
            ## TASK:
            You're in charge of every request regarding making a call.
            You have access to the user's saved contacts and have the capabilities to find and call the requested contact or number.
            Consider the context in the conversation and **use the available tools** to search and call the contact the user is requesting **before replying** to them.
            
            **General rules**: 
            - You need to understand who the user wants to call from the context of the conversation.
            - If the user dictates a phone number, convert the dictated number(s) into a usable phone number (no country prefix, unless specifically provided) and **read it back to the user for a confirmation** before proceeding. **If confirmed**, then use the "tool_call" tool to make the call.
            - If the user provides the name of a contact to call, retrieve the needed phone number from 'tool_retrieve', in order to call it via 'tool_call'.
            - If no info is available from the conversation or the user is requesting if you can make a call or call someone, ask the user directly to give you the name of the contact or a phone number to call.
            
            ## TOOLS:
            You can use the following tools:
                * **tool_handoff**: use this tool if either: 
                    (i) the user or a tool are asking to end, stop or restart the conversation; 
                    (ii) the user is requesting guidance or info about your capabilities; 
                    (iii) in **any case* the user makes a request outside your tasks scope.
                * **tool_retrieve**: search from your knowledge base the phone number of the requested contact to call. **Always use this tool if the user gives you the name of a contact but did not dictate to you a phone number!**
                * **tool_call**: finally call the requested phone number. Use this tool only **AFTER you retrieved the phone number from 'tool_retrieve' or from the user itself**.
            
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
                    ToolRetrieveContacts(
                        word="call",
                        verb="calling",
                        finalTool = "tool_call",
                    ).name to ToolRetrieveContacts(
                        word="call",
                        verb="calling",
                        finalTool = "tool_call",
                    ),
                    ToolCall().name to ToolCall(),
                ),
            )

            llmReturn = llmAgent.invoke(
                llmMessages = inMessages,
                attachments = updState.attachments
            )
        }

        updState.attachments = llmReturn.attachments
        updState.actionType = llmReturn.actionType
        updState = updateStateFromNode(updState, llmReturn)
        return updState
    }
}


// (Intent-based) Fulfillment node:
class CallIntentNode (
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
        if (!utils.checkPermission(context, Manifest.permission.CALL_PHONE)) {
            updState = fulfillmentUtils.fallback(updState, noPermission=true)
        } else {
            updState = fulfillment.contactRequest(updState)
            updState.next = END   // Mono
        }

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