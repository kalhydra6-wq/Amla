package com.amala.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** Local-only access to the sound event log. No network, no audio, no analytics. */
object SoundEventRepository {

    private lateinit var dao: SoundEventDao

    fun initialize(context: Context) {
        dao = AmlaDatabase.get(context).soundEventDao()
    }

    fun recent(): Flow<List<SoundEvent>> = dao.recent()

    suspend fun insert(event: SoundEvent) = withContext(Dispatchers.IO) { dao.insert(event) }

    suspend fun deleteOlderThan(before: Long) = withContext(Dispatchers.IO) {
        dao.deleteOlderThan(before)
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) { dao.deleteAll() }
}

/** Pure counting helpers for the transparent "what Amla remembers" view. */
object SoundEventSummary {
    fun countByDevice(events: List<SoundEvent>): Map<String, Int> =
        events.groupingBy { it.deviceType }.eachCount()

    fun countByApp(events: List<SoundEvent>): Map<String, Int> =
        events.mapNotNull { it.appPackage }.groupingBy { it }.eachCount()

    fun countByHour(events: List<SoundEvent>): Map<Int, Int> =
        events.groupingBy { it.hour }.eachCount()
}
