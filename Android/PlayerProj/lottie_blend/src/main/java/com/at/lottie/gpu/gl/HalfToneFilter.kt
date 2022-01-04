package com.at.lottie.gpu.gl

import android.opengl.GLES20
import androidx.annotation.FloatRange
import com.at.lottie.IFilter
import com.at.lottie.R
import com.at.lottie.raw2String
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter

class HalfToneFilter : BaseGlitchFilter(R.raw.halftone_frag_shader.raw2String()), IFilter {
    private var centerUniform = 0
    private var angleUniform = 0
    private var scaleUniform = 0
    private var tSizeUniform = 0
    override fun onInit() {
        super.onInit()
        centerUniform = GLES20.glGetUniformLocation(program, "center")
        angleUniform = GLES20.glGetUniformLocation(program, "angle")
        scaleUniform = GLES20.glGetUniformLocation(program, "scale")
        tSizeUniform = GLES20.glGetUniformLocation(program, "tSize")
    }

    fun setCenter(@FloatRange(from = 0.0, to = 0.5) center: Float) {
        runOnDraw { GLES20.glUniform1f(centerUniform, center) }
    }

    fun setAngle(angle: Float) {
        runOnDraw { GLES20.glUniform1f(angleUniform, angle) }
    }

    fun setScale(@FloatRange(from = 0.0, to = 1.0) scale: Float) {
        runOnDraw { GLES20.glUniform1f(scaleUniform, scale) }
    }

    fun setTSize(@FloatRange(from = 0.0, to = 256.0) tSize: Float) {
        runOnDraw { GLES20.glUniform1f(tSizeUniform, tSize) }
    }

    override fun onInitialized() {
        super.onInitialized()
        runOnDraw {
            GLES20.glUniform1f(angleUniform, 1.57f)
            GLES20.glUniform1f(scaleUniform, 0f)
            GLES20.glUniform2f(centerUniform, 0.5f, 0.5f)
            GLES20.glUniform2f(tSizeUniform, 256f, 256f)
        }
    }

    override fun setTime(time: Float) {
        super.setTime(time)
    }

    override fun setIntensity(intensity: Float) {
        runOnDraw { GLES20.glUniform1f(scaleUniform, 2 * intensity) }
    }

    override fun getFilter(): GPUImageFilter {
        return this
    }

    override fun doFrame(startFrame: Int, endFrame: Int, frame: Int, index: Int) {
        setIntensity(intensityFloat)
        setTime(calculateTimes(frame))
    }
}