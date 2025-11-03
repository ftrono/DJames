package com.ftrono.DJames.be.agents

import android.content.Context
import android.util.Log
import org.bsc.langgraph4j.action.NodeAction
import java.util.Map


interface Assistant {
    fun chat(userMessage: String): String
}

// Node that adds a greeting
class AgentNode (
    private val context: Context,
    private val apiKey: String,
) : NodeAction<StateMap?> {

    private val TAG = AgentNode::class.java.simpleName

    override fun apply(state: StateMap?): MutableMap<String?, Any?> {
        val prompt = """
            You are DJames, a smart driving assistant and personal virtual DJ! 
            You speak like an English personal chauffeur. You are helpful and gentle. 
            Use the available tools provided to get the list of available songs for the artist requested by the user.
        """
        var inMessages = state!!.messages()
        Log.d(TAG, "Current messages: $inMessages")

        val llmAgent = LlmAgent(
            context = context,
            apiKey = apiKey,
            basePrompt = prompt,
            tools = mapOf<String, Tool>(
                "searchTracks" to SearchTracks()
            )
        )

        val llmReturn = llmAgent.invoke(llmMessages = inMessages)
        //TODO: Add fail to return and store messages to DB here!
        return mutableMapOf<String?, Any?>(
            StateMap.MESSAGES to llmReturn.messages,
            StateMap.FAIL to llmReturn.fail,
        )
    }
}
