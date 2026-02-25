package io.foxbird.edumate.feature.worksheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.foxbird.edumate.data.repository.MaterialRepository
import io.foxbird.edumate.domain.service.WorksheetConfig
import io.foxbird.edumate.domain.service.WorksheetService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WorksheetUiState(
    val isGenerating: Boolean = false,
    val generatedContent: String? = null,
    val error: String? = null,
    val exportedPath: String? = null
)

class WorksheetViewModel(
    private val worksheetService: WorksheetService,
    private val materialRepository: MaterialRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorksheetUiState())
    val uiState: StateFlow<WorksheetUiState> = _uiState.asStateFlow()

    fun generate(title: String, numQuestions: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true, error = null)

            val materials = materialRepository.getByStatus("completed")
            if (materials.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    error = "No completed materials available"
                )
                return@launch
            }

            val config = WorksheetConfig(
                title = title,
                materialIds = materials.map { it.id },
                numQuestions = numQuestions
            )

            val content = worksheetService.generateWorksheet(config)
            _uiState.value = if (content != null) {
                _uiState.value.copy(isGenerating = false, generatedContent = content)
            } else {
                _uiState.value.copy(isGenerating = false, error = "Failed to generate worksheet")
            }
        }
    }

    fun exportPdf(title: String) {
        val content = _uiState.value.generatedContent ?: return
        viewModelScope.launch {
            val file = worksheetService.exportToPdf(content, title)
            _uiState.value = _uiState.value.copy(
                exportedPath = file?.absolutePath,
                error = if (file == null) "PDF export failed" else null
            )
        }
    }

    fun reset() {
        _uiState.value = WorksheetUiState()
    }
}
