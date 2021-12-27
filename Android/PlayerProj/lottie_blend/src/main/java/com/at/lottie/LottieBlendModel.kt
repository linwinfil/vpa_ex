package com.at.lottie

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Application
import android.graphics.*
import android.net.Uri
import android.os.SystemClock
import android.util.Size
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.airbnb.lottie.ImageAssetDelegate
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieComposition
import com.at.lottie.gpu.GPUImageFilterGroupImpl
import com.at.lottie.gpu.GPUImageRendererImpl
import com.at.lottie.gpu.PixelBufferImpl
import com.at.lottie.utils.BlendUtils
import com.at.lottie.utils.logd
import com.at.lottie.utils.logi
import com.blankj.utilcode.util.ImageUtils
import com.coder.ffmpeg.call.IFFmpegCallBack
import com.coder.ffmpeg.jni.FFmpegCommand
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Created by linmaoxin on 2021/12/23
 */

class FramePicture(val frameIndex: Int, val type: FrameType) {
    private val picture = Picture()
    val width: Int get() = picture.width
    val height: Int get() = picture.height
    fun drawTo(canvas: Canvas) = picture.draw(canvas)
    fun drawOn(view: View) {
        val canvas = picture.beginRecording(view.width, view.height)
        canvas.clear()
        view.draw(canvas)
        picture.endRecording()
    }

}

internal fun Canvas.clear() = drawColor(0, PorterDuff.Mode.CLEAR)

class LottieBlendModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "${Constant.TAG}.BlendModel"
        private const val STEP_DRAW_1 = 1
        private const val STEP_FILTER_2 = 1 shl 1
        private const val STEP_MERGE_3 = 1 shl 2
    }

    lateinit var blend: IBlend

    /** 逐帧扫描 */
    val scanFramesLiveData by lazy { MutableLiveData<Int>() }

    /** 序列帧合并视频 */
    val mergeFramesLiveData by lazy { MutableLiveData<BlendState>() }

    /** 序列帧混合 */
    val blendFramesLiveData by lazy { MutableLiveData<Bitmap>() }

    /** 执行进度  */
    val progressLiveData by lazy { MutableLiveData<Int>() }

    /** 序列帧质量[30-100]*/
    val frameJpgQuality: Int = 100
        get() = max(min(100, field), 30)

    private var outPutSize:Int = -1

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

    private val blendThread: HandlerHolder by lazy {
        HandlerHolder(null, null).apply { createThread("blen_frame_thread") }
    }

    private val imageDelegates by lazy { mutableListOf<ImageDelegate>() }
    private val bgImageAssetDelegate by lazy {
        ImageAssetDelegate { asset ->
            imageDelegates.find { it.fileName == asset.fileName }?.let { imageAsset ->
                when (val res = imageAsset.res) {
                    is Bitmap, is String, is File, is Uri, is Int -> BlendUtils.scaleBitmap(application, asset.width, asset.height, res)
                    else -> null
                }
            }
        }
    }

    private val frameFilterDelegates by lazy { mutableListOf<FrameFilter>() }
    private val audioDelegate by lazy { mutableListOf<AudioDelegate>() }

    //20-50-30
    private fun postProgress(step: Int, progress: Int) {
        val lastProgress = progressLiveData.value ?: 0
        val fact = progress.toFloat() / 100
        val posProgress: Int = when (step) {
            STEP_DRAW_1 -> 0 + (20 * fact).roundToInt()
            STEP_FILTER_2 -> 20 + (50 * fact).roundToInt()
            STEP_MERGE_3 -> 70 + (30 * fact).roundToInt()
            else -> lastProgress
        }
        progressLiveData.postValue(posProgress)
    }


    // @WorkerThread
    private val blendFramesRunnable = Runnable {
        if (pictureBgArr.isNullOrEmpty()) {
            return@Runnable
        }
        val bgSize = pictureBgArr.size
        val fgSize = pictureFgArr.size
        logi(TAG, "bg frames:$bgSize, fg frames:$fgSize")
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val allTimes = SystemClock.elapsedRealtime()
        val dstSize = Size(pictureBgArr[0].width, pictureBgArr[0].height)
        var bgPixelBuffer: PixelBufferImpl? = null
        val filterGroup: GPUImageFilterGroupImpl? by lazy {
            frameFilterDelegates.mapNotNull { f -> if (f.frameType == FrameType.Background) f.filter else null }
                .takeUnless { it.isNullOrEmpty() }?.let { GPUImageFilterGroupImpl(it) }
        }
        val bgGpuRenderer: GPUImageRendererImpl? by lazy {
            filterGroup?.let { GPUImageRendererImpl(it) }
        }

        var doFrameFilter: Boolean = false
        pictureBgArr.forEachIndexed { index, bgPicture ->
            val elapsed = SystemClock.elapsedRealtime()
            //draw background
            val bgBitmap = Bitmap.createBitmap(bgPicture.width, bgPicture.height, Bitmap.Config.ARGB_8888)
            val bgCanvas = Canvas(bgBitmap)
            bgPicture.drawTo(bgCanvas)

            doFrameFilter = false
            frameFilterDelegates.forEach { frameFilter ->
                filterGroup?.getFilters()?.find { iFilter -> iFilter == frameFilter.filter }?.also { iFilter->
                    val inRange = frameFilter.inRange(bgPicture)
                    iFilter.setEnableDraw(inRange)
                    if (inRange) {
                        frameFilter.doFrame(bgPicture, index)
                        doFrameFilter = true
                    }
                }
            }
            if (doFrameFilter) {
                bgGpuRenderer?.also { render ->
                    bgPixelBuffer = bgPixelBuffer ?: run { PixelBufferImpl(dstSize.width, dstSize.height) }
                    bgPixelBuffer?.also { buffer ->
                        render.setImageBitmap(bgBitmap, false)
                        buffer.setRenderer(render)
                        buffer.clearBitmap()
                        buffer.getBitmap()?.also { newBitmap ->
                            bgCanvas.clear() //清除掉原来的
                            bgCanvas.drawBitmap(newBitmap, 0f, 0f, paint)
                        }
                    }
                }
            }
            //draw foreground

            val fgPicture = pictureFgArr[if (index >= pictureFgArr.size) pictureFgArr.size - 1 else index]
            //绘制到前景画板后再绘制到背景画板
            val fgBitmap = Bitmap.createBitmap(fgPicture.width, fgPicture.height, Bitmap.Config.ARGB_8888)
            val fgCanvas = Canvas(fgBitmap)
            fgPicture.drawTo(fgCanvas)
            //blend foreground and background
            bgCanvas.drawBitmap(fgBitmap, 0f, 0f, paint)

            //save sequence image to cache dir
            val path = "${cacheFile.absolutePath}/${String.format("%03d", index)}.jpg"
            val save = ImageUtils.save(bgBitmap, path, Bitmap.CompressFormat.JPEG, frameJpgQuality, false)
            logd(TAG, "blend frame:$index -> $save, ${SystemClock.elapsedRealtime() - elapsed}ms")
            blendFramesLiveData.postValue(bgBitmap)
            postProgress(STEP_FILTER_2, (index.toFloat() / bgSize * 100).roundToInt())
        }
        logd(TAG, "blend frame: ${SystemClock.elapsedRealtime() - allTimes}ms")
        bgPixelBuffer?.destroy()
        filterGroup?.onDestroy()
        System.gc()
        startMergeFramesStep()
    }

    // @WorkerThread
    private val mergeFramesRunnable = Runnable {
        System.gc()
        val realFrameRate = (pictureBgArr.size.toFloat() / frameCount * frameRate).roundToInt()
        logd(TAG, "merge frame rate:$realFrameRate")

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

        //todo lmx 合并音频文件
        FFmpegCommand.runCmd(cmdArgs, object : IFFmpegCallBack {
            override fun onCancel() {
                mergeFramesLiveData.postValue(OnCancel)
                logd(TAG, "onCancel: ")
            }

            override fun onComplete() {
                mergeFramesLiveData.postValue(OnComplete(outputPath))
                logd(TAG, "onComplete: ")
            }

            override fun onError(errorCode: Int, errorMsg: String?) {
                mergeFramesLiveData.postValue(OnError(errorCode, errorMsg))
                logd(TAG, "onError: $errorCode, $errorMsg")
            }

            override fun onProgress(progress: Int, pts: Long) {
                mergeFramesLiveData.postValue(OnProgress(progress, pts))
                postProgress(STEP_MERGE_3, progress)
                logd(TAG, "onProgress: $progress, $pts")
            }

            override fun onStart() {
                mergeFramesLiveData.postValue(OnStart)
                logd(TAG, "onStart: ")
            }
        })
    }

    override fun onCleared() {
        super.onCleared()
        pictureBgArr.clear()
        pictureFgArr.clear()
    }

    fun addReplaceImageDelegate(delegate: ImageDelegate) = apply { imageDelegates.add(delegate) }
    fun addFrameFilterDelegate(filter: FrameFilter) = apply {
        filter.apply {
            checkArgs()
            frameFilterDelegates.add(filter)
        }
    }

    fun addVideoAudioDelegate(delegate: AudioDelegate) = apply { audioDelegate.add(delegate) }

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
                pictureBgArr.add(FramePicture(frameBg, FrameType.Background).apply { drawOn(lottieViewBg) })
                pictureFgArr.add(FramePicture(frameFg, FrameType.Foreground).apply { drawOn(lottieViewFg) })
                logd(TAG, "onCreate: add picture ${++times} bgFrame:$frameBg, fgFrame:$frameFg, sync times:${SystemClock.elapsedRealtime() - elapseT}ms")
                scanFramesLiveData.postValue(lottieViewBg.frame)
                postProgress(STEP_DRAW_1, (value * 100).roundToInt())
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
                    startBlendFramesStep()
                }
            })
            start()
        }
    }

    private fun startBlendFramesStep() {
        blendThread.handler?.post(blendFramesRunnable)
    }

    private fun startMergeFramesStep() {
        blendThread.handler?.post(mergeFramesRunnable)
    }

}