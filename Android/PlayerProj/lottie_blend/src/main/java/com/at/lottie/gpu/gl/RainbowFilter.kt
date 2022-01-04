package com.at.lottie.gpu.gl

import android.opengl.GLES20
import androidx.annotation.FloatRange
import com.at.lottie.IFilter
import com.at.lottie.R
import com.at.lottie.raw2String
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter

class RainbowFilter : BaseGlitchFilter(R.raw.rainbow_frag_shader.raw2String()), IFilter {
    private var amountUniform = 0
    private var offsetUniform = 0
    override fun onInit() {
        super.onInit()
        amountUniform = GLES20.glGetUniformLocation(program, "amount")
        offsetUniform = GLES20.glGetUniformLocation(program, "offset")
    }

    override fun onInitialized() {
        super.onInitialized()
        runOnDraw {
            GLES20.glUniform1f(amountUniform, 0f)
            GLES20.glUniform1f(offsetUniform, 0f)
        }
    }

    fun setAmount(@FloatRange(from = 0.0, to = 0.8) amount: Float) {
        runOnDraw { GLES20.glUniform1f(amountUniform, amount) }
    }

    fun setOffset(@FloatRange(from = 0.0, to = 2.0) offset: Float) {
        runOnDraw { GLES20.glUniform1f(offsetUniform, offset) }
    }

    override fun setTime(time: Float) {
        super.setTime(time)
    }

    override fun setIntensity(intensity: Float) {
        runOnDraw {
            GLES20.glUniform1f(amountUniform, intensity)
            GLES20.glUniform1f(offsetUniform, intensity)
        }
    }

    override fun getFilter(): GPUImageFilter = this


    override fun doFrame(startFrame: Int, endFrame: Int, frame: Int, index: Int) {
        setIntensity(intensityFloat)
        setTime(calculateTimes(frame))
    }
}