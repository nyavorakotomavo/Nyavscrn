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
import kotlin.math.sqrt

class GridTestView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val TARGET_CELL_DP = 14f
        const val BRUSH_RADIUS = 2
        const val PIXEL_GLOW_RADIUS = 4
        const val RIPPLE_DURATION = 900L
        const val VIBRATION_COOLDOWN_MS = 80L
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
    private var isTouching = false
    private var lastVibrationTime = 0L
    private var cellSize = 0f
    private var lastTouchCol = -1
    private var lastTouchRow = -1

    private val cellPaint = Paint()
    private val pixelGlowPaint = Paint()
    private val ripplePaints = List(4) { Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL } }
    private val interpolator = DecelerateInterpolator(1.8f)

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
            Ring(0.5f, 0.95f, 100f),
            Ring(0.75f, 0.75f, 140f),
            Ring(1.0f, 0.50f, 190f),
            Ring(1.35f, 0.25f, 260f)
        )
    ) {
        data class Ring(val speedMul: Float, val maxAlphaRatio: Float, val maxRadiusDp: Float)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return
        val density = resources.displayMetrics.density
        val targetPx = TARGET_CELL_DP * density

        val newCols = (w / targetPx).toInt().coerceAtLeast(8)
        val newRows = (h / targetPx).toInt().coerceAtLeast(8)

        val existingMap = deadZoneMap
        if (existingMap != null && existingMap.rows == newRows && existingMap.cols == newCols) {
            rows = newRows
            cols = newCols
            cellSize = w.toFloat() / cols
        } else {
            rows = newRows
            cols = newCols
            cellSize = w.toFloat() / cols
            deadZoneMap = DeadZoneMap(rows, cols)
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
                if (cell.state == ZoneState.UNTESTED) continue

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
                    else -> untestedColor
                }
                canvas.drawRect(left, top, right, bottom, cellPaint)
            }
        }

        if (isTouching && touchX >= 0 && touchY >= 0 && !ghostDetectionMode && cellSize > 0) {
            drawPixelGlow(canvas, map, touchX, touchY)
        }

        if (!ghostDetectionMode) drawRipples(canvas, now)

        if (ripples.isNotEmpty() || isTouching) invalidate()
    }

    private fun drawPixelGlow(canvas: Canvas, map: DeadZoneMap, x: Float, y: Float) {
        val centerCol = (x / cellSize).toInt().coerceIn(0, cols - 1)
        val centerRow = (y / cellSize).toInt().coerceIn(0, rows - 1)

        for (dr in -PIXEL_GLOW_RADIUS..PIXEL_GLOW_RADIUS) {
            for (dc in -PIXEL_GLOW_RADIUS..PIXEL_GLOW_RADIUS) {
                val dist = sqrt((dr * dr + dc * dc).toFloat())
                if (dist > PIXEL_GLOW_RADIUS) continue

                val r = centerRow + dr
                val c = centerCol + dc
                if (r !in 0 until rows || c !in 0 until cols) continue

                val cell = map.cellAt(r, c)
                val left = c * cellSize
                val top = r * cellSize
                val right = left + cellSize
                val bottom = top + cellSize

                val alpha = when {
                    dist <= 1.5f -> 160
                    dist <= 2.5f -> 100
                    dist <= 3.5f -> 55
                    else -> 25
                }

                pixelGlowPaint.color = if (cell.state == ZoneState.UNTESTED) {
                    Color.argb(alpha, 103, 232, 249)
                } else {
                    Color.argb(alpha, 196, 181, 253)
                }
                canvas.drawRect(left, top, right, bottom, pixelGlowPaint)
            }
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

    private fun aliveColorFor(cell: GridCell, now: Long): Int {
        val age = now - cell.activatedAt
        return when {
            age < 350 -> cyan400
            age < 800 -> cyan500
            else -> cyan600
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val map = deadZoneMap ?: return true
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isTouching = true
                touchX = event.x
                touchY = event.y
                lastTouchCol = (event.x / cellSize).toInt().coerceIn(0, cols - 1)
                lastTouchRow = (event.y / cellSize).toInt().coerceIn(0, rows - 1)
                val count = markCell(map, lastTouchRow, lastTouchCol)
                if (count > 0 && !ghostDetectionMode) {
                    ripples.add(Ripple(event.x, event.y, System.currentTimeMillis()))
                    onCellDiscovered?.invoke(count)
                    vibrate()
                }
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                touchX = event.x
                touchY = event.y
                val newCol = (event.x / cellSize).toInt().coerceIn(0, cols - 1)
                val newRow = (event.y / cellSize).toInt().coerceIn(0, rows - 1)

                val count = bresenhamProcess(map, lastTouchRow, lastTouchCol, newRow, newCol)
                lastTouchCol = newCol
                lastTouchRow = newRow

                if (count > 0 && !ghostDetectionMode) {
                    ripples.add(Ripple(event.x, event.y, System.currentTimeMillis()))
                    onCellDiscovered?.invoke(count)
                    vibrate()
                }
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isTouching = false
                touchX = -1f
                touchY = -1f
                lastTouchCol = -1
                lastTouchRow = -1
                invalidate()
            }
        }
        return true
    }

    private fun bresenhamProcess(map: DeadZoneMap, r0: Int, c0: Int, r1: Int, c1: Int): Int {
        var newCount = 0
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
            newCount += markCell(map, y0, x0)
            if (x0 == x1 && y0 == y1) break
            val e2 = 2 * err
            if (e2 > -dy) {
                err -= dy
                x0 += sx
            }
            if (e2 < dx) {
                err += dx
                y0 += sy
            }
        }

        return newCount
    }

    private fun markCell(map: DeadZoneMap, row: Int, col: Int): Int {
        var count = 0
        for (dr in -BRUSH_RADIUS..BRUSH_RADIUS) {
            for (dc in -BRUSH_RADIUS..BRUSH_RADIUS) {
                val r = row + dr
                val c = col + dc
                if (r in 0 until rows && c in 0 until cols) {
                    val cell = map.cellAt(r, c)
                    cell.tapAttempts++
                    if (cell.state == ZoneState.UNTESTED) {
                        cell.state = if (ghostDetectionMode) ZoneState.GHOST else ZoneState.ALIVE
                        cell.activatedAt = System.currentTimeMillis()
                        count++
                    }
                }
            }
        }
        return count
    }

    private fun vibrate() {
        val now = System.currentTimeMillis()
        if (now - lastVibrationTime < VIBRATION_COOLDOWN_MS) return
        lastVibrationTime = now
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(10)
        }
    }

    fun finalizeTest() {
        deadZoneMap?.finalizeTest()
        invalidate()
    }
}
