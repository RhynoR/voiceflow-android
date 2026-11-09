package com.voiceflow.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.voiceflow.app.data.db.TranscriptionDao
import com.voiceflow.app.data.db.TranscriptionDatabase
import com.voiceflow.app.data.db.TranscriptionEntity
import com.voiceflow.app.data.repository.TranscriptionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for TranscriptionRepository using an in-memory Room database.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TranscriptionRepositoryTest {

    private lateinit var db: TranscriptionDatabase
    private lateinit var dao: TranscriptionDao
    private lateinit var repository: TranscriptionRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TranscriptionDatabase::class.java
        ).allowMainThreadQueries().build()

        dao = db.transcriptionDao()
        repository = TranscriptionRepository(dao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── Insert + retrieve ────────────────────────────────────────────────────

    @Test
    fun `inserting a transcription makes it appear in getAllTranscriptions`() = runTest {
        repository.insert("hello world", wasInjected = true)

        val all = repository.getAllTranscriptions().first()
        assertEquals(1, all.size)
        assertEquals("hello world", all[0].text)
        assertTrue(all[0].wasInjected)
    }

    @Test
    fun `multiple inserts are ordered newest first`() = runTest {
        repository.insert("first", wasInjected = false)
        repository.insert("second", wasInjected = true)
        repository.insert("third", wasInjected = false)

        val all = repository.getAllTranscriptions().first()
        assertEquals(3, all.size)
        assertEquals("third", all[0].text)
        assertEquals("first", all[2].text)
    }

    @Test
    fun `empty database returns empty list`() = runTest {
        val all = repository.getAllTranscriptions().first()
        assertTrue(all.isEmpty())
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Test
    fun `deleteAll clears all transcriptions`() = runTest {
        repository.insert("a", wasInjected = false)
        repository.insert("b", wasInjected = true)

        repository.deleteAll()

        val all = repository.getAllTranscriptions().first()
        assertTrue(all.isEmpty())
    }

    @Test
    fun `deleteById removes only the targeted row`() = runTest {
        repository.insert("keep", wasInjected = true)
        repository.insert("remove", wasInjected = false)

        val all = repository.getAllTranscriptions().first()
        val removeId = all.first { it.text == "remove" }.id

        repository.deleteById(removeId)

        val remaining = repository.getAllTranscriptions().first()
        assertEquals(1, remaining.size)
        assertEquals("keep", remaining[0].text)
    }

    // ── Edge cases ───────────────────────────────────────────────────────────

    @Test
    fun `inserting empty string is stored as-is`() = runTest {
        repository.insert("", wasInjected = false)
        val all = repository.getAllTranscriptions().first()
        assertEquals("", all[0].text)
    }

    @Test
    fun `timestamp is recorded within 1 second of insertion`() = runTest {
        val before = System.currentTimeMillis()
        repository.insert("ts test", wasInjected = true)
        val after = System.currentTimeMillis()

        val entity = repository.getAllTranscriptions().first()[0]
        assertTrue(entity.timestampMs in before..after)
    }
}
