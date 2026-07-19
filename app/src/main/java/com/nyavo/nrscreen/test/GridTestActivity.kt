package com.nyavo.nrscreen.test

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.nyavo.nrscreen.R
import com.nyavo.nrscreen.data.DeadZoneMapHolder

class GridTestActivity : AppCompatActivity() {

    private lateinit var gridTestView: GridTestView
    private lateinit var finishButton: Button
    private lateinit var instructionText: TextView
    private val handler = Handler(Looper.getMainLooper())

    private var lastDiscoveryTime = 0L
    private var discoveredCount = 0
    private val totalCells = GridTestView.ROWS * GridTestView.COLS

    private val completionChecker = object : Runnable {
        override fun run() {
            val percent = discoveredCount.toFloat() / totalCells
            val timeSinceLastDiscovery = System.currentTimeMillis() - lastDiscoveryTime
            if (percent > 0.60 && timeSinceLastDiscovery > 5000 && finishButton.visibility != View.VISIBLE) {
                finishButton.visibility = View.VISIBLE
                finishButton.alpha = 0f
                finishButton.animate().alpha(1f).setDuration(400).start()
            }
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grid_test)

        val map = DeadZoneMap(GridTestView.ROWS, GridTestView.COLS)
        DeadZoneMapHolder.current = map

        gridTestView = findViewById(R.id.gridTestView)
        finishButton = findViewById(R.id.finishButton)
        instructionText = findViewById(R.id.instructionText)

        gridTestView.deadZoneMap = map
        gridTestView.onCellDiscovered = { count ->
            discoveredCount += count
            lastDiscoveryTime = System.currentTimeMillis()
            if (instructionText.visibility != View.GONE) {
                instructionText.visibility = View.GONE
            }
            if (finishButton.visibility == View.VISIBLE) {
                finishButton.visibility = View.GONE
            }
        }

        finishButton.setOnClickListener {
            handler.removeCallbacks(completionChecker)
            gridTestView.finalizeTest()
            val intent = Intent(this, DeadZoneMapActivity::class.java)
            startActivity(intent)
            finish()
        }

        finishButton.visibility = View.GONE
        lastDiscoveryTime = System.currentTimeMillis()
        handler.post(completionChecker)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(completionChecker)
    }
}
