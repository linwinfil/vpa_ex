package com.at.lottie.gpu.gl

import android.opengl.GLES20
import androidx.annotation.FloatRange
import com.at.lottie.R
import com.at.lottie.raw2String
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter

/**
 * 移轴虚化 tilt shift by https://photomosh.com/
 * Created by linmaoxin on 2021/12/30
 */
class TiltShiftFilter : BaseGlitchFilter(R.raw.tiltshift_frag_shader.raw2String()) {
    override fun setIntensity(intensity: Float) {
    }

    private var amountUniform: Int = 0
    private var positionUniform: Int = 0
    override fun onInit() {
        super.onInit()
        amountUniform = GLES20.glGetUniformLocation(program, "amount")
        positionUniform = GLES20.glGetUniformLocation(program, "position")
    }

    override fun onInitialized() {
        super.onInitialized()
    }

    override fun getFilter(): GPUImageFilter = this

    /** 虚化程度[0.00-0.02] */
    fun setAmount(amount: Float) {
        runOnDraw {
            GLES20.glUniform1f(amountUniform, amount)
        }
    }

    /** 移轴Y位置 [0.0-1.0]*/
    fun setPosition(@FloatRange(from = 0.0, to = 1.0) position: Float) {
        runOnDraw {
            GLES20.glUniform1f(positionUniform, position)
        }
    }

    override fun reset() {
        super.reset()
        setAmount(0f)
        setPosition(0f)
    }

    override fun doFrame(startFrame: Int, endFrame: Int, frame: Int, index: Int) {
        calculateTimes(frame)
    }
}