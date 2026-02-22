package io.foxbird.edumate.feature.materials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.foxbird.edumate.data.local.dao.ChunkDao
import io.foxbird.edumate.data.local.dao.ConceptDao
import io.foxbird.edumate.data.local.dao.MaterialDao
import io.foxbird.edumate.data.local.entity.ConceptEntity
import io.foxbird.edumate.data.local.entity.MaterialEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MaterialDetailViewModel(
    private val materialId: Long,
    private val materialDao: MaterialDao,
    private val chunkDao: ChunkDao,
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
            _material.value = materialDao.getById(materialId)
            _chunkCount.value = chunkDao.getCountByMaterial(materialId)
            _concepts.value = conceptDao.getByMaterialId(materialId)
        }
    }

    fun deleteMaterial() {
        viewModelScope.launch {
            materialDao.deleteById(materialId)
        }
    }
}
