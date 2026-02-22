package io.foxbird.edumate.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.foxbird.edgeai.engine.ModelManager
import io.foxbird.edgeai.model.DownloadEvent
import io.foxbird.edgeai.model.ModelConfig
import io.foxbird.edgeai.model.ModelState
import io.foxbird.edumate.core.model.AppModelConfigs
import io.foxbird.edumate.data.preferences.UserPreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val inferenceModelState: ModelState = ModelState.NotDownloaded,
    val embeddingModelState: ModelState = ModelState.NotDownloaded,
    val downloadProgress: Float = 0f,
    val error: String? = null,
    val isComplete: Boolean = false,
    val isPreparing: Boolean = false
)

enum class OnboardingStep {
    WELCOME, MODEL_SETUP, COMPLETE
}

class OnboardingViewModel(
    private val modelManager: ModelManager,
    private val prefsManager: UserPreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    // Use bundled LiteRT models as primary, fall back to GGUF
    private val primaryInference = AppModelConfigs.GEMMA_3N_E2B_LITERT
    private val primaryEmbedding = AppModelConfigs.EMBEDDING_GEMMA_LITERT

    init {
        viewModelScope.launch {
            modelManager.initialize()
            updateModelStates()
        }
    }

    fun advanceToModelSetup() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPreparing = true)
            modelManager.initialize()
            updateModelStates()
            _uiState.value = _uiState.value.copy(
                step = OnboardingStep.MODEL_SETUP,
                isPreparing = false
            )
        }
    }

    fun downloadAndSetupModels() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(error = null)

            // Check if bundled models are already available
            val inferenceAvailable = modelManager.isModelAvailable(primaryInference)
            val embeddingAvailable = modelManager.isModelAvailable(primaryEmbedding)

            if (!inferenceAvailable) {
                downloadModel(primaryInference) { progress ->
                    _uiState.value = _uiState.value.copy(
                        inferenceModelState = ModelState.Downloading(progress),
                        downloadProgress = progress * 0.7f
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(downloadProgress = 0.7f)
            }

            if (!embeddingAvailable) {
                downloadModel(primaryEmbedding) { progress ->
                    _uiState.value = _uiState.value.copy(
                        embeddingModelState = ModelState.Downloading(progress),
                        downloadProgress = 0.7f + progress * 0.2f
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(downloadProgress = 0.9f)
            }

            // Load models (ModelManager handles fallback automatically)
            _uiState.value = _uiState.value.copy(downloadProgress = 0.9f)
            val inferenceLoaded = modelManager.loadModel(primaryInference)
            val embeddingLoaded = modelManager.loadModel(primaryEmbedding)

            if (inferenceLoaded && embeddingLoaded) {
                _uiState.value = _uiState.value.copy(
                    step = OnboardingStep.COMPLETE,
                    downloadProgress = 1f,
                    inferenceModelState = ModelState.Ready,
                    embeddingModelState = ModelState.Ready,
                    isComplete = true
                )
            } else {
                val loadError = getLoadErrorMessage()
                _uiState.value = _uiState.value.copy(
                    error = loadError
                )
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            prefsManager.setOnboardingComplete(true)
            _uiState.value = _uiState.value.copy(isComplete = true)
        }
    }

    fun skipOnboarding() {
        viewModelScope.launch {
            prefsManager.setOnboardingComplete(true)
            _uiState.value = _uiState.value.copy(isComplete = true)
        }
    }

    private suspend fun downloadModel(config: ModelConfig, onProgress: (Float) -> Unit) {
        if (modelManager.isModelAvailable(config)) {
            onProgress(1f)
            return
        }

        modelManager.downloadModel(config).collect { event ->
            when (event) {
                is DownloadEvent.Progress -> onProgress(event.progressPercent)
                is DownloadEvent.Complete -> onProgress(1f)
                is DownloadEvent.Error -> {
                    _uiState.value = _uiState.value.copy(error = event.message)
                }
                is DownloadEvent.Retrying -> {}
            }
        }
    }

    private fun updateModelStates() {
        val states = modelManager.modelStates.value
        _uiState.value = _uiState.value.copy(
            inferenceModelState = states[primaryInference.id] ?: ModelState.NotDownloaded,
            embeddingModelState = states[primaryEmbedding.id] ?: ModelState.NotDownloaded
        )
    }

    private fun getLoadErrorMessage(): String {
        val states = modelManager.modelStates.value
        val inf = states[primaryInference.id]
        val emb = states[primaryEmbedding.id]
        val errors = mutableListOf<String>()
        when (inf) {
            is ModelState.LoadFailed -> errors.add("Inference: ${inf.error}")
            else -> if (inf != ModelState.Ready) errors.add("Inference: ${inf?.javaClass?.simpleName ?: "not loaded"}")
        }
        when (emb) {
            is ModelState.LoadFailed -> errors.add("Embedding: ${emb.error}")
            else -> if (emb != ModelState.Ready) errors.add("Embedding: ${emb?.javaClass?.simpleName ?: "not loaded"}")
        }
        return if (errors.isNotEmpty()) "Failed to load: ${errors.joinToString(". ")}" else "Failed to load models. You can retry or skip and set up later."
    }
}
