package com.tencent.qgame.animplayer.gl

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLUtils
import com.tencent.qgame.animplayer.AnimConfig
import com.tencent.qgame.animplayer.PointRect
import com.tencent.qgame.animplayer.RenderConstant
import com.tencent.qgame.animplayer.gl.utils.GLUtils.checkGlError
import com.tencent.qgame.animplayer.util.GlFloatArray
import com.tencent.qgame.animplayer.util.ShaderUtil
import com.tencent.qgame.animplayer.util.TexCoordsUtil
import com.tencent.qgame.animplayer.util.VertexUtil
import java.lang.IllegalStateException

/**
 * Created by linmaoxin on 2021/12/15
 */
class ScreenFilter : IFilter {
    private val vertexArray = GlFloatArray()
    private val texCoordArray = GlFloatArray()

    private var shaderProgram = 0
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var surfaceSizeChanged = false

    private var aPositionLocation: Int = 0
    private var aCoordinateLocation: Int = 0
    private var uTextureLocation: Int = 0


    override fun setVertexBuf(config: AnimConfig) {
        vertexArray.setArray(VertexUtil.create(config.width, config.height, PointRect(0, 0, config.width, config.height), vertexArray.array))
    }

    override fun setTexCoordsBuf(config: AnimConfig) {
        val array = TexCoordsUtil.create(config.videoWidth, config.videoHeight, PointRect(0, 0, config.videoWidth, config.videoHeight), texCoordArray.array, true)
        texCoordArray.setArray(array)
    }

    override fun onInit(context: Context) {
        shaderProgram = ShaderUtil.createProgram(RenderConstant.SCREEN_VERTEX_SHADER, RenderConstant.SCREEN_FRAGMENT_SHADER)
        aPositionLocation = GLES20.glGetAttribLocation(shaderProgram, "vPosition") //顶点坐标
        aCoordinateLocation = GLES20.glGetAttribLocation(shaderProgram, "vCoordinate") //纹理坐标
        uTextureLocation = GLES20.glGetAttribLocation(shaderProgram, "vTexture") //纹理id
    }

    override fun onSurfaceSize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        surfaceWidth = width
        surfaceHeight = height
        surfaceSizeChanged = true
        if (surfaceWidth > 0 && surfaceHeight > 0 && surfaceSizeChanged) {
            GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
            surfaceSizeChanged = false
        }
    }

    override fun getProgram(): Int = shaderProgram

    override fun getTextureType(): Int = GLES20.GL_TEXTURE_2D

    override fun onDrawFrame(textureId: Int): Int {

        //启动
        GLES30.glUseProgram(shaderProgram)

        // 设置顶点坐标
        vertexArray.setVertexAttribPointer(aPositionLocation)
        // 设置纹理坐标
        checkGlError("aaa")
        texCoordArray.setVertexAttribPointer(aCoordinateLocation)
        checkGlError("bbb")

        //绑定纹理
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(getTextureType(), textureId)
        GLES20.glUniform1i(uTextureLocation, 0)

        //绘制
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        //解绑

        vertexArray.disableVertexAttribPointer(aPositionLocation)
        texCoordArray.disableVertexAttribPointer(aCoordinateLocation)

        GLES30.glBindTexture(getTextureType(), 0)
        GLES30.glUseProgram(0)

        return textureId
    }

    override fun onClearFrame() {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    }

    override fun onRelease() {

    }
}