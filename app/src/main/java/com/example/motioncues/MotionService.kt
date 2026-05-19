package com.example.motioncues

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class MotionService : Service(), SensorEventListener {

    companion object {
        var isRunning = false
            private set
    }

    private lateinit var windowManager: WindowManager
    private var dotsOverlayView: DotsOverlayView? = null
    
    private lateinit var sensorManager: SensorManager
    private var accelSensor: Sensor? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "MotionCuesChannel")
            .setContentTitle("Motion Cues Active")
            .setContentText("Displaying motion alignment dots")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        
        if (Build.VERSION.SDK_INT >= 34) { // UPSIDE_DOWN_CAKE
            startForeground(1, notification, 1073741824) // FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            startForeground(1, notification)
        }

        setupOverlay()
        
        accelSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    private fun setupOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        dotsOverlayView = DotsOverlayView(this)

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        layoutParams.gravity = Gravity.TOP or Gravity.START
        windowManager.addView(dotsOverlayView, layoutParams)
        
        dotsOverlayView?.startAnimation()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            val ax = event.values[0]
            val ay = event.values[1]
            dotsOverlayView?.updateTilt(ax, ay)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        
        sensorManager.unregisterListener(this)
        
        if (dotsOverlayView != null) {
            try {
                dotsOverlayView?.stopAnimation()
                windowManager.removeView(dotsOverlayView)
            } catch (e: Exception) {}
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "MotionCuesChannel",
                "Motion Cues Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
