package com.at.lottie.gpu.gl

import android.opengl.GLES20
import com.at.lottie.IFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter

class RainbowFilter : BaseGlitchFilter(FRAGMENT_SHADER), IFilter {
    private var amountUniform = 0
    private var offsetUniform = 0
    private var intensity: Float = 0.3f
    override fun onInit() {
        super.onInit()
        amountUniform = GLES20.glGetUniformLocation(program, "amount")
        offsetUniform = GLES20.glGetUniformLocation(program, "offset")
    }

    override fun onInitialized() {
        super.onInitialized()
        runOnDraw {
            GLES20.glUniform1f(amountUniform, 0f)
            GLES20.glUniform1f(offsetUniform, 0f)
        }
    }

    override fun setTime(time: Float) {
        super.setTime(time)
    }

    override fun setIntensity(intensity: Float) {
        runOnDraw {
            GLES20.glUniform1f(amountUniform, intensity)
            GLES20.glUniform1f(offsetUniform, intensity)
        }
    }

    companion object {
        private const val FRAGMENT_SHADER = "" +
                "precision highp float;" +
                "uniform sampler2D inputImageTexture;" +
                "uniform float amount;" +
                "uniform float offset;" +
                "uniform float time;" +
                "varying vec2 textureCoordinate;" +
                "vec3 rainbow2( in float t ){" +
                "    vec3 d = vec3(0.0,0.33,0.67);" +
                "    return 0.5 + 0.5*cos( 6.28318*(t+d) );" +
                "}" +
                "void main() {" +
                "    vec2 p = textureCoordinate;" +
                "    vec3 origCol = texture2D( inputImageTexture, p ).rgb;" +
                "    vec2 off = texture2D( inputImageTexture, p ).rg - 0.5;p += off * offset;vec3 rb = rainbow2( (p.x + p.y + time * 2.0) * 0.5);" +
                "    vec3 col = mix(origCol,rb,amount);" +
                "    gl_FragColor = vec4(col, 1.0);" +
                "}"
    }

    override fun getFilter(): GPUImageFilter = this


    override fun doFrame(startFrame: Int, endFrame: Int, frame: Int, index: Int) {
        setIntensity(intensity)
        setTime(++count * 0.0167f * 0.5f)
    }
}