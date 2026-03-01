package io.foxbird.agentcore.tools

import ai.koog.agents.core.tools.SimpleTool
import io.foxbird.doclibrary.domain.rag.IRagEngine
import io.foxbird.edgeai.engine.EngineOrchestrator
import io.foxbird.edgeai.engine.GenerationParams
import kotlinx.serialization.Serializable

@Serializable
data class WorksheetArgs(
    val topic: String,
    val num_questions: Int = 5,
    val difficulty: String = "medium"
)

class WorksheetTool(
    private val ragEngine: IRagEngine,
    private val orchestrator: EngineOrchestrator
) : SimpleTool<WorksheetArgs>(
    argsSerializer = WorksheetArgs.serializer(),
    name = "generate_worksheet",
    description = "Generate a numbered practice worksheet with questions for a given topic using study materials. " +
        "Difficulty can be 'easy', 'medium', or 'hard'. Includes an answer key."
) {
    override suspend fun execute(args: WorksheetArgs): String {
        val numQ = args.num_questions.coerceIn(3, 15)
        val difficulty = args.difficulty.lowercase().takeIf { it in listOf("easy", "medium", "hard") } ?: "medium"

        val context = ragEngine.retrieve(
            query = "practice questions about ${args.topic}",
            documentIds = null,
            topK = 5,
            threshold = 0.0f
        )
        if (context.contextText.isBlank()) {
            return "No material found for \"${args.topic}\" — worksheet cannot be generated."
        }

        val prompt = buildString {
            append("System: Generate a $difficulty-difficulty practice worksheet titled '${args.topic}' with $numQ numbered questions. ")
            append("Include multiple choice, short answer, and true/false types. Add an answer key at the end.\n\n")
            append("Study Material:\n${context.contextText.take(1500)}\n\nWorksheet:")
        }

        return orchestrator.generateComplete(
            prompt = prompt,
            params = GenerationParams(maxTokens = 1024, temperature = 0.3f)
        ).fold(
            ifLeft = { "Worksheet generation failed: ${it.message}" },
            ifRight = { it.trim() }
        )
    }
}
