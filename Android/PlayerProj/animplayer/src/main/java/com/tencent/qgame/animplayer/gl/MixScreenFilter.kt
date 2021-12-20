package com.tencent.qgame.animplayer.gl

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES20
import com.tencent.qgame.animplayer.AnimConfig
import com.tencent.qgame.animplayer.PointRect
import com.tencent.qgame.animplayer.R
import com.tencent.qgame.animplayer.gl.utils.GLUtils.checkGlError
import com.tencent.qgame.animplayer.util.GlFloatArray
import com.tencent.qgame.animplayer.util.ShaderUtil
import com.tencent.qgame.animplayer.util.TexCoordsUtil
import com.tencent.qgame.animplayer.util.VertexUtil
import javax.microedition.khronos.opengles.GL11Ext

/**
 * Created by linmaoxin on 2021/12/15
 */
class MixScreenFilter : IFilter {
    private val positionVertexArray = GlFloatArray()
    private val bgVerTexArray = GlFloatArray()
    private val bgTexCoordArray = GlFloatArray()

    private val fgVertexArray = GlFloatArray()
    private val fgTexCoorArray = GlFloatArray()

    private var shaderProgram = 0
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var surfaceSizeChanged = false

    private var aPositionLocation: Int = 0
    private var aBgCoordinateLocation: Int = 0
    private var aFgCoordinateLocation: Int = 0
    private var uBgTextureLocation: Int = 0
    private var uFgTextureLocation: Int = 0


    override fun setVertexBuf(config: AnimConfig) {
        //定点坐标
        positionVertexArray.setArray(VertexUtil.create(config.width, config.height, PointRect(0, 0, config.width, config.height), positionVertexArray.array))

        //纹理坐标
        bgVerTexArray.setArray(VertexUtil.create(config.width, config.height, PointRect(0, 0, config.width, config.height), bgVerTexArray.array))
        fgVertexArray.setArray(VertexUtil.create(config.width, config.height, PointRect(0, 0, config.width, config.height), fgVertexArray.array))
    }

    override fun setTexCoordsBuf(config: AnimConfig) {
        bgTexCoordArray.setArray(TexCoordsUtil.create(config.videoWidth, config.videoHeight,
            PointRect(0, 0, config.videoWidth, config.videoHeight), bgTexCoordArray.array, true)
        )

        fgTexCoorArray.setArray(TexCoordsUtil.create(config.videoWidth, config.videoHeight,
            PointRect(0, 0, config.videoWidth, config.videoHeight), fgTexCoorArray.array, true)
        )
    }

    override fun onInit(context: Context) {

        shaderProgram = ShaderUtil.createProgram(context, R.raw.mix_screen_vertex, R.raw.mix_screen_fragment)
        aPositionLocation = GLES20.glGetAttribLocation(shaderProgram, "a_Position") //顶点坐标
        aBgCoordinateLocation = GLES20.glGetAttribLocation(shaderProgram, "a_TextureBgCoordinates") //纹理坐标
        aFgCoordinateLocation = GLES20.glGetAttribLocation(shaderProgram, "a_TextureFgCoordinates") //OES坐标

        uBgTextureLocation = GLES20.glGetUniformLocation(shaderProgram, "u_TextureBg") //纹理id
        uFgTextureLocation = GLES20.glGetUniformLocation(shaderProgram, "u_TextureFg")
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

    fun onDrawFrame(bgTextureId: Int, fgTextureId: Int): Int {

        //启动
        GLES20.glUseProgram(shaderProgram)

        // 设置顶点坐标
        checkGlError("aaa")
        positionVertexArray.setVertexAttribPointer(aPositionLocation)
        //纹理坐标
        bgTexCoordArray.setVertexAttribPointer(aBgCoordinateLocation)
        fgTexCoorArray.setVertexAttribPointer(aFgCoordinateLocation)

        //绑定纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(getTextureType(), bgTextureId)
        GLES20.glUniform1i(uBgTextureLocation, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(getTextureType(), fgTextureId)
        GLES20.glUniform1i(uFgTextureLocation, 1)


        // 融合
        // GLES20.glEnable(GLES20.GL_BLEND)
        // GLES20.glBlendFuncSeparate(GLES20.GL_SRC_COLOR, GLES20.GL_SRC_COLOR, GLES20.GL_ONE_MINUS_SRC_COLOR, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        // GLES20.glBlendEquation(GLES20.GL_FUNC_ADD)
        //绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        // GLES20.glDisable(GLES20.GL_BLEND)

        //解绑
        positionVertexArray.disableVertexAttribPointer(aPositionLocation)
        bgTexCoordArray.disableVertexAttribPointer(aBgCoordinateLocation)
        fgTexCoorArray.disableVertexAttribPointer(aFgCoordinateLocation)

        GLES20.glBindTexture(getTextureType(), 0)
        GLES20.glUseProgram(0)

        return bgTextureId
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