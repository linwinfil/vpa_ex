package com.tencent.qgame.animplayer.gl

import com.tencent.qgame.animplayer.RenderConstant
import com.tencent.qgame.animplayer.util.ShaderUtil

/**
 * Created by linmaoxin on 2021/12/14
 */
class ImageFilter(private val frameBuffer: GLFrameBuffer?) : IFilter {
    private var shaderProgram = 0

    override fun onInit() {
        shaderProgram = ShaderUtil.createProgram(RenderConstant.VERTEX_SHADER, RenderConstant.FRAGMENT_SHADER)
    }

    override fun onSurfaceSize(width: Int, height: Int) {

    }

    override fun getProgram(): Int = shaderProgram

    override fun onDrawFrame(textureId: Int): Int {
        var oesId = textureId

        frameBuffer?.bindNext()

        oesId = frameBuffer?.let {
            it.unbind()
            it.getCurrentTextureId()
        } ?: oesId
        return oesId
    }

    override fun onClearFrame() {

    }

    override fun onRelease() {

    }
}