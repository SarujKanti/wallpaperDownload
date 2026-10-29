package com.skd.wallpaper.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.skd.wallpaper.utils.applySystemBars
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.skd.wallpaper.databinding.ActivitySearchResultsBinding
import com.skd.wallpaper.R
import com.skd.wallpaper.data.WallpaperRepository
import com.skd.wallpaper.fragments.FeedHost
import com.skd.wallpaper.fragments.WallpaperGridFragment
import com.skd.wallpaper.model.Category
import com.skd.wallpaper.model.Wallpaper

class SearchResultsActivity : AppCompatActivity(), FeedHost {

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

    /** Credit Pixabay under the title whenever the results come from it (required by Pixabay). */
    override fun onFeedLoaded(category: Category, items: List<Wallpaper>) {
        val fromPixabay = items.firstOrNull()?.source == WallpaperRepository.PIXABAY_SOURCE
        binding.tvSubtitle.setText(if (fromPixabay) R.string.lbl_images_from_pixabay else R.string.lbl_explore)
    }

    companion object {
        private const val EXTRA_QUERY = "query"

        fun start(context: Context, query: String) {
            context.startActivity(Intent(context, SearchResultsActivity::class.java).putExtra(EXTRA_QUERY, query))
        }
    }
}
