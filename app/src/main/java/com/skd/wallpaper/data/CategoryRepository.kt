package com.skd.wallpaper.data

import com.skd.wallpaper.model.Category
import com.skd.wallpaper.network.RetrofitClient
import com.skd.wallpaper.network.SupabaseListRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Builds the dashboard tabs: the default tabs plus one tab per Supabase folder that
 * contains images. Upload into a new folder and its tab appears on the next app start.
 */
object CategoryRepository {

    // Own-only tabs go right after Trending and Latest
    private const val OWN_TABS_INDEX = 2

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pending: Deferred<List<Category>>? = null

    /** Tabs from a successful load, or null if not loaded yet. */
    @Volatile
    var cached: List<Category>? = null
        private set

    /** Starts loading once (the splash screen calls this early); later calls share the result. */
    @Synchronized
    fun load(): Deferred<List<Category>> {
        pending?.let { if (!it.isCompleted || cached != null) return it }
        return scope.async { buildTabs() }.also { pending = it }
    }

    private suspend fun buildTabs(): List<Category> {
        if (!RetrofitClient.isSupabaseConfigured) return Category.DEFAULT.also { cached = it }
        return try {
            val mixedFolders = Category.DEFAULT.mapNotNull { it.storageFolder }.toSet()
            val ownTabs = nonEmptyFolders()
                .filter { it !in mixedFolders }
                .sortedWith(compareBy({ Category.ownFolderOrder(it) }, { it.lowercase() }))
                .map { Category.ownFolder(it) }
                .distinctBy { it.title }
            val tabs = Category.DEFAULT.toMutableList().apply { addAll(OWN_TABS_INDEX, ownTabs) }
            tabs.also { cached = it }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            // Offline or Supabase down: show the default tabs, retry on the next load()
            Category.DEFAULT
        }
    }

    /** Top-level folders in the bucket that hold at least one image. */
    private suspend fun nonEmptyFolders(): List<String> = coroutineScope {
        val folders = RetrofitClient.supabase.list(
            bucket = Category.STORAGE_BUCKET,
            body = SupabaseListRequest(prefix = "", limit = 100, offset = 0)
        ).filter { it.id == null && it.name.isNotBlank() }.map { it.name }

        folders.map { folder ->
            async {
                val files = RetrofitClient.supabase.list(
                    bucket = Category.STORAGE_BUCKET,
                    // Supabase adds a hidden placeholder file to empty folders, so peek at a few
                    body = SupabaseListRequest(prefix = "$folder/", limit = 5, offset = 0)
                )
                folder.takeIf { files.any { it.id != null && !it.name.startsWith(".") } }
            }
        }.awaitAll().filterNotNull()
    }
}
