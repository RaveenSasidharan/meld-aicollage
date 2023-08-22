package com.panoslice.stickerview.sticker

import com.panoslice.stickerview.sticker.StickerView.Flip

/**
 * @author wupanjie
 */
class FlipVerticallyEvent : AbstractFlipEvent() {
    @get:Flip
    protected override val flipDirection: Int
        protected get() = StickerView.Companion.FLIP_VERTICALLY
}