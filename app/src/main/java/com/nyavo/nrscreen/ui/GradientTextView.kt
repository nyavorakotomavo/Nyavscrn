package com.nyavo.nrscreen.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.nyavo.nrscreen.R

class GradientTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var startColor = Color.CYAN
    private var endColor = Color.MAGENTA
    private var gradientAngle = 0f

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.GradientTextView)
        startColor = typedArray.getColor(R.styleable.GradientTextView_startColor, Color.CYAN)
        endColor = typedArray.getColor(R.styleable.GradientTextView_endColor, Color.MAGENTA)
        gradientAngle = typedArray.getFloat(R.styleable.GradientTextView_gradientAngle, 0f)
        typedArray.recycle()
    }

    override fun onDraw(canvas: Canvas) {
        val paint = this.paint
        val width = measuredWidth.toFloat()
        val height = measuredHeight.toFloat()

        val angleRad = Math.toRadians(gradientAngle.toDouble())
        val x1 = (width / 2 - Math.cos(angleRad) * width / 2).toFloat()
        val y1 = (height / 2 - Math.sin(angleRad) * height / 2).toFloat()
        val x2 = (width / 2 + Math.cos(angleRad) * width / 2).toFloat()
        val y2 = (height / 2 + Math.sin(angleRad) * height / 2).toFloat()

        val shader = LinearGradient(
            x1, y1, x2, y2,
            startColor, endColor,
            Shader.TileMode.CLAMP
        )
        paint.shader = shader

        super.onDraw(canvas)
        paint.shader = null
    }
}
