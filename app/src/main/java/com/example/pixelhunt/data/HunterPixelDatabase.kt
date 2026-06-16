package com.example.pixelhunt.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE users ADD COLUMN profile_photo_path TEXT")
    }
}

@Database(
    entities = [
        UserEntity::class,
        MissionEntity::class,
        StickerEntity::class,
        LevelEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class HunterPixelDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun missionDao(): MissionDao
    abstract fun stickerDao(): StickerDao
    abstract fun levelDao(): LevelDao

    companion object {
        @Volatile
        private var INSTANCE: HunterPixelDatabase? = null

        fun getDatabase(context: Context): HunterPixelDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HunterPixelDatabase::class.java,
                    "hunter_pixel_database"
                )
                .addMigrations(MIGRATION_3_4)
                .fallbackToDestructiveMigrationFrom(1, 2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
