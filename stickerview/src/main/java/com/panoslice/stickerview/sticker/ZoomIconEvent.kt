package com.panoslice.stickerview.sticker

import android.view.MotionEvent
import com.panoslice.stickerview.sticker.StickerIconEvent
import com.panoslice.stickerview.sticker.StickerView

/**
 * @author wupanjie
 */
class ZoomIconEvent : StickerIconEvent {
    override fun onActionDown(stickerView: StickerView, event: MotionEvent) {}
    override fun onActionMove(stickerView: StickerView, event: MotionEvent) {
        stickerView.zoomCurrentSticker(event)
    }

    override fun onActionUp(stickerView: StickerView, event: MotionEvent) {
        if (stickerView.onStickerOperationListener != null) {
            stickerView.onStickerOperationListener!!
                .onStickerZoomFinished(stickerView.currentSticker!!)
        }
    }
}