package com.ftrono.DJames.be.agents.llm

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.END
import com.ftrono.DJames.application.jsonNoPrint
import com.ftrono.DJames.application.jsonUnknown
import com.ftrono.DJames.application.mistralLlmModelSmall
import com.ftrono.DJames.application.mistralLlmTemperature
import com.ftrono.DJames.application.mistralLlmTimeout
import com.ftrono.DJames.application.mistralLlmUrl
import com.ftrono.DJames.application.mistralSttUrl
import com.ftrono.DJames.application.utils
import com.ftrono.DJames.be.agents.data.ActionType
import com.ftrono.DJames.be.agents.data.ChatMessage
import com.ftrono.DJames.be.agents.data.LlmRequest
import com.ftrono.DJames.be.agents.data.LlmResponse
import com.ftrono.DJames.be.agents.data.LlmReturn
import com.ftrono.DJames.be.agents.data.StateInfo
import com.ftrono.DJames.be.agents.data.SttResponse
import com.ftrono.DJames.be.agents.data.SttReturn
import com.ftrono.DJames.be.agents.data.ToolDefinition
import com.ftrono.DJames.be.agents.data.ToolType
import com.ftrono.DJames.be.agents.tools.Tool
import com.ftrono.DJames.be.database.Attachments
import com.ftrono.DJames.be.models.HttpResponse
import com.ftrono.DJames.be.utils.HttpClient
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File


class LlmAgent(
    private val context: Context,
    private val apiKey: String,
    private val model: String,
    private val agentName: String,
    private val tools: Map<String, Tool> = mapOf<String, Tool>(),
    private val isRouter: Boolean = false,
    private val onComplete: String = END,
    private val onFallback: String = END,
) {
    private val TAG = this::class.java.simpleName + "_" + agentName
    private var toolsDef = defineTools()

    // Define all tools:
    private fun defineTools(): List<ToolDefinition> {
        var toolsDefTemp = mutableListOf<ToolDefinition>()
        for (tool in tools.keys) {
            toolsDefTemp.add(tools[tool]!!.getDefinition())
        }
        return toolsDefTemp.toList()
    }

    // Convert custom LlmRequest class to RequestBody:
    private fun llmClassToRequestBody(llmRequest: LlmRequest): RequestBody {
        val jsonBody = jsonNoPrint.encodeToString(llmRequest)
        val requestBody = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())
        return requestBody
    }

    // Clean LLM message:
    private fun cleanLlmMessage(text: String): String {
        var re = Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+")
        return re.replace(text, " ").replace("**", "\n").replace("* ", "- ").trim()
    }

    // Send LLM API request:
    private fun sendLlmRequest(
        requestBody: RequestBody,
        isTranscribe: Boolean = false,
    ): HttpResponse {
        // BUILD REQUEST:
        val httpClient = HttpClient()
        val client = httpClient.getClient(mistralLlmTimeout)

        val request = if (isTranscribe) {
            // Audio Transcriptions request:
            Request.Builder()
                .url(mistralSttUrl)
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()
        } else {
            // Chat Completions request:
            Request.Builder()
                .url(mistralLlmUrl)
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()
        }

        var httpResponse = HttpResponse(
            code = -1,
            body = ""
        )
        runBlocking {
            var response = httpClient.makeRequest(client, request)
            if (response.code == 200) {
                Log.d(TAG, "sendLlmRequest(): query success. Status code: ${response.code}.")
            } else {
                Log.w(TAG, "sendLlmRequest(): cannot query. Status code: ${response.code}, details: ${response.body}")
            }
            httpResponse = HttpResponse(
                code = response.code,
                body = response.body,
            )
            return@runBlocking httpResponse
        }
        return httpResponse
    }

    // Main: Transcribe:
    fun transcribe(audioPath: String): SttReturn {
        try {
            val audioFile = File(audioPath)
            // Create multipart request body:
            val fileBody: RequestBody = audioFile.asRequestBody(
                contentType = "audio/flac".toMediaTypeOrNull()
            )
            val audioRequestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", audioFile.name, fileBody)
                .addFormDataPart("model", model)
                .addFormDataPart("language", "en")
                .build()

            var httpResponse = sendLlmRequest(
                requestBody = audioRequestBody,
                isTranscribe = true
            )

            if (httpResponse.code == 200) {
                var sttResponse = jsonUnknown.decodeFromString<SttResponse>(httpResponse.body)
                Log.d(TAG, "Got STT response: $sttResponse")

                val cleanedForSilence = utils.cleanString(
                    text = sttResponse.text,
                    capitalize = false,
                ).replace(" ", "")
                val isSilence = cleanedForSilence == ""

                return SttReturn(
                    language = sttResponse.language,
                    transcription = sttResponse.text,
                    isSilence = isSilence,
                    fail = isSilence,
                )
            } else {
                // Error:
                Log.w(TAG, "STT invoking error: code ${httpResponse.code}!")
                return SttReturn(
                    fail = true
                )
            }

        } catch (e: Exception) {
            // Error:
            Log.w(TAG, "STT invoking error: ", e)
            return SttReturn(
                fail = true
            )
        }
    }

    // Main: Invoke LLM:
    fun invoke(
        llmMessages: MutableList<ChatMessage>,
        attachments: Attachments,
    ): LlmReturn {
        try {
            // outMessages: must contain new updates only:
            var outMessages = mutableListOf<ChatMessage>()
            var inMessages = llmMessages   // editable
            var next = ""
            var updAttachments = attachments
            var actionType: ActionType? = null

            // Build request:
            var llmRequest = LlmRequest(
                messages = inMessages.toList(),
                model = model,
                temperature = if (isRouter) 0F else mistralLlmTemperature,
                toolChoice = if (tools.isNotEmpty()) "auto" else "none",
                parallelToolCalls = false,
                tools = if (toolsDef.isNotEmpty()) toolsDef else null,
            )

            // Invoke LLM:
            var httpResponse = sendLlmRequest(
                llmClassToRequestBody(llmRequest)
            )

            if (httpResponse.code != 200) {
                // Error:
                Log.w(TAG, "LLM invoking error: status code ${httpResponse.code}!")
                return getFallbackReply(updAttachments)

            } else {
                var llmResponse = jsonUnknown.decodeFromString<LlmResponse>(httpResponse.body)
                Log.d(TAG, "Got LLM response: $llmResponse")
                var llmChoice = llmResponse.choices.firstOrNull()

                while (llmChoice != null && llmChoice.finishReason == "tool_calls") {
                    // Parse LLM response:
                    var llmMessage = llmChoice.message

                    if (tools.isNotEmpty() && llmMessage.toolCalls != null && llmMessage.toolCalls!!.isNotEmpty()) {
                        // Get requested tool & args:
                        val toolCall = llmMessage.toolCalls!!.first()
                        val toolCallId = toolCall.id
                        val toolName = toolCall.function.name
                        val curTool = tools[toolName]!!

                        if (curTool.type == ToolType.HANDOFF) {
                            // HANDOFF CASE:
                            Log.d(TAG, "LLM: HANDOFF CALLED! Target node -> $llmResponse")
                            return LlmReturn(
                                fail = false,
                                next = onFallback,
                                messages = mutableListOf(),
                                attachments = updAttachments
                            )
                        } else if (curTool.type == ToolType.ACTION) {
                            // ACTION CASE:
                            Log.d(TAG, "LLM: ACTION tool executed!")
                            next = onComplete
                        }

                        // Other tool:
                        val toolArgs = jsonUnknown
                            .parseToJsonElement(toolCall.function.arguments)
                            .jsonObject
                        Log.d(TAG, "Requested tool call: $toolCall")
                        var updMessage: ChatMessage? = null

                        // Store "assistant" tool request message:
                        updMessage = ChatMessage(
                            role = "assistant",
                            content = "Calling tool $toolCall...",
                            toolCalls = listOf(toolCall)
                        )
                        inMessages.add(updMessage)
                        outMessages.add(updMessage)

                        //Invoke requested tool:
                        val toolResponse = tools[toolName]!!.invoke(toolArgs, updAttachments)
                        val toolResponseMsg = if (toolResponse.message != "") toolResponse.message else "Technical issue: the tool could not be contacted."
                        Log.d(TAG, "Got tool response: ${toolResponse.message}")
                        updAttachments = toolResponse.attachments
                        actionType = toolResponse.actionType

                        // Store "tool" response message:
                        updMessage =
                            ChatMessage(
                                role = "tool",
                                toolCallId = toolCallId,
                                content = toolResponseMsg
                            )
                        inMessages.add(updMessage)
                        outMessages.add(updMessage)

                        // Send tool response to LLM:
                        llmRequest = LlmRequest(
                            messages = inMessages.toList(),
                            model = if (actionType != null) mistralLlmModelSmall else model,
                            temperature = mistralLlmTemperature,
                            toolChoice = "auto",
                            parallelToolCalls = false,
                            tools = toolsDef,
                        )

                        // Get response & proceed:
                        httpResponse = sendLlmRequest(
                            llmClassToRequestBody(llmRequest)
                        )

                        if (httpResponse.code == 200) {
                            // Parse response:
                            llmResponse =
                                jsonUnknown.decodeFromString<LlmResponse>(httpResponse.body)
                            Log.d(TAG, "Got LLM response: $llmResponse")
                            llmChoice = llmResponse.choices.firstOrNull()

                        } else {
                            // Error:
                            Log.w(TAG, "LLM invoking error: status code ${httpResponse.code}!")
                            return getFallbackReply(updAttachments)
                        }

                    } else {
                        // No tool calls:
                        Log.d(TAG, "No tool calls requested: exiting tools loop.")
                        break
                    }

                }

                if (llmChoice != null && llmChoice.finishReason == "stop") {
                    // Return final AI response:
                    Log.d(TAG, "Got LLM message: ${llmChoice.message.content}")
                    if (!isRouter) {
                        outMessages.add(
                            ChatMessage(
                                role = llmChoice.message.role,
                                content = cleanLlmMessage(llmChoice.message.content)
                            )
                        )
                    }
                    return LlmReturn(
                        fail = false,
                        next = if (isRouter) llmChoice.message.content else next,
                        messages = if (!isRouter) outMessages else mutableListOf(),
                        attachments = updAttachments,
                        actionType = actionType,
                    )
                } else {
                    // Error:
                    Log.w(TAG, "LLM invoking error! Check LLM response.")
                    return getFallbackReply(updAttachments)
                }
            }
        } catch (e: Exception) {
            // Error:
            Log.w(TAG, "LLM invoking error: ", e)
            return getFallbackReply(attachments)
        }
    }

    // Get fallback reply:
    private fun getFallbackReply(attachments: Attachments): LlmReturn {
        return LlmReturn(
            fail = true,
            messages = mutableListOf(),
            attachments = attachments,
        )
    }

}