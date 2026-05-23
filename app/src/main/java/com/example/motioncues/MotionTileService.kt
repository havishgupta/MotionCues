package com.example.motioncues

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat

class MotionTileService : TileService() {

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
            stopService(Intent(this, MotionService::class.java))
        } else {
            val serviceIntent = Intent(this, MotionService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback: prompt user to open app
            }
        }
        
        // Brief delay to allow service to start/stop before updating tile UI
        android.os.Handler(mainLooper).postDelayed({
            updateTileState()
        }, 200)
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
