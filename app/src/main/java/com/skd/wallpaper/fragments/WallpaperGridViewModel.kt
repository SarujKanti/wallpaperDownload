package com.skd.wallpaper.fragments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skd.wallpaper.data.WallpaperRepository
import com.skd.wallpaper.model.Category
import com.skd.wallpaper.model.Wallpaper
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

enum class LoadState { IDLE, LOADING, LOADING_MORE, REFRESHING, ERROR }

class WallpaperGridViewModel : ViewModel() {

    private val _items = MutableLiveData<List<Wallpaper>>(emptyList())
    val items: LiveData<List<Wallpaper>> = _items

    private val _state = MutableLiveData(LoadState.IDLE)
    val state: LiveData<LoadState> = _state

    private lateinit var category: Category
    private var nextPage = 1
    private var hasMore = true
    private var job: Job? = null

    fun start(category: Category) {
        if (::category.isInitialized) return
        this.category = category
        load(reset = true, LoadState.LOADING)
    }

    fun refresh() = load(reset = true, LoadState.REFRESHING)

    fun retry() = load(reset = _items.value.isNullOrEmpty(), LoadState.LOADING)

    fun loadMore() {
        if (!hasMore || job?.isActive == true || _state.value == LoadState.ERROR) return
        load(reset = false, LoadState.LOADING_MORE)
    }

    /** Liked feed is local, so it is simply replaced. */
    fun setItems(list: List<Wallpaper>) {
        _items.value = list
        _state.value = LoadState.IDLE
    }

    private fun load(reset: Boolean, loadingState: LoadState) {
        job?.cancel()
        if (reset) {
            nextPage = 1
            hasMore = true
        }
        _state.value = loadingState
        job = viewModelScope.launch {
            try {
                val page = WallpaperRepository.load(category, nextPage)
                val existing = if (reset) emptyList() else _items.value.orEmpty()
                // Dedupe: APIs can repeat items between pages when new uploads arrive
                val seen = existing.mapTo(HashSet()) { it.key }
                _items.value = existing + page.items.filter { seen.add(it.key) }
                hasMore = page.hasMore
                nextPage++
                _state.value = LoadState.IDLE
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = LoadState.ERROR
            }
        }
    }
}
