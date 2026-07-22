package com.nyavo.nrscreen.test

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.nyavo.nrscreen.R
import com.nyavo.nrscreen.data.DeadZoneMapHolder

class GridTestActivity : AppCompatActivity() {

    private lateinit var gridTestView: GridTestView
    private lateinit var finishButton: MaterialButton
    private lateinit var instructionContainer: LinearLayout
    private lateinit var instructionText: TextView
    private lateinit var coverageBar: View
    private lateinit var coverageText: TextView
    private lateinit var cellsText: TextView
    private val handler = Handler(Looper.getMainLooper())

    private var lastDiscoveryTime = 0L
    private var discoveredCount = 0
    private var totalCells = 0

    private val completionChecker = object : Runnable {
        override fun run() {
            val map = gridTestView.deadZoneMap ?: return
            val percent = discoveredCount.toFloat() / totalCells
            val timeSinceLastDiscovery = System.currentTimeMillis() - lastDiscoveryTime

            updateHUD(percent, discoveredCount, totalCells)

            if (percent > 0.60 && timeSinceLastDiscovery > 5000 && finishButton.visibility != View.VISIBLE) {
                showFinishButton()
            }

            handler.postDelayed(this, 200)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grid_test)
        enterImmersiveMode()

        gridTestView = findViewById(R.id.gridTestView)
        finishButton = findViewById(R.id.finishButton)
        instructionContainer = findViewById(R.id.instructionContainer)
        instructionText = findViewById(R.id.instructionText)
        coverageBar = findViewById(R.id.coverageBar)
        coverageText = findViewById(R.id.coverageText)
        cellsText = findViewById(R.id.cellsText)

        val existingMap = DeadZoneMapHolder.current
        if (existingMap != null) {
            gridTestView.deadZoneMap = existingMap
            totalCells = existingMap.rows * existingMap.cols
        }

        gridTestView.onCellDiscovered = { count ->
            discoveredCount += count
            lastDiscoveryTime = System.currentTimeMillis()
            if (finishButton.visibility == View.VISIBLE) {
                hideFinishButton()
            }
        }

        finishButton.setOnClickListener {
            handler.removeCallbacks(completionChecker)
            gridTestView.finalizeTest()
            val intent = Intent(this, DeadZoneMapActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        }

        finishButton.visibility = View.GONE
        instructionText.text = getString(R.string.test_instruction)
        lastDiscoveryTime = System.currentTimeMillis()
        handler.post(completionChecker)
    }

    private fun updateHUD(percent: Float, discovered: Int, total: Int) {
        val percentInt = (percent * 100).toInt()
        coverageText.text = "$percentInt%"
        cellsText.text = "$discovered / $total"

        val barWidth = (resources.displayMetrics.widthPixels * percent).toInt()
        val params = coverageBar.layoutParams
        params.width = barWidth
        coverageBar.layoutParams = params

        coverageText.setTextColor(when {
            percent < 0.3 -> getColor(R.color.cyan_400)
            percent < 0.6 -> getColor(R.color.star_400)
            else -> getColor(R.color.nebula_400)
        })
    }

    private fun showFinishButton() {
        finishButton.visibility = View.VISIBLE
        finishButton.startAnimation(AnimationUtils.loadAnimation(this, R.anim.button_reveal))
        instructionContainer.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out))
        instructionContainer.visibility = View.INVISIBLE
        instructionText.text = "Balayage termine. Appuyez pour voir le rapport."
        instructionContainer.visibility = View.VISIBLE
        instructionContainer.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in))
    }

    private fun hideFinishButton() {
        finishButton.visibility = View.GONE
        instructionText.text = getString(R.string.test_instruction)
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
