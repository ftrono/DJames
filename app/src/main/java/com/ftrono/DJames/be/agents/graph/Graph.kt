package com.ftrono.DJames.be.agents.graph

import android.util.Log
import com.ftrono.DJames.be.agents.ChatMessage
import com.ftrono.DJames.be.agents.nodes.Node
import com.ftrono.DJames.be.agents.StateInfo
import com.ftrono.DJames.application.START
import com.ftrono.DJames.application.END
import com.ftrono.DJames.be.agents.NodeType


class Graph(
    val name: String
) {
    private val TAG = this::class.java.simpleName + "_" + name

    var nodes = mutableMapOf<String, Node>()

    fun addNode(node: Node) {
        nodes.putIfAbsent(node.name, node)
    }

    fun getNode(name: String): Node {
        try {
            return nodes[name]!!
        } catch (e: Exception) {
            throw Exception("Node '$name' not found in graph!")
        }
    }

    fun invoke(
        prevState: StateInfo
    ): StateInfo {
        var updState = prevState
        if (updState.next == START) updState.next = nodes.keys.first()
        Log.d(TAG, "Graph streaming loop STARTED from Node: '${updState.next}'.")

        // TODO: STREAMING LOOP:
        while (updState.next != END && !updState.fail) {
            Log.d(TAG, "Streaming from Node: ${updState.next}")
            updState = getNode(updState.next).invoke(updState)
            // Human-in-the-Loop:
            if (updState.interrupt) {
                Log.d(TAG, "Interrupt requested!")
                break
            } else if (updState.next == END) {
                updState.end = true
            }
            Log.d(TAG, "Next node -> ${updState.next}")
        }
        Log.d(TAG, "Graph streaming loop ENDED.")
        return updState
    }
}
