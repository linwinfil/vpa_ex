package com.at.lottie.gpu.gl

import android.opengl.GLES20
import com.at.lottie.IFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter

class BadTVFilter : BaseGlitchFilter(FRAGMENT_SHADER), IFilter {
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
        setIntensity(intensity)
        setTime(calculateTimes(frame))
    }

    companion object {
        private const val FRAGMENT_SHADER = "" +
                "precision highp float;" +
                "uniform sampler2D inputImageTexture;" +
                "uniform float time;" +
                "uniform float distortion;" +
                "uniform float distortion2;" +
                "uniform float speed;" +
                "uniform float rollSpeed;" +
                "varying vec2 textureCoordinate;" +
                "vec3 mod289(vec3 x) {" +
                "    return x - floor(x * (1.0 / 289.0)) * 289.0;}vec2 mod289(vec2 x) {  return x - floor(x * (1.0 / 289.0)) * 289.0;" +
                "}" +
                "vec3 permute(vec3 x) {" +
                "    return mod289(((x*34.0)+1.0)*x);" +
                "}" +
                "float snoise(vec2 v)  {" +
                "    const vec4 C = vec4(0.211324865405187,  0.366025403784439, -0.577350269189626,  0.024390243902439);" +
                "    vec2 i  = floor(v + dot(v, C.yy) );" +
                "    vec2 x0 = v -   i + dot(i, C.xx);" +
                "    vec2 i1;" +
                "    i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);" +
                "    vec4 x12 = x0.xyxy + C.xxzz;" +
                "    x12.xy -= i1;" +
                "    i = mod289(i);" +
                "    vec3 p = permute( permute( i.y + vec3(0.0, i1.y, 1.0 )) + i.x + vec3(0.0, i1.x, 1.0 ));" +
                "    vec3 m = max(0.5 - vec3(dot(x0,x0), dot(x12.xy,x12.xy), dot(x12.zw,x12.zw)), 0.0);" +
                "    m = m*m ;" +
                "    m = m*m ;" +
                "   vec3 x = 2.0 * fract(p * C.www) - 1.0;" +
                "    vec3 h = abs(x) - 0.5;" +
                "    vec3 ox = floor(x + 0.5);" +
                "    vec3 a0 = x - ox;" +
                "    m *= 1.79284291400159 - 0.85373472095314 * ( a0*a0 + h*h );" +
                "    vec3 g;" +
                "    g.x  = a0.x  * x0.x  + h.x  * x0.y;" +
                "    g.yz = a0.yz * x12.xz + h.yz * x12.yw;" +
                "    return 130.0 * dot(m, g);" +
                "}" +
                "void main() {" +
                "    vec2 p = textureCoordinate;" +
                "    float ty = time * speed * 17.346;" +
                "    float yt = p.y - ty;" +
                "    float offset = snoise(vec2(yt*3.0,0.0))*0.2;" +
                "    offset = offset*distortion * offset*distortion * offset;" +
                "    offset += snoise(vec2(yt*50.0,0.0))*distortion2*0.002;" +
                "    gl_FragColor = texture2D(inputImageTexture,  vec2(fract(p.x + offset),fract(p.y - time * rollSpeed) ));" +
                "}"
    }
}