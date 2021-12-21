/*
 * Tencent is pleased to support the open source community by making vap available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * Licensed under the MIT License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tencent.qgame.animplayer

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLUtils
import com.tencent.qgame.animplayer.gl.IFilter
import com.tencent.qgame.animplayer.gl.MixScreenFilter
import com.tencent.qgame.animplayer.gl.RGBShiftFilter
import com.tencent.qgame.animplayer.gl.VpaVideoFilter
import com.tencent.qgame.animplayer.gl.fbo.GLFrameBuffer
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGammaFilter

class RenderAImpl(val context: Context, surfaceTexture: SurfaceTexture) : IRenderListener {

    companion object {
        private const val TAG = "${Constant.TAG}.RenderAImpl"
    }

    private val eglUtil: EGLUtil = EGLUtil()

    // 0 -> oes texture
    // 1 -> image bitmap texture
    private val genTexture = IntArray(2)

    private val bufferSize = 2
    private var frameBuffer: GLFrameBuffer? = null
    private val imageFilter by lazy { RGBShiftFilter() }
    private val vpaVideoFilter by lazy { VpaVideoFilter() }
    private val screenFilter by lazy { MixScreenFilter() }
    private val filters = arrayOf(imageFilter, vpaVideoFilter, screenFilter)

    init {
        eglUtil.start(surfaceTexture)
        initRender()
    }

    //顶点
    private fun setVertexBuf(config: AnimConfig) {
        filters.forEach { it.setVertexBuf(config) }
    }

    //纹理
    private fun setTexCoords(config: AnimConfig) {
        filters.forEach { it.setTexCoordsBuf(config) }
    }

    override fun initRender() {
        filters.forEach { it.onInit(context) }

        GLES20.glGenTextures(genTexture.size, genTexture, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, genTexture[0])
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        val bitmap = BitmapFactory.decodeStream(context.assets.open("lottie/images/img_0.png"))
        imageFilter.bmpWidth = bitmap.width
        imageFilter.bmpHeight = bitmap.height

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, genTexture[1])
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat())
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat())
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        bitmap.recycle()
    }

    override fun renderFrame() {
        // 绑定fbo上绘制后取出fbo的纹理id
        frameBuffer?.bindNext(true)
        vpaVideoFilter.onDrawFrame(genTexture[0])
        frameBuffer?.unbind()
        val fgTexture: Int = frameBuffer?.getTextureId() ?: genTexture[0]

        frameBuffer?.bindNext(true)
        imageFilter.onDrawFrame(genTexture[1]) //fbo的纹理id
        frameBuffer?.unbind()
        val bgTexture: Int = frameBuffer?.getTextureId() ?: genTexture[1]

        screenFilter.onDrawFrame(bgTexture, fgTexture)
    }

    fun setRgbShiftIntensity(intensity: Float) {
        if (imageFilter is RGBShiftFilter) {
            (imageFilter as RGBShiftFilter).setIntensity(intensity)
        }
    }

    override fun clearFrame() {
        filters.forEach { it.onClearFrame() }
        eglUtil.swapBuffers()
    }

    override fun destroyRender() {
        releaseTexture()
        eglUtil.release()
        frameBuffer?.onDestroy()
    }

    override fun releaseTexture() {
        filters.forEach { it.onRelease() }
        GLES20.glDeleteTextures(genTexture.size, genTexture, 0)
    }

    /**
     * 设置视频配置
     */
    override fun setAnimConfig(config: AnimConfig) {
        setVertexBuf(config)
        setTexCoords(config)
    }

    /**
     * 显示区域大小变化
     */
    override fun updateViewPort(width: Int, height: Int) {
        frameBuffer?.also { it.onDestroy() }
        if (width > 0 && height > 0) {
            frameBuffer = GLFrameBuffer(width, height, bufferSize)
        }
        filters.forEach { it.onSurfaceSize(width, height) }
    }

    override fun swapBuffers() {
        eglUtil.swapBuffers()
    }

    /**
     * mediaCodec渲染使用的
     */
    override fun getExternalTexture(): Int {
        return genTexture[0]
    }

}