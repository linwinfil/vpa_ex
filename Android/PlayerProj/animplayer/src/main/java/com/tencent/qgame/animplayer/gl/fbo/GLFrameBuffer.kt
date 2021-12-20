package com.tencent.qgame.animplayer.gl.fbo

import android.opengl.GLES20
import com.tencent.qgame.animplayer.gl.utils.GLUtils
import com.tencent.qgame.animplayer.gl.utils.GLUtils.checkArgument
import com.tencent.qgame.animplayer.gl.utils.GLUtils.checkGlError
import kotlin.math.max

/**
 * Created by linmaoxin on 2021/12/14
 */
class GLFrameBuffer constructor(bufferWidth: Int, bufferHeight: Int, private val bufferSize: Int = 1) {
    private val bufferIds: IntArray = IntArray(bufferSize)
    private val textureIds: IntArray = IntArray(bufferSize)

    init {
        checkArgument(bufferWidth, bufferHeight)

        GLES20.glGenTextures(bufferSize, textureIds, 0)
        GLES20.glGenFramebuffers(bufferSize, bufferIds, 0)

        for (i in 0 until bufferSize) {
            //创建fbo所需纹理
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[i])
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat())
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat())
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                bufferWidth, bufferHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)

            //创建fbo
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, bufferIds[i])
            // 为fbo挂载texture来存储颜色
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textureIds[i], 0)
            checkFrameBufferStatus()

            // 解绑FrameBuffer和纹理
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
            checkGlError("ggg")
        }
    }

    private var currentIndex: Int = -1
    private fun checkNextIndex(index: Int): Int {
        return max(index + 1, 0) % bufferSize
    }

    private fun checkFrameBufferStatus() {
        val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            println("ERROR::FRAMEBUFFER:: Framebuffer is not complete! -> $status")
        }
    }

    internal fun bindNext(clear: Boolean = false) {
        val newIndex = checkNextIndex(currentIndex)
        //绑定frame buffer
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, bufferIds[newIndex])
        checkFrameBufferStatus()
        if (clear) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        }
        checkGlError("bind frame error:${newIndex}")
        currentIndex = newIndex
    }

    fun unbind() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    fun onDestroy() {
        GLES20.glDeleteFramebuffers(bufferSize, bufferIds, 0)
        GLES20.glDeleteTextures(bufferSize, textureIds, 0)
    }


    fun getTextureId(): Int = textureIds[currentIndex]
    fun getBufferId(): Int = bufferIds[currentIndex]


}