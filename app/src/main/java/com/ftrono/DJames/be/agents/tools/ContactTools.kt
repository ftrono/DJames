package com.ftrono.DJames.be.agents.tools

import android.Manifest
import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.dictatedNumber
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.maxSearchMatches
import com.ftrono.DJames.application.midThreshold
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.agents.chat.ActionsExecutor
import com.ftrono.DJames.kaigraph.ToolDefinition
import com.ftrono.DJames.kaigraph.ToolFunction
import com.ftrono.DJames.kaigraph.ToolParameters
import com.ftrono.DJames.kaigraph.ToolProperty
import com.ftrono.DJames.kaigraph.ToolResponse
import com.ftrono.DJames.kaigraph.ToolType
import com.ftrono.DJames.be.database.ActionType
import com.ftrono.DJames.be.database.Attachments
import com.ftrono.DJames.be.database.LibraryItem
import com.ftrono.DJames.be.database.PhoneSet
import com.ftrono.DJames.be.database.UseRequest
import com.ftrono.DJames.kaigraph.Tool
import kotlinx.serialization.json.*


class ToolRetrieveContacts(
    val word: String,
    val verb: String,
    val finalTool: String
): Tool() {
    private val TAG = this::class.java.simpleName
    override val name = "tool_retrieve"
    override val type: ToolType = ToolType.INTERMEDIATE
    private val filter = "contact"

    override fun getDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = ToolFunction(
                name = name,
                description = """
                    Get the phone number of the requested contact to $word, if you don't have it already. You must pass here the **contact name** you collect from the user conversation. 
                    **Always use this tool to retrieve the phone number** for a contact before $verb them!""".trimIndent(),
                parameters = ToolParameters(
                    type = "object",
                    properties = mapOf(
                        "contact_name" to ToolProperty(
                            type = "string",   // Arg type
                            description = "The name of the requested contact to $word"   // Arg description
                        ),
                    ),
                    required = mutableListOf("contact_name")
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
            name = (args["contact_name"]?.jsonPrimitive?.content ?: "").lowercase().trim(),
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

        // Fallback:
        if (attachments.useCandidates!!.isEmpty()) {
            retString = "Reply that you could not find any saved contact with that name. Ask the user if he/she wants to search for someone else or to specify better who to search for."

        } else if (attachments.useCandidates!!.size == 1) {
            // Success -> one match:
            val match = attachments.useCandidates!![0]
            attachments.playAcknowledge = true
            retString = """
                Contact found! Name: ${match.name}, phone number: ${match.uniId}.
                Call tool '$finalTool' with this phone number.
                """.trimMargin()

        } else {
            // Success -> multiple matches:
            var candidateStr = ""
            for (item in attachments.useCandidates!!) {
                // Use uniId instead of phone number for privacy:
                candidateStr += "\n- phone number: ${item.uniId}, name: ${item.name}"
            }
            retString = """
                Contacts found:
                (don't read the phone numbers to the user):
                [CANDIDATES]
               
                If you can clearly identify who the user is requesting among them, just call tool '$finalTool' with that contact's phone number.
                Otherwise, read to the user the most relevant contacts based on the query (in plain text, no markdown) and ask them which of these contacts they want to $word.
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


class ToolCall(): Tool() {
    private val TAG = this::class.java.simpleName
    override val name = "tool_call"
    override val type: ToolType = ToolType.ACTION

    override fun getDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = ToolFunction(
                name = name,
                description = """
                     Call the requested phone number. **Use this tool only after** you got the actual phone number from the user or from 'tool_retrieve'.""".trimIndent(),
                parameters = ToolParameters(
                    type = "object",
                    properties = mapOf(
                        "phone_number" to ToolProperty(
                            type = "string",   // Arg type
                            description = "(Mandatory) Phone number of the requested contact to call."   // Arg description
                        ),
                    ),
                    required = mutableListOf("phone_number")
                )
            )
        )
    }

    override fun invoke(args: JsonObject, attachments: Attachments): ToolResponse {
        val inputNumber: String = (args["phone_number"]?.jsonPrimitive?.content ?: "")

        if (inputNumber == "" || attachments.useCandidates == null) {
            Log.w(TAG, "ERROR: ToolGo invoked with either missing inputNumber ($inputNumber) or useCandidates!")
            return ToolResponse(
                message = "Tell the user there was a problem. Then, END this conversation.",
                attachments = attachments,
            )

        } else {
            // Retrieve from attachments:
            val sendMatches = attachments.useCandidates!!.filter { it.uniId == inputNumber }

            if (sendMatches.isEmpty()) {
                // Phone number dictated by user -> call directly:
                attachments.playAcknowledge = true
                attachments.actionType = ActionType.CALL
                attachments.usable = LibraryItem(
                    name = dictatedNumber,
                    source = "contact",
                    type = "contact",
                    phoneSet = PhoneSet(
                        prefix = if (inputNumber.first() == '+') "" else "+39",   // TODO
                        phone = inputNumber
                    )
                )
                return ToolResponse(
                    message = "Calling the phone number ${inputNumber}. Read it to the user and do NOT ask further questions to them.",
                    attachments = attachments
                )

            } else {
                // Phone number from contact:
                val callMatch = sendMatches[0]
                attachments.usable = callMatch
                attachments.playAcknowledge = true
                attachments.actionType = ActionType.CALL
                return ToolResponse(
                    message = "Calling ${callMatch.name}. Always tell the user who you're calling and do NOT ask further questions to the user.",
                    attachments = attachments,
                )
            }
        }
    }
}


class ToolSendText(
    private val context: Context
): Tool() {
    private val TAG = this::class.java.simpleName
    override val name = "tool_send"
    override val type: ToolType = ToolType.ACTION
    private val actionsExecutor = ActionsExecutor(context)

    override fun getDefinition(): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = ToolFunction(
                name = name,
                description = """
                     Send the approved message to the requested phone number. **Use this tool only **AFTER you retrieved the phone number from 'tool_retrieve' or from the user itself, you composed the message draft and you got the user's approval to send it.""".trimIndent(),
                parameters = ToolParameters(
                    type = "object",
                    properties = mapOf(
                        "message_type" to ToolProperty(
                            type = "string",   // Arg type
                            description = "(Mandatory) Type of text message to send: can be exclusively one of 'SMS' or 'WHATSAPP'. Use 'SMS' as default, unless the user specifically asks for Whatsapp."   // Arg description
                        ),
                        "phone_number" to ToolProperty(
                            type = "string",   // Arg type
                            description = "(Mandatory) Phone number of the requested contact to send the message to."   // Arg description
                        ),
                        "message_text" to ToolProperty(
                            type = "string",   // Arg type
                            description = "(Mandatory) Text of the message to send, as drafted and approved by the user."   // Arg description
                        ),
                    ),
                    required = mutableListOf("message_type", "phone_number", "message_text")
                )
            )
        )
    }

    override fun invoke(args: JsonObject, attachments: Attachments): ToolResponse {
        val messageType: String = (args["message_type"]?.jsonPrimitive?.content ?: "SMS").uppercase()
        val inputNumber: String = (args["phone_number"]?.jsonPrimitive?.content ?: "")
        val messageText: String = (args["message_text"]?.jsonPrimitive?.content ?: "")

        if (inputNumber == "" || messageText == "" || attachments.useCandidates == null) {
            return ToolResponse(
                message = "This tool was called with no input args: try again passing the correct input information.",
                attachments = attachments,
            )

        } else {
            // Retrieve from attachments:
            val sendMatches = attachments.useCandidates!!.filter { it.uniId == inputNumber }

            if (sendMatches.isEmpty()) {
                Log.w(TAG, "ERROR: No contact with uniId $inputNumber!")
                return ToolResponse(
                    message = "Tell the user there was a problem. Then, END this conversation.",
                    attachments = attachments,
                )

            } else {
                // Phone number from contact:
                val sendMatch = sendMatches[0]
                attachments.usable = sendMatch
                attachments.playAcknowledge = true

                if (messageType == "WHATSAPP") {
                    // Send the WhatsApp text message:
                    val outcomeReply = actionsExecutor.sendWhatsappText(messageText, sendMatch)
                    attachments.actionType = ActionType.WA_TEXT
                    return ToolResponse(
                        message = "Outcome: $outcomeReply. Read this to the user EXACTLY AS IT IS and do NOT ask them further questions.",
                        attachments = attachments,
                    )
                } else if (!utils.checkPermission(context, Manifest.permission.SEND_SMS)) {
                    return ToolResponse(
                        message = "Tell the user that you're sorry but you must be given the Android permission to send SMS first - he can do it from within the DJames app when he's not driving. Ask the user if he wants to send the message via Whatsapp instead.",
                        attachments = attachments,
                    )
                } else {
                    // Send the SMS:
                    val outcomeReply = actionsExecutor.sendSMS(messageText, attachments.usable)
                    attachments.actionType = ActionType.SMS
                    return ToolResponse(
                        message = "Outcome: $outcomeReply. Tell the user this and do NOT ask them further questions.",
                        attachments = attachments,
                    )
                }
            }
        }
    }
}
