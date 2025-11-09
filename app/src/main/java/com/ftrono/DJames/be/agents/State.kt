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

    fun isStart(): Boolean {
        return this.value<Boolean>("isStart")
            .orElse(false)
    }

    fun isEnd(): Boolean {
        return this.value<Boolean>("isEnd")
            .orElse(false)
    }

    fun fail(): Boolean {
        return this.value<Boolean>("fail")
            .orElse(false)
    }

    fun next(): String {
        return this.value<String>("next")
            .orElse("")
    }

    fun lastUserMessageId(): Long {
        return this.value<Long>("lastUserMessageId")
            .orElse(0L)
    }

    fun lastUserIntent(): String {
        return this.value<String>("lastUserIntent")
            .orElse("")
    }

    fun language(): String {
        return this.value<String>("language")
            .orElse("")
    }

    companion object {
        // Keys:
        const val MESSAGES: String = "messages"
        const val IS_START: String = "isStart"
        const val IS_END: String = "isEnd"
        const val FAIL: String = "fail"
        const val NEXT: String = "next"
        const val LAST_USER_MESSAGE_ID: String = "lastUserMessageId"
        const val LANGUAGE: String = "language"

        // Define state schema:
        val SCHEMA: MutableMap<String?, Channel<*>?> = mutableMapOf<String?, Channel<*>?>(
            MESSAGES to Channels.appenderWithDuplicate<Any?> { ArrayList() },
            IS_START to Channels.base<Boolean> { false },
            IS_END to Channels.base<Boolean> { false },
            FAIL to Channels.base<Boolean> { false },
            NEXT to Channels.base<String> { "" },
            LAST_USER_MESSAGE_ID to Channels.base<Long> { 0L },
            LANGUAGE to Channels.base<String> { "" },
        )
    }
}
