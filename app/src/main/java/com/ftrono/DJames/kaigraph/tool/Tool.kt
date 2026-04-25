package com.ftrono.DJames.kaigraph.tool

import com.ftrono.DJames.be.database.Attachments
import com.ftrono.DJames.kaigraph.data.ToolDefinition
import com.ftrono.DJames.kaigraph.data.ToolFunction
import com.ftrono.DJames.kaigraph.data.ToolParameters
import com.ftrono.DJames.kaigraph.data.ToolProperty
import com.ftrono.DJames.kaigraph.data.ToolResponse
import com.ftrono.DJames.kaigraph.data.ToolType
import kotlinx.serialization.json.JsonObject

open class Tool {
    open val name: String = ""
    open val type: ToolType = ToolType.INTERMEDIATE

    open fun getDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = ToolFunction(
                name = name,
                description = "Function description",
                parameters = ToolParameters(
                    type = "object",
                    properties = mapOf(
                        "param1" to ToolProperty(
                            type = "",   // Arg type
                            description = ""   // Arg description
                        ),
                    ),
                )
            )
        )
    }

    open fun invoke(args: JsonObject, attachments: Attachments): ToolResponse {
        return ToolResponse(
            message = "",
            attachments = attachments,
        )
    }

}