package com.nyavo.nrscreen.test

import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.nyavo.nrscreen.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

class GridTestView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val TARGET_CELL_DP = 10f
        const val BRUSH_RADIUS_CELLS = 3
        const val RIPPLE_DURATION = 700L
        const val VIBRATION_COOLDOWN_MS = 60L
    }

    var onCellDiscovered: ((Int) -> Unit)? = null
    var ghostDetectionMode = false

    var rows = 0
        private set
    var cols = 0
        private set

    var deadZoneMap: DeadZoneMap? = null
        set(value) {
            field = value
            value?.let {
                rows = it.rows
                cols = it.cols
                if (width > 0) cellSize = width.toFloat() / cols
            }
            invalidate()
        }

    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private val ripples = mutableListOf<Ripple>()
    private var touchX = -1f
    private var touchY = -1f
    private var prevTouchX = -1f
    private var prevTouchY = -1f
    private var isTouching = false
    private var lastVibrationTime = 0L
    private var cellSize = 0f

    private val cellPaint = Paint()
    private val haloPaint = Paint()
    private val ripplePaints = List(4) { Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL } }
    private val interpolator = DecelerateInterpolator(1.5f)

    private val untestedColor: Int = ContextCompat.getColor(context, R.color.cosmos_950)
    private val deadColor: Int = ContextCompat.getColor(context, R.color.void_500)
    private val suspectColor: Int = ContextCompat.getColor(context, R.color.star_400)
    private val ghostColor: Int = ContextCompat.getColor(context, R.color.nebula_600)
    private val degradedColor: Int = ContextCompat.getColor(context, R.color.nebula_700)
    private val cyan400: Int = ContextCompat.getColor(context, R.color.cyan_400)
    private val cyan500: Int = ContextCompat.getColor(context, R.color.cyan_500)
    private val cyan600: Int = ContextCompat.getColor(context, R.color.cyan_600)

    private data class Ripple(
        val cx: Float,
        val cy: Float,
        val startTime: Long,
        val rings: List<Ring> = listOf(
            Ring(0.4f, 0.90f, 80f),
            Ring(0.65f, 0.70f, 120f),
            Ring(0.90f, 0.45f, 170f),
            Ring(1.20f, 0.20f, 240f)
        )
    ) {
        data class Ring(val speedMul: Float, val maxAlphaRatio: Float, val maxRadiusDp: Float)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return

        val density = resources.displayMetrics.density
        val targetPx = TARGET_CELL_DP * density

        val newCols = (w / targetPx).toInt().coerceAtLeast(16)
        val newRows = (h / targetPx).toInt().coerceAtLeast(16)

        val existing = deadZoneMap
        if (existing == null || existing.rows != newRows || existing.cols != newCols) {
            rows = newRows
            cols = newCols
            cellSize = w.toFloat() / cols
            if (deadZoneMap == null) {
                deadZoneMap = DeadZoneMap(rows, cols)
            }
        } else {
            rows = newRows
            cols = newCols
            cellSize = w.toFloat() / cols
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val map = deadZoneMap ?: return
        if (cellSize <= 0) return
        val now = System.currentTimeMillis()

        canvas.drawColor(untestedColor)

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val cell = map.cellAt(r, c)
                if (cell.state == ZoneState.UNTESTED && !isInHalo(r, c)) continue

                val left = c * cellSize
                val top = r * cellSize
                val right = left + cellSize
                val bottom = top + cellSize

                cellPaint.color = when (cell.state) {
                    ZoneState.ALIVE -> aliveColorFor(cell, now)
                    ZoneState.DEAD -> deadColor
                    ZoneState.SUSPECT -> suspectColor
                    ZoneState.GHOST -> ghostColor
                    ZoneState.DEGRADED -> degradedColor
                    ZoneState.UNTESTED -> haloColorFor(r, c, now)
                }
                canvas.drawRect(left, top, right, bottom, cellPaint)
            }
        }

        if (!ghostDetectionMode) drawRipples(canvas, now)

        if (ripples.isNotEmpty() || isTouching) invalidate()
    }

    private fun isInHalo(r: Int, c: Int): Boolean {
        if (!isTouching || touchX < 0 || cellSize <= 0) return false
        val centerCol = (touchX / cellSize).toInt()
        val centerRow = (touchY / cellSize).toInt()
        val dist = sqrt(((c - centerCol) * (c - centerCol) + (r - centerRow) * (r - centerRow)).toFloat())
        return dist <= BRUSH_RADIUS_CELLS + 1
    }

    private fun haloColorFor(r: Int, c: Int, now: Long): Int {
        if (!isTouching || touchX < 0 || cellSize <= 0) return untestedColor
        val centerCol = (touchX / cellSize).toInt()
        val centerRow = (touchY / cellSize).toInt()
        val dist = sqrt(((c - centerCol) * (c - centerCol) + (r - centerRow) * (r - centerRow)).toFloat())

        if (dist > BRUSH_RADIUS_CELLS + 1) return untestedColor

        val alpha = when {
            dist <= 1.5f -> 200
            dist <= 2.5f -> 130
            dist <= 3.5f -> 70
            else -> 30
        }
        return Color.argb(alpha, 103, 232, 249)
    }

    private fun aliveColorFor(cell: GridCell, now: Long): Int {
        val age = now - cell.activatedAt
        return when {
            age < 300 -> cyan400
            age < 700 -> cyan500
            else -> cyan600
        }
    }

    private fun drawRipples(canvas: Canvas, now: Long) {
        val density = resources.displayMetrics.density
        val iter = ripples.iterator()
        while (iter.hasNext()) {
            val ripple = iter.next()
            val age = now - ripple.startTime
            if (age > RIPPLE_DURATION) { iter.remove(); continue }

            val globalProg = age / RIPPLE_DURATION.toFloat()
            ripple.rings.forEachIndexed { idx, ring ->
                val ringProg = (globalProg / ring.speedMul).coerceAtMost(1f)
                if (ringProg >= 1f) return@forEachIndexed
                val eased = interpolator.getInterpolation(ringProg)
                val radius = eased * ring.maxRadiusDp * density
                val alpha = ((1f - eased) * 255 * ring.maxAlphaRatio).toInt()
                if (alpha <= 0 || radius <= 0) return@forEachIndexed

                val shader = RadialGradient(
                    ripple.cx, ripple.cy, radius,
                    intArrayOf(
                        Color.argb(alpha, 103, 232, 249),
                        Color.argb((alpha * 0.55f).toInt(), 34, 211, 238),
                        Color.argb(0, 11, 15, 31)
                    ),
                    floatArrayOf(0.0f, 0.45f, 1.0f),
                    Shader.TileMode.CLAMP
                )
                ripplePaints[idx].shader = shader
                canvas.drawCircle(ripple.cx, ripple.cy, radius, ripplePaints[idx])
                ripplePaints[idx].shader = null
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val map = deadZoneMap ?: return true

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isTouching = true
                touchX = event.x; touchY = event.y
                prevTouchX = touchX; prevTouchY = touchY
                val count = processTouchAt(map, touchX, touchY)
                if (count > 0 && !ghostDetectionMode) vibrate()
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                prevTouchX = touchX; prevTouchY = touchY
                touchX = event.x; touchY = event.y
                val count = processTrajectory(map, prevTouchX, prevTouchY, touchX, touchY)
                if (count > 0 && !ghostDetectionMode) vibrate()
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isTouching = false
                touchX = -1f; touchY = -1f
                prevTouchX = -1f; prevTouchY = -1f
                invalidate()
            }
        }
        return true
    }

    private fun processTrajectory(map: DeadZoneMap, x1: Float, y1: Float, x2: Float, y2: Float): Int {
        if (cellSize <= 0) return 0
        val c1 = ((x1 / cellSize).toInt()).coerceIn(0, cols - 1)
        val r1 = ((y1 / cellSize).toInt()).coerceIn(0, rows - 1)
        val c2 = ((x2 / cellSize).toInt()).coerceIn(0, cols - 1)
        val r2 = ((y2 / cellSize).toInt()).coerceIn(0, rows - 1)

        var newCount = 0
        val cells = bresenhamLine(r1, c1, r2, c2)
        for ((r, c) in cells) {
            newCount += markCell(map, r, c)
        }
        return newCount
    }

    private fun processTouchAt(map: DeadZoneMap, x: Float, y: Float): Int {
        if (cellSize <= 0) return 0
        val col = (x / cellSize).toInt().coerceIn(0, cols - 1)
        val row = (y / cellSize).toInt().coerceIn(0, rows - 1)
        return markCell(map, row, col)
    }

    private fun markCell(map: DeadZoneMap, row: Int, col: Int): Int {
        var newCount = 0
        for (dr in -BRUSH_RADIUS_CELLS..BRUSH_RADIUS_CELLS) {
            for (dc in -BRUSH_RADIUS_CELLS..BRUSH_RADIUS_CELLS) {
                val dist = sqrt((dr * dr + dc * dc).toFloat())
                if (dist > BRUSH_RADIUS_CELLS) continue

                val r = row + dr
                val c = col + dc
                if (r !in 0 until rows || c !in 0 until cols) continue

                val cell = map.cellAt(r, c)
                cell.tapCount++
                if (cell.state == ZoneState.UNTESTED) {
                    cell.state = if (ghostDetectionMode) ZoneState.GHOST else ZoneState.ALIVE
                    cell.activatedAt = System.currentTimeMillis()
                    newCount++
                }
            }
        }
        if (newCount > 0 && !ghostDetectionMode) {
            ripples.add(Ripple(touchX, touchY, System.currentTimeMillis()))
            onCellDiscovered?.invoke(newCount)
        }
        return newCount
    }

    private fun bresenhamLine(r0: Int, c0: Int, r1: Int, c1: Int): List<Pair<Int, Int>> {
        val result = mutableListOf<Pair<Int, Int>>()
        var x0 = c0
        var y0 = r0
        val x1 = c1
        val y1 = r1
        val dx = abs(x1 - x0)
        val dy = abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx - dy

        while (true) {
            result.add(y0 to x0)
            if (x0 == x1 && y0 == y1) break
            val e2 = 2 * err
            if (e2 > -dy) { err -= dy; x0 += sx }
            if (e2 < dx) { err += dx; y0 += sy }
        }
        return result
    }

    private fun vibrate() {
        val now = System.currentTimeMillis()
        if (now - lastVibrationTime < VIBRATION_COOLDOWN_MS) return
        lastVibrationTime = now
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(8, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(8)
        }
    }

    fun finalizeTest() {
        deadZoneMap?.finalizeTest()
        invalidate()
    }
}
