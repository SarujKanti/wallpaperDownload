package com.skd.wallpaper.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.skd.wallpaper.utils.applySystemBars
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.skd.wallpaper.databinding.ActivitySearchResultsBinding
import com.skd.wallpaper.fragments.WallpaperGridFragment
import com.skd.wallpaper.model.Category

class SearchResultsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchResultsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBars()
        binding = ActivitySearchResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val query = intent.getStringExtra(EXTRA_QUERY).orEmpty()
        binding.tvTitle.text = "“$query”"
        binding.btnBack.setOnClickListener { finish() }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(binding.container.id, WallpaperGridFragment.newInstance(Category.search(query)))
                .commit()
        }
    }

    companion object {
        private const val EXTRA_QUERY = "query"

        fun start(context: Context, query: String) {
            context.startActivity(Intent(context, SearchResultsActivity::class.java).putExtra(EXTRA_QUERY, query))
        }
    }
}
