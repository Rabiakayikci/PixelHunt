package com.example.pixelhunt.data

import kotlinx.coroutines.flow.Flow
import java.io.File

class UserRepository(
    private val userDao: UserDao,
    private val stickerDao: StickerDao,
    private val levelDao: LevelDao
) {

    // User Operations
    val userFlow: Flow<UserEntity?> = userDao.getUserFlow()
    suspend fun getUser(): UserEntity? = userDao.getUser()
    suspend fun insertUser(user: UserEntity) = userDao.insertUser(user)
    suspend fun updateUser(user: UserEntity) = userDao.updateUser(user)

    // Level Operations
    val allLevelsFlow: Flow<List<LevelEntity>> = levelDao.getAllLevelsFlow()
    suspend fun getLevelById(levelId: Int): LevelEntity? = levelDao.getLevelById(levelId)
    suspend fun updateLevel(level: LevelEntity) = levelDao.updateLevel(level)
    suspend fun updateLevelUnlockStatus(levelId: Int, isUnlocked: Boolean) = levelDao.updateLevelUnlockStatus(levelId, isUnlocked)

    // Sticker Operations
    fun getStickersByLevelFlow(levelId: Int): Flow<List<StickerEntity>> = stickerDao.getStickersByLevelFlow(levelId)
    fun getUnlockedCountByLevelFlow(levelId: Int): Flow<Int> = stickerDao.getUnlockedCountByLevelFlow(levelId)
    val unlockedStickersCountFlow: Flow<Int> = stickerDao.getUnlockedStickersCountFlow()
    suspend fun getTotalCountByLevel(levelId: Int): Int = stickerDao.getTotalCountByLevel(levelId)
    fun getAllStickersFlow(): Flow<List<StickerEntity>> = stickerDao.getAllStickersFlow()
    
    suspend fun insertSticker(sticker: StickerEntity) = stickerDao.insertSticker(sticker)
    suspend fun updateSticker(sticker: StickerEntity) {
        stickerDao.updateSticker(sticker)
        checkAndUnlockNextLevel(sticker.levelId)
    }

    private suspend fun checkAndUnlockNextLevel(currentLevelId: Int) {
        val unlockedCount = stickerDao.getUnlockedCountByLevel(currentLevelId)
        val level = levelDao.getLevelById(currentLevelId)
        val requiredCount = level?.requiredStickerCount ?: stickerDao.getTotalCountByLevel(currentLevelId)

        if (unlockedCount >= requiredCount) {
            levelDao.updateLevelUnlockStatus(currentLevelId + 1, true)
        }
    }

    suspend fun getStickerById(id: Int): StickerEntity? = stickerDao.getStickerById(id)
    suspend fun getStickerByLevelAndName(levelId: Int, name: String): StickerEntity? =
        stickerDao.getStickerByLevelAndName(levelId, name)
    suspend fun getFirstLockedStickerByLevel(levelId: Int): StickerEntity? = stickerDao.getFirstLockedStickerByLevel(levelId)
    suspend fun getLockedStickersByLevel(levelId: Int): List<StickerEntity> = stickerDao.getLockedStickersByLevel(levelId)
    suspend fun getUnlockedCountForLevel(levelId: Int): Int = stickerDao.getUnlockedCountByLevel(levelId)
    fun getUnlockedStickersFlow(): Flow<List<StickerEntity>> = stickerDao.getUnlockedStickersFlow()

    suspend fun resetDatabase(filesDir: File? = null) {
        filesDir?.let { dir ->
            val stickers = stickerDao.getAllStickers()
            stickers.forEach { sticker ->
                if (sticker.image_path.isNotEmpty()) {
                    val file = File(sticker.image_path)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            }
            dir.listFiles { _, name -> name.startsWith("sticker_") && name.endsWith(".png") }
                ?.forEach { it.delete() }
        }

        userDao.deleteAllUsers()
        stickerDao.deleteAllStickers()
        levelDao.deleteAllLevels()
        
        insertUser(UserEntity(user_name = "Kaşif", total_score = 0))
        seedInitialData()
    }

    /**
     * Mevcut veritabanını GameData ile eşitler — uygulama her açıldığında çağrılır.
     * GameData'da artık olmayan çıkartmaları siler (görsel dosyası dahil),
     * yeni eklenenleri ekler. Toplanmış (isUnlocked) çıkartmalara dokunmaz,
     * böylece kullanıcının ilerlemesi korunur.
     */
    suspend fun syncStickersWithGameData() {
        val allStickers = stickerDao.getAllStickers()

        for (levelId in 1..5) {
            val level = levelDao.getLevelById(levelId) ?: continue
            // Theme'i seviyenin kendisinden türet — tema adı değişmediği sürece güvenli
            val expectedNames = GameData.levelThemes[level.levelTheme] ?: continue
            val levelStickers = allStickers.filter { it.levelId == levelId }

            // 1. GameData'da olmayan çıkartmaları sil (görsel dosyası dahil)
            levelStickers.filter { it.name !in expectedNames }.forEach { stale ->
                if (stale.image_path.isNotEmpty()) {
                    File(stale.image_path).takeIf { it.exists() }?.delete()
                }
                stickerDao.deleteSticker(stale)
            }

            // 2. Henüz DB'de olmayan yeni çıkartmaları ekle
            val existingNames = levelStickers.map { it.name }.toSet()
            expectedNames.filter { it !in existingNames }.forEach { name ->
                stickerDao.insertSticker(
                    StickerEntity(
                        image_path = "",
                        levelId = levelId,
                        isUnlocked = false,
                        points = 20,
                        name = name
                    )
                )
            }

            // 3. requiredStickerCount'u havuz boyutuna eşitle (eski DB'ler için kritik)
            if (level.requiredStickerCount != expectedNames.size) {
                levelDao.updateLevel(level.copy(requiredStickerCount = expectedNames.size))
            }
        }
    }

    suspend fun seedInitialData() {
        // requiredStickerCount her seviyede havuz boyutuna eşit (havuzun tümü toplanınca seviye biter)
        val themes = listOf(
            1 to "Oyuncak Dünyası",
            2 to "Mutfak Macerası",
            3 to "Okul Eşyaları",
            4 to "Bahçe Keşfi",
            5 to "Oturma Odası"
        )
        val levels = themes.map { (id, theme) ->
            val poolSize = GameData.levelThemes[theme]?.size ?: 5
            LevelEntity(
                levelId = id,
                levelName = "Seviye $id",
                levelTheme = theme,
                isUnlocked = id == 1,
                requiredStickerCount = poolSize
            )
        }
        levels.forEach { levelDao.insertLevel(it) }

        levels.forEach { level ->
            val names = GameData.levelThemes[level.levelTheme] ?: emptyList()
            names.forEach { name ->
                stickerDao.insertSticker(
                    StickerEntity(
                        image_path = "",
                        levelId = level.levelId,
                        isUnlocked = false,
                        points = 20,
                        name = name
                    )
                )
            }
        }
    }
}
