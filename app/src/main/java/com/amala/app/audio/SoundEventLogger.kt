package com.amala.app.audio

import com.amala.app.data.SoundEvent
import com.amala.app.data.SoundEventRepository
import com.amala.app.model.EqualizerProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

data class LogContext(
    val deviceType: String,
    val deviceId: String,
    val appPackage: String?,
    val hour: Int,
    val weekday: Int,
)

/** Debounced logger: only the final value after ~3 s of no slider movement is saved. */
object SoundEventLogger {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pending: Job? = null

    fun schedule(enabled: Boolean, context: LogContext, old: EqualizerProfile, new: EqualizerProfile) {
        if (!enabled) return
        val change = detectChange(old, new) ?: return
        pending?.cancel()
        pending = scope.launch {
            delay(DEBOUNCE_MS)
            SoundEventRepository.insert(
                SoundEvent(
                    timestamp = System.currentTimeMillis(),
                    deviceType = context.deviceType,
                    deviceId = context.deviceId,
                    appPackage = context.appPackage,
                    hour = context.hour,
                    weekday = context.weekday,
                    changeKind = change.kind,
                    bandIndex = change.bandIndex,
                    effectName = change.effectName,
                    oldValue = change.oldValue,
                    newValue = change.newValue,
                    presetName = new.preset.name,
                ),
            )
        }
    }

    fun cancel() {
        pending?.cancel()
        pending = null
    }

    private data class Change(
        val kind: String,
        val bandIndex: Int?,
        val effectName: String?,
        val oldValue: Float,
        val newValue: Float,
    )

    private fun detectChange(old: EqualizerProfile, new: EqualizerProfile): Change? {
        for (index in old.bands.indices) {
            val before = old.bands.getOrNull(index) ?: 0
            val after = new.bands.getOrNull(index) ?: 0
            if (before != after) {
                return Change("band", index, null, before / 100f, after / 100f)
            }
        }
        listOf(
            Triple("bass", old.bassBoost, new.bassBoost),
            Triple("virtualizer", old.virtualizer, new.virtualizer),
            Triple("loudness", old.loudness, new.loudness),
            Triple("reverb", old.reverb, new.reverb),
            Triple("preamp", old.preamp, new.preamp),
        ).forEach { (name, before, after) ->
            if (before != after) return Change("effect", null, name, before / 100f, after / 100f)
        }
        if (old.limiterEnabled != new.limiterEnabled) {
            return Change("effect", null, "limiter", 0f, if (new.limiterEnabled) 1f else 0f)
        }
        return null
    }

    fun contextOf(deviceType: String?, deviceId: String?, appPackage: String?): LogContext {
        val calendar = Calendar.getInstance()
        return LogContext(
            deviceType = deviceType ?: "unknown",
            deviceId = deviceId ?: "unknown",
            appPackage = appPackage,
            hour = calendar.get(Calendar.HOUR_OF_DAY),
            weekday = calendar.get(Calendar.DAY_OF_WEEK),
        )
    }

    private const val DEBOUNCE_MS = 3_000L
}
