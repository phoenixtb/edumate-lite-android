package io.foxbird.edgeai.engine

import arrow.core.left
import arrow.core.right
import com.llamatik.library.platform.GenStream
import com.llamatik.library.platform.LlamaBridge
import io.foxbird.edgeai.util.AppError
import io.foxbird.edgeai.util.AppResult
import io.foxbird.edgeai.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Wraps llama.cpp via Llamatik for GGUF model inference.
 *
 * Implements both [GenerationEngine] and [EmbeddingEngine] since the llama.cpp native
 * library handles tokenization internally for GGUF models — no external tokenizer needed.
 */
class LlamaCppEngine : GenerationEngine, EmbeddingEngine {

    companion object {
        private const val TAG = "LlamaCppEngine"
    }

    // --- GenerationEngine ---

    override val engineType = EngineType.LLAMA_CPP
    override val capabilities = EngineCapabilities(
        supportsGpu = false,
        supportsNpu = false,
        supportsStreaming = true,
        supportedFormats = setOf("gguf")
    )

    @Volatile
    override var lastLoadError: String? = null
        private set

    private var generationModelLoaded = false
    private var embeddingModelLoaded = false
    private var contextSize: Int = 4096

    // --- GenerationEngine.loadModel ---
    override suspend fun loadModel(modelPath: String, contextSize: Int, threads: Int): Boolean =
        withContext(Dispatchers.IO) {
            lastLoadError = null
            try {
                val file = File(modelPath)
                val fileSizeMb = file.length() / (1024L * 1024L)
                val rt = Runtime.getRuntime()
                val freeHeapMb = rt.freeMemory() / (1024L * 1024L)
                val maxHeapMb  = rt.maxMemory()  / (1024L * 1024L)
                Logger.i(TAG, "Load attempt: path=$modelPath size=${fileSizeMb}MB " +
                    "heapFree=${freeHeapMb}MB heapMax=${maxHeapMb}MB")

                if (!file.exists()) {
                    lastLoadError = "Model file not found: $modelPath"
                    Logger.e(TAG, lastLoadError!!)
                    return@withContext false
                }
                if (!file.canRead()) {
                    lastLoadError = "Model file not readable: $modelPath"
                    Logger.e(TAG, lastLoadError!!)
                    return@withContext false
                }
                if (file.length() < 10 * 1024 * 1024) {
                    lastLoadError = "Model file too small (${fileSizeMb}MB) — likely incomplete download"
                    Logger.e(TAG, lastLoadError!!)
                    return@withContext false
                }

                // Validate GGUF magic bytes ('G','G','U','F' = 0x47,0x47,0x55,0x46)
                val magic = ByteArray(4)
                file.inputStream().use { it.read(magic) }
                val isGguf = magic[0] == 0x47.toByte() && magic[1] == 0x47.toByte() &&
                             magic[2] == 0x55.toByte() && magic[3] == 0x46.toByte()
                if (!isGguf) {
                    lastLoadError = "File is not a valid GGUF (bad magic bytes: " +
                        magic.joinToString(" ") { "0x%02X".format(it) } + ") — corrupted download?"
                    Logger.e(TAG, lastLoadError!!)
                    return@withContext false
                }
                Logger.i(TAG, "GGUF magic OK, calling LlamaBridge.initGenerateModel")

                this@LlamaCppEngine.contextSize = contextSize

                val genOk = LlamaBridge.initGenerateModel(modelPath)
                if (!genOk) {
                    // LlamaBridge returns no error string. The actual llama.cpp error is in
                    // logcat under tags: "llama", "ggml", "LlamaBridge". Filter those tags
                    // for the real native reason (architecture unsupported, OOM, etc.).
                    lastLoadError = "llama.cpp rejected the model (initGenerateModel=false). " +
                        "Check logcat tags 'llama'/'ggml' for the native error."
                    Logger.e(TAG, lastLoadError!!)
                    return@withContext false
                }

                generationModelLoaded = true
                Logger.i(TAG, "Model loaded successfully: $modelPath")
                true
            } catch (e: Exception) {
                lastLoadError = e.message ?: e.toString()
                Logger.e(TAG, "Load failed with exception", e)
                false
            }
        }

    override fun unloadModel() {
        try { LlamaBridge.shutdown() } catch (e: Exception) { Logger.e(TAG, "Error during unload", e) }
        lastLoadError = null
        generationModelLoaded = false
        embeddingModelLoaded = false
    }

    override fun isModelLoaded(): Boolean = generationModelLoaded || embeddingModelLoaded

    override fun generate(prompt: String, params: GenerationParams): Flow<String> = callbackFlow {
        if (!generationModelLoaded) {
            close(IllegalStateException("GGUF generation model not loaded"))
            return@callbackFlow
        }
        LlamaBridge.updateGenerateParams(
            temperature = params.temperature,
            maxTokens = params.maxTokens,
            topP = params.topP,
            topK = params.topK,
            repeatPenalty = 1.1f
        )
        LlamaBridge.generateStream(prompt, object : GenStream {
            override fun onDelta(text: String) { trySend(text) }
            override fun onComplete() { close() }
            override fun onError(message: String) { close(Exception(message)) }
        })
        awaitClose { LlamaBridge.nativeCancelGenerate() }
    }.flowOn(Dispatchers.IO)

    override suspend fun generateComplete(prompt: String, params: GenerationParams): AppResult<String> =
        withContext(Dispatchers.IO) {
            if (!generationModelLoaded) {
                return@withContext AppError.Llm.GenerationFailed("GGUF generation model not loaded").left()
            }
            try {
                LlamaBridge.updateGenerateParams(
                    temperature = params.temperature,
                    maxTokens = params.maxTokens,
                    topP = params.topP,
                    topK = params.topK,
                    repeatPenalty = 1.1f
                )
                LlamaBridge.generate(prompt).right()
            } catch (e: Exception) {
                Logger.e(TAG, "Generation failed", e)
                AppError.Llm.GenerationFailed(e.message ?: "Unknown error").left()
            }
        }

    override fun tokenCount(text: String): Int = (text.length * 0.3).toInt().coerceAtLeast(1)

    override fun getContextSize(): Int = contextSize

    override fun cancelGeneration() { LlamaBridge.nativeCancelGenerate() }

    // --- EmbeddingEngine.loadModel (tokenizerPath ignored — llama.cpp tokenizes internally) ---
    override suspend fun loadModel(modelPath: String, contextSize: Int, tokenizerPath: String?): Boolean =
        loadModel(modelPath, contextSize, 0)

    override suspend fun embed(text: String): AppResult<FloatArray> = withContext(Dispatchers.IO) {
        if (!embeddingModelLoaded) {
            return@withContext AppError.Llm.GenerationFailed("GGUF embedding model not loaded").left()
        }
        try {
            LlamaBridge.embed(text).right()
        } catch (e: Exception) {
            Logger.e(TAG, "Embed failed", e)
            AppError.Llm.GenerationFailed(e.message ?: "Embedding error").left()
        }
    }

    override fun getEmbeddingDimension(): Int = 0

    override fun destroy() { unloadModel() }
}
