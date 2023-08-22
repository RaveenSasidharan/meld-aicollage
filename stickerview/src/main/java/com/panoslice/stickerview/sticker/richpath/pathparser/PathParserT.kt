package com.panoslice.stickerview.sticker.richpath.pathparser

import android.graphics.Path
import android.os.Build
import com.panoslice.stickerview.sticker.richpath.pathparser.PathParserCompat
import com.panoslice.stickerview.sticker.richpath.pathparser.PathParserCompatApi21
import androidx.core.graphics.PathParser

/**
 * Created by tarek on 6/27/17.
 */
object PathParserT {
    /**
     * @param pathData The string representing a path, the same as "d" string in svg file.
     * @return the generated Path object.
     */
    fun createPathFromPathData(pathData: String): Path? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            PathParserCompatApi21.createPathFromPathData(pathData)
        } else {
            PathParserCompat.createPathFromPathData(pathData)
        }
    }

    fun createPathFromPathData(path: Path, pathData: String) {
        PathParserCompat.createPathFromPathData(path, pathData)
    }
}