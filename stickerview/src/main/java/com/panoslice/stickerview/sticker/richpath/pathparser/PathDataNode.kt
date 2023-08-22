package com.panoslice.stickerview.sticker.richpath.pathparser

/**
 * Created by tarek on 7/16/17.
 */
import android.graphics.Path
import android.util.Log
import com.xiaopo.flying.sticker.richpath.pathparser.PathParserCompat.copyOfRange

/**
 * Each PathDataNode represents one command in the "d" attribute of the svg
 * file.
 * An array of PathDataNode can represent the whole "d" attribute.
 */
class PathDataNode {
    /*package*/
    var type: Char
    var params: FloatArray

    internal constructor(type: Char, params: FloatArray) {
        this.type = type
        this.params = params
    }

    internal constructor(n: PathDataNode) {
        type = n.type
        params = copyOfRange(n.params, 0, n.params.size)
    }

    /**
     * The current PathDataNode will be interpolated between the
     * `nodeFrom` and `nodeTo` according to the
     * `fraction`.
     *
     * @param nodeFrom The start value as a PathDataNode.
     * @param nodeTo   The end value as a PathDataNode
     * @param fraction The fraction to interpolate.
     */
    fun interpolatePathDataNode(
        nodeFrom: PathDataNode,
        nodeTo: PathDataNode, fraction: Float
    ) {
        for (i in nodeFrom.params.indices) {
            params[i] = (nodeFrom.params[i] * (1 - fraction)
                    + nodeTo.params[i] * fraction)
        }
    }

    companion object {
        private const val LOGTAG = "PathDataNode"

        /**
         * Convert an array of PathDataNode to Path.
         *
         * @param node The source array of PathDataNode.
         * @param path The target Path object.
         */
        fun nodesToPath(node: Array<PathDataNode>, path: Path) {
            val current = FloatArray(6)
            var previousCommand = 'm'
            for (i in node.indices) {
                addCommand(path, current, previousCommand, node[i].type, node[i].params)
                previousCommand = node[i].type
            }
        }

        private fun addCommand(
            path: Path, current: FloatArray,
            previousCmd: Char, cmd: Char, `val`: FloatArray
        ) {
            var previousCmd = previousCmd
            var incr = 2
            var currentX = current[0]
            var currentY = current[1]
            var ctrlPointX = current[2]
            var ctrlPointY = current[3]
            var currentSegmentStartX = current[4]
            var currentSegmentStartY = current[5]
            var reflectiveCtrlPointX: Float
            var reflectiveCtrlPointY: Float
            when (cmd) {
                'z', 'Z' -> {
                    path.close()
                    // Path is closed here, but we need to move the pen to the
                    // closed position. So we cache the segment's starting position,
                    // and restore it here.
                    currentX = currentSegmentStartX
                    currentY = currentSegmentStartY
                    ctrlPointX = currentSegmentStartX
                    ctrlPointY = currentSegmentStartY
                    path.moveTo(currentX, currentY)
                }
                'm', 'M', 'l', 'L', 't', 'T' -> incr = 2
                'h', 'H', 'v', 'V' -> incr = 1
                'c', 'C' -> incr = 6
                's', 'S', 'q', 'Q' -> incr = 4
                'a', 'A' -> incr = 7
            }
            var k = 0
            while (k < `val`.size) {
                when (cmd) {
                    'm' -> {
                        currentX += `val`[k + 0]
                        currentY += `val`[k + 1]
                        if (k > 0) {
                            // According to the spec, if a moveto is followed by multiple
                            // pairs of coordinates, the subsequent pairs are treated as
                            // implicit lineto commands.
                            path.rLineTo(`val`[k + 0], `val`[k + 1])
                        } else {
                            path.rMoveTo(`val`[k + 0], `val`[k + 1])
                            currentSegmentStartX = currentX
                            currentSegmentStartY = currentY
                        }
                    }
                    'M' -> {
                        currentX = `val`[k + 0]
                        currentY = `val`[k + 1]
                        if (k > 0) {
                            // According to the spec, if a moveto is followed by multiple
                            // pairs of coordinates, the subsequent pairs are treated as
                            // implicit lineto commands.
                            path.lineTo(`val`[k + 0], `val`[k + 1])
                        } else {
                            path.moveTo(`val`[k + 0], `val`[k + 1])
                            currentSegmentStartX = currentX
                            currentSegmentStartY = currentY
                        }
                    }
                    'l' -> {
                        path.rLineTo(`val`[k + 0], `val`[k + 1])
                        currentX += `val`[k + 0]
                        currentY += `val`[k + 1]
                    }
                    'L' -> {
                        path.lineTo(`val`[k + 0], `val`[k + 1])
                        currentX = `val`[k + 0]
                        currentY = `val`[k + 1]
                    }
                    'h' -> {
                        path.rLineTo(`val`[k + 0], 0f)
                        currentX += `val`[k + 0]
                    }
                    'H' -> {
                        path.lineTo(`val`[k + 0], currentY)
                        currentX = `val`[k + 0]
                    }
                    'v' -> {
                        path.rLineTo(0f, `val`[k + 0])
                        currentY += `val`[k + 0]
                    }
                    'V' -> {
                        path.lineTo(currentX, `val`[k + 0])
                        currentY = `val`[k + 0]
                    }
                    'c' -> {
                        path.rCubicTo(
                            `val`[k + 0], `val`[k + 1], `val`[k + 2], `val`[k + 3],
                            `val`[k + 4], `val`[k + 5]
                        )
                        ctrlPointX = currentX + `val`[k + 2]
                        ctrlPointY = currentY + `val`[k + 3]
                        currentX += `val`[k + 4]
                        currentY += `val`[k + 5]
                    }
                    'C' -> {
                        path.cubicTo(
                            `val`[k + 0], `val`[k + 1], `val`[k + 2], `val`[k + 3],
                            `val`[k + 4], `val`[k + 5]
                        )
                        currentX = `val`[k + 4]
                        currentY = `val`[k + 5]
                        ctrlPointX = `val`[k + 2]
                        ctrlPointY = `val`[k + 3]
                    }
                    's' -> {
                        reflectiveCtrlPointX = 0f
                        reflectiveCtrlPointY = 0f
                        if (previousCmd == 'c' || previousCmd == 's' || previousCmd == 'C' || previousCmd == 'S') {
                            reflectiveCtrlPointX = currentX - ctrlPointX
                            reflectiveCtrlPointY = currentY - ctrlPointY
                        }
                        path.rCubicTo(
                            reflectiveCtrlPointX, reflectiveCtrlPointY,
                            `val`[k + 0], `val`[k + 1],
                            `val`[k + 2], `val`[k + 3]
                        )
                        ctrlPointX = currentX + `val`[k + 0]
                        ctrlPointY = currentY + `val`[k + 1]
                        currentX += `val`[k + 2]
                        currentY += `val`[k + 3]
                    }
                    'S' -> {
                        reflectiveCtrlPointX = currentX
                        reflectiveCtrlPointY = currentY
                        if (previousCmd == 'c' || previousCmd == 's' || previousCmd == 'C' || previousCmd == 'S') {
                            reflectiveCtrlPointX = 2 * currentX - ctrlPointX
                            reflectiveCtrlPointY = 2 * currentY - ctrlPointY
                        }
                        path.cubicTo(
                            reflectiveCtrlPointX, reflectiveCtrlPointY,
                            `val`[k + 0], `val`[k + 1], `val`[k + 2], `val`[k + 3]
                        )
                        ctrlPointX = `val`[k + 0]
                        ctrlPointY = `val`[k + 1]
                        currentX = `val`[k + 2]
                        currentY = `val`[k + 3]
                    }
                    'q' -> {
                        path.rQuadTo(`val`[k + 0], `val`[k + 1], `val`[k + 2], `val`[k + 3])
                        ctrlPointX = currentX + `val`[k + 0]
                        ctrlPointY = currentY + `val`[k + 1]
                        currentX += `val`[k + 2]
                        currentY += `val`[k + 3]
                    }
                    'Q' -> {
                        path.quadTo(`val`[k + 0], `val`[k + 1], `val`[k + 2], `val`[k + 3])
                        ctrlPointX = `val`[k + 0]
                        ctrlPointY = `val`[k + 1]
                        currentX = `val`[k + 2]
                        currentY = `val`[k + 3]
                    }
                    't' -> {
                        reflectiveCtrlPointX = 0f
                        reflectiveCtrlPointY = 0f
                        if (previousCmd == 'q' || previousCmd == 't' || previousCmd == 'Q' || previousCmd == 'T') {
                            reflectiveCtrlPointX = currentX - ctrlPointX
                            reflectiveCtrlPointY = currentY - ctrlPointY
                        }
                        path.rQuadTo(
                            reflectiveCtrlPointX, reflectiveCtrlPointY,
                            `val`[k + 0], `val`[k + 1]
                        )
                        ctrlPointX = currentX + reflectiveCtrlPointX
                        ctrlPointY = currentY + reflectiveCtrlPointY
                        currentX += `val`[k + 0]
                        currentY += `val`[k + 1]
                    }
                    'T' -> {
                        reflectiveCtrlPointX = currentX
                        reflectiveCtrlPointY = currentY
                        if (previousCmd == 'q' || previousCmd == 't' || previousCmd == 'Q' || previousCmd == 'T') {
                            reflectiveCtrlPointX = 2 * currentX - ctrlPointX
                            reflectiveCtrlPointY = 2 * currentY - ctrlPointY
                        }
                        path.quadTo(
                            reflectiveCtrlPointX, reflectiveCtrlPointY,
                            `val`[k + 0], `val`[k + 1]
                        )
                        ctrlPointX = reflectiveCtrlPointX
                        ctrlPointY = reflectiveCtrlPointY
                        currentX = `val`[k + 0]
                        currentY = `val`[k + 1]
                    }
                    'a' -> {
                        // (rx ry x-axis-rotation large-arc-flag sweep-flag x y)
                        drawArc(
                            path,
                            currentX,
                            currentY,
                            `val`[k + 5] + currentX,
                            `val`[k + 6] + currentY,
                            `val`[k + 0],
                            `val`[k + 1],
                            `val`[k + 2], `val`[k + 3] != 0f, `val`[k + 4] != 0f
                        )
                        currentX += `val`[k + 5]
                        currentY += `val`[k + 6]
                        ctrlPointX = currentX
                        ctrlPointY = currentY
                    }
                    'A' -> {
                        drawArc(
                            path,
                            currentX,
                            currentY,
                            `val`[k + 5],
                            `val`[k + 6],
                            `val`[k + 0],
                            `val`[k + 1],
                            `val`[k + 2], `val`[k + 3] != 0f, `val`[k + 4] != 0f
                        )
                        currentX = `val`[k + 5]
                        currentY = `val`[k + 6]
                        ctrlPointX = currentX
                        ctrlPointY = currentY
                    }
                }
                previousCmd = cmd
                k += incr
            }
            current[0] = currentX
            current[1] = currentY
            current[2] = ctrlPointX
            current[3] = ctrlPointY
            current[4] = currentSegmentStartX
            current[5] = currentSegmentStartY
        }

        private fun drawArc(
            p: Path,
            x0: Float,
            y0: Float,
            x1: Float,
            y1: Float,
            a: Float,
            b: Float,
            theta: Float,
            isMoreThanHalf: Boolean,
            isPositiveArc: Boolean
        ) {

            /* Convert rotation angle from degrees to radians */
            val thetaD = Math.toRadians(theta.toDouble())
            /* Pre-compute rotation matrix entries */
            val cosTheta = Math.cos(thetaD)
            val sinTheta = Math.sin(thetaD)
            /* Transform (x0, y0) and (x1, y1) into unit space */
            /* using (inverse) rotation, followed by (inverse) scale */
            val x0p = (x0 * cosTheta + y0 * sinTheta) / a
            val y0p = (-x0 * sinTheta + y0 * cosTheta) / b
            val x1p = (x1 * cosTheta + y1 * sinTheta) / a
            val y1p = (-x1 * sinTheta + y1 * cosTheta) / b

            /* Compute differences and averages */
            val dx = x0p - x1p
            val dy = y0p - y1p
            val xm = (x0p + x1p) / 2
            val ym = (y0p + y1p) / 2
            /* Solve for intersecting unit circles */
            val dsq = dx * dx + dy * dy
            if (dsq == 0.0) {
                Log.w(LOGTAG, " Points are coincident")
                return  /* Points are coincident */
            }
            val disc = 1.0 / dsq - 1.0 / 4.0
            if (disc < 0.0) {
                Log.w(LOGTAG, "Points are too far apart $dsq")
                val adjust = (Math.sqrt(dsq) / 1.99999).toFloat()
                drawArc(
                    p, x0, y0, x1, y1, a * adjust,
                    b * adjust, theta, isMoreThanHalf, isPositiveArc
                )
                return  /* Points are too far apart */
            }
            val s = Math.sqrt(disc)
            val sdx = s * dx
            val sdy = s * dy
            var cx: Double
            var cy: Double
            if (isMoreThanHalf == isPositiveArc) {
                cx = xm - sdy
                cy = ym + sdx
            } else {
                cx = xm + sdy
                cy = ym - sdx
            }
            val eta0 = Math.atan2(y0p - cy, x0p - cx)
            val eta1 = Math.atan2(y1p - cy, x1p - cx)
            var sweep = eta1 - eta0
            if (isPositiveArc != sweep >= 0) {
                if (sweep > 0) {
                    sweep -= 2 * Math.PI
                } else {
                    sweep += 2 * Math.PI
                }
            }
            cx *= a.toDouble()
            cy *= b.toDouble()
            val tcx = cx
            cx = cx * cosTheta - cy * sinTheta
            cy = tcx * sinTheta + cy * cosTheta
            arcToBezier(
                p,
                cx,
                cy,
                a.toDouble(),
                b.toDouble(),
                x0.toDouble(),
                y0.toDouble(),
                thetaD,
                eta0,
                sweep
            )
        }

        /**
         * Converts an arc to cubic Bezier segments and records them in p.
         *
         * @param p     The target for the cubic Bezier segments
         * @param cx    The x coordinate center of the ellipse
         * @param cy    The y coordinate center of the ellipse
         * @param a     The radius of the ellipse in the horizontal direction
         * @param b     The radius of the ellipse in the vertical direction
         * @param e1x   E(eta1) x coordinate of the starting point of the arc
         * @param e1y   E(eta2) y coordinate of the starting point of the arc
         * @param theta The angle that the ellipse bounding rectangle makes with horizontal plane
         * @param start The start angle of the arc on the ellipse
         * @param sweep The angle (positive or negative) of the sweep of the arc on the ellipse
         */
        private fun arcToBezier(
            p: Path,
            cx: Double,
            cy: Double,
            a: Double,
            b: Double,
            e1x: Double,
            e1y: Double,
            theta: Double,
            start: Double,
            sweep: Double
        ) {
            // Taken from equations at: http://spaceroots.org/documents/ellipse/node8.html
            // and http://www.spaceroots.org/documents/ellipse/node22.html

            // Maximum of 45 degrees per cubic Bezier segment
            var e1x = e1x
            var e1y = e1y
            val numSegments = Math.ceil(Math.abs(sweep * 4 / Math.PI)).toInt()
            var eta1 = start
            val cosTheta = Math.cos(theta)
            val sinTheta = Math.sin(theta)
            val cosEta1 = Math.cos(eta1)
            val sinEta1 = Math.sin(eta1)
            var ep1x = -a * cosTheta * sinEta1 - b * sinTheta * cosEta1
            var ep1y = -a * sinTheta * sinEta1 + b * cosTheta * cosEta1
            val anglePerSegment = sweep / numSegments
            for (i in 0 until numSegments) {
                val eta2 = eta1 + anglePerSegment
                val sinEta2 = Math.sin(eta2)
                val cosEta2 = Math.cos(eta2)
                val e2x = cx + a * cosTheta * cosEta2 - b * sinTheta * sinEta2
                val e2y = cy + a * sinTheta * cosEta2 + b * cosTheta * sinEta2
                val ep2x = -a * cosTheta * sinEta2 - b * sinTheta * cosEta2
                val ep2y = -a * sinTheta * sinEta2 + b * cosTheta * cosEta2
                val tanDiff2 = Math.tan((eta2 - eta1) / 2)
                val alpha = Math.sin(eta2 - eta1) * (Math.sqrt(4 + 3 * tanDiff2 * tanDiff2) - 1) / 3
                val q1x = e1x + alpha * ep1x
                val q1y = e1y + alpha * ep1y
                val q2x = e2x - alpha * ep2x
                val q2y = e2y - alpha * ep2y

                // Adding this no-op call to workaround a proguard related issue.
                p.rLineTo(0f, 0f)
                p.cubicTo(
                    q1x.toFloat(),
                    q1y.toFloat(),
                    q2x.toFloat(),
                    q2y.toFloat(),
                    e2x.toFloat(),
                    e2y.toFloat()
                )
                eta1 = eta2
                e1x = e2x
                e1y = e2y
                ep1x = ep2x
                ep1y = ep2y
            }
        }
    }
}