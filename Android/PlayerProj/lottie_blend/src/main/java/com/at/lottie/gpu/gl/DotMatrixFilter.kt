package com.at.lottie.gpu.gl

import android.opengl.GLES20
import com.at.lottie.IFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter

class DotMatrixFilter : BaseGlitchFilter(FRAGMENT_SHADER), IFilter {
    private var dotsUniform = 0
    private var sizeUniform = 0
    private var blurUniform = 0
    override fun onInit() {
        super.onInit()
        dotsUniform = GLES20.glGetUniformLocation(program, "dots")
        sizeUniform = GLES20.glGetUniformLocation(program, "size")
        blurUniform = GLES20.glGetUniformLocation(program, "blur")
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

    companion object {
        private const val FRAGMENT_SHADER = "" +
                "precision highp float;" +
                "uniform sampler2D inputImageTexture;" +
                "uniform float dots;" +
                "uniform float size;" +
                "uniform float blur;" +
                "varying vec2 textureCoordinate;" +
                "void main() {" +
                "    if(dots == 0.0) {" +
                "       gl_FragColor = texture2D(inputImageTexture, textureCoordinate);" +
                "       return;" +
                "    } " +
                "    float dotSize = 1.0/dots;" +
                "    vec2 samplePos = textureCoordinate - mod(textureCoordinate, dotSize) + 0.5 * dotSize;" +
                "    float distanceFromSamplePoint = distance(samplePos, textureCoordinate);" +
                "    vec4 col = texture2D(inputImageTexture, samplePos);" +
                "    gl_FragColor = mix(col, vec4(0.0), smoothstep(dotSize * size, dotSize *(size + blur), distanceFromSamplePoint));" +
                "}"
    }
}