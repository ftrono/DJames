package com.ftrono.DJames.be.agents

import org.bsc.langgraph4j.state.AgentState
import org.bsc.langgraph4j.state.Channel
import org.bsc.langgraph4j.state.Channels


// Define the state for our graph
class StateMap(initData: MutableMap<String?, Any?>) : AgentState(initData) {
    // Getters:
    fun messages(): MutableList<ChatMessage?> {
        return this.value<MutableList<ChatMessage?>>("messages")
            .orElse(mutableListOf<ChatMessage?>())
    }

    fun fail(): Boolean {
        return this.value<Boolean>("fail")
            .orElse(false)
    }

    companion object {
        const val MESSAGES: String = "messages"
        const val FAIL: String = "fail"

        // Define state schema: new messages will be appended.
        val SCHEMA: MutableMap<String?, Channel<*>?> = mutableMapOf<String?, Channel<*>?>(
            MESSAGES to Channels.appenderWithDuplicate<Any?> { ArrayList() },
            FAIL to Channels.base<Boolean> { false }
        )
    }
}
