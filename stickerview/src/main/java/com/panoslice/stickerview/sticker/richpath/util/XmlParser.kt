package com.panoslice.stickerview.sticker.richpath.util

import android.content.Context
import android.content.res.XmlResourceParser
import android.graphics.Paint
import android.graphics.Path
import com.xiaopo.flying.sticker.richpath.RichPath
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.util.*

/**
 * Created by tarek on 6/30/17.
 */
object XmlParser {
    private const val NAMESPACE = "http://schemas.android.com/apk/res/android"
    @Throws(IOException::class, XmlPullParserException::class)
    fun parseVector(vector: Vector, xpp: XmlResourceParser, context: Context) {
        val groupStack: Stack<Group> = Stack<Group>()
        var eventType = xpp.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = xpp.name
            if (eventType == XmlPullParser.START_TAG) {
                when (tagName) {
                    Vector.TAG_NAME -> parseVectorElement(vector, xpp, context)
                    Group.TAG_NAME -> {
                        val group: Group = parseGroupElement(context, xpp)
                        if (!groupStack.empty()) {
                            group.scale(groupStack.peek().matrix())
                        }
                        groupStack.push(group)
                    }
                    RichPath.TAG_NAME -> {
                        val path: RichPath = parsePathElement(context, xpp)
                        if (!groupStack.empty()) {
                            path.applyGroup(groupStack.peek())
                        }
                        vector.paths.add(path)
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (Group.TAG_NAME.equals(tagName)) {
                    if (!groupStack.empty()) {
                        groupStack.pop()
                    }
                }
            }
            eventType = xpp.next()
        }
        xpp.close()
    }

    private fun parseVectorElement(vector: Vector, xpp: XmlResourceParser, context: Context) {
        vector.inflate(xpp, context)
    }

    private fun parseGroupElement(context: Context, xpp: XmlResourceParser): Group {
        return Group(context, xpp)
    }

    private fun parsePathElement(context: Context, xpp: XmlResourceParser): RichPath {
        val pathData = getAttributeString(context, xpp, "pathData", null)
        val path = RichPath(pathData)
        path.inflate(context, xpp)
        return path
    }

    fun getAttributeString(
        context: Context,
        xpp: XmlResourceParser,
        attributeName: String,
        defValue: String?
    ): String {
        val resourceId = getAttributeResourceValue(xpp, attributeName)
        val value: String?
        value = if (resourceId != -1) {
            context.getString(resourceId)
        } else {
            getAttributeValue(xpp, attributeName)
        }
        return value ?: defValue!!
    }

    fun getAttributeFloat(xpp: XmlResourceParser, attributeName: String, defValue: Float): Float {
        val value = getAttributeValue(xpp, attributeName)
        return value?.toFloat() ?: defValue
    }

    fun getAttributeDimen(
        context: Context,
        xpp: XmlResourceParser,
        attributeName: String,
        defValue: Float
    ): Float {
        val value = getAttributeValue(xpp, attributeName)
        val dp = Utils.dpToPixel(context, Utils.getDimenFromString(value))
        return if (value != null) dp else defValue
    }

    fun getAttributeBoolean(
        xpp: XmlResourceParser,
        attributeName: String,
        defValue: Boolean
    ): Boolean {
        val value = getAttributeValue(xpp, attributeName)
        return if (value != null) java.lang.Boolean.parseBoolean(value) else defValue
    }

    fun getAttributeInt(xpp: XmlResourceParser, attributeName: String, defValue: Int): Int {
        val value = getAttributeValue(xpp, attributeName)
        return value?.toInt() ?: defValue
    }

    fun getAttributeColor(
        context: Context?,
        xpp: XmlResourceParser,
        attributeName: String,
        defValue: Int
    ): Int {
        val resourceId = getAttributeResourceValue(xpp, attributeName)
        if (resourceId != -1) {
            return ContextCompat.getColor(context, resourceId)
        }
        val value = getAttributeValue(xpp, attributeName)
        return if (value != null) Utils.getColorFromString(value) else defValue
    }

    fun getAttributeStrokeLineCap(
        xpp: XmlResourceParser,
        attributeName: String,
        defValue: Paint.Cap
    ): Paint.Cap {
        val value = getAttributeValue(xpp, attributeName)
        return if (value != null) getStrokeLineCap(value.toInt(), defValue) else defValue
    }

    fun getAttributeStrokeLineJoin(
        xpp: XmlResourceParser,
        attributeName: String,
        defValue: Paint.Join
    ): Paint.Join {
        val value = getAttributeValue(xpp, attributeName)
        return if (value != null) getStrokeLineJoin(value.toInt(), defValue) else defValue
    }

    fun getAttributePathFillType(
        xpp: XmlResourceParser,
        attributeName: String,
        defValue: Path.FillType
    ): Path.FillType {
        val value = getAttributeValue(xpp, attributeName)
        return if (value != null) getPathFillType(value.toInt(), defValue) else defValue
    }

    private fun getAttributeValue(xpp: XmlResourceParser, attributeName: String): String? {
        return xpp.getAttributeValue(NAMESPACE, attributeName)
    }

    private fun getAttributeResourceValue(xpp: XmlResourceParser, attributeName: String): Int {
        return xpp.getAttributeResourceValue(NAMESPACE, attributeName, -1)
    }

    private fun getStrokeLineCap(id: Int, defValue: Paint.Cap): Paint.Cap {
        return when (id) {
            0 -> Paint.Cap.BUTT
            1 -> Paint.Cap.ROUND
            2 -> Paint.Cap.SQUARE
            else -> defValue
        }
    }

    private fun getStrokeLineJoin(id: Int, defValue: Paint.Join): Paint.Join {
        return when (id) {
            0 -> Paint.Join.MITER
            1 -> Paint.Join.ROUND
            2 -> Paint.Join.BEVEL
            else -> defValue
        }
    }

    private fun getPathFillType(id: Int, defValue: Path.FillType): Path.FillType {
        return when (id) {
            0 -> Path.FillType.WINDING
            1 -> Path.FillType.EVEN_ODD
            2 -> Path.FillType.INVERSE_WINDING
            3 -> Path.FillType.INVERSE_EVEN_ODD
            else -> defValue
        }
    }
}