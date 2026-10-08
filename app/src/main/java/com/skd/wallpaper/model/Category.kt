package com.skd.wallpaper.model

import java.io.Serializable

/**
 * A browsable feed shown as a tab on the dashboard.
 * [query] / [color] / [sorting] are passed straight to the Wallhaven search API.
 * When [storageFolder] is set, your own images from that Supabase Storage folder
 * are shown first, followed by the Wallhaven results.
 */
data class Category(
    val title: String,
    val emoji: String,
    val query: String? = null,
    val color: String? = null,
    val sorting: String = "favorites",
    // Wallhaven category bits: general / anime / people. "100" = general only (cleanest results)
    val apiCategories: String = "100",
    val storageFolder: String? = null,
    // Show the feed in a new random order every day (same order all day, also across pages)
    val shuffleDaily: Boolean = false,
    val isLiked: Boolean = false
) : Serializable {

    /** Only your Supabase images, no Wallhaven/Picsum results (tab hidden while the folder is empty). */
    val isOwnOnly: Boolean get() = storageFolder != null && query == null && color == null

    companion object {
        /** Supabase Storage bucket holding your uploaded wallpapers, one folder per category. */
        const val STORAGE_BUCKET = "wallpapers"

        val LIKED = Category("Liked", "❤️", isLiked = true)

        /**
         * Always-visible tabs. Those with a [storageFolder] show your own Supabase images first,
         * followed by Wallhaven results for [query]; an empty folder shows only Wallhaven.
         */
        val DEFAULT = listOf(
            Category("Trending", "🔥", query = "landscape", shuffleDaily = true),
            Category("Latest", "✨", query = "landscape", sorting = "date_added"),
            Category("Nature", "🌿", query = "nature", storageFolder = "nature"),
            Category("Anime", "🎌", query = "landscape", apiCategories = "010"),
            Category("Space", "🚀", query = "space"),
            Category("Cars", "🏎️", query = "cars", storageFolder = "cars"),
            Category("City", "🌃", query = "city"),
            Category("Abstract", "🌀", query = "abstract", storageFolder = "abstract"),
            Category("Minimal", "◽", query = "minimalism"),
            Category("Dark", "🌑", query = "night", color = "000000"),
            LIKED
        )

        /**
         * Titles and emojis for your own-only Supabase folders. Any other folder you create
         * still gets a tab, titled from its name with a ⭐.
         */
        private val OWN_FOLDER_LABELS = linkedMapOf(
            "movies" to ("Movies" to "🎬"),
            "movie" to ("Movies" to "🎬"),
            "series" to ("Series" to "📺"),
            "ocean" to ("Ocean" to "🌊"),
            "beach" to ("Beach" to "🏖️"),
            "building" to ("Building" to "🏢"),
            "buildings" to ("Building" to "🏢")
        )

        /** Tab for one of your own-only Supabase folders. */
        fun ownFolder(folder: String): Category {
            val (title, emoji) = OWN_FOLDER_LABELS[folder.lowercase()]
                ?: (folder.replace('-', ' ').replace('_', ' ').trim()
                    .replaceFirstChar { it.uppercase() } to "⭐")
            return Category(title, emoji, storageFolder = folder)
        }

        /** Sort key so preset folders keep their listed order and others follow alphabetically. */
        fun ownFolderOrder(folder: String): Int =
            OWN_FOLDER_LABELS.keys.indexOf(folder.lowercase()).let { if (it < 0) Int.MAX_VALUE else it }

        fun search(query: String) = Category(query, "🔍", query = query, sorting = "relevance", apiCategories = "110")
    }
}
