package com.at.lottie.gpu.gl

import android.opengl.GLES20
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter

class ShakeFilter : BaseGlitchFilter(FRAGMENT_SHADER), IGlitch {
    private var amountUniform = 0
    private var intensity = 0.4f

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
        runOnDraw { GLES20.glUniform1f(amountUniform, 0.159f * intensity) }
    }

    override fun getFilter(): GPUImageFilter {
        return this
    }


    override fun doFrame(startFrame: Int, endFrame: Int, frame: Int, index: Int) {
        setIntensity(intensity)
        setTime(++count * 0.0167f * 0.5f)
    }

    companion object {
        private const val FRAGMENT_SHADER = "" +
                "precision highp float;" +
                "uniform sampler2D inputImageTexture;" +
                "uniform float time;" +
                "uniform float amount;" +
                "varying vec2 textureCoordinate;" +
                "float random1d(float n){" +
                "    return fract(sin(n) * 43758.5453);" +
                "}" +
                "void main() {" +
                "    vec2 p = textureCoordinate;" +
                "    vec2 offset = (vec2(random1d(time),random1d(time + 999.99)) - 0.5) * amount;" +
                "    p += offset;gl_FragColor = texture2D(inputImageTexture, p);" +
                "}"
    }
}