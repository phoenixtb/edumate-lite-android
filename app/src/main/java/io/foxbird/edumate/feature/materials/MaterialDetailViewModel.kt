package io.foxbird.edumate.feature.materials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.foxbird.edumate.data.local.dao.ConceptDao
import io.foxbird.edumate.data.local.entity.ConceptEntity
import io.foxbird.edumate.data.local.entity.MaterialEntity
import io.foxbird.edumate.data.repository.ChunkRepository
import io.foxbird.edumate.data.repository.MaterialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MaterialDetailViewModel(
    private val materialId: Long,
    private val materialRepository: MaterialRepository,
    private val chunkRepository: ChunkRepository,
    private val conceptDao: ConceptDao
) : ViewModel() {

    private val _material = MutableStateFlow<MaterialEntity?>(null)
    val material: StateFlow<MaterialEntity?> = _material.asStateFlow()

    private val _chunkCount = MutableStateFlow(0)
    val chunkCount: StateFlow<Int> = _chunkCount.asStateFlow()

    private val _concepts = MutableStateFlow<List<ConceptEntity>>(emptyList())
    val concepts: StateFlow<List<ConceptEntity>> = _concepts.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _material.value = materialRepository.getById(materialId)
            _chunkCount.value = chunkRepository.getCountByMaterial(materialId)
            _concepts.value = conceptDao.getByMaterialId(materialId)
        }
    }

    fun deleteMaterial() {
        viewModelScope.launch {
            materialRepository.deleteById(materialId)
        }
    }
}
