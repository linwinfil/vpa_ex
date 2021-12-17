package com.tencent.qgame.animplayer.gl

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLES30
import com.tencent.qgame.animplayer.AnimConfig
import com.tencent.qgame.animplayer.PointRect
import com.tencent.qgame.animplayer.R
import com.tencent.qgame.animplayer.util.GlFloatArray
import com.tencent.qgame.animplayer.util.ShaderUtil
import com.tencent.qgame.animplayer.util.TexCoordsUtil
import com.tencent.qgame.animplayer.util.VertexUtil
import java.lang.IllegalStateException

/**
 * Created by linmaoxin on 2021/12/15
 */
class MixScreenFilter : IFilter {
    private val vertexArray = GlFloatArray()
    private val srcVerTexArray = GlFloatArray()
    private val srcTexCoordArray = GlFloatArray()

    private val oesVertexArray = GlFloatArray()
    private val oesTexCoorArray = GlFloatArray()

    private var shaderProgram = 0
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var surfaceSizeChanged = false

    private var aPositionLocation: Int = 0
    private var aSrcCoordinateLocation: Int = 0
    private var aOesCoordinateLocation: Int = 0
    private var uSrcTextureLocation: Int = 0
    private var uOesTextureLocation: Int = 0


    override fun setVertexBuf(config: AnimConfig) {
        vertexArray.setArray(VertexUtil.create(config.width, config.height, PointRect(0, 0, config.width, config.height), vertexArray.array))
    }

    override fun setTexCoordsBuf(config: AnimConfig) {
        val array = TexCoordsUtil.create(config.videoWidth, config.videoHeight,
            PointRect(0, 0, config.videoWidth, config.videoHeight), srcTexCoordArray.array, true)
        srcTexCoordArray.setArray(array)
    }

    override fun onInit(context: Context) {

        shaderProgram = ShaderUtil.createProgram(context, R.raw.mix_screen_vertex, R.raw.mix_screen_fragment)
        aPositionLocation = GLES20.glGetAttribLocation(shaderProgram, "a_Position") //顶点坐标
        aSrcCoordinateLocation = GLES20.glGetAttribLocation(shaderProgram, "a_TextureSrcCoordinates") //纹理坐标
        aOesCoordinateLocation = GLES20.glGetAttribLocation(shaderProgram, "a_TextureOesCoordionates") //OES坐标

        uSrcTextureLocation = GLES20.glGetAttribLocation(shaderProgram, "u_TextureSrc") //纹理id
        uOesTextureLocation = GLES20.glGetAttribLocation(shaderProgram, "u_TextureOes")
    }

    override fun onSurfaceSize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        surfaceWidth = width
        surfaceHeight = height
        surfaceSizeChanged = true
    }

    override fun getProgram(): Int = shaderProgram

    override fun getTextureType(): Int = GLES20.GL_TEXTURE_2D

    fun onDrawFrame(textureId: Int, oesTextureId: Int): Int {
        if (surfaceWidth > 0 && surfaceHeight > 0 && surfaceSizeChanged) {
            GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
            surfaceSizeChanged = false
        }
        //启动
        GLES30.glUseProgram(shaderProgram)

        // 设置顶点坐标
        vertexArray.setVertexAttribPointer(aPositionLocation)

        //2d 纹理坐标
        srcTexCoordArray.setVertexAttribPointer(aSrcCoordinateLocation)

        //oes 坐标
        oesTexCoorArray.setVertexAttribPointer(aOesCoordinateLocation)

        //绘制背景图，在绘制oes

        //绑定2d纹理
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(getTextureType(), textureId)
        GLES20.glUniform1i(uSrcTextureLocation, 0)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES20.glUniform1i(uOesTextureLocation, 1)

        //绘制
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        //解绑
        GLES30.glBindVertexArray(0)
        GLES30.glBindTexture(getTextureType(), 0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        GLES30.glUseProgram(0)

        return textureId
    }

    override fun onDrawFrame(textureId: Int): Int {
        throw IllegalStateException("call function onDrawFrame(Int,Int)")
    }

    override fun onClearFrame() {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    }

    override fun onRelease() {

    }
}