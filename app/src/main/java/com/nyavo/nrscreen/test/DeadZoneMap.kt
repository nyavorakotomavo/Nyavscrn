package com.nyavo.nrscreen.test

enum class ZoneState { UNTESTED, ALIVE, DEAD, SUSPECT, GHOST, DEGRADED }

data class GridCell(
    val row: Int,
    val col: Int,
    var state: ZoneState = ZoneState.UNTESTED,
    var activatedAt: Long = 0L,
    var tapCount: Int = 0,
    var selfActivated: Boolean = false
)

class DeadZoneMap(val rows: Int, val cols: Int) {
    val cells: Array<Array<GridCell>> = Array(rows) { r -> Array(cols) { c -> GridCell(r, c) } }

    fun cellAt(row: Int, col: Int): GridCell = cells[row][col]

    fun allCells(): List<GridCell> = cells.asSequence().flatMap { it.asSequence() }.toList()
    fun deadCells(): List<GridCell> = allCells().filter { it.state == ZoneState.DEAD }
    fun suspectCells(): List<GridCell> = allCells().filter { it.state == ZoneState.SUSPECT }
    fun degradedCells(): List<GridCell> = allCells().filter { it.state == ZoneState.DEGRADED }
    fun ghostCells(): List<GridCell> = allCells().filter { it.state == ZoneState.GHOST }

    fun alivePercentage(): Float {
        val total = rows * cols
        return if (total == 0) 0f else (allCells().count { it.state == ZoneState.ALIVE }.toFloat() / total) * 100f
    }

    fun deadPercentage(): Float {
        val total = rows * cols
        return if (total == 0) 0f else (deadCells().size.toFloat() / total) * 100f
    }

    fun coveragePercentage(): Float {
        val total = rows * cols
        val tested = allCells().count { it.state != ZoneState.UNTESTED }
        return if (total == 0) 0f else (tested.toFloat() / total) * 100f
    }

    fun markCellTouched(row: Int, col: Int): Boolean {
        if (row !in 0 until rows || col !in 0 until cols) return false
        val cell = cells[row][col]
        cell.tapCount++
        cell.lastTouchedAt = System.currentTimeMillis()
        if (cell.state == ZoneState.UNTESTED) {
            cell.state = ZoneState.ALIVE
            cell.activatedAt = System.currentTimeMillis()
            return true
        }
        return false
    }

    fun finalizeTest() {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val cell = cells[r][c]
                when {
                    cell.state == ZoneState.UNTESTED -> cell.state = ZoneState.DEAD
                    cell.selfActivated && cell.state == ZoneState.ALIVE -> cell.state = ZoneState.GHOST
                    cell.tapCount >= 3 && cell.state == ZoneState.ALIVE -> cell.state = ZoneState.DEGRADED
                }
            }
        }
    }
}
