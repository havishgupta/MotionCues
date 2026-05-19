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

    private var speedOffset: Float = 0f
    
    private var tiltXOffset: Float = 0f
    private var tiltYOffset: Float = 0f

    private var targetTiltX: Float = 0f
    private var targetTiltY: Float = 0f

    private var currentSpeed: Float = 0f
    private var filteredSpeed: Float = 0f
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
            
            // Smoothly interpolate speed so it doesn't jump
            filteredSpeed += (currentSpeed - filteredSpeed) * 0.05f
            
            // Speed controls downward scrolling. If speed is 0, it doesn't scroll automatically.
            speedOffset += filteredSpeed * 1.5f
            
            // Smoothly interpolate tilt (Low pass filter effect)
            tiltXOffset += (targetTiltX - tiltXOffset) * 0.1f
            tiltYOffset += (targetTiltY - tiltYOffset) * 0.1f
            
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
        
        val totalYShift = (speedOffset + tiltYOffset) % dotSpacing
        
        // Define margins
        val sideMargin = 30f
        val columnGap = dotRadius * 2.5f

        // Draw Left Side (2 columns)
        val leftCol1X = sideMargin + tiltXOffset
        val leftCol2X = sideMargin + columnGap + tiltXOffset
        
        // Draw Right Side (2 columns)
        val rightCol1X = width - sideMargin - columnGap + tiltXOffset
        val rightCol2X = width - sideMargin + tiltXOffset

        for (j in -5 until numDotsY) {
            val baseY = (j * dotSpacing) + totalYShift
            
            // Column 1 (outer left, non-staggered)
            drawDot(canvas, leftCol1X, baseY, dotRadius)
            
            // Column 2 (inner left, staggered)
            drawDot(canvas, leftCol2X, baseY + (dotSpacing / 2f), dotRadius)
            
            // Column 3 (inner right, staggered)
            drawDot(canvas, rightCol1X, baseY + (dotSpacing / 2f), dotRadius)
            
            // Column 4 (outer right, non-staggered)
            drawDot(canvas, rightCol2X, baseY, dotRadius)
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

    fun updateMotionData(speed: Float) {
        currentSpeed = speed
    }

    fun updateTilt(accelX: Float, accelY: Float) {
        // accelX is negative when tilted left, causing positive targetTiltX (moves right)
        targetTiltX = (-accelX * 12f).coerceIn(-120f, 120f)
        
        // accelY is positive when tilted up, moving dots up/down based on orientation
        targetTiltY = (accelY * 12f).coerceIn(-120f, 120f)
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        prefsManager.prefs.unregisterOnSharedPreferenceChangeListener(this)
    }
}
