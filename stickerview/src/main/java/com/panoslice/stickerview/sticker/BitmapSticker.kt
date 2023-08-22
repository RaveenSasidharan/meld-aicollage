package com.panoslice.stickerview.sticker

import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.IntRange
import com.panoslice.stickerview.sticker.StickerIconEvent
import com.panoslice.stickerview.sticker.StickerView
import com.panoslice.stickerview.sticker.StickerView.Flip
import com.panoslice.stickerview.sticker.Sticker
import com.panoslice.stickerview.sticker.AutoResizeTextSticker
import androidx.core.content.ContextCompat
import com.panoslice.stickerview.sticker.AutoResizeTextSticker.SizeTester
import com.panoslice.stickerview.sticker.DrawableSticker
import com.panoslice.stickerview.sticker.BitmapStickerIcon
import com.panoslice.stickerview.sticker.AbstractFlipEvent
import com.panoslice.stickerview.sticker.ImageDragEvent
import com.panoslice.stickerview.sticker.RichPathDrawableSticker
import com.panoslice.stickerview.sticker.RotateIconEvent
import com.panoslice.stickerview.sticker.StickerUtils
import com.panoslice.stickerview.sticker.StickerView.OnStickerOperationListener
import com.panoslice.stickerview.sticker.DeleteIconEvent
import com.panoslice.stickerview.sticker.ZoomIconEvent
import com.panoslice.stickerview.sticker.BoundBoxTopVerticalMoveEvent
import com.panoslice.stickerview.sticker.BoundBoxLeftHorizontalMoveEvent
import com.panoslice.stickerview.sticker.BoundBoxRightHorizontalMoveEvent
import com.panoslice.stickerview.sticker.BoundBoxBottomVerticalMoveEvent
import com.panoslice.stickerview.sticker.TextSticker
import androidx.core.view.MotionEventCompat
import com.panoslice.stickerview.sticker.BitmapSticker
import androidx.core.view.ViewCompat
import com.panoslice.stickerview.sticker.TextStickerDrawable

class BitmapSticker : Sticker {
    private val mBbitmap: Bitmap? = null
    private var realBounds: Rect
    private override var drawable: BitmapDrawable
    private val imageRect: RectF
    private var isOverlay: Boolean
    private var isFromTemplate = false
    private var layoutWidth = 0
    private var layoutHeight = 0
    var intensity = 100

    @JvmOverloads
    constructor(bitmap: Bitmap?, overlay: Boolean = false) {
        xDistance = 1f
        yDistance = 1f

        //   xScale = 0.864f;
        //   yScale = 0.864f;
        drawable = BitmapDrawable(null, bitmap)
        //        realBounds = new Rect(0, 0, width, height);
        realBounds = Rect(0, 0, width, height)
        imageRect = RectF(0, 0, fWidth, fHeight)
        isOverlay = overlay
    }

    constructor(bitmap: Bitmap?, layoutWIdth: Int, layoutHeight: Int) {
        this.layoutHeight = layoutHeight
        layoutWidth = layoutWIdth
        isFromTemplate = true
        xDistance = 1f
        yDistance = 1f

        //   xScale = 0.864f;
        //   yScale = 0.864f;
        drawable = BitmapDrawable(null, bitmap)
        //        realBounds = new Rect(0, 0, width, height);
        realBounds = Rect(0, 0, width, height)
        imageRect = RectF(0, 0, fWidth, fHeight)
        isOverlay = false
    }

    constructor(bitmap: Bitmap?, layoutWIdth: Int, layoutHeight: Int, overlay: Boolean) {
        this.layoutHeight = layoutHeight
        layoutWidth = layoutWIdth
        isFromTemplate = true
        xDistance = 1f
        yDistance = 1f

        //   xScale = 0.864f;
        //   yScale = 0.864f;
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

    val bitmapWidth: Int
        get() = if (isFromTemplate) {
            layoutWidth
        } else {
            drawable.bitmap.width
        }
    val bitmap: Bitmap
        get() = drawable.bitmap
    val bitmapHeight: Int
        get() = if (isFromTemplate) {
            layoutHeight
        } else {
            drawable.bitmap.height
        }
    override val width: Int
        get() = if (isFromTemplate) (layoutWidth * xDistance * xScale).toInt() else (drawable.bitmap.width * xDistance * xScale).toInt()
    override val height: Int
        get() = if (isFromTemplate) (layoutHeight * yDistance * yScale).toInt() else (drawable.bitmap.height * yDistance * yScale).toInt()
    override val fWidth: Float
        get() = if (isFromTemplate) layoutWidth * xDistance * xScale else drawable.bitmap.width * xDistance * xScale
    override val fHeight: Float
        get() = if (isFromTemplate) layoutHeight * yDistance * yScale else drawable.bitmap.height * yDistance * yScale

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
        val aspectRatio: Float
        val height: Float
        if (isFromTemplate) {
            aspectRatio = layoutWidth * 1f / layoutHeight
            height = layoutHeight * xDistance * xScale
        } else {
            aspectRatio = drawable.bitmap.width * 1f / drawable.bitmap.height
            height = drawable.bitmap.height * xDistance * xScale
        }
        if (isOverlay) {
            imageRect[0f, 0f, width.toFloat()] = height
        } else {
            if (height >= fHeight) {
                val top = (height - fHeight) / 2f //drawable.getIntrinsicHeight())/2);
                imageRect[0f, -top, fWidth] = height - top
            } else {
                val width = fHeight * aspectRatio
                val left = (width - fWidth) / 2f //drawable.getIntrinsicWidth())/2);
                imageRect[-left, 0f, width - left] = fHeight
            }
        }
    }

    override fun moveVertically() {
        realBounds[0, 0, width] = height
        val aspectRatio: Float
        val width: Float
        if (isFromTemplate) {
            aspectRatio = layoutWidth * 1f / layoutHeight
            width = layoutWidth * yDistance * yScale
        } else {
            aspectRatio = drawable.bitmap.width * 1f / drawable.bitmap.height
            width = drawable.bitmap.width * yDistance * yScale
        }
        if (isOverlay) {
            imageRect[0f, 0f, width] = height.toFloat()
        } else {
            if (width >= width) {
                val left = (width - width) / 2f //drawable.getIntrinsicHeight())/2);
                imageRect[-left, 0f, width - left] = height.toFloat()
            } else {
                val height = width / aspectRatio
                val top = (height - height) / 2f //drawable.getIntrinsicWidth())/2);
                imageRect[0f, -top, width] = height - top
            }
        }
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

    override fun setAlpha(@IntRange(from = 0, to = 255) alpha: Int): Sticker {
        drawable.alpha = alpha
        return this
    }

    override fun release() {
        super.release()
        if (drawable != null) {
            drawable = null
        }
    }
}