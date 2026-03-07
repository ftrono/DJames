package com.ftrono.DJames.be.agents.tools

import android.util.Log
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.maxSearchMatches
import com.ftrono.DJames.application.midThreshold
import com.ftrono.DJames.be.agents.data.ActionType
import com.ftrono.DJames.be.agents.data.ToolDefinition
import com.ftrono.DJames.be.agents.data.ToolFunction
import com.ftrono.DJames.be.agents.data.ToolParameters
import com.ftrono.DJames.be.agents.data.ToolProperty
import com.ftrono.DJames.be.agents.data.ToolResponse
import com.ftrono.DJames.be.agents.data.ToolType
import com.ftrono.DJames.be.database.Attachments
import com.ftrono.DJames.be.database.LibraryItem
import com.ftrono.DJames.be.database.PhoneSet
import com.ftrono.DJames.be.database.UseRequest
import kotlinx.serialization.json.*


class ToolRetrieveMsgContacts(): Tool() {
    private val TAG = this::class.java.simpleName
    override val name = "tool_retrieve"
    override val type: ToolType = ToolType.INTERMEDIATE

    override fun getDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = ToolFunction(
                name = name,
                description = """
                    Get the phone number of the requested contact to call, if you don't have it already. You must pass here the **contact name** you collect from the user conversation. 
                    **Always use this tool to retrieve the phone number** for a contact before calling them!""".trimIndent(),
                parameters = ToolParameters(
                    type = "object",
                    properties = mapOf(
                        "contact_name" to ToolProperty(
                            type = "string",   // Arg type
                            description = "The name of the requested contact to call"   // Arg description
                        ),
                    ),
                    required = mutableListOf("contact_name")
                )
            )
        )
    }

    // Prepare original query + add Library matches (avoiding duplicates):
    fun loadCandidates(query: String, maxMatches: Int = maxSearchMatches): MutableList<LibraryItem> {
        val matchedItems = mutableListOf<LibraryItem>()

        // Get library matches:
        val libMatches = libUtils.matchLibrary(
            filter = "contact",
            text = query.lowercase(),
            threshold = midThreshold
        )

        // Add sorted library matched items up to maxMatches:
        for (match in libMatches) {
            if (matchedItems.size < maxMatches) {
                matchedItems.add(
                    libUtils.getLibItemById(
                        match.matchId
                    )
                )
            } else {
                break
            }
        }
        return matchedItems
    }

    override fun invoke(args: JsonObject, attachments: Attachments): ToolResponse {
        // INIT:
        var retString = ""
        var updAttachments = attachments

        updAttachments.useRequest = UseRequest(
            type = "contact",   // TODO
            name = (args["contact_name"]?.jsonPrimitive?.content ?: "").lowercase().trim(),
        )

        updAttachments.useCandidates = loadCandidates(updAttachments.useRequest!!.name)
        Log.d(TAG, "MATCH NAMES: ${updAttachments.useCandidates!!.map { it.name }}")

        // Fallback:
        if (updAttachments.useCandidates!!.isEmpty()) {
            retString = "End the conversation by simply saying that you could not find any saved contact with that name."

        } else if (updAttachments.useCandidates!!.size == 1) {
            // Success -> one match:
            retString = """
                Contact found! Phone number: ${updAttachments.useCandidates!![0].uniId}.
                Call tool 'tool_call' with this phone number.
                """.trimMargin()
        } else {
            var candidateStr = ""
            for (item in updAttachments.useCandidates!!) {
                /// Use uniId instead of phone number for privacy:
                candidateStr += "\n- phone number: ${item.uniId}, name: ${item.name}"
            }

            if (candidateStr == "") {
                // Fallback:
                retString =
                    "End the conversation by simply saying that you could not find any contact with that name."

            } else {
                // Success -> multiple matches:
                retString = """
                        Contacts found:
                        (don't read the phone numbers to the user):
                        [CANDIDATES]
                       
                        If you can clearly identify what the user is requesting among them, just call tool 'tool_call' with that contact's phone number.
                        Otherwise, read to the user the most relevant contacts based on the query (in plain text, no markdown) and ask them which of these contacts they want to call.
                        """.trimIndent()
                retString = retString.replace("[CANDIDATES]", candidateStr)
            }
        }

        // Return:
        return ToolResponse(
            message = retString,
            attachments = attachments,
        )
    }
}


class ToolSend(): Tool() {
    private val TAG = this::class.java.simpleName
    override val name = "tool_send"
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

    override fun invoke(args: JsonObject, attachments: Attachments): ToolResponse {
        var spotifyID = args["spotify_id"]?: ""
        val retString = if (spotifyID != "") {
            "Playing the track with Spotify ID: $spotifyID. Do NOT make further questions to the user."
        } else {
            "Empty Spotify ID!"
        }
        return ToolResponse(
            message = retString,
            attachments = attachments,
        )
    }
}
