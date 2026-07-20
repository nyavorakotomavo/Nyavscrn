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
import kotlin.math.sqrt

class GridTestView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val ROWS = 60
        const val COLS = 120
        const val BRUSH_RADIUS = 2
        const val PIXEL_GLOW_RADIUS = 4
        const val RIPPLE_DURATION = 900L
        const val VIBRATION_COOLDOWN_MS = 80L
    }

    var deadZoneMap: DeadZoneMap? = null
    var onCellDiscovered: ((Int) -> Unit)? = null

    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private val ripples = mutableListOf<Ripple>()
    private var touchX = -1f
    private var touchY = -1f
    private var isTouching = false
    private var lastVibrationTime = 0L

    private val cellPaint = Paint()
    private val pixelGlowPaint = Paint()
    private val ripplePaints = List(4) { Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL } }
    private val interpolator = DecelerateInterpolator(1.8f)

    private val untestedColor: Int
    private val deadColor: Int
    private val suspectColor: Int
    private val ghostColor: Int
    private val degradedColor: Int
    private val cyan400: Int
    private val cyan500: Int
    private val cyan600: Int
    private val nebula300: Int

    private var cellSize = 0f
    private var offsetX = 0f
    private var offsetY = 0f

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

    init {
        deadZoneMap = DeadZoneMap(ROWS, COLS)
        untestedColor = ContextCompat.getColor(context, R.color.state_untested)
        deadColor = ContextCompat.getColor(context, R.color.state_dead)
        suspectColor = ContextCompat.getColor(context, R.color.state_suspect)
        ghostColor = ContextCompat.getColor(context, R.color.state_ghost)
        degradedColor = ContextCompat.getColor(context, R.color.nebula_700)
        cyan400 = ContextCompat.getColor(context, R.color.cyan_400)
        cyan500 = ContextCompat.getColor(context, R.color.cyan_500)
        cyan600 = ContextCompat.getColor(context, R.color.cyan_600)
        nebula300 = ContextCompat.getColor(context, R.color.nebula_300)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val sizeW = w.toFloat() / COLS
        val sizeH = h.toFloat() / ROWS
        cellSize = minOf(sizeW, sizeH)
        offsetX = (w - COLS * cellSize) / 2f
        offsetY = (h - ROWS * cellSize) / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val map = deadZoneMap ?: return
        val now = System.currentTimeMillis()

        canvas.drawColor(ContextCompat.getColor(context, R.color.cosmos_950))

        for (r in 0 until ROWS) {
            for (c in 0 until COLS) {
                val cell = map.cellAt(r, c)
                val left = offsetX + c * cellSize
                val top = offsetY + r * cellSize
                val right = left + cellSize
                val bottom = top + cellSize
                cellPaint.color = when (cell.state) {
                    ZoneState.UNTESTED -> untestedColor
                    ZoneState.ALIVE -> aliveColorFor(cell, now)
                    ZoneState.DEAD -> deadColor
                    ZoneState.SUSPECT -> suspectColor
                    ZoneState.GHOST -> ghostColor
                    ZoneState.DEGRADED -> degradedColor
                }
                canvas.drawRect(left, top, right, bottom, cellPaint)
            }
        }

        if (isTouching && touchX >= 0 && touchY >= 0) {
            drawPixelGlow(canvas, map, touchX, touchY)
        }

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

        if (ripples.isNotEmpty() || isTouching) invalidate()
    }

    private fun drawPixelGlow(canvas: Canvas, map: DeadZoneMap, x: Float, y: Float) {
        val centerCol = ((x - offsetX) / cellSize).toInt().coerceIn(0, COLS - 1)
        val centerRow = ((y - offsetY) / cellSize).toInt().coerceIn(0, ROWS - 1)

        for (dr in -PIXEL_GLOW_RADIUS..PIXEL_GLOW_RADIUS) {
            for (dc in -PIXEL_GLOW_RADIUS..PIXEL_GLOW_RADIUS) {
                val dist = sqrt((dr * dr + dc * dc).toFloat())
                if (dist > PIXEL_GLOW_RADIUS) continue

                val r = centerRow + dr
                val c = centerCol + dc
                if (r !in 0 until ROWS || c !in 0 until COLS) continue

                val cell = map.cellAt(r, c)
                val left = offsetX + c * cellSize
                val top = offsetY + r * cellSize
                val right = left + cellSize
                val bottom = top + cellSize

                val alpha = when {
                    dist <= 1.5f -> 180
                    dist <= 2.5f -> 120
                    dist <= 3.5f -> 70
                    else -> 40
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
                touchX = event.x; touchY = event.y
                val count = processTouch(map, event.x, event.y)
                if (count > 0) vibrate()
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                touchX = event.x; touchY = event.y
                val count = processTouch(map, event.x, event.y)
                if (count > 0) vibrate()
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                isTouching = false
                touchX = -1f; touchY = -1f
                invalidate()
            }
        }
        return true
    }

    private fun processTouch(map: DeadZoneMap, x: Float, y: Float): Int {
        val centerCol = ((x - offsetX) / cellSize).toInt().coerceIn(0, COLS - 1)
        val centerRow = ((y - offsetY) / cellSize).toInt().coerceIn(0, ROWS - 1)
        var newCount = 0

        for (dr in -BRUSH_RADIUS..BRUSH_RADIUS) {
            for (dc in -BRUSH_RADIUS..BRUSH_RADIUS) {
                val r = centerRow + dr
                val c = centerCol + dc
                if (r in 0 until ROWS && c in 0 until COLS) {
                    val cell = map.cellAt(r, c)
                    cell.tapAttempts++
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
