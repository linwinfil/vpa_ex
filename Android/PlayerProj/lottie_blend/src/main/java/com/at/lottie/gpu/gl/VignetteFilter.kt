package com.at.lottie.gpu.gl

import android.opengl.GLES20
import com.at.lottie.IFilter
import com.at.lottie.R
import com.at.lottie.raw2String
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter

/**
 * 晕影Vignette by https://photomosh.com/
 * Created by linmaoxin on 2021/12/30
 */
class VignetteFilter : BaseGlitchFilter(R.raw.vignette_frag_shader.raw2String()), IFilter {
    private var offsetUniform: Int = 0
    private var darknessUniform: Int = 0
    override fun onInit() {
        super.onInit()
        offsetUniform = GLES20.glGetUniformLocation(program, "offset")
        darknessUniform = GLES20.glGetUniformLocation(program, "darkness")
    }

    override fun setIntensity(intensity: Float) {
        runOnDraw {
            GLES20.glUniform1f(offsetUniform, intensity * 2.0f)
            GLES20.glUniform1f(darknessUniform, intensity * 2.0f)
        }
    }

    /** 晕影范围 */
    fun setOffset(offset: Float, factor: Float = 1f) {
        runOnDraw {
            GLES20.glUniform1f(offsetUniform, offset * factor)
        }
    }

    /** 晕影力度 */
    fun setDarkness(darkness: Float, factor: Float = 1f) {
        runOnDraw {
            GLES20.glUniform1f(darknessUniform, darkness * factor)
        }
    }

    override fun reset() {
        setOffset(0f)
        setDarkness(0f)
    }

    override fun getFilter(): GPUImageFilter = this

    override fun doFrame(startFrame: Int, endFrame: Int, frame: Int, index: Int) {
        setIntensity(intensityFloat)
        setTime(calculateTimes(frame))
    }
}