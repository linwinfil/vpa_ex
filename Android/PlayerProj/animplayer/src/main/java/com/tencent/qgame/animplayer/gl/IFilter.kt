package com.tencent.qgame.animplayer.gl

import android.content.Context
import android.opengl.GLES20
import android.util.Log
import com.tencent.qgame.animplayer.AnimConfig
import com.tencent.qgame.animplayer.util.ALog
import java.lang.RuntimeException

/**
 * Created by linmaoxin on 2021/12/14
 */
interface IFilter {
    fun onInit(context: Context)
    fun onSurfaceSize(width: Int, height: Int)
    fun getProgram(): Int
    fun getTextureType(): Int
    fun onDrawFrame(textureId: Int): Int
    fun onClearFrame()
    fun onRelease()

    fun setVertexBuf(config: AnimConfig) {}
    fun setTexCoordsBuf(config: AnimConfig) {}
}