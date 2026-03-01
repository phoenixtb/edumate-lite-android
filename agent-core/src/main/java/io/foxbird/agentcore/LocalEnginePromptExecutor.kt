package io.foxbird.agentcore

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.streaming.StreamFrame
import io.foxbird.edgeai.engine.EngineOrchestrator
import io.foxbird.edgeai.engine.GenerationParams
import io.foxbird.edgeai.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Koog [PromptExecutor] backed by the on-device [EngineOrchestrator].
 *
 * Formats Koog [Prompt] messages into a plain-text chat string, calls the local LLM, then
 * detects whether the response is a tool call (JSON) or a final text answer.
 *
 * Tool call format expected from the model (OpenAI-compatible, used by LFM2-1.2B-Tool):
 *   {"name": "tool_name", "arguments": {"param": "value"}}
 */
class LocalEnginePromptExecutor(
    private val orchestrator: EngineOrchestrator,
    private val maxTokens: Int = 512,
    private val temperature: Float = 0.6f
) : PromptExecutor {

    companion object {
        private const val TAG = "LocalEnginePromptExecutor"
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    }

    override suspend fun execute(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>
    ): List<Message.Response> {
        val textPrompt = buildPromptString(prompt, tools)
        Logger.d(TAG, "Executing prompt (${textPrompt.length} chars, ${tools.size} tools)")

        val result = orchestrator.generateComplete(
            prompt = textPrompt,
            params = GenerationParams(maxTokens = maxTokens, temperature = temperature, topP = 0.9f, topK = 40)
        )

        return result.fold(
            ifLeft = { err ->
                Logger.e(TAG, "Engine generation failed: ${err.message}")
                listOf(Message.Assistant("I'm unable to complete that right now.", ResponseMetaInfo.Empty))
            },
            ifRight = { response -> parseResponse(response.trim(), tools) }
        )
    }

    override fun executeStreaming(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>
    ): Flow<StreamFrame> = emptyFlow() // Agent loop uses execute(), not streaming

    override suspend fun moderate(prompt: Prompt, model: LLModel): ModerationResult =
        ModerationResult(isHarmful = false, categories = emptyMap())

    override fun close() { /* no external resources to release */ }

    // -------------------------------------------------------------------------
    // Prompt construction
    // -------------------------------------------------------------------------

    private fun buildPromptString(prompt: Prompt, tools: List<ToolDescriptor>): String = buildString {
        // System block (merged with tool descriptions)
        val systemMessages = prompt.messages.filterIsInstance<Message.System>()
        if (systemMessages.isNotEmpty() || tools.isNotEmpty()) {
            append("[SYSTEM]\n")
            systemMessages.forEach { append(it.content).append("\n\n") }
            if (tools.isNotEmpty()) {
                append("You have access to the following tools. When you need a tool, output ONLY the JSON call on a single line:\n")
                append("{\"name\": \"<tool>\", \"arguments\": {<params>}}\n\n")
                append("Available tools:\n")
                tools.forEach { tool ->
                    append("• ${tool.name}: ${tool.description}\n")
                    if (tool.requiredParameters.isNotEmpty()) {
                        append("  Required: ${tool.requiredParameters.joinToString { "${it.name} (${it.type.name})" }}\n")
                    }
                    if (tool.optionalParameters.isNotEmpty()) {
                        append("  Optional: ${tool.optionalParameters.joinToString { "${it.name} (${it.type.name})" }}\n")
                    }
                }
                append("\nWhen you have the final answer, write it as plain text without any JSON.\n")
            }
            append("\n")
        }

        // Conversation messages
        prompt.messages.forEach { msg ->
            when (msg) {
                is Message.System -> Unit // already handled above
                is Message.User -> append("[USER]\n${msg.content}\n\n")
                is Message.Assistant -> append("[ASSISTANT]\n${msg.content}\n\n")
                is Message.Tool.Call -> append("[TOOL CALL] ${msg.tool}: ${msg.content}\n\n")
                is Message.Tool.Result -> append("[TOOL RESULT] (${msg.tool}): ${msg.content}\n\n")
                else -> Unit
            }
        }

        append("[ASSISTANT]\n")
    }

    // -------------------------------------------------------------------------
    // Response parsing
    // -------------------------------------------------------------------------

    private fun parseResponse(response: String, tools: List<ToolDescriptor>): List<Message.Response> {
        if (tools.isEmpty()) {
            return listOf(Message.Assistant(response, ResponseMetaInfo.Empty))
        }

        val toolCall = tryParseToolCall(response)
        if (toolCall != null && tools.any { it.name == toolCall.first }) {
            Logger.d(TAG, "Tool call detected: ${toolCall.first}")
            return listOf(
                Message.Tool.Call(
                    id = "call_${System.currentTimeMillis()}",
                    tool = toolCall.first,
                    content = toolCall.second,
                    metaInfo = ResponseMetaInfo.Empty
                )
            )
        }

        return listOf(Message.Assistant(response, ResponseMetaInfo.Empty))
    }

    /**
     * Attempts to extract a `{"name": "...", "arguments": {...}}` JSON from the response.
     * Returns `Pair(toolName, argumentsJson)` or null if not a tool call.
     */
    private fun tryParseToolCall(text: String): Pair<String, String>? {
        val jsonStart = text.indexOf('{')
        if (jsonStart < 0) return null

        return try {
            val candidate = text.substring(jsonStart).let { raw ->
                // Find the matching closing brace
                var depth = 0
                var endIdx = -1
                for (i in raw.indices) {
                    when (raw[i]) {
                        '{' -> depth++
                        '}' -> {
                            depth--
                            if (depth == 0) { endIdx = i; break }
                        }
                    }
                }
                if (endIdx < 0) raw else raw.substring(0, endIdx + 1)
            }

            val obj: JsonObject = json.parseToJsonElement(candidate).jsonObject
            val name = obj["name"]?.jsonPrimitive?.content
                ?: obj["tool"]?.jsonPrimitive?.content
                ?: return null
            val arguments = obj["arguments"] ?: obj["params"] ?: obj["input"]
            Pair(name, arguments?.toString() ?: "{}")
        } catch (_: Exception) {
            null
        }
    }
}
