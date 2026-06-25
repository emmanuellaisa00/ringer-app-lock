package com.example.ringer

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.ringer.databinding.ActivityPinBinding

class PinActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPinBinding
    private val enteredPin = StringBuilder()
    private val correctPin by lazy { getString(R.string.pin_default) }
    private val dots by lazy {
        listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4)
    }
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupKeys()
        entranceAnimation()
    }

    private fun entranceAnimation() {
        // Pin dots slide down from above
        binding.pinDots.alpha = 0f
        binding.pinDots.translationY = -20f
        binding.pinDots.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .start()
    }

    private fun setupKeys() {
        val keys = listOf(
            binding.key0, binding.key1, binding.key2, binding.key3,
            binding.key4, binding.key5, binding.key6, binding.key7,
            binding.key8, binding.key9
        )

        keys.forEach { key ->
            key.setOnClickListener {
                if (enteredPin.length < 4) {
                    enteredPin.append((it as TextView).text)
                    updateDots()
                    // Tap feedback — scale pulse
                    key.animate()
                        .scaleX(0.9f)
                        .scaleY(0.9f)
                        .setDuration(80)
                        .withEndAction {
                            key.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(120)
                                .setInterpolator(OvershootInterpolator(1.5f))
                                .start()
                        }
                        .start()
                    if (enteredPin.length == 4) {
                        handler.postDelayed({ checkPin() }, 200)
                    }
                }
            }
        }

        binding.keyDelete.setOnClickListener {
            if (enteredPin.isNotEmpty()) {
                enteredPin.deleteCharAt(enteredPin.length - 1)
                updateDots()
                binding.pinError.visibility = View.INVISIBLE
            }
        }
    }

    private fun updateDots() {
        for (i in dots.indices) {
            if (i < enteredPin.length) {
                dots[i].setBackgroundResource(R.drawable.pin_dot_filled)
                dots[i].animate()
                    .scaleX(1.3f)
                    .scaleY(1.3f)
                    .setDuration(100)
                    .withEndAction {
                        dots[i].animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(150)
                            .setInterpolator(OvershootInterpolator(2f))
                            .start()
                    }
                    .start()
            } else {
                dots[i].setBackgroundResource(R.drawable.pin_dot_empty)
            }
        }
    }

    private fun checkPin() {
        if (enteredPin.toString() == correctPin) {
            binding.pinError.visibility = View.INVISIBLE

            // Success — flash green and slide to main
            dots.forEach { dot ->
                dot.setBackgroundColor(getColor(R.color.status_active))
            }

            handler.postDelayed({
                startActivity(Intent(this, MainActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                finishAffinity()
            }, 350)
        } else {
            // Wrong — shake and reset
            binding.pinError.visibility = View.VISIBLE
            shakeDots()
            dots.forEach { dot ->
                dot.setBackgroundColor(getColor(R.color.danger))
            }
            enteredPin.clear()
            handler.postDelayed({
                updateDots()
            }, 500)
        }
    }

    private fun shakeDots() {
        binding.pinDots.animate()
            .translationXBy(-16f)
            .setDuration(40)
            .withEndAction {
                binding.pinDots.animate()
                    .translationXBy(32f)
                    .setDuration(80)
                    .withEndAction {
                        binding.pinDots.animate()
                            .translationXBy(-24f)
                            .setDuration(60)
                            .withEndAction {
                                binding.pinDots.animate()
                                    .translationXBy(16f)
                                    .setDuration(40)
                                    .withEndAction {
                                        binding.pinDots.animate()
                                            .translationXBy(8f)
                                            .setDuration(30)
                                            .withEndAction {
                                                binding.pinDots.animate()
                                                    .translationXBy(-8f)
                                                    .setDuration(20)
                                                    .start()
                                            }
                                            .start()
                                    }
                                    .start()
                            }
                            .start()
                    }
                    .start()
            }
            .start()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
