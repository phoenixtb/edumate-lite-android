package io.foxbird.edumate.domain.engine

import kotlin.math.ln

class BM25Scorer(
    private val k1: Float = 1.5f,
    private val b: Float = 0.75f
) {
    fun score(
        query: String,
        documents: List<String>
    ): List<Float> {
        val queryTerms = tokenize(query)
        val docTokens = documents.map { tokenize(it) }
        val avgDl = docTokens.map { it.size }.average().toFloat().coerceAtLeast(1f)
        val n = documents.size.toFloat().coerceAtLeast(1f)

        val idf = mutableMapOf<String, Float>()
        for (term in queryTerms.toSet()) {
            val df = docTokens.count { tokens -> term in tokens }.toFloat()
            idf[term] = ln((n - df + 0.5f) / (df + 0.5f) + 1f)
        }

        return docTokens.map { tokens ->
            val dl = tokens.size.toFloat()
            val tf = tokens.groupingBy { it }.eachCount()
            queryTerms.toSet().sumOf { term ->
                val termFreq = tf[term]?.toFloat() ?: 0f
                val termIdf = idf[term] ?: 0f
                val numerator = termFreq * (k1 + 1)
                val denominator = termFreq + k1 * (1 - b + b * dl / avgDl)
                (termIdf * numerator / denominator).toDouble()
            }.toFloat()
        }
    }

    private fun tokenize(text: String): List<String> {
        return text.lowercase()
            .replace("[^a-z0-9\\s]".toRegex(), " ")
            .split("\\s+".toRegex())
            .filter { it.length > 1 }
    }
}
