package com.nyavo.nrscreen.test

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class GridTestView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var deadZoneMap: DeadZoneMap? = null
    var onCellTapped: ((Int, Int) -> Unit)? = null

    private val paintAlive = Paint().apply { color = Color.parseColor("#4CAF50") }
    private val paintDead = Paint().apply { color = Color.parseColor("#F44336") }
    private val paintUntested = Paint().apply { color = Color.parseColor("#9E9E9E") }
    private val paintSuspect = Paint().apply { color = Color.parseColor("#FF9800") }
    private val paintGrid = Paint().apply {
        color = Color.BLACK
        strokeWidth = 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val map = deadZoneMap ?: return
        val cellWidth = width.toFloat() / map.cols
        val cellHeight = height.toFloat() / map.rows

        for (r in 0 until map.rows) {
            for (c in 0 until map.cols) {
                val cell = map.cellAt(r, c)
                val left = c * cellWidth
                val top = r * cellHeight
                val right = left + cellWidth
                val bottom = top + cellHeight

                val paint = when (cell.state) {
                    ZoneState.ALIVE -> paintAlive
                    ZoneState.DEAD -> paintDead
                    ZoneState.SUSPECT -> paintSuspect
                    ZoneState.UNTESTED -> paintUntested
                }
                canvas.drawRect(left, top, right, bottom, paint)
                canvas.drawRect(left, top, right, bottom, paintGrid)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val map = deadZoneMap ?: return true
            val cellWidth = width.toFloat() / map.cols
            val cellHeight = height.toFloat() / map.rows
            val col = (event.x / cellWidth).toInt().coerceIn(0, map.cols - 1)
            val row = (event.y / cellHeight).toInt().coerceIn(0, map.rows - 1)
            val cell = map.cellAt(row, col)
            if (cell.state == ZoneState.UNTESTED) {
                cell.state = ZoneState.ALIVE
                cell.lastUpdatedTimestamp = System.currentTimeMillis()
                invalidate()
                onCellTapped?.invoke(row, col)
            }
        }
        return true
    }
}
