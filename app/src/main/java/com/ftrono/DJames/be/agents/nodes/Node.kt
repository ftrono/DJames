package com.ftrono.DJames.be.agents.nodes

import com.ftrono.DJames.be.agents.ChatMessage
import com.ftrono.DJames.be.agents.NodeType


open class Node(
    var name: String = "",
    var type: NodeType = NodeType.AGENT,
    var callerIds: List<String> = listOf(),
    var nextIds: List<String> = listOf()
) {
    open fun invoke(
        inMessages: MutableList<ChatMessage>,
    ) {
        // TODO
    }
}
