package com.tencent.qgame.animplayer.gl

import android.opengl.GLES20
import android.util.Log
import com.tencent.qgame.animplayer.util.ALog
import java.lang.RuntimeException

/**
 * Created by linmaoxin on 2021/12/14
 */
interface IFilter {
    fun onInit()
    fun onSurfaceSize(width: Int, height: Int)
    fun getProgram(): Int
    fun getTextureType(): Int
    fun onDrawFrame(textureId: Int): Int
    fun onClearFrame()
    fun onRelease()
}

fun checkGlError(op: String) {
    val error = GLES20.glGetError()
    if (error != GLES20.GL_NO_ERROR) {
        val msg = op + ": glError 0x" + Integer.toHexString(error)
        ALog.e("checkGlError", msg)
        throw RuntimeException(msg)
    }
}
