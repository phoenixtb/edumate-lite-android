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

    /**
     * Qwen3.5 2B — hybrid Gated Delta Network + MoE architecture (downloadable).
     * ~1.3 GB (Q4_K_M). Significant quality improvement over older Qwen models.
     * Supports thinking mode via <think>...</think> tags; 32K native context.
     */
    val QWEN3_5_2B = ModelConfig(
        id = "qwen3.5-2b-q4km",
        name = "Qwen 3.5 2B",
        description = "Hybrid MoE, thinking mode, 32K context, ~1.3 GB",
        huggingFaceRepo = "unsloth/Qwen3.5-2B-GGUF",
        filename = "Qwen3.5-2B-Q4_K_M.gguf",
        fileSizeMB = 1280,
        requiredRamGB = 3,
        contextLength = 32768,
        engineType = EngineType.LLAMA_CPP,
        thinkOpenTag = "<think>",
        thinkCloseTag = "</think>"
    )

    /**
     * Qwen3.5 4B — larger capacity variant of the same hybrid GDN + MoE architecture.
     * ~2.5 GB (Q4_K_M). Better reasoning and comprehension than the 2B variant.
     * Supports thinking mode via <think>...</think> tags; 32K native context.
     */
    val QWEN3_5_4B = ModelConfig(
        id = "qwen3.5-4b-q4km",
        name = "Qwen 3.5 4B",
        description = "Hybrid MoE, thinking mode, 32K context, ~2.5 GB",
        huggingFaceRepo = "unsloth/Qwen3.5-4B-GGUF",
        filename = "Qwen3.5-4B-Q4_K_M.gguf",
        fileSizeMB = 2500,
        requiredRamGB = 5,
        contextLength = 32768,
        engineType = EngineType.LLAMA_CPP,
        thinkOpenTag = "<think>",
        thinkCloseTag = "</think>"
    )

    /**
     * Agent-optimised tool-calling model — LFM2 1.2B Instruct (downloadable).
     * ~900 MB RAM (Q4_K_M). Designed explicitly for function-calling on edge devices.
     * Used by agent-core as the reasoning backbone for multi-step agentic workflows.
     */
    val LFM2_1_2B_TOOL = ModelConfig(
        id = "lfm2-1.2b-tool-q4km",
        name = "LFM2 1.2B Tool",
        description = "Agentic tool-calling model, ~900 MB",
        huggingFaceRepo = "LiquidAI/LFM2-1.2B-Tool-GGUF",
        filename = "LFM2-1.2B-Tool-Q4_K_M.gguf",
        fileSizeMB = 900,
        requiredRamGB = 2,
        contextLength = 8192,
        engineType = EngineType.LLAMA_CPP,
        isBundled = false
    )

    val ALL_INFERENCE = listOf(GEMMA_3N_E2B_LITERT, GEMMA_2B, QWEN3_5_2B, QWEN3_5_4B, LFM2_1_2B_TOOL)
    val ALL_EMBEDDING = listOf(EMBEDDING_GEMMA_LITERT)
    val ALL = ALL_INFERENCE + ALL_EMBEDDING

    fun findById(id: String): ModelConfig? = ALL.find { it.id == id }
}
