package com.nyavo.nrscreen.ui

import android.content.Context
import android.util.AttributeSet
import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.widget.AppCompatTextView

class AnimatedCounter @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var animator: ValueAnimator? = null
    private var currentValue = 0

    fun setCount(target: Int, duration: Long = 800) {
        animator?.cancel()
        val startValue = currentValue
        animator = ValueAnimator.ofInt(startValue, target).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator(1.5f)
            addUpdateListener {
                currentValue = it.animatedValue as Int
                text = currentValue.toString()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
