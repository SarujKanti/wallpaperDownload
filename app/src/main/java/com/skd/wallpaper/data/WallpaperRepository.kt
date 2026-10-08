package com.skd.wallpaper.data

import com.skd.wallpaper.model.Category
import com.skd.wallpaper.model.Wallpaper
import com.skd.wallpaper.network.PicsumPhoto
import android.net.Uri
import com.skd.wallpaper.BuildConfig
import com.skd.wallpaper.network.RetrofitClient
import com.skd.wallpaper.network.SupabaseListRequest
import com.skd.wallpaper.network.SupabaseObject
import com.skd.wallpaper.network.WallhavenWallpaper
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.util.Calendar
import kotlin.random.Random

data class WallpaperPage(
    val items: List<Wallpaper>,
    val hasMore: Boolean
)

object WallpaperRepository {

    private const val PICSUM_PAGE_SIZE = 30
    private const val STORAGE_PAGE_SIZE = 30
    // Mixed tabs fetch the whole (small) folder with the first page, before the online results
    private const val STORAGE_LIMIT = 200
    private const val WALLHAVEN_PAGE_SIZE = 24
    // Trending's daily shuffle covers the top 4 pages (96 wallpapers)
    private const val DAILY_POOL_PAGES = 4

    // Shuffled pool per daily-shuffled tab, with the day it was made for
    private val dailyPools = mutableMapOf<String, Pair<Long, List<Wallpaper>>>()

    /**
     * Loads one page for [category]:
     *  - own-only tabs: just your Supabase folder, page by page;
     *  - mixed tabs: your folder first (on page 1), then online results;
     *  - online results: Wallhaven first; if it is down or rate-limited, Lorem Picsum is
     *    used so the user always sees wallpapers.
     */
    suspend fun load(category: Category, page: Int): WallpaperPage {
        val folder = category.storageFolder
        if (category.isOwnOnly && folder != null) {
            return loadStorageFolder(folder, STORAGE_PAGE_SIZE, (page - 1) * STORAGE_PAGE_SIZE)
        }

        val own = if (page == 1 && folder != null && RetrofitClient.isSupabaseConfigured) {
            // A Supabase problem must never hide the online wallpapers
            runCatching { loadStorageFolder(folder, STORAGE_LIMIT, 0).items }.getOrDefault(emptyList())
        } else emptyList()

        val online = try {
            if (category.shuffleDaily) loadDailyShuffled(category, page) else loadOnline(category, page)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException || own.isEmpty()) throw e
            // Online sources failed but we still have your images to show
            WallpaperPage(emptyList(), hasMore = false)
        }
        return WallpaperPage(own + online.items, online.hasMore)
    }

    /**
     * A new order every day: the top [DAILY_POOL_PAGES] pages are fetched once and shuffled
     * with a seed from today's date, so every position changes each day but stays the same
     * all day (also when scrolling or refreshing). Wallhaven's own random sorting can't be
     * used: its API ignores the seed and reshuffles on every request.
     * Pages after the pool continue with the next online pages, each shuffled the same way.
     */
    private suspend fun loadDailyShuffled(category: Category, page: Int): WallpaperPage {
        val day = currentDay()
        val pool = dailyPools[category.title]?.takeIf { it.first == day }?.second
            ?: buildDailyPool(category, day).also { if (it.isNotEmpty()) dailyPools[category.title] = day to it }

        val poolPages = (pool.size + WALLHAVEN_PAGE_SIZE - 1) / WALLHAVEN_PAGE_SIZE
        if (page <= poolPages) {
            val from = (page - 1) * WALLHAVEN_PAGE_SIZE
            return WallpaperPage(pool.subList(from, minOf(from + WALLHAVEN_PAGE_SIZE, pool.size)), hasMore = true)
        }
        val next = loadOnline(category, DAILY_POOL_PAGES + page - poolPages)
        return next.copy(items = next.items.sortedBy { it.key }.shuffled(Random(day * 31L + page)))
    }

    private suspend fun buildDailyPool(category: Category, day: Long): List<Wallpaper> = coroutineScope {
        (1..DAILY_POOL_PAGES)
            .map { page -> async { runCatching { loadOnline(category, page).items }.getOrDefault(emptyList()) } }
            .awaitAll()
            .flatten()
            .distinctBy { it.key }
            // A fixed base order first, so the same day always gives the same shuffle
            .sortedBy { it.key }
            .shuffled(Random(day))
    }

    /** Today's date as a number (changes at local midnight), used as the shuffle seed. */
    private fun currentDay(): Long = Calendar.getInstance().run {
        get(Calendar.YEAR) * 1000L + get(Calendar.DAY_OF_YEAR)
    }

    private suspend fun loadOnline(category: Category, page: Int): WallpaperPage = try {
        loadWallhaven(category, page)
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        loadPicsum(page, grayscale = category.color == "000000")
    }

    private suspend fun loadWallhaven(category: Category, page: Int): WallpaperPage {
        val response = try {
            searchWallhaven(category, page)
        } catch (e: HttpException) {
            // Wallhaven allows ~45 requests/minute; wait once and retry before giving up.
            if (e.code() != 429) throw e
            delay(1500)
            searchWallhaven(category, page)
        }
        val meta = response.meta
        return WallpaperPage(
            items = response.data.map { it.toWallpaper() },
            hasMore = meta != null && meta.currentPage < meta.lastPage
        )
    }

    private suspend fun searchWallhaven(category: Category, page: Int) =
        RetrofitClient.wallhaven.search(
            query = category.query,
            colors = category.color,
            sorting = category.sorting,
            categories = category.apiCategories,
            page = page,
            topRange = if (category.sorting == "toplist") "1y" else null
        )

    private suspend fun loadStorageFolder(folder: String, limit: Int, offset: Int): WallpaperPage {
        val objects = RetrofitClient.supabase.list(
            bucket = Category.STORAGE_BUCKET,
            body = SupabaseListRequest(prefix = "$folder/", limit = limit, offset = offset)
        )
        return WallpaperPage(
            // Skip sub-folders (no id) and the hidden placeholder file Supabase adds to folders
            items = objects
                .filter { it.id != null && !it.name.startsWith(".") }
                .map { it.toWallpaper(folder) },
            // Judge by the raw count, since filtering can make a full page look short
            hasMore = objects.size == limit
        )
    }

    private suspend fun loadPicsum(page: Int, grayscale: Boolean): WallpaperPage {
        val photos = RetrofitClient.picsum.list(page, PICSUM_PAGE_SIZE)
        return WallpaperPage(
            items = photos.map { it.toWallpaper(grayscale) },
            hasMore = photos.size == PICSUM_PAGE_SIZE
        )
    }

    private fun WallhavenWallpaper.toWallpaper() = Wallpaper(
        id = id,
        // "original" keeps the image shape; "large" is a landscape crop that looks soft in 4:5 cards
        thumbUrl = thumbs?.original ?: thumbs?.large ?: path,
        fullUrl = path,
        width = width,
        height = height,
        source = "Wallhaven",
        pageUrl = url,
        views = views,
        favorites = favorites,
        fileSize = fileSize,
        fileType = fileType,
        colors = colors.orEmpty(),
        category = category
    )

    private fun PicsumPhoto.toWallpaper(grayscale: Boolean): Wallpaper {
        val suffix = if (grayscale) "?grayscale" else ""
        // Picsum can resize on the fly, so ask for a 4:5 crop matching the grid cards
        return Wallpaper(
            id = id,
            thumbUrl = "https://picsum.photos/id/$id/480/600$suffix",
            fullUrl = "https://picsum.photos/id/$id/$width/$height$suffix",
            width = width,
            height = height,
            author = author,
            source = "Picsum",
            pageUrl = url
        )
    }

    private fun SupabaseObject.toWallpaper(folder: String): Wallpaper {
        val fileName = name.removePrefix("$folder/")
        // Public bucket URL; encode each segment so names with spaces work
        val url = BuildConfig.SUPABASE_URL.trimEnd('/') +
            "/storage/v1/object/public/${Category.STORAGE_BUCKET}/" +
            Uri.encode(folder) + "/" + Uri.encode(fileName)
        return Wallpaper(
            id = id ?: "$folder/$fileName",
            // Storage doesn't resize on the free plan, so the grid uses the original too
            thumbUrl = url,
            fullUrl = url,
            // Storage doesn't report dimensions; the UI falls back to a portrait shape
            width = 0,
            height = 0,
            author = fileName.substringBeforeLast('.').replace('-', ' ').replace('_', ' ').trim(),
            source = "My Collection",
            fileSize = metadata?.size ?: 0,
            fileType = metadata?.mimeType,
            category = folder
        )
    }
}
