package com.at.lottie.gpu.gl

import android.opengl.GLES20
import com.at.lottie.IFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter

class ScanlinesFilter : BaseGlitchFilter(FRAGMENT_SHADER), IFilter {
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

    override fun getFilter(): GPUImageFilter = this

    override fun doFrame(startFrame: Int, endFrame: Int, frame: Int, index: Int) {
        setIntensity(intensityFloat)
        setTime(calculateTimes(frame))
    }

    companion object {
        private const val FRAGMENT_SHADER = "" +
                "precision highp float;" +
                "uniform sampler2D inputImageTexture;" +
                "uniform float time;" +
                "uniform float count;" +
                "uniform float noiseAmount;" +
                "uniform float linesAmount;" +
                "uniform float height;" +
                "varying vec2 textureCoordinate;" +
                "highp float rand( const in vec2 uv ) {" +
                "  const highp float a = 12.9898;" +
                "  const highp float b = 78.233;" +
                "  const highp float c = 43758.5453;" +
                "    highp float dt = dot( uv.xy, vec2( a,b ) );" +
                "    highp float sn = mod( dt, 3.14159265359 );" +
                "    return fract(sin(sn) * c);" +
                "}" +
                "void main() {" +
                "    vec4 cTextureScreen = texture2D( inputImageTexture, textureCoordinate );" +
                "    float dx = rand( textureCoordinate + time );" +
                "    vec3 cResult = cTextureScreen.rgb * dx * noiseAmount;" +
                "    float lineAmount = height * 1.8 * count;" +
                "    vec2 sc = vec2( sin( textureCoordinate.y * lineAmount), cos( textureCoordinate.y * lineAmount) );" +
                "    cResult += cTextureScreen.rgb * vec3( sc.x, sc.y, sc.x ) * linesAmount;" +
                "    cResult = cTextureScreen.rgb + ( cResult );gl_FragColor =  vec4( cResult, cTextureScreen.a );" +
                "}"
    }
}