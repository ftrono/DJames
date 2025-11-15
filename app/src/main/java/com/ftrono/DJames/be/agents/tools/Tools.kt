package com.ftrono.DJames.be.agents.tools

import android.util.Log
import com.ftrono.DJames.be.agents.ToolDefinition
import com.ftrono.DJames.be.agents.ToolFunction
import com.ftrono.DJames.be.agents.ToolParameters
import com.ftrono.DJames.be.agents.ToolProperty
import com.ftrono.DJames.be.agents.ToolType
import kotlinx.serialization.json.*


class ToolHandoff(): Tool() {
    private val TAG = this::class.simpleName
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

    override fun invoke(args: JsonObject): String {
        return "MainRouter"
    }
}


class ToolRetrievePlayer(): Tool() {
    private val TAG = this::class.simpleName
    override val name = "tool_retrieve"
    override val type: ToolType = ToolType.INTERMEDIATE

    override fun getDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = ToolFunction(
                name = name,
                description = """
                    Get the Spotify ID of the requested item to play. You must pass here ALL the parameters you collect from the user conversation (i.e. artist name, track name, ...). 
                    **Always use this tool to retrieve the Spotify ID** for songs, artists, albums, playlists, podcast episodes or liked songs collection from your knowledge base before playing them!""".trimIndent(),
                parameters = ToolParameters(
                    type = "object",
                    properties = mapOf(
                        "artist" to ToolProperty(
                            type = "string",   // Arg type
                            description = "The name of the requested music artist / band."   // Arg description
                        ),
                        "track" to ToolProperty(
                            type = "string",   // Arg type
                            description = "The name of the requested music track / song."   // Arg description
                        ),
                    ),
                )
            )
        )
    }

    override fun invoke(args: JsonObject): String {
        val allTracks = mapOf(
            "the script" to mapOf(
                "the man who can't be moved" to "0x0001",
                "science & faith" to "0x0002",
                "hall of fame" to "0x0003"
            ),
            "john mayer" to mapOf(
                "my stupid mouth" to "0x0004",
                "clarity" to "0x0005",
                "split screen sadness" to "0x0006"
            ),
            "linkin park" to mapOf(
                "lost" to "0x0007",
                "numb" to "0x0008",
                "in the end" to "0x0009"
            )
        )

        var artist = args["artist"]?.jsonPrimitive?.content ?: ""
        artist = artist.lowercase().trim()
        var track = args["track"]?.jsonPrimitive?.content ?: ""
        track = track.lowercase().trim()

        Log.d(TAG, "Input params: $artist, $track, KEYS: ${allTracks.keys}")

        var retString = if (allTracks.containsKey(artist) && allTracks[artist]!!.containsKey(track)) {
            "Track found! Spotify ID: ${allTracks[artist]!![track]!!}"
        } else if (allTracks.containsKey(artist)) {
            """Songs that can be played by $artist:
                (don't read Spotify IDs to the user):
               ${allTracks[artist]!!}
            """.trimIndent()
        } else {
            "No song found for this artist."
        }
        return retString
    }
}


class ToolPlay(): Tool() {
    private val TAG = this::class.simpleName
    override val name = "tool_play"
    override val type: ToolType = ToolType.ACTION

    override fun getDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = ToolFunction(
                name = name,
                description = """
                     Play the requested music item in Spotify. **Before calling this tool:**
                     1) **Call 'tool_retrieve' first, to get the Spotify ID** for the specific item to play. 
                     2) If 'tool_retrieve' returns multiple options, **ask confirmation** to the user before playing anything!""".trimIndent(),
                parameters = ToolParameters(
                    type = "object",
                    properties = mapOf(
                        "spotify_id" to ToolProperty(
                            type = "string",   // Arg type
                            description = "(Mandatory) Spotify ID of the requested item to play."   // Arg description
                        ),
                    ),
                    required = mutableListOf("spotify_id")
                )
            )
        )
    }

    override fun invoke(args: JsonObject): String {
        var spotifyID = args["spotify_id"]?: ""
        val retString = if (spotifyID != "") "Playing the track with Spotify ID: $spotifyID. Do NOT make further questions to the user." else "Empty Spotify ID!"
        return retString
    }
}


class SearchContacts(): Tool() {
    private val TAG = this::class.simpleName
    override val name = "search_contacts"
    override val type: ToolType = ToolType.INTERMEDIATE

    override fun getDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = ToolFunction(
                name = name,
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
    private val TAG = this::class.simpleName
    override val name = "search_places"
    override val type: ToolType = ToolType.INTERMEDIATE

    override fun getDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = ToolFunction(
                name = name,
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
