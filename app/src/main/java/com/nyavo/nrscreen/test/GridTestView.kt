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

class GridTestView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val ROWS = 60
        const val COLS = 120
        const val BRUSH_RADIUS = 2
        const val RIPPLE_DURATION = 900L
    }

    var deadZoneMap: DeadZoneMap? = null
    var onCellDiscovered: ((Int) -> Unit)? = null

    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private val ripples = mutableListOf<Ripple>()
    private var touchX = -1f
    private var touchY = -1f
    private var isTouching = false

    private val cellPaint = Paint()
    private val ripplePaints = List(4) { Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL } }
    private val glowPaints = List(3) { Paint(Paint.ANTI_ALIAS_FLAG) }
    private val interpolator = DecelerateInterpolator(1.8f)

    private val untestedColor: Int
    private val deadColor: Int
    private val suspectColor: Int
    private val ghostColor: Int
    private val cyan400: Int
    private val cyan500: Int
    private val cyan600: Int
    private val cyan300: Int
    private val nebula400: Int
    private val cosmos950: Int

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
        cyan400 = ContextCompat.getColor(context, R.color.cyan_400)
        cyan500 = ContextCompat.getColor(context, R.color.cyan_500)
        cyan600 = ContextCompat.getColor(context, R.color.cyan_600)
        cyan300 = ContextCompat.getColor(context, R.color.cyan_300)
        nebula400 = ContextCompat.getColor(context, R.color.nebula_400)
        cosmos950 = ContextCompat.getColor(context, R.color.cosmos_950)
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

        if (isTouching && touchX >= 0 && touchY >= 0) {
            val baseR = cellW * 6

            glowPaints[0].shader = RadialGradient(
                touchX, touchY, baseR * 1.6f,
                Color.argb(35, 167, 139, 250),
                Color.argb(0, 11, 15, 31),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(touchX, touchY, baseR * 1.6f, glowPaints[0])

            glowPaints[1].shader = RadialGradient(
                touchX, touchY, baseR,
                Color.argb(70, 34, 211, 238),
                Color.argb(0, 11, 15, 31),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(touchX, touchY, baseR, glowPaints[1])

            glowPaints[2].shader = RadialGradient(
                touchX, touchY, baseR * 0.35f,
                Color.argb(140, 103, 232, 249),
                Color.argb(0, 34, 211, 238),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(touchX, touchY, baseR * 0.35f, glowPaints[2])

            glowPaints.forEach { it.shader = null }
        }

        if (ripples.isNotEmpty() || isTouching) invalidate()
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
        val cellW = width.toFloat() / COLS
        val cellH = height.toFloat() / ROWS

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isTouching = true
                touchX = event.x; touchY = event.y
                val count = processTouch(map, event.x, event.y, cellW, cellH)
                if (count > 0) vibrate()
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                touchX = event.x; touchY = event.y
                val count = processTouch(map, event.x, event.y, cellW, cellH)
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
            vibrator.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(10)
        }
    }

    fun finalizeTest() {
        deadZoneMap?.finalizeUntested()
        invalidate()
    }
}
