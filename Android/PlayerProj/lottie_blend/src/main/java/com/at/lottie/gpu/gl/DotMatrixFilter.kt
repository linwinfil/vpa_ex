package com.at.lottie.gpu.gl

import android.opengl.GLES20
import androidx.annotation.FloatRange
import com.at.lottie.IFilter
import com.at.lottie.R
import com.at.lottie.raw2String
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter

class DotMatrixFilter : BaseGlitchFilter(R.raw.dotmatrix_frag_shader.raw2String()), IFilter {
    private var dotsUniform = 0
    private var sizeUniform = 0
    private var blurUniform = 0
    override fun onInit() {
        super.onInit()
        dotsUniform = GLES20.glGetUniformLocation(program, "dots")
        sizeUniform = GLES20.glGetUniformLocation(program, "size")
        blurUniform = GLES20.glGetUniformLocation(program, "blur")
    }

    /** 点个数 */
    fun setDots(@FloatRange(from = 0.0, to = 200.0) dots: Float) {
        runOnDraw { GLES20.glUniform1f(dotsUniform, dots) }
    }

    /** 点边缘黑边大小 */
    fun setSize(@FloatRange(from = 0.0, to = 1.0) size: Float) {
        runOnDraw { GLES20.glUniform1f(sizeUniform, size) }
    }

    /** 点圆角大小 */
    fun setBlur(@FloatRange(from = 0.0, to = 1.0) blur: Float) {
        runOnDraw { GLES20.glUniform1f(blurUniform, blur) }
    }

    override fun onInitialized() {
        super.onInitialized()
        runOnDraw {
            GLES20.glUniform1f(dotsUniform, 0f)
            GLES20.glUniform1f(sizeUniform, 0.429f)
            GLES20.glUniform1f(blurUniform, 0.364f)
        }
    }

    override fun setIntensity(intensity: Float) {
        runOnDraw { GLES20.glUniform1f(dotsUniform, 150 * intensity) }
    }

    override fun getFilter(): GPUImageFilter {
        return this
    }

    override fun doFrame(startFrame: Int, endFrame: Int, frame: Int, index: Int) {
        setIntensity(intensityFloat)
        setTime(calculateTimes(frame))
    }
}