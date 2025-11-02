package com.ftrono.DJames.be.agents

import android.content.Context
import android.util.Log
import com.ftrono.DJames.application.defaultReplies
import com.ftrono.DJames.application.mistralLlmModel
import com.ftrono.DJames.application.mistralLlmTemperature
import com.ftrono.DJames.application.mistralLlmTimeout
import com.ftrono.DJames.application.mistralLlmUrl
import com.ftrono.DJames.be.utils.HttpClient
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody


class LlmAgent(
    private val context: Context,
    private val apiKey: String,
    private val basePrompt: String,
    private val tools: Map<String, Tool> = mapOf<String, Tool>(),
) {
    private val TAG = LlmAgent::class.java.simpleName
    private val json = Json {
        ignoreUnknownKeys = true
    }
    private var toolsDef = defineTools()


    // Define all tools:
    private fun defineTools(): List<ToolDefinition> {
        var toolsDefTemp = mutableListOf<ToolDefinition>()
        for (tool in tools.keys) {
            toolsDefTemp.add(tools[tool]!!.getDefinition())
        }
        return toolsDefTemp.toList()
    }


    // Send LLM API request:
    private fun sendLlmRequest(
        requestBody: LlmRequest,
    ): LlmResponse {
        // BUILD REQUEST:
        val httpClient = HttpClient()
        val client = httpClient.getClient(mistralLlmTimeout)

        val jsonBody = Json { prettyPrint = false }.encodeToString(requestBody)
        val request = Request.Builder()
            .url(mistralLlmUrl)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(jsonBody.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        var llmResponse = LlmResponse()

        runBlocking {
            var response = httpClient.makeRequest(client, request)
            if (response.code == 200) {
                llmResponse = json.decodeFromString<LlmResponse>(response.body)
                return@runBlocking llmResponse
            } else {
                Log.w(TAG, "sendLlmRequest(): cannot query. Status code: ${response.code}, details: ${response.body}")
                return@runBlocking llmResponse
            }
        }

        return llmResponse
    }

    // Main: Invoke LLM:
    fun invoke(inMessage: String): String {
        // Build initial messages:
        var messages = mutableListOf(
            ChatMessage(role = "system", content = basePrompt),
            ChatMessage(role = "user", content = inMessage)
        )

        // Build request:
        var llmRequest = LlmRequest(
            messages = messages.toList(),
            model = mistralLlmModel,
            temperature = mistralLlmTemperature,
            toolChoice = if (tools.isNotEmpty()) "auto" else "none",
            parallelToolCalls = false,
            tools = if (toolsDef.isNotEmpty()) toolsDef else null,
        )

        // Invoke LLM:
        var llmResponse = sendLlmRequest(llmRequest)
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
                val toolArgs = json.parseToJsonElement(toolCall.function.arguments).jsonObject
                Log.d(TAG, "Requested tool call: $toolCall")
                messages.add(
                    ChatMessage(role="assistant", content = "Calling tool $toolCall...", toolCalls = listOf(toolCall))
                )

                //Invoke requested tool:
                val toolResponse = tools[toolName]!!.invoke(toolArgs)

                if (toolResponse != "") {
                    Log.d(TAG, "Got tool response: $toolResponse")
                    // Send tool response to LLM:
                    messages.add(
                        ChatMessage(role="tool", toolCallId = toolCallId, content = toolResponse)
                    )
                } else {
                    messages.add(
                        ChatMessage(role="tool", toolCallId = toolCallId, content = "Technical issue: the tool could not be contacted.")
                    )
                }

                llmRequest = LlmRequest(
                    messages = messages.toList(),
                    model = mistralLlmModel,
                    temperature = mistralLlmTemperature,
                    toolChoice = "auto",
                    parallelToolCalls = false,
                    tools = toolsDef,
                )

                // Get response & proceed:
                llmResponse = sendLlmRequest(llmRequest)
                Log.d(TAG, "Got LLM response: $llmResponse")
                llmChoice = llmResponse.choices.firstOrNull()

            } else {
                // No tool calls:
                Log.d(TAG, "No tool calls requested: exiting tools loop.")
                break
            }

        }

        if (llmChoice != null && llmChoice.finishReason == "stop") {
            Log.d(TAG, "Got LLM message: ${llmChoice.message.content}")
            return llmChoice.message.content
        } else {
            Log.w(TAG, "LLM invoking error! Finish reason is: ${llmChoice!!.finishReason}")
            return defaultReplies.replyError()
        }

    }

}