package com.skd.wallpaper.data

import com.skd.wallpaper.model.Category
import com.skd.wallpaper.model.Wallpaper
import com.skd.wallpaper.network.PicsumPhoto
import com.skd.wallpaper.network.RetrofitClient
import com.skd.wallpaper.network.WallhavenWallpaper
import kotlinx.coroutines.delay
import retrofit2.HttpException

data class WallpaperPage(
    val items: List<Wallpaper>,
    val hasMore: Boolean
)

object WallpaperRepository {

    private const val PICSUM_PAGE_SIZE = 30

    /**
     * Loads one page for [category]. Wallhaven is tried first; if it is down or
     * rate-limited, Lorem Picsum is used so the user always sees wallpapers.
     */
    suspend fun load(category: Category, page: Int): WallpaperPage {
        return try {
            loadWallhaven(category, page)
        } catch (e: Exception) {
            loadPicsum(page, grayscale = category.color == "000000")
        }
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
}
