package com.example.pixelhunt.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM users LIMIT 1")
    fun getUserFlow(): Flow<UserEntity?>

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getUser(): UserEntity?

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}

@Dao
interface MissionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMission(mission: MissionEntity)

    @Update
    suspend fun updateMission(mission: MissionEntity)

    @Query("SELECT * FROM missions")
    suspend fun getAllMissions(): List<MissionEntity>
}

@Dao
interface StickerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSticker(sticker: StickerEntity)

    @Update
    suspend fun updateSticker(sticker: StickerEntity)

    @Delete
    suspend fun deleteSticker(sticker: StickerEntity)

    @Query("SELECT * FROM stickers ORDER BY id ASC")
    fun getAllStickersFlow(): Flow<List<StickerEntity>>

    @Query("SELECT * FROM stickers WHERE levelId = :levelId ORDER BY id ASC")
    fun getStickersByLevelFlow(levelId: Int): Flow<List<StickerEntity>>
    
    @Query("SELECT COUNT(*) FROM stickers WHERE isUnlocked = 1 AND levelId = :levelId")
    fun getUnlockedCountByLevelFlow(levelId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM stickers WHERE isUnlocked = 1 AND levelId = :levelId")
    suspend fun getUnlockedCountByLevel(levelId: Int): Int

    @Query("SELECT COUNT(*) FROM stickers WHERE isUnlocked = 1")
    fun getUnlockedStickersCountFlow(): Flow<Int>

    @Query("SELECT * FROM stickers WHERE isUnlocked = 1 ORDER BY id ASC")
    fun getUnlockedStickersFlow(): Flow<List<StickerEntity>>

    @Query("SELECT COUNT(*) FROM stickers WHERE levelId = :levelId")
    suspend fun getTotalCountByLevel(levelId: Int): Int

    @Query("SELECT * FROM stickers WHERE id = :id")
    suspend fun getStickerById(id: Int): StickerEntity?

    @Query("SELECT * FROM stickers WHERE levelId = :levelId AND name = :name LIMIT 1")
    suspend fun getStickerByLevelAndName(levelId: Int, name: String): StickerEntity?

    @Query("SELECT * FROM stickers WHERE isUnlocked = 0 AND levelId = :levelId ORDER BY id ASC LIMIT 1")
    suspend fun getFirstLockedStickerByLevel(levelId: Int): StickerEntity?

    @Query("SELECT * FROM stickers WHERE isUnlocked = 0 AND levelId = :levelId ORDER BY id ASC")
    suspend fun getLockedStickersByLevel(levelId: Int): List<StickerEntity>

    @Query("DELETE FROM stickers")
    suspend fun deleteAllStickers()

    @Query("SELECT * FROM stickers")
    suspend fun getAllStickers(): List<StickerEntity>
}

@Dao
interface LevelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLevel(level: LevelEntity)

    @Update
    suspend fun updateLevel(level: LevelEntity)

    @Query("SELECT * FROM levels ORDER BY levelId ASC")
    fun getAllLevelsFlow(): Flow<List<LevelEntity>>

    @Query("SELECT * FROM levels WHERE levelId = :levelId")
    suspend fun getLevelById(levelId: Int): LevelEntity?

    @Query("UPDATE levels SET isUnlocked = :isUnlocked WHERE levelId = :levelId")
    suspend fun updateLevelUnlockStatus(levelId: Int, isUnlocked: Boolean)

    @Query("DELETE FROM levels")
    suspend fun deleteAllLevels()
}
