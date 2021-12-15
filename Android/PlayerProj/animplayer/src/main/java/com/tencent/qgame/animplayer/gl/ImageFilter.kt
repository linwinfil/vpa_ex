package com.tencent.qgame.animplayer.gl

import android.opengl.GLES20
import com.tencent.qgame.animplayer.RenderConstant
import com.tencent.qgame.animplayer.util.ShaderUtil

/**
 * Created by linmaoxin on 2021/12/14
 */
class ImageFilter : IFilter {
    private var shaderProgram = 0
    var frameBuffer: GLFrameBuffer? = null
    override fun onInit() {
        shaderProgram = ShaderUtil.createProgram(RenderConstant.IMAGE_VERTEX_SHADER, RenderConstant.IMAGE_FRAGMENT_SHADER)
    }

    override fun onSurfaceSize(width: Int, height: Int) {

    }

    override fun getProgram(): Int = shaderProgram
    override fun getTextureType(): Int = GLES20.GL_TEXTURE_2D

    override fun onDrawFrame(textureId: Int): Int {
        var oesId = textureId

        frameBuffer?.bindNext()

        oesId = frameBuffer?.let {
            it.unbind()
            it.getTextureId()
        } ?: oesId
        return oesId
    }

    override fun onClearFrame() {

    }

    override fun onRelease() {

    }
}