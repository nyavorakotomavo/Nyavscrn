package com.nyavo.nrscreen.test

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.nyavo.nrscreen.R
import com.nyavo.nrscreen.ui.GradientTextView
import com.nyavo.nrscreen.ui.OnboardingIllustrationView

class OnboardingActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var nextButton: Button
    private lateinit var skipText: TextView
    private lateinit var dots: List<View>
    private lateinit var prefs: SharedPreferences

    private val pages = listOf(
        Page(
            "Bienvenue dans Nyavscrn",
            "Votre ecran est casse ? Cette app va vous aider a continuer a utiliser votre telephone en identifiant les zones mortes.",
            R.color.nebula_500
        ),
        Page(
            "Comment faire le test",
            "Balayez l'ecran avec votre doigt. Les zones qui repondent deviendront cyan. Les zones mortes resteront noires.",
            R.color.cyan_500
        ),
        Page(
            "Comprendre les resultats",
            "Cyan = Fonctionnel, Rouge = Mort, Rose = Suspect, Violet = Fantome. Le rapport vous montrera exactement quelles zones eviter.",
            R.color.star_500
        )
    )

    private var currentPage = 0

    data class Page(val title: String, val desc: String, val colorRes: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("nyavscrn_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("has_seen_onboarding", false)) {
            startActivity(Intent(this, CalibrationActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_onboarding)

        recyclerView = findViewById(R.id.recyclerView)
        nextButton = findViewById(R.id.nextButton)
        skipText = findViewById(R.id.skipText)
        dots = listOf(findViewById(R.id.dot0), findViewById(R.id.dot1), findViewById(R.id.dot2))

        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        PagerSnapHelper().attachToRecyclerView(recyclerView)
        recyclerView.adapter = OnboardingAdapter(pages)

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val newPage = layoutManager.findFirstVisibleItemPosition()
                    if (newPage != currentPage) {
                        currentPage = newPage
                        updateIndicators()
                    }
                }
            }
        })

        nextButton.setOnClickListener {
            if (currentPage < pages.size - 1) {
                recyclerView.smoothScrollToPosition(currentPage + 1)
            } else {
                prefs.edit().putBoolean("has_seen_onboarding", true).apply()
                startActivity(Intent(this, CalibrationActivity::class.java))
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                finish()
            }
        }

        skipText.setOnClickListener {
            prefs.edit().putBoolean("has_seen_onboarding", true).apply()
            startActivity(Intent(this, CalibrationActivity::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        }

        updateIndicators()
    }

    private fun updateIndicators() {
        dots.forEachIndexed { index, view ->
            val targetWidth = if (index == currentPage) (24 * resources.displayMetrics.density).toInt() else (8 * resources.displayMetrics.density).toInt()
            val targetColor = if (index == currentPage) pages[currentPage].colorRes else R.color.cosmos_700

            view.animate()
                .scaleX(if (index == currentPage) 1f else 0.8f)
                .scaleY(if (index == currentPage) 1f else 0.8f)
                .setDuration(200)
                .start()

            val layoutParams = view.layoutParams
            layoutParams.width = targetWidth
            view.layoutParams = layoutParams
            view.setBackgroundColor(ContextCompat.getColor(this, targetColor))
        }

        nextButton.text = if (currentPage == pages.size - 1) "Commencer" else "Suivant"
        skipText.visibility = if (currentPage == pages.size - 1) View.GONE else View.VISIBLE
    }

    class OnboardingAdapter(private val pages: List<Page>) :
        RecyclerView.Adapter<OnboardingAdapter.VH>() {

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val illustration: OnboardingIllustrationView = view.findViewById(R.id.illustration)
            val title: GradientTextView = view.findViewById(R.id.titleText)
            val desc: TextView = view.findViewById(R.id.descText)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_onboarding_page, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val page = pages[position]
            holder.title.text = page.title
            holder.desc.text = page.desc
            holder.illustration.setPage(position)

            holder.itemView.startAnimation(
                AnimationUtils.loadAnimation(holder.itemView.context, R.anim.scale_in)
            )
        }

        override fun getItemCount() = pages.size
    }
}
