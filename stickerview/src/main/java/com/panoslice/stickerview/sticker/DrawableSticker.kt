package com.panoslice.stickerview.sticker

import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
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

/**
 * @author wupanjie
 */
open class DrawableSticker(override var drawable: Drawable) : Sticker() {
    private val realBounds: Rect
    private val imageRect: RectF
    private val mTouchOffset = PointF()
    protected var shadowIcon = false
    private var shadowPaint: Paint? = null
    private var bitmap: Bitmap? = null

    init {
        xDistance = 1f
        yDistance = 1f
        val bitmapdrawable = drawable as BitmapDrawable
        if (bitmapdrawable.bitmap != null) {
            bitmap = bitmapdrawable.bitmap
        }
        realBounds = Rect(0, 0, width, height)
        imageRect = RectF(0, 0, fWidth, fHeight)
    }

    constructor(drawable: Drawable, shadowIcon: Boolean) : this(drawable) {
        this.shadowIcon = shadowIcon
        if (this.shadowIcon) {
            shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            shadowPaint!!.style = Paint.Style.FILL
            shadowPaint!!.isAntiAlias = true
            //      shadowPaint.setStrokeWidth(8);
            shadowPaint!!.setShadowLayer(30f, 0f, 0f, Color.GRAY)
            Log.d("DrawableSticker", " shadowIcon = $shadowIcon")
        }
    }

    override fun setDrawable(drawable: Drawable): DrawableSticker {
        this.drawable = drawable
        return this
    }

    override fun draw(canvas: Canvas) {
        val matrix = matrix
        canvas.save()
        canvas.concat(matrix)
        canvas.clipRect(realBounds)
        canvas.drawBitmap(bitmap!!, null, imageRect, shadowPaint)
        canvas.restore()
    }

    override fun setAlpha(@IntRange(from = 0, to = 255) alpha: Int): DrawableSticker {
        drawable.alpha = alpha
        return this
    }

    override val width: Int
        get() = (drawable.intrinsicWidth * xDistance * xScale).toInt()
    override val height: Int
        get() = (drawable.intrinsicHeight * yDistance * yScale).toInt()
    override val fWidth: Float
        get() = drawable.intrinsicWidth * xDistance * xScale
    override val fHeight: Float
        get() = drawable.intrinsicHeight * yDistance * yScale

    override fun release() {
        super.release()
        if (drawable != null) {
            drawable = null
        }
    }

    override fun movedLeftHorizontally(distance: Float) {
        xScale = distance
        moveHorizontally()
    }

    override fun movedTopVertically(distance: Float) {
        yScale = distance
        moveVertically()
    }

    override fun movedRightHorizontally(distance: Float) {
        xScale = distance
        moveHorizontally()
    }

    override fun movedBottomVertically(distance: Float) {
        yScale = distance
        moveVertically()
    }

    override fun moveHorizontally() {
        realBounds[0, 0, width] = height
        val aspectRatio = drawable.intrinsicWidth * 1f / drawable.intrinsicHeight
        val height = drawable.intrinsicHeight * xDistance * xScale
        if (height >= fHeight) {
            val top = (height - fHeight) / 2f //drawable.getIntrinsicHeight())/2);
            imageRect[0f, -top, fWidth] = height - top
        } else {
            val width = fHeight * aspectRatio
            val left = (width - fWidth) / 2f //drawable.getIntrinsicWidth())/2);
            imageRect[-left, 0f, width - left] = fHeight
        }
    }

    override fun moveVertically() {
        realBounds[0, 0, width] = height
        val aspectRatio = drawable.intrinsicWidth * 1f / drawable.intrinsicHeight
        val width = drawable.intrinsicWidth * yDistance * yScale
        if (width >= width) {
            val left = (width - width) / 2f //drawable.getIntrinsicHeight())/2);
            imageRect[-left, 0f, width - left] = height.toFloat()
        } else {
            val height = width / aspectRatio
            val top = (height - height) / 2f //drawable.getIntrinsicWidth())/2);
            imageRect[0f, -top, width] = height - top
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

    fun resizeImage(canvas: Canvas, matrix: Matrix?): Bitmap? {
        canvas.save()
        canvas.scale(xScale, yScale)
        /* draw whatever you want scaled at 0,0*/canvas.restore()
        return null
    }

    fun xresizeImage(canvas: Canvas, matrix: Matrix?): Bitmap? {
// convert image Drawable to bitmap
        canvas.save()
        canvas.concat(matrix)
        var bitmap: Bitmap? = null
        //    if (drawable instanceof BitmapDrawable) {
        val bitmapdrawable = drawable as BitmapDrawable
        if (bitmapdrawable.bitmap != null) {
            bitmap = bitmapdrawable.bitmap
        }
        Log.d("DrawableSticker", "resizebitmap bitmap != null = " + (bitmap != null))

//    final int color = 0xff424242;
        val paint = Paint()
        val rect = Rect(0, 0, bitmap!!.width, bitmap.height / 2)
        val rectF = RectF(imageRect)
        val roundPx = 0f
        paint.isAntiAlias = true
        //    canvas.drawARGB(0, 0, 0, 0);
//    paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint)
        //    paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
//    canvas.drawBitmap(bitmap, imageRect, rect, null);
        canvas.scale(xScale, yScale)
        canvas.restore()
        return null
    }

    fun xdraw(canvas: Canvas) {
        val matrix = matrix
        canvas.save()
        canvas.concat(matrix)

//    int height  = (int) (drawable.getIntrinsicHeight()*xScale);
//    if(height>getHeight()){
//      canvas.scale(xScale, xScale);
//    } else {
//      canvas.scale(yScale,yScale);
//    }
//    canvas.translate(xValue, yValue);
//    canvas.scale(xScale, yScale);
        /* draw whatever you want scaled at 0,0*/
        val imageBounds = Rect(
            0,
            0,
            width,
            height
        ) //You should move this line to constructor. You know, performance and all.

//    canvas.save();                 //save the current clip. You can't use canvasBounds to restore it later, there is no setClipBounds()
        canvas.clipRect(imageBounds)
        //    drawable.setBounds(realBounds);
//    drawable.draw(canvas);
        canvas.drawBitmap(bitmap!!, null, imageRect, null)
        canvas.restore()
        //    resizeImage(canvas, matrix);
    }
}