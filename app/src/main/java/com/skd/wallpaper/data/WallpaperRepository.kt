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
import kotlinx.coroutines.delay
import retrofit2.HttpException

data class WallpaperPage(
    val items: List<Wallpaper>,
    val hasMore: Boolean
)

object WallpaperRepository {

    private const val PICSUM_PAGE_SIZE = 30
    private const val STORAGE_PAGE_SIZE = 30
    // Mixed tabs fetch the whole (small) folder with the first page, before the online results
    private const val STORAGE_LIMIT = 200

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
            loadOnline(category, page)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException || own.isEmpty()) throw e
            // Online sources failed but we still have your images to show
            WallpaperPage(emptyList(), hasMore = false)
        }
        return WallpaperPage(own + online.items, online.hasMore)
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
        thumbUrl = thumbs?.large ?: thumbs?.original ?: path,
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
        // Picsum can resize on the fly, so ask for a small portrait crop for the grid
        return Wallpaper(
            id = id,
            thumbUrl = "https://picsum.photos/id/$id/400/700$suffix",
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
