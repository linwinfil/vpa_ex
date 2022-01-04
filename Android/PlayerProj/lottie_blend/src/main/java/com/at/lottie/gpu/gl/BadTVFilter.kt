package com.at.lottie.gpu.gl

import android.opengl.GLES20
import androidx.annotation.FloatRange
import com.at.lottie.IFilter
import com.at.lottie.R
import com.at.lottie.raw2String
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter

class BadTVFilter : BaseGlitchFilter(R.raw.badtv_frag_shader.raw2String()), IFilter {
    private var distortionUniform = 0
    private var distortion2Uniform = 0
    private var speedUniform = 0
    private var rollSpeedUniform = 0
    override fun onInit() {
        super.onInit()
        distortionUniform = GLES20.glGetUniformLocation(program, "distortion")
        distortion2Uniform = GLES20.glGetUniformLocation(program, "distortion2")
        speedUniform = GLES20.glGetUniformLocation(program, "speed")
        rollSpeedUniform = GLES20.glGetUniformLocation(program, "rollSpeed")
    }

    override fun onInitialized() {
        super.onInitialized()
        runOnDraw {
            GLES20.glUniform1f(distortionUniform, 0f)
            GLES20.glUniform1f(distortion2Uniform, 0f)
            GLES20.glUniform1f(speedUniform, 0.116f)
            GLES20.glUniform1f(rollSpeedUniform, 1f)
        }
    }

    /** x轴 波纹大幅度扭曲 */
    fun setDistortion(@FloatRange(from = 0.0, to = 6.0) distortion: Float) {
        runOnDraw { GLES20.glUniform1f(distortionUniform, distortion) }
    }

    /** x轴 波纹下幅度扭曲 */
    fun setDistortion2(@FloatRange(from = 0.0, to = 6.0) distortion: Float) {
        runOnDraw { GLES20.glUniform1f(distortion2Uniform, distortion) }
    }

    fun setSpeed(speed:Float) {
        runOnDraw { GLES20.glUniform1f(speedUniform, speed) }
    }

    /* y轴 滚动速度 */
    fun setRollSpeed(@FloatRange(from = 0.0, to = 3.0) speed: Float) {
        runOnDraw { GLES20.glUniform1f(rollSpeedUniform, speed) }
    }

    override fun setIntensity(intensity: Float) {
        runOnDraw {
            GLES20.glUniform1f(distortionUniform, 3.75f * intensity)
            GLES20.glUniform1f(distortion2Uniform, 12.5f * intensity)
        }
    }

    override fun getFilter(): GPUImageFilter {
        return this
    }


    override fun doFrame(startFrame: Int, endFrame: Int, frame: Int, index: Int) {
        setIntensity(intensityFloat)
        setTime(calculateTimes(frame))
    }
}