package com.at.lottie.gpu.gl

import android.opengl.GLES20
import com.at.lottie.IFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter

class SmearFilter : BaseGlitchFilter(FRAGMENT_SHADER), IFilter {
    private var amountUniform = 0
    override fun onInit() {
        super.onInit()
        amountUniform = GLES20.glGetUniformLocation(program, "amount")
    }

    override fun onInitialized() {
        super.onInitialized()
        runOnDraw { GLES20.glUniform1f(amountUniform, 0f) }
    }

    override fun setTime(time: Float) {
        super.setTime(time)
    }

    override fun setIntensity(intensity: Float) {
        runOnDraw { GLES20.glUniform1f(amountUniform, 0.04318f * intensity) }
    }

    override fun getFilter(): GPUImageFilter {
        return this
    }


    override fun doFrame(startFrame: Int, endFrame: Int, frame: Int, index: Int) {
        setIntensity(intensityFloat)
        setTime(calculateTimes(frame))
    }

    companion object {
        private const val FRAGMENT_SHADER = "" +
                "precision highp float;" +
                "const float TWO_PI = 6.283185307179586;" +
                "uniform sampler2D inputImageTexture;" +
                "uniform float amount;" +
                "uniform float time;" +
                "varying vec2 textureCoordinate;" +
                "vec2 rotate2D(vec2 position, float theta){" +
                "    mat2 m = mat2( cos(theta), -sin(theta), sin(theta), cos(theta) );" +
                "    return m * position;" +
                "}" +
                "void main() {" +
                "    vec2 p = textureCoordinate;" +
                "    vec2 sPos = textureCoordinate;" +
                "   vec2 off = texture2D( inputImageTexture, sPos ).rg - 0.5;" +
                "    float ang = time * TWO_PI;" +
                "    off = rotate2D(off,ang);p += off * amount;" +
                "    vec4 col = texture2D(inputImageTexture,p);gl_FragColor = col;" +
                "}"
    }
}