package com.ftrono.DJames.be.agents.tools

import com.ftrono.DJames.kaigraph.ToolDefinition
import com.ftrono.DJames.kaigraph.ToolFunction
import com.ftrono.DJames.kaigraph.ToolParameters
import com.ftrono.DJames.kaigraph.ToolResponse
import com.ftrono.DJames.kaigraph.ToolType
import com.ftrono.DJames.be.database.Attachments
import com.ftrono.DJames.kaigraph.Tool
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