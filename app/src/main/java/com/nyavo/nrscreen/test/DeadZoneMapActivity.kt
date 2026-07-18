package com.nyavo.nrscreen.test

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.nyavo.nrscreen.R
import com.nyavo.nrscreen.data.DeadZoneMapHolder

class DeadZoneMapActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deadzone_map)

        val summaryText = findViewById<TextView>(R.id.summaryText)
        val map = DeadZoneMapHolder.current

        if (map == null) {
            summaryText.text = "Aucun résultat disponible. Veuillez refaire le test."
            return
        }

        val deadCount = map.deadCells().size
        val total = map.rows * map.cols
        val percentage = map.deadPercentage()
        val all = map.allCells()

        summaryText.text = buildString {
            append("Zones mortes détectées : $deadCount / $total\n")
            append("Pourcentage : ${"%.1f".format(percentage)}%\n\n")
            append("Détail par état :\n")
            append("- ALIVE : ${all.count { it.state == ZoneState.ALIVE }}\n")
            append("- DEAD : $deadCount\n")
            append("- SUSPECT : ${map.suspectCells().size}\n")
            append("- UNTESTED : ${all.count { it.state == ZoneState.UNTESTED }}")
        }
    }
}
