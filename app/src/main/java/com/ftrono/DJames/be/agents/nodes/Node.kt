package com.ftrono.DJames.be.agents.nodes

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.dateOnlyFormat
import com.ftrono.DJames.application.lastRequestIntent
import com.ftrono.DJames.application.messageUtils
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.agents.LlmReply
import com.ftrono.DJames.be.agents.LlmReturn
import com.ftrono.DJames.be.agents.NodeType
import com.ftrono.DJames.be.agents.StateInfo
import com.ftrono.DJames.be.agents.promptDateStr
import com.ftrono.DJames.be.agents.promptIntro
import com.ftrono.DJames.be.agents.promptJsonIntro
import com.ftrono.DJames.be.agents.promptJsonOut
import com.ftrono.DJames.be.agents.promptRouterIntro
import com.ftrono.DJames.be.agents.promptRouterOut
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

    // Build applicable System prompt:
    fun buildSystemPrompt(
        isRouter: Boolean = false,
        useJson: Boolean = false,
    ): String {
        if (isRouter) {
            return promptRouterIntro
        } else {
            val curDate = utils.convertTimestamp(utils.getCurrentTimestamp(), dateOnlyFormat)
            return if (useJson) promptJsonIntro.replace(promptDateStr, curDate) else promptIntro.replace(promptDateStr, curDate)
        }
    }

    // Build applicable User prompt:
    fun buildUserPrompt(
        systemPrompt: String,
        userCorePrompt: String,
        isRouter: Boolean = false,
        useJson: Boolean = false,
    ): String {
        var userPrompt = systemPrompt + "\n" + userCorePrompt
        if (useJson) {
            userPrompt = userPrompt + "\n" + promptJsonOut
        } else if (isRouter) {
            userPrompt = userPrompt + "\n" + promptRouterOut
        }
        return userPrompt
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
