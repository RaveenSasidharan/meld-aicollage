package com.panoslice.stickerview.sticker

import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.os.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.annotation.IntDef
import androidx.annotation.IntRange
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
import java.io.File
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.util.*

/**
 * Sticker View
 * @author wupanjie
 */
class StickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private var showIcons = false
    private var showCurrentActionIcon = false
    private var showMoveIcons = false
    private var showBorder = false
    private var mIsExpandOrCollapse = false
    private var mIsAdLoading = false
    private var mIsAspectRatioSelected = false
    var isAutoSnapOn = false
    var isRulerLineOn = false
    var isManualScroll = false
        private set
    private var isManualScale = false
    private var bringToFrontCurrentSticker = false
    var isBgLock = false
    private val isCanvasLock = false

    @IntDef(
        ActionMode.NONE,
        ActionMode.DRAG,
        ActionMode.ZOOM_WITH_TWO_FINGER,
        ActionMode.ICON,
        ActionMode.CLICK
    )
    @Retention(
        RetentionPolicy.SOURCE
    )
    protected annotation class ActionMode {
        companion object {
            var NONE = 0
            var DRAG = 1
            var ZOOM_WITH_TWO_FINGER = 2
            var ICON = 3
            var CLICK = 4
        }
    }

    @IntDef(flag = true, value = [FLIP_HORIZONTALLY, FLIP_VERTICALLY])
    @Retention(RetentionPolicy.SOURCE)
    annotation class Flip

    private val stickers: MutableList<Sticker?> = ArrayList()
    private val icons: MutableList<BitmapStickerIcon> = ArrayList(5)
    private val borderPaint = Paint()
    private val rotationPaint = Paint()
    private val objectRulerPaint = Paint()
    private val blurPaint = Paint()
    private val iconPaint = Paint()
    private val stickerRect = RectF()
    private val sizeMatrix = Matrix()
    private val downMatrix = Matrix()
    private val moveMatrix = Matrix()

    // region storing variables
    private val bitmapPoints = FloatArray(8)
    private val bounds = FloatArray(8)
    private val point = FloatArray(2)
    private val currentCenterPoint = PointF()
    private val tmp = FloatArray(2)
    private var midPoint = PointF()
    private val midPointList: MutableList<PointF> = ArrayList()
    private val snapPointList: MutableList<PointF> = ArrayList()
    private val squarePointList: MutableList<List<PointF>> = ArrayList()

    // endregion
    private val touchSlop: Int
    private var mTileWidth = 0
    private var mTileHeight = 0
    private var mSliceCount = 0
    private var currentIcon: BitmapStickerIcon? = null

    //  public void  longPressVibrator(){
    //    Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
    //// Vibrate for 500 milliseconds
    //    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    //      v.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.CONTENTS_FILE_DESCRIPTOR));
    //    } else {
    //      //deprecated in API 26
    //      v.vibrate(1000);
    //    }
    //  }
    //the first point down position
    var downX = 0f
        private set
    var downY = 0f
        private set
    private var oldDistance = 0f
    private var oldRotation = 0f
    private var ooldDistance = 0f
    private var pastMovedDistance = 0f

    @ActionMode
    private var currentMode = ActionMode.NONE
    var currentSticker: Sticker? = null
        private set
    var isLocked = false
        private set
    var isConstrained = false
        private set
    private var isTouchLocked = false
    private var isSelected = true
    var onStickerOperationListener: OnStickerOperationListener? = null
        private set
    private var lastClickTime: Long = 0
    var minClickDelayTime = DEFAULT_MIN_CLICK_DELAY_TIME
        private set
    var offsetX = 0
    var offsetY = 0
    var blurRadius = 5
    var isVibratingAngle = false
    private var handler: Handler? = null
    private var mLongPressed: Runnable? = null

    init {
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        var a: TypedArray? = null
        try {
            a = context.obtainStyledAttributes(attrs, R.styleable.StickerView)
            showIcons = a.getBoolean(R.styleable.StickerView_showIcons, false)
            showBorder = a.getBoolean(R.styleable.StickerView_showBorder, false)
            bringToFrontCurrentSticker =
                a.getBoolean(R.styleable.StickerView_bringToFrontCurrentSticker, false)
            borderPaint.isAntiAlias = true
            iconPaint.isAntiAlias = true
            iconPaint.color = Color.TRANSPARENT
            borderPaint.color =
                a.getColor(R.styleable.StickerView_borderColor, Color.WHITE)

//            borderPaint.setAlpha(a.getInteger(R.styleable.StickerView_borderAlpha, 128));
            borderPaint.strokeWidth = 4f
            rotationPaint.strokeWidth = 4f
            rotationPaint.isAntiAlias = true
            rotationPaint.color = a.getColor(R.styleable.StickerView_borderColor, Color.GRAY)
            objectRulerPaint.strokeWidth = 2f
            objectRulerPaint.isAntiAlias = true
            objectRulerPaint.color =
                a.getColor(R.styleable.StickerView_borderColor, Color.BLUE)
            blurPaint.isAntiAlias = true
            blurPaint.style = Paint.Style.FILL
            blurPaint.color = Color.GRAY
            //            blurPaint.setColor(Color.parseColor("#ececec"));
            blurPaint.strokeWidth = 8f
            blurPaint.maskFilter = BlurMaskFilter(
                blurRadius.toFloat() /* shadowRadius */,
                BlurMaskFilter.Blur.NORMAL
            )
            configDefaultIcons()
            handler = Handler()
            mLongPressed = Runnable {
                Log.e("touchT", "Long press!")
                //          longPressVibrator();
            }
        } finally {
            a?.recycle()
        }
    }

    fun configDefaultIcons() {
        val rotateIcon = BitmapStickerIcon(
            ContextCompat.getDrawable(
                context,
                R.drawable.rotate
            ),
            BitmapStickerIcon.Companion.MID_BOTTOM_CENTER
        )
        rotateIcon.iconEvent = RotateIconEvent()
        val deleteIcon = BitmapStickerIcon(
            ContextCompat.getDrawable(context, R.drawable.rect_top_right_shadow),
            BitmapStickerIcon.Companion.RIGHT_TOP
        )
        deleteIcon.iconEvent = DeleteIconEvent()
        val zoomTopLeft = BitmapStickerIcon(
            ContextCompat.getDrawable(context, R.drawable.rect_top_left_shadow),
            BitmapStickerIcon.Companion.LEFT_TOP
        )
        zoomTopLeft.iconEvent = ZoomIconEvent()
        val zoomBottomLeft = BitmapStickerIcon(
            ContextCompat.getDrawable(context, R.drawable.rect_bottom_left_shadow),
            BitmapStickerIcon.Companion.LEFT_BOTTOM
        )
        zoomBottomLeft.iconEvent = ZoomIconEvent()
        val zoomBottomRight = BitmapStickerIcon(
            ContextCompat.getDrawable(
                context,
                R.drawable.rect_bottom_right_shadow
            ), BitmapStickerIcon.Companion.RIGHT_BOTOM
        )
        zoomBottomRight.iconEvent = ZoomIconEvent()
        val topMiddleIcon = BitmapStickerIcon(
            ContextCompat.getDrawable(
                context,
                R.drawable.rect_top_middle
            ), BitmapStickerIcon.Companion.MID_TOP
        )
        topMiddleIcon.iconEvent = BoundBoxTopVerticalMoveEvent()
        val leftMiddleIcon = BitmapStickerIcon(
            ContextCompat.getDrawable(
                context,
                R.drawable.rect_left_middle
            ), BitmapStickerIcon.Companion.MID_LEFT
        )
        leftMiddleIcon.iconEvent = BoundBoxLeftHorizontalMoveEvent()
        val rightMiddleIcon = BitmapStickerIcon(
            ContextCompat.getDrawable(
                context,
                R.drawable.rect_right_middle
            ), BitmapStickerIcon.Companion.MID_RIGHT
        )
        rightMiddleIcon.iconEvent = BoundBoxRightHorizontalMoveEvent()
        val bottomMiddleIcon = BitmapStickerIcon(
            ContextCompat.getDrawable(
                context,
                R.drawable.rect_bottom_middle
            ), BitmapStickerIcon.Companion.MID_BOTTOM
        )
        bottomMiddleIcon.iconEvent = BoundBoxBottomVerticalMoveEvent()
        val rightObjectMoveIcon = BitmapStickerIcon(
            ContextCompat.getDrawable(
                context,
                R.drawable.rect_right_holder
            ), BitmapStickerIcon.Companion.MID_RIGHT_CENTER
        )
        rightObjectMoveIcon.iconEvent = ImageDragEvent()
        icons.clear()
        icons.add(rotateIcon)
        icons.add(rightObjectMoveIcon)
        icons.add(deleteIcon)
        icons.add(zoomTopLeft)
        icons.add(zoomBottomLeft)
        icons.add(zoomBottomRight)
        icons.add(topMiddleIcon)
        icons.add(leftMiddleIcon)
        icons.add(rightMiddleIcon)
        icons.add(bottomMiddleIcon)
    }

    fun rotateVibrator() {
        val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        // Vibrate for 500 milliseconds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.CONTENTS_FILE_DESCRIPTOR))
        } else {
            //deprecated in API 26
            v.vibrate(50)
        }
    }

    fun setManualScrollOn(manualScrollOn: Boolean) {
        setManualScrollOn(manualScrollOn, null)
    }

    fun setManualScrollOn(manualScrollOn: Boolean, event: MotionEvent?) {
        isManualScroll = manualScrollOn
        if (currentSticker != null && manualScrollOn) {
            downX = event!!.x
            downY = event.y
            downMatrix.set(currentSticker.getMatrix())
        }
    }

    fun setManualScaleOn(manualScaleOn: Boolean) {
        isManualScale = manualScaleOn
    }

    /**
     * Swaps sticker at layer [[oldPos]] with the one at layer [[newPos]].
     * Does nothing if either of the specified layers doesn't exist.
     */
    fun swapLayers(oldPos: Int, newPos: Int) {
        if (stickers.size > oldPos && stickers.size > newPos) {
            Collections.swap(stickers, oldPos, newPos)
            invalidate()
        }
    }

    /**
     * Sends sticker from layer [[oldPos]] to layer [[newPos]].
     * Does nothing if either of the specified layers doesn't exist.
     */
    fun sendToLayer(oldPos: Int, newPos: Int) {
        if (stickers.size >= oldPos && stickers.size >= newPos) {
            val s = stickers[oldPos]
            stickers.removeAt(oldPos)
            stickers.add(newPos, s)
            invalidate()
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            stickerRect.left = left.toFloat()
            stickerRect.top = top.toFloat()
            stickerRect.right = right.toFloat()
            stickerRect.bottom = bottom.toFloat()
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        drawStickers(canvas)
    }

    fun resetIcons() {
        showBorder = false
        showIcons = false
    }

    fun drawTextOnCanvas(canvas: Canvas, rotationDegree: Float) {
        canvas.drawText(rotationDegree.toString(), 200f, 300f, borderPaint)
        borderPaint.textSize = 50f
    }

    fun findObjectSnapOffset(offset: Float): PointF? {
        val arr = FloatArray(8)
        getStickerPoints(currentSticker, arr)
        val p1 = arr[0]
        val q1 = arr[1]
        val p2 = arr[2]
        val q2 = arr[3]
        val p3 = arr[4]
        val q3 = arr[5]
        val p4 = arr[6]
        val q4 = arr[7]
        val midP = (p1 + p4) / 2
        val midQ = (q1 + q4) / 2
        val arrP = floatArrayOf(p1, midP, p2)
        val arrQ = floatArrayOf(q1, midQ, q3)
        var foundX = false
        var foundY = false
        val snapPoint = PointF()
        for (i in stickers.indices) {
            val sticker = stickers[i]
            if (currentSticker !== sticker) {
                val arrPoints = FloatArray(8)
                getStickerPoints(sticker, arrPoints)
                val x1 = arrPoints[0]
                val y1 = arrPoints[1]
                val x2 = arrPoints[2]
                val y2 = arrPoints[3]
                val x3 = arrPoints[4]
                val y3 = arrPoints[5]
                val x4 = arrPoints[6]
                val y4 = arrPoints[7]
                val midX = (x1 + x4) / 2
                val midY = (y1 + y4) / 2
                if (!foundX) {
                    val arrX = floatArrayOf(x1, midX, x2)
                    for (xpt in arrX) {
                        for (ppt in arrP) {
                            if (Math.abs(xpt - ppt) <= offset) {
                                snapPoint.x = xpt - ppt
                                foundX = true
                                break
                            }
                        }
                        if (foundX) break
                    }
                }
                if (!foundY) {
                    val arrY = floatArrayOf(y1, midY, y3)
                    for (ypt in arrY) {
                        for (qpt in arrQ) {
                            if (Math.abs(ypt - qpt) <= offset) {
                                snapPoint.y = ypt - qpt
                                foundY = true
                                break
                            }
                        }
                        if (foundY) break
                    }
                }
                if (foundX && foundY) {
                    return snapPoint
                }
            }
        }
        for (points in squarePointList) {
            for (sp in points) {
                if (!foundX) {
                    val xpt = sp.x
                    for (ppt in arrP) {
                        if (Math.abs(xpt - ppt) <= offset) {
                            snapPoint.x = xpt - ppt
                            foundX = true
                            break
                        }
                    }
                }
                if (!foundY) {
                    val ypt = sp.y
                    for (qpt in arrQ) {
                        if (Math.abs(ypt - qpt) <= offset) {
                            snapPoint.y = ypt - qpt
                            foundY = true
                            break
                        }
                    }
                }
                if (foundX && foundY) {
                    return snapPoint
                }
            }
        }
        for (sp in snapPointList) {
            if (!foundX) {
                val xpt = sp.x
                for (ppt in arrP) {
                    if (Math.abs(xpt - ppt) <= offset) {
                        snapPoint.x = xpt - ppt
                        foundX = true
                        break
                    }
                }
            }
            if (!foundY) {
                val ypt = sp.y
                for (qpt in arrQ) {
                    if (Math.abs(ypt - qpt) <= offset) {
                        snapPoint.y = ypt - qpt
                        foundY = true
                        break
                    }
                }
            }
            if (foundX && foundY) {
                return snapPoint
            }
        }
        if (foundX || foundY) {
            if (!foundX) {
                snapPoint.x = 0f
            } else if (!foundY) {
                snapPoint.y = 0f
            }
            return snapPoint
        }
        return null
    }

    fun findObjectRulerPoint(offset: Float): ArrayList<MutableSet<Float>> {
        val arr = FloatArray(8)
        getStickerPoints(currentSticker, arr)
        val p1 = arr[0]
        val q1 = arr[1]
        val p2 = arr[2]
        val q2 = arr[3]
        val p3 = arr[4]
        val q3 = arr[5]
        val p4 = arr[6]
        val q4 = arr[7]
        val midP = (p1 + p4) / 2
        val midQ = (q1 + q4) / 2
        val arrP = floatArrayOf(p1, midP, p2)
        val arrQ = floatArrayOf(q1, midQ, q3)
        val snapPoints = ArrayList<MutableSet<Float>>()
        snapPoints.add(HashSet()) //0 index for - x coordinate
        snapPoints.add(HashSet()) //1 index for - y coordinate
        for (i in stickers.indices) {
            val sticker = stickers[i]
            if (currentSticker !== sticker) {
                val arrPoints = FloatArray(8)
                getStickerPoints(sticker, arrPoints)
                val x1 = arrPoints[0]
                val y1 = arrPoints[1]
                val x2 = arrPoints[2]
                val y2 = arrPoints[3]
                val x3 = arrPoints[4]
                val y3 = arrPoints[5]
                val x4 = arrPoints[6]
                val y4 = arrPoints[7]
                val midX = (x1 + x4) / 2
                val midY = (y1 + y4) / 2
                val arrX = floatArrayOf(x1, midX, x2)
                for (xpt in arrX) {
                    for (ppt in arrP) {
                        if (Math.abs(xpt - ppt) <= offset) {
                            snapPoints[0].add(xpt)
                        }
                    }
                }
                val arrY = floatArrayOf(y1, midY, y3)
                for (ypt in arrY) {
                    for (qpt in arrQ) {
                        if (Math.abs(ypt - qpt) <= offset) {
                            snapPoints[1].add(ypt)
                        }
                    }
                }
            }
        }
        var squareRuler = false
        for (points in squarePointList) {
            for (snapPoint in points) {
                val xpt = snapPoint.x
                val ypt = snapPoint.y
                for (ppt in arrP) {
                    if (Math.abs(xpt - ppt) <= offset) {
                        squareRuler = true
                        break
                    }
                }
                if (!squareRuler) {
                    for (qpt in arrQ) {
                        if (Math.abs(ypt - qpt) <= offset) {
                            if (!(p2 < points[0].x || points[1].x < p1)) {
                                squareRuler = true
                                break
                            }
                        }
                    }
                }
                if (squareRuler) break
            }
            if (squareRuler) {
                snapPoints.add(HashSet()) //2 index for - x coordinate square ruler
                snapPoints.add(HashSet()) //3 index for - y coordinate square ruler
                snapPoints[2].add(points[0].x)
                snapPoints[2].add(points[1].x)
                snapPoints[3].add(points[0].y)
                snapPoints[3].add(points[1].y)
                break
            }
        }
        for (snapPoint in snapPointList) {
            val xpt = snapPoint.x
            val ypt = snapPoint.y
            for (ppt in arrP) {
                if (Math.abs(xpt - ppt) <= offset) {
                    snapPoints[0].add(xpt)
                }
            }
            for (qpt in arrQ) {
                if (Math.abs(ypt - qpt) <= offset) {
                    snapPoints[1].add(ypt)
                }
            }
        }
        return snapPoints
    }

    fun findObjectSnapOffsetForResize(offset: Float): PointF? {
        val arr = FloatArray(8)
        getStickerPoints(currentSticker, arr)
        var minP = Float.MAX_VALUE
        var minQ = Float.MAX_VALUE
        var maxP = Float.MIN_VALUE
        var maxQ = Float.MIN_VALUE
        for (i in 0..3) {
            val temp = arr[i * 2]
            if (temp < minP) {
                minP = temp
            } else if (temp > maxP) {
                maxP = temp
            }
        }
        for (i in 0..3) {
            val temp = arr[i * 2 + 1]
            if (temp < minQ) {
                minQ = temp
            } else if (temp > maxQ) {
                maxQ = temp
            }
        }
        val arrP = floatArrayOf(minP, maxP)
        val arrQ = floatArrayOf(minQ, maxQ)
        var foundX = false
        var foundY = false
        val snapPoint = PointF()
        for (i in stickers.indices) {
            val sticker = stickers[i]
            if (currentSticker !== sticker) {
                val arrPoints = FloatArray(8)
                getStickerPoints(sticker, arrPoints)
                var minX = Float.MAX_VALUE
                var minY = Float.MAX_VALUE
                var maxX = Float.MIN_VALUE
                var maxY = Float.MIN_VALUE
                for (j in 0..3) {
                    val tempX = arrPoints[j * 2]
                    if (tempX < minX) {
                        minX = tempX
                    } else if (tempX > maxX) {
                        maxX = tempX
                    }
                }
                for (k in 0..3) {
                    val tempY = arrPoints[k * 2 + 1]
                    if (tempY < minY) {
                        minY = tempY
                    } else if (tempY > maxY) {
                        maxY = tempY
                    }
                }
                if (!foundX) {
                    val arrX = floatArrayOf(minX, maxX)
                    for (xpt in arrX) {
                        for (ppt in arrP) {
                            if (Math.abs(xpt - ppt) <= offset) {
                                snapPoint.x = xpt - ppt
                                foundX = true
                                break
                            }
                        }
                        if (foundX) break
                    }
                }
                if (!foundY) {
                    val arrY = floatArrayOf(minY, maxY)
                    for (ypt in arrY) {
                        for (qpt in arrQ) {
                            if (Math.abs(ypt - qpt) <= offset) {
                                snapPoint.y = ypt - qpt
                                foundY = true
                                break
                            }
                        }
                        if (foundY) break
                    }
                }
                if (foundX && foundY) {
                    return snapPoint
                }
            }
        }
        for (points in squarePointList) {
            for (sp in points) {
                if (!foundX) {
                    val xpt = sp.x
                    for (ppt in arrP) {
                        if (Math.abs(xpt - ppt) <= offset) {
                            snapPoint.x = xpt - ppt
                            foundX = true
                            break
                        }
                    }
                }
                if (!foundY) {
                    val ypt = sp.y
                    for (qpt in arrQ) {
                        if (Math.abs(ypt - qpt) <= offset) {
                            snapPoint.y = ypt - qpt
                            foundY = true
                            break
                        }
                    }
                }
                if (foundX && foundY) {
                    return snapPoint
                }
            }
        }
        for (sp in snapPointList) {
            if (!foundX) {
                val xpt = sp.x
                for (ppt in arrP) {
                    if (Math.abs(xpt - ppt) <= offset) {
                        snapPoint.x = xpt - ppt
                        foundX = true
                        break
                    }
                }
            }
            if (!foundY) {
                val ypt = sp.y
                for (qpt in arrQ) {
                    if (Math.abs(ypt - qpt) <= offset) {
                        snapPoint.y = ypt - qpt
                        foundY = true
                        break
                    }
                }
            }
            if (foundX && foundY) {
                return snapPoint
            }
        }
        if (foundX || foundY) {
            if (!foundX) {
                snapPoint.x = 0f
            } else if (!foundY) {
                snapPoint.y = 0f
            }
            return snapPoint
        }
        return null
    }

    fun findObjectRulerPointForResize(offset: Float): ArrayList<MutableSet<Float>> {
        val arr = FloatArray(8)
        getStickerPoints(currentSticker, arr)
        var minP = Float.MAX_VALUE
        var minQ = Float.MAX_VALUE
        var maxP = Float.MIN_VALUE
        var maxQ = Float.MIN_VALUE
        for (i in 0..3) {
            val temp = arr[i * 2]
            if (temp < minP) {
                minP = temp
            } else if (temp > maxP) {
                maxP = temp
            }
        }
        for (i in 0..3) {
            val temp = arr[i * 2 + 1]
            if (temp < minQ) {
                minQ = temp
            } else if (temp > maxQ) {
                maxQ = temp
            }
        }
        val arrP = floatArrayOf(minP, maxP)
        val arrQ = floatArrayOf(minQ, maxQ)
        val snapPoints = ArrayList<MutableSet<Float>>()
        snapPoints.add(HashSet()) //0 index for - x coordinate
        snapPoints.add(HashSet()) //1 index for - y coordinate
        for (i in stickers.indices) {
            val sticker = stickers[i]
            if (currentSticker !== sticker) {
                val arrPoints = FloatArray(8)
                getStickerPoints(sticker, arrPoints)
                var minX = Float.MAX_VALUE
                var minY = Float.MAX_VALUE
                var maxX = Float.MIN_VALUE
                var maxY = Float.MIN_VALUE
                for (j in 0..3) {
                    val tempX = arrPoints[j * 2]
                    if (tempX < minX) {
                        minX = tempX
                    } else if (tempX > maxX) {
                        maxX = tempX
                    }
                }
                for (k in 0..3) {
                    val tempY = arrPoints[k * 2 + 1]
                    if (tempY < minY) {
                        minY = tempY
                    } else if (tempY > maxY) {
                        maxY = tempY
                    }
                }
                val arrX = floatArrayOf(minX, maxX)
                for (xpt in arrX) {
                    for (ppt in arrP) {
                        if (Math.abs(xpt - ppt) <= offset) {
                            snapPoints[0].add(xpt)
                        }
                    }
                }
                val arrY = floatArrayOf(minY, maxY)
                for (ypt in arrY) {
                    for (qpt in arrQ) {
                        if (Math.abs(ypt - qpt) <= offset) {
                            snapPoints[1].add(ypt)
                        }
                    }
                }
            }
        }
        var squareRuler = false
        for (points in squarePointList) {
            for (snapPoint in points) {
                val xpt = snapPoint.x
                val ypt = snapPoint.y
                for (ppt in arrP) {
                    if (Math.abs(xpt - ppt) <= offset) {
                        squareRuler = true
                        break
                    }
                }
                if (!squareRuler) {
                    for (qpt in arrQ) {
                        if (Math.abs(ypt - qpt) <= offset) {
                            if (!(maxP < points[0].x || points[1].x < minP)) {
                                squareRuler = true
                                break
                            }
                        }
                    }
                }
                if (squareRuler) break
            }
            if (squareRuler) {
                snapPoints.add(HashSet()) //2 index for - x coordinate square ruler
                snapPoints.add(HashSet()) //3 index for - y coordinate square ruler
                snapPoints[2].add(points[0].x)
                snapPoints[2].add(points[1].x)
                snapPoints[3].add(points[0].y)
                snapPoints[3].add(points[1].y)
                break
            }
        }
        for (snapPoint in snapPointList) {
            val xpt = snapPoint.x
            val ypt = snapPoint.y
            for (ppt in arrP) {
                if (Math.abs(xpt - ppt) <= offset) {
                    snapPoints[0].add(xpt)
                }
            }
            for (qpt in arrQ) {
                if (Math.abs(ypt - qpt) <= offset) {
                    snapPoints[1].add(ypt)
                }
            }
        }
        return snapPoints
    }

    protected fun drawStickers(canvas: Canvas) {
        for (i in stickers.indices) {
            val sticker = stickers[i]
            sticker?.draw(canvas)
        }
        if (currentSticker != null && !isLocked && (showBorder || showIcons)) {
            getStickerPoints(currentSticker, bitmapPoints)
            val x1 = bitmapPoints[0]
            val y1 = bitmapPoints[1]
            val x2 = bitmapPoints[2]
            val y2 = bitmapPoints[3]
            val x3 = bitmapPoints[4]
            val y3 = bitmapPoints[5]
            val x4 = bitmapPoints[6]
            val y4 = bitmapPoints[7]
            val xmt = (x1 + x2) / 2
            val ymt = (y1 + y2) / 2
            val xmb = (x3 + x4) / 2
            val ymb = (y3 + y4) / 2
            val xmid = (xmt + xmb) / 2
            val ymid = (ymt + ymb) / 2
            val xmb1 = (x2 + x4) / 2
            val ymb1 = (y2 + y4) / 2
            val point = rotate_point(xmb, ymb, -90f, PointF(x3, y3))
            val xr = point.x
            val yr = point.y
            val pointmove = rotate_point(xmb1, ymb1, -90f, PointF(x4, y4))
            val xr1 = pointmove.x
            val yr1 = pointmove.y
            val xml = (x1 + x3) / 2
            val yml = (y1 + y3) / 2
            val xmr = (x2 + x4) / 2
            val ymr = (y2 + y4) / 2
            val xe = x1 - 100
            val ye = y1 - 100
            val rotationDegree = calculateRotation(x3, y3, x1, y1)
            val layoutParams = layoutParams
            val width1 = layoutParams.width
            val height1 = layoutParams.height
            val yMidSticker = (y1 + y4) / 2f
            val yMidCanvas = height1 / 2f
            val xMidSticker = (x1 + x4) / 2f
            val xMidCanvas = width1 / 2f
            if (showBorder) {
//                canvas.drawRect(20 + offsetX, 20 + offsetY, 100 + offsetX, 100 + offsetY, blurPaint);
                canvas.drawLine(x1 + offsetX, y1 + offsetY, x2 + offsetX, y2 + offsetY, blurPaint)
                canvas.drawLine(x1 + offsetX, y1 + offsetY, x3 + offsetX, y3 + offsetY, blurPaint)
                canvas.drawLine(x2 + offsetX, y2 + offsetY, x4 + offsetX, y4 + offsetY, blurPaint)
                canvas.drawLine(x4 + offsetX, y4 + offsetY, x3 + offsetX, y3 + offsetY, blurPaint)
                canvas.drawLine(x1, y1, x2, y2, borderPaint)
                canvas.drawLine(x1, y1, x3, y3, borderPaint)
                canvas.drawLine(x2, y2, x4, y4, borderPaint)
                canvas.drawLine(x4, y4, x3, y3, borderPaint)


//                canvas.drawLine(xmt,y2-100,xmt,ymt,new Paint());
//                drawTextOnCanvas(canvas,rotationDegree);
            } else if (currentIcon != null && currentIcon.getPosition() == BitmapStickerIcon.Companion.MID_BOTTOM_CENTER) {
//                    && handlingSticker instanceof TextSticker) {
                val rot = rotationDegree.toInt() //Math.round(rotation);
                if (rot % 90 == 0) {
                    if (isVibratingAngle == false) {
                        rotateVibrator()
                    }
                    canvas.drawLine(xmt, ymt, xmb, ymb, rotationPaint)
                    canvas.drawLine(xml, yml, xmr, ymr, rotationPaint)
                    isVibratingAngle = true
                } else if (rot % 45 == 0) {
                    if (isVibratingAngle == false) {
                        rotateVibrator()
                    }
                    val rotateWidth = x2 - x1
                    val rotateHeight = y4 - y2
                    var rotateDistance =
                        if (rotateWidth < rotateHeight) rotateWidth else rotateHeight
                    rotateDistance = rotateDistance / 2
                    canvas.drawLine(
                        midPoint.x - rotateDistance, midPoint.y - rotateDistance,
                        midPoint.x + rotateDistance, midPoint.y + rotateDistance, rotationPaint
                    )
                    canvas.drawLine(
                        midPoint.x - rotateDistance, midPoint.y + rotateDistance,
                        midPoint.x + rotateDistance, midPoint.y - rotateDistance, rotationPaint
                    )
                    isVibratingAngle = true
                } else {
                    isVibratingAngle = false
                }
            }
            if ((currentMode == ActionMode.DRAG || isManualScroll) && isRulerLineOn) {
                val arrSnapData = findObjectRulerPoint(1f)
                for (xpt in arrSnapData[0]) {
                    canvas.drawLine(xpt, 0f, xpt, height1.toFloat(), objectRulerPaint)
                }
                for (ypt in arrSnapData[1]) {
                    canvas.drawLine(0f, ypt, width1.toFloat(), ypt, objectRulerPaint)
                }
                if (arrSnapData.size == 4) {
                    val squarPtX: MutableList<Float> = ArrayList()
                    squarPtX.addAll(arrSnapData[2])
                    val squarPtY: MutableList<Float> = ArrayList()
                    squarPtY.addAll(arrSnapData[3])
                    val p1 = squarPtX[0]
                    val p2 = squarPtX[1]
                    val q1 = squarPtY[0]
                    val q2 = squarPtY[1]
                    canvas.drawLine(p1, q1, p2, q1, objectRulerPaint)
                    canvas.drawLine(p2, q1, p2, q2, objectRulerPaint)
                    canvas.drawLine(p1, q2, p2, q2, objectRulerPaint)
                    canvas.drawLine(p1, q1, p1, q2, objectRulerPaint)
                }
            } else if (currentIcon != null && (currentIcon.getPosition() == BitmapStickerIcon.Companion.LEFT_TOP || currentIcon.getPosition() == BitmapStickerIcon.Companion.LEFT_BOTTOM || currentIcon.getPosition() == BitmapStickerIcon.Companion.RIGHT_BOTOM || currentIcon.getPosition() == BitmapStickerIcon.Companion.MID_TOP || currentIcon.getPosition() == BitmapStickerIcon.Companion.MID_BOTTOM || currentIcon.getPosition() == BitmapStickerIcon.Companion.MID_LEFT || currentIcon.getPosition() == BitmapStickerIcon.Companion.MID_RIGHT) && isRulerLineOn) {
//        Log.d(TAG,"ZoomIconEvent for Corner Button");
                val arrSnapData = findObjectRulerPointForResize(1f)
                Log.d(TAG, "ZoomIconEvent for Corner Button len = " + arrSnapData.size)
                for (xpt in arrSnapData[0]) {
                    canvas.drawLine(xpt, 0f, xpt, height1.toFloat(), objectRulerPaint)
                }
                for (ypt in arrSnapData[1]) {
                    canvas.drawLine(0f, ypt, width1.toFloat(), ypt, objectRulerPaint)
                }
            }
            //draw icons
            showMoveIcons = false
            if (showIcons) {
                val rotation = calculateRotation(x4, y4, x3, y3)
                val width = calculateDistance(x1, y1, x2, y2)
                val height = calculateDistance(x2, y2, x4, y4)
                //        showMoveIcons=width < 200 || height < 200;
                showMoveIcons = height * width < 200 * 200
                for (i in icons.indices) {
                    val icon = icons[i]
                    var displayIcon = true
                    if (showCurrentActionIcon) {
                        if (currentIcon !== icon) {
                            displayIcon = false
                        }
                    }
                    when (icon.position) {
                        BitmapStickerIcon.Companion.LEFT_TOP -> if (showMoveIcons == true) {
//                            Log.d("rot___", "drawStickers2: " + rotation);
                            displayIcon = false
                        } else {
                            configIconMatrix(icon, x1, y1, rotation)
                        }
                        BitmapStickerIcon.Companion.RIGHT_TOP -> configIconMatrix(
                            icon,
                            x2,
                            y2,
                            rotation
                        )
                        BitmapStickerIcon.Companion.LEFT_BOTTOM -> configIconMatrix(
                            icon,
                            x3,
                            y3,
                            rotation
                        )
                        BitmapStickerIcon.Companion.RIGHT_BOTOM -> if (showMoveIcons == true) {
                            displayIcon = false
                        } else {
                            configIconMatrix(icon, x4, y4, rotation)
                        }
                        BitmapStickerIcon.Companion.MID_TOP -> if (showMoveIcons == true) {
                            displayIcon = false
                        } else {
                            configIconMatrix(icon, xmt, ymt, rotation)
                        }
                        BitmapStickerIcon.Companion.MID_BOTTOM -> if (showMoveIcons == true) {
                            displayIcon = false
                        } else {
                            Log.d("rot___", "drawStickers: $rotation")
                            configIconMatrix(icon, xmb, ymb, rotation)
                        }
                        BitmapStickerIcon.Companion.MID_LEFT -> if (showMoveIcons == true) {
                            displayIcon = false
                        } else {
                            Log.d("rot___", "drawStickers: $rotation")
                            configIconMatrix(icon, xml, yml, rotation)
                        }
                        BitmapStickerIcon.Companion.MID_RIGHT -> if (showMoveIcons == true) {
                            displayIcon = false
                        } else {
                            Log.d("rot___", "drawStickers: $rotation")
                            configIconMatrix(icon, xmr, ymr, rotation)
                        }
                        BitmapStickerIcon.Companion.EXT_LEFT_TOP -> if (showMoveIcons == true) {
                            displayIcon = false
                        } else {
                            configIconMatrixT(icon, x1, y1, rotation)
                        }
                        BitmapStickerIcon.Companion.MID_BOTTOM_CENTER -> {
                            Log.d("rot___", "drawStickers: $rotation")
                            configIconMatrix(icon, xr, yr, rotation)
                        }
                        BitmapStickerIcon.Companion.MID_RIGHT_CENTER -> if (showMoveIcons == true) {
                            Log.d("rot___", "drawStickers: $rotation")
                            configIconMatrix(icon, xr1, yr1, rotation)
                        } else {
                            displayIcon = false
                        }
                    }
                    if (displayIcon) {
                        icon.draw(canvas, iconPaint)
                    }
                }
            }
        }
    }

    fun rotate_point(x5: Float, y5: Float, angle: Float, p: PointF): PointF {
        val sin = Math.sin(Math.toRadians(angle.toDouble())).toFloat()
        val cos = Math.cos(Math.toRadians(angle.toDouble())).toFloat()
        p.x -= x5
        p.y -= y5
        val xnew = p.x * cos - p.y * sin
        val ynew = p.x * sin + p.y * cos
        // translate point back:
        p.x = (xnew + x5).toInt().toFloat()
        p.y = (ynew + y5).toInt().toFloat()
        //      calculate point at x distance from bottom middle point vertically for rotate icon to set it position
        val d = calculateDistance(x5, y5, p.x, p.y)
        val t: Float
        t = 70 / d
        val xt = (1 - t) * x5 + t * p.x
        val yt = (1 - t) * y5 + t * p.y
        p.x = xt
        p.y = yt
        return p
    }

    fun stickerImageDrag(event: MotionEvent): Boolean {
        if (currentSticker != null) {
            currentMode = ActionMode.DRAG
            moveMatrix.set(downMatrix)
            moveMatrix.postTranslate(event.x - downX, event.y - downY)
            currentSticker!!.setMatrix(moveMatrix)
            if (isConstrained) {
                constrainSticker(currentSticker!!)
            }
            if (isAutoSnapOn) {
                val snapPoint = returnSnapPoint(event)
                if (snapPoint != null) {
                    if (currentSticker != null) {
                        currentSticker.getMatrix().postTranslate(snapPoint.x, snapPoint.y)
                    }
                    //        moveToSnapPoint(snapPoint);
                } else {
                    val snapOffset = findObjectSnapOffset(OFFSET_RULER)
                    if (currentSticker != null && snapOffset != null) {
                        currentSticker.getMatrix().postTranslate(snapOffset.x, snapOffset.y)
                    }
                }
            }
            return true
        }
        return false
    }

    fun stickerImageDragManual(event: MotionEvent, dx: Float, dy: Float, scale: Float): Boolean {
        if (currentSticker != null) {
            if (isManualScroll && !isManualScale) {
                moveMatrix.set(downMatrix)
                moveMatrix.postTranslate((event.x - downX) * scale, (event.y - downY) * scale)
                currentSticker!!.setMatrix(moveMatrix)
                if (isConstrained) {
                    constrainSticker(currentSticker!!)
                }
                invalidate()
                if (isAutoSnapOn) {
                    val snapPoint = returnSnapPoint(event)
                    if (snapPoint != null) {
                        if (currentSticker != null) {
                            currentSticker.getMatrix().postTranslate(snapPoint.x, snapPoint.y)
                        }
                        //        moveToSnapPoint(snapPoint);
                    } else {
                        val snapOffset = findObjectSnapOffset(OFFSET_RULER)
                        if (currentSticker != null && snapOffset != null) {
                            currentSticker.getMatrix().postTranslate(snapOffset.x, snapOffset.y)
                        }
                    }
                }
            }
            return true
        }
        return false
    }

    fun stickerImageZoomManual(scaleFactor: Float): Boolean {
        if (currentSticker != null) {
//      float newDistance = calculateDistance(event);
            if (currentSticker is TextSticker) {
                (currentSticker as TextSticker).scaleText(scaleFactor)
                (currentSticker as TextSticker).onZoomFinished(scaleFactor)
                //        invalidate();
            } else {
//        float newRotation = calculateRotation(event);
                val pt = currentSticker!!.mappedCenterPoint
                val matrix = currentSticker.getMatrix()
                //        moveMatrix.set(downMatrix);
                matrix.postScale(
                    scaleFactor, scaleFactor, pt.x, pt.y
                )
                //         moveMatrix.postRotate(newRotation - oldRotation, midPoint.x, midPoint.y);
                currentSticker!!.setMatrix(matrix)
            }
            invalidate()
            if (onStickerOperationListener != null) {
                onStickerOperationListener!!.onStickerZoom(currentSticker!!)
            }
            return true
        }
        return false
    }

    protected fun configIconMatrix(
        icon: BitmapStickerIcon, x: Float, y: Float,
        rotation: Float
    ) {
        icon.x = x
        icon.y = y
        icon.matrix.reset()
        icon.matrix.postRotate(rotation, (icon.width / 2).toFloat(), (icon.height / 2).toFloat())
        icon.matrix.postTranslate(x - icon.width / 2, y - icon.height / 2)
    }

    protected fun configIconMatrixT(
        icon: BitmapStickerIcon, x: Float, y: Float,
        rotation: Float
    ) {
        icon.x = x - 100
        icon.y = y - 100
        icon.matrix.reset()
        icon.matrix.postRotate(
            rotation,
            (icon.width / 2 + 100).toFloat(),
            (icon.height / 2 + 100).toFloat()
        )
        icon.matrix.postTranslate(x - icon.width / 2 - 100, y - icon.height / 2 - 100)
    }

    protected fun configIconMatrixTT(icon: BitmapStickerIcon, x: Float, y: Float, rotation: Float) {
        icon.x = x
        icon.y = y
        //        icon.setZ(z);
        icon.matrix.reset()
        icon.matrix.postRotate(rotation, (icon.width / 2).toFloat(), (icon.height / 20).toFloat())
        icon.matrix.postTranslate(x - icon.width / 2, 50 + y - icon.height / 20)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        Log.e("touchT", "onInterceptTouchEvent")
        mIsExpandOrCollapse = false
        mIsAdLoading = false
        if (isLocked || isBgLock) return super.onInterceptTouchEvent(ev)
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                val isTouched = findCurrentIconTouched() != null || currentSticker != null
                if (!isTouched && findHandlingSticker() != null) {
                    isTouchLocked = false
                }
                //        if (!isTouched)
//        {
//          isTouchLocked = true;
//          onStickerOperationListener.onStickerTouchedOutside();
//          resetIcons();
//        }
//        else
//          isTouchLocked = false;
                return false //isTouched;
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        Log.e("touchT", "onTouchEvent")
        mIsExpandOrCollapse = false
        mIsAdLoading = false
        mIsAspectRatioSelected = false
        parent.requestDisallowInterceptTouchEvent(true)
        if (isLocked || isTouchLocked || isBgLock) {
            Log.e(
                "touchT",
                "if locked" + isLocked + "isTouchLocked" + isTouchLocked + "isBgLock" + isBgLock
            )
            return super.onTouchEvent(event)
        }
        val action = MotionEventCompat.getActionMasked(event)
        when (action) {
            MotionEvent.ACTION_DOWN -> if (!onTouchDown(event)) {
                return false
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                oldDistance = calculateDistance(event)
                oldRotation = calculateRotation(event)
                midPoint = calculateMidPoint(event)
                Log.e("touchT", "onTouchEvent midPoint x = " + midPoint.x + ", y = " + midPoint.y)
                if (currentSticker != null //                && isInStickerArea(handlingSticker, event.getX(1), event.getY(1))
                    && findCurrentIconTouched() == null
                ) {
//                    Log.d(TAG, "onTouchEvent: Current Stiicker Rotate");
                    currentMode = ActionMode.ZOOM_WITH_TWO_FINGER
                    midPoint = currentSticker!!.mappedCenterPoint
                    Log.e(
                        "touchT",
                        "onTouchEvent midPoint 2  x = " + midPoint.x + ", y = " + midPoint.y
                    )
                    onZoomTouchDown(event)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (Math.abs(event.x - downX) >= touchSlop
                    || Math.abs(event.y - downY) >= touchSlop
                ) {
                    handler!!.removeCallbacks(mLongPressed!!)
                }
                if (isSelected) {
                    handleCurrentMode(event)
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> onTouchUp(event)
            MotionEvent.ACTION_POINTER_UP -> {
                if (currentMode == ActionMode.ZOOM_WITH_TWO_FINGER && currentSticker != null) {
                    onZoomTouchUp(event)
                    if (onStickerOperationListener != null) {
                        onStickerOperationListener!!.onStickerZoomFinished(currentSticker!!)
                    }
                }
                currentMode = ActionMode.NONE
            }
        }
        return true
    }

    fun setmIsAspectRatioSelected(mIsAspectRatioSelected: Boolean) {
        this.mIsAspectRatioSelected = mIsAspectRatioSelected
    }

    fun lockImages(isLocked: Boolean) {
        this.isLocked = isLocked
    }

    fun setIsSelected(isSelected: Boolean) {
        this.isSelected = isSelected
    }

    fun ismIsAspectRatioSelected(): Boolean {
        return mIsAspectRatioSelected
    }

    fun setmIsAdLoading(mIsAdLoading: Boolean) {
        this.mIsAdLoading = mIsAdLoading
    }

    /**
     * @param event MotionEvent received from [)][.onTouchEvent]
     */
    protected fun onTouchDown(event: MotionEvent): Boolean {
        Log.e("touchT", "onTouchEvent onTouchDown")
        currentMode = ActionMode.DRAG
        downX = event.x
        downY = event.y
        midPoint = calculateMidPoint()
        oldDistance = calculateDistance(midPoint.x, midPoint.y, downX, downY)
        oldRotation = calculateRotation(midPoint.x, midPoint.y, downX, downY)
        ooldDistance = Math.sqrt(
            ((midPoint.x - event.x) * (midPoint.x - event.x) +
                    (midPoint.y - event.y) * (midPoint.y - event.y)).toDouble()
        ).toFloat()
        pastMovedDistance = -1f
        currentIcon = findCurrentIconTouched()
        if (currentIcon != null) {
            currentMode = ActionMode.ICON
            currentIcon!!.onActionDown(this, event)
            showBorder = false
            showCurrentActionIcon = true
        } else {
//      handlingSticker = findHandlingSticker();
        }
        if (findHandlingSticker() != null) {
            handler!!.postDelayed(mLongPressed!!, ViewConfiguration.getLongPressTimeout().toLong())
        }
        if (currentSticker != null) {
            downMatrix.set(currentSticker.getMatrix())
            if (bringToFrontCurrentSticker) {
                stickers.remove(currentSticker)
                stickers.add(currentSticker)
            }
            if (onStickerOperationListener != null) {
                onStickerOperationListener!!.onStickerTouchedDown(currentSticker!!)
            }
        }
        if (currentIcon == null && currentSticker == null) {
            return false
        }
        invalidate()
        return true
    }

    protected fun onTouchUp(event: MotionEvent) {
        Log.e("touchT", "onTouchEvent onTouchUp")
        val currentTime = SystemClock.uptimeMillis()
        handler!!.removeCallbacks(mLongPressed!!)
        if (currentMode == ActionMode.ICON && currentIcon != null && currentSticker != null && isSelected) {
            currentIcon!!.onActionUp(this, event)
        }
        if (currentMode == ActionMode.DRAG && Math.abs(event.x - downX) < touchSlop && Math.abs(
                event.y - downY
            ) < touchSlop
        ) {
            currentMode = ActionMode.CLICK
            val isTouched = findCurrentIconTouched() != null || findHandlingSticker() != null
            if (!isTouched) {
                isTouchLocked = true
                currentSticker = null
                onStickerOperationListener!!.onStickerTouchedOutside()
                resetIcons()
            } else {
                isTouchLocked = false
                if (currentIcon == null) {
                    currentSticker = findHandlingSticker()
                } else {
                    currentSticker = null
                }
            }
            if (currentSticker != null) {
                if (onStickerOperationListener != null) {
                    isSelected = true
                    onStickerOperationListener!!.onStickerClicked(currentSticker!!)
                }
                if (currentTime - lastClickTime < minClickDelayTime) {
                    if (onStickerOperationListener != null) {
//          isSelected = false;
                        onStickerOperationListener!!.onStickerDoubleTapped(currentSticker!!)
                        resetIcons()
                    }
                }
            }
        }
        if (currentMode == ActionMode.DRAG && currentSticker != null) {
            if (onStickerOperationListener != null) {
                onStickerOperationListener!!.onStickerDragFinished(currentSticker!!)
            }
        }
        showBorder = true
        showIcons = true
        showCurrentActionIcon = false
        currentMode = ActionMode.NONE
        lastClickTime = currentTime
        invalidate()
    }

    protected fun handleCurrentMode(event: MotionEvent) {
        Log.e("touchT", "onTouchEvent handleCurrentMode currentMode = $currentMode")
        when (currentMode) {
            ActionMode.NONE, ActionMode.CLICK -> {}
            ActionMode.DRAG -> stickerImageDrag(event)
            ActionMode.ZOOM_WITH_TWO_FINGER -> if (currentSticker != null) {
                val newDistance = calculateDistance(event)
                if (currentSticker is TextSticker) {
                    (currentSticker as TextSticker).scaleText(newDistance / oldDistance)
                    invalidate()
                } else {
                    val newRotation = calculateRotation(event)
                    val pt = currentSticker!!.centerPoint
                    moveMatrix.set(downMatrix)
                    moveMatrix.postScale(
                        newDistance / oldDistance, newDistance / oldDistance, midPoint.x, midPoint.y
                    )
                    // moveMatrix.postRotate(newRotation - oldRotation, midPoint.x, midPoint.y);
                    currentSticker!!.setMatrix(moveMatrix)
                }
                if (onStickerOperationListener != null) {
                    onStickerOperationListener!!.onStickerZoom(currentSticker!!)
                }
            }
            ActionMode.ICON -> if (currentSticker != null && currentIcon != null) {
                currentIcon!!.onActionMove(this, event)
                //                    pastMovedDistance = calculateDistance(midPoint.x, midPoint.y, event.getX(), event.getY());
            }
        }
    }

    protected fun onZoomTouchDown(event: MotionEvent) {}
    protected fun onZoomTouchUp(event: MotionEvent) {
        if (currentSticker is TextSticker) {
            val newDistance = calculateDistance(event)
            (currentSticker as TextSticker).onZoomFinished(newDistance / oldDistance)
        }
    }

    private fun returnSnapPoint(event: MotionEvent): PointF? {
//        Log.e(TAG, "returnSnapPoint");
        val currentMidPoint: PointF
        currentMidPoint = if (currentSticker != null) {
            currentSticker!!.mappedCenterPoint
        } else {
            PointF(event.x, event.y)
        }
        for (snapPoint in midPointList) {
            if (Math.abs(currentMidPoint.x - snapPoint.x) <= 50 && Math.abs(currentMidPoint.y - snapPoint.y) <= 50) {
                Log.e(TAG, "snapPint done")
                onStickerOperationListener!!.onSnapPointDone(snapPoint)
                return PointF(
                    snapPoint.x - currentMidPoint.x,
                    snapPoint.y - currentMidPoint.y
                ) //snapPoint;
            } else onStickerOperationListener!!.dismissRural()
        }
        return null
    }

    private fun moveToSnapPoint(snapPoint: PointF) {
        var moveX = 0f
        var moveY = 0f
        val matrixValues = FloatArray(9)
        currentSticker.getMatrix().getValues(matrixValues)
        moveX = snapPoint.x - matrixValues[Matrix.MSCALE_X] * currentSticker.getWidth()
            .toFloat() / 2 - matrixValues[Matrix.MTRANS_X]
        moveY = snapPoint.y - matrixValues[Matrix.MSCALE_Y] * currentSticker.getHeight()
            .toFloat() / 2 - matrixValues[Matrix.MTRANS_Y]
        if (currentSticker != null) currentSticker.getMatrix().postTranslate(moveX, moveY)
    }

    fun zoomCurrentSticker(event: MotionEvent) {
//    if(handlingSticker instanceof TextSticker) {
//      float newDistance =  calculateDistance(midPoint.x, midPoint.y, event.getX(), event.getY());
//      ((TextSticker)handlingSticker).onFontSizeValue(newDistance/oldDistance);
//    }
        zoomCurrentSticker(currentSticker, event)
    }

    fun zoomCurrentSticker(sticker: Sticker?, event: MotionEvent) {
        if (sticker != null) {
            var newDistance = calculateDistance(midPoint.x, midPoint.y, event.x, event.y)
            val newRotation = calculateRotation(midPoint.x, midPoint.y, event.x, event.y)
            moveMatrix.set(downMatrix)
            moveMatrix.postScale(
                newDistance / oldDistance, newDistance / oldDistance, midPoint.x,
                midPoint.y
            )
            currentSticker!!.setMatrix(moveMatrix)
            val snapPointRuler = findObjectSnapOffsetForResize(OFFSET_RULER)
            if (snapPointRuler != null) {
                Log.d(TAG, "zoom snap offset x = " + snapPointRuler.x + ", y = " + snapPointRuler.y)
                newDistance = calculateDistance(
                    midPoint.x,
                    midPoint.y,
                    event.x + snapPointRuler.x,
                    event.y + snapPointRuler.y
                )
                moveMatrix.set(downMatrix)
                moveMatrix.postScale(
                    newDistance / oldDistance, newDistance / oldDistance, midPoint.x,
                    midPoint.y
                )
                currentSticker!!.setMatrix(moveMatrix)
            }
            if (onStickerOperationListener != null) {
                onStickerOperationListener!!.onStickerZoom(currentSticker!!)
            }
        }
    }

    fun handleHorizontalLeftMovement(event: MotionEvent) {
//        float newDistance = calculateDistance(midPoint.x, midPoint.y, event.getX(), event.getY());
        val x1 = bitmapPoints[0]
        val y1 = bitmapPoints[1]
        val x2 = bitmapPoints[2]
        val y2 = bitmapPoints[3]
        val x4 = bitmapPoints[6]
        val y4 = bitmapPoints[7]
        val mx24 = (x2 + x4) / 2f
        val my24 = (y2 + y4) / 2f
        var newDistance = calculateDistance(mx24, my24, event.x, event.y)
        val oppositeDirection = calculateIsOppositeDirection(x2, x4, y2, y4, event)
        if (oppositeDirection) {
            newDistance = 0f
        }
        val newRotation = calculateRotation(x2, y2, x1, y1)
        Log.d(TAG, "handleHorizontalLeftMovement newrot = $newRotation, oldrot = $oldRotation")
        var d: Float = if (pastMovedDistance == -1f) 0 else newDistance - pastMovedDistance
        var tx = (d * Math.cos(Math.toRadians(newRotation.toDouble()))).toFloat()
        var ty = (d * Math.sin(Math.toRadians(newRotation.toDouble()))).toFloat()
        currentSticker.getMatrix().postTranslate(-tx, -ty)
        var scale = newDistance / (2f * oldDistance)
        currentSticker!!.setXScale(scale)
        val snapPointRuler = findObjectSnapOffsetForResize(OFFSET_RULER)
        if (snapPointRuler != null && !oppositeDirection) {
            pastMovedDistance = newDistance
            newDistance = calculateDistance(
                mx24,
                my24,
                event.x + snapPointRuler.x,
                event.y + snapPointRuler.y
            )
            d = if (pastMovedDistance == -1f) 0 else newDistance - pastMovedDistance
            tx = (d * Math.cos(Math.toRadians(newRotation.toDouble()))).toFloat()
            ty = (d * Math.sin(Math.toRadians(newRotation.toDouble()))).toFloat()
            currentSticker.getMatrix().postTranslate(-tx, -ty)
            scale = newDistance / (2f * oldDistance)
        }
        currentSticker!!.movedLeftHorizontally(scale)
        invalidate()
        pastMovedDistance = newDistance
    }

    fun xhandleHorizontalLeftMovement(event: MotionEvent) {
        val newDistance = calculateDistance(midPoint.x, midPoint.y, event.x, event.y)
        val x1 = bitmapPoints[0]
        val y1 = bitmapPoints[1]
        val x2 = bitmapPoints[2]
        val y2 = bitmapPoints[3]
        val x3 = bitmapPoints[4]
        val y3 = bitmapPoints[5]
        val mx13 = (x1 + x3) / 2f
        val my13 = (y1 + y3) / 2f
        //        float newDistance = calculateDistance(mx13, my13, event.getX(), event.getY());
        val newRotation = calculateRotation(x2, y2, x1, y1)
        val oldWidth = currentSticker.getWidth().toFloat()
        val scale = (newDistance + oldDistance) / (2f * oldDistance)
        //        float scale = (newDistance) / (2f*oldDistance);
        currentSticker!!.movedLeftHorizontally(scale)
        invalidate()
        val newWidth = currentSticker.getWidth().toFloat()
        Log.d(TAG, "handleHorizontalLeftMovement newrot = $newRotation, oldrot = $oldRotation")
        val d = newWidth - oldWidth
        //        float d=newWidth-oldWidth;
        val vx = x2 - x1
        val vy = y2 - y1
        val mv = Math.sqrt((vx * vx + vy * vy).toDouble()).toFloat()
        val dux = d * vx / mv
        val duy = d * vy / mv
        val tx = dux / 2f //x1-dux;
        val ty = duy / 2f //y1-duy;
        //
//        float d1=calculateDistance(x1,y1,x2,y2);
//        float t;
//        t=newWidth/d1;
//        float tx=(((1-t)*x2+t*x1));
//        float ty=(((1-t)*y2+t*y1));

//        float tx= (float) (d*Math.cos(Math.toRadians(newRotation)))/2f;
//        float ty= (float) (d*Math.sin(Math.toRadians(newRotation)))/2f;
        currentSticker.getMatrix().postTranslate(-tx, -ty)
        invalidate()
    }

    fun handleHorizontalRightMovement(event: MotionEvent) {
//        float newDistance = calculateDistance(midPoint.x, midPoint.y, event.getX(), event.getY());
        val x1 = bitmapPoints[0]
        val y1 = bitmapPoints[1]
        val x2 = bitmapPoints[2]
        val y2 = bitmapPoints[3]
        val x3 = bitmapPoints[4]
        val y3 = bitmapPoints[5]
        val mx13 = (x1 + x3) / 2f
        val my13 = (y1 + y3) / 2f
        var newDistance = calculateDistance(mx13, my13, event.x, event.y)
        if (calculateIsOppositeDirection(x1, x3, y1, y3, event)) {
            newDistance = 0f
        }
        var scale = newDistance / (2f * oldDistance)
        currentSticker!!.setXScale(scale)
        val snapPointRuler = findObjectSnapOffsetForResize(OFFSET_RULER)
        if (snapPointRuler != null) {
            newDistance = calculateDistance(
                mx13,
                my13,
                event.x + snapPointRuler.x,
                event.y + snapPointRuler.y
            )
            scale = newDistance / (2f * oldDistance)
        }
        currentSticker!!.movedRightHorizontally(scale)
        invalidate()
    }

    fun handleVerticalTopMovement(event: MotionEvent) {
//        float newDistance = calculateDistance(midPoint.x, midPoint.y, event.getX(), event.getY());
        val x2 = bitmapPoints[2]
        val y2 = bitmapPoints[3]
        val x3 = bitmapPoints[4]
        val y3 = bitmapPoints[5]
        val x4 = bitmapPoints[6]
        val y4 = bitmapPoints[7]
        val mx34 = (x3 + x4) / 2f
        val my34 = (y3 + y4) / 2f
        var newDistance = calculateDistance(mx34, my34, event.x, event.y)
        val newRotation = calculateRotation(x4, y4, x2, y2)
        Log.d(TAG, "handleHorizontalLeftMovement newrot = $newRotation, oldrot = $oldRotation")
        val oppositeDirection = calculateIsOppositeDirection(x3, x4, y3, y4, event)
        var isMinDistanceSet = false
        if (currentSticker is TextSticker) {
            val calculatedMinDistance =
                2f * oldDistance * (currentSticker as TextSticker).calculateMinYDistance() / currentSticker.getYDistance()
            if (newDistance < calculatedMinDistance || oppositeDirection) {
                newDistance = calculatedMinDistance
                isMinDistanceSet = true
            }
        } else if (oppositeDirection) {
            newDistance = 1f
            isMinDistanceSet = true
        }
        var d: Float = if (pastMovedDistance == -1f) 0 else newDistance - pastMovedDistance
        var tx = (d * Math.cos(Math.toRadians(newRotation.toDouble()))).toFloat()
        var ty = (d * Math.sin(Math.toRadians(newRotation.toDouble()))).toFloat()
        currentSticker.getMatrix().postTranslate(-tx, -ty)
        var scale = newDistance / (2f * oldDistance)
        currentSticker!!.setYScale(scale)
        val snapPointRuler = findObjectSnapOffsetForResize(OFFSET_RULER)
        if (snapPointRuler != null && !isMinDistanceSet) {
            pastMovedDistance = newDistance
            newDistance = calculateDistance(
                mx34,
                my34,
                event.x + snapPointRuler.x,
                event.y + snapPointRuler.y
            )
            d = if (pastMovedDistance == -1f) 0 else newDistance - pastMovedDistance
            tx = (d * Math.cos(Math.toRadians(newRotation.toDouble()))).toFloat()
            ty = (d * Math.sin(Math.toRadians(newRotation.toDouble()))).toFloat()
            currentSticker.getMatrix().postTranslate(-tx, -ty)
            scale = newDistance / (2f * oldDistance)
        }
        currentSticker!!.movedTopVertically(scale)
        invalidate()
        pastMovedDistance = newDistance
    }

    fun handleVerticalBottomMovement(event: MotionEvent) {
//        float newDistance = calculateDistance(midPoint.x, midPoint.y, event.getX(), event.getY());
        val x1 = bitmapPoints[0]
        val y1 = bitmapPoints[1]
        val x2 = bitmapPoints[2]
        val y2 = bitmapPoints[3]
        val mx = (x1 + x2) / 2f
        val my = (y1 + y2) / 2f
        var newDistance = calculateDistance(mx, my, event.x, event.y)
        if (calculateIsOppositeDirection(x1, x2, y1, y2, event)) {
            newDistance = 0f
        }
        var scale = newDistance / (2f * oldDistance)
        currentSticker!!.setYScale(scale)
        val snapPointVertical = findObjectSnapOffsetForResize(OFFSET_RULER)
        if (snapPointVertical != null) {
            newDistance = calculateDistance(
                mx,
                my,
                event.x + snapPointVertical.x,
                event.y + snapPointVertical.y
            )
            scale = newDistance / (2f * oldDistance)
        }
        currentSticker!!.movedBottomVertically(scale)
        invalidate()
    }

    fun handleHorizontalRightUp(event: MotionEvent) {
//        float newDistance = calculateDistance(midPoint.x, midPoint.y, event.getX(), event.getY());
        val x1 = bitmapPoints[0]
        val y1 = bitmapPoints[1]
        val x2 = bitmapPoints[4]
        val y2 = bitmapPoints[5]
        val mx = (x1 + x2) / 2f
        val my = (y1 + y2) / 2f
        var newDistance = calculateDistance(mx, my, event.x, event.y)
        if (calculateIsOppositeDirection(x1, x2, y1, y2, event)) {
            newDistance = 0f
        }
        val scale = newDistance / (2f * oldDistance)
        currentSticker!!.upRightHorizontally(scale)
        invalidate()
        //        scaleVerticalSticker(event);


//        moveMatrix.set(handlingSticker.getMatrix());
////        float newDistance = (float) Math.abs(midPoint.x-event.getX());
//
//        PointF pf = handlingSticker.getMappedCenterPoint();
//        moveMatrix.postScale(scale, 1, pf.x, pf.y);
//
//        handlingSticker.setMatrix(moveMatrix);
    }

    fun handleHorizontalLeftUp(event: MotionEvent) {
//        float newDistance = calculateDistance(midPoint.x, midPoint.y, event.getX(), event.getY());
        val x1 = bitmapPoints[2]
        val y1 = bitmapPoints[3]
        val x2 = bitmapPoints[6]
        val y2 = bitmapPoints[7]
        val mx = (x1 + x2) / 2f
        val my = (y1 + y2) / 2f
        var newDistance = calculateDistance(mx, my, event.x, event.y)
        if (calculateIsOppositeDirection(x1, x2, y1, y2, event)) {
            newDistance = 0f
        }
        val scale = newDistance / (2f * oldDistance)
        currentSticker!!.upLeftHorizontally(scale)
        invalidate()
    }

    fun handleVerticalBottomUp(event: MotionEvent) {
//        float newDistance = calculateDistance(midPoint.x, midPoint.y, event.getX(), event.getY());
        val x1 = bitmapPoints[0]
        val y1 = bitmapPoints[1]
        val x2 = bitmapPoints[2]
        val y2 = bitmapPoints[3]
        val mx = (x1 + x2) / 2f
        val my = (y1 + y2) / 2f
        var newDistance = calculateDistance(mx, my, event.x, event.y)
        if (calculateIsOppositeDirection(x1, x2, y1, y2, event)) {
            newDistance = 0f
        }
        val scale = newDistance / (2f * oldDistance)
        currentSticker!!.upBottomVertically(scale)
        invalidate()
    }

    private fun calculateIsOppositeDirection(
        x1: Float,
        x2: Float,
        y1: Float,
        y2: Float,
        event: MotionEvent
    ): Boolean {
        val d = (downX - x1) * (y2 - y1) - (downY - y1) * (x2 - x1)
        val d1 = (event.x - x1) * (y2 - y1) - (event.y - y1) * (x2 - x1)
        return if (d > 0 && d1 < 0 || d < 0 && d1 > 0) {
            true
        } else false
    }

    fun handleVerticalTopUp(event: MotionEvent) {
//        float newDistance = calculateDistance(midPoint.x, midPoint.y, event.getX(), event.getY());
        val x1 = bitmapPoints[4]
        val y1 = bitmapPoints[5]
        val x2 = bitmapPoints[6]
        val y2 = bitmapPoints[7]
        val mx = (x1 + x2) / 2f
        val my = (y1 + y2) / 2f
        var newDistance = calculateDistance(mx, my, event.x, event.y)
        if (calculateIsOppositeDirection(x1, x2, y1, y2, event)) {
            newDistance = 0f
        }
        val scale = newDistance / (2f * oldDistance)
        currentSticker!!.upTopVertically(scale)
        invalidate()
    }

    fun scaleHorizontalSticker(event: MotionEvent) {
        scaleHorizontalSticker(currentSticker!!, event)
    }

    fun scaleHorizontalSticker(sticker: Sticker, event: MotionEvent?) {
        if (sticker != null) {
            moveMatrix.set(downMatrix)
            moveMatrix.postScale(
                1f,
                Math.abs(midPoint.y - event!!.y) / oldDistance,
                midPoint.x,
                midPoint.y
            )
            currentSticker!!.setMatrix(moveMatrix)
        }
    }

    fun scaleVerticalSticker(event: MotionEvent) {
        scaleVerticalSticker(currentSticker!!, event)
    }

    fun scaleVerticalSticker(sticker: Sticker, event: MotionEvent?) {
        if (sticker != null) {
            moveMatrix.set(downMatrix)
            moveMatrix.postScale(
                Math.abs(midPoint.x - event!!.x) / oldDistance,
                1f,
                midPoint.x,
                midPoint.y
            )
            currentSticker!!.setMatrix(moveMatrix)
        }
    }

    fun rotateotateCurrentSticker(event: MotionEvent) {
        rotateCurrentSticker(currentSticker, event)
    }

    fun rotateCurrentSticker(sticker: Sticker?, event: MotionEvent) {
        if (sticker != null) {
            val offset = 2.5f
            val newDistance = calculateDistance(midPoint.x, midPoint.y, event.x, event.y)
            var newRotation = calculateRotation(midPoint.x, midPoint.y, event.x, event.y)
            moveMatrix.set(downMatrix)
            moveMatrix.postRotate(newRotation - oldRotation, midPoint.x, midPoint.y)
            val matrixRotation = getMatrixAngle(moveMatrix)
            var setAngle = false
            val rotationMod = matrixRotation % 45f
            if (rotationMod < -45 + offset || rotationMod > 0 - offset && rotationMod < 0 + offset || rotationMod > 45 - offset) {
                val divide = matrixRotation / 45f
                newRotation = 45f * Math.round(divide)
                setAngle = true
            }
            if (setAngle) {
                moveMatrix.postRotate(newRotation - matrixRotation, midPoint.x, midPoint.y)
            }
            currentSticker!!.setMatrix(moveMatrix)
        }
    }

    fun getMatrixAngle(matrix: Matrix): Float {
        return Math.toDegrees(
            -Math.atan2(
                getMatrixValue(matrix, Matrix.MSKEW_X).toDouble(),
                getMatrixValue(matrix, Matrix.MSCALE_X).toDouble()
            )
        ).toFloat()
    }

    fun getMatrixValue(matrix: Matrix, @IntRange(from = 0, to = 9) valueIndex: Int): Float {
        val matrixValues = FloatArray(9)
        matrix.getValues(matrixValues)
        return matrixValues[valueIndex]
    }

    fun setmIsExpandOrCollapse(mIsExpandOrCollapse: Boolean) {
        this.mIsExpandOrCollapse = mIsExpandOrCollapse
    }

    fun resetDownXY() {
        downX = -1f
        downY = -1f
    }

    fun resetHandlingSticker() {
        currentSticker = null
    }

    fun setHandlingSticker(event: MotionEvent?, scale: Float) {
        if (downX < 0 && downY < 0) {
            currentSticker = null
        } else {
            currentSticker = findHandlingSticker()
        }
        if (currentSticker != null && onStickerOperationListener != null) {
            isSelected = true
            onStickerOperationListener!!.onStickerClicked(currentSticker!!)
            showBorder = true
            showIcons = true
            showCurrentActionIcon = false
            currentMode = ActionMode.NONE
            val currentTime = SystemClock.uptimeMillis()
            lastClickTime = currentTime
            invalidate()
        }
    }

    fun setmTileWidth(tileWidth: Int) {
        mTileWidth = tileWidth
    }

    fun setmTileHeight(tileHeight: Int) {
        mTileHeight = tileHeight
    }

    fun setSliceCount(sliceCount: Int) {
        mSliceCount = sliceCount
    }

    protected fun constrainSticker(sticker: Sticker) {
        var moveX = 0f
        var moveY = 0f
        val width = width
        val height = height
        sticker.getMappedCenterPoint(currentCenterPoint, point, tmp)
        if (currentCenterPoint.x < 0) {
            moveX = -currentCenterPoint.x
        }
        if (currentCenterPoint.x > width) {
            moveX = width - currentCenterPoint.x
        }
        if (currentCenterPoint.y < 0) {
            moveY = -currentCenterPoint.y
        }
        if (currentCenterPoint.y > height) {
            moveY = height - currentCenterPoint.y
        }
        sticker.matrix.postTranslate(moveX, moveY)
    }

    protected fun findCurrentIconTouched(): BitmapStickerIcon? {
//        Log.d(TAG, "onTouchEvent: Current Stiicker event:: icons size ="+icons.size());
        for (icon in icons) {
            if (showMoveIcons) {
                if (icon.position == BitmapStickerIcon.Companion.MID_LEFT || icon.position == BitmapStickerIcon.Companion.MID_RIGHT || icon.position == BitmapStickerIcon.Companion.MID_TOP || icon.position == BitmapStickerIcon.Companion.MID_BOTTOM || icon.position == BitmapStickerIcon.Companion.LEFT_TOP || icon.position == BitmapStickerIcon.Companion.RIGHT_BOTOM) {
                    continue
                }
            } else {
                if (icon.position == BitmapStickerIcon.Companion.MID_RIGHT_CENTER) {
                    continue
                }
            }
            val x = icon.x - downX
            val y = icon.y - downY
            val distance_pow_2 = x * x + y * y
            if (distance_pow_2 <= Math.pow((icon.iconRadius + icon.iconRadius).toDouble(), 2.0)) {
//                Log.d(TAG, "onTouchEvent: Current Stiicker event::"+icon.getIconEvent());
                return icon
            }
        }
        return null
    }

    /**
     * find the touched Sticker
     */
    protected fun findHandlingSticker(): Sticker? {
        for (i in stickers.indices.reversed()) {
            if (isInStickerArea(stickers[i]!!, downX, downY)) {
                return stickers[i]
            }
        }
        return null
    }

    protected fun isInStickerArea(sticker: Sticker, downX: Float, downY: Float): Boolean {
        tmp[0] = downX
        tmp[1] = downY
        return sticker.contains(tmp)
    }

    protected fun calculateMidPoint(event: MotionEvent?): PointF {
        if (event == null || event.pointerCount < 2) {
            midPoint[0f] = 0f
            return midPoint
        }
        val x = (event.getX(0) + event.getX(1)) / 2
        val y = (event.getY(0) + event.getY(1)) / 2
        midPoint[x] = y
        return midPoint
    }

    protected fun calculateMidPoint(): PointF {
        if (currentSticker == null) {
            midPoint[0f] = 0f
            return midPoint
        }
        currentSticker!!.getMappedCenterPoint(midPoint, point, tmp)
        return midPoint
    }

    fun updateAspectRatio(scaleX: Float, scaleY: Float) {}

    /**
     * calculate rotation in line with two fingers and x-axis
     */
    protected fun calculateRotation(event: MotionEvent?): Float {
        return if (event == null || event.pointerCount < 2) {
            0f
        } else calculateRotation(event.getX(0), event.getY(0), event.getX(1), event.getY(1))
    }

    protected fun calculateRotation(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val x = (x1 - x2).toDouble()
        val y = (y1 - y2).toDouble()
        val radians = Math.atan2(y, x)
        return Math.toDegrees(radians).toFloat()
    }

    /**
     * calculate Distance in two fingers
     */
    protected fun calculateDistance(event: MotionEvent?): Float {
        return if (event == null || event.pointerCount < 2) {
            0f
        } else calculateDistance(event.getX(0), event.getY(0), event.getX(1), event.getY(1))
    }

    protected fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val x = (x1 - x2).toDouble()
        val y = (y1 - y2).toDouble()
        return Math.sqrt(x * x + y * y).toFloat()
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        for (i in stickers.indices) {
            val sticker = stickers[i]
            if (sticker != null) {
                //   transformSticker(sticker);
            }
        }
    }

    /**
     * Sticker's drawable will be too bigger or smaller
     * This method is to transform it to fit
     * step 1Ã¯Â¼Å¡let the center of the sticker image is coincident with the center of the View.
     * step 2Ã¯Â¼Å¡Calculate the zoom and zoom
     */
    protected fun transformSticker(sticker: Sticker?) {
        Log.e(TAG, "asticker$mIsAspectRatioSelected $mIsAdLoading $mIsExpandOrCollapse")
        if (sticker == null) {
            Log.e(
                TAG,
                "transformSticker: the bitmapSticker is null or the bitmapSticker bitmap is null"
            )
            return
        }
        if (mIsExpandOrCollapse || mIsAdLoading || mIsAspectRatioSelected) {
            return
        }
        sizeMatrix.reset()
        val width = width.toFloat()
        val height = height.toFloat()
        val stickerWidth = sticker.width.toFloat()
        val stickerHeight = sticker.height.toFloat()
        //step 1
        val offsetX = (width - stickerWidth) / 2
        val offsetY = (height - stickerHeight) / 2
        sizeMatrix.postTranslate(offsetX, offsetY)

        //step 2
        val scaleFactor: Float
        scaleFactor = if (width < height) {
            width / stickerWidth
        } else {
            height / stickerHeight
        }
        sizeMatrix.postScale(scaleFactor / 2f, scaleFactor / 2f, width / 2f, height / 2f)
        sticker.matrix.reset()
        sticker.setMatrix(sizeMatrix)
        invalidate()
    }

    fun flipCurrentSticker(direction: Int) {
        flip(currentSticker, direction)
    }

    fun flip(sticker: Sticker?, @Flip direction: Int) {
        if (sticker != null) {
            sticker.getCenterPoint(midPoint)
            if (direction and FLIP_HORIZONTALLY > 0) {
                sticker.matrix.preScale(-1f, 1f, midPoint.x, midPoint.y)
                sticker.isFlippedHorizontally = !sticker.isFlippedHorizontally
            }
            if (direction and FLIP_VERTICALLY > 0) {
                sticker.matrix.preScale(1f, -1f, midPoint.x, midPoint.y)
                sticker.isFlippedVertically = !sticker.isFlippedVertically
            }
            if (onStickerOperationListener != null) {
                onStickerOperationListener!!.onStickerFlipped(sticker)
            }
            invalidate()
        }
    }

    @JvmOverloads
    fun replace(sticker: Sticker?, needStayState: Boolean = true): Boolean {
        return if (currentSticker != null && sticker != null) {
            val width = width.toFloat()
            val height = height.toFloat()
            if (needStayState) {
                sticker.setMatrix(currentSticker.getMatrix())
                sticker.isFlippedVertically = currentSticker!!.isFlippedVertically
                sticker.isFlippedHorizontally = currentSticker!!.isFlippedHorizontally
                sticker.setXScale(currentSticker!!.getXScale())
                sticker.setYScale(currentSticker!!.getYScale())
                if (sticker is BitmapSticker) {
                    val xDist =
                        currentSticker.getWidth() * 1f / (sticker.bitmapWidth * sticker.getXScale())
                    val yDist =
                        currentSticker.getHeight() * 1f / (sticker.bitmapHeight * sticker.getYScale())
                    sticker.setXDistance(xDist)
                    sticker.setYDistance(yDist)
                } else {
                    sticker.setXDistance(currentSticker!!.getXDistance())
                    sticker.setYDistance(currentSticker!!.getYDistance())
                }
            } else {
                currentSticker.getMatrix().reset()
                // reset scale, angle, and put it in center
                val offsetX = (width - currentSticker.getWidth()) / 2f
                val offsetY = (height - currentSticker.getHeight()) / 2f
                sticker.matrix.postTranslate(offsetX, offsetY)
                val scaleFactor: Float
                scaleFactor = if (width < height) {
                    width / currentSticker.getDrawable().intrinsicWidth
                } else {
                    height / currentSticker.getDrawable().intrinsicHeight
                }
                sticker.matrix.postScale(
                    scaleFactor / 2f,
                    scaleFactor / 2f,
                    width / 2f,
                    height / 2f
                )
            }
            val index = stickers.indexOf(currentSticker)
            stickers[index] = sticker
            currentSticker = sticker
            if (onStickerOperationListener != null) {
                onStickerOperationListener!!.onStickerReplaced(sticker)
            }
            invalidate()
            true
        } else {
            false
        }
    }

    fun remove(sticker: Sticker?): Boolean {
        return if (stickers.contains(sticker)) {
            stickers.remove(sticker)
            if (onStickerOperationListener != null) {
                onStickerOperationListener!!.onStickerDeleted(sticker!!)
            }
            if (currentSticker === sticker) {
                currentSticker = null
            }
            invalidate()
            true
        } else {
            Log.d(
                TAG,
                "remove: the sticker is not in this StickerView"
            )
            false
        }
    }

    fun removeCurrentSticker(): Boolean {
        return remove(currentSticker)
    }

    fun removeAllStickers() {
        stickers.clear()
        if (currentSticker != null) {
            currentSticker!!.release()
            currentSticker = null
        }
        invalidate()
    }

    fun addSticker(sticker: Sticker, i: Int, n: Int): StickerView {
        return addSticker(sticker, true, i, n)
    }

    fun addSticker(sticker: Sticker, isSelected: Boolean, i: Int, n: Int): StickerView {
        return addSticker(sticker, Sticker.Position.Companion.CENTER, isSelected, i, n)
    }

    fun addSticker(
        sticker: Sticker,
        @Sticker.Position position: Int, isSelected: Boolean,
        i: Int, n: Int
    ): StickerView {
        if (ViewCompat.isLaidOut(this)) {
            addStickerImmediately(sticker, position, isSelected, i, n)
        } else {
            post { addStickerImmediately(sticker, position, isSelected, i, n) }
        }
        return this
    }

    protected fun addStickerImmediately(
        sticker: Sticker,
        @Sticker.Position position: Int,
        isSelected: Boolean,
        i: Int,
        n: Int
    ) {
        setStickerPosition(sticker, position, i, n)
        val scaleFactor: Float
        val widthScaleFactor: Float
        val heightScaleFactor: Float
        widthScaleFactor = (width / 1).toFloat() / sticker.drawable.intrinsicWidth
        heightScaleFactor = (height / 1).toFloat() / sticker.drawable.intrinsicHeight
        scaleFactor =
            if (widthScaleFactor > heightScaleFactor) heightScaleFactor else widthScaleFactor
        sticker.matrix
            .postScale(
                scaleFactor / 2,
                scaleFactor / 2,
                (width / 2).toFloat(),
                (height / 2).toFloat()
            )
        if (isSelected == true) {
            currentSticker = sticker
        } else {
            currentSticker = null
        }
        stickers.add(sticker)
        if (onStickerOperationListener != null) {
            onStickerOperationListener!!.onStickerAdded(sticker)
        }
        invalidate()
    }

    fun setStickerPosition(sticker: Sticker, @Sticker.Position position: Int, i: Int, n: Int) {
        val width = width.toFloat()
        val height = height.toFloat()
        var offsetX = width - sticker.width
        var offsetY = height - sticker.height
        val overlapOffset = calculateCenterOffset(i, n)
        if (position and Sticker.Position.Companion.TOP > 0) {
            offsetY /= 4f
            Log.d(TAG, "setStickerPosition: Offset$offsetY")
        } else if (position and Sticker.Position.Companion.BOTTOM > 0) {
            offsetY *= 3f / 4f
        } else {
            offsetY /= 2f
        }
        if (position and Sticker.Position.Companion.LEFT > 0) {
            offsetX /= 4f
        } else if (position and Sticker.Position.Companion.RIGHT > 0) {
            offsetX *= 3f / 4f
        } else {
            offsetX /= 2f
        }
        sticker.matrix.postTranslate(offsetX + overlapOffset, offsetY)
    }

    private fun calculateCenterOffset(k: Int, n: Int): Float {
        val centerX = width / 2f
        val centerY = height / 2f
        var offset = 0f
        val step = 200f
        val startX = -((n - 1) * 100) / 2f
        offset = startX + step * k
        return offset
    }

    @JvmOverloads
    fun addSticker(sticker: Sticker, isSelected: Boolean = true): StickerView {
        return addSticker(sticker, Sticker.Position.Companion.CENTER, isSelected)
    }

    @JvmOverloads
    fun addSticker(
        sticker: Sticker,
        @Sticker.Position position: Int, isSelected: Boolean = true
    ): StickerView {
        if (ViewCompat.isLaidOut(this)) {
            addStickerImmediately(sticker, position, isSelected)
        } else {
            post { addStickerImmediately(sticker, position, isSelected) }
        }
        return this
    }

    @JvmOverloads
    fun addStickerWithatrix(
        sticker: Sticker,
        matrix: Matrix?,
        isSelected: Boolean = true
    ): StickerView {
        if (ViewCompat.isLaidOut(this)) {
            addStickerImmediately(sticker, matrix, isSelected)
        } else {
            post { addStickerImmediately(sticker, matrix, isSelected) }
        }
        return this
    }

    protected fun addStickerImmediately(sticker: Sticker, matrix: Matrix?, isSelected: Boolean) {
        if (isSelected == true) {
            currentSticker = sticker
        } else {
            currentSticker = null
        }
        sticker.setMatrix(matrix)
        stickers.add(sticker)
        if (onStickerOperationListener != null) {
            onStickerOperationListener!!.onStickerAdded(sticker)
        }
        invalidate()
    }

    protected fun addStickerImmediately(
        sticker: Sticker,
        @Sticker.Position position: Int,
        isSelected: Boolean
    ) {
        setStickerPosition(sticker, position)
        val scaleFactor: Float
        val widthScaleFactor: Float
        val heightScaleFactor: Float
        widthScaleFactor = (width / 1).toFloat() / sticker.drawable.intrinsicWidth
        heightScaleFactor = (height / 1).toFloat() / sticker.drawable.intrinsicHeight
        scaleFactor =
            if (widthScaleFactor > heightScaleFactor) heightScaleFactor else widthScaleFactor
        sticker.matrix
            .postScale(
                scaleFactor / 2,
                scaleFactor / 2,
                (width / 2).toFloat(),
                (height / 2).toFloat()
            )
        if (isSelected == true) {
            currentSticker = sticker
        } else {
            currentSticker = null
        }
        stickers.add(sticker)
        if (onStickerOperationListener != null) {
            onStickerOperationListener!!.onStickerAdded(sticker)
        }
        invalidate()
    }

    fun setStickerPosition(sticker: Sticker, @Sticker.Position position: Int) {
        val width = width.toFloat()
        val height = height.toFloat()
        var offsetX = width - sticker.width
        var offsetY = height - sticker.height
        val overlapOffset = calculateCenterOffset()
        if (position and Sticker.Position.Companion.TOP > 0) {
            offsetY /= 4f
            Log.d(TAG, "setStickerPosition: Offset$offsetY")
        } else if (position and Sticker.Position.Companion.BOTTOM > 0) {
            offsetY *= 3f / 4f
        } else {
            offsetY /= 2f
        }
        if (position and Sticker.Position.Companion.LEFT > 0) {
            offsetX /= 4f
        } else if (position and Sticker.Position.Companion.RIGHT > 0) {
            offsetX *= 3f / 4f
        } else {
            offsetX /= 2f
        }
        sticker.matrix.postTranslate(offsetX + overlapOffset, offsetY)
    }

    private fun calculateCenterOffset(): Float {
        var centerX = width / 2f
        var centerY = height / 2f
        var offset = 0f
        val step = 25f
        val margin = step / 2f
        for (i in stickers.indices) {
            centerX += offset
            centerY += offset
            val sticker = stickers[i]
            val point = sticker!!.mappedCenterPoint
            if (sticker.currentAngle == 0f && Math.abs(centerX - point.x) < margin && Math.abs(
                    centerY - point.y
                ) < margin
            ) {
                offset += step
            }
        }
        return offset
    }

    fun getStickerPoints(sticker: Sticker?): FloatArray {
        val points = FloatArray(8)
        getStickerPoints(sticker, points)
        return points
    }

    fun getStickerPoints(sticker: Sticker?, dst: FloatArray) {
        if (sticker == null) {
            Arrays.fill(dst, 0f)
            return
        }
        sticker.getBoundPoints(bounds)
        sticker.getMappedPoints(dst, bounds)
    }

    fun save(file: File) {
        try {
            StickerUtils.saveImageToGallery(file, createBitmap())
            StickerUtils.notifySystemGallery(context, file)
        } catch (ignored: IllegalArgumentException) {
            //
        } catch (ignored: IllegalStateException) {
        }
    }

    @Throws(OutOfMemoryError::class)
    fun createBitmap(): Bitmap {
        currentSticker = null
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        draw(canvas)
        return bitmap
    }

    val stickerCount: Int
        get() = stickers.size
    val isNoneSticker: Boolean
        get() = stickerCount == 0

    fun setLocked(locked: Boolean): StickerView {
        isLocked = locked
        invalidate()
        return this
    }

    fun setMinClickDelayTime(minClickDelayTime: Int): StickerView {
        this.minClickDelayTime = minClickDelayTime
        return this
    }

    fun setConstrained(constrained: Boolean): StickerView {
        isConstrained = constrained
        postInvalidate()
        return this
    }

    fun setOnStickerOperationListener(
        onStickerOperationListener: OnStickerOperationListener?
    ): StickerView {
        this.onStickerOperationListener = onStickerOperationListener
        return this
    }

    fun getIcons(): List<BitmapStickerIcon> {
        return icons
    }

    fun setIcons(icons: List<BitmapStickerIcon>) {
        this.icons.clear()
        this.icons.addAll(icons)
        invalidate()
    }

    fun addSquarePoints(midPints: List<List<PointF>>?) {
        squarePointList.clear()
        squarePointList.addAll(midPints!!)
    }

    fun addPoints(midPints: List<PointF>) {
        midPointList.clear()
        midPointList.addAll(midPints)
        addSnapPoints(midPints)
    }

    private fun addSnapPoints(midPints: List<PointF>) {
        val layoutParams = layoutParams
        snapPointList.clear()
        snapPointList.add(PointF(1, 0))
        snapPointList.add(PointF(layoutParams.width.toFloat(), layoutParams.height.toFloat()))
        snapPointList.addAll(midPointList)
    }

    fun removePoints(points: List<PointF>?) {
        midPointList.removeAll(points!!)
    }

    fun removeLastSnapPointsSpecifiedByCount(count: Int) {
        var count = count
        do {
            if (!midPointList.isEmpty()) midPointList.removeAt(midPointList.size - 1)
            count--
        } while (count > 0)
    }

    fun addPoint(pointF: PointF) {
        midPointList.add(pointF)
    }

    interface OnStickerOperationListener {
        fun onStickerAdded(sticker: Sticker)
        fun onStickerClicked(sticker: Sticker)
        fun onStickerDeleted(sticker: Sticker)
        fun onStickerDragFinished(sticker: Sticker)
        fun onStickerTouchedOutside()
        fun onStickerTouchedDown(sticker: Sticker)
        fun onStickerZoom(sticker: Sticker)
        fun onStickerZoomFinished(sticker: Sticker)
        fun onStickerHorizontalScale(sticker: Sticker?)
        fun onStickerVerticalScale(sticker: Sticker?)
        fun onStickerRotateFinished(sticker: Sticker)
        fun onStickerFlipped(sticker: Sticker)
        fun onStickerDoubleTapped(sticker: Sticker)
        fun onSnapPointDone(pointF: PointF?)
        fun dismissRural()
        fun onStickerVerticalMovementFinished(sticker: Sticker)
        fun onStickerHorizontalMovementFinished(sticker: Sticker)
        fun onStickerReplaced(sticker: Sticker?)
    }

    companion object {
        private const val TAG = "StickerView"
        private const val DEFAULT_MIN_CLICK_DELAY_TIME = 200
        const val FLIP_HORIZONTALLY = 1
        const val FLIP_VERTICALLY = 1 shl 1
        const val OFFSET_RULER = 20f
        const val SQUARE_RULER = 100f
    }
}