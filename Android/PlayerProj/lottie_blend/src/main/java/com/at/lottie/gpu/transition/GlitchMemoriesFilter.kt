package com.at.lottie.gpu.transition

import com.at.lottie.R
import com.at.lottie.gpu.gl.BaseGlitchFilter
import com.blankj.utilcode.util.ResourceUtils
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter

/**
 * Created by linmaoxin on 2021/12/31
 */


fun Int.transitionFrag(): String {
    val method: String = ResourceUtils.readRaw2String(this)
    return ResourceUtils.readRaw2String(R.raw.transition_base_frag_shader).run {
        this.replace("//#transition", method)
    }
}

class GlitchMemoriesFilter : BaseGlitchFilter(R.raw.transition_glitch_memories_frag_shader.transitionFrag()) {
    override fun setIntensity(intensity: Float) {
        TODO("Not yet implemented")
    }

    override fun getFilter(): GPUImageFilter {
        TODO("Not yet implemented")
    }

    override fun doFrame(startFrame: Int, endFrame: Int, frame: Int, index: Int) {
        TODO("Not yet implemented")
    }
}