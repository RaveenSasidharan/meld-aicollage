package com.panoslice.stickerview.sticker.richpath

import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.MotionEvent
import android.widget.ImageView
import androidx.annotation.IntRange
import com.xiaopo.flying.sticker.richpath.listener.OnRichPathUpdatedListener
import java.util.ArrayList

/**
 * Created by tarek on 6/29/17.
 */
class RichPathDrawable(vector: Vector?, scaleType: ImageView.ScaleType) : Drawable() {
    private val vector: Vector?
    var drawableWidth = 0
        private set
    var drawableHeight = 0
        private set
    private val scaleType: ImageView.ScaleType

    init {
        this.vector = vector
        this.scaleType = scaleType
        listenToPathsUpdates()
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        if (bounds.width() > 0 && bounds.height() > 0) {
            drawableWidth = bounds.width()
            drawableHeight = bounds.height()
            mapPaths()
        }
        Log.e("stickerT", "width" + drawableWidth + "height" + drawableHeight)
    }

    fun mapPaths() {
        if (vector == null) return
        val centerX = (drawableWidth / 2).toFloat()
        val centerY = (drawableHeight / 2).toFloat()
        val matrix = Matrix()
        matrix.postTranslate(
            centerX - vector.getCurrentWidth() / 2,
            centerY - vector.getCurrentHeight() / 2
        )
        val widthRatio: Float = drawableWidth / vector.getCurrentWidth()
        val heightRatio: Float = drawableHeight / vector.getCurrentHeight()
        if (scaleType == ImageView.ScaleType.FIT_XY) {
            matrix.postScale(widthRatio, heightRatio, centerX, centerY)
        } else {
            val ratio: Float
            ratio = if (drawableWidth < drawableHeight) {
                widthRatio
            } else {
                heightRatio
            }
            matrix.postScale(ratio, ratio, centerX, centerY)
        }
        val absWidthRatio: Float = drawableWidth / vector.getViewportWidth()
        val absHeightRatio: Float = drawableHeight / vector.getViewportHeight()
        val absRatio = Math.min(absWidthRatio, absHeightRatio)
        for (path in vector.paths) {
            path.mapToMatrix(matrix)
            path.scaleStrokeWidth(absRatio)
        }
        vector.setCurrentWidth(drawableWidth)
        vector.setCurrentHeight(drawableHeight)
    }

    fun findAllRichPaths(): Array<RichPath?> {
        if (vector == null) {
            return arrayOfNulls(0)
        }
        val richPathArr = arrayOfNulls<RichPath>(vector.paths.size())
        return vector.paths.toArray(richPathArr)
    }

    fun findAllRichPathsAsList(): List<RichPath> {
        if (vector == null) {
            return ArrayList()
        }
        val richPathArr = arrayOfNulls<RichPath>(vector.paths.size())
        return vector.paths
    }

    fun clearAllPaths() {
        if (vector.paths != null
            && vector.paths.size() > 0
        ) vector.paths.clear()
    }

    fun findRichPathByName(name: String): RichPath? {
        if (vector == null) return null
        for (path in vector.paths) {
            if (name == path.name) {
                return path
            }
        }
        return null
    }

    /**
     * find the first [RichPath] or null if not found
     *
     *
     * This can be in handy if the vector consists of 1 path only
     *
     * @return the [RichPath] object found or null
     */
    fun findFirstRichPath(): RichPath? {
        return findRichPathByIndex(0)
    }

    /**
     * find [RichPath] by its index or null if not found
     *
     *
     * Note that the provided index must be the flattened index of the path
     *
     *
     * example:
     * <pre>
     * `<vector>
     * <path> // index = 0
     * <path> // index = 1
     * <group>
     * <path> // index = 2
     * <group>
     * <path> // index = 3
     * </group>
     * </group>
     * <path> // index = 4
     * </vector>`
    </pre> *
     *
     * @param index the flattened index of the path
     * @return the [RichPath] object found or null
     */
    fun findRichPathByIndex(@IntRange(from = 0) index: Int): RichPath? {
        return if (vector == null || index < 0 || index >= vector.paths.size()) null else vector.paths.get(
            index
        )
    }

    fun listenToPathsUpdates() {
        if (vector == null) return
        for (path in vector.paths) {
            path.setOnRichPathUpdatedListener(object : OnRichPathUpdatedListener() {
                fun onPathUpdated() {
                    invalidateSelf()
                }
            })
        }
    }

    fun addPath(path: String?) {
        addPath(PathParserT.createPathFromPathData(path))
    }

    fun addPath(path: Path?) {
        if (path is RichPath) {
            addPath(path)
        } else {
            addPath(RichPath(path))
        }
    }

    private fun addPath(path: RichPath) {
        if (vector == null) return
        vector.paths.add(path)
        path.setOnRichPathUpdatedListener(object : OnRichPathUpdatedListener() {
            fun onPathUpdated() {
                invalidateSelf()
            }
        })
        invalidateSelf()
    }

    fun getTouchedPath(event: MotionEvent): RichPath? {
        if (vector == null) return null
        val action = event.action
        if (action == MotionEvent.ACTION_UP) {
            for (i in vector.paths.size() - 1 downTo 0) {
                val richPath: RichPath = vector.paths.get(i)
                if (PathUtils.isTouched(richPath, event.x, event.y)) {
                    return richPath
                }
            }
        }
        return null
    }

    override fun draw(canvas: Canvas) {
        if (vector == null || vector.paths.size() < 0) return
        for (path in vector.paths) {
            path.draw(canvas)
        }
    }

    override fun setAlpha(@IntRange(from = 0, to = 255) alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    fun getWidth(): Int { //TODO merge in a new class
        return vector.getWidth()
    }

    fun getHeight(): Int { //TODO merge in a new class
        return vector.getHeight()
    }
}