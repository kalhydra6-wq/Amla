package com.amala.app.ui

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.amala.app.audio.AudioController

/** Quick Settings tile that toggles the global equalizer with a clear active state. */
class AmlaTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        AudioController.toggleEnabled()
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val active = AudioController.enabled
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (active) "Amla EQ on" else "Amla EQ off"
        tile.contentDescription = "Toggle Amla equalizer"
        runCatching { tile.updateTile() }
    }

    companion object {
        fun requestUpdate(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                runCatching {
                    TileService.requestListeningState(context, ComponentName(context, AmlaTileService::class.java))
                }
            }
        }
    }
}
