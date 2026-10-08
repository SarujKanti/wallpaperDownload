package com.skd.wallpaper.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.skd.wallpaper.R
import com.skd.wallpaper.activities.WallpaperPreviewActivity
import com.skd.wallpaper.adapters.WallpaperAdapter
import com.skd.wallpaper.data.FavoritesStore
import com.skd.wallpaper.databinding.FragmentWallpaperGridBinding
import com.skd.wallpaper.model.Category
import com.skd.wallpaper.model.Wallpaper

/** Lets the screen hosting the grids (the dashboard) react to a tab's wallpapers, e.g. for its header photo. */
interface FeedHost {
    fun onFeedLoaded(category: Category, items: List<Wallpaper>)
}

/** A 2-column grid of 4:5 wallpaper cards for one [Category], with infinite scroll. */
class WallpaperGridFragment : Fragment() {

    private var _binding: FragmentWallpaperGridBinding? = null
    private val binding get() = _binding!!

    private lateinit var category: Category
    private lateinit var viewModel: WallpaperGridViewModel
    private lateinit var adapter: WallpaperAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        category = BundleCompat.getSerializable(requireArguments(), ARG_CATEGORY, Category::class.java)!!
        viewModel = ViewModelProvider(this)[WallpaperGridViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWallpaperGridBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupList()
        observe()
        if (!category.isLiked) viewModel.start(category)
    }

    override fun onResume() {
        super.onResume()
        // Likes may have changed on the preview screen
        refreshLikes()
    }

    private fun setupList() {
        adapter = WallpaperAdapter(
            category = category,
            onClick = { WallpaperPreviewActivity.start(requireContext(), it) },
            onLikeClick = { wallpaper ->
                FavoritesStore.toggle(requireContext(), wallpaper)
                refreshLikes()
            }
        )
        // All cards share the same 4:5 shape, so a plain 2-column grid keeps rows aligned
        val layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = adapter
        binding.recyclerView.setHasFixedSize(true)

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0 || category.isLiked) return
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                if (lastVisible >= adapter.itemCount - PRELOAD_THRESHOLD) viewModel.loadMore()
            }
        })

        binding.swipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(requireContext(), R.color.accent_pink),
            ContextCompat.getColor(requireContext(), R.color.accent_purple)
        )
        binding.swipeRefresh.setProgressBackgroundColorSchemeColor(
            ContextCompat.getColor(requireContext(), R.color.surface)
        )
        binding.swipeRefresh.setOnRefreshListener {
            if (category.isLiked) {
                refreshLikes()
                binding.swipeRefresh.isRefreshing = false
            } else {
                viewModel.refresh()
            }
        }
        binding.btnRetry.setOnClickListener { viewModel.retry() }
    }

    private fun observe() {
        viewModel.items.observe(viewLifecycleOwner) { list ->
            val firstFill = adapter.itemCount == 0 && list.isNotEmpty()
            adapter.submitList(list) {
                if (firstFill) binding.recyclerView.scheduleLayoutAnimation()
            }
            if (list.isNotEmpty()) (activity as? FeedHost)?.onFeedLoaded(category, list)
            renderState(viewModel.state.value ?: LoadState.IDLE)
        }
        viewModel.state.observe(viewLifecycleOwner) { renderState(it) }
    }

    private fun renderState(state: LoadState) = with(binding) {
        val isEmpty = viewModel.items.value.isNullOrEmpty()
        progressCenter.visibility = if (state == LoadState.LOADING && isEmpty) View.VISIBLE else View.GONE
        progressMore.visibility = if (state == LoadState.LOADING_MORE) View.VISIBLE else View.GONE
        if (state != LoadState.REFRESHING) swipeRefresh.isRefreshing = false

        when {
            state == LoadState.ERROR && isEmpty -> showStateView(
                "📡", R.string.lbl_error_title, R.string.lbl_error_msg, showRetry = true
            )
            state == LoadState.ERROR -> {
                stateView.visibility = View.GONE
                Snackbar.make(root, R.string.lbl_error_msg, Snackbar.LENGTH_LONG)
                    .setAction(R.string.lbl_retry) { viewModel.retry() }
                    .show()
            }
            state == LoadState.IDLE && isEmpty && category.isLiked -> showStateView(
                "💜", R.string.lbl_liked_empty_title, R.string.lbl_liked_empty_msg, showRetry = false
            )
            state == LoadState.IDLE && isEmpty -> showStateView(
                "🔭", R.string.lbl_empty_title, R.string.lbl_empty_msg, showRetry = true
            )
            else -> stateView.visibility = View.GONE
        }
    }

    private fun showStateView(emoji: String, title: Int, message: Int, showRetry: Boolean) = with(binding) {
        stateView.visibility = View.VISIBLE
        tvStateEmoji.text = emoji
        tvStateTitle.setText(title)
        tvStateMessage.setText(message)
        btnRetry.visibility = if (showRetry) View.VISIBLE else View.GONE
    }

    private fun refreshLikes() {
        val liked = FavoritesStore.getAll(requireContext())
        adapter.likedKeys = liked.mapTo(HashSet()) { it.key }
        if (category.isLiked) viewModel.setItems(liked)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CATEGORY = "category"
        private const val PRELOAD_THRESHOLD = 6

        fun newInstance(category: Category) = WallpaperGridFragment().apply {
            arguments = Bundle().apply { putSerializable(ARG_CATEGORY, category) }
        }
    }
}
