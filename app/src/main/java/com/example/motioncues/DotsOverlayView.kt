package com.example.motioncues

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

class DotsOverlayView(context: Context) : View(context) {

    private val dotPaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    private val bgPaint = Paint().apply {
        color = Color.TRANSPARENT
    }

    var speedOffset: Float = 0f
    var directionOffset: Float = 0f

    private val dotRadius = 12f
    private val dotSpacing = 80f
    private val numDotsY = 30
    private val numDotsX = 3

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Calculate continuous scrolling offset for Y (simulating speed)
        val yShift = speedOffset % dotSpacing
        
        // Calculate X offset based on direction (clamped for visual effect)
        val maxOffset = 150f
        val currentXOffset = (directionOffset * maxOffset).coerceIn(-maxOffset, maxOffset)

        // Draw left side dots
        drawDotGrid(canvas, 50f + currentXOffset, yShift)
        
        // Draw right side dots
        drawDotGrid(canvas, width - 50f - (numDotsX * dotSpacing) + currentXOffset, yShift)
    }

    private fun drawDotGrid(canvas: Canvas, startX: Float, yShift: Float) {
        for (i in 0 until numDotsX) {
            for (j in -2 until numDotsY) {
                val x = startX + (i * dotSpacing)
                val y = (j * dotSpacing) + yShift
                canvas.drawCircle(x, y, dotRadius, dotPaint)
            }
        }
    }

    fun updateOffsets(speed: Float, direction: Float) {
        // Increment speed offset to make dots scroll continuously
        // Speed is roughly m/s. We multiply by a factor to make it visible.
        speedOffset += speed * 0.5f 
        
        // Direction from location is usually 0 to 360 (Bearing).
        // Let's make the dots shift left/right based on the sine of the bearing 
        // to represent North/South vs East/West.
        // If bearing is 90 (East), sin is 1. If 270 (West), sin is -1.
        val directionRad = Math.toRadians(direction.toDouble())
        directionOffset = Math.sin(directionRad).toFloat()
        
        invalidate()
    }
}
