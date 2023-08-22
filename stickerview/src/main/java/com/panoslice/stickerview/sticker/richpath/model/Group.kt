package com.panoslice.stickerview.sticker.richpath.model

import android.content.Context
import android.content.res.XmlResourceParser
import android.graphics.Matrix
import com.xiaopo.flying.sticker.richpath.util.XmlParser

/**
 * Created by tarek on 6/30/17.
 */
class Group {
    var rotation = 0f
        private set
    var pivotX = 0f
        private set
    var pivotY = 0f
        private set
    var scaleX = 1f
        private set
    var scaleY = 1f
        private set
    var translateX = 0f
        private set
    var translateY = 0f
        private set
    var name: String? = null
        private set
    private var matrix: Matrix? = null

    constructor(
        name: String,
        rotation: Float,
        pivotX: Float,
        pivotY: Float,
        scaleX: Float,
        scaleY: Float,
        translateX: Float,
        translateY: Float
    ) {
        inflate(name, rotation, pivotX, pivotY, scaleX, scaleY, translateX, translateY)
    }

    constructor(context: Context, xpp: XmlResourceParser) {
        inflate(context, xpp)
    }

    private fun inflate(
        name: String,
        rotation: Float,
        pivotX: Float,
        pivotY: Float,
        scaleX: Float,
        scaleY: Float,
        translateX: Float,
        translateY: Float
    ) {
        this.name = name
        this.rotation = rotation
        this.pivotX = pivotX
        this.pivotY = pivotY
        this.scaleX = scaleX
        this.scaleY = scaleY
        this.translateX = translateX
        this.translateY = translateY
        matrix()
    }

    private fun inflate(context: Context, xpp: XmlResourceParser) {
        name = XmlParser.getAttributeString(context, xpp, "name", name)
        rotation = XmlParser.getAttributeFloat(xpp, "rotation", rotation)
        scaleX = XmlParser.getAttributeFloat(xpp, "scaleX", scaleX)
        scaleY = XmlParser.getAttributeFloat(xpp, "scaleY", scaleY)
        translateX = XmlParser.getAttributeFloat(xpp, "translateX", translateX)
        translateY = XmlParser.getAttributeFloat(xpp, "translateY", translateY)
        pivotX = XmlParser.getAttributeFloat(xpp, "pivotX", pivotX) + translateX
        pivotY = XmlParser.getAttributeFloat(xpp, "pivotY", pivotY) + translateY
        matrix()
    }

    fun matrix(): Matrix {
        if (matrix == null) {
            matrix = Matrix()
            matrix!!.postTranslate(-pivotX, -pivotY)
            matrix!!.postScale(scaleX, scaleY)
            matrix!!.postRotate(rotation, 0f, 0f)
            matrix!!.postTranslate(translateX + pivotX, translateY + pivotY)
        }
        return matrix!!
    }

    fun scale(matrix: Matrix?) {
        matrix().postConcat(matrix)
    }

    companion object {
        const val TAG_NAME = "group"
    }
}