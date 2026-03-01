package io.foxbird.agentcore.tools

import ai.koog.agents.core.tools.SimpleTool
import io.foxbird.doclibrary.domain.rag.IRagEngine
import kotlinx.serialization.Serializable

@Serializable
data class RagSearchArgs(
    val query: String,
    val top_k: Int = 5
)

class RagSearchTool(private val ragEngine: IRagEngine) : SimpleTool<RagSearchArgs>(
    argsSerializer = RagSearchArgs.serializer(),
    name = "rag_search",
    description = "Search the student's study materials for information relevant to a query. " +
        "Returns the most relevant excerpts. Use this when you need factual content from materials."
) {
    override suspend fun execute(args: RagSearchArgs): String {
        val context = ragEngine.retrieve(
            query = args.query,
            documentIds = null,
            topK = args.top_k.coerceIn(1, 10),
            threshold = 0.2f
        )
        return if (context.contextText.isBlank()) {
            "No relevant material found for: \"${args.query}\""
        } else {
            context.contextText.take(2000)
        }
    }
}
