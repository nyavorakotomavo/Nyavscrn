package com.nyavo.nrscreen.test

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
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
                finishButton.animate().alpha(1f).setDuration(500).start()
                instructionText.text = "Balayage termine. Appuyez pour voir le rapport."
                instructionText.visibility = View.VISIBLE
            }
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grid_test)
        enterImmersiveMode()

        val map = DeadZoneMap(GridTestView.ROWS, GridTestView.COLS)
        DeadZoneMapHolder.current = map

        gridTestView = findViewById(R.id.gridTestView)
        finishButton = findViewById(R.id.finishButton)
        instructionText = findViewById(R.id.instructionText)

        gridTestView.deadZoneMap = map
        gridTestView.onCellDiscovered = { count ->
            discoveredCount += count
            lastDiscoveryTime = System.currentTimeMillis()
            if (finishButton.visibility == View.VISIBLE) {
                finishButton.visibility = View.GONE
                instructionText.text = "Balayez l'ecran sans pause. Ne levez pas le doigt."
                instructionText.visibility = View.VISIBLE
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
        instructionText.text = "Balayez l'ecran sans pause. Ne levez pas le doigt."
        lastDiscoveryTime = System.currentTimeMillis()
        handler.post(completionChecker)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersiveMode()
    }

    private fun enterImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(completionChecker)
    }
}
