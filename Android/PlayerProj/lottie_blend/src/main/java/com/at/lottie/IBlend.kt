package com.at.lottie

import android.os.Handler
import android.os.HandlerThread
import com.airbnb.lottie.LottieAnimationView
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter

/**
 * Created by linmaoxin on 2021/12/23
 */
interface IBlend {
    fun initLottie()
    fun getLottieViewBg(): LottieAnimationView
    fun getLottieViewFg(): LottieAnimationView
}

interface IFilter {
    fun doFrame(startFrame: Int, endFrame: Int, frame: Int)
}

sealed class Frame()

/**
 * 对指定帧序做滤镜处理
 * @param startFrame 起始帧序
 * @param endFrame 结束帧序
 * @param filter 滤镜
 * @param isBgFrame 是否是背景帧
 */
class FrameFilter(val startFrame: Int, val endFrame: Int, val filter: GPUImageFilter, val isBgFrame: Boolean = true) : Frame() {
    fun inFrameRange(frame: Int) = frame in startFrame..endFrame
    fun doFrame(frame: Int) {

    }
}

/**
 * @param fileName 对应内置素材名
 * @param res 待替换的素材，File、Path、Bitmap、Uri
 */
data class ImageDelegate(val fileName: String, val res: Any)
internal data class HandlerHolder(var thread: HandlerThread?, var handler: Handler?) {
    fun createThread(name: String): Boolean = runCatching {
        if (thread == null || thread?.isAlive == false) {
            thread = HandlerThread(name).apply {
                start()
                handler = Handler(looper)
            }
        }
        true
    }.getOrElse { it.printStackTrace();false }
}

sealed class BlendState
data class OnProgress(val progress: Int, val pts: Long) : BlendState()
data class OnError(val errorCode: Int, val errorMsg: String?) : BlendState()
data class OnComplete(val path: String) : BlendState()
object OnCancel : BlendState()
object OnStart : BlendState()