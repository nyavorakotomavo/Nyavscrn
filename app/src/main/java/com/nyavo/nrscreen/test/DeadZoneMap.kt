package com.nyavo.nrscreen.test

enum class ZoneState { UNTESTED, ALIVE, SUSPECT, DEAD }

data class GridCell(
    val row: Int,
    val col: Int,
    var state: ZoneState = ZoneState.UNTESTED,
    var confidence: Float = 1.0f,
    var missedTapCount: Int = 0,
    var lastUpdatedTimestamp: Long = 0L
)

class DeadZoneMap(val rows: Int, val cols: Int) {
    private val cells: List<List<GridCell>> = List(rows) { r ->
        List(cols) { c -> GridCell(r, c) }
    }

    fun cellAt(row: Int, col: Int): GridCell = cells[row][col]
    fun allCells(): List<GridCell> = cells.flatten()
    fun deadCells(): List<GridCell> = cells.flatten().filter { it.state == ZoneState.DEAD }
    fun suspectCells(): List<GridCell> = cells.flatten().filter { it.state == ZoneState.SUSPECT }
    fun deadPercentage(): Float = if (rows * cols == 0) 0f else (deadCells().size.toFloat() / (rows * cols)) * 100f

    fun recordMissedTap(row: Int, col: Int) {
        if (row in 0 until rows && col in 0 until cols) {
            val cell = cells[row][col]
            cell.missedTapCount++
            cell.lastUpdatedTimestamp = System.currentTimeMillis()
            if (cell.state == ZoneState.ALIVE) {
                cell.state = ZoneState.SUSPECT
            }
        }
    }

    fun finalizeUntested() {
        cells.flatten().forEach {
            if (it.state == ZoneState.UNTESTED) {
                it.state = ZoneState.DEAD
            }
        }
    }
}
