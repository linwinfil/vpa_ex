package com.at.lottie.gpu.gl

import android.opengl.GLES20
import com.at.lottie.IFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter

class HalfToneFilter : BaseGlitchFilter(FRAGMENT_SHADER), IFilter {
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

    companion object {
        private const val FRAGMENT_SHADER = "" +
                "precision highp float;" +
                "uniform vec2 center;" +
                "uniform float angle;" +
                "uniform float scale;" +
                "uniform vec2 tSize;" +
                "uniform sampler2D inputImageTexture;" +
                "varying vec2 textureCoordinate;" +
                "float pattern() {" +
                "    float s = sin( angle );" +
                "    float c = cos( angle );" +
                "    vec2 tex = textureCoordinate * tSize - center;" +
                "    vec2 point = vec2( c * tex.x - s * tex.y, s * tex.x + c * tex.y ) * scale;" +
                "    return ( sin( point.x ) * sin( point.y ) ) * 4.0;" +
                "}" +
                "void main() {" +
                "    vec4 color = texture2D( inputImageTexture, textureCoordinate );" +
                "    if(scale == 0.0) {" +
                "       gl_FragColor = color;" +
                "       return;" +
                "    } " +
                "    float average = ( color.r + color.g + color.b ) / 3.0;" +
                "    gl_FragColor = vec4( vec3( average * 10.0 - 5.0 + pattern() ), color.a );" +
                "}"
    }
}