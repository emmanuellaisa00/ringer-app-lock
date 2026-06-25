package com.example.ringer

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.ringer.databinding.ActivityVolumeBinding

class VolumeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVolumeBinding
    private val barLevels = IntArray(6) { 0 } // 0-7 levels per bar
    private val maxLevel = 7

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVolumeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBars()
    }

    private fun setupBars() {
        val bars = listOf(
            binding.bar1, binding.bar2, binding.bar3,
            binding.bar4, binding.bar5, binding.bar6
        )

        bars.forEachIndexed { index, barContainer ->
            // Create level segments (bottom to top)
            for (level in 0 until maxLevel) {
                val segment = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                    )
                    setBackgroundColor(getColor(R.color.surface_elevated))
                    alpha = 0.3f
                }
                barContainer.addView(segment, 0) // add at top so bottom is first
            }

            // Tap to increase level
            barContainer.setOnClickListener {
                if (barLevels[index] < maxLevel) {
                    barLevels[index]++
                    updateBarDisplay(barContainer, barLevels[index])
                    checkAllMaxed()
                } else {
                    // Already maxed — reset this bar
                    barLevels[index] = 0
                    updateBarDisplay(barContainer, 0)
                }
            }

            // Long press to fill entire bar instantly
            barContainer.setOnLongClickListener {
                barLevels[index] = maxLevel
                updateBarDisplay(barContainer, maxLevel)
                checkAllMaxed()
                true
            }
        }
    }

    private fun updateBarDisplay(barContainer: LinearLayout, level: Int) {
        for (i in 0 until maxLevel) {
            val segment = barContainer.getChildAt(i) as View
            if (i < level) {
                // Filled segment — color based on level
                val colorRes = when {
                    level >= maxLevel -> R.color.primary
                    level > maxLevel / 2 -> R.color.accent_cyan
                    else -> R.color.accent_emerald
                }
                segment.setBackgroundColor(getColor(colorRes))
                segment.alpha = 1f
            } else {
                // Empty segment
                segment.setBackgroundColor(getColor(R.color.surface_elevated))
                segment.alpha = 0.3f
            }
        }
    }

    private fun checkAllMaxed() {
        if (barLevels.all { it >= maxLevel }) {
            // All bars maxed — launch PIN screen
            binding.volumeHint.visibility = View.GONE
            startActivity(Intent(this, PinActivity::class.java))
            // Don't finish — user can come back with back button
        }
    }

    override fun onResume() {
        super.onResume()
        // Reset bars when coming back from PIN or main
        if (barLevels.any { it >= maxLevel }) {
            barLevels.fill(0)
            val bars = listOf(
                binding.bar1, binding.bar2, binding.bar3,
                binding.bar4, binding.bar5, binding.bar6
            )
            bars.forEach { updateBarDisplay(it, 0) }
            binding.volumeHint.visibility = View.VISIBLE
        }
    }
}
