package com.ftrono.DJames.kaigraph

import com.ftrono.DJames.be.database.Attachments
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