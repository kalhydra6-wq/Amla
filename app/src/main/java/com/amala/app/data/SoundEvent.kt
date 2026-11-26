package com.amala.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One user-made EQ change, stored locally. No audio or song titles are stored here. */
@Entity(tableName = "sound_events")
data class SoundEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val deviceType: String,
    val deviceId: String,
    val appPackage: String?,
    val hour: Int,
    val weekday: Int,
    val changeKind: String,
    val bandIndex: Int?,
    val effectName: String?,
    val oldValue: Float,
    val newValue: Float,
    val presetName: String?,
)
