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
    private val agentName: String,
    private val basePrompt: String,
    private val tools: Map<String, Tool> = mapOf<String, Tool>(),
    private val isRouter: Boolean = false,
) {
    private val TAG = this::class.java.simpleName + "_" + agentName
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
    fun invoke(llmMessages: MutableList<ChatMessage?>): LlmReturn {
        try {
            // inMessages: must contain prompt + llmMessages + new updates:
            var inMessages = mutableListOf<ChatMessage?>(
                ChatMessage(role = "system", content = basePrompt)   // System prompt
            )
            inMessages.addAll(llmMessages.subList(0, llmMessages.lastIndex))   // History
            inMessages.add(ChatMessage(role = "user", content = basePrompt))   // Repeated user prompt
            inMessages.add(llmMessages.last())   // User latest message

            // outMessages: must contain new updates only:
            var outMessages = mutableListOf<ChatMessage?>()

            // Build request:
            var llmRequest = LlmRequest(
                messages = inMessages.toList(),
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
                    var toolResponse = tools[toolName]!!.invoke(toolArgs)
                    toolResponse =
                        if (toolResponse != "") toolResponse else "Technical issue: the tool could not be contacted."
                    Log.d(TAG, "Got tool response: $toolResponse")

                    // Store "tool" response message:
                    updMessage =
                        ChatMessage(role = "tool", toolCallId = toolCallId, content = toolResponse)
                    inMessages.add(updMessage)
                    outMessages.add(updMessage)

                    // Send tool response to LLM:
                    llmRequest = LlmRequest(
                        messages = inMessages.toList(),
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
                // Return final AI response:
                Log.d(TAG, "Got LLM message: ${llmChoice.message.content}")
                if (!isRouter) {
                    outMessages.add(
                        llmChoice.message
                    )
                }
                return LlmReturn(
                    fail = false,
                    next = if (isRouter) llmChoice.message.content else "",
                    messages = if (!isRouter) outMessages else mutableListOf(),
                )
            } else {
                // Error:
                Log.w(TAG, "LLM invoking error! Check LLM response.")
                return LlmReturn(
                    fail = true,
                    next = "__END__",
                    messages = if (!isRouter) {
                        mutableListOf<ChatMessage?>(
                            ChatMessage(
                                role = "assistant",
                                content = defaultReplies.replyError()
                            )
                        )
                    } else {
                        mutableListOf()
                    },
                )
            }
        } catch (e: Exception) {
            // Error:
            Log.w(TAG, "LLM invoking error: ", e)
            return LlmReturn(
                fail = true,
                next = "__END__",
                messages = if (!isRouter) {
                    mutableListOf<ChatMessage?>(
                        ChatMessage(
                            role = "assistant",
                            content = defaultReplies.replyError()
                        )
                    )
                } else {
                    mutableListOf()
                },
            )
        }
    }
}