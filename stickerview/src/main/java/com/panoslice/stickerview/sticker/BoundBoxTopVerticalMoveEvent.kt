package com.panoslice.stickerview.sticker

import android.view.MotionEvent

class BoundBoxTopVerticalMoveEvent : StickerIconEvent {
    override fun onActionDown(stickerView: StickerView?, event: MotionEvent?) {}
    override fun onActionMove(stickerView: StickerView, event: MotionEvent) {
        stickerView.handleVerticalTopMovement(event)
    }

    override fun onActionUp(stickerView: StickerView, event: MotionEvent) {
        stickerView.handleVerticalTopUp(event)
        if (stickerView.onStickerOperationListener != null) {
            stickerView.onStickerOperationListener
                .onStickerVerticalMovementFinished(stickerView.currentSticker)
        }
    }
}