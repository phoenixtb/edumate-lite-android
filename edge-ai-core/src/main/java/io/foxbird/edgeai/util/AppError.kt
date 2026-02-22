package io.foxbird.edgeai.util

import arrow.core.Either

typealias AppResult<T> = Either<AppError, T>

sealed class AppError(open val message: String) {

    sealed class Llm(override val message: String) : AppError(message) {
        data class ModelNotFound(val path: String) : Llm("Model not found: $path")
        data class LoadFailed(override val message: String) : Llm(message)
        data class GenerationFailed(override val message: String) : Llm(message)
    }

    sealed class Storage(override val message: String) : AppError(message) {
        data class ReadFailed(override val message: String) : Storage(message)
        data class WriteFailed(override val message: String) : Storage(message)
        data class InsufficientSpace(val requiredMb: Long) : Storage("Need ${requiredMb}MB free space")
    }

    sealed class Processing(override val message: String) : AppError(message) {
        data class ExtractionFailed(override val message: String) : Processing(message)
        data class ChunkingFailed(override val message: String) : Processing(message)
        data class EmbeddingFailed(override val message: String) : Processing(message)
    }

    sealed class Network(override val message: String) : AppError(message) {
        data class DownloadFailed(override val message: String) : Network(message)
        data class ConnectionError(override val message: String) : Network(message)
    }

    data class Unknown(override val message: String) : AppError(message)
}
