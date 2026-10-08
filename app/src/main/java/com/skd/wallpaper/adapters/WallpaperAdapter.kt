package com.skd.wallpaper.adapters

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.skd.wallpaper.R
import com.skd.wallpaper.databinding.ItemWallpaperBinding
import com.skd.wallpaper.model.Category
import com.skd.wallpaper.model.Wallpaper
import com.skd.wallpaper.utils.ImageDimensions
import com.skd.wallpaper.utils.WallpaperText
import com.skd.wallpaper.utils.shareWallpaper

/** 4:5 wallpaper cards; each card shows the selected tab's name, e.g. "🔥 Trending". */
class WallpaperAdapter(
    private val category: Category,
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
            tvBadge.text = "${category.emoji}  ${category.title}"
            tvTitle.text = WallpaperText.title(item, category)

            val size = ImageDimensions.size(item)
            showMeta(size)

            val placeholder = ColorDrawable(placeholderColor(item, root.context))
            ivWallpaper.load(item.thumbUrl) {
                crossfade(300)
                placeholder(placeholder)
                error(placeholder)
                if (size == null) listener(onSuccess = { _, _ -> showMetaWhenKnown(item) })
            }

            bindLike(item)
            root.setOnClickListener { onClick(item) }
            ivLike.setOnClickListener {
                it.animate().scaleX(1.3f).scaleY(1.3f).setDuration(120).withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                }.start()
                onLikeClick(item)
            }
            ivMenu.setOnClickListener { showMenu(it, item) }
        }

        private fun showMeta(size: Pair<Int, Int>?) {
            val meta = size?.let { (w, h) -> WallpaperText.meta(w, h) }
            binding.tvMeta.text = meta
            binding.metaRow.visibility = if (meta != null) View.VISIBLE else View.INVISIBLE
        }

        /** Own Supabase images: read the size once the image is downloaded, then show "4K • Portrait". */
        private fun showMetaWhenKnown(item: Wallpaper) {
            ImageDimensions.resolve(binding.root.context, item) { size ->
                // The view may have been recycled for another wallpaper meanwhile
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION || getItem(position).key != item.key) return@resolve
                showMeta(size)
            }
        }

        private fun showMenu(anchor: View, item: Wallpaper) {
            val liked = item.key in likedKeys
            PopupMenu(anchor.context, anchor).apply {
                menu.add(0, MENU_OPEN, 0, R.string.lbl_open)
                menu.add(0, MENU_LIKE, 1, if (liked) R.string.lbl_unlike else R.string.lbl_like)
                menu.add(0, MENU_SHARE, 2, R.string.lbl_share)
                setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        MENU_OPEN -> onClick(item)
                        MENU_LIKE -> onLikeClick(item)
                        MENU_SHARE -> shareWallpaper(anchor.context, item)
                    }
                    true
                }
            }.show()
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

    private fun placeholderColor(item: Wallpaper, context: Context): Int =
        item.colors.firstOrNull()?.let { runCatching { Color.parseColor(it) }.getOrNull() }
            ?: ContextCompat.getColor(context, R.color.surface_variant)

    private object Diff : DiffUtil.ItemCallback<Wallpaper>() {
        override fun areItemsTheSame(oldItem: Wallpaper, newItem: Wallpaper) = oldItem.key == newItem.key
        override fun areContentsTheSame(oldItem: Wallpaper, newItem: Wallpaper) = oldItem == newItem
    }

    companion object {
        private const val PAYLOAD_LIKE = "like"
        private const val MENU_OPEN = 1
        private const val MENU_LIKE = 2
        private const val MENU_SHARE = 3
    }
}
