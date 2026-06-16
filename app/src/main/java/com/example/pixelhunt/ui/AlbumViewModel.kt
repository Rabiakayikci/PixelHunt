package com.example.pixelhunt.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pixelhunt.PixelHuntApplication
import com.example.pixelhunt.data.*
import kotlinx.coroutines.flow.*

class AlbumViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: UserRepository = (application as PixelHuntApplication).repository

    val allStickers: StateFlow<List<StickerEntity>>
    val unlockedCount: StateFlow<Int>
    val isAlmostFull: StateFlow<Boolean>

    init {

        // Eagerly kullanarak veritabanı değişimlerini anında yakalıyoruz
        allStickers = repository.getAllStickersFlow()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        unlockedCount = repository.unlockedStickersCountFlow
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

        // Hayalet uyarıyı yok eden kesin mantık
        isAlmostFull = combine(allStickers, unlockedCount) { stickers, unlocked ->
            val total = stickers.size
            // KESİN ŞART: 
            // 1. Toplam çıkartma sayısı 0'dan büyük olmalı (Reset anı kontrolü)
            // 2. En az 1 çıkartma açılmış olmalı (0/0 durumunu engellemek için)
            // 3. Toplam sayı 5'ten büyük olmalı (Yeni başlayanlarda gösterme)
            // 4. Doluluk oranı %90 ve üzeri olmalı
            total > 5 && unlocked > 0 && unlocked >= (total * 0.9)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    }
}
