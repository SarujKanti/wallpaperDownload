package com.skd.wallpaper.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.skd.wallpaper.R

/**
 * Shimmering placeholder cards shown while wallpapers load, placed after the real cards
 * (a full screen of them on the first load, one row while loading more).
 */
class SkeletonAdapter : RecyclerView.Adapter<SkeletonAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    /** Number of placeholder cards to show; 0 hides them. */
    var count: Int = 0
        set(value) {
            if (value == field) return
            val old = field
            field = value
            when {
                value > old -> notifyItemRangeInserted(old, value - old)
                else -> notifyItemRangeRemoved(value, old - value)
            }
        }

    override fun getItemCount() = count

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_wallpaper_skeleton, parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = Unit
}
