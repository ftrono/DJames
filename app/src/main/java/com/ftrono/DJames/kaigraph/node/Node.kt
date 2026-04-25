package com.ftrono.DJames.kaigraph.node

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.datetimePromptFormat
import com.ftrono.DJames.application.messageUtils
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.agents.data.promptDateStr
import com.ftrono.DJames.be.agents.data.promptGenderStr
import com.ftrono.DJames.be.agents.data.promptIntro
import com.ftrono.DJames.be.agents.data.promptRouterIntro
import com.ftrono.DJames.be.agents.data.promptRouterOut
import com.ftrono.DJames.kaigraph.data.ChatMessage
import com.ftrono.DJames.kaigraph.data.LlmReturn
import com.ftrono.DJames.kaigraph.data.NodeType
import com.ftrono.DJames.kaigraph.data.StateInfo

open class Node() {

    open val name: String = ""
    open val type: NodeType = NodeType.AGENT
    open val nextOptions: List<String> = listOf()
    open val onComplete: String = ""
    open val onFallback: String = ""
    open val TAG = this::class.java.simpleName


    // Build prompts & prepare inMessages:
    fun prepareInMessages(
        origMessages: MutableList<ChatMessage>,
        corePrompt: String,
        isRouter: Boolean = false,
    ): MutableList<ChatMessage> {
        // inMessages contains the system prompt + the chosen input messages. They are NOT added to the message history.
        // outMessages (in LlmAgent) contains the newly-generated messages for the current agentic turn.

        // 1) Build system prompt:
        var systemPrompt = if (isRouter) {
            promptRouterIntro
            } else {
                val curDate = utils.convertTimestamp(
                    utils.getCurrentTimestamp(), datetimePromptFormat
                )
                promptIntro.replace(promptDateStr, curDate).replace(promptGenderStr, prefs.userGender)
            }

        // 2) Prepare user prompt:
        systemPrompt = if (isRouter) {
            systemPrompt + "\n" + corePrompt + "\n" + promptRouterOut
        } else {
            systemPrompt + "\n" + corePrompt + "\n"
        }

        // 3) Prepare inMessages: must contain prompt + llmMessages + new updates:
        // System prompt
        val inMessages = mutableListOf<ChatMessage>(
            ChatMessage(role = "system", content = systemPrompt)
        )
        // Message history:
        if (isRouter) {
            val msg = origMessages.last()
            inMessages.add(
                ChatMessage(
                    role = "user", content = "**## USER MESSAGE:** \"${msg.content} \""
                )
            )

        } else {
            inMessages.addAll(origMessages)
        }
        // Log.d(TAG, "inMessages for node $name: $inMessages")
        return inMessages
    }


    // (Router) Route to 'next' node:
    fun updateStateFromRouter(
        context: Context,
        llmReturn: LlmReturn,
        prevState: StateInfo,
        updateIntent: Boolean = false,
    ): StateInfo {
        var updState = prevState

        if (llmReturn.next in nextOptions) {
            Log.d(TAG, "Routing to -> ${llmReturn.next}")
            // Update last user message:
            if (updateIntent) {
                updState.intentName = if (llmReturn.next == "MessageRouter" || name == "MessageRouter") "MessageAgent" else llmReturn. next
            }
            if (prevState.lastUserMsgId != 0L) {
                messageUtils.updateMessage(
                    context = context,
                    id = prevState.lastUserMsgId,
                    requestIntent = updState.intentName,
                )
            }
            // Route to next:
            updState.next = llmReturn.next

        } else {
            // Non-existent 'next' option:
            Log.w(TAG, "ERROR: Next ID '${llmReturn.next}' not in nextIds array!")
            updState.fail = true
        }
        updState.attachments = llmReturn.attachments
        return updState
    }

    fun updateStateFromNode(
        prevState: StateInfo,
        llmReturn: LlmReturn,
    ): StateInfo {
        // inMessages contains the system prompt + the chosen input messages. They are NOT added to the message history.
        // outMessages (in LlmAgent) contains the newly-generated messages for the current agentic turn.

        var updState = prevState
        if (llmReturn.fail) {
            updState.fail = true
        } else {
            when (llmReturn.next) {
                onComplete -> {
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
        updState.attachments = llmReturn.attachments
        updState.attachments.latestTurnFlow.addAll(llmReturn.messages)
        updState.actionType = llmReturn.attachments.actionType
        updState.attachments.actionType = null
        return updState
    }

    // (Open) Custom invoke logic:
    open fun invoke(
        prevState: StateInfo
    ): StateInfo {
        // TODO
        var updState = prevState
        return updState
    }
}