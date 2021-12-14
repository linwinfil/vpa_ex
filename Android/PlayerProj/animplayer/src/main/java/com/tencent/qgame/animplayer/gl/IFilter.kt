package com.tencent.qgame.animplayer.gl

/**
 * Created by linmaoxin on 2021/12/14
 */
interface IFilter {
    fun onInit()
    fun onSurfaceSize(width: Int, height: Int)
    fun getProgram(): Int
    fun onDrawFrame(textureId: Int): Int
    fun onClearFrame()
    fun onRelease()
}