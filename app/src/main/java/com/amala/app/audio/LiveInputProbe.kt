package com.amala.app.audio

import android.media.audiofx.Visualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Reads real properties of the audio output mix using Android's Visualizer. */
object LiveInputProbe {

    data class Info(
        val sampleRateHz: Int? = null,
        val peakMdB: Int? = null,
        val available: Boolean = false,
    )

    private val _state = MutableStateFlow(Info())
    val state: StateFlow<Info> = _state.asStateFlow()

    private var visualizer: Visualizer? = null

    fun start(): Boolean {
        stop()
        val effect = runCatching { Visualizer(0) }.getOrNull() ?: return false
        visualizer = effect
        runCatching { effect.setMeasurementMode(Visualizer.MEASUREMENT_MODE_PEAK_RMS) }
        runCatching { effect.setEnabled(true) }
        refresh()
        return true
    }

    fun refresh() {
        val effect = visualizer ?: return
        val rateHz = runCatching { effect.samplingRate }
            .getOrNull()
            ?.takeIf { it > 0 }
            ?.let { it / 1_000_000 } // Visualizer reports millihertz.
        val measurement = Visualizer.MeasurementPeakRms()
        val status = runCatching { effect.getMeasurementPeakRms(measurement) }.getOrDefault(-1)
        val peak = if (status == Visualizer.SUCCESS && measurement.peak >= -9_600) measurement.peak else null
        _state.value = Info(sampleRateHz = rateHz, peakMdB = peak, available = true)
    }

    fun stop() {
        runCatching { visualizer?.setEnabled(false) }
        runCatching { visualizer?.release() }
        visualizer = null
        _state.value = Info()
    }
}
