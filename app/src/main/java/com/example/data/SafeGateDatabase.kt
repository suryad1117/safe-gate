package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AppEntity::class,
        ScheduleEntity::class,
        ScheduleAppEntity::class,
        VaultItemEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class SafeGateDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun vaultDao(): VaultDao

    companion object {
        @Volatile
        private var INSTANCE: SafeGateDatabase? = null

        fun getDatabase(context: Context): SafeGateDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SafeGateDatabase::class.java,
                    "safe_gate_database"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
