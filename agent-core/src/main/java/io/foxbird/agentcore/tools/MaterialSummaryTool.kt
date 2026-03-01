package io.foxbird.agentcore.tools

import ai.koog.agents.core.tools.SimpleTool
import io.foxbird.doclibrary.domain.rag.IRagEngine
import io.foxbird.edgeai.engine.EngineOrchestrator
import io.foxbird.edgeai.engine.GenerationParams
import kotlinx.serialization.Serializable

@Serializable
data class MaterialSummaryArgs(val topic: String)

class MaterialSummaryTool(
    private val ragEngine: IRagEngine,
    private val orchestrator: EngineOrchestrator
) : SimpleTool<MaterialSummaryArgs>(
    argsSerializer = MaterialSummaryArgs.serializer(),
    name = "summarize_material",
    description = "Retrieve and summarize the key points about a topic from the student's study materials. " +
        "Use this to give the student a concise overview before diving into details."
) {
    override suspend fun execute(args: MaterialSummaryArgs): String {
        val context = ragEngine.retrieve(
            query = args.topic,
            documentIds = null,
            topK = 6,
            threshold = 0.15f
        )
        if (context.contextText.isBlank()) return "No material found for \"${args.topic}\"."

        val prompt = buildString {
            append("System: Summarize the following study material about '${args.topic}' in 3-5 concise bullet points. Be factual and educational.\n\n")
            append("Material:\n${context.contextText.take(1500)}\n\nSummary:")
        }

        return orchestrator.generateComplete(
            prompt = prompt,
            params = GenerationParams(maxTokens = 300, temperature = 0.3f)
        ).fold(
            ifLeft = { "Could not summarize material." },
            ifRight = { it.trim() }
        )
    }
}
