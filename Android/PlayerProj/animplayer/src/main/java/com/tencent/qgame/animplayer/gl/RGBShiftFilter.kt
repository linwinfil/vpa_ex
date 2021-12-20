package com.tencent.qgame.animplayer.gl

import android.content.Context
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.opengl.GLES20
import android.opengl.GLES30
import android.util.Log
import com.tencent.qgame.animplayer.AnimConfig
import com.tencent.qgame.animplayer.PointRect
import com.tencent.qgame.animplayer.R
import com.tencent.qgame.animplayer.gl.utils.GLUtils
import com.tencent.qgame.animplayer.util.GlFloatArray
import com.tencent.qgame.animplayer.util.ShaderUtil
import com.tencent.qgame.animplayer.util.TexCoordsUtil
import com.tencent.qgame.animplayer.util.VertexUtil

/**
 * Created by linmaoxin on 2021/12/15
 */
class RGBShiftFilter : IFilter {

    private val vertexArray = GlFloatArray()
    private val texCoordArray = GlFloatArray()

    private var shaderProgram = 0
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var surfaceSizeChanged = false

    private var aPositionLocation: Int = 0
    private var aCoordinateLocation: Int = 0
    private var uTextureLocation: Int = 0


    private var uAmountLocation: Int = 0
    private var uAngleLocation: Int = 0

    var bmpWidth = 0
    var bmpHeight = 0

    override fun setVertexBuf(config: AnimConfig) {
        val rect = Matrix().run {
            val srcRect = RectF(0f, 0f, bmpWidth.toFloat(), bmpHeight.toFloat())
            val dstRect = RectF(0f, 0f, surfaceWidth.toFloat(), surfaceHeight.toFloat())
            setRectToRect(srcRect, dstRect, Matrix.ScaleToFit.CENTER)
            mapRect(srcRect)
            Rect(srcRect.left.toInt(), srcRect.top.toInt(), srcRect.right.toInt(), srcRect.bottom.toInt())
        }
        val point = PointRect(rect.left, rect.top, rect.width(), rect.height())

        val array = VertexUtil.create(surfaceWidth, surfaceHeight, point, vertexArray.array)
        Log.d("vertexArray", "rect: $rect")
        Log.d("vertexArray", "setVertexBuf: ${array.asList()}")
        vertexArray.setArray(array)
    }

    override fun setTexCoordsBuf(config: AnimConfig) {
        val point = PointRect(0, 0, bmpWidth, bmpHeight)
        val array = TexCoordsUtil.create(bmpWidth, bmpHeight, point, texCoordArray.array)
        Log.d("texCoordArray", "setTexCoordsBuf: ${array.asList()}")
        texCoordArray.setArray(array)
    }

    fun setIntensity(intensity: Float) {
        GLES20.glUniform1f(uAmountLocation, 0.03f * intensity)
    }

    fun setTime(time: Float) {
        GLES20.glUniform1f(uAngleLocation, 10 * time)
    }

    override fun onInit(context: Context) {
        shaderProgram = ShaderUtil.createProgram(context, R.raw.image_vertex_shader, R.raw.rgb_shift_fragment)
        aPositionLocation = GLES20.glGetAttribLocation(shaderProgram, "vPosition") //顶点坐标
        aCoordinateLocation = GLES20.glGetAttribLocation(shaderProgram, "vCoordinate") //纹理坐标
        uTextureLocation = GLES20.glGetAttribLocation(shaderProgram, "vTexture") //纹理id

        uAmountLocation = GLES20.glGetAttribLocation(shaderProgram, "u_amount")
        uAngleLocation = GLES20.glGetAttribLocation(shaderProgram, "u_angle")

        setIntensity(0.01f)
        setTime(0f)
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
        GLES20.glUseProgram(shaderProgram)
        vertexArray.setVertexAttribPointer(aPositionLocation)   // 设置顶点坐标
        texCoordArray.setVertexAttribPointer(aCoordinateLocation) //纹理坐标

        //激活纹理
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLUtils.checkGlError("333")
        GLES30.glBindTexture(getTextureType(), textureId)
        GLES20.glUniform1i(uTextureLocation, 0)
        //绘制
        GLUtils.checkGlError("333")
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