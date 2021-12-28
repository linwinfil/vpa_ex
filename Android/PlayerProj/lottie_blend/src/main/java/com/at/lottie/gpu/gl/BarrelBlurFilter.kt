package com.at.lottie.gpu.gl

import android.opengl.GLES20
import com.at.lottie.IFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter

class BarrelBlurFilter : BaseGlitchFilter(FRAGMENT_SHADER), IFilter {
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
        runOnDraw { GLES20.glUniform1f(amountUniform, 0.159f * intensity) }
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
                "uniform float amount;" +
                "uniform float time;" +
                "varying vec2 textureCoordinate;" +
                "const int num_iter = 16;" +
                "const float reci_num_iter_f = 1.0 / float(num_iter);" +
                "const float gamma = 2.2;" +
                "const float MAX_DIST_PX = 200.0;" +
                "vec2 barrelDistortion( vec2 p, vec2 amt ){" +
                "    p = 2.0*p-1.0;" +
                "    const float maxBarrelPower = 3.0;" +
                "    float theta  = atan(p.y, p.x);" +
                "    float radius = length(p);" +
                "    radius = pow(radius, 1.0 + maxBarrelPower * amt.x);" +
                "    p.x = radius * cos(theta);" +
                "    p.y = radius * sin(theta);" +
                "    return 0.5 * ( p + 1.0 );" +
                "}" +
                "float sat( float t ){" +
                "    return clamp( t, 0.0, 1.0 );" +
                "}" +
                "float linterp( float t ) {" +
                "    return sat( 1.0 - abs( 2.0*t - 1.0 ) );" +
                "}" +
                "float remap( float t, float a, float b ) {" +
                "    return sat( (t - a) / (b - a) );" +
                "}" +
                "vec3 spectrum_offset( float t ) {" +
                "    vec3 ret;" +
                "    float lo = step(t,0.5);" +
                "    float hi = 1.0-lo;" +
                "    float w = linterp( remap( t, 1.0/6.0, 5.0/6.0 ) );" +
                "    ret = vec3(lo,1.0,hi) * vec3(1.0-w, w, 1.0-w);" +
                "   return pow( ret, vec3(1.0/2.2) );" +
                "}" +
                "float nrand( vec2 n ){" +
                "    return fract(sin(dot(n.xy, vec2(12.9898, 78.233)))* 43758.5453);" +
                "}" +
                "vec3 lin2srgb( vec3 c ){" +
                "    return pow( c, vec3(gamma) );" +
                "}" +
                "vec3 srgb2lin( vec3 c ){" +
                "    return pow( c, vec3(1.0/gamma));" +
                "}" +
                "void main() {" +
                "    vec2 uv = textureCoordinate;" +
                "    vec2 max_distort = vec2(amount);" +
                "    vec2 oversiz = barrelDistortion( vec2(1,1), max_distort );" +
                "    uv = 2.0 * uv - 1.0;" +
                "    uv = uv / (oversiz*oversiz);" +
                "    uv = 0.5 * uv + 0.5;" +
                "    vec3 sumcol = vec3(0.0);" +
                "    vec3 sumw = vec3(0.0);" +
                "    float rnd = nrand( uv + fract(time) );" +
                "    for ( int i=0; i<num_iter;++i ){" +
                "        float t = (float(i)+rnd) * reci_num_iter_f;" +
                "        vec3 w = spectrum_offset( t );" +
                "        sumw += w;" +
                "        sumcol += w * srgb2lin(texture2D( inputImageTexture, barrelDistortion(uv, max_distort*t ) ).rgb);" +
                "    }" +
                "     sumcol.rgb /= sumw;" +
                "    vec3 outcol = lin2srgb(sumcol.rgb);" +
                "    outcol += rnd/255.0;" +
                "    gl_FragColor = vec4( outcol, 1.0);" +
                "}"
    }
}