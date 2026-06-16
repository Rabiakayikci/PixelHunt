package com.example.pixelhunt.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pixelhunt.PixelHuntApplication
import com.example.pixelhunt.data.StickerEntity
import com.example.pixelhunt.data.UserEntity
import com.example.pixelhunt.data.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: UserRepository = (application as PixelHuntApplication).repository

    val userState: StateFlow<UserEntity?> = repository.userFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val unlockedStickersCount: StateFlow<Int> = repository.unlockedStickersCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val unlockedStickers: StateFlow<List<StickerEntity>> = repository.getUnlockedStickersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            if (repository.getUser() == null) {
                repository.insertUser(UserEntity(user_name = "Kaşif", total_score = 0))
            }
        }
    }

    fun updateUser(newName: String) {
        viewModelScope.launch {
            val currentUser = repository.getUser() ?: return@launch
            repository.updateUser(currentUser.copy(user_name = newName))
        }
    }

    fun updateAvatarSticker(stickerId: Int) {
        viewModelScope.launch {
            val currentUser = repository.getUser() ?: return@launch
            repository.updateUser(
                currentUser.copy(
                    selected_avatar_sticker_id = stickerId,
                    profile_photo_path = null
                )
            )
        }
    }

    fun updateProfilePhoto(path: String) {
        viewModelScope.launch {
            val currentUser = repository.getUser() ?: return@launch
            repository.updateUser(
                currentUser.copy(
                    profile_photo_path = path,
                    selected_avatar_sticker_id = -1
                )
            )
        }
    }

    fun resetDatabase() {
        viewModelScope.launch {
            repository.resetDatabase(getApplication<Application>().filesDir)
        }
    }
}
