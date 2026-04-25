package com.ftrono.DJames.be.agents.tools

import com.ftrono.DJames.kaigraph.data.ToolDefinition
import com.ftrono.DJames.kaigraph.data.ToolFunction
import com.ftrono.DJames.kaigraph.data.ToolParameters
import com.ftrono.DJames.kaigraph.data.ToolResponse
import com.ftrono.DJames.kaigraph.data.ToolType
import com.ftrono.DJames.be.database.Attachments
import com.ftrono.DJames.kaigraph.tool.Tool
import kotlinx.serialization.json.*


class ToolHandoff(
    val description: String
): Tool() {
    private val TAG = this::class.java.simpleName
    override val name = "tool_handoff"
    override val type: ToolType = ToolType.HANDOFF

    override fun getDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = ToolFunction(
                name = name,
                description = description,
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