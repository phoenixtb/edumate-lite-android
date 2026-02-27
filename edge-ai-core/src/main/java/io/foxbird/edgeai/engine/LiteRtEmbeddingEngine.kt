package io.foxbird.edgeai.engine

import arrow.core.left
import arrow.core.right
import com.google.ai.edge.localagents.rag.models.EmbedData
import com.google.ai.edge.localagents.rag.models.EmbeddingRequest
import com.google.ai.edge.localagents.rag.models.GemmaEmbeddingModel
import io.foxbird.edgeai.util.AppError
import io.foxbird.edgeai.util.AppResult
import io.foxbird.edgeai.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

/**
 * [EmbeddingEngine] backed by Google's [GemmaEmbeddingModel] from the AI Edge LocalAgents RAG SDK.
 *
 * Delegates tokenization (SentencePiece) to the native `gemma_embedding_model_jni` library —
 * no external tokenizer library needed. Requires both the `.tflite` model and `sentencepiece.model`.
 */
class LiteRtEmbeddingEngine : EmbeddingEngine {

    companion object {
        private const val TAG = "LiteRtEmbeddingEngine"
    }

    override val engineType = EngineType.LITE_RT

    @Volatile
    override var lastLoadError: String? = null
        private set

    private var gemmaEmbedder: GemmaEmbeddingModel? = null
    private var embeddingDim: Int = 0
    private var loaded = false

    override suspend fun loadModel(
        modelPath: String,
        contextSize: Int,
        tokenizerPath: String?
    ): Boolean = withContext(Dispatchers.IO) {
        lastLoadError = null

        if (tokenizerPath == null) {
            lastLoadError = "GemmaEmbeddingModel requires a sentencepiece.model path"
            return@withContext false
        }

        try {
            val modelFile = java.io.File(modelPath)
            val tokFile = java.io.File(tokenizerPath)

            if (!modelFile.exists()) {
                lastLoadError = "Embedding model not found: $modelPath"
                return@withContext false
            }
            if (!tokFile.exists()) {
                lastLoadError = "SentencePiece tokenizer not found: $tokenizerPath"
                return@withContext false
            }

            gemmaEmbedder = null
            gemmaEmbedder = GemmaEmbeddingModel(modelPath, tokenizerPath, /* useGpu= */ false)
            loaded = true
            Logger.i(TAG, "GemmaEmbeddingModel loaded: $modelPath")
            true
        } catch (e: Exception) {
            lastLoadError = e.message ?: e.toString()
            Logger.e(TAG, "Load failed", e)
            false
        }
    }

    override fun unloadModel() {
        gemmaEmbedder = null
        loaded = false
        embeddingDim = 0
        lastLoadError = null
    }

    override fun isModelLoaded(): Boolean = loaded

    override suspend fun embed(text: String): AppResult<FloatArray> = withContext(Dispatchers.IO) {
        embedRaw(text)
    }

    /** Single-text embedding: one model call, returns normalised FloatArray. */
    private fun embedRaw(text: String): AppResult<FloatArray> {
        val embedder = gemmaEmbedder
            ?: return AppError.Llm.GenerationFailed("Embedding model not loaded").left()
        return try {
            val request = EmbeddingRequest.create(
                listOf(EmbedData.create(text, EmbedData.TaskType.RETRIEVAL_DOCUMENT))
            )
            val rawList = embedder.getEmbeddings(request).get()
            normalise(FloatArray(rawList.size) { rawList[it] }).right()
        } catch (e: Exception) {
            Logger.e(TAG, "Embedding failed", e)
            AppError.Llm.GenerationFailed(e.message ?: "Embedding error").left()
        }
    }

    /**
     * Batch embedding: sends [texts] in a single [EmbeddingRequest] call.
     * GemmaEmbeddingModel returns a flat [List<Float>] — split by [embeddingDim].
     * Falls back to sequential if the batch response size is unexpected.
     */
    override suspend fun embedBatch(texts: List<String>): AppResult<List<FloatArray>> =
        withContext(Dispatchers.IO) { doBatchEmbed(texts) }

    /** Pure, non-suspending batch implementation — keeps return-type inference clean. */
    private fun doBatchEmbed(texts: List<String>): AppResult<List<FloatArray>> {
        val embedder = gemmaEmbedder
            ?: return AppError.Llm.GenerationFailed("Embedding model not loaded").left()
        if (texts.isEmpty()) return emptyList<FloatArray>().right()

        return try {
            val embedDataList = texts.map {
                EmbedData.create(it, EmbedData.TaskType.RETRIEVAL_DOCUMENT)
            }
            val request = EmbeddingRequest.create(embedDataList)
            val flatList = embedder.getEmbeddings(request).get()

            // Infer dim: use cached value or divide evenly
            val dim = if (embeddingDim > 0) embeddingDim
            else if (flatList.size % texts.size == 0) flatList.size / texts.size
            else 0

            if (dim == 0 || flatList.size != dim * texts.size) {
                Logger.w(
                    TAG,
                    "Unexpected batch response size ${flatList.size} for ${texts.size} texts — falling back to sequential"
                )
                sequentialFallback(texts).right()
            } else {
                embeddingDim = dim
                texts.indices.map { idx ->
                    normalise(FloatArray(dim) { flatList[idx * dim + it] })
                }.right()
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Batch embedding failed, retrying sequentially", e)
            sequentialFallback(texts).right()
        }
    }

    private fun sequentialFallback(texts: List<String>): List<FloatArray> =
        texts.map { text -> embedRaw(text).fold(ifLeft = { FloatArray(0) }, ifRight = { it }) }

    private fun normalise(v: FloatArray): FloatArray {
        if (embeddingDim == 0) embeddingDim = v.size
        val norm = sqrt(v.fold(0.0) { acc, f -> acc + f.toDouble() * f }.toFloat())
        return if (norm > 0f) FloatArray(v.size) { v[it] / norm } else v
    }

    override fun getEmbeddingDimension(): Int = embeddingDim

    override fun destroy() {
        unloadModel()
    }
}
