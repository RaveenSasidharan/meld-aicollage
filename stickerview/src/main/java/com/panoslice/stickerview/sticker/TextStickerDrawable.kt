package com.panoslice.stickerview.sticker

import android.graphics.*
import android.graphics.drawable.Drawable
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

class TextStickerDrawable(private val width: Int, private val height: Int) : Drawable() {
    private val mDrawablePaint: Paint

    init {
        mDrawablePaint = Paint()
        mDrawablePaint.color = Color.TRANSPARENT
        mDrawablePaint.style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), mDrawablePaint)
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    override fun getIntrinsicHeight(): Int {
        return height
    }

    override fun getIntrinsicWidth(): Int {
        return width
    }

    override fun getOpacity(): Int {
        return PixelFormat.UNKNOWN
    }
}