package com.nyavo.nrscreen.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.animation.ValueAnimator
import androidx.core.content.ContextCompat
import com.nyavo.nrscreen.R

class DonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class Segment(
        val color: Int,
        val percentage: Float,
        val label: String
    )

    private val segments = mutableListOf<Segment>()
    private var animatedProgress = 0f
    private var animator: ValueAnimator? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.cosmos_700)
        strokeCap = Paint.Cap.ROUND
    }

    private var innerRadius = 0f
    private var centerX = 0f
    private var centerY = 0f

    fun setData(data: List<Segment>) {
        segments.clear()
        segments.addAll(data)
        startAnimation()
    }

    private fun startAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1200
            interpolator = DecelerateInterpolator(1.5f)
            addUpdateListener {
                animatedProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        val minDim = minOf(w, h).toFloat()
        val strokeWidth = minDim * 0.12f
        paint.strokeWidth = strokeWidth
        trackPaint.strokeWidth = strokeWidth
        innerRadius = (minDim - strokeWidth) / 2f - strokeWidth * 0.5f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (segments.isEmpty()) return

        canvas.drawCircle(centerX, centerY, innerRadius, trackPaint)

        var startAngle = -90f
        val total = segments.sumOf { it.percentage.toDouble() }.toFloat()
        val scale = if (total > 0) 360f / total else 0f

        segments.forEach { segment ->
            val sweepAngle = segment.percentage * scale * animatedProgress
            if (sweepAngle > 0.5f) {
                paint.color = segment.color
                val rect = RectF(
                    centerX - innerRadius,
                    centerY - innerRadius,
                    centerX + innerRadius,
                    centerY + innerRadius
                )
                canvas.drawArc(rect, startAngle, sweepAngle - 2f, false, paint)
            }
            startAngle += segment.percentage * scale
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
