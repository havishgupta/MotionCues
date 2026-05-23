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

    init {
        updatePaints()
        prefsManager.prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        updatePaints()
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
            gpsSpeedFlow += currentGpsSpeed * 3f * prefsManager.speedMultiplier
            if (prefsManager.dotSpacing > 0) {
                gpsSpeedFlow %= (prefsManager.dotSpacing * 2) // Wrap around over 2 dot spacings to be safe
            }
            
            invalidate()
            postOnAnimation(this)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        if (width == 0 || height == 0) return

        val dotRadius = prefsManager.dotSize
        val dotSpacing = prefsManager.dotSpacing
        val numDotsY = prefsManager.verticalRows
        
        // Margin so dots have space to slide into
        val sideMargin = 80f
        val columnGap = dotRadius * 3.5f

        val finalOffsetX = tiltXOffset
        val finalOffsetY = tiltYOffset + gpsSpeedFlow

        // Draw Left Side (2 columns)
        val leftCol1X = sideMargin + finalOffsetX
        val leftCol2X = sideMargin + columnGap + finalOffsetX
        
        // Draw Right Side (2 columns)
        val rightCol1X = width - sideMargin - columnGap + finalOffsetX
        val rightCol2X = width - sideMargin + finalOffsetX

        // Outer dots are larger
        val outerRadius = dotRadius * 1.5f
        val innerRadius = dotRadius * 0.8f

        // Massive iteration window so when it shifts 1000px up/down, you never see the end of the dots
        for (j in -30 until numDotsY + 30) {
            val baseY = (j * dotSpacing) + finalOffsetY
            
            // Column 1 (outer left, non-staggered)
            drawDot(canvas, leftCol1X, baseY, outerRadius)
            
            // Column 2 (inner left, staggered)
            drawDot(canvas, leftCol2X, baseY + (dotSpacing / 2f), innerRadius)
            
            // Column 3 (inner right, staggered)
            drawDot(canvas, rightCol1X, baseY + (dotSpacing / 2f), innerRadius)
            
            // Column 4 (outer right, non-staggered)
            drawDot(canvas, rightCol2X, baseY, outerRadius)
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