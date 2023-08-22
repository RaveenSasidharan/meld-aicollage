package com.panoslice.stickerview.sticker.richpath.model

import android.content.Context
import android.content.res.XmlResourceParser
import com.xiaopo.flying.sticker.richpath.RichPath
import com.xiaopo.flying.sticker.richpath.util.XmlParser
import java.util.ArrayList

/**
 * Created by tarek on 6/30/17.
 */
class Vector {
    var name: String? = null
    private var tint = 0
    var height = 0f
        private set
    var width = 0f
        private set
    private var alpha = 0f
    private var autoMirrored = false

    //TODO private PorterDuff.Mode tintMode = PorterDuff.Mode.SRC_IN;
    var viewportWidth = 0f
        private set
    var viewportHeight = 0f
        private set
    var currentWidth = 0f
    var currentHeight = 0f
    var paths: List<RichPath> = ArrayList<RichPath>()
    fun inflate(xpp: XmlResourceParser?, context: Context?) {
        name = XmlParser.getAttributeString(context, xpp, "name", name)
        tint = XmlParser.getAttributeColor(context, xpp, "tint", tint)
        width = XmlParser.getAttributeDimen(context, xpp, "width", width)
        height = XmlParser.getAttributeDimen(context, xpp, "height", height)
        alpha = XmlParser.getAttributeFloat(xpp, "alpha", alpha)
        autoMirrored = XmlParser.getAttributeBoolean(xpp, "autoMirrored", autoMirrored)
        viewportWidth = XmlParser.getAttributeFloat(xpp, "viewportWidth", viewportWidth)
        viewportHeight = XmlParser.getAttributeFloat(xpp, "viewportHeight", viewportHeight)
        currentWidth = viewportWidth
        currentHeight = viewportHeight

        //TODO tintMode = XmlParser.getAttributeFloat(xpp, "tintMode", tintMode);
    }

    fun inflate(height: Float, width: Float, viewportHeight: Float, viewportWidth: Float) {
        name = null
        this.width = width
        this.height = height
        alpha = 1f
        this.viewportHeight = viewportHeight
        this.viewportWidth = viewportWidth
        currentHeight = viewportHeight
        currentWidth = viewportWidth
    }

    companion object {
        const val TAG_NAME = "vector"
    }
}