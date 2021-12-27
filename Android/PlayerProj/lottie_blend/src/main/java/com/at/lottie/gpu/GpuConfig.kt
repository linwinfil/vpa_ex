package com.at.lottie.gpu

import com.at.lottie.IFilter
import com.at.lottie.gpu.gl.JitterFilter
import com.at.lottie.gpu.gl.RGBShiftFilter

/** @author lmx
 * Created by lmx on 2021/12/28.
 */


object GpuConfig {
    const val FILTER_GLITCH = 1
    const val FILTER_RGB_SHIFT = 2

    fun getGpuFilter(filter: Int): IFilter? {
        return when (filter) {
            FILTER_GLITCH -> JitterFilter()
            FILTER_RGB_SHIFT -> RGBShiftFilter()
            else -> null
        }
    }

}