package com.panoslice.stickerview.sticker.richpath

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import com.xiaopo.flying.sticker.richpath.model.Group
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.*
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class RichPathParser {
    var counter = 0
    fun retrunRichpathDrawableFromServer(context: Context?, url: String?): RichPathDrawable? {
        return try {
            Log.v("APP", "Downloading File")
            val output = StringBuffer("")
            var stream: InputStream? = null
            val conn = URL(url).openConnection()
            val httpURLConnection = conn as HttpURLConnection
            httpURLConnection.requestMethod = "GET"
            httpURLConnection.connect()
            if (httpURLConnection.responseCode == HttpURLConnection.HTTP_OK) {
                stream = httpURLConnection.inputStream
                val buffer = BufferedReader(
                    InputStreamReader(stream)
                )
                var s: String? = ""
                while (buffer.readLine().also { s = it } != null) output.append(s)
            }
            val inputString = output.toString()
            val sample = StringBuilder()
            // Use the URL passed in the AysncClass and return an InputStream to be used in onPostExecute
            val xmlFactoryObject = XmlPullParserFactory.newInstance()
            val myParser = xmlFactoryObject.newPullParser()
            val inputStream: InputStream = ByteArrayInputStream(inputString.toByteArray())
            val vector = Vector()
            myParser.setInput(inputStream, "UTF-8")
            val groupStack: Stack<Group> = Stack<Group>()
            var event = myParser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                val tagName = myParser.name
                when (event) {
                    XmlPullParser.START_TAG -> {
                        if (tagName == Vector.TAG_NAME) {
                            val width = getAttributeDimen(
                                myParser.getAttributeValue(null, "android:width"),
                                480.0f
                            )
                            val height = getAttributeDimen(
                                myParser.getAttributeValue(
                                    null,
                                    "android:height"
                                ), 480.0f
                            )
                            val viewportWidth = getAttributeFloat(
                                myParser.getAttributeValue(
                                    null,
                                    "android:viewportWidth"
                                ), 480.0f
                            )
                            val viewportHeight = getAttributeFloat(
                                myParser.getAttributeValue(
                                    null,
                                    "android:viewportHeight"
                                ), 480.0f
                            )
                            vector.inflate(height, width, viewportHeight, viewportWidth)
                        }
                        if (tagName == Group.TAG_NAME) {
                            val name =
                                getAttributeName(myParser.getAttributeValue(null, "android:name"))
                            val rotation =
                                myParser.getAttributeValue(null, "android:rotation").toFloat()
                            val pivotX =
                                myParser.getAttributeValue(null, "android:pivotX").toFloat()
                            val pivotY =
                                myParser.getAttributeValue(null, "android:pivotY").toFloat()
                            val scaleX =
                                myParser.getAttributeValue(null, "android:scaleX").toFloat()
                            val scaleY =
                                myParser.getAttributeValue(null, "android:scaleY").toFloat()
                            val translateX =
                                myParser.getAttributeValue(null, "android:translateX").toFloat()
                            val translateY =
                                myParser.getAttributeValue(null, "android:translateY").toFloat()
                            val group = Group(
                                name,
                                rotation,
                                pivotX,
                                pivotY,
                                scaleX,
                                scaleY,
                                translateX,
                                translateY
                            )
                            if (!groupStack.empty()) {
                                group.scale(groupStack.peek().matrix())
                            }
                            groupStack.push(group)
                        }
                        if (tagName == RichPath.Companion.TAG_NAME) {
                            val pathData = myParser.getAttributeValue(null, "android:pathData")
                            val path = RichPath(pathData)
                            val name = myParser.getAttributeValue(null, "android:name")
                            val fillAlpha = getAttributeFloat(
                                myParser.getAttributeValue(
                                    null,
                                    "android:fillAlpha"
                                ), 1.0f
                            )
                            val fillColor = getAttributeColor(
                                myParser.getAttributeValue(
                                    null,
                                    "android:fillColor"
                                )
                            )
                            val strokeAlpha = getAttributeFloat(
                                myParser.getAttributeValue(
                                    null,
                                    "android:strokeAlpha"
                                ), 1.0f
                            )
                            val strokeColor = getAttributeColor(
                                myParser.getAttributeValue(
                                    null,
                                    "android:strokeColor"
                                )
                            )
                            val strokeLineCap = Paint.Cap.BUTT
                            val strokeLineJoin = Paint.Join.MITER
                            val strokeMiterLimit = getAttributeFloat(
                                myParser.getAttributeValue(
                                    null,
                                    "android:strokeMeterLimit"
                                ), 4f
                            )
                            val strokeWidth = getAttributeFloat(
                                myParser.getAttributeValue(
                                    null,
                                    "android:strokeWidth"
                                ), 0f
                            )
                            val trimPathStart = getAttributeFloat(
                                myParser.getAttributeValue(
                                    null,
                                    "android:trimPathStart"
                                ), 0f
                            )
                            val trimPathEnd = getAttributeFloat(
                                myParser.getAttributeValue(
                                    null,
                                    "android:trimPathEnd"
                                ), 1f
                            )
                            val trimPathOffset = getAttributeFloat(
                                myParser.getAttributeValue(
                                    null,
                                    "android:trimPathOffset"
                                ), 0f
                            )
                            val fillType = getAttributePathFillType(
                                myParser.getAttributeValue(
                                    null,
                                    "android:fillType"
                                ), Path.FillType.WINDING
                            )
                            path.inflate(
                                pathData,
                                name,
                                fillAlpha,
                                fillColor,
                                strokeAlpha,
                                strokeColor,
                                strokeLineCap,
                                strokeLineJoin,
                                strokeMiterLimit,
                                strokeWidth,
                                trimPathStart,
                                trimPathEnd,
                                trimPathOffset,
                                fillType
                            )
                            if (!groupStack.empty()) {
                                path.applyGroup(groupStack.peek())
                            }
                            vector.paths.add(path)
                        }
                    }
                    XmlPullParser.END_TAG -> if (Group.TAG_NAME.equals(tagName)) {
                        if (!groupStack.empty()) {
                            groupStack.pop()
                        }
                    }
                }
                event = myParser.next()
            }
            val alienRichPathDrawable: RichPathDrawable
            val alienScaleType = ImageView.ScaleType.CENTER
            alienRichPathDrawable = RichPathDrawable(vector, alienScaleType)
            inputStream.close()
            alienRichPathDrawable
        } catch (e: IOException) {
            Log.e("localS", "es" + e.message)
            null
        } catch (e: XmlPullParserException) {
            Log.e("localS", "es" + e.message)
            null
        }
    }

    fun getRichPathDrawableFromUri(context: Context, uri: Uri?): RichPathDrawable? {
        return try {
            // Use the URL passed in the AysncClass and return an InputStream to be used in onPostExecute
            val xmlFactoryObject = XmlPullParserFactory.newInstance()
            val myParser = xmlFactoryObject.newPullParser()
            val inputStream = context.contentResolver.openInputStream(
                uri!!
            )
            val vector = Vector()
            myParser.setInput(inputStream, "UTF-8")
            val groupStack: Stack<Group> = Stack<Group>()
            var event = myParser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                val tagName = myParser.name
                when (event) {
                    XmlPullParser.START_TAG -> {
                        if (tagName == Vector.TAG_NAME) {
                            val width = getAttributeDimen(
                                myParser.getAttributeValue(null, "android:width"),
                                480.0f
                            )
                            val height = getAttributeDimen(
                                myParser.getAttributeValue(
                                    null,
                                    "android:height"
                                ), 480.0f
                            )
                            val viewportWidth = getAttributeFloat(
                                myParser.getAttributeValue(
                                    null,
                                    "android:viewportWidth"
                                ), 480.0f
                            )
                            val viewportHeight = getAttributeFloat(
                                myParser.getAttributeValue(
                                    null,
                                    "android:viewportHeight"
                                ), 480.0f
                            )
                            vector.inflate(height, width, viewportHeight, viewportWidth)
                        }
                        if (tagName == Group.TAG_NAME) {
                            val name =
                                getAttributeName(myParser.getAttributeValue(null, "android:name"))
                            val rotation =
                                myParser.getAttributeValue(null, "android:rotation").toFloat()
                            val pivotX =
                                myParser.getAttributeValue(null, "android:pivotX").toFloat()
                            val pivotY =
                                myParser.getAttributeValue(null, "android:pivotY").toFloat()
                            val scaleX =
                                myParser.getAttributeValue(null, "android:scaleX").toFloat()
                            val scaleY =
                                myParser.getAttributeValue(null, "android:scaleY").toFloat()
                            val translateX =
                                myParser.getAttributeValue(null, "android:translateX").toFloat()
                            val translateY =
                                myParser.getAttributeValue(null, "android:translateY").toFloat()
                            val group = Group(
                                name,
                                rotation,
                                pivotX,
                                pivotY,
                                scaleX,
                                scaleY,
                                translateX,
                                translateY
                            )
                            if (!groupStack.empty()) {
                                group.scale(groupStack.peek().matrix())
                            }
                            groupStack.push(group)
                        }
                        if (tagName == RichPath.Companion.TAG_NAME) {
                            val pathData = myParser.getAttributeValue(null, "android:pathData")
                            val path = RichPath(pathData)
                            val name = myParser.getAttributeValue(null, "android:name")
                            val fillAlpha = getAttributeFloat(
                                myParser.getAttributeValue(
                                    null,
                                    "android:fillAlpha"
                                ), 1.0f
                            )
                            val fillColor = getAttributeColor(
                                myParser.getAttributeValue(
                                    null,
                                    "android:fillColor"
                                )
                            )
                            val strokeAlpha = getAttributeFloat(
                                myParser.getAttributeValue(
                                    null,
                                    "android:strokeAlpha"
                                ), 1.0f
                            )
                            val strokeColor = getAttributeColor(
                                myParser.getAttributeValue(
                                    null,
                                    "android:strokeColor"
                                )
                            )
                            val strokeLineCap = Paint.Cap.BUTT
                            val strokeLineJoin = Paint.Join.MITER
                            val strokeMiterLimit = getAttributeFloat(
                                myParser.getAttributeValue(
                                    null,
                                    "android:strokeMeterLimit"
                                ), 4f
                            )
                            val strokeWidth = getAttributeFloat(
                                myParser.getAttributeValue(
                                    null,
                                    "android:strokeWidth"
                                ), 0f
                            )
                            val trimPathStart = getAttributeFloat(
                                myParser.getAttributeValue(
                                    null,
                                    "android:trimPathStart"
                                ), 0f
                            )
                            val trimPathEnd = getAttributeFloat(
                                myParser.getAttributeValue(
                                    null,
                                    "android:trimPathEnd"
                                ), 1f
                            )
                            val trimPathOffset = getAttributeFloat(
                                myParser.getAttributeValue(
                                    null,
                                    "android:trimPathOffset"
                                ), 0f
                            )
                            val fillType = getAttributePathFillType(
                                myParser.getAttributeValue(
                                    null,
                                    "android:fillType"
                                ), Path.FillType.WINDING
                            )
                            path.inflate(
                                pathData,
                                name,
                                fillAlpha,
                                fillColor,
                                strokeAlpha,
                                strokeColor,
                                strokeLineCap,
                                strokeLineJoin,
                                strokeMiterLimit,
                                strokeWidth,
                                trimPathStart,
                                trimPathEnd,
                                trimPathOffset,
                                fillType
                            )
                            if (!groupStack.empty()) {
                                path.applyGroup(groupStack.peek())
                            }
                            vector.paths.add(path)
                        }
                    }
                    XmlPullParser.END_TAG -> if (Group.TAG_NAME.equals(tagName)) {
                        if (!groupStack.empty()) {
                            groupStack.pop()
                        }
                    }
                }
                event = myParser.next()
            }
            val alienRichPathDrawable: RichPathDrawable
            val alienScaleType = ImageView.ScaleType.CENTER
            alienRichPathDrawable = RichPathDrawable(vector, alienScaleType)
            inputStream!!.close()
            alienRichPathDrawable
        } catch (e: IOException) {
            null
        } catch (e: XmlPullParserException) {
            null
        }
    }

    private fun getAttributePathFillType(value: String?, defValue: Path.FillType): Path.FillType {
        return if (value != null) getPathFillType(value.toInt(), defValue) else defValue
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

    private fun getAttributeDimen(value: String?, defValue: Float): Float {
        val dp: Float = Utils.dpToPx(Utils.getDimenFromString(value))
        return if (value != null) dp else defValue
    }

    private fun getAttributeFloat(value: String?, defValue: Float): Float {
        return value?.toFloat() ?: defValue
    }

    private fun getAttributeColor(value: String?): Int {
        if (value != null && value == "null") {
            return Color.TRANSPARENT
        }
        return if (value != null) Utils.getColorFromString(value) else Color.TRANSPARENT
    }

    private fun getAttributeName(value: String?): String {
        counter += 1
        return value ?: counter.toString()
    }
}