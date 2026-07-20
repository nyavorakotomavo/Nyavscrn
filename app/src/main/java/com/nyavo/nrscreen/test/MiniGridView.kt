package com.nyavo.nrscreen.test

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.nyavo.nrscreen.R

class MiniGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var deadZoneMap: DeadZoneMap? = null
        set(value) {
            field = value
            invalidate()
        }

    private val cellPaint = Paint()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val map = deadZoneMap ?: return
        val cellW = width.toFloat() / map.cols
        val cellH = height.toFloat() / map.rows

        for (r in 0 until map.rows) {
            for (c in 0 until map.cols) {
                val cell = map.cellAt(r, c)
                val left = c * cellW
                val top = r * cellH
                val right = left + cellW
                val bottom = top + cellH

                cellPaint.color = when (cell.state) {
                    ZoneState.UNTESTED -> ContextCompat.getColor(context, R.color.cosmos_800)
                    ZoneState.ALIVE -> ContextCompat.getColor(context, R.color.cyan_400)
                    ZoneState.DEAD -> ContextCompat.getColor(context, R.color.void_500)
                    ZoneState.SUSPECT -> ContextCompat.getColor(context, R.color.star_400)
                    ZoneState.GHOST -> ContextCompat.getColor(context, R.color.nebula_600)
                    ZoneState.DEGRADED -> ContextCompat.getColor(context, R.color.nebula_700)
                }
                canvas.drawRect(left, top, right, bottom, cellPaint)
            }
        }
    }
}
