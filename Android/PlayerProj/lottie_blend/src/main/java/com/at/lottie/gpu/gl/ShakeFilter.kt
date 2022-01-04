package com.at.lottie.gpu.gl

import android.opengl.GLES20
import androidx.annotation.FloatRange
import com.at.lottie.R
import com.at.lottie.raw2String
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter

class ShakeFilter : BaseGlitchFilter(R.raw.shake_frag_shader.raw2String()), IGlitch {
    private var amountUniform = 0

    override fun onInit() {
        super.onInit()
        amountUniform = GLES20.glGetUniformLocation(program, "amount")
    }


    override fun onInitialized() {
        super.onInitialized()
        runOnDraw { GLES20.glUniform1f(amountUniform, 0f) }
    }

    override fun setTime(time: Float) {
        super.setTime(time)
    }

    /** 震动幅度 */
    fun setAmount(@FloatRange(from = 0.0, to = 0.2) amount: Float) {
        runOnDraw { GLES20.glUniform1f(amountUniform, amount) }
    }

    override fun setIntensity(intensity: Float) {
        runOnDraw { GLES20.glUniform1f(amountUniform, 0.159f * intensity) }
    }

    override fun getFilter(): GPUImageFilter {
        return this
    }


    override fun doFrame(startFrame: Int, endFrame: Int, frame: Int, index: Int) {
        setIntensity(intensityFloat)
        setTime(calculateTimes(frame))
    }
}