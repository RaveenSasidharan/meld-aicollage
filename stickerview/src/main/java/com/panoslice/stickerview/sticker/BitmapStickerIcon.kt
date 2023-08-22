package com.panoslice.stickerview.sticker

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.MotionEvent
import androidx.annotation.IntDef
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
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * @author wupanjie
 */
class BitmapStickerIcon : DrawableSticker, StickerIconEvent {
    @IntDef(
        LEFT_TOP,
        RIGHT_TOP,
        LEFT_BOTTOM,
        RIGHT_BOTOM,
        MID_BOTTOM,
        MID_TOP,
        MID_LEFT,
        MID_RIGHT,
        MID_BOTTOM_CENTER,
        MID_LEFT_CENTER,
        MID_RIGHT_CENTER,
        MID_TOP_CENTER,
        EXT_LEFT_TOP
    )
    @Retention(
        RetentionPolicy.SOURCE
    )
    annotation class Gravity

    var iconRadius = DEFAULT_ICON_RADIUS
    var iconExtraRadius = DEFAULT_ICON_EXTRA_RADIUS
    var x = 0f
    var y = 0f
    var z = 0f

    @get:Gravity
    @Gravity
    var position = LEFT_TOP
    var iconEvent: StickerIconEvent? = null

    constructor(drawable: Drawable?, @Gravity gravity: Int) : super(
        drawable!!
    ) {
        //   this.context=context;
        position = gravity
    }

    constructor(drawable: Drawable, @Gravity gravity: Int, shadowIcon: Boolean) : super(
        drawable,
        shadowIcon
    ) {
        position = gravity
    }

    fun draw(canvas: Canvas, paint: Paint?) {
        canvas.drawCircle(x, y, iconRadius, paint!!)
        super.draw(canvas)
    }

    fun drawT(canvas: Canvas, paint: Paint?) {
        canvas.drawCircle(x, y, iconRadius, paint!!)
        super.draw(canvas)
    }

    override fun onActionDown(stickerView: StickerView?, event: MotionEvent?) {
        Log.e(TAG, "onActionDown$iconEvent")
        if (iconEvent != null) {
            iconEvent!!.onActionDown(stickerView, event)
        }
    }

    override fun onActionMove(stickerView: StickerView, event: MotionEvent) {
        Log.e(TAG, "onActionMove$iconEvent")
        if (iconEvent != null) {
            iconEvent!!.onActionMove(stickerView, event)
        }
    }

    override fun onActionUp(stickerView: StickerView, event: MotionEvent) {
        Log.e(TAG, "onActionUp$iconEvent")
        if (iconEvent != null) {
            iconEvent!!.onActionUp(stickerView, event)
        }
    }

    companion object {
        private val TAG = BitmapStickerIcon::class.java.simpleName
        const val DEFAULT_ICON_RADIUS = 30f
        const val DEFAULT_ICON_EXTRA_RADIUS = 10f
        const val LEFT_TOP = 0
        const val RIGHT_TOP = 1
        const val LEFT_BOTTOM = 2
        const val RIGHT_BOTOM = 3
        const val MID_BOTTOM = 4
        const val MID_LEFT = 5
        const val MID_RIGHT = 6
        const val MID_TOP = 7
        const val MID_BOTTOM_CENTER = 8
        const val MID_LEFT_CENTER = 9
        const val MID_RIGHT_CENTER = 10
        const val MID_TOP_CENTER = 11
        const val EXT_LEFT_TOP = 12
    }
}