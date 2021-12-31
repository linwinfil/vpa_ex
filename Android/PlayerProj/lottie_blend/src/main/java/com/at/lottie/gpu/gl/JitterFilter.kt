package com.at.lottie.gpu.gl

import android.opengl.GLES20
import androidx.annotation.FloatRange
import com.at.lottie.R
import com.at.lottie.raw2String
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter

class JitterFilter : BaseGlitchFilter(R.raw.jitter_frag_shader.raw2String()), IGlitch {
    private var amountUniform = 0
    private var speedUniform = 0

    override fun onInit() {
        super.onInit()
        amountUniform = GLES20.glGetUniformLocation(program, "amount")
        speedUniform = GLES20.glGetUniformLocation(program, "speed")
    }

    override fun onInitialized() {
        super.onInitialized()
        runOnDraw {
            GLES20.glUniform1f(speedUniform, 0.7f)
            GLES20.glUniform1f(amountUniform, 0.0f)
        }
    }

    fun setAmount(@FloatRange(from = 0.0, to = 0.5) amount: Float) {
        runOnDraw { GLES20.glUniform1f(amountUniform, amount) }
    }

    fun setSpeed(@FloatRange(from = 0.0, to = 1.0) speed: Float) {
        runOnDraw { GLES20.glUniform1f(speedUniform, speed) }
    }

    override fun setTime(time: Float) {
        super.setTime(time)
    }

    override fun setIntensity(intensity: Float) {
        runOnDraw { GLES20.glUniform1f(amountUniform, intensity) }
    }

    override fun getFilter(): GPUImageFilter {
        return this
    }

    override fun doFrame(startFrame: Int, endFrame: Int, frame: Int, index: Int) {
        setIntensity(intensityFloat)
        setTime(calculateTimes(frame))
    }
}