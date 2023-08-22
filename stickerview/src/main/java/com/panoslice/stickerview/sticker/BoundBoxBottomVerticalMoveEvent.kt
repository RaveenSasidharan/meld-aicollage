package com.panoslice.stickerview.sticker

import android.view.MotionEvent

class BoundBoxBottomVerticalMoveEvent : StickerIconEvent {
    override fun onActionDown(stickerView: StickerView?, event: MotionEvent?) {}
    override fun onActionMove(stickerView: StickerView, event: MotionEvent) {
        stickerView.handleVerticalBottomMovement(event)
    }

    override fun onActionUp(stickerView: StickerView, event: MotionEvent) {
        stickerView.handleVerticalBottomUp(event)
        if (stickerView.onStickerOperationListener != null) {
            stickerView.onStickerOperationListener
                .onStickerVerticalMovementFinished(stickerView.currentSticker)
        }
    }
}