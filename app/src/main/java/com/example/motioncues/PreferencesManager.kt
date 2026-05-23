package com.example.motioncues

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color

class PreferencesManager(context: Context) {
    val prefs: SharedPreferences = context.getSharedPreferences("motion_cues_prefs", Context.MODE_PRIVATE)

    var dotColor: Int
        get() = prefs.getInt("dot_color", Color.BLACK)
        set(value) = prefs.edit().putInt("dot_color", value).apply()

    var dotSize: Float
        get() = prefs.getFloat("dot_size", 15f)
        set(value) = prefs.edit().putFloat("dot_size", value).apply()

    var dotSpacing: Float
        get() = prefs.getFloat("dot_spacing", 110f)
        set(value) = prefs.edit().putFloat("dot_spacing", value).apply()
        
    var dotOpacity: Float
        get() = prefs.getFloat("dot_opacity", 0.5f)
        set(value) = prefs.edit().putFloat("dot_opacity", value).apply()
        
    var verticalRows: Int
        get() = prefs.getInt("vertical_rows", 15)
        set(value) = prefs.edit().putInt("vertical_rows", value).apply()
        
    var hasSeenIntro: Boolean
        get() = prefs.getBoolean("has_seen_intro", false)
        set(value) = prefs.edit().putBoolean("has_seen_intro", value).apply()
        
    var tiltSensitivity: Float
        get() = prefs.getFloat("tilt_sensitivity", 1.0f)
        set(value) = prefs.edit().putFloat("tilt_sensitivity", value).apply()

    var gpsSensitivity: Float
        get() = prefs.getFloat("gps_sensitivity", 1.0f)
        set(value) = prefs.edit().putFloat("gps_sensitivity", value).apply()
        
    var speedMultiplier: Float
        get() = prefs.getFloat("speed_multiplier", 1.0f)
        set(value) = prefs.edit().putFloat("speed_multiplier", value).apply()
        
    var debugMode: Boolean
        get() = prefs.getBoolean("debug_mode", false)
        set(value) = prefs.edit().putBoolean("debug_mode", value).apply()
}
