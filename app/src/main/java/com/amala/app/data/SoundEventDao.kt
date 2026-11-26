package com.amala.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SoundEventDao {
    @Insert
    suspend fun insert(event: SoundEvent)

    @Query("SELECT * FROM sound_events ORDER BY timestamp DESC LIMIT 200")
    fun recent(): Flow<List<SoundEvent>>

    @Query("DELETE FROM sound_events WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM sound_events")
    suspend fun deleteAll()
}
