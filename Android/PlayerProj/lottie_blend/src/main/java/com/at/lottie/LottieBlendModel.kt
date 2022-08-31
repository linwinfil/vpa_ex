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
import androidx.annotation.IntDef
import androidx.annotation.MainThread
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
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.ImageUtils
import com.blankj.utilcode.util.ResourceUtils
import com.blankj.utilcode.util.Utils
import com.coder.ffmpeg.call.IFFmpegCallBack
import com.coder.ffmpeg.jni.FFmpegCommand
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
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

internal fun Canvas.clear() = drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
inline fun <reified State> createChannel(): Channel<State> = Channel(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

class LottieBlendModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "${Constant.TAG}.BlendModel"

        @Retention(AnnotationRetention.SOURCE)
        @IntDef(STEP_DRAW_1, STEP_FILTER_2, STEP_MERGE_3)
        annotation class Step
        const val STEP_DRAW_1 = 1
        const val STEP_FILTER_2 = 1 shl 1
        const val STEP_MERGE_3 = 1 shl 2
    }

    lateinit var blend: IBlend

    /** 逐帧扫描 */
    val scanFramesLiveData by lazy { MutableLiveData<Int>() }

    /** 序列帧合并视频 */
    val mergeFramesLiveData by lazy { MutableLiveData<BlendState>() }

    /** 序列帧混合 */
    val blendFramesLiveData by lazy { MutableLiveData<Bitmap>() }

    private val _blendFrameFlow = createChannel<Bitmap>()
    val bendFrameFlow = _blendFrameFlow.receiveAsFlow()

    /** 执行进度  */
    val progressLiveData by lazy { MutableLiveData<BlendState>() }

    /** 序列帧质量[30-100]*/
    val frameJpgQuality: Int = 100
        get() = max(min(100, field), 30)

    private val tempCacheFile = File(application.cacheDir, "lottie_blend")
    private val tempCacheImages = File(tempCacheFile, "lottie_seq_images")
    private val tempOutputPath = "${tempCacheFile.absolutePath}/merge.mp4"

    private val duration: Float get() = compositionBg?.duration ?: 0.0f
    private val frameRate: Float get() = compositionBg?.frameRate ?: 0f
    private val endFrame: Float get() = compositionBg?.endFrame ?: 0f
    private val startFrame: Float get() = compositionBg?.startFrame ?: 0f
    private val frameCount: Int get() = (endFrame - startFrame).roundToInt()

    private val lottieViewBg: LottieAnimationView get() = blend.getLottieViewBg()
    private val lottieViewFg: LottieAnimationView get() = blend.getLottieViewFg()

    private var loadedCompositionFlag: Int = 0
    private fun addLoadedCompositionFlag(flag:Int) {
        loadedCompositionFlag =  loadedCompositionFlag.or(1 shl flag)
    }
    private var compositionBg: LottieComposition? = null
        set(value) {
            field = value
            addLoadedCompositionFlag(1)
        }
    private var compositionFg: LottieComposition? = null
        set(value) {
            field = value
            addLoadedCompositionFlag(2)
        }
    private val isLoadedBgComposition get() = loadedCompositionFlag.and(1 shl 1) != 0
    private val isLoadedFgComposition get() = loadedCompositionFlag.and(1 shl 2) != 0

    private val pictureBgArr by lazy { mutableListOf<FramePicture>() }
    private val pictureFgArr by lazy { mutableListOf<FramePicture>() }


    private var isScanningFrame = false

    @Volatile private var stopBlend = false
    private val lock = Object()
    private var stopFlag: Int = 0
    private fun addStopFlag(flag:Int) {
        stopFlag = stopFlag.or(1 shl flag)
    }

    private val animator: ValueAnimator = ValueAnimator.ofFloat(0f, 1f).also {
        it.interpolator = LinearInterpolator()
    }
    private fun ValueAnimator.reset() {
        removeAllListeners()
        removeAllUpdateListeners()
        cancel()
    }
    private val animatorUpdateListener = ValueAnimator.AnimatorUpdateListener {
        if (stopBlend || !isScanningFrame) {
            postOnCancel()
            stopScanFramesStep()
            return@AnimatorUpdateListener
        }
        isScanningFrame = true
        val value = it.animatedValue as Float
        val elapseT = SystemClock.elapsedRealtime()
        lottieViewBg.progress = value
        val frameBg = lottieViewBg.frame
        pictureBgArr.add(FramePicture(frameBg, FrameType.Background).apply { drawOn(lottieViewBg) })

        val frameFg = if (isLoadedFgComposition) {
            lottieViewFg.progress = value
            val frameFg = lottieViewFg.frame
            pictureFgArr.add(FramePicture(frameFg, FrameType.Foreground).apply { drawOn(lottieViewFg) })
            frameFg
        } else 0

        logd(TAG, "bgFrame:$frameBg, fgFrame:$frameFg, sync times:${SystemClock.elapsedRealtime() - elapseT}ms")
        scanFramesLiveData.postValue(lottieViewBg.frame)
        postOnProgress(STEP_DRAW_1, (value * 100).roundToInt())
    }
    private val animatorListener = object : AnimatorListenerAdapter() {
        override fun onAnimationStart(animation: Animator) {
            isScanningFrame = true
            pictureBgArr.clear()
            pictureFgArr.clear()
        }

        override fun onAnimationCancel(animation: Animator?) {
            isScanningFrame = false
        }

        override fun onAnimationEnd(animation: Animator) {
            (animation as ValueAnimator).reset()
            isScanningFrame = false
            if (stopBlend) {
                postOnCancel()
                stopBlend = false
            } else {
                startBlendFramesStep()
            }
        }
    }

    private val blendThread: HandlerHolder by lazy {
        HandlerHolder(null, null).apply { createThread("blen_frame_thread") }
    }

    private val imageDelegates by lazy { mutableListOf<ImageDelegate>() }
    private val bgImageAssetDelegate by lazy {
        ImageAssetDelegate { asset ->
            imageDelegates.find {
                it.fileName.equals(asset.fileName, true)
                        || it.fileName.substringBeforeLast(".").equals(asset.fileName.substringBeforeLast("."), true)
            }?.let { imageAsset ->
                when (val res = imageAsset.res) {
                    is Bitmap, is String, is File, is Uri, is Int -> BlendUtils.scaleBitmap(application, asset.width, asset.height, res)
                    else -> null
                }
            }
        }
    }

    private val frameFilterDelegates by lazy { mutableListOf<FrameFilter>() }
    private val audioDelegate by lazy { mutableListOf<AudioDelegate>() }

    private fun postOnStart() {
        logd("$TAG.progress", "OnStart")
        progressLiveData.postValue(OnStart)
    }

    private fun postOnCancel() {
        logd("$TAG.progress", "OnCancel")
        progressLiveData.postValue(OnCancel)
    }

    private fun postOnError(code: Int, msg: String?) {
        logd("$TAG.progress", "OnError")
        progressLiveData.postValue(OnError(code, msg))
    }

    private fun postOnCompleted() {
        logd("$TAG.progress", "OnComplete")
        progressLiveData.postValue(OnComplete(tempOutputPath))
    }

    private fun postOnProgress(step: Int, progress: Int, pts: Long = 0) {
        //按照20-50-30进度分割
        val fact = progress.toFloat() / 100
        val posProgress: Int = when (step) {
            STEP_DRAW_1 -> 0 + (20 * fact).roundToInt()
            STEP_FILTER_2 -> 20 + (50 * fact).roundToInt()
            STEP_MERGE_3 -> 70 + (30 * fact).roundToInt()
            else -> progressLiveData.value?.takeIf { it is OnProgress }?.let { (it as OnProgress).progress } ?: 0
        }
        logd("$TAG.progress", "progress:$posProgress")
        progressLiveData.postValue(OnProgress(posProgress, 0, step))
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

        tempCacheImages.deleteRecursively()

        var bgPixelBuffer: PixelBufferImpl? = null
        val filterGroup: GPUImageFilterGroupImpl? by lazy {
            frameFilterDelegates.mapNotNull { f -> if (f.frameType == FrameType.Background) f.filter else null }
                .takeUnless { it.isNullOrEmpty() }?.let { GPUImageFilterGroupImpl(it) }
        }
        val bgGpuRenderer: GPUImageRendererImpl? by lazy {
            filterGroup?.let { GPUImageRendererImpl(it) }
        }

        var stopMerge = false
        var doFrameFilter: Boolean

        var backgroundBitmap: Bitmap? = null
        var backgroundCanvas: Canvas? = null

        var foregroundBitmap: Bitmap? = null
        var foregroundCanvas: Canvas? = null

        pictureBgArr.forEachIndexed { index, bgPicture ->
            synchronized(lock) {
                if (stopBlend) {
                    stopMerge = true
                    stopBlend = false
                    postOnCancel()
                    return@forEachIndexed
                }

                val elapsed = SystemClock.elapsedRealtime()
                //draw background
                backgroundBitmap = backgroundBitmap?.apply {
                    this.eraseColor(Color.TRANSPARENT)
                }?: run { Bitmap.createBitmap(bgPicture.width, bgPicture.height, Bitmap.Config.ARGB_8888) }
                val bgBitmap = backgroundBitmap!!

                backgroundCanvas = backgroundCanvas?.apply {
                    clear()
                }?: run { Canvas(bgBitmap) }
                val bgCanvas = backgroundCanvas!!
                bgPicture.drawTo(bgCanvas)

                doFrameFilter = false
                frameFilterDelegates.forEach { frameFilter ->
                    filterGroup?.getFilters()?.find { iFilter -> iFilter == frameFilter.filter }?.also { iFilter ->
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

                if (pictureFgArr.isNotEmpty()) {
                    //draw foreground
                    val fgPicture = pictureFgArr[if (index >= pictureFgArr.size) pictureFgArr.size - 1 else index]
                    //绘制到前景画板后再绘制到背景画板

                    foregroundBitmap = foregroundBitmap?.apply {
                        eraseColor(Color.TRANSPARENT)
                    }?: run { Bitmap.createBitmap(fgPicture.width, fgPicture.height, Bitmap.Config.ARGB_8888) }
                    val fgBitmap = foregroundBitmap!!
                    foregroundCanvas = foregroundCanvas?.apply { clear() }?:run { Canvas(fgBitmap) }
                    val fgCanvas = foregroundCanvas!!

                    fgPicture.drawTo(fgCanvas)
                    //blend foreground and background
                    bgCanvas.drawBitmap(fgBitmap, 0f, 0f, paint)
                }

                //save sequence image to cache dir
                val path = "${tempCacheImages.absolutePath}/${String.format("%03d", index)}.jpg"
                val save = ImageUtils.save(bgBitmap, path, Bitmap.CompressFormat.JPEG, frameJpgQuality, false)
                logd(TAG, "blend frame:$index -> $save, ${SystemClock.elapsedRealtime() - elapsed}ms")
                blendFramesLiveData.postValue(bgBitmap)
                postOnProgress(STEP_FILTER_2, (index.toFloat() / bgSize * 100).roundToInt())
            }
        }
        logd(TAG, "blend frame: ${SystemClock.elapsedRealtime() - allTimes}ms")
        bgPixelBuffer?.destroy()
        filterGroup?.onDestroy()
        System.gc()
        if (!stopMerge) {
            startMergeFramesStep()
        }
    }

    // @WorkerThread
    private val mergeFramesRunnable = Runnable {
        System.gc()
        com.blankj.utilcode.util.FileUtils.delete(tempOutputPath)

        synchronized(lock) {
            if (stopBlend) {
                stopBlend = false
                postOnCancel()
                return@Runnable
            }
        }

        val tempAudioFile: File? = audioDelegate.lastOrNull()?.let { delegate ->
            when (val p = delegate.path) {
                is File -> p.takeIf { it.exists() }?.let {
                    File("${tempCacheFile}/audio.${p.absolutePath.substringAfterLast(".")}").apply {
                        delete()
                        FileUtils.copy(it, this)
                    }
                }
                is String -> {
                    if (!FileUtils.isFileExists(p)) {
                        if (hasAssetFile(p)) {
                            File("${tempCacheFile}/audio.${p.substringAfterLast(".")}").apply {
                                delete()
                                ResourceUtils.copyFileFromAssets(p, this.absolutePath)
                            }
                        } else null
                    } else {
                        File("${tempCacheFile}/audio.${p.substringAfterLast(".")}").apply {
                            delete()
                            FileUtils.copy(File(p), this)
                        }
                    }
                }
                else -> null
            }
        }
        tempAudioFile?.apply { logd(TAG, "temp audio path:${this.absolutePath}") }

        val realFrameRate = /*(pictureBgArr.size.toFloat() / frameCount * frameRate).roundToInt()*/60
        logd(TAG, "merge frame rate:$realFrameRate")


        //[ffmpeg](https://trac.ffmpeg.org/wiki/Slideshow)
        //crf 0(无损) - 50(最大压缩)

        val args = mutableListOf<String?>().apply {
            add("ffmpeg")
            add("-framerate");add("$realFrameRate")
            add("-i");add("${tempCacheImages.absolutePath}/%03d.jpg")
            tempAudioFile?.also {
                add("-i");add(it.absolutePath)
            }
            add("-pix_fmt");add("yuv420p")
            add("-crf");add("15")
            add("-c:v");add("libx264")
            add("-b:v");add("5000k") //视频比特率，越大文件越大
            add("-preset");add("medium")//编码预值，veryslow，slow，medium，fast和veryfast，越慢压缩率更好
            add("-c:a");add("copy")
            add("-level");add("4.0")
            add("-bf");add("0")
            add("-y");add(tempOutputPath)
        }
        val cmdArgs: Array<String?> = args.toTypedArray()
        FFmpegCommand.setDebug(BuildConfig.DEBUG)
        FFmpegCommand.runCmd(cmdArgs, object : IFFmpegCallBack {
            override fun onCancel() {
                mergeFramesLiveData.postValue(OnCancel)
                postOnCancel()
                logd(TAG, "onCancel: ")
            }

            override fun onComplete() {
                mergeFramesLiveData.postValue(OnComplete(tempOutputPath))
                postOnCompleted()
                logd(TAG, "onComplete: ")
            }

            override fun onError(errorCode: Int, errorMsg: String?) {
                mergeFramesLiveData.postValue(OnError(errorCode, errorMsg))
                postOnError(errorCode, errorMsg)
                logd(TAG, "onError: $errorCode, $errorMsg")
            }

            override fun onProgress(progress: Int, pts: Long) {
                synchronized(lock) {
                    if (stopBlend) {
                        stopBlend = false
                        FFmpegCommand.cancel()
                        postOnCancel()
                        return
                    }
                    mergeFramesLiveData.postValue(OnProgress(progress, pts, STEP_MERGE_3))
                    postOnProgress(STEP_MERGE_3, progress, pts)
                    logd(TAG, "onProgress: $progress, $pts")
                }
            }

            override fun onStart() {
                mergeFramesLiveData.postValue(OnStart)
                logd(TAG, "onStart: ")
            }
        })
    }

    private fun hasAssetFile(fileName:String):Boolean {
        return runCatching {
            Utils.getApp().resources.assets.open(fileName)
            true
        }.getOrElse { false }
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
    fun addFrameFilterDelegate(filters:List<FrameFilter>) = apply {
        filters.forEach {
            it.checkArgs()
            frameFilterDelegates.add(it)
        }
    }
    fun removeAllFrameFilterDelegate() = apply { frameFilterDelegates.clear() }

    fun addVideoAudioDelegate(delegate: AudioDelegate) = apply { audioDelegate.add(delegate) }

    fun setAssetFile(fileName: String, imageAssetFolder: String, type: FrameType) {
        blend.setAssetFile(fileName, imageAssetFolder, type)
    }

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
        animator.reset()
    }

    @MainThread
    fun stopBlend() {
        stopBlend = true
        stopScanFramesStep()
    }

    @MainThread
    fun startBlend() {
        if (!isLoadedBgComposition || isScanningFrame) return
        stopScanFramesStep()
        animator.apply {
            duration = this@LottieBlendModel.duration.toLong()
            addUpdateListener(animatorUpdateListener)
            addListener(animatorListener)
            postOnStart()
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