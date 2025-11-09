package com.ftrono.DJames.be.agents.graph

import android.util.Log
import com.ftrono.DJames.be.agents.ChatMessage
import com.ftrono.DJames.be.agents.nodes.Node


class Graph() {
    private val TAG = this::class.java.simpleName
    var START = "__START__"
    var END = "__END__"

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
        startNode: String = START,
        inMessages: MutableList<ChatMessage>,
    ) {
        var curNodeId = startNode
        while (curNodeId != END) {
            val curNode = getNode(curNodeId)

        }
    }
}
