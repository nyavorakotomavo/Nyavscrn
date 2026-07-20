package com.nyavo.nrscreen.test

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.nyavo.nrscreen.R
import java.util.LinkedList

class DeadZoneReportView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var deadZoneMap: DeadZoneMap? = null
        set(value) {
            field = value
            computeBlobs()
            invalidate()
        }

    private val deadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.void_500)
        style = Paint.Style.FILL
    }
    private val deadStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.void_400)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val suspectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.star_400)
        style = Paint.Style.FILL
    }
    private val suspectStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.star_300)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val gridPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.cosmos_800)
        strokeWidth = 0.5f
        alpha = 60
    }

    private var deadBlobs: List<Blob> = emptyList()
    private var suspectBlobs: List<Blob> = emptyList()

    data class Blob(val minRow: Int, val maxRow: Int, val minCol: Int, val maxCol: Int)

    private fun computeBlobs() {
        val map = deadZoneMap ?: return
        deadBlobs = findBlobs(map, ZoneState.DEAD)
        suspectBlobs = findBlobs(map, ZoneState.SUSPECT)
    }

    private fun findBlobs(map: DeadZoneMap, state: ZoneState): List<Blob> {
        val visited = Array(map.rows) { BooleanArray(map.cols) }
        val blobs = mutableListOf<Blob>()

        for (r in 0 until map.rows) {
            for (c in 0 until map.cols) {
                if (visited[r][c]) continue
                val cell = map.cellAt(r, c)
                if (cell.state != state) {
                    visited[r][c] = true
                    continue
                }

                val queue = LinkedList<Pair<Int, Int>>()
                queue.add(r to c)
                visited[r][c] = true
                var minR = r; var maxR = r
                var minC = c; var maxC = c

                while (queue.isNotEmpty()) {
                    val (cr, cc) = queue.poll()
                    minR = minOf(minR, cr); maxR = maxOf(maxR, cr)
                    minC = minOf(minC, cc); maxC = maxOf(maxC, cc)

                    val neighbors = listOf(
                        cr - 1 to cc, cr + 1 to cc,
                        cr to cc - 1, cr to cc + 1
                    )
                    for (nr to nc in neighbors) {
                        if (nr in 0 until map.rows && nc in 0 until map.cols && !visited[nr][nc]) {
                            if (map.cellAt(nr, nc).state == state) {
                                visited[nr][nc] = true
                                queue.add(nr to nc)
                            }
                        }
                    }
                }
                blobs.add(Blob(minR, maxR, minC, maxC))
            }
        }
        return blobs
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val map = deadZoneMap ?: return
        val cellW = width.toFloat() / map.cols
        val cellH = height.toFloat() / map.rows

        canvas.drawColor(ContextCompat.getColor(context, R.color.cosmos_950))

        for (r in 0..map.rows) {
            canvas.drawLine(0f, r * cellH, width.toFloat(), r * cellH, gridPaint)
        }
        for (c in 0..map.cols) {
            canvas.drawLine(c * cellW, 0f, c * cellW, height.toFloat(), gridPaint)
        }

        for (blob in deadBlobs) {
            val l = blob.minCol * cellW
            val t = blob.minRow * cellH
            val r = (blob.maxCol + 1) * cellW
            val b = (blob.maxRow + 1) * cellH
            val rect = RectF(l + 1, t + 1, r - 1, b - 1)
            canvas.drawRect(rect, deadPaint)
            canvas.drawRect(rect, deadStrokePaint)
        }

        for (blob in suspectBlobs) {
            val l = blob.minCol * cellW
            val t = blob.minRow * cellH
            val r = (blob.maxCol + 1) * cellW
            val b = (blob.maxRow + 1) * cellH
            val rect = RectF(l + 1, t + 1, r - 1, b - 1)
            canvas.drawRect(rect, suspectPaint)
            canvas.drawRect(rect, suspectStrokePaint)
        }
    }
}
