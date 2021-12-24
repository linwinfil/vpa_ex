package com.tencent.qgame.playerproj.gl

import android.opengl.GLES20
import androidx.annotation.FloatRange
import com.at.lottie.IFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter

class RGBShiftFilter(vertex: String, fragment: String) : GPUImageFilter(vertex, fragment), IFilter {
    private var amountUniform = 0
    private var angleUniform = 0
    override fun onInit() {
        super.onInit()
        amountUniform = GLES20.glGetUniformLocation(program, "u_amount")
        angleUniform = GLES20.glGetUniformLocation(program, "u_angle")
    }

    override fun onInitialized() {
        super.onInitialized()
        runOnDraw {
            GLES20.glUniform1f(angleUniform, 0.0f)
            GLES20.glUniform1f(amountUniform, 0.0f)
        }
    }

    fun setTime(@FloatRange(from = 0.0, to = 1.0)time: Float) {
        runOnDraw { GLES20.glUniform1f(angleUniform, 10 * time) }
    }

    fun setIntensity(intensity: Float) {
        runOnDraw { GLES20.glUniform1f(amountUniform, 0.03f * intensity) }
    }

    override fun doFrame(startFrame: Int, endFrame: Int, frame: Int) {

    }
}