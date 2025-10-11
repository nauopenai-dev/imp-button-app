package com.example.impbutton

import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.example.impbutton.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.redButton.setOnClickListener {
            if (b.impView.visibility != View.VISIBLE) {
                b.impView.apply {
                    scaleX = 0f; scaleY = 0f; alpha = 0f
                    visibility = View.VISIBLE
                    animate().alpha(1f).scaleX(1f).scaleY(1f)
                        .setDuration(350)
                        .setInterpolator(OvershootInterpolator())
                        .start()
                }
            } else {
                b.impView.animate()
                    .alpha(0f).scaleX(0.8f).scaleY(0.8f)
                    .setDuration(200)
                    .withEndAction { b.impView.visibility = View.GONE }
                    .start()
            }
        }
    }
}
