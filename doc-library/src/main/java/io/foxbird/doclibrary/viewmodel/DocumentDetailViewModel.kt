package io.foxbird.doclibrary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.foxbird.doclibrary.data.local.dao.ConceptDao
import io.foxbird.doclibrary.data.local.entity.ConceptEntity
import io.foxbird.doclibrary.data.local.entity.DocumentEntity
import io.foxbird.doclibrary.data.repository.ChunkRepository
import io.foxbird.doclibrary.data.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DocumentDetailViewModel(
    private val documentId: Long,
    private val documentRepository: DocumentRepository,
    private val chunkRepository: ChunkRepository,
    private val conceptDao: ConceptDao
) : ViewModel() {

    private val _document = MutableStateFlow<DocumentEntity?>(null)
    val document: StateFlow<DocumentEntity?> = _document.asStateFlow()

    private val _chunkCount = MutableStateFlow(0)
    val chunkCount: StateFlow<Int> = _chunkCount.asStateFlow()

    private val _concepts = MutableStateFlow<List<ConceptEntity>>(emptyList())
    val concepts: StateFlow<List<ConceptEntity>> = _concepts.asStateFlow()

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            _document.value = documentRepository.getById(documentId)
            _chunkCount.value = chunkRepository.getCountByDocument(documentId)
            _concepts.value = conceptDao.getByDocumentId(documentId)
        }
    }

    fun deleteDocument() {
        viewModelScope.launch { documentRepository.deleteById(documentId) }
    }
}
