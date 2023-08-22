package com.panoslice.stickerview.sticker

import android.view.MotionEvent

class LockIconEvent : StickerIconEvent {
    override fun onActionDown(stickerView: StickerView?, event: MotionEvent?) {}
    override fun onActionMove(stickerView: StickerView, event: MotionEvent) {}
    override fun onActionUp(stickerView: StickerView, event: MotionEvent) {
        //stickerView.lockImages();
    }
}