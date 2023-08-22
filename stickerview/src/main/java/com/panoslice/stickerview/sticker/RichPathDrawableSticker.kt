package com.panoslice.stickerview.sticker

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.annotation.IntRange
import com.xiaopo.flying.sticker.Sticker

class RichPathDrawableSticker(drawable: RichPathDrawable) : Sticker() {
    private override var drawable: RichPathDrawable
    private val realBounds: Rect
    private val imageRect: Rect
    protected var shadowIcon = false
    private var shadowPaint: Paint? = null

    init {
        this.drawable = drawable
        xDistance = 1f
        yDistance = 1f
        realBounds = Rect(0, 0, drawable.getWidth(), drawable.getHeight())
        imageRect = Rect(0, 0, drawable.getWidth(), drawable.getHeight())
        drawable.setBounds(imageRect)
        drawable.invalidateSelf()
    }

    constructor(drawable: RichPathDrawable, shadowIcon: Boolean) : this(drawable) {
        this.shadowIcon = shadowIcon
        if (this.shadowIcon) {
            shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            shadowPaint!!.style = Paint.Style.FILL
            shadowPaint!!.isAntiAlias = true
            shadowPaint!!.setShadowLayer(30f, 0f, 0f, Color.GRAY)
        }
    }

    override fun getDrawable(): Drawable {
        return drawable
    }

    override fun setDrawable(drawable: Drawable): RichPathDrawableSticker {
        this.drawable = drawable as RichPathDrawable
        return this
    }

    override fun draw(canvas: Canvas) {
        canvas.save()
        canvas.concat(matrix)
        drawable.draw(canvas)
        canvas.restore()
    }

    override fun setAlpha(@IntRange(from = 0, to = 255) alpha: Int): RichPathDrawableSticker {
        drawable.setAlpha(alpha)
        return this
    }

    override val width: Int
        get() {
            Log.d(TAG, "getWidth()" + drawable.getDrawableWidth())
            return (drawable.getDrawableWidth() * xDistance * xScale)
        }
    override val height: Int
        get() {
            Log.d(TAG, "getHeight()" + drawable.getDrawableHeight())
            return (drawable.getDrawableHeight() * yDistance * yScale)
        }
    override val fWidth: Float
        get() {
            Log.d(TAG, "getFWidth()")
            return drawable.getDrawableWidth() * xDistance * xScale
        }
    override val fHeight: Float
        get() {
            Log.d(TAG, "getFHeight()")
            return drawable.getDrawableHeight() * yDistance * yScale
        }

    override fun release() {
        super.release()
        if (drawable != null) {
            drawable = null
        }
    }

    override fun movedLeftHorizontally(distance: Float) {
        Log.d(TAG, "movedLeftHorizontally()")
        xScale = distance
        moveHorizontally()
    }

    override fun movedTopVertically(distance: Float) {
        Log.d(TAG, "movedTopVertically()")
        yScale = distance
        moveVertically()
    }

    override fun movedRightHorizontally(distance: Float) {
        Log.d(TAG, "movedRightHorizontally()")
        xScale = distance
        moveHorizontally()
    }

    override fun movedBottomVertically(distance: Float) {
        Log.d(TAG, "movedBottomVertically()")
        yScale = distance
        moveVertically()
    }

    override fun moveHorizontally() {
        imageRect[0, 0, width] = height
        drawable.setBounds(imageRect)
    }

    override fun moveVertically() {
        imageRect[0, 0, width] = height
        drawable.setBounds(imageRect)
    }

    override fun upRightHorizontally(distance: Float) {
        Log.d(TAG, "upRightHorizontally()")
        xDistance *= distance
        xScale *= distance
        moveHorizontally()
    }

    override fun upLeftHorizontally(distance: Float) {
        Log.d(TAG, "upLeftHorizontally()")
        xDistance *= distance
        yScale *= distance
        moveHorizontally()
    }

    override fun upTopVertically(distance: Float) {
        Log.d(TAG, "upTopVertically()")
        yDistance *= distance
        yScale = 1f
        moveVertically()
    }

    override fun upBottomVertically(distance: Float) {
        Log.d(TAG, "upBottomVertically()")
        yDistance *= distance
        yScale = 1f
        moveVertically()
    }

    companion object {
        private val TAG = RichPathDrawableSticker::class.java.simpleName
    }
}