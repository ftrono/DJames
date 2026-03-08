package com.ftrono.DJames.be.agents.tools

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.dictatedNumber
import com.ftrono.DJames.application.libUtils
import com.ftrono.DJames.application.maxSearchMatches
import com.ftrono.DJames.application.midThreshold
import com.ftrono.DJames.be.agents.chat.ActionsExecutor
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

        attachments.useCandidates = loadCandidates(attachments.useRequest!!.name)
        Log.d(TAG, "MATCH NAMES: ${attachments.useCandidates!!.map { it.name }}")

        // Fallback:
        if (attachments.useCandidates!!.isEmpty()) {
            attachments.playFail = true
            retString = "End the conversation by simply saying that you could not find any saved contact with that name."

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
        val updAttachments = attachments

        if (inputNumber == "") {
            updAttachments.playFail = true
            return ToolResponse(
                message = "Tell the user there was a problem. Then, END this conversation.",
                attachments = updAttachments,
            )

        } else {
            // Retrieve from attachments:
            val sendMatches = if (updAttachments.useCandidates == null) {
                listOf()
            } else {
                updAttachments.useCandidates!!.filter { it.uniId == inputNumber }
            }

            if (sendMatches.isEmpty()) {
                // Phone number dictated by user -> call directly:
                updAttachments.playAcknowledge = true
                updAttachments.usable = LibraryItem(
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
                    attachments = updAttachments,
                    actionType = ActionType.CALL,
                )

            } else {
                // Phone number from contact:
                val callMatch = sendMatches[0]
                updAttachments.usable = callMatch
                updAttachments.playAcknowledge = true
                return ToolResponse(
                    message = "Calling ${callMatch.name}. Always tell the user who you're calling and do NOT ask further questions to the user.",
                    attachments = updAttachments,
                    actionType = ActionType.CALL,
                )
            }
        }
    }
}


class ToolSendSMS(
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
                     Send the approved SMS to the requested phone number. **Use this tool only **AFTER you retrieved the phone number from 'tool_retrieve' or from the user itself, you composed the message draft and you got the user's approval to send it.""".trimIndent(),
                parameters = ToolParameters(
                    type = "object",
                    properties = mapOf(
                        "phone_number" to ToolProperty(
                            type = "string",   // Arg type
                            description = "(Mandatory) Phone number of the requested contact to send the SMS to."   // Arg description
                        ),
                        "message_text" to ToolProperty(
                            type = "string",   // Arg type
                            description = "(Mandatory) Text of the message to send, as drafted and approved by the user."   // Arg description
                        ),
                    ),
                    required = mutableListOf("phone_number", "message_text")
                )
            )
        )
    }

    override fun invoke(args: JsonObject, attachments: Attachments): ToolResponse {
        val inputNumber: String = (args["phone_number"]?.jsonPrimitive?.content ?: "")
        val messageText: String = (args["message_text"]?.jsonPrimitive?.content ?: "")

        if (inputNumber == "" || messageText == "") {
            attachments.playFail = true
            return ToolResponse(
                message = "Tell the user there was a problem. Then, END this conversation.",
                attachments = attachments,
            )

        } else {
            // Retrieve from attachments:
            val sendMatches = if (attachments.useCandidates == null) {
                listOf()
            } else {
                attachments.useCandidates!!.filter { it.uniId == inputNumber }
            }

            if (sendMatches.isEmpty()) {
                // Phone number dictated by user -> call directly:
                attachments.playAcknowledge = true
                attachments.usable = LibraryItem(
                    name = dictatedNumber,
                    source = "contact",
                    type = "contact",
                    phoneSet = PhoneSet(
                        prefix = if (inputNumber.first() == '+') "" else "+39",   // TODO
                        phone = inputNumber
                    )
                )

            } else {
                // Phone number from contact:
                val sendMatch = sendMatches[0]
                attachments.usable = sendMatch
                attachments.playAcknowledge = true
            }

            // Send the SMS:
            val outcomeReply = actionsExecutor.sendSMS(messageText, attachments.usable)
            return ToolResponse(
                message = "Outcome: $outcomeReply. Tell the user this and do NOT ask them further questions.",
                attachments = attachments,
                actionType = ActionType.SMS,
            )
        }
    }
}


class ToolSendWAText(
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
                     Send the approved text message draft. **Use this tool only **AFTER you composed the message draft and you got the user's approval to send it.""".trimIndent(),
                parameters = ToolParameters(
                    type = "object",
                    properties = mapOf(
                        "message_text" to ToolProperty(
                            type = "string",   // Arg type
                            description = "(Mandatory) Text of the message to send, as drafted and approved by the user."   // Arg description
                        ),
                    ),
                    required = mutableListOf("message_text")
                )
            )
        )
    }

    override fun invoke(args: JsonObject, attachments: Attachments): ToolResponse {
        val messageText: String = (args["message_text"]?.jsonPrimitive?.content ?: "")

        if (messageText == "") {
            attachments.playFail = true
            return ToolResponse(
                message = "Tell the user there was a problem. Then, END this conversation.",
                attachments = attachments,
            )

        } else {
            // Send the WhatsApp message:
            val outcomeReply = actionsExecutor.sendWhatsappText(messageText)
            attachments.playAcknowledge = true
            return ToolResponse(
                message = "Outcome: $outcomeReply. Read this all to the user and do NOT ask them further questions.",
                attachments = attachments,
                actionType = ActionType.WA_TEXT,
            )
        }
    }
}
