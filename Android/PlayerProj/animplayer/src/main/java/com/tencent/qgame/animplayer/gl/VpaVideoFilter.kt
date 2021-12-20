package com.tencent.qgame.animplayer.gl

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES20
import com.tencent.qgame.animplayer.AnimConfig
import com.tencent.qgame.animplayer.PointRect
import com.tencent.qgame.animplayer.R
import com.tencent.qgame.animplayer.RenderConstant
import com.tencent.qgame.animplayer.gl.fbo.GLFrameBuffer
import com.tencent.qgame.animplayer.gl.utils.GLUtils
import com.tencent.qgame.animplayer.util.GlFloatArray
import com.tencent.qgame.animplayer.util.ShaderUtil
import com.tencent.qgame.animplayer.util.ShaderUtil.createProgram
import com.tencent.qgame.animplayer.util.TexCoordsUtil
import com.tencent.qgame.animplayer.util.VertexUtil

/**
 * Created by linmaoxin on 2021/12/14
 */
class VpaVideoFilter : IFilter {
    private val vertexArray = GlFloatArray()
    private val alphaArray = GlFloatArray()
    private val rgbArray = GlFloatArray()
    private var surfaceSizeChanged = false
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var shaderProgram = 0
    private var uTextureLocation: Int = 0
    private var aPositionLocation: Int = 0
    private var aTextureAlphaLocation: Int = 0
    private var aTextureRgbLocation: Int = 0

    override fun setVertexBuf(config: AnimConfig) {
        vertexArray.setArray(VertexUtil.create(config.width, config.height, PointRect(0, 0, config.width, config.height), vertexArray.array))
    }

    override fun setTexCoordsBuf(config: AnimConfig) {
        val alpha = TexCoordsUtil.create(config.videoWidth, config.videoHeight, config.alphaPointRect, alphaArray.array)
        val rgb = TexCoordsUtil.create(config.videoWidth, config.videoHeight, config.rgbPointRect, rgbArray.array)
        alphaArray.setArray(alpha)
        rgbArray.setArray(rgb)
    }

    override fun onInit(context: Context) {
        shaderProgram = createProgram(context, R.raw.default_vertex_shader, R.raw.vpa_fragment)
        uTextureLocation = GLES20.glGetUniformLocation(shaderProgram, "texture")
        aPositionLocation = GLES20.glGetAttribLocation(shaderProgram, "vPosition")
        aTextureAlphaLocation = GLES20.glGetAttribLocation(shaderProgram, "vTexCoordinateAlpha")
        aTextureRgbLocation = GLES20.glGetAttribLocation(shaderProgram, "vTexCoordinateRgb")
    }

    override fun onSurfaceSize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        surfaceWidth = width
        surfaceHeight = height
        surfaceSizeChanged = true
        if (surfaceSizeChanged && surfaceWidth > 0 && surfaceHeight > 0) {
            surfaceSizeChanged = false
            GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
        }
    }

    override fun getProgram(): Int = shaderProgram
    override fun getTextureType(): Int = GLES11Ext.GL_TEXTURE_EXTERNAL_OES
    override fun onDrawFrame(textureId: Int): Int {
        GLES20.glUseProgram(shaderProgram)
        // 设置顶点坐标
        vertexArray.setVertexAttribPointer(aPositionLocation)

        // 绑定纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(getTextureType(), textureId)
        GLES20.glUniform1i(uTextureLocation, 0)

        GLUtils.checkGlError("333")
        // 设置纹理坐标
        // alpha 通道坐标
        alphaArray.setVertexAttribPointer(aTextureAlphaLocation)
        // rgb 通道坐标
        rgbArray.setVertexAttribPointer(aTextureRgbLocation)

        GLUtils.checkGlError("333")
        // draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        alphaArray.disableVertexAttribPointer(aTextureAlphaLocation)
        rgbArray.disableVertexAttribPointer(aTextureRgbLocation)

        //解绑oes
        GLES20.glBindTexture(getTextureType(), 0)
        GLES20.glUseProgram(0)

        return textureId
    }

    override fun onClearFrame() {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    }

    override fun onRelease() {

    }
}