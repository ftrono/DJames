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
) : NodeAction<SimpleState?> {

    private val TAG = AgentNode::class.java.simpleName

    override fun apply(state: SimpleState?): MutableMap<String?, Any?> {

        var outMessage = ""
        val prompt = """
            You are DJames, a smart driving assistant and personal virtual DJ! 
            You speak like an English personal chauffeur. You are helpful and gentle. 
            Use the available tools provided to get the list of available songs for the artist requested by the user.
        """
        val currentMessages = state!!.messages()
        Log.d(TAG, "Current messages: $currentMessages")

        val llmAgent = LlmAgent(
            context = context,
            apiKey = apiKey,
            basePrompt = prompt,
            tools = mapOf<String, Tool>(
                "searchTracks" to SearchTracks()
            )
        )

        outMessage = llmAgent.invoke(inMessage = currentMessages.last()!!)
        return mutableMapOf<String?, Any?>(
            SimpleState.MESSAGES_KEY to outMessage
        )
    }
}

// Node that adds a greeting
class GreeterNode : NodeAction<SimpleState?> {
    override fun apply(state: SimpleState?): MutableMap<String?, Any?> {
        println("GreeterNode executing. Current messages: " + state!!.messages())
        return mutableMapOf<String?, Any?>(
            SimpleState.MESSAGES_KEY to "Hello from GreeterNode!"
        )
    }
}

// Node that adds a response
class ResponderNode : NodeAction<SimpleState?> {
    override fun apply(state: SimpleState?): MutableMap<String?, Any?> {
        println("ResponderNode executing. Current messages: " + state!!.messages())
        val currentMessages = state.messages()
        if (currentMessages.contains("Hello from GreeterNode!")) {
            return mutableMapOf<String?, Any?>(
                SimpleState.MESSAGES_KEY to "Acknowledged greeting!"
            )
        }
        return mutableMapOf<String?, Any?>(
            SimpleState.MESSAGES_KEY to "No greeting found."
        )
    }
}
