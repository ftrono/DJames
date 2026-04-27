package com.ftrono.DJames.be.agents.tools

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.maxSearchMatches
import com.ftrono.DJames.application.midThreshold
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.agents.chat.ActionsExecutor
import com.ftrono.DJames.kaigraph.data.ToolDefinition
import com.ftrono.DJames.kaigraph.data.ToolFunction
import com.ftrono.DJames.kaigraph.data.ToolParameters
import com.ftrono.DJames.kaigraph.data.ToolProperty
import com.ftrono.DJames.kaigraph.data.ToolResponse
import com.ftrono.DJames.kaigraph.data.ToolType
import com.ftrono.DJames.be.database.ActionType
import com.ftrono.DJames.be.database.Attachments
import com.ftrono.DJames.be.database.LibraryItem
import com.ftrono.DJames.be.database.UseRequest
import com.ftrono.DJames.kaigraph.tool.Tool
import kotlinx.serialization.json.*


class ToolRetrievePlaces(): Tool() {
    private val TAG = this::class.java.simpleName
    override val name = "tool_retrieve"
    override val type: ToolType = ToolType.INTERMEDIATE
    private val filter = "place"

    override fun getDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = ToolFunction(
                name = name,
                description = """
                    Get the identifier of the requested address from Google Maps, if you don't have it already. You must pass here the **place name**, the **full address** or **both of them together**, depending on what info you collect from the user conversation. 
                    **Always use this tool to retrieve the identifier**  of the Google Maps place before navigating there!""".trimIndent(),
                parameters = ToolParameters(
                    type = "object",
                    properties = mapOf(
                        "place_or_address" to ToolProperty(
                            type = "string",   // Arg type
                            description = "Where the user wants to go. Write here the place name, the full address, or both"   // Arg description
                        ),
                    ),
                    required = mutableListOf("place_or_address")
                )
            )
        )
    }

    // Get Library matches:
    fun loadCandidates(query: String, maxMatches: Int = maxSearchMatches): MutableList<LibraryItem> {
        val matchedItems = mutableListOf<LibraryItem>()

        // Get library matches:
        val libMatches = libUtils.matchLibrary(
            filter = filter,
            text = query.lowercase(),
            threshold = midThreshold
        )

        // Add sorted library matched items up to maxMatches:
        for (match in libMatches) {
            if (matchedItems.size < maxMatches) {
                val item = libUtils.getLibItemById(
                    match.matchId
                )
                item.matchScore = match.matchScore
                matchedItems.add(item)
            } else {
                break
            }
        }
        return matchedItems
    }

    override fun invoke(args: JsonObject, attachments: Attachments): ToolResponse {
        // INIT:
        var retString = ""

        attachments.useRequest = UseRequest(
            type = filter,
            name = (args["place_or_address"]?.jsonPrimitive?.content ?: "").lowercase().trim(),
        )

        // Fallback:
        if (attachments.useRequest!!.name == "") {
            return ToolResponse(
                message = "This tool was called with no input args: try again passing the correct input information.",
                attachments = attachments,
            )
        }

        attachments.useCandidates = loadCandidates(attachments.useRequest!!.name)
        Log.d(TAG, "MATCH NAMES: ${attachments.useCandidates!!.map { it.name }}")

        if (attachments.useCandidates!!.isEmpty()) {
            // Place NOT found in Library -> Build from message:
            Log.d(TAG, "DRIVE -> Place from Message")
            val placeInfo = LibraryItem(
                type = filter,
                name = utils.capitalizeWords(attachments.useRequest!!.name),
                detail = "",
            )
            placeInfo.url = libUtils.buildPlaceUrlFromItemInfo(placeInfo)
            placeInfo.uniId = "0x0" + utils.generateRandomString(5, numOnly = true)
            attachments.useCandidates!!.add(placeInfo)
            retString = """
                Place found! Google Maps ID: ${placeInfo.uniId}, name: ${placeInfo.name}.
                Call tool 'tool_go' with this Google Maps ID.
                """.trimMargin()


        } else if (attachments.useCandidates!!.size == 1) {
            //Place found in Library (one match):
            Log.d(TAG, "DRIVE -> Place from Library")
            val placeInfo = attachments.useCandidates!![0]
            placeInfo.detail = libUtils.buildPlaceReadableDetail(placeInfo)
            attachments.useCandidates!![0] = placeInfo
            retString = """
                Place found! Google Maps ID: ${placeInfo.uniId}, name: ${placeInfo.name}, address: ${placeInfo.detail}.
                Call tool 'tool_go' with this Google Maps ID.
                """.trimMargin()

        } else {
            //Places found in Library (multiple matches):
            var candidateStr = ""
            for (item in attachments.useCandidates!!) {
                // Use uniId instead of phone number for privacy:
                item.detail = libUtils.buildPlaceReadableDetail(item)
                candidateStr += "\n- Google Maps ID: ${item.uniId}, name: \"${item.name}\", address: \"${item.detail}\""
            }
            retString = """
                Places found (read all of them in spoken format, no bullet points or numbered items):
                [CANDIDATES]
                
                If you can clearly identify which one the user is requesting among them, just call tool 'tool_go' with that place's Google Maps ID.
                Otherwise, read to the user the most relevant places based on the query (in plain text, no markdown) and ask them which of these places they want to navigate to.
                """.trimIndent()
            retString = retString.replace("[CANDIDATES]", candidateStr)
        }

        // Return:
        return ToolResponse(
            message = retString,
            attachments = attachments,
        )
    }
}

class ToolGo(
    private val context: Context
): Tool() {
    private val TAG = this::class.java.simpleName
    override val name = "tool_go"
    override val type: ToolType = ToolType.ACTION
    private val actionsExecutor = ActionsExecutor(context)

    override fun getDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = ToolFunction(
                name = name,
                description = """
                     Navigate to the requested place. **Use this tool only after** you got the actual Google Maps ID from 'tool_retrieve'.""".trimIndent(),
                parameters = ToolParameters(
                    type = "object",
                    properties = mapOf(
                        "google_maps_id" to ToolProperty(
                            type = "string",   // Arg type
                            description = "(Mandatory) Google Maps ID of the requested place/address to navigate to."   // Arg description
                        ),
                    ),
                    required = mutableListOf("google_maps_id")
                )
            )
        )
    }

    override fun invoke(args: JsonObject, attachments: Attachments): ToolResponse {
        val gMapsId: String = (args["google_maps_id"]?.jsonPrimitive?.content ?: "")

        if (gMapsId == "" || attachments.useCandidates == null) {
            Log.w(TAG, "ERROR: ToolGo invoked with either missing gMapsId ($gMapsId) or useCandidates!")
            return ToolResponse(
                message = "Tell the user there was a problem. Then, END this conversation.",
                attachments = attachments,
            )

        } else {
            // Retrieve from attachments:
            val candidates = attachments.useCandidates!!.filter { it.uniId == gMapsId }
            if (candidates.isEmpty()) {
                Log.w(TAG, "ERROR: No useCandidate with gMapsId $gMapsId!")
                return ToolResponse(
                    message = "Tell the user there was a problem. Then, END this conversation.",
                    attachments = attachments,
                )
            }

            // Navigate:
            val placeInfo = candidates[0]
            attachments.usable = placeInfo
            attachments.playAcknowledge = true
            attachments.actionType = ActionType.OPEN_URL
            actionsExecutor.openLink(attachments.usable)
            val detailString = if (placeInfo.detail != "") "${placeInfo.name}, ${placeInfo.detail}" else placeInfo.name

            return ToolResponse(
                message = "Showing the route towards: $detailString (read the destination to the user AS IT IS). Do NOT invent any information (i.e. no ETA) and do NOT ask further questions to the user.",
                attachments = attachments
            )
        }
    }
}