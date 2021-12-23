package com.at.lottie

import android.os.Handler
import android.os.HandlerThread
import com.airbnb.lottie.LottieAnimationView

/**
 * Created by linmaoxin on 2021/12/23
 */
interface IBlend {
    fun initLottie()
    fun getDuration(): Float
    fun getFrameRate(): Float
    fun getEndFrame(): Float
    fun getStartFrame(): Float
    fun getLottieViewBg(): LottieAnimationView
    fun getLottieViewFg(): LottieAnimationView
}

/**
 * @param fileName 对应内置素材名
 * @param res 待替换的素材，File、Path、Bitmap、Uri
 */
data class ImageDelegate(val fileName: String, val res: Any)
data class HandlerHolder(var thread: HandlerThread?, var handler: Handler?) {
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