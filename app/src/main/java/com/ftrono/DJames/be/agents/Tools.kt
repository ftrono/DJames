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


class SearchContacts(): Tool() {
    override fun getName(): String {
        return "searchContacts"
    }

    override fun getDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = ToolFunction(
                name = this.getName(),
                description = "Get the full list of contacts that the user can call or send messages to.",
                parameters = ToolParameters(
                    type = "object",
                    properties = mapOf(),
                )
            )
        )
    }

    override fun invoke(args: JsonObject): String {
        val allTracks = listOf(
            "amal", "myself", "rick", "mom", "dad"
        )
        return "Contacts that can be called:\n${allTracks}"
    }
}


class SearchPlaces(): Tool() {
    override fun getName(): String {
        return "searchPlaces"
    }

    override fun getDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = ToolFunction(
                name = this.getName(),
                description = "Get the full list of places the user can go nearby.",
                parameters = ToolParameters(
                    type = "object",
                    properties = mapOf(),
                )
            )
        )
    }

    override fun invoke(args: JsonObject): String {
        val allPlaces = listOf(
            "lecce", "florence", "cagliari", "manchester", "rome", "trento"
        )
        return "Places reachable nearby:\n${allPlaces}"
    }
}
