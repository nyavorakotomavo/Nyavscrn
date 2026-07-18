package com.nyavo.nrscreen.test

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.nyavo.nrscreen.R
import com.nyavo.nrscreen.data.DeadZoneMapHolder

class GridTestActivity : AppCompatActivity() {

    private lateinit var gridTestView: GridTestView
    private lateinit var finishButton: Button
    private val handler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable { finalizeTest() }

    companion object {
        private const val ROWS = 12
        private const val COLS = 6
        private const val TIMEOUT_MS = 15000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grid_test)

        val map = DeadZoneMap(ROWS, COLS)
        DeadZoneMapHolder.current = map

        gridTestView = findViewById(R.id.gridTestView)
        finishButton = findViewById(R.id.finishButton)

        gridTestView.deadZoneMap = map
        gridTestView.onCellTapped = { _, _ -> }

        finishButton.setOnClickListener {
            handler.removeCallbacks(timeoutRunnable)
            finalizeTest()
        }

        handler.postDelayed(timeoutRunnable, TIMEOUT_MS)
    }

    private fun finalizeTest() {
        DeadZoneMapHolder.current?.finalizeUntested()
        val intent = Intent(this, DeadZoneMapActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timeoutRunnable)
    }
}
