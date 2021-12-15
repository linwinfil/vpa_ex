package com.tencent.qgame.animplayer.gl

import android.opengl.GLES30
import kotlin.math.max

/**
 * Created by linmaoxin on 2021/12/14
 */
class GLFrameBuffer constructor(val bufferWidth: Int, val bufferHeight: Int, val bufferSize: Int = 1) {
    private val bufferArr: IntArray = IntArray(bufferSize)
    private val textureArr: IntArray = IntArray(bufferSize)

    init {
        //创建frame buffer
        GLES30.glGenFramebuffers(bufferSize, bufferArr, 0)
        //创建挂载颜色的纹理
        GLES30.glGenTextures(bufferSize, textureArr, 0)
        for (i in 0 until bufferSize) {
            // 创建挂载颜色缓冲纹理
            val textureId = textureArr[i]
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR.toFloat())
            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR.toFloat())
            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE.toFloat())
            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE.toFloat())

            //创建2d图像
            GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, bufferWidth, bufferHeight, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null)

            //绑定fbo
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, bufferArr[i])

            //fbo和纹理绑定
            GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, textureId, 0)

            //解绑
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        }
    }

    @Volatile
    private var currentIndex: Int = -1

    internal fun bindNext(index: Int = currentIndex, textureId: Int = GLES30.GL_NONE, isOesTexture: Boolean = false) {
        val newIndex = checkNextIndex(index)
        //绑定frame buffer
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, bufferArr[newIndex])
        if (textureId != GLES30.GL_NONE) {
            if (!isOesTexture) {
                GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, textureId, 0)
                /*GLES30.glColorMask(true, true, true, true)*/
            }
        } else {
            //挂载到一个纹理上
            GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, textureArr[newIndex], 0)
            /*//清空之前的颜色纹理
            GLES30.glColorMask(true, true, true, true)
            GLES30.glClearColor(0f, 0f, 0f, 0f)
            GLES30.glClear(GLES20.GL_COLOR_BUFFER_BIT)*/
            if (GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER) != GLES30.GL_FRAMEBUFFER_COMPLETE) {
                println("ERROR::FRAMEBUFFER:: Framebuffer is not complete!")
            }
        }
        currentIndex = newIndex
    }

    fun unbind(isOesTexture: Boolean = false) {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        if (!isOesTexture) GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, 0)
    }

    fun onDestroy() {
        GLES30.glDeleteFramebuffers(bufferSize, bufferArr, 0)
        GLES30.glDeleteTextures(bufferSize, textureArr, 0)
    }

    private fun checkNextIndex(index: Int): Int {
        return max(index + 1, 0) % bufferSize
    }

    fun getTextureId(): Int = textureArr[currentIndex]


}