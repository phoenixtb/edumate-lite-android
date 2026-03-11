package io.foxbird.edumate.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.foxbird.edgeai.engine.MemoryMonitor
import io.foxbird.edgeai.engine.MemorySnapshot
import io.foxbird.edgeai.engine.ModelManager
import io.foxbird.edgeai.model.DownloadEvent
import io.foxbird.edgeai.model.ModelConfig
import io.foxbird.edgeai.model.ModelState
import io.foxbird.doclibrary.data.local.DocumentDatabase
import io.foxbird.doclibrary.data.repository.ChunkRepository
import io.foxbird.doclibrary.data.repository.DocumentRepository
import io.foxbird.edumate.data.local.EduMateDatabase
import io.foxbird.edumate.data.preferences.AppPreferences
import io.foxbird.edumate.data.preferences.UserPreferencesManager
import io.foxbird.edumate.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val prefsManager: UserPreferencesManager,
    val modelManager: ModelManager,
    private val memoryMonitor: MemoryMonitor,
    private val documentRepository: DocumentRepository,
    private val chunkRepository: ChunkRepository,
    private val chatDatabase: EduMateDatabase,
    private val documentDatabase: DocumentDatabase
) : ViewModel() {

    val preferences: StateFlow<AppPreferences> = prefsManager.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferences())

    val modelStates: StateFlow<Map<String, ModelState>> = modelManager.modelStates
    val activeInferenceModelId: StateFlow<String?> = modelManager.activeInferenceModelId

    val memorySnapshot: StateFlow<MemorySnapshot> = memoryMonitor.snapshot

    private val _materialCount = MutableStateFlow(0)
    val documentCount: StateFlow<Int> = _materialCount.asStateFlow()

    private val _chunkCount = MutableStateFlow(0)
    val chunkCount: StateFlow<Int> = _chunkCount.asStateFlow()

    private val _isLoadingModel = MutableStateFlow<String?>(null)
    val isLoadingModel: StateFlow<String?> = _isLoadingModel.asStateFlow()

    private val _lastLoadError = MutableStateFlow<String?>(null)
    val lastLoadError: StateFlow<String?> = _lastLoadError.asStateFlow()

    init {
        refreshCounts()
        viewModelScope.launch {
            modelManager.initialize()
            restorePreferredModel()
        }
    }

    /**
     * After initialization, auto-load the model the user last used.
     * Falls back to the bundled default (Gemma 3n E2B) if the preferred model
     * is not downloaded or not registered.
     */
    private suspend fun restorePreferredModel() {
        val preferredId = prefsManager.preferencesFlow.first().activeInferenceModelId

        val target = if (preferredId.isNotEmpty()) {
            modelManager.getInferenceModels().find { it.config.id == preferredId }
        } else null

        val modelToLoad = when {
            target != null && modelManager.isModelAvailable(target.config) -> target.config
            else -> modelManager.getInferenceModels()
                .firstOrNull { it.config.isBundled && modelManager.isModelAvailable(it.config) }
                ?.config
        } ?: return

        // Only load if not already loaded
        if (modelManager.activeInferenceModelId.value == modelToLoad.id) return
        loadModel(modelToLoad)
    }

    private fun refreshCounts() {
        viewModelScope.launch {
            _materialCount.value = documentRepository.getCount()
            _chunkCount.value = chunkRepository.getTotalCount()
        }
    }

    fun loadModel(config: ModelConfig) {
        viewModelScope.launch {
            _isLoadingModel.value = config.id
            _lastLoadError.value = null
            try {
                val success = modelManager.loadModel(config)
                if (success) {
                    prefsManager.setActiveInferenceModelId(config.id)
                } else {
                    val state = modelManager.modelStates.value[config.id]
                    _lastLoadError.value = (state as? ModelState.LoadFailed)?.error
                        ?: "Failed to load model"
                }
            } finally {
                _isLoadingModel.value = null
            }
        }
    }

    fun unloadModel(config: ModelConfig) {
        modelManager.unloadModel(config)
    }

    fun downloadModel(config: ModelConfig) {
        viewModelScope.launch {
            _isLoadingModel.value = config.id
            _lastLoadError.value = null
            try {
                modelManager.downloadModel(config).collect { event ->
                    when (event) {
                        is DownloadEvent.Progress -> Unit  // state updated by ModelManager
                        is DownloadEvent.Complete -> _isLoadingModel.value = null
                        is DownloadEvent.Error -> {
                            _isLoadingModel.value = null
                            _lastLoadError.value = event.message
                        }
                        is DownloadEvent.Retrying -> Unit  // keep loading indicator
                    }
                }
            } catch (e: Exception) {
                _isLoadingModel.value = null
                _lastLoadError.value = e.message ?: "Download failed unexpectedly"
            }
        }
    }

    fun deleteModel(config: ModelConfig) {
        viewModelScope.launch {
            modelManager.deleteModel(config)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefsManager.setThemeMode(mode) }
    }

    fun setExtractConcepts(enabled: Boolean) {
        viewModelScope.launch { prefsManager.setExtractConcepts(enabled) }
    }

    fun setDeveloperMode(enabled: Boolean) {
        viewModelScope.launch { prefsManager.setDeveloperMode(enabled) }
    }

    fun clearAllData() {
        viewModelScope.launch {
            chatDatabase.clearAllTables()
            documentDatabase.clearAllTables()
            refreshCounts()
        }
    }
}
