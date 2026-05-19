package com.example.motioncues

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

class DotsOverlayView(context: Context) : View(context) {

    private val dotPaint = Paint().apply {
        color = Color.BLACK
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
    private var directionOffset: Float = 0f

    private val dotRadius = 14f
    private val dotSpacing = 80f
    private val numDotsY = 40
    private val numDotsX = 3

    private var currentSpeed: Float = 0f
    private var currentBearing: Float = 0f
    private var isAnimating = false

    private val animationRunnable = object : Runnable {
        override fun run() {
            if (!isAnimating) return
            
            // apply a minimum speed so the balls always move slowly downward
            // to show that the overlay is functioning even without GPS speed.
            speedOffset += (currentSpeed * 2f).coerceAtLeast(1f)
            
            // Map 0-360 bearing to a subtle left/right sway
            val directionRad = Math.toRadians(currentBearing.toDouble())
            val targetOffset = Math.sin(directionRad).toFloat()
            directionOffset += (targetOffset - directionOffset) * 0.1f // smooth interpolation
            
            invalidate()
            postOnAnimation(this)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Make the scrolling wrap around seamlessly
        val yShift = speedOffset % dotSpacing
        
        // Direction offset shifts X
        val maxOffset = 150f
        val currentXOffset = (directionOffset * maxOffset).coerceIn(-maxOffset, maxOffset)

        // Draw left side dots
        drawDotGrid(canvas, 50f + currentXOffset, yShift)
        
        // Draw right side dots
        drawDotGrid(canvas, width - 50f - (numDotsX * dotSpacing) + currentXOffset, yShift)
    }

    private fun drawDotGrid(canvas: Canvas, startX: Float, yShift: Float) {
        for (i in 0 until numDotsX) {
            for (j in -5 until numDotsY) {
                val x = startX + (i * dotSpacing)
                val y = (j * dotSpacing) + yShift
                canvas.drawCircle(x, y, dotRadius, dotPaint)
                canvas.drawCircle(x, y, dotRadius, outlinePaint)
            }
        }
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

    fun updateMotionData(speed: Float, bearing: Float) {
        currentSpeed = speed
        currentBearing = bearing
    }
}
