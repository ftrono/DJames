package com.ftrono.DJames.be.agents

import com.ftrono.DJames.be.models.JsonConverter
import com.ftrono.DJames.be.models.JsonListConverter
import io.objectbox.annotation.Convert
import kotlinx.serialization.SerialName
import java.io.Serializable
import kotlinx.serialization.Serializable as KxSerializable
import kotlinx.serialization.json.*


// Tools:
@KxSerializable
data class ToolProperty(
    var type: String,
    var description: String,
)

@KxSerializable
data class ToolParameters(
    var type: String,
    var properties: Map<String, ToolProperty>,
)

@KxSerializable
data class ToolFunction(
    var name: String,
    var description: String,
    var parameters: ToolParameters,
)

@KxSerializable
data class ToolDefinition(
    var type: String,
    var function: ToolFunction,
)


// STATE INTERNALS:
// Tool calls:
@KxSerializable
data class FunctionCall(
    var name: String = "",
    var arguments: String = "",
): Serializable

@KxSerializable
data class ToolCall(
    var id: String = "",
    var index: Int = 0,
    @Convert(converter = FunctionCallConverter::class, dbType = String::class)
    var function: FunctionCall = FunctionCall()
): Serializable


// Messages:
@KxSerializable
data class ChatMessage(
    var role: String = "",   // "system", "user", "tool"
    var content: String = "",
    @Convert(converter = ToolCallConverter::class, dbType = String::class)
    @SerialName("tool_calls")
    var toolCalls: List<ToolCall>? = null,
    @SerialName("tool_call_id")
    var toolCallId: String? = null,
): Serializable

class ChatMessageConverter : JsonConverter<ChatMessage>(ChatMessage.serializer())
class FunctionCallConverter : JsonConverter<FunctionCall>(FunctionCall.serializer())
class ToolCallConverter : JsonListConverter<ToolCall>(ToolCall.serializer())


// LLM API interaction:
@KxSerializable
data class LlmRequest(
    var messages: List<ChatMessage?> = listOf<ChatMessage?>(),
    var model: String = "",
    var temperature: Float = 0.0F,
    @SerialName("tool_choice")
    var toolChoice: String = "auto",   // "auto", "none", ToolDefinition
    @SerialName("parallel_tool_calls")
    var parallelToolCalls: Boolean = false,
    var tools: List<ToolDefinition>? = null,
    // you can add other parameters (max_tokens, top_p, etc) if supported
)

@KxSerializable
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

@KxSerializable
data class LlmChoice(
    var index: Int,
    var message: ChatMessage,
    @SerialName("finish_reason")
    var finishReason: String = "",
)

@KxSerializable
data class LlmResponse(
    var usage: LlmUsage = LlmUsage(),
    var choices: List<LlmChoice> = listOf<LlmChoice>(),
)

@KxSerializable
data class SttResponse(
    var usage: LlmUsage = LlmUsage(),
    var language: String = "",
    var model: String = "",
    var text: String = "",
)

// Status:
@KxSerializable
data class LlmReturn(
    var fail: Boolean = false,
    var next: String = "",
    var language: String = "",
    var messages: MutableList<ChatMessage?> = mutableListOf<ChatMessage?>(),
)

// Status:
@KxSerializable
data class SttReturn(
    var fail: Boolean = false,
    var isSilence: Boolean = false,
    var language: String = "",
    var transcription: String = "",
)
