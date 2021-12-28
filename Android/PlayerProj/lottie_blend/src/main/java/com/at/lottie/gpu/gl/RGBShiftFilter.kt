package com.at.lottie.gpu.gl

import android.opengl.GLES20
import androidx.annotation.FloatRange
import com.at.lottie.IFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter

class RGBShiftFilter : BaseGlitchFilter(FRAGMENT_SHADER), IFilter {

    companion object {
        private val FRAGMENT_SHADER = "precision highp float;\n" +
                "uniform sampler2D u_texture;\n" +
                "uniform float u_amount;\n" +
                "uniform float u_angle;\n" +
                "varying vec2 textureCoordinate;\n" +
                "void main() {\n" +
                "    vec2 offset = u_amount * vec2(cos(u_angle), sin(u_angle));\n" +
                "    vec4 cr = texture2D(u_texture, textureCoordinate + offset);\n" +
                "    vec4 cga = texture2D(u_texture, textureCoordinate);\n" +
                "    vec4 cb = texture2D(u_texture, textureCoordinate - offset);\n" +
                "    gl_FragColor = vec4(cr.r, cga.g, cb.b, cga.a);\n" +
                "}"
    }

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

    override fun setTime(@FloatRange(from = 0.0, to = 1.0) time: Float) {
        runOnDraw { GLES20.glUniform1f(angleUniform, 10 * time) }
    }

    override fun setIntensity(intensity: Float) {
        this.intensity = intensity
        runOnDraw {
            GLES20.glUniform1f(amountUniform, 0.03f * intensity)
        }
    }

    override fun doFrame(startFrame: Int, endFrame: Int, frame: Int, index: Int) {
        setIntensity(intensity)
        setTime(calculateTimes(frame))
    }

    override fun getFilter(): GPUImageFilter = this

}