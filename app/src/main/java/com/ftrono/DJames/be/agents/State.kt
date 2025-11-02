package com.ftrono.DJames.be.agents

import org.bsc.langgraph4j.prebuilt.MessagesState
import org.bsc.langgraph4j.state.AgentState
import org.bsc.langgraph4j.state.Channel
import org.bsc.langgraph4j.state.Channels


// Define the state for our graph
class SimpleState(initData: MutableMap<String?, Any?>) : AgentState(initData) {
    fun messages(): MutableList<String?> {
        return this.value<MutableList<String?>>("messages")
            .orElse(mutableListOf<String?>())
    }

    companion object {
        const val MESSAGES_KEY: String = "messages"

        // Define the schema for the state.
        // MESSAGES_KEY will hold a list of strings, and new messages will be appended.
        val SCHEMA: MutableMap<String?, Channel<*>?> = mutableMapOf<String?, Channel<*>?>(
            MESSAGES_KEY to Channels.appender<Any?> { ArrayList() }
        )
    }
}
