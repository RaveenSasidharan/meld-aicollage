package com.panoslice.stickerview.sticker.richpath.util

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import com.panoslice.stickerview.sticker.richpath.util.XmlParser
import androidx.core.content.ContextCompat

/**
 * Created by tarek360 on 7/1/17.
 */
object Utils {
    fun dpToPixel(context: Context, dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }

    fun getDimenFromString(value: String?): Float {
        val end = if (value!![value.length - 3] == 'd') 3 else 2
        return value.substring(0, value.length - end).toFloat()
    }

    fun getColorFromString(value: String): Int {
        var color = Color.TRANSPARENT
        if (value.length == 7 || value.length == 9) {
            color = Color.parseColor(value)
        } else if (value.length == 4) {
            color = Color.parseColor(
                "#"
                        + value[1]
                        + value[1]
                        + value[2]
                        + value[2]
                        + value[3]
                        + value[3]
            )
        } else if (value.length == 2) {
            color = Color.parseColor(
                "#"
                        + value[1]
                        + value[1]
                        + value[1]
                        + value[1]
                        + value[1]
                        + value[1]
                        + value[1]
                        + value[1]
            )
        }
        return color
    }

    fun convertSpToPx(context: Context, scaledPixels: Float): Float {
        return scaledPixels * context.resources.displayMetrics.scaledDensity
    }

    fun dpToPx(dp: Float): Int {
        val density = Resources.getSystem().displayMetrics.density
        return Math.round(dp * density)
    }
}