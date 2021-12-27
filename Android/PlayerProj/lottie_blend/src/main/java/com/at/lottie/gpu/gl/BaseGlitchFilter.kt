package com.at.lottie.gpu.gl

import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import android.opengl.GLES20
import androidx.annotation.CallSuper
import com.at.lottie.IFilter
import java.nio.FloatBuffer

internal interface IGlitch {
    fun setTime(time: Float)
    fun setIntensity(intensity: Float)
    fun reset()
}

abstract class BaseGlitchFilter : GPUImageFilter, IGlitch, IFilter {
    private var timeUniform = 0

    internal open var enableDraw: Boolean = true
    internal open var count = 0

    constructor(fragmentShader: String?) : super(NO_FILTER_VERTEX_SHADER, fragmentShader) {}
    constructor(vertexShader: String?, fragmentShader: String?) : super(vertexShader, fragmentShader) {}

    override fun onInit() {
        super.onInit()
        timeUniform = GLES20.glGetUniformLocation(program, "time")
    }

    override fun onInitialized() {
        super.onInitialized()
        runOnDraw { GLES20.glUniform1f(timeUniform, 0f) }
    }

    @CallSuper
    override fun onDraw(textureId: Int, cubeBuffer: FloatBuffer?, textureBuffer: FloatBuffer?) {
        if (!enableDraw) return
        super.onDraw(textureId, cubeBuffer, textureBuffer)
    }

    override fun isEnableDraw(): Boolean {
        return enableDraw
    }

    override fun setEnableDraw(enable: Boolean) {
        enableDraw = enable
    }

    override fun setTime(time: Float) {
        runOnDraw { GLES20.glUniform1f(timeUniform, time) }
    }

    abstract override fun setIntensity(intensity: Float)
    override fun reset() {
        setTime(0f)
        setIntensity(0f)
        count = 0
    }
}