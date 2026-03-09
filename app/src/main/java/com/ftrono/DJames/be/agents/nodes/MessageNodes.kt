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
            - "SMSAgent" -> a SMS (default category - in any case involving messages but not Whatsapp or voice / audio messages);
            - "WAVoiceAgent" -> a Whatsapp voice/audio message (if the user asks for a "voice" or "audio" message or to "record" a message);
            - "WATextAgent" -> a Whatsapp text message (if the user specifies "Whatsapp" but does not request a voice/audio message);
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
class SMSAgentNode (
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
        updState.messageType = "sms"
        updState.actionType = ActionType.SMS

        if (!utils.checkPermission(context, Manifest.permission.SEND_SMS)) {
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
            You're in charge of every request regarding preparing and sending a message.
            You have access to the user's saved contacts and have the capabilities to search for a contact, compose a text message and send it via SMS.
            Consider the context in the conversation and **always use the available tools** find the right contact, compose a message and finally send the message.
            
            **General rules**: 
            - Extract the name of the contact from the context of the conversation (if not present, ask the user) and **immediately pass it to "tool_retrieve" to retrieve the corresponding phone number** (always, whoever the contact is - even the user itself).
            - If the user dictates a phone number, convert the dictated number(s) into a usable phone number (no country prefix, unless specifically provided) and **read it back to the user for a confirmation** before proceeding.
            - Ask the user to dictate the message they want to send, if there's no message content already in the conversation context. 
            - Compose an SMS draft with the content of the message provided by the user. 
            - **Always read back the message to the user before sending it and ask for the user'.**
            - Finally, **only after** you collected the target contact's phone number, message text and you got the user's approval to send the prepared message, **you must call "tool_send" to send it**.
            
            ## TOOLS:
            You can use the following tools:
                * **tool_handoff**: use this tool if either: 
                    (i) the user is asking to end, stop or restart the conversation;
                    (ii) the user starts asking for Whatsapp or to record a voice/audio message;
                    (iii) the user is requesting guidance or info about your capabilities; 
                    (iv) in **any case* the user makes a request outside your tasks scope.
                * **tool_retrieve**: search from your knowledge base the phone number of the requested contact to send the SMS to. **Always use this tool if the user gives you the name of a contact but did not dictate to you a phone number!**
                * **tool_send**: finally send the SMS to the requested phone number. Use this tool only **AFTER you retrieved the phone number from 'tool_retrieve' or from the user itself, you composed the message draft and you got the user's approval to send it**.
            
            ## FURTHER INFO:
            - If the user's request is unclear, ask for clarification before proceeding with any tools.
            - If the user's request includes additional information that is not relevant to the task, focus on the primary request and ignore the additional information.
            - **Reply in the same language in which the user is speaking!** 
            - **Always follow the indications you receive from the tools!**
        """.trimIndent()

            val handoffDescriptionSMS = """
            Handoff tool. Use this tool **only when**: 
                (i) the user wants to end/stop the conversation; 
                (ii) the user starts asking for Whatsapp or to record a voice/audio message;
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
                    ToolHandoff(handoffDescriptionSMS).name to ToolHandoff(handoffDescriptionSMS),
                    ToolRetrieveContacts(
                        word="message",
                        verb="messaging",
                        finalTool = "tool_send",
                    ).name to ToolRetrieveContacts(
                        word="message",
                        verb="messaging",
                        finalTool = "tool_send",
                    ),
                    ToolSendSMS(context).name to ToolSendSMS(context),
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


// (LLM-based) ReAct agent node:
class WATextAgentNode (
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

        // Control params:
        updState.messageMode = true
        updState.messageType = "whatsapp"
        updState.actionType = ActionType.WA_TEXT

        // Build prompt:
        val corePrompt = """
            ## TASK:
            You're in charge of every request regarding preparing and sending a Whatsapp text message (no voice/audio).
            You have the capabilities to compose a text message and send it via Whatsapp to a contact that the user will select manually.
            Consider the context in the conversation, compose the text message and **always use the available tools** to finally send the SMS message.
            
            **General rules**: 
            - From the context of the conversation, you need to understand the content of the message to send.
            - If this info is not available from the conversation or the user is requesting if you can generically write or send a message to someone, ask the user to provide the information you need.
            - Ignore any contact name or phone number that the user provides - the user will need to select the contact later on the display, manually.
            - Ask the user to dictate the message they want to send, if there's no message content in the conversation context. 
            - Compose a message draft with the content of the message provided by the user. 
            - **Always read back the message to the user before sending it and ask for the user'.**
            - Finally, **only after** you prepared the message draft and you got the user's approval to send it, **you must call "tool_send" to send it via Whatsapp**.
            
            ## TOOLS:
            You can use the following tools:
                * **tool_handoff**: use this tool if either: 
                    (i) the user is asking to end, stop or restart the conversation;
                    (ii) the user starts asking for a SMS or to record a voice/audio message;
                    (iii) the user is requesting guidance or info about your capabilities; 
                    (iv) in **any case* the user makes a request outside your tasks scope.
                * **tool_send**: finally send the Whatsapp text message. Use this tool only **AFTER you composed the message draft and you got the user's approval to send it**.
            
            ## FURTHER INFO:
            - If the user's request is unclear, ask for clarification before proceeding with any tools.
            - If the user's request includes additional information that is not relevant to the task, focus on the primary request and ignore the additional information.
            - **Reply in the same language in which the user is speaking!** 
            - **Always follow the indications you receive from the tools!**
        """.trimIndent()

        val handoffDescriptionWAText = """
            Handoff tool. Use this tool **only when**: 
                (i) the user wants to end/stop the conversation; 
                (ii) the user starts asking for a SMS or to record a voice/audio message;
                (iii) the user is requesting guidance or info about your capabilities; 
                (iv) in **any case* the user makes a request outside your tasks scope.
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
                ToolHandoff(handoffDescriptionWAText).name to ToolHandoff(handoffDescriptionWAText),
                ToolSendWAText(context).name to ToolSendWAText(context),
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
        updState.actionType = ActionType.WA_VOICE

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
            }
        }

        updState.actionType = ActionType.WA_VOICE
        updState.attachments = llmReturn.attachments
        updState.actionType = llmReturn.actionType
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