package com.example.smartgrow

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class LiveLineChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var dataPoints = listOf<Float>()
    private val maxPoints = 20 // How many points to show on screen at once

    var lineColor = Color.parseColor("#2F7F34")
    var fillColor = Color.parseColor("#202F7F34")

    // Fallback bounds if no data is present
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

    // New paints for the dynamic axes and labels
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#888888") // Subtle gray
        textSize = 28f
        textAlign = Paint.Align.RIGHT // Aligns decimal points beautifully
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#20888888") // Faint gray for grid lines
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    fun setData(points: List<Float>) {
        this.dataPoints = points
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataPoints.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()

        // 1. DYNAMIC SCALING ALGORITHM
        var currentMin = dataPoints.minOrNull() ?: minY
        var currentMax = dataPoints.maxOrNull() ?: maxY

        val diff = currentMax - currentMin
        if (diff == 0f) {
            // If data is a flat line, force a +/- 5 gap so it centers nicely
            currentMin -= 5f
            currentMax += 5f
        } else {
            // Add 15% padding to top and bottom so peaks don't clip the edges
            currentMin -= diff * 0.15f
            currentMax += diff * 0.15f
        }
        val range = currentMax - currentMin

        // 2. DRAW AXES & LABELS
        val labelAreaWidth = 90f // Reserve space on the left for text
        val graphAreaWidth = w - labelAreaWidth

        // Top line (Max)
        canvas.drawText(String.format("%.1f", currentMax), labelAreaWidth - 16f, 30f, textPaint)
        canvas.drawLine(labelAreaWidth, 0f, w, 0f, gridPaint)

        // Mid line
        val midY = h / 2f
        canvas.drawText(String.format("%.1f", currentMin + (range / 2f)), labelAreaWidth - 16f, midY + 10f, textPaint)
        canvas.drawLine(labelAreaWidth, midY, w, midY, gridPaint)

        // Bottom line (Min)
        canvas.drawText(String.format("%.1f", currentMin), labelAreaWidth - 16f, h - 5f, textPaint)
        canvas.drawLine(labelAreaWidth, h, w, h, gridPaint)

        // 3. DRAW THE DATA GRAPH
        linePaint.color = lineColor
        fillPaint.color = fillColor
        path.reset()
        fillPath.reset()

        val dx = graphAreaWidth / (maxPoints - 1).coerceAtLeast(1).toFloat()
        val startX = w - (dataPoints.size - 1) * dx

        fillPath.moveTo(startX, h)

        for ((i, value) in dataPoints.withIndex()) {
            val x = startX + i * dx
            val normalizedY = 1f - ((value - currentMin) / range)
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