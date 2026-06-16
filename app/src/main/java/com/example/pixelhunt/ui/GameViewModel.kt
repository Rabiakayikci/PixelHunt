package com.example.pixelhunt.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pixelhunt.PixelHuntApplication
import com.example.pixelhunt.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: UserRepository = (application as PixelHuntApplication).repository

    val allLevels: StateFlow<List<LevelEntity>>
    val unlockedStickersCount: StateFlow<Int>
    val allStickers: StateFlow<List<StickerEntity>>

    init {

        allLevels = repository.allLevelsFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        unlockedStickersCount = repository.unlockedStickersCountFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
            
        allStickers = repository.getAllStickersFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Başlangıç verisi kontrolü + GameData ile eşitleme
        viewModelScope.launch {
            if (repository.getLevelById(1) == null) {
                repository.seedInitialData()
            } else {
                // Mevcut DB'yi güncel GameData içeriğiyle eşitle (eski nesneleri temizle, yenileri ekle)
                repository.syncStickersWithGameData()
            }
        }
    }

    fun getStickersForLevel(levelId: Int): Flow<List<StickerEntity>> {
        return repository.getStickersByLevelFlow(levelId)
    }

    suspend fun getLevel(levelId: Int): LevelEntity? {
        return repository.getLevelById(levelId)
    }
}
