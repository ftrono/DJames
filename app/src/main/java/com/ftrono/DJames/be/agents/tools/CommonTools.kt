package com.ftrono.DJames.be.agents.tools

import com.ftrono.DJames.be.agents.data.ToolDefinition
import com.ftrono.DJames.be.agents.data.ToolFunction
import com.ftrono.DJames.be.agents.data.ToolParameters
import com.ftrono.DJames.be.agents.data.ToolResponse
import com.ftrono.DJames.be.agents.data.ToolType
import com.ftrono.DJames.be.database.Attachments
import kotlinx.serialization.json.*


class ToolHandoff(): Tool() {
    private val TAG = this::class.java.simpleName
    override val name = "tool_handoff"
    override val type: ToolType = ToolType.HANDOFF

    override fun getDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = ToolFunction(
                name = name,
                description = """
                    Handoff tool. Use this tool if the user either: 
                        (i) wants to end/stop the conversation; 
                        (ii) is requesting guidance or info about your capabilities; 
                        (iii) in **any case* the user makes a request outside your tasks scope.
                    """.trimIndent(),
                parameters = ToolParameters(
                    type = "object",
                    properties = mapOf(),
                )
            )
        )
    }

    override fun invoke(args: JsonObject, attachments: Attachments): ToolResponse {
        return ToolResponse(
            message = "",
            attachments = attachments,
        )   // Not needed
    }
}