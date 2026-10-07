package com.skd.wallpaper.adapters

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.skd.wallpaper.R
import com.skd.wallpaper.databinding.ItemWallpaperBinding
import com.skd.wallpaper.model.Wallpaper
import com.skd.wallpaper.utils.ImageDimensions

class WallpaperAdapter(
    private val onClick: (Wallpaper) -> Unit,
    private val onLikeClick: (Wallpaper) -> Unit
) : ListAdapter<Wallpaper, WallpaperAdapter.ViewHolder>(Diff) {

    var likedKeys: Set<String> = emptySet()
        set(value) {
            field = value
            notifyItemRangeChanged(0, itemCount, PAYLOAD_LIKE)
        }

    inner class ViewHolder(val binding: ItemWallpaperBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Wallpaper) = with(binding) {
            // Staggered heights based on the real aspect ratio of the wallpaper
            val columnWidth = (root.parent as? RecyclerView)?.let { it.width / 2 }
                ?.takeIf { it > 0 } ?: (root.resources.displayMetrics.widthPixels / 2)
            imageFrame.layoutParams = imageFrame.layoutParams.apply {
                height = (columnWidth * aspectRatio(item)).toInt()
            }

            val label = ImageDimensions.label(item)
            tvResolution.text = label
            tvResolution.visibility = if (label != null) View.VISIBLE else View.GONE

            val placeholder = ColorDrawable(placeholderColor(item, root.context))
            ivWallpaper.load(item.thumbUrl) {
                crossfade(300)
                placeholder(placeholder)
                error(placeholder)
                if (label == null) listener(onSuccess = { _, _ -> showResolutionWhenKnown(item) })
            }

            bindLike(item)
            root.setOnClickListener { onClick(item) }
            ivLike.setOnClickListener {
                it.animate().scaleX(1.3f).scaleY(1.3f).setDuration(120).withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                }.start()
                onLikeClick(item)
            }
        }

        /** Own Supabase images: read the size once the image is downloaded, then show the badge. */
        private fun showResolutionWhenKnown(item: Wallpaper) {
            ImageDimensions.resolve(binding.root.context, item) { label ->
                // The view may have been recycled for another wallpaper meanwhile
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION || getItem(position).key != item.key) return@resolve
                binding.tvResolution.text = label
                binding.tvResolution.visibility = View.VISIBLE
            }
        }

        fun bindLike(item: Wallpaper) = with(binding) {
            val liked = item.key in likedKeys
            ivLike.setImageResource(if (liked) R.drawable.ic_heart else R.drawable.ic_heart_outline)
            ivLike.setColorFilter(
                ContextCompat.getColor(root.context, if (liked) R.color.like_red else R.color.white)
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemWallpaperBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_LIKE)) holder.bindLike(getItem(position))
        else super.onBindViewHolder(holder, position, payloads)
    }

    private fun aspectRatio(item: Wallpaper): Float {
        // Picsum thumbnails are requested as 400x700 crops
        if (item.source == "Picsum" || item.width <= 0) return 1.75f
        return (item.height.toFloat() / item.width).coerceIn(1.3f, 2.1f)
    }

    private fun placeholderColor(item: Wallpaper, context: Context): Int =
        item.colors.firstOrNull()?.let { runCatching { Color.parseColor(it) }.getOrNull() }
            ?: ContextCompat.getColor(context, R.color.surface_variant)

    private object Diff : DiffUtil.ItemCallback<Wallpaper>() {
        override fun areItemsTheSame(oldItem: Wallpaper, newItem: Wallpaper) = oldItem.key == newItem.key
        override fun areContentsTheSame(oldItem: Wallpaper, newItem: Wallpaper) = oldItem == newItem
    }

    companion object {
        private const val PAYLOAD_LIKE = "like"
    }
}
