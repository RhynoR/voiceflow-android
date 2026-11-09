package com.voiceflow.app.data.repository

import com.voiceflow.app.data.db.TranscriptionDao
import com.voiceflow.app.data.db.TranscriptionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptionRepository @Inject constructor(
    private val dao: TranscriptionDao,
) {
    fun getAllTranscriptions(): Flow<List<TranscriptionEntity>> = dao.getAll()

    suspend fun insert(text: String, wasInjected: Boolean) {
        dao.insert(
            TranscriptionEntity(
                text = text,
                timestampMs = System.currentTimeMillis(),
                wasInjected = wasInjected,
            )
        )
    }

    suspend fun deleteAll() = dao.deleteAll()

    suspend fun deleteById(id: Long) = dao.deleteById(id)
}
