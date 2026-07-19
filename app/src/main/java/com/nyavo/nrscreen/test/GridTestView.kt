package com.nyavo.nrscreen.test

import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.nyavo.nrscreen.R

class GridTestView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val ROWS = 60
        const val COLS = 120
        const val BRUSH_RADIUS = 1
        const val RIPPLE_DURATION = 300L
        const val RIPPLE_MAX_RADIUS_DP = 60f
    }

    var deadZoneMap: DeadZoneMap? = null
    var onCellDiscovered: ((Int) -> Unit)? = null

    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private val ripples = mutableListOf<Ripple>()
    private var touchX = -1f
    private var touchY = -1f
    private var isTouching = false

    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint()
    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val untestedColor: Int
    private val deadColor: Int
    private val suspectColor: Int
    private val ghostColor: Int
    private val cyan400: Int
    private val cyan500: Int
    private val cyan600: Int

    private data class Ripple(val cx: Float, val cy: Float, val startTime: Long)

    init {
        deadZoneMap = DeadZoneMap(ROWS, COLS)

        untestedColor = ContextCompat.getColor(context, R.color.state_untested)
        deadColor = ContextCompat.getColor(context, R.color.state_dead)
        suspectColor = ContextCompat.getColor(context, R.color.state_suspect)
        ghostColor = ContextCompat.getColor(context, R.color.state_ghost)
        cyan400 = ContextCompat.getColor(context, R.color.cyan_400)
        cyan500 = ContextCompat.getColor(context, R.color.cyan_500)
        cyan600 = ContextCompat.getColor(context, R.color.cyan_600)

        gridPaint.color = ContextCompat.getColor(context, R.color.cosmos_700)
        gridPaint.strokeWidth = 0.3f
        gridPaint.alpha = 40
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val map = deadZoneMap ?: return
        val cellW = width.toFloat() / COLS
        val cellH = height.toFloat() / ROWS
        val now = System.currentTimeMillis()

        for (r in 0 until ROWS) {
            for (c in 0 until COLS) {
                val cell = map.cellAt(r, c)
                val left = c * cellW
                val top = r * cellH
                val right = left + cellW
                val bottom = top + cellH

                cellPaint.color = when (cell.state) {
                    ZoneState.UNTESTED -> untestedColor
                    ZoneState.ALIVE -> aliveColorFor(cell, now)
                    ZoneState.DEAD -> deadColor
                    ZoneState.SUSPECT -> suspectColor
                    ZoneState.GHOST -> ghostColor
                }
                canvas.drawRect(left, top, right, bottom, cellPaint)
            }
        }

        val iterator = ripples.iterator()
        while (iterator.hasNext()) {
            val ripple = iterator.next()
            val age = now - ripple.startTime
            if (age > RIPPLE_DURATION) {
                iterator.remove()
                continue
            }
            val progress = age / RIPPLE_DURATION.toFloat()
            val radius = progress * RIPPLE_MAX_RADIUS_DP * resources.displayMetrics.density
            val alpha = ((1f - progress) * 180).toInt()
            ripplePaint.color = Color.argb(alpha, Color.red(cyan400), Color.green(cyan400), Color.blue(cyan400))
            canvas.drawCircle(ripple.cx, ripple.cy, radius, ripplePaint)
        }

        if (isTouching && touchX >= 0 && touchY >= 0) {
            val glowRadius = cellW * 5
            glowPaint.shader = RadialGradient(
                touchX, touchY, glowRadius,
                Color.argb(100, 103, 232, 249),
                Color.argb(0, 103, 232, 249),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(touchX, touchY, glowRadius, glowPaint)
            glowPaint.shader = null
        }

        if (ripples.isNotEmpty() || isTouching) {
            invalidate()
        }
    }

    private fun aliveColorFor(cell: GridCell, now: Long): Int {
        val age = now - cell.activatedAt
        return when {
            age < 500 -> cyan400
            age < 1000 -> cyan500
            else -> cyan600
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val map = deadZoneMap ?: return true
        val cellW = width.toFloat() / COLS
        val cellH = height.toFloat() / ROWS

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isTouching = true
                touchX = event.x
                touchY = event.y
                val count = processTouch(map, event.x, event.y, cellW, cellH)
                if (count > 0) vibrate()
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                touchX = event.x
                touchY = event.y
                val count = processTouch(map, event.x, event.y, cellW, cellH)
                if (count > 0) vibrate()
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                isTouching = false
                touchX = -1f
                touchY = -1f
                invalidate()
            }
        }
        return true
    }

    private fun processTouch(map: DeadZoneMap, x: Float, y: Float, cellW: Float, cellH: Float): Int {
        val centerCol = (x / cellW).toInt().coerceIn(0, COLS - 1)
        val centerRow = (y / cellH).toInt().coerceIn(0, ROWS - 1)
        var newCount = 0

        for (dr in -BRUSH_RADIUS..BRUSH_RADIUS) {
            for (dc in -BRUSH_RADIUS..BRUSH_RADIUS) {
                val r = centerRow + dr
                val c = centerCol + dc
                if (r in 0 until ROWS && c in 0 until COLS) {
                    val cell = map.cellAt(r, c)
                    if (cell.state == ZoneState.UNTESTED) {
                        cell.state = ZoneState.ALIVE
                        cell.activatedAt = System.currentTimeMillis()
                        newCount++
                    }
                }
            }
        }

        if (newCount > 0) {
            ripples.add(Ripple(x, y, System.currentTimeMillis()))
            onCellDiscovered?.invoke(newCount)
        }
        return newCount
    }

    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(8, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(8)
        }
    }

    fun finalizeTest() {
        deadZoneMap?.finalizeUntested()
        invalidate()
    }
}
