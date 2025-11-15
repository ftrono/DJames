package com.ftrono.DJames.be.agents.nodes

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.dateOnlyFormat
import com.ftrono.DJames.application.lastRequestIntent
import com.ftrono.DJames.application.messageUtils
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.agents.ChatMessage
import com.ftrono.DJames.be.agents.LlmReply
import com.ftrono.DJames.be.agents.LlmReturn
import com.ftrono.DJames.be.agents.NodeType
import com.ftrono.DJames.be.agents.StateInfo
import com.ftrono.DJames.be.agents.promptDateStr
import com.ftrono.DJames.be.agents.promptEnd
import com.ftrono.DJames.be.agents.promptGenderStr
import com.ftrono.DJames.be.agents.promptIntro
import com.ftrono.DJames.be.agents.promptJsonOut
import com.ftrono.DJames.be.agents.promptRouterIntro
import com.ftrono.DJames.be.agents.promptRouterOut
import com.ftrono.DJames.be.agents.promptUserStr
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json


open class Node() {

    open val name: String = ""
    open val type: NodeType = NodeType.AGENT
    open val useJson: Boolean = false
    open val nextOptions: List<String> = listOf()
    open val onComplete: String = ""
    open val onFallback: String = ""
    open val TAG = this::class.java.simpleName


    // Build applicable User prompt:
    fun addPromptsToMessages(
        origMessages: MutableList<ChatMessage>,
        corePrompt: String,
        isRouter: Boolean = false,
        useJson: Boolean = false,
        joinMessages: Boolean = false,
    ): MutableList<ChatMessage> {

        // 1) Build system prompt:
        val systemPrompt = if (isRouter) {
                promptRouterIntro
            } else {
                val curDate = utils.convertTimestamp(utils.getCurrentTimestamp(), dateOnlyFormat)
                promptIntro.replace(promptDateStr, curDate).replace(promptGenderStr, prefs.userGender)
            }

        // 2) Prepare user prompt:
        var userPrompt = if (useJson) {
                systemPrompt + "\n" + corePrompt + "\n" + promptJsonOut
            } else if (isRouter) {
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

    // Clean & parse Json output from a LLM reply:
    fun decodeJson(text: String): LlmReply {
        val cleanText = text.replace("```json", "").replace("```", "").trim()
        return Json.decodeFromString<LlmReply>(cleanText)
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

        } else {
            // Non-existent 'next' option:
            Log.w(TAG, "ERROR: Next ID '${llmReturn.next}' not in nextIds array!")
            updState.fail = true
        }
        return updState
    }

    // (Agent) Extract & use 'next' & 'reply' from JSON output:
    fun parseJson(
        llmReturn: LlmReturn,
        prevState: StateInfo,
    ): StateInfo {
        var updState = prevState

        // Parse structured JSON output:
        val outJson = decodeJson(llmReturn.messages.last().content)
        if (outJson.next == "HANDOFF") {
            // Only "HANDOFF", no messages:
            updState.next = onFallback

        } else {
            // Update Next and messages list:
            if (outJson.next == "HUMAN") {
                updState.interrupt = true
            } else {
                updState.next = outJson.next
            }
            llmReturn.messages.last().content = outJson.reply.replace("* ", "- ")
            updState.messages.addAll(llmReturn.messages)
            updState.intentName = name
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
