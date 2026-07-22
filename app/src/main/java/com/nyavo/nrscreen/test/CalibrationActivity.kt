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
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.nyavo.nrscreen.R
import com.nyavo.nrscreen.data.DeadZoneMapHolder

class CalibrationActivity : AppCompatActivity() {

    private lateinit var gridTestView: GridTestView
    private lateinit var countdownText: TextView
    private lateinit var countdownNumber: TextView
    private lateinit var countdownProgress: ProgressBar
    private val handler = Handler(Looper.getMainLooper())
    private var secondsLeft = 5
    private val totalSeconds = 5

    private val countdownRunnable = object : Runnable {
        override fun run() {
            if (secondsLeft > 0) {
                countdownNumber.text = secondsLeft.toString()
                countdownNumber.startAnimation(AnimationUtils.loadAnimation(this@CalibrationActivity, R.anim.count_bounce))
                countdownProgress.progress = secondsLeft
                secondsLeft--
                handler.postDelayed(this, 1000)
            } else {
                countdownNumber.text = ""
                countdownText.text = "Analyse terminee"
                countdownText.startAnimation(AnimationUtils.loadAnimation(this@CalibrationActivity, R.anim.fade_in))
                handler.postDelayed({
                    val intent = Intent(this@CalibrationActivity, GridTestActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    finish()
                }, 800)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calibration)
        enterImmersiveMode()

        gridTestView = findViewById(R.id.gridTestView)
        countdownText = findViewById(R.id.countdownText)
        countdownNumber = findViewById(R.id.countdownNumber)
        countdownProgress = findViewById(R.id.countdownProgress)

        countdownProgress.max = totalSeconds
        countdownProgress.progress = totalSeconds

        gridTestView.ghostDetectionMode = true

        gridTestView.post {
            val map = gridTestView.deadZoneMap
            if (map != null) {
                DeadZoneMapHolder.current = map
            }
        }

        countdownNumber.text = totalSeconds.toString()
        countdownText.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in))
        handler.postDelayed(countdownRunnable, 1000)
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
        handler.removeCallbacks(countdownRunnable)
    }
}
