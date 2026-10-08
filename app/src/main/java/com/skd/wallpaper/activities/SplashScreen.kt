package com.skd.wallpaper.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.skd.wallpaper.data.CategoryRepository
import com.skd.wallpaper.databinding.ActivitySplashBinding
import com.skd.wallpaper.utils.applySystemBars
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Light icons over the purple gradient, whatever the theme
        applySystemBars(forceDark = true)
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        animateIn()

        // Find your Supabase folders while the animation plays, so their tabs are ready
        val tabs = CategoryRepository.load()
        lifecycleScope.launch {
            delay(SPLASH_MS)
            withTimeoutOrNull(MAX_EXTRA_WAIT_MS) { tabs.await() }
            openDashboard()
        }
    }

    private fun openDashboard() {
        startActivity(Intent(this, MainDashboardActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
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
                // Gentle "breathing" pulse once the logo lands
                logoContainer.animate().scaleX(1.06f).scaleY(1.06f).setDuration(600).withEndAction {
                    logoContainer.animate().scaleX(1f).scaleY(1f).setDuration(600).start()
                }.start()
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

    companion object {
        private const val SPLASH_MS = 2000L
        // On a slow network, don't hold the user on the splash for long; the dashboard adds tabs later
        private const val MAX_EXTRA_WAIT_MS = 2000L
    }
}
