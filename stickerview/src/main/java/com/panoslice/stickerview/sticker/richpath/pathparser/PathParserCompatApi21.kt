package com.panoslice.stickerview.sticker.richpath.pathparser

import android.graphics.Path
import com.panoslice.stickerview.sticker.richpath.pathparser.PathParserCompat
import com.panoslice.stickerview.sticker.richpath.pathparser.PathParserCompatApi21
import androidx.core.graphics.PathParser
import java.lang.Exception
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * Created by tarek on 6/27/17.
 */
internal object PathParserCompatApi21 {
    /**
     * @param pathData The string representing a path, the same as "d" string in svg file.
     * @return the generated Path object.
     */
    fun createPathFromPathData(pathData: String?): Path {
        try {
            val method = createPathFromPathDataMethod
            if (method != null) {
                val `object` = method.invoke(null, pathData)
                return `object` as Path
            }
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
        return PathParser.createPathFromPathData(pathData)
    }

    private val createPathFromPathDataMethod: Method?
        private get() {
            try {
                return Class.forName("android.util.PathParser")
                    .getDeclaredMethod("createPathFromPathData", String::class.java)
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
}