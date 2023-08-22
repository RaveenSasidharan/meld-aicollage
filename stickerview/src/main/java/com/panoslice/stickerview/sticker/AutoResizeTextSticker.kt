package com.panoslice.stickerview.sticker

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.SparseIntArray
import android.util.TypedValue
import androidx.annotation.ColorInt
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

class AutoResizeTextSticker : Sticker {
    private interface SizeTester {
        /**
         *
         * @param suggestedSize
         * Size of text to be tested
         * @param availableSpace
         * available space in which text must fit
         * @return an integer < 0 if after applying `suggestedSize` to
         * text, it takes less space than `availableSpace`, > 0
         * otherwise
         */
        fun onTestSize(suggestedSize: Int, availableSpace: RectF?): Int
    }

    private val mTextRect = RectF()
    private var mAvailableSpaceRect: RectF? = null
    private var mRealBoud: Rect? = null
    private var mTextCachedSizes: SparseIntArray? = null
    private var mPaint: TextPaint? = null
    private var mMaxTextSize = 0f
    private var mSpacingMult = 1.0f
    private var mSpacingAdd = 0.0f
    private var mMinTextSize = 20f
    private var mWidthLimit = 0
    private var mMaxLines = 0
    private var mEnableSizeCache = true
    private var mInitiallized = false
    private var mContext: Context
    private var text: String? = null
    private var mDrawable: Drawable? = null
    private var mStaticLayout: StaticLayout? = null
    private var mAlignment: Layout.Alignment? = null

    constructor(context: Context) {
        mContext = context
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet?) {
        mContext = context
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) {
        mContext = context
        initialize()
    }

    private fun initialize() {
        mPaint = TextPaint(TextPaint.ANTI_ALIAS_FLAG)
        mMaxTextSize = Utils.convertSpToPx(mContext, 32f)
        mAvailableSpaceRect = RectF()
        mTextCachedSizes = SparseIntArray()
        if (mMaxLines == 0) {
            // no value was assigned during construction
            mMaxLines = NO_LINE_LIMIT
        }
        mInitiallized = true
        mDrawable = ContextCompat.getDrawable(mContext, R.drawable.sticker_transparent_background)
    }

    fun setText(text: String) {
        this.text = text
        adjustTextSize(text)
    }

    fun setTextSize(size: Float) {
        mMaxTextSize = size
        mPaint!!.textSize = size
        mTextCachedSizes!!.clear()
        adjustTextSize(text.toString())
    }

    var maxLines: Int
        get() = mMaxLines
        set(maxlines) {
            mMaxLines = maxlines
            reAdjust()
        }

    fun setSingleLine() {
        mMaxLines = 1
        reAdjust()
    }

    fun setSingleLine(singleLine: Boolean) {
        mMaxLines = if (singleLine) {
            1
        } else {
            NO_LINE_LIMIT
        }
        reAdjust()
    }

    fun setLines(lines: Int) {
        mMaxLines = lines
        reAdjust()
    }

    fun setTextSize(unit: Int, size: Float) {
        val c = mContext
        val r: Resources
        r = if (c == null) Resources.getSystem() else c.resources
        mMaxTextSize = TypedValue.applyDimension(
            unit, size,
            r.displayMetrics
        )
        mTextCachedSizes!!.clear()
        mPaint!!.textSize = mMaxTextSize
    }

    fun setLineSpacing(add: Float, mult: Float) {
        mSpacingMult = mult
        mSpacingAdd = add
    }

    /**
     * Set the lower text size limit and invalidate the view
     *
     * @param minTextSize
     */
    fun setMinTextSize(minTextSize: Float) {
        mMinTextSize = minTextSize
        reAdjust()
    }

    private fun reAdjust() {
        adjustTextSize(text.toString())
    }

    private fun adjustTextSize(string: String) {
        if (!mInitiallized) {
            return
        }
        val startSize = mMinTextSize.toInt()
        val heightLimit = height
        mWidthLimit = width
        mAvailableSpaceRect!!.right = mWidthLimit.toFloat()
        mAvailableSpaceRect!!.bottom = heightLimit.toFloat()
        setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            efficientTextSizeSearch(
                startSize, mMaxTextSize.toInt(),
                mSizeTester, mAvailableSpaceRect
            ).toFloat()
        )
    }

    override fun draw(canvas: Canvas) {
        val matrix = matrix
        canvas.save()
        canvas.concat(matrix)
        if (mDrawable != null) {
            if (mRealBoud == null) mRealBoud = Rect()
            mAvailableSpaceRect!!.roundOut(mRealBoud!!)
            mDrawable!!.bounds = mRealBoud!!
            mDrawable!!.draw(canvas)
        }
        canvas.restore()
        canvas.save()
        canvas.concat(matrix)
        if (mTextRect.width() == width.toFloat()) {
            val dy = height / 2 - mStaticLayout!!.height / 2
            // center vertical
            canvas.translate(0f, dy.toFloat())
        } else {
            val dx = mTextRect.left
            val dy = mTextRect.top + mTextRect.height() / 2 - mStaticLayout!!.height / 2
            canvas.translate(dx, dy)
        }
        mStaticLayout!!.draw(canvas)
        canvas.restore()
    }

    fun setTypeface(typeface: Typeface?): AutoResizeTextSticker {
        mPaint!!.typeface = typeface
        return this
    }

    fun setUnderLine(isUnderLime: Boolean): AutoResizeTextSticker {
        mPaint!!.isUnderlineText = isUnderLime
        return this
    }

    fun setBold(isBold: Boolean): AutoResizeTextSticker {
        mPaint!!.isFakeBoldText = isBold
        return this
    }

    fun setItalic(isItalic: Boolean): AutoResizeTextSticker {
        if (isItalic) mPaint!!.textSkewX = -0.25f else mPaint!!.textSkewX = 0f
        return this
    }

    fun setTextStrikeThrough(isStrikeThroughRext: Boolean): AutoResizeTextSticker {
        mPaint!!.isStrikeThruText = isStrikeThroughRext
        return this
    }

    fun setTextColor(@ColorInt color: Int): AutoResizeTextSticker {
        mPaint!!.color = color
        return this
    }

    fun setTextAlign(alignment: Layout.Alignment): AutoResizeTextSticker {
        mAlignment = alignment
        return this
    }

    private val mSizeTester: SizeTester = object : SizeTester {
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        override fun onTestSize(suggestedSize: Int, availableSPace: RectF?): Int {
            mPaint!!.textSize = suggestedSize.toFloat()
            val text = text.toString()
            val singleline = maxLines == 1
            if (singleline) {
                mTextRect.bottom = mPaint!!.fontSpacing
                mTextRect.right = mPaint!!.measureText(text)
                mStaticLayout = StaticLayout(
                    text,
                    mPaint,
                    mTextRect.width().toInt(),
                    Layout.Alignment.ALIGN_NORMAL,
                    mSpacingMult,
                    mSpacingAdd,
                    true
                )
            } else {
                mStaticLayout = StaticLayout(
                    text, mPaint,
                    mWidthLimit, Layout.Alignment.ALIGN_NORMAL, mSpacingMult,
                    mSpacingAdd, true
                )
                // return early if we have more lines
                if (maxLines != NO_LINE_LIMIT
                    && mStaticLayout!!.lineCount > maxLines
                ) {
                    return 1
                }
                mTextRect.bottom = mStaticLayout!!.height.toFloat()
                var maxWidth = -1
                for (i in 0 until mStaticLayout!!.lineCount) {
                    if (maxWidth < mStaticLayout!!.getLineWidth(i)) {
                        maxWidth = mStaticLayout!!.getLineWidth(i).toInt()
                    }
                }
                mTextRect.right = maxWidth.toFloat()
            }
            mTextRect.offsetTo(0f, 0f)
            return if (availableSPace!!.contains(mTextRect)) {
                // may be too small, don't worry we will find the best match
                -1
            } else {
                // too big
                1
            }
        }
    }

    /**
     * Enables or disables size caching, enabling it will improve performance
     * where you are animating a value inside TextView. This stores the font
     * size against getText().length() Be careful though while enabling it as 0
     * takes more space than 1 on some fonts and so on.
     *
     * @param enable
     * enable font size caching
     */
    fun enableSizeCache(enable: Boolean) {
        mEnableSizeCache = enable
        mTextCachedSizes!!.clear()
        adjustTextSize(text.toString())
    }

    private fun efficientTextSizeSearch(
        start: Int, end: Int,
        sizeTester: SizeTester, availableSpace: RectF?
    ): Int {
        if (!mEnableSizeCache) {
            return binarySearch(start, end, sizeTester, availableSpace)
        }
        val text = text.toString()
        val key = text?.length ?: 0
        var size = mTextCachedSizes!![key]
        if (size != 0) {
            return size
        }
        size = binarySearch(start, end, sizeTester, availableSpace)
        mTextCachedSizes!!.put(key, size)
        return size
    }

    override val width: Int
        get() = (mDrawable!!.intrinsicWidth * xScale).toInt()
    override val height: Int
        get() = (mDrawable!!.intrinsicHeight * yScale).toInt()

    override fun movedRightHorizontally(scale: Float) {
        xScale = scale
        mAvailableSpaceRect!![0f, 0f, width.toFloat()] = height.toFloat()
        mTextRect[0f, 0f, width.toFloat()] = height.toFloat()
        reAdjust()
    }

    override fun movedLeftHorizontally(scale: Float) {
        xScale = scale
        mAvailableSpaceRect!![0f, 0f, width.toFloat()] = height.toFloat()
        mTextRect[0f, 0f, width.toFloat()] = height.toFloat()
        reAdjust()
    }

    //    @Override
    //    public void movedVertically(float scale) {
    //
    //        yScale = scale;
    //        mAvailableSpaceRect.set(0, 0, getWidth(), getHeight());
    //        mTextRect.set(0, 0, getWidth(), getHeight());
    //        reAdjust();
    //    }
    override fun movedTopVertically(distance: Float) {}
    override fun movedBottomVertically(distance: Float) {}
    override fun release() {
        super.release()
        if (mDrawable != null) {
            mDrawable = null
        }
    }

    override fun setDrawable(drawable: Drawable): Sticker {
        mDrawable = drawable
        return this
    }

    override val drawable: Drawable
        get() = mDrawable!!

    override fun setAlpha(alpha: Int): Sticker {
        return null
    }

    companion object {
        private const val NO_LINE_LIMIT = -1
        private fun binarySearch(
            start: Int, end: Int, sizeTester: SizeTester,
            availableSpace: RectF?
        ): Int {
            var lastBest = start
            var lo = start
            var hi = end - 1
            var mid = 0
            while (lo <= hi) {
                mid = lo + hi ushr 1
                val midValCmp = sizeTester.onTestSize(mid, availableSpace)
                if (midValCmp < 0) {
                    lastBest = lo
                    lo = mid + 1
                } else if (midValCmp > 0) {
                    hi = mid - 1
                    lastBest = hi
                } else {
                    return mid
                }
            }
            // make sure to return last best
            // this is what should always be returned
            return lastBest
        }
    }
}