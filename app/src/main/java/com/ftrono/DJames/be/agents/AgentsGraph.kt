package com.ftrono.DJames.be.agents

import android.content.Context
import android.util.Log
import com.ftrono.DJames.R
import com.ftrono.DJames.application.defaultReplies
import com.ftrono.DJames.application.lastRecordingName
import com.ftrono.DJames.application.prefs
import com.ftrono.DJames.be.models.AiReply
import com.ftrono.DJames.be.models.DispatcherInfo
import com.google.gson.JsonParser
import org.bsc.langgraph4j.CompiledGraph
import org.bsc.langgraph4j.StateGraph
import org.bsc.langgraph4j.StateGraph.END
import org.bsc.langgraph4j.StateGraph.START
import org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async
import org.bsc.langgraph4j.action.AsyncNodeAction.node_async
import org.bsc.langgraph4j.state.AgentStateFactory
import org.bsc.langgraph4j.utils.EdgeMappings
import java.io.BufferedReader
import java.io.InputStreamReader


// MAIN GRAPH:
class AgentsGraph(
    private val context: Context,
) {
    private val TAG = this::class.java.simpleName

    //GET LLM CREDENTIALS:
    val reader = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.env)))
    private val apiKey = JsonParser.parseReader(reader).asJsonObject.get("mistral_api_key").asString

    // Initialize:
    val stateGraph = this.build()
    var messages: MutableList<ChatMessage?> = this.loadMessages()

    fun build(): StateGraph<StateMap?> {
        // Define the graph structure:
        val stateGraph = StateGraph<StateMap?>(
            StateMap.SCHEMA,
            AgentStateFactory { initData: MutableMap<String?, Any?>? ->
                StateMap(
                    initData!!
                )
            })
            // Nodes:
            .addNode("MainRouter", node_async(MainRouterNode(context, apiKey)))
            .addNode("PlayerAgent", node_async(PlayerAgentNode(context, apiKey)))
            .addNode("CallAgent", node_async(CallAgentNode(context, apiKey)))
            .addNode("MessageAgent", node_async(MessageAgentNode(context, apiKey)))
            .addNode("DriveAgent", node_async(DriveAgentNode(context, apiKey)))
            .addNode("GuidanceAgent", node_async(GuidanceAgentNode(context, apiKey)))
            // Edges:
            .addEdge(START, "MainRouter")
            .addConditionalEdges(
                "MainRouter",
                edge_async { state ->
                    state!!.next()
                },
                EdgeMappings.builder()
                    .to("PlayerAgent")
                    .to("CallAgent")
                    .to("MessageAgent")
                    .to("DriveAgent")
                    .to("GuidanceAgent")
                    .toEND(END)
                    .build()
            )
            // End:
            .addEdge("PlayerAgent", END)
            .addEdge("CallAgent", END)
            .addEdge("MessageAgent", END)
            .addEdge("DriveAgent", END)
            .addEdge("GuidanceAgent", END)

        Log.d(TAG, "Graph built!")
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
        Log.d(TAG, "Graph compiled!")
        messages.add(
            ChatMessage(role = "user", content = inMessage)
        )
        Log.d(TAG, "Messages size (input): ${messages.size} items.")

        // Run the graph:
        // The `stream` method returns an AsyncGenerator. Results are in the final state after execution:
        var outMessage = ""
        var fail = false
        var next = ""

        for (item in compiledGraph.stream(
            // Input:
            mutableMapOf<String?, Any?>(
                StateMap.MESSAGES to messages
            )
        )) {
            // Output:
            // Log.d(TAG, item.toString())
            messages = item.state()!!.messages()
            outMessage = item.state()!!.messages().last()!!.content.replace("* ", "- ")
            fail = item.state()!!.fail()
            next = item.state()!!.next()
        }
        Log.d(TAG, "Messages size (output): ${messages.size} items, fail: $fail.")

        //TODO: Add store messages to DB here!

        return DispatcherInfo(
            lastRecording = lastRecordingName,
            testV3 = true,
            followUp = !fail && next != END,
            fail = fail,
            end = next == END,
            aiReplies = listOf(
                AiReply(
                    langCode = prefs.queryLanguage,
                    text = if (next != END) outMessage else defaultReplies.replyNevermind()
                )
            )
        )
    }
}