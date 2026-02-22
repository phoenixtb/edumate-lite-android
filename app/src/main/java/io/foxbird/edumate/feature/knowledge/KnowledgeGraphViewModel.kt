package io.foxbird.edumate.feature.knowledge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.foxbird.edumate.data.local.dao.ConceptDao
import io.foxbird.edumate.data.local.entity.ConceptEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class KnowledgeGraphViewModel(
    conceptDao: ConceptDao
) : ViewModel() {

    val concepts: StateFlow<List<ConceptEntity>> = conceptDao.getAllDisplayableFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
