package com.skd.wallpaper.utils

import android.widget.ImageView
import com.skd.wallpaper.services.AlphabetImageMapper

object Validations {
    fun setImageForName(name: String, imageView: ImageView) {
        val firstChar = name.getOrNull(0)?.uppercaseChar() ?: ' '
        val imageResource = AlphabetImageMapper.getImageResourceForLetter(firstChar)
        imageView.setImageResource(imageResource)
    }
}