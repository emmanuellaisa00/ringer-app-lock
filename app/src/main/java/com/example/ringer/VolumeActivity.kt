package com.example.ringer

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.ringer.databinding.ActivityVolumeBinding

class VolumeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVolumeBinding
    private val barLevels = IntArray(6) { 0 }
    private val maxLevel = 10
    private var isTransitioning = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVolumeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBars()

        // Entrance animation — fade in bars staggered
        binding.barsContainer.alpha = 0f
        binding.barsContainer.translationY = 30f
        binding.barsContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .start()

        // Speaker icon pulse
        startIconPulse()
    }

    private fun startIconPulse() {
        binding.speakerIcon.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(1500)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                binding.speakerIcon.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(1500)
                    .setInterpolator(DecelerateInterpolator())
                    .withEndAction { startIconPulse() }
                    .start()
            }
            .start()
    }

    private fun setupBars() {
        val bars = listOf(
            binding.bar1, binding.bar2, binding.bar3,
            binding.bar4, binding.bar5, binding.bar6
        )

        bars.forEachIndexed { index, barContainer ->
            // Create 10 rounded segments per bar (bottom to top)
            for (level in 0 until maxLevel) {
                val segment = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                    ).apply {
                        topMargin = if (level < maxLevel - 1) 3 else 0
                        bottomMargin = if (level > 0) 3 else 0
                    }
                    setBackgroundResource(R.drawable.eq_segment_empty)
                    scaleY = 0f
                    alpha = 0f
                }
                barContainer.addView(segment, 0)
            }

            // Tap to increase
            barContainer.setOnClickListener {
                if (isTransitioning) return@setOnClickListener
                if (barLevels[index] < maxLevel) {
                    barLevels[index]++
                    animateSegmentIn(barContainer, barLevels[index] - 1)
                    updateVolumePercent()
                    checkAllMaxed()
                } else {
                    // Reset with animation
                    val oldLevel = barLevels[index]
                    barLevels[index] = 0
                    animateBarReset(barContainer, oldLevel)
                    updateVolumePercent()
                }
            }

            // Long press to fill all
            barContainer.setOnLongClickListener {
                if (isTransitioning) return@setOnLongClickListener true
                val oldLevel = barLevels[index]
                barLevels[index] = maxLevel
                animateBarFill(barContainer, oldLevel, maxLevel)
                updateVolumePercent()
                checkAllMaxed()
                true
            }
        }
    }

    private fun animateSegmentIn(barContainer: LinearLayout, segmentIndex: Int) {
        val segment = barContainer.getChildAt(segmentIndex) as View
        segment.setBackgroundResource(getSegmentDrawable(
            listOf(binding.bar1, binding.bar2, binding.bar3,
                   binding.bar4, binding.bar5, binding.bar6).indexOf(barContainer),
            segmentIndex
        ))
        segment.animate()
            .scaleY(1f)
            .alpha(1f)
            .setDuration(200)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()

        // Brief scale-up on the whole bar
        barContainer.animate()
            .scaleX(1.08f)
            .setDuration(100)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                barContainer.animate()
                    .scaleX(1f)
                    .setDuration(150)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
            .start()
    }

    private fun animateBarReset(barContainer: LinearLayout, fromLevel: Int) {
        for (i in 0 until fromLevel) {
            val segment = barContainer.getChildAt(i) as View
            segment.animate()
                .scaleY(0f)
                .alpha(0f)
                .setDuration(150)
                .setStartDelay((fromLevel - 1 - i) * 20L)
                .withEndAction {
                    segment.setBackgroundResource(R.drawable.eq_segment_empty)
                }
                .start()
        }
    }

    private fun animateBarFill(barContainer: LinearLayout, fromLevel: Int, toLevel: Int) {
        val barIndex = listOf(
            binding.bar1, binding.bar2, binding.bar3,
            binding.bar4, binding.bar5, binding.bar6
        ).indexOf(barContainer)

        for (i in fromLevel until toLevel) {
            val segment = barContainer.getChildAt(i) as View
            segment.setBackgroundResource(getSegmentDrawable(barIndex, i))
            segment.animate()
                .scaleY(1f)
                .alpha(1f)
                .setDuration(120)
                .setStartDelay((i - fromLevel) * 40L)
                .setInterpolator(OvershootInterpolator(0.8f))
                .start()
        }

        // Satisfying bar bounce
        barContainer.animate()
            .scaleX(1.12f)
            .setDuration(120)
            .withEndAction {
                barContainer.animate()
                    .scaleX(1f)
                    .setDuration(200)
                    .setInterpolator(OvershootInterpolator(0.5f))
                    .start()
            }
            .start()
    }

    private fun getSegmentDrawable(barIndex: Int, segmentIndex: Int): Int {
        val ratio = segmentIndex.toFloat() / maxLevel
        return when {
            ratio < 0.35f -> R.drawable.eq_segment_glow_emerald
            ratio < 0.7f -> R.drawable.eq_segment_glow_cyan
            else -> R.drawable.eq_segment_glow_primary
        }
    }

    private fun updateVolumePercent() {
        val totalFilled = barLevels.sum()
        val totalPossible = maxLevel * 6
        val percent = (totalFilled * 100) / totalPossible
        binding.volumePercent.text = "$percent%"

        val color = when {
            percent >= 100 -> getColor(R.color.primary)
            percent >= 60 -> getColor(R.color.accent_cyan)
            percent >= 30 -> getColor(R.color.accent_emerald)
            else -> getColor(R.color.text_hint)
        }
        binding.volumePercent.setTextColor(color)

        // Scale bump on change
        binding.volumePercent.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(100)
            .withEndAction {
                binding.volumePercent.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150)
                    .start()
            }
            .start()
    }

    private fun checkAllMaxed() {
        if (barLevels.all { it >= maxLevel } && !isTransitioning) {
            isTransitioning = true
            binding.volumeHint.visibility = View.GONE

            // Ripple pulse across all bars
            val bars = listOf(
                binding.bar1, binding.bar2, binding.bar3,
                binding.bar4, binding.bar5, binding.bar6
            )
            bars.forEachIndexed { i, bar ->
                bar.animate()
                    .scaleX(1.15f)
                    .scaleY(1.02f)
                    .setDuration(150)
                    .setStartDelay(i * 60L)
                    .withEndAction {
                        bar.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(200)
                            .start()
                    }
                    .start()
            }

            // Flash overlay + slide transition
            binding.transitionOverlay.visibility = View.VISIBLE
            binding.transitionOverlay.animate()
                .alpha(0.3f)
                .setDuration(300)
                .setStartDelay(400)
                .withEndAction {
                    startActivity(Intent(this, PinActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
                .start()
        }
    }

    override fun onResume() {
        super.onResume()
        if (barLevels.any { it > 0 }) {
            val bars = listOf(
                binding.bar1, binding.bar2, binding.bar3,
                binding.bar4, binding.bar5, binding.bar6
            )
            bars.forEachIndexed { index, bar ->
                val oldLevel = barLevels[index]
                if (oldLevel > 0) {
                    animateBarReset(bar, oldLevel)
                }
            }
            barLevels.fill(0)
            binding.volumePercent.text = "0%"
            binding.volumePercent.setTextColor(getColor(R.color.text_hint))
            binding.volumeHint.visibility = View.VISIBLE
            binding.transitionOverlay.visibility = View.GONE
            binding.transitionOverlay.alpha = 0f
            isTransitioning = false
        }

        // Slide-in animation when returning
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
