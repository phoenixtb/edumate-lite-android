package io.foxbird.edgeai.agent

import kotlinx.coroutines.flow.SharedFlow

/**
 * Contract for multi-step agentic workflows.
 *
 * Implementation: EduMateAgentOrchestrator in :agent-core wraps EngineOrchestrator
 * as a Koog PromptExecutor using JetBrains Koog (github.com/JetBrains/koog, Apache 2.0).
 */
interface IAgentOrchestrator {
    /** Emits intermediate steps (tool calls, results) while [runAgent] is executing. */
    val agentSteps: SharedFlow<AgentStep>

    /** Returns true if the underlying inference engine has a model loaded and ready. */
    fun isReady(): Boolean

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

sealed class AgentStep {
    data class ToolCalling(val toolName: String, val args: String) : AgentStep()
    data class ToolResult(val toolName: String, val result: String) : AgentStep()
    data class Thinking(val text: String) : AgentStep()
}
