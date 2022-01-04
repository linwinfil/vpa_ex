package com.at.lottie.gpu.gl

import android.opengl.GLES20
import androidx.annotation.FloatRange
import com.at.lottie.IFilter
import com.at.lottie.R
import com.at.lottie.raw2String
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import java.time.temporal.TemporalAmount

class RGBShiftFilter : BaseGlitchFilter(R.raw.rgb_shift_frag_shader.raw2String()), IFilter {

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

    /** rgb 中心旋转角度 */
    fun setAngle(@FloatRange(from = 0.0, to = 6.28) angle: Float) {
        runOnDraw {
            GLES20.glUniform1f(angleUniform, angle)
        }
    }

    /** rgb Y轴分离强度 */
    fun setAmount(@FloatRange(from = 0.0, to = 0.1) amount: Float) {
        runOnDraw {
            GLES20.glUniform1f(amountUniform, amount)
        }
    }

    override fun setTime(@FloatRange(from = 0.0, to = 1.0) time: Float) {
        runOnDraw { GLES20.glUniform1f(angleUniform, 10 * time) }
    }

    override fun setIntensity(intensity: Float) {
        this.intensityFloat = intensity
        runOnDraw {
            GLES20.glUniform1f(amountUniform, 0.03f * intensity)
        }
    }

    override fun doFrame(startFrame: Int, endFrame: Int, frame: Int, index: Int) {
        setIntensity(intensityFloat)
        setTime(calculateTimes(frame))
    }

    override fun getFilter(): GPUImageFilter = this

}