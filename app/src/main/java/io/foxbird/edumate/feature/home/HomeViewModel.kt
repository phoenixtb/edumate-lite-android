package io.foxbird.edumate.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.foxbird.edgeai.engine.ModelManager
import io.foxbird.edgeai.model.ModelState
import io.foxbird.edumate.core.model.AppModelConfigs
import io.foxbird.edumate.data.local.dao.ConceptDao
import io.foxbird.edumate.data.local.entity.MaterialEntity
import io.foxbird.edumate.data.repository.ChunkRepository
import io.foxbird.edumate.data.repository.ConversationRepository
import io.foxbird.edumate.data.repository.MaterialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeStats(
    val materialCount: Int = 0,
    val completedMaterials: Int = 0,
    val chunkCount: Int = 0,
    val conversationCount: Int = 0,
    val conceptCount: Int = 0
)

class HomeViewModel(
    private val materialRepository: MaterialRepository,
    private val chunkRepository: ChunkRepository,
    private val conversationRepository: ConversationRepository,
    private val conceptDao: ConceptDao,
    private val modelManager: ModelManager
) : ViewModel() {

    private val _stats = MutableStateFlow(HomeStats())
    val stats: StateFlow<HomeStats> = _stats.asStateFlow()

    private val _recentMaterials = MutableStateFlow<List<MaterialEntity>>(emptyList())
    val recentMaterials: StateFlow<List<MaterialEntity>> = _recentMaterials.asStateFlow()

    val inferenceModelState: StateFlow<ModelState> = modelManager.modelStates
        .map { states -> states[AppModelConfigs.GEMMA_3N_E2B_LITERT.id] ?: ModelState.NotDownloaded }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ModelState.NotDownloaded)

    val activeModelName: StateFlow<String?> = modelManager.activeInferenceModelId
        .map { id -> id?.let { AppModelConfigs.findById(it)?.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        refreshStats()
    }

    fun refreshStats() {
        viewModelScope.launch {
            _stats.value = HomeStats(
                materialCount = materialRepository.getCount(),
                completedMaterials = materialRepository.getCompletedCount(),
                chunkCount = chunkRepository.getTotalCount(),
                conversationCount = conversationRepository.getCount(),
                conceptCount = conceptDao.getDisplayableCount()
            )
            _recentMaterials.value = materialRepository.getRecent(5)
        }
    }
}
