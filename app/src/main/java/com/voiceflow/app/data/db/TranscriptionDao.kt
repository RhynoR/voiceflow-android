package com.voiceflow.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TranscriptionEntity)

    @Query("SELECT * FROM transcriptions ORDER BY timestampMs DESC")
    fun getAll(): Flow<List<TranscriptionEntity>>

    @Query("DELETE FROM transcriptions")
    suspend fun deleteAll()

    @Query("DELETE FROM transcriptions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
