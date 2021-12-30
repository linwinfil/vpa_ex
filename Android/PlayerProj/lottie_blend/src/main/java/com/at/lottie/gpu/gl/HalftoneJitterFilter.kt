package com.at.lottie.gpu.gl

import androidx.annotation.CallSuper
import com.at.lottie.IFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup
import java.nio.FloatBuffer

class HalftoneJitterFilter : GPUImageFilterGroup(
    arrayListOf(HalfToneFilter(), JitterFilter()).toList()
), IGlitch, IFilter {

    var enableDrawer: Boolean = true
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

    @CallSuper
    override fun onDraw(textureId: Int, cubeBuffer: FloatBuffer?, textureBuffer: FloatBuffer?) {
        if (!enableDrawer) return
        super.onDraw(textureId, cubeBuffer, textureBuffer)
    }

    override fun getFilter(): GPUImageFilter {
        return this
    }

    override fun isEnableDraw(): Boolean = enableDrawer

    override fun setEnableDraw(enable: Boolean) {
        enableDrawer = enable
    }

    override fun doFrame(startFrame: Int, endFrame: Int, frame: Int, index: Int) {
        for (filter in filters) {
            val glitchFilter = filter as IFilter
            glitchFilter.doFrame(startFrame, endFrame, frame, index)
        }
    }
}