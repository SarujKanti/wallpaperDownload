package com.skd.wallpaper.model

import java.io.Serializable

/**
 * A browsable feed shown as a tab on the dashboard.
 * [wallhavenQuery] / [color] / [sorting] / [ratios] are passed to the Wallhaven search API.
 * When [storageFolder] is set, your own images from that Supabase Storage folder
 * are shown first, followed by the Wallhaven results.
 */
data class Category(
    val title: String,
    val emoji: String,
    val query: String? = null,
    val color: String? = null,
    val sorting: String = "favorites",
    // Wallhaven category bits: general / anime / people. Always "100" (general only): anime and
    // people images are mostly copyrighted characters or recognisable persons
    val apiCategories: String = "100",
    val storageFolder: String? = null,
    // Show the feed in a new random order every day (same order all day, also across pages)
    val shuffleDaily: Boolean = false,
    // User searches: try accurate photo search (Pixabay) first, Wallhaven art as fallback
    val photoSearch: Boolean = false,
    /**
     * Strict Wallhaven query for a tab, e.g. "+nature": "+" requires the tag, and
     * [TAB_EXCLUSIONS] is added so only the tab's subject shows (no people or anime).
     * [query] stays the plain word used for card titles.
     */
    val tagQuery: String? = null,
    // Wallhaven aspect ratios to request; "portrait,landscape" where portrait-only is too scarce
    val ratios: String = "portrait",
    // Use black-and-white photos if Wallhaven is down and Picsum is used instead
    val grayscaleFallback: Boolean = false,
    val isLiked: Boolean = false
) : Serializable {

    /** The query actually sent to Wallhaven. */
    val wallhavenQuery: String? get() = tagQuery?.let { it + TAB_EXCLUSIONS } ?: query

    /** Only your Supabase images, no Wallhaven/Picsum results (tab hidden while the folder is empty). */
    val isOwnOnly: Boolean get() = storageFolder != null && query == null && color == null

    companion object {
        /** Supabase Storage bucket holding your uploaded wallpapers, one folder per category. */
        const val STORAGE_BUCKET = "wallpapers"

        /**
         * Added to every tab query. Wallhaven tags any picture that contains e.g. a car or a city
         * as such, including art of people posing in front of it; these keep tabs on topic.
         * (Single words only: multi-word exclusions make Wallhaven return nothing.)
         */
        const val TAB_EXCLUSIONS = " -women -girl -model -anime -people"

        val LIKED = Category("Liked", "❤️", isLiked = true)

        /**
         * Always-visible tabs. Those with a [storageFolder] show your own Supabase images first,
         * followed by Wallhaven results for [query]; an empty folder shows only Wallhaven.
         */
        val DEFAULT = listOf(
            Category("Trending", "🔥", query = "landscape", tagQuery = "+landscape", shuffleDaily = true),
            Category("Latest", "✨", query = "landscape", tagQuery = "+nature +landscape", sorting = "date_added"),
            Category("Nature", "🌿", query = "nature", tagQuery = "+nature", storageFolder = "nature"),
            Category("Space", "🚀", query = "space", tagQuery = "+space"),
            Category("Cars", "🏎️", query = "cars", tagQuery = "+sports car", storageFolder = "cars",
                ratios = "portrait,landscape"),
            Category("City", "🌃", query = "city", tagQuery = "+city"),
            Category("Abstract", "🌀", query = "abstract", tagQuery = "+abstract", storageFolder = "abstract"),
            Category("Minimal", "◽", query = "minimalism", tagQuery = "+minimalism +simple background"),
            Category("Dark", "🌑", query = "night", tagQuery = "+night +landscape", grayscaleFallback = true),
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

        fun search(query: String) =
            Category(query, "🔍", query = query, sorting = "relevance", photoSearch = true)
    }
}
