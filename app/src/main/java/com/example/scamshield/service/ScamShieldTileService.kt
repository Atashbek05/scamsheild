package com.example.scamshield.service

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.scamshield.R

class ScamShieldTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        syncTile()
    }

    override fun onClick() {
        super.onClick()
        if (isMonitoringRunning()) {
            MonitoringForegroundService.stop(applicationContext)
            applyState(active = false)
        } else {
            MonitoringForegroundService.start(applicationContext)
            applyState(active = true)
        }
    }

    private fun syncTile() = applyState(isMonitoringRunning())

    private fun applyState(active: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.icon = Icon.createWithResource(
            this,
            if (active) R.drawable.ic_tile_shield_on else R.drawable.ic_tile_shield_off,
        )
        tile.label = getString(
            if (active) R.string.tile_label_on else R.string.tile_label_off,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(
                if (active) R.string.tile_subtitle_on else R.string.tile_subtitle_off,
            )
        }
        tile.updateTile()
    }

    @Suppress("DEPRECATION")
    private fun isMonitoringRunning(): Boolean {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == MonitoringForegroundService::class.java.name }
    }

    companion object {
        fun requestUpdate(context: Context) {
            requestListeningState(
                context,
                ComponentName(context, ScamShieldTileService::class.java),
            )
        }
    }
}
