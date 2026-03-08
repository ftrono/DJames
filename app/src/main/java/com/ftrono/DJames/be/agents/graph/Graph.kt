package com.ftrono.DJames.be.agents.graph

import android.content.Context
import android.util.Log
import com.ftrono.DJames.be.agents.nodes.Node
import com.ftrono.DJames.be.agents.data.StateInfo
import com.ftrono.DJames.application.START
import com.ftrono.DJames.application.END
import com.ftrono.DJames.application.maxGraphLoops
import com.ftrono.DJames.be.agents.data.ChatMessage
import com.ftrono.DJames.be.agents.data.LlmReturn
import com.ftrono.DJames.be.models.RecDetails


open class Graph(
    private val context: Context,
) {
    open val name: String = this::class.java.simpleName
    open var TAG = name

    var graph = mutableMapOf<String, Node>()

    // Graph utils:
    fun addNode(node: Node) {
        graph.putIfAbsent(node.name, node)
    }

    fun addNodes(nodes: MutableList<Node>) {
        for (node in nodes) {
            addNode(node)
        }
    }

    fun getNode(name: String): Node {
        try {
            return graph[name]!!
        } catch (e: Exception) {
            throw Exception("Node '$name' not found in graph!")
        }
    }

    // Abstract:
    open fun build() {
        // TODO
    }

    open fun loadMessages(): MutableList<ChatMessage> {
        // TODO
        return mutableListOf()
    }

    open fun processUserMessage(
        prevState: StateInfo,
        recDetails: RecDetails?,
        inMessage: String,
    ): StateInfo {
        // TODO
        return StateInfo()
    }

    open fun invoke(
        prevState: StateInfo,
        recDetails: RecDetails? = null,
        inMessage: String = "",
    ): StateInfo {
        // TODO
        return StateInfo()
    }

    // CENTRALIZED:
    // Stream graph:
    fun stream(
        prevState: StateInfo,
        routerNodes: List<String>,
        onRestart: String = "",
    ): StateInfo {

        var updState = prevState
        var loops = 0
        updState.interrupt = false   // turn off from previous run
        if (updState.next == START) {
            updState.isStart = true
            updState.next = graph.keys.first()
        } else {
            updState.isStart = false
        }
        Log.d(TAG, "Graph streaming loop STARTED from Node: '${updState.next}'.")

        // STREAMING LOOP:
        while (updState.next != END && !updState.fail) {
            if (loops >= maxGraphLoops) {
                // Avoid infinite looping:
                updState.fail = true
                Log.d(TAG, "Max graph loops reached! Interrupting...")
                break

            } else {
                Log.d(TAG, "Streaming from Node: ${updState.next}")
                // Log.d(TAG, "State -> $updState")
                val newNode = getNode(updState.next)
                if (routerNodes.contains(newNode.name)) loops += 1
                updState = newNode.invoke(updState)   // invoke
                // Human-in-the-Loop:
                if (updState.interrupt) {
                    Log.d(TAG, "Interrupt requested!")
                    break
                } else if (updState.next == START) {
                    // Fresh start:
                    updState = StateInfo(
                        next = onRestart,
                        isStart = true,
                        messages = mutableListOf(
                            ChatMessage(
                                role = "user",
                                content = "Hi!"
                            )
                        )
                    )
                }
                Log.d(TAG, "Next node -> ${updState.next}")
            }
        }
        Log.d(TAG, "Graph streaming loop ENDED.")
        // Log.d(TAG, "Final State -> $updState")
        return updState
    }
}
