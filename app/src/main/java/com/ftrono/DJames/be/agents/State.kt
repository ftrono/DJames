package com.ftrono.DJames.be.agents

import org.bsc.langgraph4j.state.AgentState
import org.bsc.langgraph4j.state.Channel
import org.bsc.langgraph4j.state.Channels


// Define the state for our graph
class StateMap(initData: MutableMap<String?, Any?>) : AgentState(initData) {
    fun messages(): MutableList<ChatMessage?> {
        return this.value<MutableList<ChatMessage?>>("messages")
            .orElse(mutableListOf<ChatMessage?>())
    }

    companion object {
        const val MESSAGES: String = "messages"

        // Define the schema for the state.
        // MESSAGES will hold a list of strings, and new messages will be appended.
        val SCHEMA: MutableMap<String?, Channel<*>?> = mutableMapOf<String?, Channel<*>?>(
            MESSAGES to Channels.appenderWithDuplicate<Any?> { ArrayList() }
        )
    }
}
