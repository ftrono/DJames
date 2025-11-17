package com.ftrono.DJames.be.agents.tools

import android.util.Log
import com.ftrono.DJames.be.agents.data.ToolDefinition
import com.ftrono.DJames.be.agents.data.ToolFunction
import com.ftrono.DJames.be.agents.data.ToolParameters
import com.ftrono.DJames.be.agents.data.ToolProperty
import com.ftrono.DJames.be.agents.data.ToolType
import kotlinx.serialization.json.*


class ToolRetrieveDriver(): Tool() {
    private val TAG = this::class.java.simpleName
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
            """
            Track found! Spotify ID: ${allTracks[artist]!![track]!!}.
            Call tool 'tool_play' with this ID.
            """.trimMargin()
        } else if (allTracks.containsKey(artist)) {
            """
            Songs that can be played by $artist:
            (don't read Spotify IDs to the user):
            ${allTracks[artist]!!}
           
            Ask the user which of these they want to play.
            """.trimIndent()
        } else {
            "No song found for this artist."
        }
        return retString
    }
}


class ToolGo(): Tool() {
    private val TAG = this::class.java.simpleName
    override val name = "tool_go"
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
        val retString = if (spotifyID != "") {
            "Playing the track with Spotify ID: $spotifyID. Do NOT make further questions to the user."
        } else {
            "Empty Spotify ID!"
        }
        return retString
    }
}
