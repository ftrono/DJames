package com.ftrono.DJames.be.agents.nodes

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.mistralLlmModelSmall
import com.ftrono.DJames.kaigraph.llm.LlmAgent
import com.ftrono.DJames.kaigraph.data.StateInfo
import com.ftrono.DJames.be.agents.data.handoffDescription
import com.ftrono.DJames.be.agents.tools.*
import com.ftrono.DJames.kaigraph.node.Node
import com.ftrono.DJames.kaigraph.tool.Tool


// (LLM-based) ReAct agent node:
class GuidanceAgentNode (
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
            Your only task is to provide information on your functionalities to the user, depending on what info they are requesting.
            Your functionalities enable you to do the following:
                - Search and play songs or podcasts via Spotify;
                - Call the user's contacts;
                - Send SMS messages or Whatsapp text / voice messages to the user's contacts;
                - Search for a place on Google Maps and show the driving route to the user.
            
            EACH of these functionalities are performed by specialized LLM agents. This means that:
                a) If the user is actually asking to do any of that, you MUST call **tool_handoff**;
                b) If the user is only asking information on them, you can answer by explaining very shortly;
                c) If the user is asking ANYTHING that's outside your functionalities scope, just say politely that it's not your job;
                c) If the user of a tool are asking to END end, stop or restart the conversation, you MUST call **tool_handoff**.
            
            **Always answer in plain text** (no numbered / bullet points lists and no markdowns). Also, **always end your replies by asking the user what does it need or how can you help**.
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
                ToolHandoff(handoffDescription).name to ToolHandoff(handoffDescription),
            ),
        )

        val llmReturn = llmAgent.invoke(
            llmMessages = inMessages,
            attachments = updState.attachments
        )

        updState = updateStateFromNode(updState, llmReturn)
        return updState
    }
}
