package io.foxbird.edumate.feature.materials

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.foxbird.edumate.data.local.dao.MaterialDao
import io.foxbird.edumate.data.local.entity.MaterialEntity
import io.foxbird.edumate.domain.service.MaterialProcessor
import io.foxbird.edumate.domain.service.ProcessingEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MaterialsUiState(
    val processingStage: String? = null,
    val processingProgress: Pair<Int, Int>? = null,
    val processingMaterialName: String? = null,
    val error: String? = null,
    val showAddSheet: Boolean = false
)

class MaterialsViewModel(
    private val materialDao: MaterialDao,
    private val materialProcessor: MaterialProcessor
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val materials: StateFlow<List<MaterialEntity>> = refreshTrigger
        .flatMapLatest { materialDao.getAllFlow() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(MaterialsUiState())
    val uiState: StateFlow<MaterialsUiState> = _uiState.asStateFlow()

    fun refresh() {
        refreshTrigger.value++
    }

    fun showAddSheet() {
        _uiState.value = _uiState.value.copy(showAddSheet = true)
    }

    fun hideAddSheet() {
        _uiState.value = _uiState.value.copy(showAddSheet = false)
    }

    fun addPdf(uri: Uri, title: String, subject: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                showAddSheet = false, error = null, processingMaterialName = title
            )
            materialProcessor.processPdf(uri, title, subject).collect { event ->
                handleProcessingEvent(event)
            }
        }
    }

    fun addText(text: String, title: String, subject: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                showAddSheet = false, error = null, processingMaterialName = title
            )
            materialProcessor.processText(text, title, subject).collect { event ->
                handleProcessingEvent(event)
            }
        }
    }

    fun deleteMaterial(materialId: Long) {
        viewModelScope.launch {
            materialDao.deleteById(materialId)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun handleProcessingEvent(event: ProcessingEvent) {
        when (event) {
            is ProcessingEvent.Progress -> {
                _uiState.value = _uiState.value.copy(
                    processingStage = event.stage,
                    processingProgress = event.current to event.total
                )
            }
            is ProcessingEvent.Complete -> {
                _uiState.value = _uiState.value.copy(
                    processingStage = null,
                    processingProgress = null,
                    processingMaterialName = null
                )
            }
            is ProcessingEvent.Error -> {
                _uiState.value = _uiState.value.copy(
                    processingStage = null,
                    processingProgress = null,
                    processingMaterialName = null,
                    error = event.message
                )
            }
        }
    }
}
