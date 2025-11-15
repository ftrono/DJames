package com.ftrono.DJames.be.agents.tools

import com.ftrono.DJames.be.agents.ToolDefinition
import com.ftrono.DJames.be.agents.ToolFunction
import com.ftrono.DJames.be.agents.ToolParameters
import com.ftrono.DJames.be.agents.ToolProperty
import com.ftrono.DJames.be.agents.ToolType
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

    open fun invoke(args: JsonObject): String {
        return ""
    }

}