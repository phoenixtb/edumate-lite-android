package io.foxbird.edgeai.engine

/**
 * Engine backend type. Used by [io.foxbird.edgeai.model.ModelConfig] and
 * [EngineOrchestrator] to route model load/unload requests.
 */
enum class EngineType {
    LLAMA_CPP,
    LITE_RT
}
