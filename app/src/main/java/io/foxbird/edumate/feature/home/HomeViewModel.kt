package io.foxbird.edumate.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.foxbird.doclibrary.data.local.entity.DocumentEntity
import io.foxbird.doclibrary.data.repository.ChunkRepository
import io.foxbird.doclibrary.data.repository.DocumentRepository
import io.foxbird.doclibrary.data.local.dao.ConceptDao
import io.foxbird.doclibrary.domain.processor.IDocumentProcessor
import io.foxbird.doclibrary.domain.processor.ProcessingState
import io.foxbird.edgeai.engine.ModelManager
import io.foxbird.edgeai.model.ModelState
import io.foxbird.edumate.core.model.AppModelConfigs
import io.foxbird.edumate.data.repository.ConversationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeStats(
    val documentCount: Int = 0,
    val completedDocuments: Int = 0,
    val chunkCount: Int = 0,
    val conversationCount: Int = 0,
    val conceptCount: Int = 0
)

class HomeViewModel(
    private val documentRepository: DocumentRepository,
    private val chunkRepository: ChunkRepository,
    private val conversationRepository: ConversationRepository,
    private val conceptDao: ConceptDao,
    private val modelManager: ModelManager,
    private val documentProcessor: IDocumentProcessor
) : ViewModel() {

    private val _stats = MutableStateFlow(HomeStats())
    val stats: StateFlow<HomeStats> = _stats.asStateFlow()

    private val _recentDocuments = MutableStateFlow<List<DocumentEntity>>(emptyList())
    val recentDocuments: StateFlow<List<DocumentEntity>> = _recentDocuments.asStateFlow()

    /** Single source of truth — same StateFlow as LibraryViewModel reads. */
    val processingState: StateFlow<ProcessingState?> = documentProcessor.processingState

    val inferenceModelState: StateFlow<ModelState> = modelManager.modelStates
        .map { states -> states[AppModelConfigs.GEMMA_3N_E2B_LITERT.id] ?: ModelState.NotDownloaded }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ModelState.NotDownloaded)

    val activeModelName: StateFlow<String?> = modelManager.activeInferenceModelId
        .map { id -> id?.let { AppModelConfigs.findById(it)?.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init { refreshStats() }

    fun refreshStats() {
        viewModelScope.launch {
            _stats.value = HomeStats(
                documentCount = documentRepository.getCount(),
                completedDocuments = documentRepository.getCompletedCount(),
                chunkCount = chunkRepository.getTotalCount(),
                conversationCount = conversationRepository.getCount(),
                conceptCount = conceptDao.getDisplayableCount()
            )
            _recentDocuments.value = documentRepository.getRecent(5)
        }
    }
}
