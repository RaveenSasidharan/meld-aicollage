package com.panoslice.stickerview.sticker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
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

/**
 * Customize your sticker with text and image background.
 * You can place some text into a given region, however,
 * you can also add a plain text sticker. To support text
 * auto resizing , I take most of the code from AutoResizeTextView.
 * See https://adilatwork.blogspot.com/2014/08/android-textview-which-resizes-its-text.html
 * Notice: It's not efficient to add long text due to too much of
 * StaticLayout object allocation.
 * Created by liutao on 30/11/2016.
 */
class TextSticker : Sticker {
    private val isFromTemplate = false
    private var layout_height = 0
    private var layout_width = 0
    private val context: Context
    private val realBounds: Rect
    private val textRect: Rect
    private val textPaint: TextPaint
    override var drawable: Drawable
        private set
    private var staticLayout: StaticLayout? = null
    private var alignment: Layout.Alignment
    var text: String? = null
        private set

    /**
     * Upper bounds for text size.
     * This acts as a starting point for resizing.
     */
    private var maxTextSizePixels: Float
    /**
     * @return lower text size limit, in pixels.
     */
    /**
     * Lower bounds for text size.
     */
    var minTextSizePixels: Float
        private set

    /**
     * Line spacing multiplier.
     */
    private var lineSpacingMultiplier = 1.0f

    /**
     * Additional line spacing.
     */
    private var lineSpacingExtra = 0.0f
    private var fontSize: Float
    private var fontScale: Float
    private val minHeight = 125f
    private var letterSpacing = 0f
    var textSize = 0f

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context) : this(context, null) {
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, drawable: Drawable?) {
        this.context = context
        this.drawable = drawable!!
        if (drawable == null) {
            this.drawable =
                ContextCompat.getDrawable(context, R.drawable.sticker_transparent_background)!!
        }
        xDistance = 1f
        yDistance = 1f
        textPaint = TextPaint(TextPaint.ANTI_ALIAS_FLAG)
        realBounds = Rect(0, 0, width, height)
        textRect = Rect(0, 0, width, height)
        minTextSizePixels = convertSpToPx(20f)
        maxTextSizePixels = convertSpToPx(32f)
        alignment = Layout.Alignment.ALIGN_CENTER
        fontSize = 20f
        textPaint.textSize = fontSize
        textPaint.letterSpacing = letterSpacing
        alignment.getDeclaringClass()
        fontSize = textPaint.textSize //convertSpToPx(textPaint.getTextSize());
        fontScale = 1f
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, layout_width: Int, layout_height: Int) {
        this.context = context
        this.layout_height = layout_height
        this.layout_width = layout_width
        drawable = TextStickerDrawable(layout_width, layout_height)
        xDistance = 1f
        yDistance = 1f
        Log.e("drawWidth", "width" + width + "height" + height)
        textPaint = TextPaint(TextPaint.ANTI_ALIAS_FLAG)
        realBounds = Rect(0, 0, width, height)
        textRect = Rect(0, 0, width, height)
        minTextSizePixels = convertSpToPx(20f)
        maxTextSizePixels = convertSpToPx(32f)
        alignment = Layout.Alignment.ALIGN_CENTER
        fontSize = 20f
        textPaint.textSize = fontSize
        textPaint.letterSpacing = letterSpacing
        alignment.getDeclaringClass()
        fontSize = textPaint.textSize //convertSpToPx(textPaint.getTextSize());
        fontScale = 1f
    }

    override fun draw(canvas: Canvas) {
        val matrix = matrix
        canvas.save()
        canvas.concat(matrix)
        if (drawable != null) {
            drawable.bounds = realBounds
            drawable.draw(canvas)
        }
        canvas.restore()
        canvas.save()
        canvas.concat(matrix)
        if (textRect.width() == width) {
            val dy = height / 2 - staticLayout!!.height / 2
            // center vertical
            canvas.translate(0f, dy.toFloat())
        } else {
            val dx = textRect.left
            val dy = textRect.top + textRect.height() / 2 - staticLayout!!.height / 2
            canvas.translate(dx.toFloat(), dy.toFloat())
        }
        staticLayout!!.draw(canvas)
        canvas.restore()
    }

    override val width: Int
        get() = (drawable.intrinsicWidth * xDistance * xScale).toInt()
    override val height: Int
        get() = (drawable.intrinsicHeight * yDistance * yScale).toInt()

    fun scaleText(distance: Float) {
        val tempFontSize = fontSize * distance
        drawFont(tempFontSize)
    }

    fun getFitTextSize(newSize: Float): Float {
        var newSize = newSize
        val nowWidth = textPaint.measureText(text)
        newSize = width.toFloat() / nowWidth * textPaint.textSize
        return newSize
    }

    fun drawFontOnTextContentChange(updateHeight: Boolean) {
        val minYDistance = calculateMinYDistance(fontSize, updateHeight)
        yDistance = minYDistance
        realBounds[0, 0, width] = height
        textRect[0, 0, width] = height
        drawText(fontSize)
    }

    fun drawFont() {
        drawFont(fontSize)
        Log.d("TextSticker", "drawFont: " + textPaint.textSize)
    }

    fun drawFont(fs: Float) {
        val minYDistance = calculateMinYDistance(fs)
        //        if(yDistance<minYDistance){
        yDistance = minYDistance

//        }
        realBounds[0, 0, width] = height
        textRect[0, 0, width] = height
        //    setMaxTextSize(tempFontSize);
//    fontSize=tempFontSize;
        drawText(fs)
    }

    @JvmOverloads
    fun calculateMinYDistance(fs: Float = fontSize, updateHeight: Boolean = true): Float {
        val text: CharSequence? = text
        val availableWidthPixels = width
        // Safety check
        // (Do not resize if the view does not have dimensions or if there is no text)
        if (text == null || text.length <= 0 || availableWidthPixels <= 0) {
            return 1
        }
        var targetTextHeightPixels = getTextHeightPixels(text, availableWidthPixels, fs)
        if (updateHeight == false) {
            if (targetTextHeightPixels < height) {
                targetTextHeightPixels = height
            }
        }
        if (targetTextHeightPixels * getMatrixScale(matrix) < minHeight) {
            targetTextHeightPixels = (minHeight / getMatrixScale(matrix)).toInt()
        }
        Log.d(
            "TextSticker",
            "targetheight = " + targetTextHeightPixels + ", scaledHeight = " + targetTextHeightPixels * getMatrixScale(
                matrix
            )
        )
        return targetTextHeightPixels / (1f * drawable.intrinsicHeight)
    }

    fun drawText(fs: Float) {
        textPaint.textSize = fs
        if (text != null) staticLayout = StaticLayout(
            text, textPaint, textRect.width(), alignment, lineSpacingMultiplier,
            lineSpacingExtra, true
        )
    }

    fun onZoomFinished(distance: Float) {
        fontSize = fontSize * distance
        Log.d("TextSticker", "onZoomFinished: " + textPaint.textSize)
        drawText(fontSize)
    }

    fun onFontSizeValue(x: Float) {
        fontSize = x
        Log.d("TextSticker", "onFontSizeValue: " + textPaint.textSize)
        drawText(x)
    }

    override fun movedLeftHorizontally(distance: Float) {
//    xDistance += distance;
//    int oldWidth = getWidth();
        xScale = distance
        //    float tx = (getWidth()-oldWidth)/2f;
//    getMatrix().postTranslate(-tx,  0);
        moveHorizontally()
        Log.d("TextSticker", "movedLeftHorizontally: " + getFontSize())
    }

    override fun movedRightHorizontally(distance: Float) {
//    xDistance += distance;
        xScale = distance
        moveHorizontally()
        Log.d("TextSticker", "movedRightHorizontally: " + getFontSize())
    }

    override fun moveHorizontally() {
        Log.d("Text Sticker", "width=$width")
        //    Toast.makeText(context,"Sticker Text width="+getWidth(),Toast.LENGTH_LONG).show();
        realBounds[0, 0, width] = height
        textRect[0, 0, width] = height
        //    resizeText();
//    drawText(fontSize);
        scaleText(1f)
        Log.d("TextSticker", "moveHorizontally: " + getFontSize())
    }

    override fun moveVertically() {
        realBounds[0, 0, width] = height
        textRect[0, 0, width] = height
        //    resizeText();
        drawText(fontSize)
        //    scaleText(1);
        Log.d("TextSticker", "moveVertically: " + getFontSize())
    }

    override fun movedTopVertically(distance: Float) {
//    yDistance += distance;
//    int oldHeight = getHeight();
        yScale = distance
        val minYDistance = calculateMinYDistance(fontSize)
        if (yScale * yDistance < minYDistance) {
            yScale = minYDistance / yDistance
        }
        //    float ty = (getHeight()-oldHeight)/2f;
//    getMatrix().postTranslate(0,  -ty);
        moveVertically()
        Log.d("TextSticker", "movedTopVertically: " + getFontSize())
    }

    override fun movedBottomVertically(distance: Float) {
//    yDistance += distance;
        yScale = distance
        val minYDistance = calculateMinYDistance(fontSize)
        if (yScale * yDistance < minYDistance) {
            yScale = minYDistance / yDistance
        }
        moveVertically()
        Log.d("TextSticker", "movedBottomVertically: " + getFontSize())
    }

    override fun upRightHorizontally(distance: Float) {
        xDistance *= distance
        xScale = 1f
        moveHorizontally()
        //    getMatrix().postScale(distance,1);
        Log.d("TextSticker", "upRightHorizontally: " + getFontSize())
    }

    override fun upLeftHorizontally(distance: Float) {
        xDistance *= distance
        xScale = 1f
        moveHorizontally()
        //    getMatrix().postScale(distance,1);
        Log.d("TextSticker", "upLeftHorizontally: " + getFontSize())
    }

    override fun upTopVertically(distance: Float) {
        yDistance *= distance
        yScale = 1f
        val minYDistance = calculateMinYDistance(fontSize)
        if (yDistance < minYDistance) {
            yDistance = minYDistance
        }
        moveVertically()
        Log.d("TextSticker", "upTopVertically: " + getFontSize())
        //    getMatrix().postScale(distance,1);
    }

    override fun upBottomVertically(distance: Float) {
        yDistance *= distance
        yScale = 1f
        val minYDistance = calculateMinYDistance(fontSize)
        if (yDistance < minYDistance) {
            yDistance = minYDistance
        }
        moveVertically()
        Log.d("TextSticker", "upBottomVertically: " + getFontSize())
        //    getMatrix().postScale(distance,1);
    }

    override fun release() {
        super.release()
        if (drawable != null) {
            drawable = null
        }
    }

    override fun setAlpha(@IntRange(from = 0, to = 255) alpha: Int): TextSticker {
        textPaint.alpha = alpha
        return this
    }

    override fun setDrawable(drawable: Drawable): TextSticker {
        this.drawable = drawable
        realBounds[0, 0, width] = height
        textRect[0, 0, width] = height
        return this
    }

    fun setDrawable(drawable: Drawable, region: Rect?): TextSticker {
        this.drawable = drawable
        realBounds[0, 0, width] = height
        if (region == null) {
            textRect[0, 0, width] = height
        } else {
            textRect[region.left, region.top, region.right] = region.bottom
        }
        return this
    }

    fun setTypeface(typeface: Typeface?): TextSticker {
        textPaint.typeface = typeface
        Log.d("TextSticker", "setTypeface: " + textPaint.typeface)
        return this
    }

    val typeface: Typeface
        get() = textPaint.typeface

    fun setUnderLine(isUnderLime: Boolean): TextSticker {
        textPaint.isUnderlineText = isUnderLime
        return this
    }

    fun setBold(isBold: Boolean): TextSticker {
        textPaint.isFakeBoldText = isBold
        return this
    }

    val bold: TextSticker
        get() {
            textPaint.isFakeBoldText
            return this
        }
    val italic: TextSticker
        get() {
            textPaint.textSkewX
            return this
        }
    val textStrikeThrough: TextSticker
        get() {
            textPaint.isStrikeThruText
            return this
        }
    val underline: TextSticker
        get() {
            textPaint.isUnderlineText
            return this
        }

    fun setItalic(isItalic: Boolean): TextSticker {
        if (isItalic) textPaint.textSkewX = -0.25f else textPaint.textSkewX = 0f
        return this
    }

    fun setTextStrikeThrough(isStrikeThroughRext: Boolean): TextSticker {
        textPaint.isStrikeThruText = isStrikeThroughRext
        return this
    }

    fun setTextColor(@ColorInt color: Int): TextSticker {
        textPaint.color = color
        return this
    }

    val textColor: TextSticker
        get() {
            textPaint.color
            return this
        }

    fun getLineSpacingMultiplier(): TextSticker {
        textPaint.fontSpacing
        return this
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun getLetterSpacing(): TextSticker {
        textPaint.letterSpacing
        return this
    }

    fun setTextAlign(alignmnt: Layout.Alignment): TextSticker {
        alignment = alignmnt
        Log.d("TextSticker", "setTextAlign: $alignment::$alignmnt")
        return this
    }

    fun getAlignment(align: Layout.Alignment): TextSticker {
        alignment = align
        Log.d("TextSticker", "getAlignment: " + alignment + "align" + align)
        return this
    }

    fun setMaxTextSize(@Dimension(unit = Dimension.SP) size: Float): TextSticker {
        minTextSizePixels = size
        textPaint.textSize = convertSpToPx(size)
        maxTextSizePixels = textPaint.textSize
        fontSize = maxTextSizePixels
        Log.d("TextSticker", "setMaxTextSize: $size")
        return this
    }

    fun setFontSize(@Dimension(unit = Dimension.SP) size: Float): TextSticker {
        fontSize = size / currentScale
        return this
    }

    fun setFontSizeWithoutScale(@Dimension(unit = Dimension.SP) size: Float): TextSticker {
        fontSize = size
        return this
    }

    fun getFontSize(): Float {
        return textPaint.textSize * currentScale
    }

    /**
     * Sets the lower text size limit
     *
     * @param minTextSizeScaledPixels the minimum size to use for text in this view,
     * in scaled pixels.
     */
    fun setMinTextSize(minTextSizeScaledPixels: Float): TextSticker {
        minTextSizePixels = convertSpToPx(minTextSizeScaledPixels)
        return this
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun setLetterSpacing(letterspacing: Float): TextSticker {
        textPaint.letterSpacing = letterspacing
        letterSpacing = textPaint.letterSpacing
        return this
    }

    fun setLineSpacing(add: Float, multiplier: Float): TextSticker {
        lineSpacingMultiplier = multiplier
        lineSpacingExtra = add
        return this
    }

    fun setText(text: String?): TextSticker {
        this.text = text
        return this
    }

    fun resizeText(): TextSticker {
        drawFont()
        return this
    }

    /**
     * Resize this view's text size with respect to its width and height
     * (minus padding). You should always call this method after the initialization.
     */
    fun adjustFontSize(): TextSticker {
        val availableHeightPixels = textRect.height()
        val availableWidthPixels = textRect.width()
        val text: CharSequence? = text
        // Safety checkTextSticker: dr
        // (Do not resize if the view does not have dimensions or if there is no text)
        if (text == null || text.length <= 0 || availableHeightPixels <= 0 || availableWidthPixels <= 0 || maxTextSizePixels <= 0) {
            return this
        }
        var targetTextSizePixels = maxTextSizePixels
        var targetTextHeightPixels =
            getTextHeightPixels(text, availableWidthPixels, targetTextSizePixels)

        // Until we either fit within our TextView
        // or we have reached our minimum text size,
        // incrementally try smaller sizes
        while (targetTextHeightPixels > availableHeightPixels
            && targetTextSizePixels > 0
        ) //minTextSizePixels)
        {
            targetTextSizePixels = Math.max(targetTextSizePixels - 2, 1f) //minTextSizePixels);
            targetTextHeightPixels =
                getTextHeightPixels(text, availableWidthPixels, targetTextSizePixels)
        }

        // If we have reached our minimum text size and the text still doesn't fit,
        // append an ellipsis
        // (NOTE: Auto-ellipsize doesn't work hence why we have to do it here)
//    if (targetTextSizePixels == minTextSizePixels
//            && targetTextHeightPixels > availableHeightPixels) {
//      // Make a copy of the original TextPaint object for measuring
//      TextPaint textPaintCopy = new TextPaint(textPaint);
//      textPaintCopy.setTextSize(targetTextSizePixels);
//
//      // Measure using a StaticLayout instance
//      StaticLayout staticLayout =
//              new StaticLayout(text, textPaintCopy, availableWidthPixels, Layout.Alignment.ALIGN_NORMAL,
//                      lineSpacingMultiplier, lineSpacingExtra, false);
//
//      // Check that we have a least one line of rendered text
//      if (staticLayout.getLineCount() > 0) {
//        // Since the line at the specific vertical position would be cut off,
//        // we must trim up to the previous line and add an ellipsis
//        int lastLine = staticLayout.getLineForVertical(availableHeightPixels) - 1;
//
//        if (lastLine >= 0) {
//          int startOffset = staticLayout.getLineStart(lastLine);
//          int endOffset = staticLayout.getLineEnd(lastLine);
//          float lineWidthPixels = staticLayout.getLineWidth(lastLine);
//          float ellipseWidth = textPaintCopy.measureText(mEllipsis);
//
//          // Trim characters off until we have enough room to draw the ellipsis
//          while (availableWidthPixels < lineWidthPixels + ellipseWidth) {
//            endOffset--;
//            lineWidthPixels =
//                    textPaintCopy.measureText(text.subSequence(startOffset, endOffset + 1).toString());
//          }
//
//          setText(text.subSequence(0, endOffset) + mEllipsis);
//        }
//      }
//    }
        fontSize = targetTextSizePixels
        drawText(targetTextSizePixels)
        return this
    }

    /**
     * Sets the text size of a clone of the view's [TextPaint] object
     * and uses a [StaticLayout] instance to measure the height of the text.
     * Q
     * @return the height of the text when placed in a view
     * with the specified width
     * and when the text has the specified size.
     */
    protected fun getTextHeightPixels(
        source: CharSequence, availableWidthPixels: Int,
        textSizePixels: Float
    ): Int {
        textPaint.textSize = textSizePixels
        // It's not efficient to create a StaticLayout instance
        // every time when measuring, we can use StaticLayout.Builder
        // since api 23.
        val staticLayout = StaticLayout(
            source, textPaint, availableWidthPixels, Layout.Alignment.ALIGN_NORMAL,
            lineSpacingMultiplier, lineSpacingExtra, true
        )
        return staticLayout.height
    }

    /**
     * @return the number of pixels which scaledPixels corresponds to on the device.
     */
    private fun convertSpToPx(scaledPixels: Float): Float {
        return scaledPixels * context.resources.displayMetrics.scaledDensity
    }

    private fun convertPxToSp(scaledPixels: Float): Float {
        return scaledPixels / context.resources.displayMetrics.scaledDensity
    }

    companion object {
        /**
         * Our ellipsis string.
         */
        private const val mEllipsis = "\u2026"
    }
}