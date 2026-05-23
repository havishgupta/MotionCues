package com.example.motioncues

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle

class ToggleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (!MotionService.isRunning) {
            val serviceIntent = Intent(this, MotionService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        finish()
    }
}
