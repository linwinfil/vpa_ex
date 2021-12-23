package com.at.lottie

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Application
import android.graphics.*
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import android.view.animation.LinearInterpolator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.airbnb.lottie.ImageAssetDelegate
import com.airbnb.lottie.LottieAnimationView
import com.at.lottie.engine.ImageDelegate
import com.at.lottie.utils.Utils
import com.blankj.utilcode.util.ImageUtils
import com.coder.ffmpeg.call.IFFmpegCallBack
import com.coder.ffmpeg.jni.FFmpegCommand
import java.io.File
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Created by linmaoxin on 2021/12/23
 */
class LottieBlendModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "${Constant.TAG}.BlendModel"
    }

    lateinit var blend: IBlend

    val scanFramesLiveData by lazy { MutableLiveData<Any>() }
    val mergeFramesLiveData by lazy { MutableLiveData<BlendState>() }
    val frameJpgQuality: Int = 100
        get() = min(100, field)

    private val cacheFile = File(application.cacheDir, "lottie_seq_images")
    private val outputPath = "${application.cacheDir.absolutePath}/merge.mp4"

    private val duration: Float get() = blend.getDuration()
    private val frameRate: Float get() = blend.getFrameRate()
    private val endFrame: Float get() = blend.getEndFrame()
    private val startFrame: Float get() = blend.getStartFrame()
    private val frameCount: Int get() = (endFrame - startFrame).roundToInt()

    private val lottieViewBg: LottieAnimationView get() = blend.getLottieViewBg()
    private val lottieViewFg: LottieAnimationView get() = blend.getLottieViewFg()
    private val pictureBgArr by lazy { mutableListOf<Picture>() }
    private val pictureFgArr by lazy { mutableListOf<Picture>() }
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


    // @WorkerThread
    private val decodeFramesRunnable = Runnable {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        pictureBgArr.forEachIndexed { index, bgPicture ->
            val elapsed = SystemClock.elapsedRealtime()

            //draw background
            val bgBitmap = Bitmap.createBitmap(bgPicture.width, bgPicture.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bgBitmap)
            bgPicture.draw(canvas)

            // do something filter
            //todo lmx

            //draw foreground
            val fgIndex = if (index >= pictureFgArr.size) pictureFgArr.size - 1 else index
            val fgPicture = pictureFgArr[fgIndex]
            val fgBitmap = Bitmap.createBitmap(fgPicture.width, fgPicture.height, Bitmap.Config.ARGB_8888).apply {
                fgPicture.draw(Canvas(this))
            }
            canvas.drawBitmap(fgBitmap, 0f, 0f, paint)

            //save sequence image to cache dir
            val path = "${cacheFile.absolutePath}/${String.format("%03d", index)}.jpg"
            val save = ImageUtils.save(bgBitmap, path,
                Bitmap.CompressFormat.JPEG, frameJpgQuality, false)
            Log.d(TAG, "decode frame:$index -> $save, ${SystemClock.elapsedRealtime() - elapsed}")
        }
        startMergeFramesStep()
    }

    // @WorkerThread
    private val mergeFramesRunnable = Runnable {
        val realFrameRate = (pictureBgArr.size.toFloat() / frameCount * frameRate).roundToInt()
        Log.d(TAG, "merge frame rate:$realFrameRate")

        //crf 0(无损) - 50(最大压缩)
        val cmdArgs: Array<String?> = arrayOf(
            "ffmpeg",
            "-framerate", "${realFrameRate.toInt()}",
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

    fun replaceImageDelegate(delegate: ImageDelegate) = apply { imageDelegates.add(delegate) }

    fun initLottie() {
        lottieViewBg.setImageAssetDelegate(bgImageAssetDelegate)
        blend.initLottie()
    }

    fun stopScanFramesStep() {
        animator?.apply {
            removeAllListeners()
            removeAllUpdateListeners()
            cancel()
        }
        animator = null
    }

    fun startScanFramesStep() {
        stopScanFramesStep()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            var times = 0
            duration = this@LottieBlendModel.duration.toLong()
            interpolator = LinearInterpolator()
            addUpdateListener {
                val value = it.animatedValue as Float
                val elapseT = SystemClock.elapsedRealtime()
                lottieViewBg.progress = value
                lottieViewFg.progress = value
                Picture().apply {
                    val canvas = beginRecording(lottieViewBg.width, lottieViewBg.height)
                    canvas.drawColor(Color.TRANSPARENT)
                    lottieViewBg.draw(canvas)
                    endRecording()
                    pictureBgArr.add(this)
                }
                Picture().apply {
                    val canvas = beginRecording(lottieViewFg.width, lottieViewFg.height)
                    canvas.drawColor(Color.TRANSPARENT)
                    lottieViewFg.draw(canvas)
                    endRecording()
                    pictureFgArr.add(this)
                }
                Log.d(TAG, "onCreate: add picture ${++times} sync times:${SystemClock.elapsedRealtime() - elapseT}")
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    pictureBgArr.clear()
                    pictureFgArr.clear()
                    times = 0
                }

                override fun onAnimationEnd(animation: Animator) {
                    animation.removeAllListeners()
                    (animation as ValueAnimator).removeAllUpdateListeners()
                    animator = null
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