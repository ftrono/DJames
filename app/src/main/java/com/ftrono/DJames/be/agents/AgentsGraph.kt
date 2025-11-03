package com.ftrono.DJames.be.agents

import android.content.Context
import android.util.Log
import com.ftrono.DJames.R
import com.ftrono.DJames.application.lastRecordingName
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.be.models.AiReply
import com.ftrono.DJames.be.models.DispatcherInfo
import com.google.gson.JsonParser
import org.bsc.langgraph4j.CompiledGraph
import org.bsc.langgraph4j.StateGraph
import org.bsc.langgraph4j.StateGraph.END
import org.bsc.langgraph4j.StateGraph.START
import org.bsc.langgraph4j.action.AsyncNodeAction.node_async
import org.bsc.langgraph4j.state.AgentStateFactory
import java.io.BufferedReader
import java.io.InputStreamReader


// MAIN GRAPH:
class AgentsGraph(
    private val context: Context,
) {
    private val TAG = AgentsGraph::class.java.simpleName

    //GET LLM CREDENTIALS:
    val reader = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.env)))
    private val apiKey = JsonParser.parseReader(reader).asJsonObject.get("mistral_api_key").asString

    // Initialize:
    val stateGraph = this.build()
    var messages: MutableList<ChatMessage?> = this.loadMessages()

    fun build(): StateGraph<StateMap?> {
        // Initialize nodes
        val agentNode = AgentNode(context, apiKey)

        // Define the graph structure
        val stateGraph = StateGraph<StateMap?>(
            StateMap.SCHEMA,
            AgentStateFactory { initData: MutableMap<String?, Any?>? ->
                StateMap(
                    initData!!
                )
            })
//            .addNode("greeter", node_async(greeterNode))
//            .addNode("responder", node_async(responderNode))
            .addNode("agent", node_async(agentNode))
            // Define edges
            .addEdge(START, "agent")
//            .addEdge("greeter", "responder")
//            .addEdge("responder", "agent")
            .addEdge("agent", END)

        return stateGraph
    }

    fun loadMessages(): MutableList<ChatMessage?> {
        // TODO: Build initial messages list:
        val msgList = mutableListOf<ChatMessage?>(
            // ChatMessage(role = "user", content = inMessage)
        )
        Log.d(TAG, "Messages size: ${msgList.size} items.")
        return msgList
    }

    fun compile(stateGraph: StateGraph<StateMap?>): CompiledGraph<StateMap?> {
        return stateGraph.compile()
    }

    fun invoke(
        inMessage: String
    ): DispatcherInfo {
        // Build & compile the graph:
        val compiledGraph = this.compile(stateGraph)
        messages.add(
            ChatMessage(role = "user", content = inMessage)
        )
        Log.d(TAG, "Messages size (input): ${messages.size} items.")

        // Run the graph:
        // The `stream` method returns an AsyncGenerator. Results are in the final state after execution:
        var outMessage = ""
        var fail = false

        for (item in compiledGraph.stream(
            // Input:
            mutableMapOf<String?, Any?>(
                StateMap.MESSAGES to messages
            )
        )) {
            // Output:
            Log.d(TAG, item.toString())
            messages = item.state()!!.messages()
            outMessage = item.state()!!.messages().last()!!.content
            fail = item.state()!!.fail()
            Log.d(TAG, "Messages size (output): ${messages.size} items, fail: $fail.")
        }

        return DispatcherInfo(
            lastRecording = lastRecordingName,
            testV3 = true,
            followUp = !fail,
            fail = fail,
            aiReplies = listOf(
                AiReply(
                    langCode = prefs.queryLanguage,
                    text = outMessage
                )
            )
        )
    }
}