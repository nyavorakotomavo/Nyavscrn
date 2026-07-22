package com.nyavo.nrscreen.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class StarfieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val stars = mutableListOf<Star>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val random = Random(System.currentTimeMillis())
    private var time = 0f

    private data class Star(
        var x: Float,
        var y: Float,
        var size: Float,
        var alpha: Int,
        var speed: Float,
        var twinkleOffset: Float
    )

    init {
        generateStars()
    }

    private fun generateStars() {
        stars.clear()
        repeat(80) {
            stars.add(Star(
                x = random.nextFloat(),
                y = random.nextFloat(),
                size = random.nextFloat() * 2.5f + 0.5f,
                alpha = random.nextInt(80, 200),
                speed = random.nextFloat() * 0.3f + 0.1f,
                twinkleOffset = random.nextFloat() * 6.28f
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

        stars.forEach { star ->
            val twinkle = (kotlin.math.sin(time * 2f + star.twinkleOffset) * 0.5f + 0.5f)
            val currentAlpha = (star.alpha * twinkle).toInt().coerceIn(20, 255)
            paint.color = Color.argb(currentAlpha, 200, 220, 255)
            canvas.drawCircle(
                star.x * width,
                star.y * height,
                star.size,
                paint
            )
        }

        invalidate()
    }
}
