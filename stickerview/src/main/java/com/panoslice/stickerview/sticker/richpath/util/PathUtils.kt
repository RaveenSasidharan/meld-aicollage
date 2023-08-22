package com.panoslice.stickerview.sticker.richpath.util

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Region
import com.xiaopo.flying.sticker.richpath.pathparser.PathDataNode

/**
 * Created by tarek on 6/29/17.
 */
object PathUtils {
    fun resizePath(path: Path, width: Float, height: Float) {
        val bounds = RectF(0, 0, width, height)
        val src = RectF()
        path.computeBounds(src, true)
        val resizeMatrix = Matrix()
        resizeMatrix.setRectToRect(src, bounds, Matrix.ScaleToFit.FILL)
        path.transform(resizeMatrix)
    }

    fun setPathWidth(path: Path, width: Float) {
        val src = RectF()
        path.computeBounds(src, true)
        val resizeMatrix = Matrix()
        val bounds = RectF(src.left, src.top, src.left + width, src.bottom)
        resizeMatrix.setRectToRect(src, bounds, Matrix.ScaleToFit.FILL)
        path.transform(resizeMatrix)
    }

    fun setPathHeight(path: Path, height: Float) {
        val src = RectF()
        path.computeBounds(src, true)
        val resizeMatrix = Matrix()
        val bounds = RectF(src.left, src.top, src.right, src.top + height)
        resizeMatrix.setRectToRect(src, bounds, Matrix.ScaleToFit.FILL)
        path.transform(resizeMatrix)
    }

    fun getPathWidth(path: Path): Float {
        val rect = RectF()
        path.computeBounds(rect, true)
        return rect.width()
    }

    fun getPathHeight(path: Path): Float {
        val rect = RectF()
        path.computeBounds(rect, true)
        return rect.height()
    }

    fun setPathRotation(path: Path, rotation: Float) {
        val rect = RectF()
        path.computeBounds(rect, true)
        setPathRotation(path, rotation, rect.centerX(), rect.centerY())
    }

    fun setPathRotation(path: Path, rotation: Float, px: Float, py: Float) {
        val matrix = Matrix()
        matrix.setRotate(rotation, px, py)
        path.transform(matrix)
    }

    fun setPathTranslationX(path: Path, translationX: Float) {
        val matrix = Matrix()
        matrix.postTranslate(translationX, 0f)
        path.transform(matrix)
    }

    fun setPathTranslationY(path: Path, translationY: Float) {
        val matrix = Matrix()
        matrix.setTranslate(0f, translationY)
        path.transform(matrix)
    }

    fun setPathScaleX(path: Path, scale: Float, px: Float, py: Float) {
        val matrix = Matrix()
        matrix.setScale(scale, 1f, px, py)
        path.transform(matrix)
    }

    fun setPathScaleY(path: Path, scale: Float, px: Float, py: Float) {
        val matrix = Matrix()
        matrix.setScale(1f, scale, px, py)
        path.transform(matrix)
    }

    fun setPathScaleX(path: Path, scale: Float) {
        val rect = RectF()
        path.computeBounds(rect, true)
        setPathScaleX(path, scale, rect.centerX(), rect.centerY())
    }

    fun setPathScaleY(path: Path, scale: Float) {
        val rect = RectF()
        path.computeBounds(rect, true)
        setPathScaleY(path, scale, rect.centerX(), rect.centerY())
    }

    fun setPathDataNodes(path: Path, pathDataNodes: Array<PathDataNode?>?) {
        path.reset()
        PathDataNode.nodesToPath(pathDataNodes, path)
    }

    fun isTouched(path: Path, x: Float, y: Float): Boolean {
        val region = Region()
        val rectF = RectF()
        path.computeBounds(rectF, true)
        region.setPath(
            path,
            Region(rectF.left.toInt(), rectF.top.toInt(), rectF.right.toInt(), rectF.bottom.toInt())
        )
        val offset = 10
        return (region.contains(x.toInt(), y.toInt())
                || region.contains(x.toInt() + offset, y.toInt() + offset)
                || region.contains(x.toInt() + offset, y.toInt() - offset)
                || region.contains(x.toInt() - offset, y.toInt() - offset)
                || region.contains(x.toInt() - offset, y.toInt() + offset))
    }
}