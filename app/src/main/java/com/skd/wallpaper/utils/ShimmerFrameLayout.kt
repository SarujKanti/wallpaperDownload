package com.skd.wallpaper.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.os.SystemClock
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.skd.wallpaper.R

/**
 * Draws its children (the grey skeleton shapes) with a soft light band sweeping across them,
 * for loading placeholders. The band is drawn only on top of the shapes, never on the gaps.
 * The sweep is driven by the clock, so every skeleton on screen shimmers in sync.
 */
class ShimmerFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val shine = ContextCompat.getColor(context, R.color.skeleton_shine)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }
    private var shader: LinearGradient? = null

    init {
        setWillNotDraw(false)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // A band about as wide as the view: transparent → light → transparent
        shader = LinearGradient(
            0f, 0f, w.toFloat(), 0f,
            intArrayOf(shine and 0x00FFFFFF, shine, shine and 0x00FFFFFF),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun dispatchDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val band = shader
        if (w <= 0f || band == null) return super.dispatchDraw(canvas)

        val layer = canvas.saveLayer(0f, 0f, w, h, null)
        super.dispatchDraw(canvas)
        // Sweep from fully left of the view to fully right of it
        val progress = (SystemClock.uptimeMillis() % DURATION_MS) / DURATION_MS.toFloat()
        val offset = -w + progress * 2 * w
        canvas.save()
        canvas.translate(offset, 0f)
        paint.shader = band
        canvas.drawRect(0f, 0f, w, h, paint)
        canvas.restore()
        canvas.restoreToCount(layer)

        // Keep animating while on screen
        if (isAttachedToWindow && isShown) postInvalidateOnAnimation()
    }

    private companion object {
        const val DURATION_MS = 1400L
    }
}
