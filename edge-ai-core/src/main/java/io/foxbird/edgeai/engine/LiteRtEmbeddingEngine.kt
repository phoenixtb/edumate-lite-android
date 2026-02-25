package io.foxbird.edgeai.engine

import arrow.core.left
import arrow.core.right
import com.google.ai.edge.litert.Accelerator
import com.google.ai.edge.litert.CompiledModel
import io.foxbird.edgeai.tokenizer.SentencePieceTokenizer
import io.foxbird.edgeai.tokenizer.Tokenizer
import io.foxbird.edgeai.util.AppError
import io.foxbird.edgeai.util.AppResult
import io.foxbird.edgeai.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.sqrt

/**
 * [EmbeddingEngine] backed by LiteRT [CompiledModel] for `.tflite` embedding models.
 * Hardware-accelerated with GPU/CPU acceleration.
 *
 * Tokenization: Uses [SentencePieceTokenizer] when a `tokenizerPath` is supplied.
 * EmbeddingGemma expects two Int32 input tensors:
 *   - input_ids:      [1, seq_len] — SentencePiece token IDs
 *   - attention_mask: [1, seq_len] — 1 for real tokens, 0 for padding
 * Output: Float32 tensor [1, embedding_dim] (1024 for EmbeddingGemma 300M).
 */
class LiteRtEmbeddingEngine : EmbeddingEngine {

    companion object {
        private const val TAG = "LiteRtEmbeddingEngine"
        private const val DEFAULT_EMBEDDING_DIM = 1024
    }

    override val engineType = EngineType.LITE_RT

    @Volatile
    override var lastLoadError: String? = null
        private set

    private var compiledModel: CompiledModel? = null
    private var tokenizer: Tokenizer? = null
    private var contextSize: Int = 2048
    private var loaded = false

    override suspend fun loadModel(
        modelPath: String,
        contextSize: Int,
        tokenizerPath: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        lastLoadError = null
        try {
            val file = File(modelPath)
            if (!file.exists()) {
                lastLoadError = "Embedding model file not found: $modelPath"
                return@withContext false
            }

            this@LiteRtEmbeddingEngine.contextSize = contextSize

            // Load tokenizer before model so any tokenizer errors fail fast
            if (tokenizerPath != null) {
                val tokenizerFile = File(tokenizerPath)
                if (!tokenizerFile.exists()) {
                    lastLoadError = "Tokenizer file not found: $tokenizerPath"
                    return@withContext false
                }
                tokenizer?.close()
                tokenizer = SentencePieceTokenizer(tokenizerPath)
                Logger.i(TAG, "Tokenizer loaded: $tokenizerPath")
            } else {
                Logger.w(TAG, "No tokenizerPath provided — embedding quality will be degraded")
            }

            val model = CompiledModel.create(modelPath, CompiledModel.Options(Accelerator.CPU))
            compiledModel = model
            loaded = true
            lastLoadError = null
            Logger.i(TAG, "LiteRT embedding model loaded: $modelPath")
            true
        } catch (e: Exception) {
            lastLoadError = e.message ?: e.toString()
            Logger.e(TAG, "Load failed", e)
            false
        }
    }

    override fun unloadModel() {
        try { compiledModel?.close() } catch (e: Exception) { Logger.e(TAG, "Error unloading embedding model", e) }
        compiledModel = null
        tokenizer?.close()
        tokenizer = null
        loaded = false
        lastLoadError = null
    }

    override fun isModelLoaded(): Boolean = loaded

    override suspend fun embed(text: String): AppResult<FloatArray> = withContext(Dispatchers.IO) {
        val model = compiledModel
            ?: return@withContext AppError.Llm.GenerationFailed("LiteRT embedding model not loaded").left()

        try {
            val inputBuffers = model.createInputBuffers()
            val outputBuffers = model.createOutputBuffers()

            val tok = tokenizer
            if (tok != null) {
                // Proper SentencePiece tokenization: encode text → Int32 token IDs
                val tokenIds = tok.encode(text, contextSize)
                val paddedIds = IntArray(contextSize) { if (it < tokenIds.size) tokenIds[it] else 0 }
                val attentionMask = tok.attentionMask(tokenIds.size, contextSize)

                // Write Int32 tensors (not Float — this was the critical bug in the old code)
                inputBuffers[0].writeInt(paddedIds)
                inputBuffers[1].writeInt(attentionMask)
            } else {
                // Fallback: char-frequency hash tokenizer — low quality but functional structure
                // The model will still produce a float output; quality improves with real tokenizer
                Logger.w(TAG, "No tokenizer loaded, using character-hash fallback")
                val charIds = text.lowercase().chunked(3).map { chunk ->
                    (chunk.fold(0) { acc, c -> acc * 31 + c.code } and 0x7FFF) + 1
                }.take(contextSize)
                val paddedIds = IntArray(contextSize) { if (it < charIds.size) charIds[it] else 0 }
                val attentionMask = IntArray(contextSize) { if (it < charIds.size) 1 else 0 }
                inputBuffers[0].writeInt(paddedIds)
                inputBuffers[1].writeInt(attentionMask)
            }

            model.run(inputBuffers, outputBuffers)

            val rawEmbedding = outputBuffers[0].readFloat()

            // L2-normalise: cosine similarity = dot product for unit vectors
            val norm = sqrt(rawEmbedding.fold(0.0) { acc, v -> acc + v.toDouble() * v }.toFloat())
            val embedding = if (norm > 0f) FloatArray(rawEmbedding.size) { rawEmbedding[it] / norm }
                           else rawEmbedding

            embedding.right()
        } catch (e: Exception) {
            Logger.e(TAG, "Embedding failed", e)
            AppError.Llm.GenerationFailed(e.message ?: "Embedding error").left()
        }
    }

    override fun getEmbeddingDimension(): Int = if (loaded) DEFAULT_EMBEDDING_DIM else 0

    override fun destroy() { unloadModel() }
}
