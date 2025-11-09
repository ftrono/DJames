package com.ftrono.DJames.be.agents.tools

import com.ftrono.DJames.be.agents.ToolDefinition
import com.ftrono.DJames.be.agents.ToolFunction
import com.ftrono.DJames.be.agents.ToolParameters
import com.ftrono.DJames.be.agents.ToolProperty
import kotlinx.serialization.json.JsonObject

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