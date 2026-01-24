package com.ftrono.DJames.be.agents.nodes

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.END
import com.ftrono.DJames.application.mistralLlmModelSmall
import com.ftrono.DJames.be.agents.data.ChatMessage
import com.ftrono.DJames.be.agents.llm.LlmAgent
import com.ftrono.DJames.be.agents.data.StateInfo
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
    val model = mistralLlmModelSmall

    override fun invoke(prevState: StateInfo): StateInfo {
        Log.d(TAG, "${name} activated")
        var updState = prevState

        // Build prompt:
        var corePrompt = """
            ## TASK:
            Your domain is places, maps and driving directions. Your task is to help the user find a place to drive to.
            Any request not involving places, maps or driving directions is outside your tasks scope.
            Use the available tools provided to get the list of available places the user can go nearby.
        """.trimIndent()
        var inMessages = prepareInMessages(
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
                ToolHandoff().name to ToolHandoff(),
                ToolRetrieveDriver().name to ToolRetrieveDriver(),
                ToolGo().name to ToolGo(),
            ),
        )

        val llmReturn = llmAgent.invoke(llmMessages = inMessages)
        updState = updateStateFlow(updState, llmReturn)
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