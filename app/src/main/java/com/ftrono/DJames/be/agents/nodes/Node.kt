package com.ftrono.DJames.be.agents.nodes

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.END
import com.ftrono.DJames.application.datetimePromptFormat
import com.ftrono.DJames.application.lastRequestIntent
import com.ftrono.DJames.application.messageUtils
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.agents.data.ChatMessage
import com.ftrono.DJames.be.agents.data.LlmReturn
import com.ftrono.DJames.be.agents.data.NodeType
import com.ftrono.DJames.be.agents.data.StateInfo
import com.ftrono.DJames.be.agents.data.promptDateStr
import com.ftrono.DJames.be.agents.data.promptEnd
import com.ftrono.DJames.be.agents.data.promptGenderStr
import com.ftrono.DJames.be.agents.data.promptIntro
import com.ftrono.DJames.be.agents.data.promptRouterIntro
import com.ftrono.DJames.be.agents.data.promptRouterOut
import com.ftrono.DJames.be.agents.data.promptUserStr


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
        joinMessages: Boolean = false,
    ): MutableList<ChatMessage> {

        // 1) Build system prompt:
        val systemPrompt = if (isRouter) {
                promptRouterIntro
            } else {
                val curDate = utils.convertTimestamp(
                    utils.getCurrentTimestamp(), datetimePromptFormat
                )
                promptIntro.replace(promptDateStr, curDate).replace(promptGenderStr, prefs.userGender)
            }

        // 2) Prepare user prompt:
        var userPrompt = if (isRouter) {
                systemPrompt + "\n" + corePrompt + "\n" + promptRouterOut
            } else {
                systemPrompt + "\n" + corePrompt + "\n"
            }


        // 3) Prepare inMessages: must contain prompt + llmMessages + new updates:
        var inMessages = mutableListOf<ChatMessage>(
            ChatMessage(role = "system", content = systemPrompt + userPrompt)   // System prompt
        )

        if (joinMessages) {
            // Join the entire conversation in one message only:
            var fullConv = "## FULL CONVERSATION TRANSCRIPTION:"
            for (msg in origMessages) {
                fullConv = "$fullConv\n   - ${msg.role.uppercase()}: \"${msg.content} \""
            }
            inMessages.add(
                ChatMessage(
                    role = "user", content = fullConv
                )
            )

        } else {
            // Prepend user prompt to last user message:
            userPrompt = userPrompt + "\n" + promptEnd
            inMessages.addAll(origMessages.subList(0, origMessages.lastIndex))  // History
            inMessages.add(
                ChatMessage(
                    role = "user",
                    content = userPrompt.replace(promptUserStr, origMessages.last().content)
                )
            )   // User prompt + user message
        }

        return inMessages
    }


    // (Router) Route to 'next' node:
    fun routeRequest(
        context: Context,
        llmReturn: LlmReturn,
        prevState: StateInfo
    ): StateInfo {
        var updState = prevState

        if (llmReturn.next in nextOptions) {
            Log.d(TAG, "Routing to -> ${llmReturn.next}")
            // Update last user message:
            if (prevState.lastUserMsgId != 0L) {
                messageUtils.updateMessage(
                    context = context,
                    id = prevState.lastUserMsgId,
                    requestIntent = llmReturn.next,
                )
            }
            // Route to next:
            lastRequestIntent = llmReturn.next   // TODO
            updState.intentName = llmReturn.next
            updState.next = llmReturn.next
            if (updState.next == END) {
                updState.messages = mutableListOf<ChatMessage>()
            }

        } else {
            // Non-existent 'next' option:
            Log.w(TAG, "ERROR: Next ID '${llmReturn.next}' not in nextIds array!")
            updState.fail = true
        }
        return updState
    }

    fun updateStateFlow(
        prevState: StateInfo,
        llmReturn: LlmReturn,
    ): StateInfo {
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
        return updState
    }

    // (Open) Custom invoke logic:
    open fun invoke(
        prevState: StateInfo): StateInfo {
        // TODO
        var updState = prevState
        return updState
    }
}
