package com.ftrono.DJames.be.agents.graph

import android.util.Log
import com.ftrono.DJames.be.agents.nodes.Node
import com.ftrono.DJames.be.agents.StateInfo
import com.ftrono.DJames.application.START
import com.ftrono.DJames.application.END


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
        updState.interrupt = false   // turn off from previous run
        if (updState.next == START) updState.next = nodes.keys.first()
        Log.d(TAG, "Graph streaming loop STARTED from Node: '${updState.next}'.")

        // TODO: STREAMING LOOP:
        while (updState.next != END && !updState.fail) {
            Log.d(TAG, "Streaming from Node: ${updState.next}")
            // Log.d(TAG, "State -> $updState")
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
        // Log.d(TAG, "Final State -> $updState")
        return updState
    }
}
