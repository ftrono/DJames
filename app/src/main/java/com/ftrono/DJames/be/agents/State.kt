package com.ftrono.DJames.be.agents

import org.bsc.langgraph4j.StateGraph.END
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

    fun next(): String {
        return this.value<String>("next")
            .orElse("")
    }

    companion object {
        // Keys:
        const val MESSAGES: String = "messages"
        const val FAIL: String = "fail"
        const val NEXT: String = "next"

        // Define state schema:
        val SCHEMA: MutableMap<String?, Channel<*>?> = mutableMapOf<String?, Channel<*>?>(
            MESSAGES to Channels.appenderWithDuplicate<Any?> { ArrayList() },
            FAIL to Channels.base<Boolean> { false },
            NEXT to Channels.base<String> { "" },
        )
    }
}
