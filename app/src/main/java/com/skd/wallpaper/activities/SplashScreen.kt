package com.skd.wallpaper.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.skd.wallpaper.databinding.ActivitySplashBinding

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val handler = Handler(Looper.getMainLooper())
    private val openDashboard = Runnable {
        startActivity(Intent(this, MainDashboardActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        animateIn()
        handler.postDelayed(openDashboard, 2000)
    }

    private fun animateIn() = with(binding) {
        logoContainer.scaleX = 0.4f
        logoContainer.scaleY = 0.4f
        logoContainer.alpha = 0f
        logoContainer.animate()
            .scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(700)
            .setInterpolator(OvershootInterpolator(1.6f))
            .withEndAction {
                // Gentle floating rotation once the logo lands
                logoContainer.animate().rotationBy(360f).setDuration(1200).start()
            }
            .start()

        listOf<View>(campus, txtAppName, txtTagline, progress).forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 40f
            view.animate()
                .alpha(1f).translationY(0f)
                .setStartDelay(300L + index * 120L)
                .setDuration(500)
                .start()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(openDashboard)
        super.onDestroy()
    }
}
