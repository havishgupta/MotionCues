package com.example.motioncues

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

class DotsOverlayView(context: Context) : View(context), SharedPreferences.OnSharedPreferenceChangeListener {

    private val prefsManager = PreferencesManager(context)

    private val dotPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    private val outlinePaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val bgPaint = Paint().apply {
        color = Color.TRANSPARENT
    }

    private var tiltXOffset: Float = 0f
    private var tiltYOffset: Float = 0f

    private var targetTiltX: Float = 0f
    private var targetTiltY: Float = 0f

    private var currentGpsSpeed: Float = 0f
    private var gpsSpeedFlow: Float = 0f

    private var jitterX: Float = 0f
    private var jitterY: Float = 0f

    private var isAnimating = false

    private var lastAccelX: Float = 0f
    private var lastAccelY: Float = 0f
    private var lastLat: Double = 0.0
    private var lastLon: Double = 0.0
    
    private val debugTextPaint = Paint().apply {
        color = Color.GREEN
        textSize = 40f
        isAntiAlias = true
        style = Paint.Style.FILL
        setShadowLayer(5f, 0f, 0f, Color.BLACK)
    }

    private data class Dot(
        var baseX: Float,
        var baseY: Float,
        var radius: Float,
        var speedMultiplier: Float,
        var isLeft: Boolean
    )
    
    private val dots = mutableListOf<Dot>()
    private var isInitialized = false

    init {
        updatePaints()
        prefsManager.prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        updatePaints()
        isInitialized = false // Re-initialize dots on pref change (e.g. size/color)
    }

    private fun updatePaints() {
        val baseColor = prefsManager.dotColor
        val alpha = (prefsManager.dotOpacity * 255).toInt()
        
        dotPaint.color = Color.argb(alpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
        outlinePaint.alpha = alpha
    }

    private val animationRunnable = object : Runnable {
        override fun run() {
            if (!isAnimating) return
            
            // Smoothly interpolate tilt (Low pass filter effect)
            tiltXOffset += (targetTiltX - tiltXOffset) * 0.1f * prefsManager.speedMultiplier
            tiltYOffset += (targetTiltY - tiltYOffset) * 0.1f * prefsManager.speedMultiplier
            
            // Continuous flow based on GPS speed
            // No need to wrap this variable anymore since individual dots modulo wrap their position
            gpsSpeedFlow += currentGpsSpeed * 3f * prefsManager.speedMultiplier

            // Jitter for extra realism/sensory feedback
            jitterX = (Math.random().toFloat() - 0.5f) * 6f * prefsManager.speedMultiplier
            jitterY = (Math.random().toFloat() - 0.5f) * 6f * prefsManager.speedMultiplier
            
            invalidate()
            postOnAnimation(this)
        }
    }

    private fun initializeDots(w: Int, h: Int) {
        dots.clear()
        // Total dots based on preferences
        val numDots = prefsManager.verticalRows * 6 
        val dotRadius = prefsManager.dotSize
        
        // Define two "lanes" on the sides
        val sideMargin = 60f
        val laneWidth = dotRadius * 5f
        
        for (i in 0 until numDots) {
            val isLeft = i % 2 == 0
            
            // Randomize position within the lane
            val laneStartX = if (isLeft) sideMargin else (w - sideMargin - laneWidth)
            val baseX = laneStartX + Math.random().toFloat() * laneWidth
            
            // Random Y across the whole extended height
            val baseY = Math.random().toFloat() * (h + 400f) - 200f
            
            // Random size variation per dot (0.6x to 1.4x)
            val radius = dotRadius * (0.6f + Math.random().toFloat() * 0.8f)
            
            // Random speed variation so they drift apart organically
            val speedMult = 0.7f + Math.random().toFloat() * 0.6f
            
            dots.add(Dot(baseX, baseY, radius, speedMult, isLeft))
        }
        isInitialized = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        if (width == 0 || height == 0) return

        if (!isInitialized) {
            initializeDots(width, height)
        }

        val wrapHeight = height + 400f // Extension top and bottom to avoid popping

        for (dot in dots) {
            // Apply global offsets and speed flow, then add individual speed and jitter
            val currentY = dot.baseY + tiltYOffset + (gpsSpeedFlow * dot.speedMultiplier) + jitterY
            val currentX = dot.baseX + tiltXOffset + jitterX

            // Wrap vertically so the particle system is infinite
            val wrappedY = ((currentY + 200f) % wrapHeight + wrapHeight) % wrapHeight - 200f

            drawDot(canvas, currentX, wrappedY, dot.radius)
        }

        if (prefsManager.debugMode) {
            val debugLines = listOf(
                "Debug Mode Active",
                "Accel X: ${String.format("%.2f", lastAccelX)}",
                "Accel Y: ${String.format("%.2f", lastAccelY)}",
                "GPS Lat: ${String.format("%.5f", lastLat)}",
                "GPS Lon: ${String.format("%.5f", lastLon)}",
                "Speed (m/s): ${String.format("%.2f", currentGpsSpeed)}",
                "Flow Y: ${String.format("%.2f", gpsSpeedFlow)}"
            )

            var textY = 100f
            for (line in debugLines) {
                // Draw in top right
                val textWidth = debugTextPaint.measureText(line)
                canvas.drawText(line, width - textWidth - 40f, textY, debugTextPaint)
                textY += 50f
            }
        }
    }

    private fun drawDot(canvas: Canvas, x: Float, y: Float, radius: Float) {
        canvas.drawCircle(x, y, radius, dotPaint)
        canvas.drawCircle(x, y, radius, outlinePaint)
    }

    fun startAnimation() {
        if (!isAnimating) {
            isAnimating = true
            postOnAnimation(animationRunnable)
        }
    }

    fun stopAnimation() {
        isAnimating = false
        removeCallbacks(animationRunnable)
    }

    fun updateTilt(accelX: Float, accelY: Float) {
        lastAccelX = accelX
        lastAccelY = accelY
        // Massive limits allowing for extreme visual sliding across screen
        targetTiltX = (-accelX * 60f * prefsManager.tiltSensitivity).coerceIn(-800f, 800f)
        targetTiltY = (accelY * 60f * prefsManager.tiltSensitivity).coerceIn(-1200f, 1200f)
    }

    fun updateSpeed(speed: Float) {
        currentGpsSpeed = speed
    }

    fun updateDebugLocation(lat: Double, lon: Double) {
        lastLat = lat
        lastLon = lon
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        prefsManager.prefs.unregisterOnSharedPreferenceChangeListener(this)
    }
}
