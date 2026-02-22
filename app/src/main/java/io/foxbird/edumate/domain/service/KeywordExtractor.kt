package io.foxbird.edumate.domain.service

import kotlin.math.ln

class KeywordExtractor {

    fun extractKeywords(text: String, topN: Int = 10): List<String> {
        val words = tokenize(text)
        if (words.isEmpty()) return emptyList()

        val tf = computeTf(words)
        val idf = computeIdf(text, tf.keys)
        val tfidf = tf.mapValues { (word, freq) -> freq * (idf[word] ?: 1.0) }

        return tfidf.entries
            .sortedByDescending { it.value }
            .take(topN)
            .map { it.key }
    }

    fun extractConceptTags(chunks: List<String>, topN: Int = 20): List<String> {
        val allText = chunks.joinToString(" ")
        return extractKeywords(allText, topN)
    }

    private fun tokenize(text: String): List<String> {
        return text.lowercase()
            .replace("[^a-z0-9\\s]".toRegex(), " ")
            .split("\\s+".toRegex())
            .filter { it.length > 2 && it !in STOP_WORDS }
    }

    private fun computeTf(words: List<String>): Map<String, Double> {
        val counts = words.groupingBy { it }.eachCount()
        val total = words.size.toDouble()
        return counts.mapValues { (_, count) -> count / total }
    }

    private fun computeIdf(text: String, vocab: Set<String>): Map<String, Double> {
        val sentences = text.split("[.!?]+".toRegex()).filter { it.isNotBlank() }
        val n = sentences.size.toDouble().coerceAtLeast(1.0)
        return vocab.associateWith { word ->
            val df = sentences.count { it.lowercase().contains(word) }.toDouble().coerceAtLeast(1.0)
            ln(n / df) + 1.0
        }
    }

    companion object {
        private val STOP_WORDS = setOf(
            "the", "and", "for", "are", "but", "not", "you", "all", "any", "can", "had",
            "her", "was", "one", "our", "out", "has", "his", "how", "its", "may", "new",
            "now", "old", "see", "way", "who", "did", "get", "let", "say", "she", "too",
            "use", "that", "with", "have", "this", "will", "your", "from", "they", "been",
            "said", "each", "which", "their", "than", "also", "into", "some", "could",
            "them", "then", "these", "two", "more", "very", "when", "what", "there",
            "about", "would", "make", "like", "just", "over", "such", "take", "other",
            "being", "because", "does", "here", "most", "only", "come", "made", "after",
        )
    }
}
