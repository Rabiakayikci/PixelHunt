package com.example.pixelhunt.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val user_name: String,
    val total_score: Int = 0,
    val selected_avatar_sticker_id: Int = -1,
    val profile_photo_path: String? = null
)

@Entity(tableName = "missions")
data class MissionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val target_object: String,
    val clue_text: String
)

@Entity(tableName = "stickers")
data class StickerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val image_path: String,
    val levelId: Int,
    val isUnlocked: Boolean = false,
    val points: Int = 0,
    val name: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "levels")
data class LevelEntity(
    @PrimaryKey val levelId: Int,
    val levelName: String,
    val levelTheme: String,
    val isUnlocked: Boolean = false,
    val requiredStickerCount: Int = 5
)
