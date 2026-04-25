package com.ftrono.DJames.be.agents.nodes

import android.Manifest
import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.END
import com.ftrono.DJames.application.defaultReplies
import com.ftrono.DJames.application.fulfillmentUtils
import com.ftrono.DJames.application.mistralLlmModelMedium
import com.ftrono.DJames.application.mistralLlmModelSmall
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.agents.data.ActionType
import com.ftrono.DJames.be.agents.data.ChatMessage
import com.ftrono.DJames.be.agents.data.LlmReturn
import com.ftrono.DJames.be.agents.data.NodeType
import com.ftrono.DJames.be.agents.llm.LlmAgent
import com.ftrono.DJames.be.agents.data.StateInfo
import com.ftrono.DJames.be.agents.tools.*
import com.ftrono.DJames.be.agents.fulfillment.GenericFulfillment


// (LLM-based) Message router node:
class MessageRouterNode (
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
        val corePrompt = """
            ## TASK:
            You have **only one task*: to **classify** the user request into **ONE of the following literal categories**.

            ## AVAILABLE CATEGORIES:
            - "WAVoiceAgent" -> a Whatsapp voice/audio message (if the user asks for a "voice" or "audio" message or to "record" a message);
            - "TextAgent" -> in any other case involving sending messages, like SMS and Whatsapp text messages, but NOT audio / voice messages);
            - "MessageRouter" -> in any other case NOT involving messages or involving ending the conversation.
        """.trimIndent()
        val inMessages = prepareInMessages(
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
        updState.attachments = llmReturn.attachments
        updState.actionType = llmReturn.actionType
        updState = updateStateFromRouter(context, llmReturn, updState)
        return updState
    }
}


// (LLM-based) ReAct agent node:
class TextAgentNode (
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
        var llmReturn: LlmReturn? = null

        // Control params:
        updState.messageMode = true

        // Build prompt:
        val corePrompt = """
        ## TASK:
        You're in charge of every request regarding preparing and sending a message.
        You have access to the user's saved contacts and have the capabilities to search for a contact, compose a text message and send it via SMS or Whatsapp.
        Consider the context in the conversation and **always use the available tools** find the right contact, compose a message and finally send the message.
        
        **General rules**: 
        - The user may ask to send either a SMS or a Whatsapp message. Use SMS as default, unless the user specifically asks for Whatsapp.
        - Extract the name of the contact from the context of the conversation (if not present, ask the user) and **immediately pass it to "tool_retrieve" to retrieve the corresponding phone number** (always, whoever the contact is - even the user itself).
        - Ask the user to dictate the message they want to send, if there's no message content already in the conversation context. 
        - Compose a message draft with the content of the message provided by the user. 
        - **Always read back the message to the user before sending it and ask for the user'.**
        - Finally, **only after** you collected the target contact's phone number, message text and you got the user's approval to send the prepared message, **you must call "tool_send" to send it**.
        
        ## TOOLS:
        You can use the following tools:
            * **tool_handoff**: use this tool if either: 
                (i) the user is asking to end, stop or restart the conversation;
                (ii) the user starts asking to send or record a voice/audio message;
                (iii) the user is requesting guidance or info about your capabilities; 
                (iv) in **any case* the user makes a request outside your tasks scope.
            * **tool_retrieve**: search from your knowledge base the phone number of the requested contact to send the message to. **Always use this tool if the user gives you the name of a contact!**
            * **tool_send**: finally send the message to the requested phone number. Use this tool only **AFTER you retrieved the phone number from 'tool_retrieve' or from the user itself, you composed the message draft and you got the user's approval to send it**.
        
        ## FURTHER INFO:
        - If the user's request is unclear, ask for clarification before proceeding with any tools.
        - If the user's request includes additional information that is not relevant to the task, focus on the primary request and ignore the additional information.
        - The user cannot dictate a phone number to send a message - it must give you the name of a saved contact.
        - **Reply in the same language in which the user is speaking!** 
        - **Always follow the indications you receive from the tools!**
        """.trimIndent()

        val handoffDescriptionText = """
        Handoff tool. Use this tool **only when**: 
            (i) the user wants to end/stop the conversation; 
            (ii) the user starts asking to send or record a voice/audio message;
            (iii) is requesting guidance or info about your capabilities; 
            (iv) in **any case* the user makes a request outside your tasks scope (not related to messages).
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
                ToolHandoff(handoffDescriptionText).name to ToolHandoff(handoffDescriptionText),
                ToolRetrieveContacts(
                    word="message",
                    verb="messaging",
                    finalTool = "tool_send",
                ).name to ToolRetrieveContacts(
                    word="message",
                    verb="messaging",
                    finalTool = "tool_send",
                ),
                ToolSendText(context).name to ToolSendText(context),
            ),
        )

        llmReturn = llmAgent.invoke(
            llmMessages = inMessages,
            attachments = updState.attachments
        )

        updState.attachments = llmReturn.attachments
        updState.actionType = llmReturn.actionType
        when (updState.actionType) {
            ActionType.SMS -> updState.messageType = "sms"
            ActionType.WA_TEXT -> updState.messageType = "wa_text"
            else -> null
        }
        updState = updateStateFromNode(updState, llmReturn)
        return updState
    }
}


// (LLM-based) ReAct agent node:
class WAVoiceAgentNode (
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
        var llmReturn: LlmReturn? = null

        // Control params:
        updState.messageMode = true
        updState.messageType = "voice"

        if (!updState.fromVoice) {
            // Chat mode -> FAIL:
            llmReturn = LlmReturn(
                fail = true,
                next = END,
                messages = mutableListOf(
                    ChatMessage(
                        role = "assistant",
                        content = defaultReplies.replyMessageCannotRecord(),
                    )
                ),
            )

        } else {
            // Simulate LLM return:
            if (updState.isStart) {
                llmReturn = LlmReturn(
                    next = name,
                    messages = mutableListOf(
                        ChatMessage(
                            role = "assistant",
                            content = defaultReplies.replyMessageRecord(),
                        )
                    ),
                )

            } else {
                // Simulate LLM return:
                llmReturn = LlmReturn(
                    next = onComplete,
                    messages = mutableListOf(
                        ChatMessage(
                            role = "assistant",
                            content = defaultReplies.replyWAVoiceReady(),
                        )
                    ),
                )
                updState.playAcknowledge = true
                updState.actionType = ActionType.WA_VOICE
            }
        }

        updState = updateStateFromNode(updState, llmReturn)
        return updState
    }
}


// (Intent-based) Fulfillment node:
class MessageIntentNode (
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
        if (!utils.checkPermission(context, Manifest.permission.SEND_SMS)) {
            updState = fulfillmentUtils.fallback(updState, noPermission=true)
        } else {
            updState = if (updState.isStart) fulfillment.contactRequest(updState) else fulfillment.sendMessage2(prevState)
            updState.next = if (updState.isStart) name else END
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