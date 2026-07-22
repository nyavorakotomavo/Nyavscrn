package com.nyavo.nrscreen.test

enum class ZoneState { UNTESTED, ALIVE, SUSPECT, DEAD, GHOST, DEGRADED }

data class GridCell(
    val row: Int,
    val col: Int,
    var state: ZoneState = ZoneState.UNTESTED,
    var confidence: Float = 1.0f,
    var missedTapCount: Int = 0,
    var lastUpdatedTimestamp: Long = 0L,
    var activatedAt: Long = 0L,
    var tapAttempts: Int = 0,
    var selfActivated: Boolean = false
)

class DeadZoneMap(val rows: Int, val cols: Int) {
    private val cells: List<List<GridCell>> = List(rows) { r ->
        List(cols) { c -> GridCell(r, c) }
    }

    fun cellAt(row: Int, col: Int): GridCell = cells[row][col]
    fun allCells(): List<GridCell> = cells.flatten()
    fun deadCells(): List<GridCell> = cells.flatten().filter { it.state == ZoneState.DEAD }
    fun suspectCells(): List<GridCell> = cells.flatten().filter { it.state == ZoneState.SUSPECT }
    fun degradedCells(): List<GridCell> = cells.flatten().filter { it.state == ZoneState.DEGRADED }
    fun ghostCells(): List<GridCell> = cells.flatten().filter { it.state == ZoneState.GHOST }
    fun aliveCells(): List<GridCell> = cells.flatten().filter { it.state == ZoneState.ALIVE }
    fun untestedCells(): List<GridCell> = cells.flatten().filter { it.state == ZoneState.UNTESTED }
    fun deadPercentage(): Float = if (rows * cols == 0) 0f else (deadCells().size.toFloat() / (rows * cols)) * 100f
    fun coveragePercentage(): Float = if (rows * cols == 0) 0f else {
        val tested = allCells().count { it.state != ZoneState.UNTESTED }
        tested.toFloat() / (rows * cols) * 100f
    }

    fun recordMissedTap(row: Int, col: Int) {
        if (row in 0 until rows && col in 0 until cols) {
            val cell = cells[row][col]
            cell.missedTapCount++
            cell.lastUpdatedTimestamp = System.currentTimeMillis()
            if (cell.state == ZoneState.ALIVE && cell.missedTapCount >= 2) {
                cell.state = ZoneState.SUSPECT
            }
        }
    }

    fun markSelfActivated(row: Int, col: Int) {
        if (row in 0 until rows && col in 0 until cols) {
            val cell = cells[row][col]
            cell.selfActivated = true
            if (cell.state == ZoneState.UNTESTED) {
                cell.state = ZoneState.GHOST
                cell.activatedAt = System.currentTimeMillis()
            }
        }
    }

    fun finalizeTest() {
        cells.flatten().forEach { cell ->
            when {
                cell.state == ZoneState.UNTESTED -> cell.state = ZoneState.DEAD
                cell.state == ZoneState.ALIVE && cell.tapAttempts >= 3 -> cell.state = ZoneState.DEGRADED
                cell.state == ZoneState.ALIVE && cell.selfActivated -> cell.state = ZoneState.GHOST
                cell.state == ZoneState.SUSPECT && cell.missedTapCount >= 3 -> cell.state = ZoneState.DEAD
            }
        }
    }
}
