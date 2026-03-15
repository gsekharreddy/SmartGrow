package com.example.smartgrow

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class LiveLineChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dataPoints = mutableListOf<Float>()
    private val maxPoints = 20 // How many points to show on screen at once

    // Default colors (can be changed programmatically)
    var lineColor = Color.parseColor("#2F7F34") // Default Green
    var fillColor = Color.parseColor("#202F7F34") // Faint Green Fill

    // Y-Axis bounds to keep the graph stable
    var minY = 0f
    var maxY = 100f

    private val path = Path()
    private val fillPath = Path()
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    fun addDataPoint(value: Float) {
        dataPoints.add(value)
        if (dataPoints.size > maxPoints) {
            dataPoints.removeAt(0) // Remove oldest point to slide the graph left
        }
        invalidate() // Trigger a redraw
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataPoints.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()

        linePaint.color = lineColor
        fillPaint.color = fillColor

        path.reset()
        fillPath.reset()

        val dx = w / (maxPoints - 1).coerceAtLeast(1).toFloat()
        val range = (maxY - minY).takeIf { it > 0 } ?: 1f

        // Start drawing from the right edge, moving left for older points
        val startX = w - (dataPoints.size - 1) * dx

        fillPath.moveTo(startX, h)

        for ((i, value) in dataPoints.withIndex()) {
            val x = startX + i * dx
            // Normalize value to fit within the view's height
            val normalizedY = 1f - ((value.coerceIn(minY, maxY) - minY) / range)
            val y = normalizedY * h

            if (i == 0) {
                path.moveTo(x, y)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(w, h)
        fillPath.close()

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(path, linePaint)
    }
}