package com.at.lottie.gpu.gl

import com.at.lottie.IFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup

class RGBShiftShakeFilter : GPUImageFilterGroup(
    arrayListOf(BarrelBlurFilter(), SmearFilter()).toList()
), IGlitch, IFilter {
    private var enableDrawer = true
    override fun setTime(time: Float) {
        for (filter in filters) {
            val glitchFilter = filter as IGlitch
            glitchFilter.setTime(time)
        }
    }

    override fun setIntensity(intensity: Float) {
        for (filter in filters) {
            val glitchFilter = filter as IGlitch
            glitchFilter.setIntensity(intensity)
        }
    }

    override fun setIntensityValue(intensity: Float) {
        for (filter in filters) {
            val glitchFilter = filter as IGlitch
            glitchFilter.setIntensityValue(intensity)
        }
    }

    override fun reset() {
        for (filter in filters) {
            val glitchFilter = filter as IGlitch
            glitchFilter.reset()
        }
    }

    override fun getFilter(): GPUImageFilter {
        return this
    }

    override fun doFrame(startFrame: Int, endFrame: Int, frame: Int, index: Int) {
        for (filter in filters) {
            val glitchFilter = filter as IFilter
            glitchFilter.doFrame(startFrame, endFrame, frame, index)
        }
    }
    override fun isEnableDraw(): Boolean {
        return enableDrawer
    }

    override fun setEnableDraw(enable: Boolean) {
        enableDrawer = enable
    }
}