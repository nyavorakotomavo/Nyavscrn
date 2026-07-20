package com.nyavo.nrscreen.test

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.nyavo.nrscreen.R
import com.nyavo.nrscreen.data.DeadZoneMapHolder

class DeadZoneMapActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deadzone_map)

        val reportView = findViewById<DeadZoneReportView>(R.id.reportView)
        val summaryText = findViewById<TextView>(R.id.summaryText)
        val legendContainer = findViewById<LinearLayout>(R.id.legendContainer)
        val map = DeadZoneMapHolder.current

        if (map == null) {
            summaryText.text = "Aucun resultat disponible. Veuillez refaire le test."
            return
        }

        reportView.deadZoneMap = map

        val deadCount = map.deadCells().size
        val suspectCount = map.suspectCells().size
        val aliveCount = map.allCells().count { it.state == ZoneState.ALIVE }
        val total = map.rows * map.cols
        val percentage = map.deadPercentage()

        summaryText.text = buildString {
            append("Zones mortes detectees : $deadCount / $total\n")
            append("Zones suspectes : $suspectCount\n")
            append("Zones fonctionnelles : $aliveCount\n")
            append("Taux de casse : ${"%.1f".format(percentage)}%")
        }

        addLegendItem(legendContainer, R.color.cyan_400, "Fonctionnelle (testee)")
        addLegendItem(legendContainer, R.color.void_500, "Morte (non reactive)")
        addLegendItem(legendContainer, R.color.star_400, "Suspecte (degradation)")
        addLegendItem(legendContainer, R.color.cosmos_800, "Non testee (inconnu)")
    }

    private fun addLegendItem(container: LinearLayout, colorRes: Int, label: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 8)
        }

        val square = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(48, 48).apply {
                marginEnd = 24
            }
            setBackgroundColor(ContextCompat.getColor(this@DeadZoneMapActivity, colorRes))
        }

        val text = TextView(this).apply {
            this.text = label
            textAppearance = R.style.TextAppearance_Nyavscrn_BodyLarge
            setTextColor(ContextCompat.getColor(this@DeadZoneMapActivity, R.color.text_primary))
        }

        row.addView(square)
        row.addView(text)
        container.addView(row)
    }
}
