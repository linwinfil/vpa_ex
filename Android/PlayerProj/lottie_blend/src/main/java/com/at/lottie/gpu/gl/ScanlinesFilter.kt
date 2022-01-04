package com.at.lottie.gpu.gl

import android.opengl.GLES20
import androidx.annotation.FloatRange
import com.at.lottie.IFilter
import com.at.lottie.R
import com.at.lottie.raw2String
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import java.time.temporal.TemporalAmount

class ScanlinesFilter : BaseGlitchFilter(R.raw.scanlines_frag_shader.raw2String()), IFilter {
    private var countUniform = 0
    private var noiseAmountUniform = 0
    private var linesAmountUniform = 0
    private var heightUniform = 0
    override fun onInit() {
        super.onInit()
        countUniform = GLES20.glGetUniformLocation(program, "count")
        noiseAmountUniform = GLES20.glGetUniformLocation(program, "noiseAmount")
        linesAmountUniform = GLES20.glGetUniformLocation(program, "linesAmount")
        heightUniform = GLES20.glGetUniformLocation(program, "height")
    }

    override fun onInitialized() {
        super.onInitialized()
        runOnDraw {
            GLES20.glUniform1f(countUniform, 0f)
            GLES20.glUniform1f(noiseAmountUniform, 0f)
            GLES20.glUniform1f(linesAmountUniform, 0f)
            GLES20.glUniform1f(heightUniform, outputHeight.toFloat())
        }
    }

    override fun setIntensity(intensity: Float) {
        runOnDraw {
            if (intensity == 0f) {
                GLES20.glUniform1f(noiseAmountUniform, 0f)
                GLES20.glUniform1f(linesAmountUniform, 0f)
            } else {
                GLES20.glUniform1f(noiseAmountUniform, 0.52f)
                GLES20.glUniform1f(linesAmountUniform, 0.85f)
            }
            GLES20.glUniform1f(countUniform, intensity)
            GLES20.glUniform1f(heightUniform, outputHeight.toFloat())
        }
    }

    fun setCount(@FloatRange(from = 0.0, to = 1.0) count: Float) {
        runOnDraw { GLES20.glUniform1f(countUniform, count) }
    }

    fun setNoiseAmount(@FloatRange(from = 0.0, to = 2.0) amount: Float) {
        runOnDraw { GLES20.glUniform1f(noiseAmountUniform, amount) }
    }

    fun setLineAmount(@FloatRange(from = 0.0, to = 2.0) amount: Float) {
        runOnDraw { GLES20.glUniform1f(linesAmountUniform, amount) }
    }
    override fun getFilter(): GPUImageFilter = this

    override fun doFrame(startFrame: Int, endFrame: Int, frame: Int, index: Int) {
        setIntensity(intensityFloat)
        setTime(calculateTimes(frame))
    }
}