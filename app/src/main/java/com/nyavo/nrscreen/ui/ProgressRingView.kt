package com.nyavo.nrscreen.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.nyavo.nrscreen.R

class ProgressRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.cosmos_700)
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private var progress = 0f
    private var max = 100f
    private var animator: ValueAnimator? = null

    fun setProgress(value: Float, animate: Boolean = true) {
        val target = value.coerceIn(0f, max)
        if (animate) {
            animator?.cancel()
            animator = ValueAnimator.ofFloat(progress, target).apply {
                duration = 500
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    progress = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else {
            progress = target
            invalidate()
        }
    }

    fun setMax(value: Float) {
        max = value
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val strokeWidth = minOf(w, h) * 0.08f
        trackPaint.strokeWidth = strokeWidth
        progressPaint.strokeWidth = strokeWidth
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = (minOf(width, height) / 2f) - trackPaint.strokeWidth

        canvas.drawCircle(cx, cy, radius, trackPaint)

        val sweepAngle = (progress / max) * 360f
        if (sweepAngle > 0) {
            val shader = SweepGradient(cx, cy, intArrayOf(
                ContextCompat.getColor(context, R.color.cyan_400),
                ContextCompat.getColor(context, R.color.nebula_500),
                ContextCompat.getColor(context, R.color.cyan_400)
            ), floatArrayOf(0f, 0.5f, 1f))
            progressPaint.shader = shader
            val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
            canvas.drawArc(rect, -90f, sweepAngle, false, progressPaint)
            progressPaint.shader = null
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
