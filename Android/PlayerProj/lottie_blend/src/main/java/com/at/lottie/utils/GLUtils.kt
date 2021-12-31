package com.at.lottie.utils

import android.opengl.GLES20
import java.lang.RuntimeException

/**
 * Created by linmaoxin on 2021/12/31
 */
object GLUtils {
    fun checkGlError(op: String) {
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            val msg = op + ": glError 0x" + Integer.toHexString(error)
            logd("checkGlError", msg)
            throw RuntimeException(msg)
        }
    }

}