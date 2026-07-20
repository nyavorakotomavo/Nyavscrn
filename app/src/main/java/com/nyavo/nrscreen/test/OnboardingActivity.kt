package com.nyavo.nrscreen.test

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.nyavo.nrscreen.R

class OnboardingActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var nextButton: Button
    private lateinit var dots: List<View>

    private val pages = listOf(
        Page(
            "Bienvenue dans Nyavscrn",
            "Votre ecran est casse ? Cette app va vous aider a continuer a utiliser votre telephone.",
            R.color.nebula_500
        ),
        Page(
            "Comment faire le test",
            "Balayez l'ecran avec votre doigt. Les zones qui repondent deviendront cyan. Les zones mortes resteront noires.",
            R.color.cyan_500
        ),
        Page(
            "Comprendre les resultats",
            "Cyan = Fonctionnel, Rouge = Mort, Rose = Suspect. Le rapport vous montrera exactement quelles zones eviter.",
            R.color.star_500
        )
    )

    private var currentPage = 0

    data class Page(val title: String, val desc: String, val colorRes: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        recyclerView = findViewById(R.id.recyclerView)
        nextButton = findViewById(R.id.nextButton)
        dots = listOf(findViewById(R.id.dot0), findViewById(R.id.dot1), findViewById(R.id.dot2))

        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        PagerSnapHelper().attachToRecyclerView(recyclerView)
        recyclerView.adapter = OnboardingAdapter(pages)

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    currentPage = layoutManager.findFirstVisibleItemPosition()
                    updateIndicators()
                }
            }
        })

        nextButton.setOnClickListener {
            if (currentPage < pages.size - 1) {
                recyclerView.smoothScrollToPosition(currentPage + 1)
            } else {
                startActivity(Intent(this, GridTestActivity::class.java))
                finish()
            }
        }

        updateIndicators()
    }

    private fun updateIndicators() {
        dots.forEachIndexed { index, view ->
            val color = if (index == currentPage) pages[currentPage].colorRes else R.color.cosmos_700
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ContextCompat.getColor(this@OnboardingActivity, color))
            }
            view.background = drawable
        }
        nextButton.text = if (currentPage == pages.size - 1) "Commencer" else "Suivant"
    }

    class OnboardingAdapter(private val pages: List<Page>) :
        RecyclerView.Adapter<OnboardingAdapter.VH>() {

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val illustration: View = view.findViewById(R.id.illustration)
            val title: TextView = view.findViewById(R.id.titleText)
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
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16f
                setColor(ContextCompat.getColor(holder.itemView.context, page.colorRes))
            }
            holder.illustration.background = drawable
        }

        override fun getItemCount() = pages.size
    }
}
