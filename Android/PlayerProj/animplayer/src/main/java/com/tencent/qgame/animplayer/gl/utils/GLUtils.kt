package com.tencent.qgame.animplayer.gl.utils

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import com.tencent.qgame.animplayer.util.ALog
import java.lang.RuntimeException

/**
 * Created by linmaoxin on 2021/12/20
 */
object GLUtils {
    fun checkArgument(width: Int, height: Int) {
        require(!(width <= 0 || height <= 0)) { "width or height must > 0" }
    }

    fun checkGlError(op: String) {
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            val msg = op + ": glError 0x" + Integer.toHexString(error)
            ALog.e("checkGlError", msg)
            throw RuntimeException(msg)
        }
    }

    fun createTexture(textureTarget: Int): Int {
        return createTexture(textureTarget, null, GLES20.GL_LINEAR, GLES20.GL_LINEAR,
            GLES20.GL_CLAMP_TO_EDGE, GLES20.GL_CLAMP_TO_EDGE, false)
    }

    fun createTexture(bitmap: Bitmap?, recycleBmp: Boolean): Int {
        return createTexture(GLES20.GL_TEXTURE_2D, bitmap, GLES20.GL_LINEAR, GLES20.GL_LINEAR,
            GLES20.GL_CLAMP_TO_EDGE, GLES20.GL_CLAMP_TO_EDGE, recycleBmp)
    }

    fun createTexture(textureTarget: Int, bitmap: Bitmap?, minFilter: Int,
                      magFilter: Int, wrapS: Int, wrapT: Int, recycleBmp: Boolean): Int {
        val textureHandle = IntArray(1)
        GLES20.glGenTextures(1, textureHandle, 0)
        checkGlError("glGenTextures")
        GLES20.glBindTexture(textureTarget, textureHandle[0])
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_MIN_FILTER, minFilter)
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_MAG_FILTER, magFilter)
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_S, wrapS)
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_T, wrapT)
        if (textureTarget == GLES20.GL_TEXTURE_2D && bitmap != null) {
            GLUtils.texImage2D(textureTarget, 0, bitmap, 0)
        }
        GLES20.glBindTexture(textureTarget, 0)
        if (recycleBmp) bitmap?.recycle()
        return textureHandle[0]
    }

}