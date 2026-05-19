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

    private var isAnimating = false

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
            
            invalidate()
            postOnAnimation(this)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val dotRadius = prefsManager.dotSize
        val dotSpacing = prefsManager.dotSpacing
        val numDotsY = prefsManager.verticalRows
        
        // Increased margin so dots have huge space to slide into
        val sideMargin = 80f
        val columnGap = dotRadius * 3.5f

        // Draw Left Side (2 columns)
        val leftCol1X = sideMargin + tiltXOffset
        val leftCol2X = sideMargin + columnGap + tiltXOffset
        
        // Draw Right Side (2 columns)
        val rightCol1X = width - sideMargin - columnGap + tiltXOffset
        val rightCol2X = width - sideMargin + tiltXOffset

        // Outer dots are larger
        val outerRadius = dotRadius * 1.5f
        val innerRadius = dotRadius * 0.8f

        // Massive iteration window so when it shifts 1000px up/down, you never see the end of the dots
        for (j in -30 until numDotsY + 30) {
            val baseY = (j * dotSpacing) + tiltYOffset
            
            // Column 1 (outer left, non-staggered)
            drawDot(canvas, leftCol1X, baseY, outerRadius)
            
            // Column 2 (inner left, staggered)
            drawDot(canvas, leftCol2X, baseY + (dotSpacing / 2f), innerRadius)
            
            // Column 3 (inner right, staggered)
            drawDot(canvas, rightCol1X, baseY + (dotSpacing / 2f), innerRadius)
            
            // Column 4 (outer right, non-staggered)
            drawDot(canvas, rightCol2X, baseY, outerRadius)
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
        // Massive limits allowing for extreme visual sliding across screen
        targetTiltX = (-accelX * 60f * prefsManager.tiltSensitivity).coerceIn(-800f, 800f)
        targetTiltY = (accelY * 60f * prefsManager.tiltSensitivity).coerceIn(-1200f, 1200f)
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        prefsManager.prefs.unregisterOnSharedPreferenceChangeListener(this)
    }
}
