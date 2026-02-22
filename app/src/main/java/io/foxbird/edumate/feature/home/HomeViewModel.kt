package io.foxbird.edumate.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.foxbird.edumate.data.local.dao.ChunkDao
import io.foxbird.edumate.data.local.dao.ConceptDao
import io.foxbird.edumate.data.local.dao.ConversationDao
import io.foxbird.edumate.data.local.dao.MaterialDao
import io.foxbird.edumate.data.local.entity.MaterialEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeStats(
    val materialCount: Int = 0,
    val completedMaterials: Int = 0,
    val chunkCount: Int = 0,
    val conversationCount: Int = 0,
    val conceptCount: Int = 0
)

class HomeViewModel(
    private val materialDao: MaterialDao,
    private val chunkDao: ChunkDao,
    private val conversationDao: ConversationDao,
    private val conceptDao: ConceptDao
) : ViewModel() {

    private val _stats = MutableStateFlow(HomeStats())
    val stats: StateFlow<HomeStats> = _stats.asStateFlow()

    private val _recentMaterials = MutableStateFlow<List<MaterialEntity>>(emptyList())
    val recentMaterials: StateFlow<List<MaterialEntity>> = _recentMaterials.asStateFlow()

    init {
        refreshStats()
    }

    fun refreshStats() {
        viewModelScope.launch {
            _stats.value = HomeStats(
                materialCount = materialDao.getCount(),
                completedMaterials = materialDao.getCompletedCount(),
                chunkCount = chunkDao.getTotalCount(),
                conversationCount = conversationDao.getCount(),
                conceptCount = conceptDao.getDisplayableCount()
            )
            _recentMaterials.value = materialDao.getRecent(5)
        }
    }
}
