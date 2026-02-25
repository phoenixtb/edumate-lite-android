package io.foxbird.edumate.core.model

import io.foxbird.edgeai.engine.EngineType
import io.foxbird.edgeai.model.ModelConfig
import io.foxbird.edgeai.model.ModelPurpose

object AppModelConfigs {

    /** Primary inference model — Gemma 3n E2B via LiteRT (hardware-accelerated). */
    val GEMMA_3N_E2B_LITERT = ModelConfig(
        id = "gemma-3n-e2b-litert",
        name = "Gemma 3n E2B",
        description = "Vision & text, hardware accelerated",
        huggingFaceRepo = "litert-community/gemma-3n-E2B-it",
        filename = "gemma-3n-E2B-it-int4.litertlm",
        fileSizeMB = 3500,
        requiredRamGB = 4,
        contextLength = 4096,
        engineType = EngineType.LITE_RT,
        isBundled = true
    )

    /** Bundled embedding model — hardware-accelerated via LiteRT.
     *  Requires sentencepiece.model tokenizer co-located in the models directory. */
    val EMBEDDING_GEMMA_LITERT = ModelConfig(
        id = "embedding-gemma-300m-litert",
        name = "EmbeddingGemma 300M",
        description = "Semantic search, hardware accelerated",
        huggingFaceRepo = "litert-community/embeddinggemma-300m",
        filename = "embeddinggemma-300M_seq2048_mixed-precision.tflite",
        fileSizeMB = 200,
        requiredRamGB = 1,
        contextLength = 2048,
        purpose = ModelPurpose.EMBEDDING,
        engineType = EngineType.LITE_RT,
        isBundled = true,
        tokenizerFilename = "sentencepiece.model"
    )

    /** Lighter inference alternative for low-RAM devices (downloadable). */
    val GEMMA_2B = ModelConfig(
        id = "gemma-2-2b-it-q4km",
        name = "Gemma 2 2B IT",
        description = "Lightweight alternative, ~1.5 GB",
        huggingFaceRepo = "bartowski/gemma-2-2b-it-GGUF",
        filename = "gemma-2-2b-it-Q4_K_M.gguf",
        fileSizeMB = 1500,
        requiredRamGB = 3,
        contextLength = 4096,
        engineType = EngineType.LLAMA_CPP,
        isBundled = false
    )

    /** Lightest inference alternative (downloadable). */
    val QWEN_1_5B = ModelConfig(
        id = "qwen2.5-1.5b-instruct-q4km",
        name = "Qwen 2.5 1.5B Instruct",
        description = "Lightweight alternative, ~1.1 GB",
        huggingFaceRepo = "Qwen/Qwen2.5-1.5B-Instruct-GGUF",
        filename = "qwen2.5-1.5b-instruct-q4_k_m.gguf",
        fileSizeMB = 1100,
        requiredRamGB = 2,
        contextLength = 4096,
        engineType = EngineType.LLAMA_CPP
    )

    val ALL_INFERENCE = listOf(GEMMA_3N_E2B_LITERT, GEMMA_2B, QWEN_1_5B)
    val ALL_EMBEDDING = listOf(EMBEDDING_GEMMA_LITERT)
    val ALL = ALL_INFERENCE + ALL_EMBEDDING

    fun findById(id: String): ModelConfig? = ALL.find { it.id == id }
}
