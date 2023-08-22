/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.panoslice.stickerview.sticker.richpath.pathparser

import android.graphics.Path
import com.panoslice.stickerview.sticker.richpath.pathparser.PathParserCompat
import com.panoslice.stickerview.sticker.richpath.pathparser.PathParserCompatApi21
import androidx.core.graphics.PathParser
import java.lang.NumberFormatException
import java.lang.RuntimeException
import java.util.ArrayList

// This class is a duplicate from the PathParserCompat.java of frameworks/base, with slight
// update on incompatible API like copyOfRange().
object PathParserCompat {
    // Copy from Arrays.copyOfRange() which is only available from API level 9.
    /**
     * Copies elements from `original` into a new array, from indexes start (inclusive) to
     * end (exclusive). The original order of elements is preserved.
     * If `end` is greater than `original.length`, the result is padded
     * with the value `0.0f`.
     *
     * @param original the original array
     * @param start    the start index, inclusive
     * @param end      the end index, exclusive
     * @return the new array
     * @throws ArrayIndexOutOfBoundsException if `start < 0 || start > original.length`
     * @throws IllegalArgumentException       if `start > end`
     * @throws NullPointerException           if `original == null`
     */
    fun copyOfRange(original: FloatArray, start: Int, end: Int): FloatArray {
        require(start <= end)
        val originalLength = original.size
        if (start < 0 || start > originalLength) {
            throw ArrayIndexOutOfBoundsException()
        }
        val resultLength = end - start
        val copyLength = Math.min(resultLength, originalLength - start)
        val result = FloatArray(resultLength)
        System.arraycopy(original, start, result, 0, copyLength)
        return result
    }

    /**
     * @param pathData The string representing a path, the same as "d" string in svg file.
     * @return the generated Path object.
     */
    fun createPathFromPathData(pathData: String): Path {
        val path = Path()
        createPathFromPathData(path, pathData)
        return path
    }

    fun createPathFromPathData(path: Path, pathData: String) {
        val nodes = createNodesFromPathData(pathData)
        if (nodes != null) {
            try {
                PathDataNode.Companion.nodesToPath(nodes, path)
            } catch (e: RuntimeException) {
                throw RuntimeException("Error in parsing $pathData", e)
            }
        }
    }

    /**
     * @param pathData The string representing a path, the same as "d" string in svg file.
     * @return an array of the PathDataNode.
     */
    fun createNodesFromPathData(pathData: String?): Array<PathDataNode>? {
        if (pathData == null) {
            return null
        }
        var start = 0
        var end = 1
        val list = ArrayList<PathDataNode>()
        while (end < pathData.length) {
            end = nextStart(pathData, end)
            val s = pathData.substring(start, end).trim { it <= ' ' }
            if (s.length > 0) {
                val `val` = getFloats(s)
                addNode(list, s[0], `val`)
            }
            start = end
            end++
        }
        if (end - start == 1 && start < pathData.length) {
            addNode(list, pathData[start], FloatArray(0))
        }
        return list.toTypedArray()
    }

    /**
     * @param source The array of PathDataNode to be duplicated.
     * @return a deep copy of the `source`.
     */
    fun deepCopyNodes(source: Array<PathDataNode>?): Array<PathDataNode?>? {
        if (source == null) {
            return null
        }
        val copy = arrayOfNulls<PathDataNode>(source.size)
        for (i in source.indices) {
            copy[i] = PathDataNode(source[i])
        }
        return copy
    }

    /**
     * @param nodesFrom The source path represented in an array of PathDataNode
     * @param nodesTo   The target path represented in an array of PathDataNode
     * @return whether the `nodesFrom` can morph into `nodesTo`
     */
    fun canMorph(nodesFrom: Array<PathDataNode>?, nodesTo: Array<PathDataNode>?): Boolean {
        if (nodesFrom == null || nodesTo == null) {
            return false
        }
        if (nodesFrom.size != nodesTo.size) {
            return false
        }
        for (i in nodesFrom.indices) {
            if (nodesFrom[i].type != nodesTo[i].type
                || nodesFrom[i].params.size != nodesTo[i].params.size
            ) {
                return false
            }
        }
        return true
    }

    /**
     * @param nodes paths represented in an array of an array of PathDataNode
     * @return whether the `nodesFrom` can morph into `nodesTo`
     */
    fun canMorph(vararg nodes: Array<PathDataNode>): Boolean {
        for (pathDataNode in nodes) {
            if (pathDataNode == null) {
                return false
            }
        }
        for (i in 0 until nodes.size - 1) {
            if (nodes[i].size != nodes[i + 1].size) {
                return false
            }
        }
        for (i in 0 until nodes.size - 1) {
            for (j in nodes[i].indices) {
                if (nodes[i][j].type != nodes[i + 1][j].type
                    || nodes[i][j].params.size != nodes[i + 1][j].params.size
                ) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * Update the target's data to match the source.
     * Before calling this, make sure canMorph(target, source) is true.
     *
     * @param target The target path represented in an array of PathDataNode
     * @param source The source path represented in an array of PathDataNode
     */
    fun updateNodes(target: Array<PathDataNode>, source: Array<PathDataNode>) {
        for (i in source.indices) {
            target[i].type = source[i].type
            for (j in source[i].params.indices) {
                target[i].params[j] = source[i].params[j]
            }
        }
    }

    private fun nextStart(s: String, end: Int): Int {
        var end = end
        var c: Char
        while (end < s.length) {
            c = s[end]
            // Note that 'e' or 'E' are not valid path commands, but could be
            // used for floating point numbers' scientific notation.
            // Therefore, when searching for next command, we should ignore 'e'
            // and 'E'.
            if (((c - 'A') * (c - 'Z') <= 0 || (c - 'a') * (c - 'z') <= 0)
                && c != 'e' && c != 'E'
            ) {
                return end
            }
            end++
        }
        return end
    }

    private fun addNode(list: ArrayList<PathDataNode>, cmd: Char, `val`: FloatArray) {
        list.add(PathDataNode(cmd, `val`))
    }

    /**
     * Parse the floats in the string.
     * This is an optimized version of parseFloat(s.split(",|\\s"));
     *
     * @param s the string containing a command and list of floats
     * @return array of floats
     */
    private fun getFloats(s: String): FloatArray {
        return if (s[0] == 'z' or s[0] == 'Z') {
            FloatArray(0)
        } else try {
            val results = FloatArray(s.length)
            var count = 0
            var startPosition = 1
            var endPosition = 0
            val result = ExtractFloatResult()
            val totalLength = s.length

            // The startPosition should always be the first character of the
            // current number, and endPosition is the character after the current
            // number.
            while (startPosition < totalLength) {
                extract(s, startPosition, result)
                endPosition = result.mEndPosition
                if (startPosition < endPosition) {
                    results[count++] =
                        s.substring(startPosition, endPosition).toFloat()
                }
                startPosition = if (result.mEndWithNegOrDot) {
                    // Keep the '-' or '.' sign with next number.
                    endPosition
                } else {
                    endPosition + 1
                }
            }
            copyOfRange(results, 0, count)
        } catch (e: NumberFormatException) {
            throw RuntimeException("error in parsing \"$s\"", e)
        }
    }

    /**
     * Calculate the position of the next comma or space or negative sign
     *
     * @param s      the string to search
     * @param start  the position to start searching
     * @param result the result of the extraction, including the position of the
     * the starting position of next number, whether it is ending with a '-'.
     */
    private fun extract(s: String, start: Int, result: ExtractFloatResult) {
        // Now looking for ' ', ',', '.' or '-' from the start.
        var currentIndex = start
        var foundSeparator = false
        result.mEndWithNegOrDot = false
        var secondDot = false
        var isExponential = false
        while (currentIndex < s.length) {
            val isPrevExponential = isExponential
            isExponential = false
            val currentChar = s[currentIndex]
            when (currentChar) {
                ' ', ',' -> foundSeparator = true
                '-' ->                     // The negative sign following a 'e' or 'E' is not a separator.
                    if (currentIndex != start && !isPrevExponential) {
                        foundSeparator = true
                        result.mEndWithNegOrDot = true
                    }
                '.' -> if (!secondDot) {
                    secondDot = true
                } else {
                    // This is the second dot, and it is considered as a separator.
                    foundSeparator = true
                    result.mEndWithNegOrDot = true
                }
                'e', 'E' -> isExponential = true
            }
            if (foundSeparator) {
                break
            }
            currentIndex++
        }
        // When there is nothing found, then we put the end position to the end
        // of the string.
        result.mEndPosition = currentIndex
    }

    private class ExtractFloatResult internal constructor() {
        // We need to return the position of the next separator and whether the
        // next float starts with a '-' or a '.'.
        var mEndPosition = 0
        var mEndWithNegOrDot = false
    }
}