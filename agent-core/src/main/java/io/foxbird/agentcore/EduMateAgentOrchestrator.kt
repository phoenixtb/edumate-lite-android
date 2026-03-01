package io.foxbird.agentcore

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import io.foxbird.agentcore.tools.MaterialSummaryTool
import io.foxbird.agentcore.tools.RagSearchTool
import io.foxbird.agentcore.tools.WorksheetTool
import io.foxbird.doclibrary.domain.rag.IRagEngine
import io.foxbird.edgeai.agent.AgentResult
import io.foxbird.edgeai.agent.AgentStep
import io.foxbird.edgeai.agent.AgentTool
import io.foxbird.edgeai.agent.IAgentOrchestrator
import io.foxbird.edgeai.engine.EngineOrchestrator
import io.foxbird.edgeai.engine.MemoryMonitor
import io.foxbird.edgeai.engine.MemoryPressure
import io.foxbird.edgeai.util.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * On-device agentic orchestrator backed by JetBrains Koog + local [EngineOrchestrator].
 *
 * Koog's [AIAgent] runs a ReAct-style loop:
 *   1. Calls [LocalEnginePromptExecutor.execute] → LLM decides: tool call OR final answer.
 *   2. If tool call → Koog executes the registered [ToolRegistry] tool and feeds result back.
 *   3. Repeats until the LLM outputs a text response (or [maxIterations] is exhausted).
 *
 * Memory gating via [MemoryMonitor]: maxIterations is capped dynamically based on RAM pressure,
 * preventing OOM on low-RAM variants (Nothing 3A 8 GB).
 */
class EduMateAgentOrchestrator(
    private val orchestrator: EngineOrchestrator,
    private val ragEngine: IRagEngine,
    private val memoryMonitor: MemoryMonitor
) : IAgentOrchestrator {

    companion object {
        private const val TAG = "EduMateAgentOrchestrator"

        private val LOCAL_LLM_MODEL = LLModel(
            provider = LocalLLMProvider,
            id = "local-engine",
            capabilities = listOf(LLMCapability.Tools, LLMCapability.Temperature),
            contextLength = 8192L,
            maxOutputTokens = 512L
        )

        private val SYSTEM_PROMPT = """
            You are EduMate, an expert educational AI assistant for students in grades 5–10.
            Use the provided tools to search study materials, summarize content, and generate practice worksheets.
            Think step-by-step. Be educational and age-appropriate.

            CRITICAL OUTPUT RULES:
            - When a tool returns generated content (worksheet, quiz, summary), include it IN FULL in your final answer. Do NOT describe or paraphrase it.
            - Present worksheets, quizzes, and summaries exactly as returned by the tool, with only a brief intro sentence before the content.
            - Never say "I have generated a worksheet" — show the actual worksheet.
            - For rag_search results, summarize the key points clearly with bullet points.
        """.trimIndent()
    }

    private val _agentSteps = MutableSharedFlow<AgentStep>(extraBufferCapacity = 32)
    override val agentSteps: SharedFlow<AgentStep> = _agentSteps.asSharedFlow()

    private val toolRegistry = ToolRegistry {
        tool(RagSearchTool(ragEngine))
        tool(WorksheetTool(ragEngine, orchestrator))
        tool(MaterialSummaryTool(ragEngine, orchestrator))
    }

    override fun isReady(): Boolean = orchestrator.isInferenceReady()

    override suspend fun runAgent(goal: String, tools: List<AgentTool>): AgentResult {
        if (!orchestrator.isInferenceReady()) {
            return AgentResult.Failure("No inference model loaded. Please load a model first.")
        }

        val maxIterations = resolveMaxIterations()
        Logger.i(TAG, "Starting agent for goal: \"$goal\" (maxIterations=$maxIterations)")

        return try {
            val executor = LocalEnginePromptExecutor(
                orchestrator = orchestrator,
                maxTokens = 512,
                temperature = 0.65f
            )

            val agent = AIAgent(
                promptExecutor = executor,
                llmModel = LOCAL_LLM_MODEL,
                toolRegistry = toolRegistry,
                systemPrompt = SYSTEM_PROMPT,
                maxIterations = maxIterations,
                temperature = 0.65
            ) {
                handleEvents {
                    onToolCallStarting { ctx ->
                        val argsStr = ctx.toolArgs.toString()
                        _agentSteps.tryEmit(AgentStep.ToolCalling(ctx.toolName, argsStr))
                        Logger.d(TAG, "Tool calling: ${ctx.toolName}($argsStr)")
                    }
                    onToolCallCompleted { ctx ->
                        val resultStr = ctx.toolResult?.toString() ?: ""
                        _agentSteps.tryEmit(AgentStep.ToolResult(ctx.toolName, resultStr.take(300)))
                        Logger.d(TAG, "Tool result: ${ctx.toolName} → ${resultStr.take(100)}")
                    }
                    onLLMCallCompleted { ctx ->
                        val responseText = ctx.responses
                            .filterIsInstance<Message.Assistant>()
                            .firstOrNull()?.content
                        if (!responseText.isNullOrBlank()) {
                            _agentSteps.tryEmit(AgentStep.Thinking(responseText.take(200)))
                        }
                    }
                }
            }

            val result = agent.run(goal)
            val toolsUsed = toolRegistry.tools.map { it.name }.filter { name ->
                result.contains(name, ignoreCase = true)
            }
            AgentResult.Success(output = result, toolsUsed = toolsUsed)

        } catch (e: Exception) {
            Logger.e(TAG, "Agent run failed", e)
            AgentResult.Failure(e.message ?: "Agent encountered an error")
        }
    }

    /**
     * Dynamically caps agent iterations based on current RAM pressure.
     *
     * Each Koog iteration = 1 LLM call. With 2-3 tool calls, the agent needs
     * at least N+1 iterations (N tool LLM calls + 1 final synthesis call).
     *
     * | RAM pressure | Max iterations | Rationale                        |
     * |-------------|----------------|----------------------------------|
     * | NORMAL      | 10             | Comfortable headroom             |
     * | MODERATE    | 6              | Reduce context accumulation      |
     * | CRITICAL    | 3              | Prevent OOM — minimal loop       |
     */
    private fun resolveMaxIterations(): Int {
        val snapshot = memoryMonitor.getSnapshot()
        return when (snapshot.pressure) {
            MemoryPressure.CRITICAL -> 3
            MemoryPressure.MODERATE -> 6
            MemoryPressure.NORMAL -> 10
        }.also { limit ->
            Logger.d(TAG, "Memory: ${snapshot.availableMb}MB free (${snapshot.pressure}) → maxIterations=$limit")
        }
    }
}

/** Sentinel [LLMProvider] for the on-device local engine. */
private object LocalLLMProvider : LLMProvider("local", "Local Engine")
