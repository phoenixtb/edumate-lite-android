package io.foxbird.edgeai.model

import io.foxbird.edgeai.engine.EngineType
import kotlinx.serialization.Serializable

@Serializable
data class ModelConfig(
    val id: String,
    val name: String,
    val description: String,
    val huggingFaceRepo: String,
    val filename: String,
    val fileSizeMB: Int,
    val requiredRamGB: Int,
    val contextLength: Int,
    val purpose: ModelPurpose = ModelPurpose.INFERENCE,
    val engineType: EngineType = EngineType.LLAMA_CPP,
    val isBundled: Boolean = false,
    val fallbackModelId: String? = null
) {
    val downloadUrl: String
        get() = "https://huggingface.co/$huggingFaceRepo/resolve/main/$filename"

    val fileSizeDisplay: String
        get() = if (fileSizeMB >= 1000) {
            String.format("%.1f GB", fileSizeMB / 1000.0)
        } else {
            "$fileSizeMB MB"
        }

    val fileExtension: String
        get() = filename.substringAfterLast('.', "").lowercase()
}

enum class ModelPurpose {
    INFERENCE,
    EMBEDDING
}

sealed class ModelState {
    data object NotDownloaded : ModelState()
    data class Downloading(val progress: Float) : ModelState()
    data class DownloadFailed(val error: String) : ModelState()
    data object Downloaded : ModelState()
    data object Loading : ModelState()
    data object Ready : ModelState()
    data class LoadFailed(val error: String) : ModelState()
}

data class ModelInfo(
    val config: ModelConfig,
    val state: ModelState,
    val localPath: String? = null
)
