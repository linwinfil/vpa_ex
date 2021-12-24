package com.at.lottie

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Application
import android.graphics.*
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.airbnb.lottie.ImageAssetDelegate
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieComposition
import com.at.lottie.utils.Utils
import com.blankj.utilcode.util.ImageUtils
import com.coder.ffmpeg.call.IFFmpegCallBack
import com.coder.ffmpeg.jni.FFmpegCommand
import jp.co.cyberagent.android.gpuimage.PixelBuffer
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Created by linmaoxin on 2021/12/23
 */

class FramePicture(val frameIndex: Int, val isBg: Boolean) {
    private val picture = Picture()
    val width: Int get() = picture.width
    val height: Int get() = picture.height
    fun drawTo(canvas: Canvas) = picture.draw(canvas)
    fun drawOn(view: View) {
        val canvas = picture.beginRecording(width, height)
        canvas.drawColor(Color.TRANSPARENT)
        view.draw(canvas)
        picture.endRecording()
    }

}

class LottieBlendModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "${Constant.TAG}.BlendModel"
    }

    lateinit var blend: IBlend

    /** 逐帧扫描 */
    val scanFramesLiveData by lazy { MutableLiveData<Int>() }

    /** 序列帧合并视频 */
    val mergeFramesLiveData by lazy { MutableLiveData<BlendState>() }

    /** 序列帧质量[30-100]*/
    val frameJpgQuality: Int = 100
        get() = max(min(100, field), 30)

    var dstWidth: Int = -1
    var dstHeight: Int = -1


    private val cacheFile = File(application.cacheDir, "lottie_seq_images")
    private val outputPath = "${application.cacheDir.absolutePath}/merge.mp4"

    private val duration: Float get() = compositionBg?.duration ?: 0.0f
    private val frameRate: Float get() = compositionBg?.frameRate ?: 0f
    private val endFrame: Float get() = compositionBg?.endFrame ?: 0f
    private val startFrame: Float get() = compositionBg?.startFrame ?: 0f
    private val frameCount: Int get() = (endFrame - startFrame).roundToInt()

    private val lottieViewBg: LottieAnimationView get() = blend.getLottieViewBg()
    private val lottieViewFg: LottieAnimationView get() = blend.getLottieViewFg()

    private var loadedCompositionFlag: Int = 0
    private var compositionBg: LottieComposition? = null
        set(value) {
            field = value
            loadedCompositionFlag = loadedCompositionFlag.or(1 shl 1)
        }
    private var compositionFg: LottieComposition? = null
        set(value) {
            field = value
            loadedCompositionFlag = loadedCompositionFlag.or(1 shl 2)
        }
    private val isLoadedComposition get() = loadedCompositionFlag.and(1 shl 1) != 0 && loadedCompositionFlag.and(1 shl 2) != 0

    private val pictureBgArr by lazy { mutableListOf<FramePicture>() }
    private val pictureFgArr by lazy { mutableListOf<FramePicture>() }


    @Volatile
    private var isScanningFrame = false

    @Volatile
    private var isDecodingFrame = false

    private var animator: ValueAnimator? = null

    private val decodeThread: HandlerHolder by lazy {
        HandlerHolder(null, null).apply { createThread("lottie_frame_thread") }
    }

    private val imageDelegates by lazy { mutableListOf<ImageDelegate>() }
    private val bgImageAssetDelegate by lazy {
        ImageAssetDelegate { asset ->
            imageDelegates.find { it.fileName == asset.fileName }?.let { imageAsset ->
                when (val res = imageAsset.res) {
                    is Bitmap, is String, is File, is Uri, is Int -> Utils.scaleBitmap(application, asset.width, asset.height, res)
                    else -> null
                }
            }
        }
    }

    private val frameFilterDelegates by lazy { mutableListOf<FrameFilter>() }
    private fun findFilterDelegate(frameIndex: Int, isBg: Boolean): FrameFilter? = frameFilterDelegates.find { it.inFrameRange(frameIndex) && it.isBgFrame == isBg }


    // @WorkerThread
    private val decodeFramesRunnable = Runnable {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val allTimes = SystemClock.elapsedRealtime()

        val pixelBuffer by lazy { PixelBuffer() }
        pictureBgArr.forEachIndexed { index, bgPicture ->
            val elapsed = SystemClock.elapsedRealtime()

            //draw background
            val bgBitmap = Bitmap.createBitmap(bgPicture.width, bgPicture.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bgBitmap)
            bgPicture.drawTo(canvas)

            findFilterDelegate(bgPicture.frameIndex, true)?.also {
                it.doFrame(bgPicture.frameIndex)
            }

            // do something filter
            //todo lmx

            //draw foreground
            val fgIndex = if (index >= pictureFgArr.size) pictureFgArr.size - 1 else index
            val fgPicture = pictureFgArr[fgIndex]
            val fgBitmap = Bitmap.createBitmap(fgPicture.width, fgPicture.height, Bitmap.Config.ARGB_8888).apply {
                fgPicture.drawTo(Canvas(this))
            }
            canvas.drawBitmap(fgBitmap, 0f, 0f, paint)

            //save sequence image to cache dir
            val path = "${cacheFile.absolutePath}/${String.format("%03d", index)}.jpg"
            val save = ImageUtils.save(bgBitmap, path,
                Bitmap.CompressFormat.JPEG, frameJpgQuality, true)
            Log.d(TAG, "decode frame:$index -> $save, ${SystemClock.elapsedRealtime() - elapsed}")
        }
        Log.d(TAG, "decode frame: ${SystemClock.elapsedRealtime() - allTimes}")
        startMergeFramesStep()
    }

    // @WorkerThread
    private val mergeFramesRunnable = Runnable {
        System.gc()
        val realFrameRate = (pictureBgArr.size.toFloat() / frameCount * frameRate).roundToInt()
        Log.d(TAG, "merge frame rate:$realFrameRate")

        //crf 0(无损) - 50(最大压缩)
        val cmdArgs: Array<String?> = arrayOf(
            "ffmpeg",
            "-framerate", "$realFrameRate",
            "-i", "${cacheFile.absolutePath}/%03d.jpg",
            "-pix_fmt", "yuv420p",
            "-crf", "15",
            "-vcodec", "mpeg4", //libx264 不行？
            // "-c:v", "libx264",
            "-b:v", "5000k", //视频比特率，越大文件越大
            // "-vpre", "default", //libx264 更好质量需要更长时间编码，default，normal，hq，max
            "-level", "4.0",
            "-bf", "0",
            // "-bufsize", "2000k",
            "-y", outputPath,
        )
        FFmpegCommand.runCmd(cmdArgs, object : IFFmpegCallBack {
            override fun onCancel() {
                mergeFramesLiveData.postValue(OnCancel)
                Log.d(TAG, "onCancel: ")
            }

            override fun onComplete() {
                mergeFramesLiveData.postValue(OnComplete(outputPath))
                Log.d(TAG, "onComplete: ")
            }

            override fun onError(errorCode: Int, errorMsg: String?) {
                mergeFramesLiveData.postValue(OnError(errorCode, errorMsg))
                Log.d(TAG, "onError: $errorCode, $errorMsg")
            }

            override fun onProgress(progress: Int, pts: Long) {
                mergeFramesLiveData.postValue(OnProgress(progress, pts))
                Log.d(TAG, "onProgress: $progress, $pts")
            }

            override fun onStart() {
                mergeFramesLiveData.postValue(OnStart)
                Log.d(TAG, "onStart: ")
            }
        })
    }

    override fun onCleared() {
        super.onCleared()
        pictureBgArr.clear()
        pictureFgArr.clear()
    }

    fun addReplaceImageDelegate(delegate: ImageDelegate) = apply { imageDelegates.add(delegate) }
    fun addFrameFilterDelegate(filter: FrameFilter) = apply { frameFilterDelegates.add(filter) }

    fun initLottie() {
        lottieViewBg.apply {
            addLottieOnCompositionLoadedListener { composition ->
                this.removeAllLottieOnCompositionLoadedListener()
                compositionBg = composition
            }
            setImageAssetDelegate(bgImageAssetDelegate)
        }
        lottieViewFg.apply {
            addLottieOnCompositionLoadedListener { composition ->
                this.removeAllLottieOnCompositionLoadedListener()
                compositionFg = composition
            }
        }
        blend.initLottie()
    }

    private fun stopScanFramesStep() {
        animator?.apply {
            removeAllListeners()
            removeAllUpdateListeners()
            cancel()
        }
        animator = null
    }

    fun startFrameBlend() {
        if (!isLoadedComposition || isScanningFrame) return
        stopScanFramesStep()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            var times = 0
            duration = this@LottieBlendModel.duration.toLong()
            interpolator = LinearInterpolator()
            addUpdateListener {
                if (!isScanningFrame) return@addUpdateListener
                isScanningFrame = true
                val value = it.animatedValue as Float
                val elapseT = SystemClock.elapsedRealtime()
                lottieViewBg.progress = value
                lottieViewFg.progress = value
                val frameBg = lottieViewBg.frame
                val frameFg = lottieViewFg.frame
                pictureBgArr.add(FramePicture(frameBg, true).apply { drawOn(lottieViewBg) })
                pictureFgArr.add(FramePicture(frameFg, false).apply { drawOn(lottieViewFg) })
                Log.d(TAG, "onCreate: add picture ${++times} bgFrame:$frameBg, fgFrame:$frameFg, sync times:${SystemClock.elapsedRealtime() - elapseT}")
                scanFramesLiveData.postValue(lottieViewBg.frame)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    isScanningFrame = true
                    pictureBgArr.clear()
                    pictureFgArr.clear()
                    times = 0
                }

                override fun onAnimationCancel(animation: Animator?) {
                    isScanningFrame = false
                }

                override fun onAnimationEnd(animation: Animator) {
                    animation.removeAllListeners()
                    (animation as ValueAnimator).removeAllUpdateListeners()
                    animator = null
                    isScanningFrame = false
                    startDecodeFramesStep()
                }
            })
            start()
        }
    }

    private fun startDecodeFramesStep() {
        decodeThread.handler?.post(decodeFramesRunnable)
    }

    private fun startMergeFramesStep() {
        decodeThread.handler?.post(mergeFramesRunnable)
    }

}