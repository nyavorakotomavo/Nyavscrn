package com.nyavo.nrscreen.test

import android.os.Bundle
import android.view.View
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

        val miniGrid = findViewById<MiniGridView>(R.id.miniGrid)
        val statsText = findViewById<TextView>(R.id.statsText)
        val legendContainer = findViewById<LinearLayout>(R.id.legendContainer)
        val map = DeadZoneMapHolder.current

        if (map == null) {
            statsText.text = "Aucun resultat. Veuillez refaire le test."
            return
        }

        miniGrid.deadZoneMap = map

        val deadCount = map.deadCells().size
        val suspectCount = map.suspectCells().size
        val degradedCount = map.degradedCells().size
        val aliveCount = map.allCells().count { it.state == ZoneState.ALIVE }
        val total = map.rows * map.cols
        val percent = map.deadPercentage()

        statsText.text = buildString {
            appendLine("Zones mortes : $deadCount")
            appendLine("Zones degradees : $degradedCount")
            appendLine("Zones suspectes : $suspectCount")
            appendLine("Zones OK : $aliveCount")
            appendLine("Total teste : ${deadCount + degradedCount + suspectCount + aliveCount} / $total")
            appendLine("Taux de casse : ${"%.1f".format(percent)}%")
        }

        addLegendItem(legendContainer, R.color.cyan_400, "Fonctionnelle")
        addLegendItem(legendContainer, R.color.void_500, "Morte (ne repond pas)")
        addLegendItem(legendContainer, R.color.nebula_700, "Degradee (3+ essais)")
        addLegendItem(legendContainer, R.color.star_400, "Suspecte (instable)")
        addLegendItem(legendContainer, R.color.nebula_600, "Fantome (tappe seule)")
        addLegendItem(legendContainer, R.color.cosmos_800, "Non testee")
    }

    private fun addLegendItem(container: LinearLayout, colorRes: Int, label: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 12, 0, 12)
        }

        val square = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(56, 56).apply {
                marginEnd = 24
            }
            setBackgroundColor(ContextCompat.getColor(this@DeadZoneMapActivity, colorRes))
        }

        val text = TextView(this).apply {
            this.text = label
            setTextAppearance(R.style.TextAppearance_Nyavscrn_BodyLarge)
            setTextColor(ContextCompat.getColor(this@DeadZoneMapActivity, R.color.text_primary))
        }

        row.addView(square)
        row.addView(text)
        container.addView(row)
    }
}
