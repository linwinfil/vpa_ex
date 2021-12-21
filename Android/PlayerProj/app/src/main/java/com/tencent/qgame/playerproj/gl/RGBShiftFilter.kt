package com.tencent.qgame.playerproj.gl

import android.content.Context
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import android.opengl.GLES20
import com.tencent.qgame.playerproj.R
import okio.buffer
import okio.source

class RGBShiftFilter(context: Context) : GPUImageFilter(NO_FILTER_VERTEX_SHADER,
    context.resources.openRawResource(R.raw.rgb_shift_fragment).source().buffer().readUtf8()) {
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

    fun setTime(time: Float) {
        runOnDraw { GLES20.glUniform1f(angleUniform, 10 * time) }
    }

    fun setIntensity(intensity: Float) {
        runOnDraw { GLES20.glUniform1f(amountUniform, 0.03f * intensity) }
    }
}