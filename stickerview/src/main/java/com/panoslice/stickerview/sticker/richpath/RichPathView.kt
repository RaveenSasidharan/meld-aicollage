package com.panoslice.stickerview.sticker.richpath

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.IntRange
import com.xiaopo.flying.sticker.R
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

/**
 * Created by tarek on 6/29/17.
 */
@SuppressLint("AppCompatCustomView")
class RichPathView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr) {
    private var vector: Vector? = null
    private var richPathDrawable: RichPathDrawable? = null
    private var onPathClickListener: OnPathClickListener? = null

    init {
        init()
        setupAttributes(attrs)
    }

    private fun init() {
        //Disable hardware
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    private fun setupAttributes(attrs: AttributeSet?) {

        // Obtain a typed array of attributes
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.RichPathView, 0, 0)
        // Extract custom attributes into member variables
        val resID = -1
        //        try {
//            resID = a.getResourceId(R.styleable.RichPathView_vector, -1);
//        } finally {
//            // TypedArray objects are shared and must be recycled.
//            a.recycle();
//        }
        if (resID != -1) {
            setVectorDrawable(resID)
        }
    }

    /**
     * Set a VectorDrawable resource ID.
     *
     * @param resId the resource ID for VectorDrawableCompat object.
     */
    fun setVectorDrawable(@DrawableRes resId: Int) {
        val xpp = context.resources.getXml(resId)
        vector = Vector()
        try {
            XmlParser.parseVector(vector, xpp, context)
            richPathDrawable = RichPathDrawable(vector, scaleType)
            setImageDrawable(richPathDrawable)
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (vector == null) return
        val desiredWidth = vector.getWidth() as Int
        val desiredHeight = vector.getHeight() as Int
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)


        //Measure Width
        val width: Int
        width = if (widthMode == MeasureSpec.EXACTLY) {
            //Must be this size
            widthSize
        } else if (widthMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            Math.min(desiredWidth, widthSize)
        } else {
            //Be whatever you want
            desiredWidth
        }

        //Measure Height
        val height: Int
        height = if (heightMode == MeasureSpec.EXACTLY) {
            //Must be this size
            heightSize
        } else if (heightMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            Math.min(desiredHeight, heightSize)
        } else {
            //Be whatever you want
            desiredHeight
        }

        //MUST CALL THIS
        setMeasuredDimension(width, height)
    }

    fun findAllRichPaths(): Array<RichPath?> {
        return if (richPathDrawable == null) arrayOfNulls(0) else richPathDrawable!!.findAllRichPaths()
    }

    fun findRichPathByName(name: String): RichPath? {
        return if (richPathDrawable == null) null else richPathDrawable!!.findRichPathByName(name)
    }

    /**
     * find the first [RichPath] or null if not found
     *
     *
     * This can be in handy if the vector consists of 1 path only
     *
     * @return the [RichPath] object found or null
     */
    fun findFirstRichPath(): RichPath? {
        return if (richPathDrawable == null) null else richPathDrawable!!.findFirstRichPath()
    }

    /**
     * find [RichPath] by its index or null if not found
     *
     *
     * Note that the provided index must be the flattened index of the path
     *
     *
     * example:
     * <pre>
     * `<vector>
     * <path> // index = 0
     * <path> // index = 1
     * <group>
     * <path> // index = 2
     * <group>
     * <path> // index = 3
     * </group>
     * </group>
     * <path> // index = 4
     * </vector>`
    </pre> *
     *
     * @param index the flattened index of the path
     * @return the [RichPath] object found or null
     */
    fun findRichPathByIndex(@IntRange(from = 0) index: Int): RichPath? {
        return if (richPathDrawable == null) null else richPathDrawable!!.findRichPathByIndex(index)
    }

    fun addPath(path: String?) {
        if (richPathDrawable != null) {
            richPathDrawable.addPath(PathParserT.createPathFromPathData(path))
        }
    }

    fun addPath(path: Path?) {
        if (richPathDrawable != null) {
            richPathDrawable!!.addPath(path)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action
        when (action) {
            MotionEvent.ACTION_UP -> performClick()
        }
        val richPath = richPathDrawable!!.getTouchedPath(event)
        if (richPath != null) {
            val onPathClickListener: OnPathClickListener? = richPath.onPathClickListener
            if (onPathClickListener != null) {
                onPathClickListener.onClick(richPath)
            }
            if (this.onPathClickListener != null) {
                this.onPathClickListener.onClick(richPath)
            }
        }
        return true
    }

    fun setOnPathClickListener(onPathClickListener: OnPathClickListener?) {
        this.onPathClickListener = onPathClickListener
    }
}