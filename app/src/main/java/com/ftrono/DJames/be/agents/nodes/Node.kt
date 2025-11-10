package com.ftrono.DJames.be.agents.nodes

import com.ftrono.DJames.be.agents.NodeType
import com.ftrono.DJames.be.agents.StateInfo


open class Node() {

    open val name: String = ""
    open val type: NodeType = NodeType.AGENT

    open fun invoke(
        prevState: StateInfo): StateInfo {
        // TODO
        var updState = prevState
        return updState
    }
}
