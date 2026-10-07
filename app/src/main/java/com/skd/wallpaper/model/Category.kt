package com.skd.wallpaper.model

import java.io.Serializable

/**
 * A browsable feed shown as a tab on the dashboard.
 * [query] / [color] / [sorting] are passed straight to the Wallhaven search API.
 */
data class Category(
    val title: String,
    val emoji: String,
    val query: String? = null,
    val color: String? = null,
    val sorting: String = "favorites",
    // Wallhaven category bits: general / anime / people. "100" = general only (cleanest results)
    val apiCategories: String = "100",
    val isLiked: Boolean = false
) : Serializable {

    companion object {
        val LIKED = Category("Liked", "❤️", isLiked = true)

        val ALL = listOf(
            Category("Trending", "🔥", query = "landscape"),
            Category("Latest", "✨", query = "landscape", sorting = "date_added"),
            Category("Nature", "🌿", query = "nature"),
            Category("Anime", "🎌", query = "landscape", apiCategories = "010"),
            Category("Space", "🚀", query = "space"),
            Category("Cars", "🏎️", query = "cars"),
            Category("City", "🌃", query = "city"),
            Category("Abstract", "🌀", query = "abstract"),
            Category("Minimal", "◽", query = "minimalism"),
            Category("Dark", "🌑", query = "night", color = "000000"),
            LIKED
        )

        fun search(query: String) = Category(query, "🔍", query = query, sorting = "relevance", apiCategories = "110")
    }
}
