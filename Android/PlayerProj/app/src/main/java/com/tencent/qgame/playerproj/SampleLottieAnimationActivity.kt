package com.tencent.qgame.playerproj

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Picture
import android.os.*
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieDrawable
import com.blankj.utilcode.util.ImageUtils
import com.coder.ffmpeg.annotation.CodecAttribute
import com.coder.ffmpeg.call.IFFmpegCallBack
import com.coder.ffmpeg.jni.FFmpegCommand
import com.tencent.qgame.playerproj.databinding.ActivitySampleLottieAnimationBinding
import com.tencent.qgame.playerproj.gl.RGBShiftFilter
import jp.co.cyberagent.android.gpuimage.GPUImageRenderer
import jp.co.cyberagent.android.gpuimage.PixelBuffer
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import okio.buffer
import okio.source
import java.io.File
import kotlin.math.roundToInt

class SampleLottieAnimationActivity : AppCompatActivity() {
    lateinit var bind: ActivitySampleLottieAnimationBinding

    companion object {
        private const val TAG = "SampleLottieAnimationAc"
    }

    var times = 0
    private val bgpictureArray = mutableListOf<Picture>()
    private val fgpictureArray = mutableListOf<Picture>()
    private val cacheFile by lazy {
        File(this.cacheDir, "cache_lottie_seq")
    }
    private val lottieViewBg get() = bind.lottieViewBg
    private val lottieViewFg get() = bind.lottieViewFg
    private val ivPreviewBg get() = bind.ivPreviewBg
    private val ivPreviewFg get() = bind.ivPreviewFg
    private var duration: Long = 0
    private var frameRate: Float = 0.0f
    private var frameCount: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivitySampleLottieAnimationBinding.inflate(layoutInflater)
        setContentView(bind.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), 0x11)
        }

        lottieViewBg.addLottieOnCompositionLoadedListener {
            duration = it.duration.toLong()
            frameRate = it.frameRate
            frameCount = (it.endFrame - it.startFrame).roundToInt()
            bind.btnStart.visibility = View.VISIBLE
            bind.btnMergeMp4.visibility = View.VISIBLE
            StringBuilder()
                .append("duration:").append(it.duration)
                .append(",startFrame:").append(it.startFrame)
                .append(",endFrame:").append(it.endFrame)
                .append(",frameRate:").append(it.frameRate)
                .toString().apply {
                    Log.d(TAG, "lottie composition: $this")
                }
        }
        bind.btnStart.setOnClickListener { startShow(duration) }
        bind.btnProcess.setOnClickListener { startProcess() }
        bind.btnMergeMp4.setOnClickListener { startMergeVideo() }
        bind.btnFfmpegFormat.setOnClickListener { startLogFFmpegFormat() }
    }

    private fun startShow(duration: Long) {
        File(cacheDir.absolutePath).deleteRecursively()
        lottieViewBg.isDrawingCacheEnabled = true
        ValueAnimator.ofFloat(0f, 1f).setDuration(duration).apply {
            interpolator = LinearInterpolator()
            addUpdateListener {
                val elapseT = SystemClock.elapsedRealtime()
                lottieViewBg.progress = it.animatedValue as Float
                lottieViewFg.progress = it.animatedValue as Float
                Picture().apply {
                    val canvas = beginRecording(lottieViewBg.width, lottieViewBg.height)
                    lottieViewBg.draw(canvas)
                    endRecording()
                    bgpictureArray.add(this)
                }
                Picture().apply {
                    val canvas = beginRecording(lottieViewFg.width, lottieViewFg.height)
                    lottieViewFg.draw(canvas)
                    endRecording()
                    fgpictureArray.add(this)
                }
                Log.d(TAG, "onCreate: add picture ${++times} sync:${SystemClock.elapsedRealtime() - elapseT}")
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    lottieViewBg.postOnAnimation { bind.btnProcess.visibility = View.VISIBLE }
                }
            })
            start()
        }
    }

    private fun startProcess() {
        val handlerThread = HandlerThread("process_handler_thread").apply { start() }
        val processHandler = ProcessHandler(lottieViewBg.width, lottieViewBg.height, handlerThread.looper)
        processHandler.sendEmptyMessage(0)
    }

    private inner class ProcessHandler(val width: Int, val height: Int, looper: Looper) : Handler(looper) {
        @WorkerThread
        override fun handleMessage(msg: Message) {

            val times = SystemClock.elapsedRealtime()
            val pixelBuffer = PixelBuffer(width, height)
            val filter = RGBShiftFilter(GPUImageFilter.NO_FILTER_VERTEX_SHADER,
                this@SampleLottieAnimationActivity.resources.openRawResource(R.raw.rgb_shift_fragment).source().buffer().readUtf8())
            val renderer = GPUImageRenderer(filter)
            var count = 0
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            bgpictureArray.forEachIndexed { index, picture ->
                var bgBitmap = Bitmap.createBitmap(picture.width, picture.height, Bitmap.Config.ARGB_8888).apply {
                    picture.draw(Canvas(this))
                }
                renderer.setImageBitmap(bgBitmap, true)
                filter.setIntensity(0.2f)
                filter.setTime(++count * 0.0167f * 0.5f)
                pixelBuffer.setRenderer(renderer)
                bgBitmap = pixelBuffer.bitmap

                val fgIndex = if (index >= fgpictureArray.size) fgpictureArray.size - 1 else index
                val fgPicture = fgpictureArray[fgIndex]
                val fgBitmap = Bitmap.createBitmap(fgPicture.width, fgPicture.height, Bitmap.Config.ARGB_8888).apply {
                    fgPicture.draw(Canvas(this))
                }
                val canvas = Canvas(bgBitmap)
                canvas.drawBitmap(fgBitmap, 0f, 0f, paint)

                val path = "${cacheFile.absolutePath}/${String.format("%03d", index)}.jpg"
                runOnUiThread { ivPreviewBg.setImageBitmap(bgBitmap) }
                runOnUiThread { ivPreviewFg.setImageBitmap(fgBitmap) }
                val save = ImageUtils.save(bgBitmap, path,
                    Bitmap.CompressFormat.JPEG, 80, false)
                Log.d(TAG, "handleMessage: save $index -> $save")
            }
            Log.d(TAG, "handleMessage: finish!! ${(SystemClock.elapsedRealtime() - times) / 1000f}")
            runOnUiThread { bind.btnMergeMp4.visibility = View.VISIBLE }
        }
    }

    private fun startMergeVideo() {
        Thread {
            val realFrameRate = /*pictureArray.size.toFloat() / frameCount * frameRate*/60f
            //crf 0(无损) - 50(最大压缩) ，推荐29
            var cmdArgs: Array<String?>
            cmdArgs = arrayOf(
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
                "-y", "${this.cacheDir.absolutePath}/merge.mp4"
            )
            FFmpegCommand.runCmd(cmdArgs, object : IFFmpegCallBack {
                override fun onCancel() {
                    Log.d(TAG, "onCancel: ")
                }

                override fun onComplete() {
                    Log.d(TAG, "onComplete: ")
                }

                override fun onError(errorCode: Int, errorMsg: String?) {
                    Log.d(TAG, "onError: $errorCode, $errorMsg")
                }

                override fun onProgress(progress: Int, pts: Long) {
                    Log.d(TAG, "onProgress: $progress, $pts")
                }

                override fun onStart() {
                    Log.d(TAG, "onStart: ")
                }
            })
        }.start()
    }

    private fun startLogFFmpegFormat() {
        // val inputFormat = FFmpegCommand.getSupportFormat(FormatAttribute.INPUT_FORMAT)
        // val outputFormat = FFmpegCommand.getSupportFormat(FormatAttribute.INPUT_FORMAT)
        // Log.d(TAG, "startLogFFmpegFormat: inputFormat:$inputFormat, outputFormat:$outputFormat")

        val encodeFormat = FFmpegCommand.getSupportCodec(CodecAttribute.ENCODE_VIDEO)
        Log.d(TAG, "startLogFFmpegFormat: encodeFormat:${encodeFormat}")
    }
}