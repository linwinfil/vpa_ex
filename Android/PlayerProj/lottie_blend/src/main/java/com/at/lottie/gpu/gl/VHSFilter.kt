package com.at.lottie.gpu.gl

import com.at.lottie.R
import com.at.lottie.raw2String
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter

/**
 * Created by linmaoxin on 2021/12/31
 */
class VHSFilter : BaseGlitchFilter(R.raw.vhs_frag_shader.raw2String()) {

    override fun onInit() {
        super.onInit()
    }

    override fun onInitialized() {

    }

    override fun setIntensity(intensity: Float) {
    }

    override fun getFilter(): GPUImageFilter = this

    override fun doFrame(startFrame: Int, endFrame: Int, frame: Int, index: Int) {
        TODO("Not yet implemented")
    }

    override fun onDrawArraysPre() {
        com.at.lottie.utils.GLUtils.checkGlError("vhs gl error")
    }
}