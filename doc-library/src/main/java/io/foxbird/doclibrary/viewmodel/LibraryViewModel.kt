package io.foxbird.doclibrary.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.foxbird.doclibrary.data.local.entity.DocumentEntity
import io.foxbird.doclibrary.data.repository.DocumentRepository
import io.foxbird.doclibrary.domain.processor.IDocumentProcessor
import io.foxbird.doclibrary.domain.processor.ProcessingEvent
import io.foxbird.doclibrary.domain.processor.ProcessingMode
import io.foxbird.doclibrary.domain.processor.ProcessingState
import io.foxbird.doclibrary.domain.task.AiTask
import io.foxbird.doclibrary.domain.task.TaskQueue
import io.foxbird.doclibrary.domain.task.TaskType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ---------- Add flow state machine ----------

enum class SourceType { PDF, GALLERY, CAMERA }

sealed class AddFlow {
    object Idle : AddFlow()

    data class SourcePicked(
        val type: SourceType,
        val uri: Uri? = null,
        val uris: List<Uri> = emptyList(),
        val suggestedTitle: String = ""
    ) : AddFlow()

    data class Details(
        val type: SourceType,
        val uri: Uri?,
        val uris: List<Uri>,
        val title: String,
        val subject: String?,
        val gradeLevel: Int?
    ) : AddFlow()

    data class ModeSelection(
        val type: SourceType,
        val uri: Uri?,
        val uris: List<Uri>,
        val title: String,
        val subject: String?,
        val gradeLevel: Int?
    ) : AddFlow()
}

// ---------- UI state ----------

data class LibraryUiState(
    val addFlow: AddFlow = AddFlow.Idle,
    val error: String? = null
)

class LibraryViewModel(
    private val documentRepository: DocumentRepository,
    private val documentProcessor: IDocumentProcessor,
    private val taskQueue: TaskQueue,
    private val appScope: CoroutineScope
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val documents: StateFlow<List<DocumentEntity>> = refreshTrigger
        .flatMapLatest { documentRepository.getAllFlow() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    val processingState: StateFlow<ProcessingState?> = documentProcessor.processingState

    /** Exposes task queue state for progress indicators. */
    val tasks = taskQueue.tasks

    private val _showAddSheet = MutableStateFlow(false)
    val showAddSheet: StateFlow<Boolean> = _showAddSheet.asStateFlow()

    fun refresh() { refreshTrigger.value++ }

    // ---------- Add sheet ----------

    fun openAddSheet() { _showAddSheet.value = true }
    fun closeAddSheet() { _showAddSheet.value = false }

    // ---------- Add flow steps ----------

    fun onSourcePicked(
        type: SourceType,
        uri: Uri? = null,
        uris: List<Uri> = emptyList(),
        suggestedTitle: String? = null
    ) {
        _showAddSheet.value = false
        val title = suggestedTitle?.takeIf { it.isNotBlank() } ?: when {
            uris.isNotEmpty() -> "Scanned Document"
            else -> "Untitled"
        }
        _uiState.value = _uiState.value.copy(
            addFlow = AddFlow.SourcePicked(type, uri, uris, title)
        )
    }

    fun confirmDetails(title: String, subject: String?, gradeLevel: Int?) {
        val current = _uiState.value.addFlow as? AddFlow.SourcePicked ?: return
        _uiState.value = _uiState.value.copy(
            addFlow = AddFlow.Details(current.type, current.uri, current.uris, title, subject, gradeLevel)
        )
    }

    /** Enqueues processing through [TaskQueue] so requests are serialized. */
    fun confirmProcessing(mode: ProcessingMode) {
        val current = _uiState.value.addFlow as? AddFlow.Details ?: return
        _uiState.value = _uiState.value.copy(addFlow = AddFlow.Idle, error = null)

        val taskId = "doc_${System.currentTimeMillis()}"
        val taskTitle = "Processing: ${current.title}"

        taskQueue.enqueue(
            AiTask(
                id = taskId,
                type = TaskType.DOCUMENT_PROCESSING,
                title = taskTitle
            ) { _ ->
                val flow = when (current.type) {
                    SourceType.PDF -> documentProcessor.processPdf(
                        uri = current.uri!!,
                        title = current.title,
                        subject = current.subject,
                        gradeLevel = current.gradeLevel,
                        mode = mode
                    )
                    SourceType.GALLERY, SourceType.CAMERA -> documentProcessor.processImages(
                        uris = if (current.uri != null) listOf(current.uri) else current.uris,
                        title = current.title,
                        subject = current.subject,
                        gradeLevel = current.gradeLevel,
                        mode = mode
                    )
                }
                flow.collect { event -> handleProcessingEvent(event) }
            }
        )
    }

    fun cancelAddFlow() {
        _uiState.value = _uiState.value.copy(addFlow = AddFlow.Idle)
    }

    // ---------- Direct add (text path) ----------

    fun addText(text: String, title: String, subject: String? = null) {
        _uiState.value = _uiState.value.copy(error = null)
        taskQueue.enqueue(
            AiTask(
                id = "text_${System.currentTimeMillis()}",
                type = TaskType.DOCUMENT_PROCESSING,
                title = "Processing: $title"
            ) { _ ->
                documentProcessor.processText(text, title, subject).collect { event ->
                    handleProcessingEvent(event)
                }
            }
        )
    }

    // ---------- Edit & Delete ----------

    fun deleteDocument(documentId: Long) {
        appScope.launch { documentRepository.deleteById(documentId) }
    }

    fun updateDocument(id: Long, title: String, subject: String?, gradeLevel: Int?) {
        appScope.launch { documentRepository.updateMetadata(id, title, subject, gradeLevel) }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }

    // ---------- Internal ----------

    private fun handleProcessingEvent(event: ProcessingEvent) {
        when (event) {
            is ProcessingEvent.Progress -> Unit
            is ProcessingEvent.Complete -> refresh()
            is ProcessingEvent.Error -> _uiState.value = _uiState.value.copy(error = event.message)
            is ProcessingEvent.Duplicate -> _uiState.value = _uiState.value.copy(
                error = "\"${event.existingTitle}\" is already in your library."
            )
        }
    }
}
