package com.nyavo.nrscreen.ui

import android.content.Context
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.nyavo.nrscreen.R
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.random.Random

class OnboardingIllustrationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), SensorEventListener {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var time = 0f
    private var pageIndex = 0
    private val random = Random(System.currentTimeMillis())

    private val gridCols = 12
    private val gridRows = 18
    private val cellStates = Array(gridRows) { Array(gridCols) { random.nextFloat() } }
    private val cellColors = Array(gridRows) { Array(gridCols) { random.nextInt(4) } }
    private val cellPhase = Array(gridRows) { Array(gridCols) { random.nextFloat() * 6.28f } }

    private val dustParticles = mutableListOf<DustParticle>()
    private val dustPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var tiltX = 0f
    private var tiltY = 0f
    private var targetTiltX = 0f
    private var targetTiltY = 0f

    private val nebulaColor = ContextCompat.getColor(context, R.color.nebula_500)
    private val cyanColor = ContextCompat.getColor(context, R.color.cyan_400)
    private val starColor = ContextCompat.getColor(context, R.color.star_400)
    private val voidColor = ContextCompat.getColor(context, R.color.void_500)
    private val cosmosColor = ContextCompat.getColor(context, R.color.cosmos_800)

    private data class DustParticle(
        var x: Float, var y: Float,
        var size: Float, var alpha: Int,
        var vx: Float, var vy: Float,
        var baseX: Float, var baseY: Float,
        var twinkleSpeed: Float, var twinklePhase: Float
    )

    init {
        generateDustParticles()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    private fun generateDustParticles() {
        dustParticles.clear()
        repeat(60) {
            val x = random.nextFloat()
            val y = random.nextFloat()
            dustParticles.add(DustParticle(
                x = x, y = y,
                size = random.nextFloat() * 2.5f + 0.5f,
                alpha = random.nextInt(40, 180),
                vx = (random.nextFloat() - 0.5f) * 0.0003f,
                vy = (random.nextFloat() - 0.5f) * 0.0003f,
                baseX = x, baseY = y,
                twinkleSpeed = random.nextFloat() * 2f + 1f,
                twinklePhase = random.nextFloat() * 6.28f
            ))
        }
    }

    fun setPage(index: Int) {
        pageIndex = index
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        time += 0.016f

        tiltX += (targetTiltX - tiltX) * 0.08f
        tiltY += (targetTiltY - tiltY) * 0.08f

        val cx = width / 2f
        val cy = height / 2f

        when (pageIndex) {
            0 -> drawAnimatedGrid(canvas, cx, cy)
            1 -> drawSwipeGesture(canvas, cx, cy)
            2 -> drawColorGrid(canvas, cx, cy)
        }

        drawDustParticles(canvas)
        invalidate()
    }

    private fun drawAnimatedGrid(canvas: Canvas, cx: Float, cy: Float) {
        val gridW = width * 0.65f
        val gridH = height * 0.75f
        val cellW = gridW / gridCols
        val cellH = gridH / gridRows
        val startX = cx - gridW / 2
        val startY = cy - gridH / 2

        paint.color = Color.argb(30, 17, 24, 39)
        canvas.drawRect(startX - 4, startY - 4, startX + gridW + 4, startY + gridH + 4, paint)

        for (r in 0 until gridRows) {
            for (c in 0 until gridCols) {
                val left = startX + c * cellW
                val top = startY + r * cellH
                val right = left + cellW - 1
                val bottom = top + cellH - 1

                val breath = (sin(time * 0.8f + cellPhase[r][c]) * 0.3f + 0.7f)
                val state = cellColors[r][c]

                val baseColor = when (state) {
                    0 -> cyanColor
                    1 -> nebulaColor
                    2 -> starColor
                    3 -> cosmosColor
                    else -> cyanColor
                }

                val alpha = (breath * 200).toInt().coerceIn(30, 255)
                paint.color = Color.argb(alpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))

                val offsetX = tiltX * (c - gridCols / 2) * 0.5f
                val offsetY = tiltY * (r - gridRows / 2) * 0.5f

                canvas.drawRect(left + offsetX, top + offsetY, right + offsetX, bottom + offsetY, paint)
            }
        }

        val borderAlpha = (sin(time * 1.5f) * 40 + 80).toInt()
        paint.color = Color.argb(borderAlpha, 139, 92, 246)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRect(startX - 2, startY - 2, startX + gridW + 2, startY + gridH + 2, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawSwipeGesture(canvas: Canvas, cx: Float, cy: Float) {
        val gridW = width * 0.65f
        val gridH = height * 0.75f
        val cellW = gridW / 8
        val cellH = gridH / 12
        val startX = cx - gridW / 2
        val startY = cy - gridH / 2

        for (r in 0 until 12) {
            for (c in 0 until 8) {
                val alpha = if ((r + c) % 3 == 0) 25 else 12
                paint.color = Color.argb(alpha, 34, 211, 238)
                val left = startX + c * cellW
                val top = startY + r * cellH
                canvas.drawRect(left, top, left + cellW - 1, top + cellH - 1, paint)
            }
        }

        val trailProgress = (time * 0.4f) % 1.5f
        if (trailProgress <= 1f) {
            val trailPoints = (0..15).map { i ->
                val t = i / 15f * trailProgress
                val x = startX + t * gridW
                val y = startY + gridH * 0.5f + sin(t * 4f) * gridH * 0.25f
                Pair(x, y)
            }

            for (i in 0 until trailPoints.size - 1) {
                val alpha = (255 * (1f - i / 15f) * (1f - trailProgress)).toInt().coerceIn(0, 255)
                paint.strokeWidth = 6f * (1f - i / 15f)
                paint.strokeCap = Paint.Cap.ROUND
                paint.color = Color.argb(alpha, 34, 211, 238)
                canvas.drawLine(trailPoints[i].first, trailPoints[i].second,
                    trailPoints[i + 1].first, trailPoints[i + 1].second, paint)
            }

            if (trailPoints.isNotEmpty()) {
                val last = trailPoints.last()
                paint.color = cyanColor
                canvas.drawCircle(last.first, last.second, 10f, paint)
                paint.color = Color.argb(80, 34, 211, 238)
                canvas.drawCircle(last.first, last.second, 22f, paint)
            }
        }
    }

    private fun drawColorGrid(canvas: Canvas, cx: Float, cy: Float) {
        val cellSize = minOf(width, height) * 0.08f
        val gap = cellSize * 0.15f
        val colors = listOf(cyanColor, voidColor, starColor, nebulaColor, cosmosColor, Color.argb(100, 100, 116, 139))
        val labels = listOf("OK", "MORT", "SUSP", "FANT", "DEG", "N/T")

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = cellSize * 0.35f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.MONOSPACE
        }

        for (i in 0..5) {
            val row = i / 3
            val col = i % 3
            val x = cx - cellSize * 1.8f + col * (cellSize + gap)
            val y = cy - cellSize * 1.2f + row * (cellSize + gap + cellSize * 0.5f)

            val breath = (sin(time * 0.6f + i * 0.8f) * 0.15f + 0.85f)
            val baseColor = colors[i]
            paint.color = Color.argb((255 * breath).toInt(), Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))

            val rect = RectF(x, y, x + cellSize, y + cellSize)
            canvas.drawRoundRect(rect, 6f, 6f, paint)

            paint.color = Color.argb((40 * breath).toInt(), Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
            val glowRect = RectF(x - 4, y - 4, x + cellSize + 4, y + cellSize + 4)
            canvas.drawRoundRect(glowRect, 8f, 8f, paint)

            textPaint.color = if (i == 5) Color.argb(150, 148, 163, 184)
            else Color.argb(220, 248, 250, 252)
            canvas.drawText(labels[i], x + cellSize / 2, y + cellSize + cellSize * 0.45f, textPaint)
        }
    }

    private fun drawDustParticles(canvas: Canvas) {
        dustParticles.forEach { particle ->
            particle.baseX += particle.vx
            particle.baseY += particle.vy

            if (particle.baseX < 0) particle.baseX += 1f
            if (particle.baseX > 1) particle.baseX -= 1f
            if (particle.baseY < 0) particle.baseY += 1f
            if (particle.baseY > 1) particle.baseY -= 1f

            val parallaxX = tiltX * particle.size * 3f
            val parallaxY = tiltY * particle.size * 3f

            particle.x = particle.baseX + parallaxX
            particle.y = particle.baseY + parallaxY

            val twinkle = (sin(time * particle.twinkleSpeed + particle.twinklePhase) * 0.5f + 0.5f)
            val currentAlpha = (particle.alpha * twinkle).toInt().coerceIn(10, 200)

            dustPaint.color = Color.argb(currentAlpha, 200, 220, 255)
            canvas.drawCircle(particle.x * width, particle.y * height, particle.size, dustPaint)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                targetTiltX = it.values[0] / 9.8f
                targetTiltY = it.values[1] / 9.8f
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        sensorManager.unregisterListener(this)
    }
}
