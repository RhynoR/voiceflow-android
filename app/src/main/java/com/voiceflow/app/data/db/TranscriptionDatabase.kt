package com.voiceflow.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TranscriptionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class TranscriptionDatabase : RoomDatabase() {
    abstract fun transcriptionDao(): TranscriptionDao
}
