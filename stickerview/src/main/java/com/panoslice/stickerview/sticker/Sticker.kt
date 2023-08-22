package com.panoslice.stickerview.sticker

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.Drawable
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
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * @author wupanjie
 */
abstract class Sticker {
    @IntDef(
        flag = true,
        value = [Position.CENTER, Position.TOP, Position.BOTTOM, Position.LEFT, Position.RIGHT, Position.MID_BOTTOM_CENTER]
    )
    @Retention(
        RetentionPolicy.SOURCE
    )
    annotation class Position {
        companion object {
            var CENTER = 1
            var TOP = 1 shl 1
            var LEFT = 1 shl 2
            var RIGHT = 1 shl 3
            var BOTTOM = 1 shl 4
            var MID_BOTTOM_CENTER = 1 shl 5
        }
    }

    private val matrixValues = FloatArray(9)
    private val unrotatedWrapperCorner = FloatArray(8)
    private val unrotatedPoint = FloatArray(2)
    private val boundPoints = FloatArray(8)
    private val mappedBounds = FloatArray(8)
    private val trappedRect = RectF()
    val matrix = Matrix()
    var isFlippedHorizontally = false
        private set
    var isFlippedVertically = false
        private set
    protected var xDistance = 0f
    protected var yDistance = 0f
    protected var xScale = 1.0f
    protected var yScale = 1.0f
    fun getXDistance(): Float {
        return xDistance
    }

    fun getYDistance(): Float {
        return yDistance
    }

    fun getXScale(): Float {
        return xScale
    }

    fun getYScale(): Float {
        return yScale
    }

    fun setXDistance(xDistance: Float) {
        this.xDistance = xDistance
        moveHorizontally()
    }

    fun setYDistance(yDistance: Float) {
        this.yDistance = yDistance
        moveVertically()
    }

    fun setXScale(xScale: Float) {
        this.xScale = xScale
        moveHorizontally()
    }

    fun setYScale(yScale: Float) {
        this.yScale = yScale
        moveVertically()
    }

    fun setFlippedHorizontally(flippedHorizontally: Boolean): Sticker {
        isFlippedHorizontally = flippedHorizontally
        return this
    }

    fun setFlippedVertically(flippedVertically: Boolean): Sticker {
        isFlippedVertically = flippedVertically
        return this
    }

    fun setMatrix(matrix: Matrix?): Sticker {
        this.matrix.set(matrix)
        return this
    }

    abstract fun draw(canvas: Canvas)
    abstract val width: Int
    abstract val height: Int
    abstract fun movedLeftHorizontally(distance: Float)
    open fun upRightHorizontally(distance: Float) {}
    open fun upLeftHorizontally(distance: Float) {}
    open fun upTopVertically(distance: Float) {}
    open fun upBottomVertically(distance: Float) {}
    open fun moveVertically() {}
    open fun moveHorizontally() {}
    open val fWidth: Float
        get() = (-1).toFloat()
    open val fHeight: Float
        get() = (-1).toFloat()

    abstract fun movedRightHorizontally(distance: Float)
    abstract fun movedTopVertically(distance: Float)
    abstract fun movedBottomVertically(distance: Float)
    abstract fun setDrawable(drawable: Drawable): Sticker
    abstract val drawable: Drawable
    abstract fun setAlpha(@IntRange(from = 0, to = 255) alpha: Int): Sticker
    fun getBoundPoints(): FloatArray {
        val points = FloatArray(8)
        getBoundPoints(points)
        return points
    }

    fun getBoundPoints(points: FloatArray) {
        if (!isFlippedHorizontally) {
            if (!isFlippedVertically) {
                points[0] = 0f
                points[1] = 0f
                points[2] = width.toFloat()
                points[3] = 0f
                points[4] = 0f
                points[5] = height.toFloat()
                points[6] = width.toFloat()
                points[7] = height.toFloat()
            } else {
                points[0] = 0f
                points[1] = height.toFloat()
                points[2] = width.toFloat()
                points[3] = height.toFloat()
                points[4] = 0f
                points[5] = 0f
                points[6] = width.toFloat()
                points[7] = 0f
            }
        } else {
            if (!isFlippedVertically) {
                points[0] = width.toFloat()
                points[1] = 0f
                points[2] = 0f
                points[3] = 0f
                points[4] = width.toFloat()
                points[5] = height.toFloat()
                points[6] = 0f
                points[7] = height.toFloat()
            } else {
                points[0] = width.toFloat()
                points[1] = height.toFloat()
                points[2] = 0f
                points[3] = height.toFloat()
                points[4] = width.toFloat()
                points[5] = 0f
                points[6] = 0f
                points[7] = 0f
            }
        }
    }

    val mappedBoundPoints: FloatArray
        get() {
            val dst = FloatArray(8)
            getMappedPoints(dst, getBoundPoints())
            return dst
        }

    fun getMappedPoints(src: FloatArray): FloatArray {
        val dst = FloatArray(src.size)
        matrix.mapPoints(dst, src)
        return dst
    }

    fun getMappedPoints(dst: FloatArray, src: FloatArray) {
        matrix.mapPoints(dst, src)
    }

    val bound: RectF
        get() {
            val bound = RectF()
            getBound(bound)
            return bound
        }

    fun getBound(dst: RectF) {
        dst[0f, 0f, width.toFloat()] = height.toFloat()
    }

    val mappedBound: RectF
        get() {
            val dst = RectF()
            getMappedBound(dst, bound)
            return dst
        }

    fun getMappedBound(dst: RectF, bound: RectF) {
        matrix.mapRect(dst, bound)
    }

    val centerPoint: PointF
        get() {
            val center = PointF()
            getCenterPoint(center)
            return center
        }

    fun getCenterPoint(dst: PointF) {
        dst[width * 1f / 2] = height * 1f / 2
    }

    val mappedCenterPoint: PointF
        get() {
            val pointF = centerPoint
            getMappedCenterPoint(pointF, FloatArray(2), FloatArray(2))
            return pointF
        }

    fun getMappedCenterPoint(
        dst: PointF, mappedPoints: FloatArray,
        src: FloatArray
    ) {
        getCenterPoint(dst)
        src[0] = dst.x
        src[1] = dst.y
        getMappedPoints(mappedPoints, src)
        dst[mappedPoints[0]] = mappedPoints[1]
    }

    val currentScale: Float
        get() = getMatrixScale(matrix)
    val currentHeight: Float
        get() = getMatrixScale(matrix) * height
    val currentWidth: Float
        get() = getMatrixScale(matrix) * width

    /**
     * This method calculates scale value for given Matrix object.
     */
    fun getMatrixScale(matrix: Matrix): Float {
        return Math.sqrt(
            Math.pow(getMatrixValue(matrix, Matrix.MSCALE_X).toDouble(), 2.0) + Math.pow(
                getMatrixValue(matrix, Matrix.MSKEW_Y).toDouble(), 2.0
            )
        ).toFloat()
    }

    /**
     * @return - current image rotation angle.
     */
    val currentAngle: Float
        get() = getMatrixAngle(matrix)

    /**
     * This method calculates rotation angle for given Matrix object.
     */
    fun getMatrixAngle(matrix: Matrix): Float {
        return Math.toDegrees(
            -Math.atan2(
                getMatrixValue(matrix, Matrix.MSKEW_X).toDouble(),
                getMatrixValue(matrix, Matrix.MSCALE_X).toDouble()
            )
        ).toFloat()
    }

    fun getMatrixValue(matrix: Matrix, @IntRange(from = 0, to = 9) valueIndex: Int): Float {
        matrix.getValues(matrixValues)
        return matrixValues[valueIndex]
    }

    fun contains(x: Float, y: Float): Boolean {
        return contains(floatArrayOf(x, y))
    }

    operator fun contains(point: FloatArray): Boolean {
        val tempMatrix = Matrix()
        tempMatrix.setRotate(-currentAngle)
        getBoundPoints(boundPoints)
        getMappedPoints(mappedBounds, boundPoints)
        tempMatrix.mapPoints(unrotatedWrapperCorner, mappedBounds)
        tempMatrix.mapPoints(unrotatedPoint, point)
        StickerUtils.trapToRect(trappedRect, unrotatedWrapperCorner)
        return trappedRect.contains(unrotatedPoint[0], unrotatedPoint[1])
    }

    open fun release() {}
}