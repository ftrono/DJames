package com.ftrono.DJames.be.agents

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*


// Tools:
@Serializable
data class ToolProperty(
    var type: String,
    var description: String,
)

@Serializable
data class ToolParameters(
    var type: String,
    var properties: Map<String, ToolProperty>,
)

@Serializable
data class ToolFunction(
    var name: String,
    var description: String,
    var parameters: ToolParameters,
)

@Serializable
data class ToolDefinition(
    var type: String,
    var function: ToolFunction,
)


// Tool calls:
@Serializable
data class FunctionCall(
    var name: String = "",
    var arguments: String = "",
)

@Serializable
data class ToolCall(
    var id: String = "",
    var index: Int = 0,
    var function: FunctionCall = FunctionCall()
)


// Messages:
@Serializable
data class ChatMessage(
    var role: String = "",   // "system", "user", "tool"
    var content: String = "",
    @SerialName("tool_calls")
    var toolCalls: List<ToolCall>? = null,
    @SerialName("tool_call_id")
    var toolCallId: String? = null,
)


// LLM API interaction:
@Serializable
data class LlmRequest(
    var messages: List<ChatMessage> = listOf<ChatMessage>(),
    var model: String = "",
    var temperature: Float = 0.0F,
    @SerialName("tool_choice")
    var toolChoice: String = "auto",   // "auto", "none", ToolDefinition
    @SerialName("parallel_tool_calls")
    var parallelToolCalls: Boolean = false,
    var tools: List<ToolDefinition>? = null,
    // you can add other parameters (max_tokens, top_p, etc) if supported
)

@Serializable
data class LlmUsage(
    @SerialName("prompt_tokens")
    var promptTokens: Long = 0L,
    @SerialName("total_tokens")
    var totalTokens: Long = 0L,
    @SerialName("completion_tokens")
    var completionTokens: Long = 0L,
    @SerialName("prompt_audio_seconds")
    var promptAudioSeconds: Long = 0L,
)

@Serializable
data class LlmChoice(
    var index: Int,
    var message: ChatMessage,
    @SerialName("finish_reason")
    var finishReason: String = "",
)

@Serializable
data class LlmResponse(
    var usage: LlmUsage = LlmUsage(),
    var choices: List<LlmChoice> = listOf<LlmChoice>(),
)
