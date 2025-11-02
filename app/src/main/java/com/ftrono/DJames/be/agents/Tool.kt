package com.ftrono.DJames.be.agents

import kotlinx.serialization.json.*


open class Tool {

    open fun getName(): String {
        return ""
    }

    open fun getDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = ToolFunction(
                name = this.getName(),
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

    open fun invoke(args: JsonObject): String {
        return ""
    }

}