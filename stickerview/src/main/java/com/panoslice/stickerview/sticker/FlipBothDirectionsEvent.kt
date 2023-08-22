package com.panoslice.stickerview.sticker

import com.panoslice.stickerview.sticker.StickerView.Flip

/**
 * @author wupanjie
 */
class FlipBothDirectionsEvent : AbstractFlipEvent() {
    @get:Flip
    protected override val flipDirection: Int
        protected get() = StickerView.Companion.FLIP_VERTICALLY or StickerView.Companion.FLIP_HORIZONTALLY
}