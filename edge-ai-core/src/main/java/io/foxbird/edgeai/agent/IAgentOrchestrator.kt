package io.foxbird.edgeai.agent

/**
 * Future contract for multi-step agentic workflows.
 *
 * Planned implementation: wrap [io.foxbird.edgeai.engine.EngineOrchestrator] as a
 * KoogLiteRtLlm provider using JetBrains Koog (github.com/JetBrains/koog, Apache 2.0).
 * Koog v0.6.2 supports Kotlin Multiplatform including Android and provides tool-use,
 * agent state persistence, retry, and observability out of the box.
 */
interface IAgentOrchestrator {
    suspend fun runAgent(goal: String, tools: List<AgentTool>): AgentResult
}

data class AgentTool(
    val name: String,
    val description: String,
    val execute: suspend (Map<String, Any>) -> Any
)

sealed class AgentResult {
    data class Success(val output: String, val toolsUsed: List<String>) : AgentResult()
    data class Failure(val reason: String) : AgentResult()
}
