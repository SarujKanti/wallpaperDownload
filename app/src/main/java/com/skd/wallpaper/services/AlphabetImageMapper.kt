package com.skd.wallpaper.services
import com.skd.wallpaper.R
object AlphabetImageMapper {
    fun getImageResourceForLetter(firstChar: Char): Int {
        return when (firstChar.lowercaseChar()) {
            'a', 'A' -> R.drawable.ic_a
            'b', 'B' -> R.drawable.ic_b
            'c', 'C' -> R.drawable.ic_c
            'd', 'D' -> R.drawable.ic_d
            'e', 'E' -> R.drawable.ic_e
            'f', 'F' -> R.drawable.ic_f
            'g', 'G' -> R.drawable.ic_g
            'h', 'H' -> R.drawable.ic_h
            'i', 'I' -> R.drawable.ic_i
            'j', 'J' -> R.drawable.ic_j
            'k', 'K' -> R.drawable.ic_k
            'l', 'L' -> R.drawable.ic_l
            'm', 'M' -> R.drawable.ic_m
            'n', 'N' -> R.drawable.ic_n
            'o', 'O' -> R.drawable.ic_o
            'p', 'P' -> R.drawable.ic_p
            'q', 'Q' -> R.drawable.ic_q
            'r', 'R' -> R.drawable.ic_r
            's', 'S' -> R.drawable.ic_s
            't', 'T' -> R.drawable.ic_t
            'u', 'U' -> R.drawable.ic_u
            'v', 'V' -> R.drawable.ic_v
            'w', 'W' -> R.drawable.ic_w
            'x', 'X' -> R.drawable.ic_x
            'y', 'Y' -> R.drawable.ic_y
            'z', 'Z' -> R.drawable.ic_z
            '1' -> R.drawable.ic_1
            '2' -> R.drawable.ic_2
            '3' -> R.drawable.ic_3
            '4' -> R.drawable.ic_4
            '5' -> R.drawable.ic_5
            '6' -> R.drawable.ic_6
            '7' -> R.drawable.ic_7
            '8' -> R.drawable.ic_8
            '9' -> R.drawable.ic_9
            else -> R.drawable.ic_launcher_background // Default placeholder
        }
    }
}