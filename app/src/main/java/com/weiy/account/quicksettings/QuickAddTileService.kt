package com.weiy.account.quicksettings

import android.app.PendingIntent
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.weiy.account.MainActivity
import com.weiy.account.R

class QuickAddTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.let { tile ->
            tile.state = Tile.STATE_ACTIVE
            tile.label = getString(R.string.quick_settings_tile_label)
            tile.subtitle = getString(R.string.quick_settings_tile_subtitle)
            tile.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_TRANSACTION_EDIT, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            REQUEST_CODE_OPEN_TRANSACTION_EDIT_FROM_TILE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        startActivityAndCollapse(pendingIntent)
    }

    companion object {
        private const val REQUEST_CODE_OPEN_TRANSACTION_EDIT_FROM_TILE = 2011
    }
}
