package io.foxbird.edumate.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.foxbird.edgeai.engine.ModelManager
import io.foxbird.edgeai.model.ModelState
import io.foxbird.edumate.core.model.AppModelConfigs
import io.foxbird.edumate.data.preferences.UserPreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val isComplete: Boolean = false,
    val error: String? = null
)

class OnboardingViewModel(
    private val modelManager: ModelManager,
    private val prefsManager: UserPreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    // Expose live model loading state (driven by Application auto-load)
    val inferenceModelState: StateFlow<ModelState> = modelManager.modelStates
        .map { states ->
            states[AppModelConfigs.GEMMA_3N_E2B_LITERT.id]
                ?: ModelState.NotDownloaded
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ModelState.NotDownloaded)

    init {
        // Initialize model file discovery (idempotent — Application may have already called this)
        viewModelScope.launch { modelManager.initialize() }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            prefsManager.setOnboardingComplete(true)
            _uiState.value = _uiState.value.copy(isComplete = true)
        }
    }
}
