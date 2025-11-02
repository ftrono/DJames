package com.ftrono.DJames.be.agents

import kotlinx.serialization.json.*


class SearchTracks(): Tool() {

    override fun getName(): String {
        return "searchTracks"
    }

    override fun getDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = ToolFunction(
                name = this.getName(),
                description = "Get the full list of tracks that can be played for a given artist.",
                parameters = ToolParameters(
                    type = "object",
                    properties = mapOf(
                        "artist" to ToolProperty(
                            type = "string",   // Arg type
                            description = "The name of the requested artist."   // Arg description
                        ),
                    ),
                )
            )
        )
    }

    override fun invoke(args: JsonObject): String {
        val allTracks = mapOf(
            "the script" to listOf(
                "The man who can't be moved",
                "Science & Faith",
                "Hall of Fame"
            ),
            "john mayer" to listOf(
                "My stupid mouth",
                "Clarity",
                "Split screen sadness"
            ),
            "linkin park" to listOf(
                "Lost",
                "Numb",
                "In the end"
            )
        )

        val artist = args["artist"]?.jsonPrimitive?.content ?: ""
        return "Songs that can be played:\n\n" + allTracks.getOrDefault(
            artist.lowercase(), "No song found for this artist."
        ).toString()
    }

}