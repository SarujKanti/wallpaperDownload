package com.skd.wallpaper.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.skd.wallpaper.model.Wallpaper

/** Liked wallpapers, persisted locally as JSON in SharedPreferences (newest first). */
object FavoritesStore {

    private const val PREFS = "favorites_prefs"
    private const val KEY = "liked_wallpapers"
    private val gson = Gson()
    private val type = object : TypeToken<List<Wallpaper>>() {}.type

    fun getAll(context: Context): List<Wallpaper> {
        val json = prefs(context).getString(KEY, null) ?: return emptyList()
        return runCatching<List<Wallpaper>> { gson.fromJson(json, type) }.getOrDefault(emptyList())
    }

    fun isLiked(context: Context, wallpaper: Wallpaper): Boolean =
        getAll(context).any { it.key == wallpaper.key }

    /** Toggles the like state and returns the new state. */
    fun toggle(context: Context, wallpaper: Wallpaper): Boolean {
        val current = getAll(context).toMutableList()
        val liked = current.removeAll { it.key == wallpaper.key }.not()
        if (liked) current.add(0, wallpaper)
        prefs(context).edit().putString(KEY, gson.toJson(current)).apply()
        return liked
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
