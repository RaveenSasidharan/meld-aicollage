package com.punchylabs.jewelleria.utils

import android.content.Context
import android.os.Handler
import androidx.viewpager.widget.ViewPager
import com.panoslice.meldphotocollage.R
import com.panoslice.meldphotocollage.utils.AppResult

fun ViewPager.autoScroll(interval: Long) {

    val handler = Handler()
    var scrollPosition = 0

    val runnable = object : Runnable {

        override fun run() {

            /**
             * Calculate "scroll position" with
             * adapter pages count and current
             * value of scrollPosition.
             */
            val count = adapter?.count ?: 0
            setCurrentItem(scrollPosition++ % count, true)

            handler.postDelayed(this, interval)
        }
    }

    addOnPageChangeListener(object: ViewPager.OnPageChangeListener {
        override fun onPageSelected(position: Int) {
            // Updating "scroll position" when user scrolls manually
            scrollPosition = position + 1
        }

        override fun onPageScrollStateChanged(state: Int) {
            // Not necessary
        }

        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            // Not necessary
        }
    })

    handler.post(runnable)
}

fun Context.noNetworkConnectivityError(): AppResult.Error {
    return AppResult.Error(Exception(this.resources.getString(R.string.no_network_connectivity)))
}