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

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.view.ViewParent
import com.tencent.qgame.animplayer.gl.GLFrameBuffer
import com.tencent.qgame.animplayer.gl.VpaVideoFilter
import com.tencent.qgame.animplayer.util.GlFloatArray
import com.tencent.qgame.animplayer.util.ShaderUtil
import com.tencent.qgame.animplayer.util.TexCoordsUtil
import com.tencent.qgame.animplayer.util.VertexUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer

class RenderAImpl(surfaceTexture: SurfaceTexture) : IRenderListener {

    companion object {
        private const val TAG = "${Constant.TAG}.RenderAImpl"
    }

    private val eglUtil: EGLUtil = EGLUtil()
    private val genTexture = IntArray(1)

    private var frameBuffer: GLFrameBuffer? = null
    private val vpaVideoFilter by lazy { VpaVideoFilter() }

    init {
        eglUtil.start(surfaceTexture)
        initRender()
    }

    private fun setVertexBuf(config: AnimConfig) {
        vpaVideoFilter.setVertexBuf(config)
    }

    private fun setTexCoords(config: AnimConfig) {
        vpaVideoFilter.setTexCoords(config)
    }

    override fun initRender() {
        vpaVideoFilter.onInit()

        GLES20.glGenTextures(genTexture.size, genTexture, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, genTexture[0])
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    override fun renderFrame() {
        vpaVideoFilter.onDrawFrame(genTexture[0])
    }

    override fun clearFrame() {
        vpaVideoFilter.onClearFrame()
        eglUtil.swapBuffers()
    }

    override fun destroyRender() {
        releaseTexture()
        eglUtil.release()
    }

    override fun releaseTexture() {
        vpaVideoFilter.onRelease()
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
        // frameBuffer?.also { it.onDestroy() }
        // frameBuffer = GLFrameBuffer(width, height, 1)
        // vpaVideoFilter.frameBuffer = frameBuffer
        vpaVideoFilter.onSurfaceSize(width, height)
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