package com.panoslice.stickerview.sticker.richpath

import android.content.Context
import android.content.res.XmlResourceParser
import android.graphics.*
import com.xiaopo.flying.sticker.richpath.listener.OnRichPathUpdatedListener
import java.util.ArrayList

/**
 * Created by tarek on 6/29/17.
 */
class RichPath(src: Path?) : Path(src) {
    private var fillColor = Color.TRANSPARENT
    private var strokeColor = Color.TRANSPARENT
    private var fillAlpha = 1.0f
    private var strokeAlpha = 1.0f
    private var strokeWidth = 0f
    private var trimPathStart = 0f
    private var trimPathEnd = 1f
    private var trimPathOffset = 0f
    private var strokeLineCap = Paint.Cap.BUTT
    private var strokeLineJoin = Paint.Join.MITER
    private var strokeMiterLimit = 4f
    var name: String? = null
    private var paint: Paint? = null
    var rotation = 0f
        set(rotation) {
            val deltaValue = rotation - this.rotation
            if (isPivotToCenter) {
                PathUtils.setPathRotation(this, deltaValue)
                PathUtils.setPathRotation(originalPath, deltaValue)
            } else {
                PathUtils.setPathRotation(this, deltaValue, pivotX, pivotY)
                PathUtils.setPathRotation(originalPath, deltaValue, pivotX, pivotY)
            }
            field = rotation
            onPathUpdated()
        }//reset scaling

    //new scaling
    //reset scaling
    //new scaling
    var scaleX = 1f
        set(scaleX) {
            if (isPivotToCenter) {
                //reset scaling
                PathUtils.setPathScaleX(this, 1.0f / this.scaleX)
                PathUtils.setPathScaleX(originalPath, 1.0f / this.scaleX)
                //new scaling
                PathUtils.setPathScaleX(this, scaleX)
                PathUtils.setPathScaleX(originalPath, scaleX)
            } else {
                //reset scaling
                PathUtils.setPathScaleX(this, 1.0f / this.scaleX, pivotX, pivotY)
                PathUtils.setPathScaleX(originalPath, 1.0f / this.scaleX, pivotX, pivotY)
                //new scaling
                PathUtils.setPathScaleX(this, scaleX, pivotX, pivotY)
                PathUtils.setPathScaleX(originalPath, scaleX, pivotX, pivotY)
            }
            field = scaleX
            onPathUpdated()
        }//reset scaling

    //new scaling
    //reset scaling
    //new scaling
    var scaleY = 1f
        set(scaleY) {
            if (isPivotToCenter) {
                //reset scaling
                PathUtils.setPathScaleY(this, 1.0f / this.scaleY)
                PathUtils.setPathScaleY(originalPath, 1.0f / this.scaleY)
                //new scaling
                PathUtils.setPathScaleY(this, scaleY)
                PathUtils.setPathScaleY(originalPath, scaleY)
            } else {
                //reset scaling
                PathUtils.setPathScaleY(this, 1.0f / this.scaleY, pivotX, pivotY)
                PathUtils.setPathScaleY(originalPath, 1.0f / this.scaleY, pivotX, pivotY)
                //new scaling
                PathUtils.setPathScaleY(this, scaleY, pivotX, pivotY)
                PathUtils.setPathScaleY(originalPath, scaleY, pivotX, pivotY)
            }
            field = scaleY
            onPathUpdated()
        }
    var translationX = 0f
        set(translationX) {
            PathUtils.setPathTranslationX(this, translationX - this.translationX)
            PathUtils.setPathTranslationX(originalPath, translationX - this.translationX)
            field = translationX
            onPathUpdated()
        }
    var translationY = 0f
        set(translationY) {
            PathUtils.setPathTranslationY(this, translationY - this.translationY)
            PathUtils.setPathTranslationY(originalPath, translationY - this.translationY)
            field = translationY
            onPathUpdated()
        }
    var originalWidth = 0f
        private set
    var originalHeight = 0f
        private set
    var pivotX = 0f
    var pivotY = 0f
    var isPivotToCenter = false
    private var onRichPathUpdatedListener: OnRichPathUpdatedListener? = null
    private var pathMeasure: PathMeasure? = null
    private var originalPath: Path? = null
    private var pathDataNodes: Array<PathDataNode>
    private var matrices: MutableList<Matrix>? = null
    var onPathClickListener: OnPathClickListener? = null

    constructor(pathData: String?) : this(PathParserT.createPathFromPathData(pathData)) {}

    init {
        originalPath = src
        init()
    }

    private fun init() {
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint!!.style = Paint.Style.STROKE
        matrices = ArrayList()
        updateOriginalDimens()
    }

    var width: Float
        get() = PathUtils.getPathWidth(this)
        set(width) {
            PathUtils.setPathWidth(this, width)
            PathUtils.setPathWidth(originalPath, width)
            onPathUpdated()
        }
    var height: Float
        get() = PathUtils.getPathHeight(this)
        set(height) {
            PathUtils.setPathHeight(this, height)
            PathUtils.setPathHeight(originalPath, height)
            onPathUpdated()
        }

    fun setOnRichPathUpdatedListener(onRichPathUpdatedListener: OnRichPathUpdatedListener?) {
        this.onRichPathUpdatedListener = onRichPathUpdatedListener
    }

    fun getOnRichPathUpdatedListener(): OnRichPathUpdatedListener? {
        return onRichPathUpdatedListener
    }

    fun getStrokeColor(): Int {
        return strokeColor
    }

    fun setStrokeColor(strokeColor: Int) {
        this.strokeColor = strokeColor
        onPathUpdated()
    }

    fun getStrokeWidth(): Float {
        return strokeWidth
    }

    fun setStrokeWidth(strokeWidth: Float) {
        this.strokeWidth = strokeWidth
        onPathUpdated()
    }

    fun getFillColor(): Int {
        return fillColor
    }

    fun setFillColor(fillColor: Int) {
        this.fillColor = fillColor
        onPathUpdated()
    }

    fun getStrokeAlpha(): Float {
        return strokeAlpha
    }

    fun setStrokeAlpha(strokeAlpha: Float) {
        this.strokeAlpha = strokeAlpha
        onPathUpdated()
    }

    fun getFillAlpha(): Float {
        return fillAlpha
    }

    fun setFillAlpha(fillAlpha: Float) {
        this.fillAlpha = fillAlpha
        onPathUpdated()
    }

    fun getTrimPathStart(): Float {
        return trimPathStart
    }

    fun setTrimPathStart(trimPathStart: Float) {
        this.trimPathStart = trimPathStart
        trim()
        onPathUpdated()
    }

    fun getTrimPathEnd(): Float {
        return trimPathEnd
    }

    fun setTrimPathEnd(trimPathEnd: Float) {
        this.trimPathEnd = trimPathEnd
        trim()
        onPathUpdated()
    }

    fun getTrimPathOffset(): Float {
        return trimPathOffset
    }

    fun setTrimPathOffset(trimPathOffset: Float) {
        this.trimPathOffset = trimPathOffset
        trim()
        onPathUpdated()
    }

    fun getStrokeLineCap(): Paint.Cap {
        return strokeLineCap
    }

    fun setStrokeLineCap(strokeLineCap: Paint.Cap) {
        this.strokeLineCap = strokeLineCap
        onPathUpdated()
    }

    fun getStrokeLineJoin(): Paint.Join {
        return strokeLineJoin
    }

    fun setStrokeLineJoin(strokeLineJoin: Paint.Join) {
        this.strokeLineJoin = strokeLineJoin
        onPathUpdated()
    }

    fun getStrokeMiterLimit(): Float {
        return strokeMiterLimit
    }

    fun setStrokeMiterLimit(strokeMiterLimit: Float) {
        this.strokeMiterLimit = strokeMiterLimit
        onPathUpdated()
    }

    fun draw(canvas: Canvas) {
        paint!!.color = applyAlpha(fillColor, fillAlpha)
        paint!!.style = Paint.Style.FILL
        canvas.drawPath(this, paint!!)
        paint!!.color = applyAlpha(strokeColor, strokeAlpha)
        paint!!.style = Paint.Style.STROKE
        canvas.drawPath(this, paint!!)
    }

    fun applyGroup(group: Group) {
        mapToMatrix(group.matrix())
        pivotX = group.getPivotX()
        pivotY = group.getPivotY()
    }

    fun mapToMatrix(matrix: Matrix) {
        matrices!!.add(matrix)
        transform(matrix)
        originalPath!!.transform(matrix)
        mapPoints(matrix)
        updateOriginalDimens()
    }

    private fun mapPoints(matrix: Matrix) {
        val src = floatArrayOf(pivotX, pivotY)
        matrix.mapPoints(src)
        pivotX = src[0]
        pivotY = src[1]
    }

    fun scaleStrokeWidth(scale: Float) {
        paint!!.strokeWidth = strokeWidth * scale
    }

    fun setPathData(pathData: String?) {
        setPathDataNodes(PathParserCompat.createNodesFromPathData(pathData))
    }

    fun getPathDataNodes(): Array<PathDataNode> {
        return pathDataNodes
    }

    fun setPathDataNodes(pathDataNodes: Array<PathDataNode>) {
        PathUtils.setPathDataNodes(this, pathDataNodes)
        this.pathDataNodes = pathDataNodes
        for (matrix in matrices!!) {
            transform(matrix)
        }
        onPathUpdated()
    }

    fun inflate(context: Context?, xpp: XmlResourceParser?) {
        val pathData: String = XmlParser.getAttributeString(context, xpp, "pathData", name)
        pathDataNodes = PathParserCompat.createNodesFromPathData(pathData)
        name = XmlParser.getAttributeString(context, xpp, "name", name)
        fillAlpha = XmlParser.getAttributeFloat(xpp, "fillAlpha", fillAlpha)
        fillColor = XmlParser.getAttributeColor(context, xpp, "fillColor", fillColor)
        strokeAlpha = XmlParser.getAttributeFloat(xpp, "strokeAlpha", strokeAlpha)
        strokeColor = XmlParser.getAttributeColor(context, xpp, "strokeColor", strokeColor)
        strokeLineCap = XmlParser.getAttributeStrokeLineCap(xpp, "strokeLineCap", strokeLineCap)
        strokeLineJoin = XmlParser.getAttributeStrokeLineJoin(xpp, "strokeLineJoin", strokeLineJoin)
        strokeMiterLimit = XmlParser.getAttributeFloat(xpp, "strokeMiterLimit", strokeMiterLimit)
        strokeWidth = XmlParser.getAttributeFloat(xpp, "strokeWidth", strokeWidth)
        trimPathStart = XmlParser.getAttributeFloat(xpp, "trimPathStart", trimPathStart)
        trimPathEnd = XmlParser.getAttributeFloat(xpp, "trimPathEnd", trimPathEnd)
        trimPathOffset = XmlParser.getAttributeFloat(xpp, "trimPathOffset", trimPathOffset)
        fillType = XmlParser.getAttributePathFillType(xpp, "fillType", fillType)
        updatePaint()
        trim()
    }

    fun inflate(
        pathData: String?,
        name: String?,
        fillAlpha: Float,
        fillColor: Int,
        strokeAlpha: Float,
        strokeColor: Int,
        strokeLineCap: Paint.Cap,
        strokeLineJoin: Paint.Join,
        strokeMiterLimit: Float,
        strokeWidth: Float,
        trimPathStart: Float,
        trimPathEnd: Float,
        trimPathOffset: Float,
        fillType: FillType?
    ) {
        pathDataNodes = PathParserCompat.createNodesFromPathData(pathData)
        this.name = name
        this.fillAlpha = fillAlpha
        this.fillColor = fillColor
        this.strokeAlpha = strokeAlpha
        this.strokeColor = strokeColor
        this.strokeLineCap = strokeLineCap
        this.strokeLineJoin = strokeLineJoin
        this.strokeMiterLimit = strokeMiterLimit
        this.strokeWidth = strokeWidth
        this.trimPathStart = trimPathStart
        this.trimPathEnd = trimPathEnd
        this.trimPathOffset = trimPathOffset
        setFillType(fillType!!)
        updatePaint()
        trim()
    }

    private fun updateOriginalDimens() {
        originalWidth = PathUtils.getPathWidth(this)
        originalHeight = PathUtils.getPathHeight(this)
    }

    private fun trim() {
        if (trimPathStart != 0.0f || trimPathEnd != 1.0f) {
            var start = (trimPathStart + trimPathOffset) % 1.0f
            var end = (trimPathEnd + trimPathOffset) % 1.0f
            if (pathMeasure == null) {
                pathMeasure = PathMeasure()
            }
            pathMeasure!!.setPath(originalPath, false)
            val len = pathMeasure!!.length
            start = start * len
            end = end * len
            reset()
            if (start > end) {
                pathMeasure!!.getSegment(start, len, this, true)
                pathMeasure!!.getSegment(0f, end, this, true)
            } else {
                pathMeasure!!.getSegment(start, end, this, true)
            }
            rLineTo(0f, 0f) // fix bug in measure
        }
    }

    private fun updatePaint() {
        paint!!.strokeCap = strokeLineCap
        paint!!.strokeJoin = strokeLineJoin
        paint!!.strokeMiter = strokeMiterLimit
        paint!!.strokeWidth = strokeWidth

        //todo fillType
    }

    private fun onPathUpdated() {
        if (onRichPathUpdatedListener != null) {
            onRichPathUpdatedListener.onPathUpdated()
        }
    }

    private fun applyAlpha(color: Int, alpha: Float): Int {
        var color = color
        val alphaBytes = Color.alpha(color)
        color = color and 0x00FFFFFF
        color = color or ((alphaBytes * alpha).toInt() shl 24)
        return color
    }

    interface OnPathClickListener {
        fun onClick(richPath: RichPath?)
    }

    companion object {
        const val TAG_NAME = "path"
    }
}