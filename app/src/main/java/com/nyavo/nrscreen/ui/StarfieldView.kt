package com.nyavo.nrscreen.ui

import android.content.Context
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class StarfieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), SensorEventListener {

    private val stars = mutableListOf<Star>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val random = Random(System.currentTimeMillis())
    private var time = 0f

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var tiltX = 0f
    private var tiltY = 0f
    private var targetTiltX = 0f
    private var targetTiltY = 0f

    private data class Star(
        var x: Float, var y: Float,
        var size: Float, var alpha: Int,
        var speed: Float, var twinkleOffset: Float,
        var depth: Float
    )

    init {
        generateStars()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    private fun generateStars() {
        stars.clear()
        repeat(120) {
            stars.add(Star(
                x = random.nextFloat(), y = random.nextFloat(),
                size = random.nextFloat() * 3f + 0.3f,
                alpha = random.nextInt(30, 220),
                speed = random.nextFloat() * 0.2f + 0.05f,
                twinkleOffset = random.nextFloat() * 6.28f,
                depth = random.nextFloat() * 0.8f + 0.2f
            ))
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        generateStars()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        time += 0.016f

        tiltX += (targetTiltX - tiltX) * 0.06f
        tiltY += (targetTiltY - tiltY) * 0.06f

        stars.forEach { star ->
            star.y += star.speed * 0.0005f
            if (star.y > 1f) star.y -= 1f

            val parallaxX = tiltX * star.depth * 30f
            val parallaxY = tiltY * star.depth * 30f

            val drawX = (star.x + parallaxX * 0.01f).coerceIn(0f, 1f) * width
            val drawY = (star.y + parallaxY * 0.01f).coerceIn(0f, 1f) * height

            val twinkle = (kotlin.math.sin(time * star.speed * 3f + star.twinkleOffset) * 0.5f + 0.5f)
            val currentAlpha = (star.alpha * twinkle).toInt().coerceIn(15, 255)

            val blueTint = (star.depth * 40).toInt()
            paint.color = Color.argb(currentAlpha, 200 + blueTint, 220 + blueTint, 255)
            canvas.drawCircle(drawX, drawY, star.size, paint)

            if (star.size > 2f) {
                paint.color = Color.argb((currentAlpha * 0.15f).toInt(), 200, 220, 255)
                canvas.drawCircle(drawX, drawY, star.size * 2.5f, paint)
            }
        }

        invalidate()
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
