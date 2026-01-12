package com.samsunghub.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.samsunghub.app.MainActivity
import com.samsunghub.app.R
import com.samsunghub.app.utils.UserPrefs

class LoginActivity : AppCompatActivity() {

    private var enteredPin = StringBuilder()
    private val dots = ArrayList<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!UserPrefs.isPinSet(this)) {
            startMainActivity()
            return
        }

        setContentView(R.layout.activity_login)
        setupUI()

        findViewById<TextView>(R.id.tvForgot).setOnClickListener {
            android.app.AlertDialog.Builder(this)
                .setTitle("Forgot PIN?")
                .setMessage("To reset your PIN, you must clear the app data from Android Settings.\n\nSettings > Apps > SamsungHub > Storage > Clear Data")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun setupUI() {
        dots.add(findViewById(R.id.dot1))
        dots.add(findViewById(R.id.dot2))
        dots.add(findViewById(R.id.dot3))
        dots.add(findViewById(R.id.dot4))

        val grid = findViewById<GridLayout>(R.id.gridLayout)
        for (i in 0 until grid.childCount) {
            val child = grid.getChildAt(i)
            if (child is Button) {
                child.setOnClickListener { v ->
                    val tag = v.tag.toString()
                    when (tag) {
                        "CLR" -> clearPin()
                        "GO" -> validatePin()
                        else -> addDigit(tag)
                    }
                }
            }
        }
    }

    private fun addDigit(digit: String) {
        if (enteredPin.length < 4) {
            enteredPin.append(digit)
            updateDots()
            if (enteredPin.length == 4) {
                // Auto validate on 4th digit
                validatePin()
            }
        }
    }

    private fun clearPin() {
        enteredPin.setLength(0)
        updateDots()
    }

    private fun updateDots() {
        for (i in 0 until 4) {
            if (i < enteredPin.length) {
                dots[i].setBackgroundResource(R.drawable.pin_dot_filled_blue)
            } else {
                dots[i].setBackgroundResource(R.drawable.pin_dot_empty_blue)
            }
        }
    }

    private fun validatePin() {
        val savedPin = UserPrefs.getPin(this)
        if (enteredPin.toString() == savedPin) {
            startMainActivity()
        } else {
            // Shake Animation
            val dotsContainer = findViewById<View>(R.id.dotsContainer)
            dotsContainer.animate()
                .translationX(20f)
                .setDuration(50)
                .withEndAction {
                    dotsContainer.animate()
                        .translationX(-20f)
                        .setDuration(50)
                        .withEndAction {
                            dotsContainer.animate().translationX(0f).setDuration(50).start()
                        }
                        .start()
                }
                .start()

            Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
            enteredPin.setLength(0)
            updateDots()
        }
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
