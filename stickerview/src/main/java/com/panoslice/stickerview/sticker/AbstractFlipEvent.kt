package com.panoslice.stickerview.sticker

import android.view.MotionEvent
import com.panoslice.stickerview.sticker.StickerView.Flip

abstract class AbstractFlipEvent : StickerIconEvent {
    override fun onActionDown(stickerView: StickerView?, event: MotionEvent?) {}
    override fun onActionMove(stickerView: StickerView, event: MotionEvent) {}
    override fun onActionUp(stickerView: StickerView, event: MotionEvent) {
        stickerView.flipCurrentSticker(flipDirection)
    }

    @get:Flip
    protected abstract val flipDirection: Int
}