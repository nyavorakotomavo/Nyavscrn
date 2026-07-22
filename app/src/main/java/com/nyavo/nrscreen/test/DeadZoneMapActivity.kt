package com.nyavo.nrscreen.test

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.nyavo.nrscreen.R
import com.nyavo.nrscreen.data.DeadZoneMapHolder
import com.nyavo.nrscreen.ui.AnimatedCounter
import com.nyavo.nrscreen.ui.DonutChartView
import kotlin.math.roundToInt

class DeadZoneMapActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deadzone_map)

        val miniGrid = findViewById<MiniGridView>(R.id.miniGrid)
        val donutChart = findViewById<DonutChartView>(R.id.donutChart)
        val donutPercent = findViewById<TextView>(R.id.donutPercent)
        val severityIndicator = findViewById<View>(R.id.severityIndicator)
        val legendContainer = findViewById<LinearLayout>(R.id.legendContainer)
        val restartButton = findViewById<MaterialButton>(R.id.restartButton)
        val shareButton = findViewById<MaterialButton>(R.id.shareButton)

        val deadCounter = findViewById<AnimatedCounter>(R.id.deadCounter)
        val degradedCounter = findViewById<AnimatedCounter>(R.id.degradedCounter)
        val suspectCounter = findViewById<AnimatedCounter>(R.id.suspectCounter)
        val aliveCounter = findViewById<AnimatedCounter>(R.id.aliveCounter)

        val map = DeadZoneMapHolder.current

        if (map == null) {
            showEmptyState()
            return
        }

        miniGrid.deadZoneMap = map

        val deadCount = map.deadCells().size
        val suspectCount = map.suspectCells().size
        val degradedCount = map.degradedCells().size
        val ghostCount = map.allCells().count { it.state == ZoneState.GHOST }
        val aliveCount = map.allCells().count { it.state == ZoneState.ALIVE }
        val untestedCount = map.allCells().count { it.state == ZoneState.UNTESTED }
        val total = map.rows * map.cols
        val damagePercent = map.deadPercentage()

        deadCounter.setCount(deadCount, 1000)
        degradedCounter.setCount(degradedCount, 1200)
        suspectCounter.setCount(suspectCount, 1400)
        aliveCounter.setCount(aliveCount, 1600)

        val segments = mutableListOf<DonutChartView.Segment>()
        if (aliveCount > 0) segments.add(DonutChartView.Segment(
            ContextCompat.getColor(this, R.color.cyan_400),
            aliveCount.toFloat(), "OK"
        ))
        if (deadCount > 0) segments.add(DonutChartView.Segment(
            ContextCompat.getColor(this, R.color.void_500),
            deadCount.toFloat(), "Mort"
        ))
        if (degradedCount > 0) segments.add(DonutChartView.Segment(
            ContextCompat.getColor(this, R.color.nebula_700),
            degradedCount.toFloat(), "Degrade"
        ))
        if (suspectCount > 0) segments.add(DonutChartView.Segment(
            ContextCompat.getColor(this, R.color.star_400),
            suspectCount.toFloat(), "Suspect"
        ))
        if (ghostCount > 0) segments.add(DonutChartView.Segment(
            ContextCompat.getColor(this, R.color.nebula_600),
            ghostCount.toFloat(), "Fantome"
        ))
        if (untestedCount > 0) segments.add(DonutChartView.Segment(
            ContextCompat.getColor(this, R.color.cosmos_800),
            untestedCount.toFloat(), "Non teste"
        ))
        donutChart.setData(segments)

        donutPercent.text = "${damagePercent.roundToInt()}%"
        donutPercent.setTextColor(when {
            damagePercent < 10 -> ContextCompat.getColor(this, R.color.cyan_400)
            damagePercent < 30 -> ContextCompat.getColor(this, R.color.star_400)
            else -> ContextCompat.getColor(this, R.color.void_500)
        })

        val severityParams = severityIndicator.layoutParams
        severityParams.width = (resources.displayMetrics.widthPixels * (damagePercent / 100f)).toInt()
        severityIndicator.layoutParams = severityParams
        severityIndicator.setBackgroundColor(when {
            damagePercent < 10 -> ContextCompat.getColor(this, R.color.cyan_400)
            damagePercent < 25 -> ContextCompat.getColor(this, R.color.star_400)
            damagePercent < 50 -> ContextCompat.getColor(this, R.color.nebula_500)
            else -> ContextCompat.getColor(this, R.color.void_500)
        })

        addLegendChip(legendContainer, R.color.cyan_400, "Fonctionnelle", aliveCount)
        addLegendChip(legendContainer, R.color.void_500, "Morte (ne repond pas)", deadCount)
        addLegendChip(legendContainer, R.color.nebula_700, "Degradee (3+ essais)", degradedCount)
        addLegendChip(legendContainer, R.color.star_400, "Suspecte (instable)", suspectCount)
        addLegendChip(legendContainer, R.color.nebula_600, "Fantome (tappe seule)", ghostCount)
        addLegendChip(legendContainer, R.color.cosmos_800, "Non testee", untestedCount)

        restartButton.setOnClickListener {
            DeadZoneMapHolder.current = null
            startActivity(Intent(this, CalibrationActivity::class.java))
            finish()
        }

        shareButton.setOnClickListener {
            shareReport(map, deadCount, degradedCount, suspectCount, aliveCount, total, damagePercent)
        }
    }

    private fun addLegendChip(container: LinearLayout, colorRes: Int, label: String, count: Int) {
        val chip = LayoutInflater.from(this).inflate(R.layout.item_legend_chip, container, false)
        val indicator = chip.findViewById<View>(R.id.chipIndicator)
        val text = chip.findViewById<TextView>(R.id.chipText)
        val countText = chip.findViewById<TextView>(R.id.chipCount)

        indicator.setBackgroundColor(ContextCompat.getColor(this, colorRes))
        text.text = label
        countText.text = count.toString()
        countText.setTextColor(ContextCompat.getColor(this, colorRes))

        container.addView(chip)
    }

    private fun showEmptyState() {
        // Afficher message d'erreur
    }

    private fun shareReport(
        map: DeadZoneMap,
        dead: Int, degraded: Int, suspect: Int, alive: Int,
        total: Int, percent: Float
    ) {
        val report = buildString {
            appendLine("═══ NYAVSCRN - Rapport de diagnostic ═══")
            appendLine()
            appendLine("Resolution : ${map.cols} x ${map.rows} cellules")
            appendLine("Total : $total zones analysees")
            appendLine()
            appendLine("Zones mortes : $dead")
            appendLine("Zones degradees : $degraded")
            appendLine("Zones suspectes : $suspect")
            appendLine("Zones OK : $alive")
            appendLine()
            appendLine("Taux de casse : ${"%.1f".format(percent)}%")
            appendLine()
            appendLine("Recommandation : ${when {
                percent < 10 -> "Ecran en bon etat. Aucune action necessaire."
                percent < 30 -> "Quelques zones mortes detectees. Utilisation possible avec precautions."
                percent < 50 -> "Dommages moderes. Considerer un remplacement prochain."
                else -> "Dommages severes. Remplacement recommande."
            }}")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Rapport Nyavscrn")
            putExtra(Intent.EXTRA_TEXT, report)
        }
        startActivity(Intent.createChooser(shareIntent, "Partager le rapport"))
    }
}
