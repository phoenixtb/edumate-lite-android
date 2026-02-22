package io.foxbird.edumate.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.foxbird.edgeai.engine.ModelManager
import io.foxbird.edgeai.model.DownloadEvent
import io.foxbird.edgeai.model.ModelConfig
import io.foxbird.edgeai.model.ModelState
import io.foxbird.edumate.data.local.EduMateDatabase
import io.foxbird.edumate.data.local.dao.ChunkDao
import io.foxbird.edumate.data.local.dao.MaterialDao
import io.foxbird.edumate.data.preferences.AppPreferences
import io.foxbird.edumate.data.preferences.UserPreferencesManager
import io.foxbird.edumate.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val prefsManager: UserPreferencesManager,
    val modelManager: ModelManager,
    private val materialDao: MaterialDao,
    private val chunkDao: ChunkDao,
    private val database: EduMateDatabase
) : ViewModel() {

    val preferences: StateFlow<AppPreferences> = prefsManager.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferences())

    val modelStates: StateFlow<Map<String, ModelState>> = modelManager.modelStates
    val activeInferenceModelId: StateFlow<String?> = modelManager.activeInferenceModelId

    private val _materialCount = MutableStateFlow(0)
    val materialCount: StateFlow<Int> = _materialCount.asStateFlow()

    private val _chunkCount = MutableStateFlow(0)
    val chunkCount: StateFlow<Int> = _chunkCount.asStateFlow()

    private val _isLoadingModel = MutableStateFlow<String?>(null)
    val isLoadingModel: StateFlow<String?> = _isLoadingModel.asStateFlow()

    init {
        refreshCounts()
    }

    private fun refreshCounts() {
        viewModelScope.launch {
            _materialCount.value = materialDao.getCount()
            _chunkCount.value = chunkDao.getTotalCount()
        }
    }

    fun loadModel(config: ModelConfig) {
        viewModelScope.launch {
            _isLoadingModel.value = config.id
            try {
                modelManager.loadModel(config)
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
            try {
                modelManager.downloadModel(config).collect { event ->
                    when (event) {
                        is DownloadEvent.Progress -> { /* state updated by ModelManager */ }
                        is DownloadEvent.Complete -> { _isLoadingModel.value = null }
                        is DownloadEvent.Error -> { _isLoadingModel.value = null }
                        is DownloadEvent.Retrying -> { /* keep loading indicator */ }
                    }
                }
            } catch (_: Exception) {
                _isLoadingModel.value = null
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
            database.clearAllTables()
            refreshCounts()
        }
    }
}
