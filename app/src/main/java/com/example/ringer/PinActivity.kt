package com.example.ringer

import android.content.Intent
import android.os.Bundle
import android.view.View
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupKeys()
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
                    if (enteredPin.length == 4) {
                        checkPin()
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
            val params = dots[i].layoutParams
            if (i < enteredPin.length) {
                dots[i].setBackgroundColor(getColor(R.color.primary))
            } else {
                dots[i].setBackgroundColor(getColor(R.color.surface_elevated))
            }
            dots[i].layoutParams = params
        }
    }

    private fun checkPin() {
        if (enteredPin.toString() == correctPin) {
            // Correct — launch main lock management screen
            binding.pinError.visibility = View.INVISIBLE
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity() // clear volume + pin from back stack
        } else {
            // Wrong — shake and reset
            binding.pinError.visibility = View.VISIBLE
            shakeDots()
            enteredPin.clear()
            // Brief delay before resetting dots
            binding.pinDots.postDelayed({ updateDots() }, 300)
        }
    }

    private fun shakeDots() {
        binding.pinDots.animate()
            .translationXBy(-20f)
            .setDuration(50)
            .withEndAction {
                binding.pinDots.animate()
                    .translationXBy(40f)
                    .setDuration(100)
                    .withEndAction {
                        binding.pinDots.animate()
                            .translationXBy(-20f)
                            .setDuration(50)
                            .start()
                    }
                    .start()
            }
            .start()
    }

    override fun onBackPressed() {
        // Go back to volume screen
        super.onBackPressed()
    }
}
