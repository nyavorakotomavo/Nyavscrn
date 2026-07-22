package com.nyavo.nrscreen.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.nyavo.nrscreen.R
import kotlin.math.sin
import kotlin.math.cos

class OnboardingIllustrationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var time = 0f
    private var pageIndex = 0

    private val nebulaColor = ContextCompat.getColor(context, R.color.nebula_500)
    private val cyanColor = ContextCompat.getColor(context, R.color.cyan_400)
    private val starColor = ContextCompat.getColor(context, R.color.star_400)

    fun setPage(index: Int) {
        pageIndex = index
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        time += 0.02f

        val cx = width / 2f
        val cy = height / 2f
        val baseRadius = minOf(width, height) * 0.35f

        when (pageIndex) {
            0 -> drawPhoneScreen(canvas, cx, cy, baseRadius)
            1 -> drawSwipeGesture(canvas, cx, cy, baseRadius)
            2 -> drawColorGrid(canvas, cx, cy, baseRadius)
        }

        invalidate()
    }

    private fun drawPhoneScreen(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = nebulaColor
        val rect = RectF(cx - radius * 0.6f, cy - radius, cx + radius * 0.6f, cy + radius)
        canvas.drawRoundRect(rect, 16f, 16f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.argb(40, 139, 92, 246)
        val screenRect = RectF(
            cx - radius * 0.5f,
            cy - radius * 0.85f,
            cx + radius * 0.5f,
            cy + radius * 0.7f
        )
        canvas.drawRoundRect(screenRect, 8f, 8f, paint)

        paint.strokeWidth = 1f
        paint.color = Color.argb(60, 139, 92, 246)
        for (i in 0..4) {
            val x = screenRect.left + (screenRect.width() / 4) * i
            canvas.drawLine(x, screenRect.top, x, screenRect.bottom, paint)
            val y = screenRect.top + (screenRect.height() / 6) * i
            canvas.drawLine(screenRect.left, y, screenRect.right, y, paint)
        }

        paint.strokeWidth = 2f
        paint.color = ContextCompat.getColor(context, R.color.void_500)
        val crackX = cx + radius * 0.1f * sin(time)
        canvas.drawLine(crackX, screenRect.top, crackX + radius * 0.3f, screenRect.bottom, paint)
    }

    private fun drawSwipeGesture(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        paint.style = Paint.Style.FILL
        val cellSize = radius * 0.15f
        for (row in 0..6) {
            for (col in 0..4) {
                val alpha = if ((row + col) % 3 == 0) 30 else 15
                paint.color = Color.argb(alpha, 34, 211, 238)
                val x = cx - radius * 0.7f + col * cellSize
                val y = cy - radius * 0.8f + row * cellSize
                canvas.drawRect(x, y, x + cellSize - 2, y + cellSize - 2, paint)
            }
        }

        paint.strokeWidth = 8f
        paint.strokeCap = Paint.Cap.ROUND
        val trailPoints = (0..20).map { i ->
            val t = i / 20f
            val x = cx - radius * 0.5f + t * radius
            val y = cy + sin(t * 3.14f + time) * radius * 0.3f
            Pair(x, y)
        }
        for (i in 0 until trailPoints.size - 1) {
            val alpha = (255 * (1f - i / 20f)).toInt()
            paint.color = Color.argb(alpha, 34, 211, 238)
            canvas.drawLine(trailPoints[i].first, trailPoints[i].second,
                trailPoints[i + 1].first, trailPoints[i + 1].second, paint)
        }

        paint.color = cyanColor
        val fingerX = trailPoints.last().first
        val fingerY = trailPoints.last().second
        canvas.drawCircle(fingerX, fingerY, 12f, paint)
        paint.color = Color.argb(60, 34, 211, 238)
        canvas.drawCircle(fingerX, fingerY, 24f, paint)
    }

    private fun drawColorGrid(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val cellSize = radius * 0.18f
        val colors = listOf(
            ContextCompat.getColor(context, R.color.cyan_400),
            ContextCompat.getColor(context, R.color.void_500),
            ContextCompat.getColor(context, R.color.star_400),
            ContextCompat.getColor(context, R.color.nebula_600),
            ContextCompat.getColor(context, R.color.nebula_700),
            ContextCompat.getColor(context, R.color.cosmos_800)
        )
        val labels = listOf("OK", "MORT", "SUSP", "FANT", "DEG", "N/T")

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.text_primary)
            textSize = 10f
            textAlign = Paint.Align.CENTER
        }

        for (i in 0..5) {
            val row = i / 3
            val col = i % 3
            val x = cx - radius * 0.6f + col * (cellSize + 8)
            val y = cy - radius * 0.4f + row * (cellSize + 24)

            paint.color = colors[i]
            val rect = RectF(x, y, x + cellSize, y + cellSize)
            canvas.drawRoundRect(rect, 4f, 4f, paint)

            textPaint.color = if (i == 5) ContextCompat.getColor(context, R.color.text_secondary)
            else ContextCompat.getColor(context, R.color.text_primary)
            canvas.drawText(labels[i], x + cellSize / 2, y + cellSize + 14, textPaint)
        }
    }
}
