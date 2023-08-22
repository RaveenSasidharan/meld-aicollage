package com.panoslice.stickerview.sticker

import android.view.MotionEvent

class HorizontalZoomIconEvent : StickerIconEvent {
    override fun onActionDown(stickerView: StickerView?, event: MotionEvent?) {}
    override fun onActionMove(stickerView: StickerView, event: MotionEvent) {
        // stickerView.strechHorizontally(event);
        stickerView.scaleHorizontalSticker(event)
    }

    override fun onActionUp(stickerView: StickerView, event: MotionEvent) {
        if (stickerView.onStickerOperationListener != null) {
//            stickerView.getOnStickerOperationListener()
//                    .onStickerZoomFinished(stickerView.getCurrentSticker());
            stickerView.onStickerOperationListener.onStickerHorizontalScale(stickerView.currentSticker)
        }
    }
}