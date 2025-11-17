package com.ftrono.DJames.be.agents.nodes

import android.content.Context
import android.util.Log
import com.ftrono.DJames.be.agents.llm.LlmAgent
import com.ftrono.DJames.be.agents.data.StateInfo
import com.ftrono.DJames.be.agents.tools.*


// (LLM-based) ReAct agent node:
class CallAgentNode (
    private val context: Context,
    private val apiKey: String,
    override val onComplete: String,
    override val onFallback: String,
) : Node() {

    override val TAG = this::class.java.simpleName
    override val name: String = TAG.replace("Node", "")

    override fun invoke(prevState: StateInfo): StateInfo {
        Log.d(TAG, "$name activated")
        var updState = prevState

        // Build prompt:
        var corePrompt = """
            ## TASK:
            Your task is to help the user make a call to one of his contacts. 
            Any request not involving making a call is outside your tasks scope.
            Use the available tools provided to get the list of available contacts that the user can call.
        """.trimIndent()
        var inMessages = prepareInMessages(
            origMessages = prevState.messages,
            corePrompt = corePrompt,
        )

        val llmAgent = LlmAgent(
            context = context,
            apiKey = apiKey,
            agentName = name,
            onComplete = onComplete,
            onFallback = onFallback,
            tools = mapOf<String, Tool>(
                ToolHandoff().name to ToolHandoff(),
                ToolRetrieveContacts().name to ToolRetrieveContacts(),
                ToolCall().name to ToolCall(),
            ),
        )

        val llmReturn = llmAgent.invoke(llmMessages = inMessages)
        updState = updateStateFlow(updState, llmReturn)
        return updState
    }
}