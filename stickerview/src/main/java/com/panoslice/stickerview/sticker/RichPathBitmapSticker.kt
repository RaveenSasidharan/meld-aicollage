package com.panoslice.stickerview.sticker

import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.IntRange
import com.xiaopo.flying.sticker.richpath.RichPathDrawable

class RichPathBitmapSticker(bitmap: Bitmap?, overlay: Boolean) : Sticker() {
    private val mBbitmap: Bitmap? = null
    private val realBounds: Rect
    private override var drawable: BitmapDrawable
    private val imageRect: RectF
    private val isOverlay: Boolean
    var richPathDrawable: RichPathDrawable? = null
    var intensity = 100

    constructor(bitmap: Bitmap?, drawable: RichPathDrawable?) : this(bitmap, false) {
        richPathDrawable = drawable
    }

    init {
        xDistance = 1f
        yDistance = 1f
        drawable = BitmapDrawable(null, bitmap)
        //        realBounds = new Rect(0, 0, width, height);
        realBounds = Rect(0, 0, width, height)
        imageRect = RectF(0, 0, fWidth, fHeight)
        isOverlay = overlay
    }

    override fun draw(canvas: Canvas) {
        canvas.save()
        canvas.concat(matrix)
        canvas.clipRect(realBounds)
        val paint = Paint()
        paint.alpha = (intensity * 255 / 100f).toInt()
        canvas.drawBitmap(drawable.bitmap, null, imageRect, paint)
        canvas.restore()
    }

    override val width: Int
        get() = (drawable.intrinsicWidth * xDistance * xScale).toInt()
    override val height: Int
        get() = (drawable.intrinsicHeight * yDistance * yScale).toInt()
    override val fWidth: Float
        get() = drawable.intrinsicWidth * xDistance * xScale
    override val fHeight: Float
        get() = drawable.intrinsicHeight * yDistance * yScale

    override fun movedLeftHorizontally(distance: Float) {
        xScale = distance
        moveHorizontally()
    }

    override fun movedRightHorizontally(distance: Float) {
        xScale = distance
        moveHorizontally()
    }

    override fun movedTopVertically(distance: Float) {
        yScale = distance
        moveVertically()
    }

    override fun movedBottomVertically(distance: Float) {
        yScale = distance
        moveVertically()
    }

    override fun moveHorizontally() {
        realBounds[0, 0, width] = height
        val aspectRatio = drawable.intrinsicWidth * 1f / drawable.intrinsicHeight
        val height = drawable.intrinsicHeight * xDistance * xScale
        imageRect[0f, 0f, width.toFloat()] = height
    }

    override fun moveVertically() {
        realBounds[0, 0, width] = height
        val aspectRatio = drawable.intrinsicWidth * 1f / drawable.intrinsicHeight
        val width = drawable.intrinsicWidth * yDistance * yScale
        imageRect[0f, 0f, width] = height.toFloat()
    }

    override fun upRightHorizontally(distance: Float) {
        xDistance *= distance
        xScale = 1f
        moveHorizontally()
    }

    override fun upLeftHorizontally(distance: Float) {
        xDistance *= distance
        xScale = 1f
        moveHorizontally()
    }

    override fun upTopVertically(distance: Float) {
        yDistance *= distance
        yScale = 1f
        moveVertically()
    }

    override fun upBottomVertically(distance: Float) {
        yDistance *= distance
        yScale = 1f
        moveVertically()
    }

    override fun setDrawable(drawable: Drawable): Sticker {
        this.drawable = drawable as BitmapDrawable
        return this
    }

    override fun getDrawable(): Drawable {
        return drawable
    }

    fun getRichPathDrawable(): RichPathDrawable? {
        return richPathDrawable
    }

    override fun setAlpha(@IntRange(from = 0, to = 255) alpha: Int): Sticker {
        drawable.alpha = alpha
        richPathDrawable.setAlpha(alpha)
        return this
    }

    override fun release() {
        super.release()
        if (drawable != null) {
            drawable = null
        }
    }
}