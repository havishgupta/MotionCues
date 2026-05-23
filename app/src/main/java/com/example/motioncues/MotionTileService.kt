package com.example.motioncues

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat

class MotionTileService : TileService() {

    companion object {
        fun requestTileStateUpdate(context: Context) {
            try {
                TileService.requestListeningState(
                    context,
                    ComponentName(context, MotionTileService::class.java)
                )
            } catch (e: Exception) {}
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }
    
    private fun hasRequiredPermissions(): Boolean {
        var hasPerms = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPerms = hasPerms && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }
        hasPerms = hasPerms && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return hasPerms
    }

    override fun onClick() {
        super.onClick()
        
        if (!Settings.canDrawOverlays(this) || !hasRequiredPermissions()) {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(
                    android.app.PendingIntent.getActivity(
                        this, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
            return
        }

        if (MotionService.isRunning) {
            val serviceIntent = Intent(this, MotionService::class.java)
            stopService(serviceIntent)
            
            // Optimistically update tile state
            val tile = qsTile
            if (tile != null) {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "Cues Off"
                tile.updateTile()
            }
        } else {
            val serviceIntent = Intent(this, MotionService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
                
                // Optimistically update tile state
                val tile = qsTile
                if (tile != null) {
                    tile.state = Tile.STATE_ACTIVE
                    tile.label = "Cues On"
                    tile.updateTile()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback: prompt user to open app if background start failed
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startActivityAndCollapse(
                        android.app.PendingIntent.getActivity(
                            this, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    startActivityAndCollapse(intent)
                }
            }
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        
        if (MotionService.isRunning) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "Cues On"
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "Cues Off"
        }
        tile.updateTile()
    }
}
