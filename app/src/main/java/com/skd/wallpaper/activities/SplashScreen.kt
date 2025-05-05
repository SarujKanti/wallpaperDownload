package com.skd.wallpaper.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.skd.wallpaper.R

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val txtAppName: TextView = findViewById(R.id.txt_appName)
        txtAppName.text = getString(R.string.lbl_git_repo)

        val handler = android.os.Handler()

        val runnable = Runnable {
            val intent = Intent(this@SplashActivity, MainDashboardActivity::class.java)
            startActivity(intent)
            finish()
        }

        handler.postDelayed(runnable, 2000)
    }
}