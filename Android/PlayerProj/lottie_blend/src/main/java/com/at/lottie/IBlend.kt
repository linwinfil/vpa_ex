package com.at.lottie

import android.os.Handler
import android.os.HandlerThread
import android.os.Parcelable
import com.airbnb.lottie.LottieAnimationView
import com.at.lottie.gpu.GpuFilters
import com.at.lottie.gpu.gl.IGlitch
import com.blankj.utilcode.util.ResourceUtils
import com.blankj.utilcode.util.Utils
import com.google.gson.annotations.SerializedName
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import java.io.ByteArrayOutputStream
import java.io.Serializable

/**
 * Created by linmaoxin on 2021/12/23
 */

// ====================================== //
interface IData : Serializable, Cloneable
class Blend : IData {
    @SerializedName("bg") var bg: Ground? = null
    @SerializedName("fg") var fg: Ground? = null
    @SerializedName("audio") var audio: Audio? = null

    fun toBgFrameFilters(): List<FrameFilter>? = bg?.toFrameFilters()
    fun toAudioDelegate(): AudioDelegate? = audio?.data.takeUnless { it.isNullOrEmpty() }?.let { AudioDelegate(it) }
}

class Audio : IData {
    @SerializedName("data") var data: String = ""
}

class Ground : IData {
    @SerializedName("data") var data: String = ""
    @SerializedName("images") var images: String = ""
    @SerializedName("filters") var filters: List<Filter?>? = null

    fun toFrameFilters():List<FrameFilter>? = filters?.mapNotNull { it?.toFrameFilter() }
}

class Filter : IData {
    @SerializedName("id") var id: Int = 0
    @SerializedName("start") var start: Int = 0
    @SerializedName("end" )var end: Int = -1
    @SerializedName("intensity") var intensity:Float = -1f

    fun toFrameFilter(): FrameFilter? = GpuFilters.getGpuFilter(id)?.run {
        FrameFilter(start, end, this, intensity = intensity)
    }
}
// ====================================== //

interface IBlend {
    fun initLottie()
    fun getLottieViewBg(): LottieAnimationView
    fun getLottieViewFg(): LottieAnimationView

    fun setAssetFile(fileName: String, imageAssetFolder: String, type: FrameType)
}

interface IFilter {

    /** 获取真实 filter */
    fun getFilter(): GPUImageFilter

    /** 是否能绘制 */
    fun isEnableDraw(): Boolean

    /** 设置能否绘制 */
    fun setEnableDraw(enable: Boolean)

    /**
     * @param startFrame 起始帧
     * @param endFrame 结束帧
     * @param frame 当前帧
     * @param index 当前帧下标
     */
    fun doFrame(startFrame: Int, endFrame: Int, frame: Int, index: Int)
}

fun Int.raw2String():String = ResourceUtils.readRaw2String(this)

enum class FrameType {
    Background, Foreground
}


/**
 * 对指定帧序做滤镜处理
 * @param startFrame 起始帧序 默认0起始
 * @param endFrame 结束帧序  默认-1结束
 * @param filter 滤镜
 * @param frameType [FrameType.Background],[FrameType.Foreground]
 */
class FrameFilter(
    private val startFrame: Int = 0, private val endFrame: Int = -1,
    val filter: IFilter, val frameType: FrameType = FrameType.Background, intensity: Float = -1f
) {
    init {
        if (intensity != -1f && filter is IGlitch) {
            filter.setIntensityValue(intensity)
        }
    }
    fun inRange(frame: FramePicture): Boolean {
        checkArgs()
        if (frameType != frame.type) return false
        return startFrame <= frame.frameIndex && (endFrame == -1 || frame.frameIndex <= endFrame)
    }

    fun doFrame(frame: FramePicture, index: Int) {
        filter.doFrame(startFrame, endFrame, frame.frameIndex, index)
    }

    internal fun checkArgs() {
        if (endFrame in 0 until startFrame) throw IllegalArgumentException("start frame must < end frame!")
        if (startFrame < 0) throw IllegalArgumentException("start frame must >= 0")
    }
}

/**
 * 背景音频
 * @param path String、File
 */
class AudioDelegate(val path: Any)

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
data class OnProgress(val progress: Int, val pts: Long, @LottieBlendModel.Companion.Step val step:Int) : BlendState()
data class OnError(val errorCode: Int, val errorMsg: String?) : BlendState()
data class OnComplete(val path: String) : BlendState()
object OnCancel : BlendState()
object OnStart : BlendState()