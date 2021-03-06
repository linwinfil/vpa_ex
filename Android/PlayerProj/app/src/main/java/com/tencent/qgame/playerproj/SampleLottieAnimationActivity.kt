package com.tencent.qgame.playerproj

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.*
import android.os.*
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.annotation.DrawableRes
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.ImageUtils
import com.coder.ffmpeg.annotation.CodecAttribute
import com.coder.ffmpeg.call.IFFmpegCallBack
import com.coder.ffmpeg.jni.FFmpegCommand
import com.tencent.qgame.playerproj.databinding.ActivitySampleLottieAnimationBinding
import com.at.lottie.gpu.gl.RGBShiftFilter
import com.at.lottie.utils.append
import com.at.lottie.utils.logd
import com.coder.ffmpeg.utils.FFmpegUtils
import jp.co.cyberagent.android.gpuimage.GPUImageRenderer
import jp.co.cyberagent.android.gpuimage.PixelBuffer
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
        File(this.cacheDir, "cache_lottie_seq").apply { mkdir() }
    }
    private val cacheFile2 by lazy {
        File(this.cacheDir, "lottie_blend/lottie_seq_images")
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

        lottieViewBg.setImageAssetDelegate {
            Log.d(TAG, "setImageAssetDelegate: $it")
            when {
                it.fileName == "img_0.jpg" || it.id == "image_0" -> {
                    replaceImg(it.width, it.height, R.drawable.r_img_1)
                }
                it.fileName == "img_1.png" || it.id == "image_1" -> {
                    replaceImg(it.width, it.height, R.drawable.r_img_2)
                }
                else -> null
            }
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
        bind.btnVideoBright.setOnClickListener { startVideoBright() }
    }

    private fun replaceImg(width: Int, height: Int, @DrawableRes resId: Int): Bitmap {
        return BitmapFactory.decodeResource(resources, resId).let { src ->
            Bitmap.createBitmap(width, height, src.config).apply {
                val matrix = Matrix()
                val scale: Float
                var dx = 0f
                var dy = 0f
                if (src.width * height > width * src.height) {
                    scale = height.toFloat() / src.height.toFloat()
                    dx = (width - src.width * scale) * 0.5f
                } else {
                    scale = width.toFloat() / src.width.toFloat()
                    dy = (height - src.height * scale) * 0.5f
                }
                matrix.setScale(scale, scale)
                matrix.postTranslate(dx.roundToInt().toFloat(), dy.roundToInt().toFloat())
                Canvas(this).drawBitmap(src, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
            }
        }
    }

    private fun startShow(duration: Long) {
        bind.btnProcess.visibility = View.GONE
        fgpictureArray.clear()
        bgpictureArray.clear()
        File(cacheDir.absolutePath).deleteRecursively()
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
                    animation.removeAllListeners()
                    (animation as ValueAnimator).removeAllUpdateListeners()
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
            val filter = RGBShiftFilter()
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
            //crf 0(??????) - 50(????????????) ?????????29


            // val inputTxt = File("${cacheDir.absolutePath}/input.txt").apply { delete() }
            // inputTxt.createNewFile()
            // val list = cacheFile.list()
            // val buffer = inputTxt.sink().buffer()
            // list.forEachIndexed { index, s ->
            //     val isLast = index == list.size - 1
            //     val string = "${cacheFile.absolutePath}/${String.format("%03d", index)}.jpg"
            //     buffer.writeUtf8(string)
            //     if (!isLast) {
            //         buffer.writeUtf8("\n")
            //     }
            // }
            // buffer.close()

            File("${filesDir.absolutePath}/merge.mp4").apply {
                delete()
                createNewFile()
            }
            File("${cacheFile.absolutePath}/merge.mp4").apply {
                delete()
                createNewFile()
            }

            val args = mutableListOf<String?>().apply {
                append("ffmpeg")
                append("-framerate").append("$realFrameRate")
                append("-i").append("${cacheFile.absolutePath}/%03d.jpg")
                append("-pix_fmt").append("yuv420p")
                append("-crf").append("15")
                append("-vcodec").append("mpeg4")
                append("-b:v").append("5000k") //????????????????????????????????????
                // a("-vpre").a("default") //libx264 ???????????????????????????????????????default???normal???hq???max
                append("-level").append("4.0")
                append("-bf").append("0")
                append("-y").append("${cacheDir.absolutePath}/merge.mp4")


                //libx264
                clear()
                // append("ffmpeg")
                append("-y")
                append("-f").append("image2")
                append("-framerate").append("60")
                append("-i").append("${cacheFile2.absolutePath}/%03d.jpg")
                append("-c:v").append("libx264")
                append("-crf").append("15")
                append("-pix_fmt").append("yuv420p")
                // a("-b:v").a("5000k") //????????????????????????????????????
                append("${cacheFile.absolutePath}/merge.mp4")

            }
            var cmdArgs: Array<String?> = args.toTypedArray()
            logd(TAG, "$cmdArgs")
            FFmpegCommand.setDebug(true)
            FFmpegCommand.runCmd(cmdArgs, object : IFFmpegCallBack {
                override fun onCancel() {
                    Log.d(TAG, "onCancel: ")
                }

                override fun onComplete() {
                    Log.d(TAG, "onComplete: ")
                    runOnUiThread {
                        val intent = Intent(this@SampleLottieAnimationActivity, SampleVideoViewActivity::class.java)
                        intent.putExtra("video", "${this@SampleLottieAnimationActivity.cacheDir.absolutePath}/merge.mp4")
                        this@SampleLottieAnimationActivity.startActivity(intent)
                    }
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
    private fun startVideoBright() {
        Thread {
            val srcFile = File(this.applicationContext.filesDir, "video.mp4")
            val targetFile = File(this.applicationContext.filesDir, "video_bright.mp4").apply { delete() }
            val cmdArg = FFmpegUtils.videoBright(srcFile.absolutePath, targetFile.absolutePath, 0.1f)
            FFmpegCommand.runCmd(cmdArg, object : IFFmpegCallBack {
                override fun onCancel() {
                    Log.d(TAG, "onCancel: ")
                }

                override fun onComplete() {
                    Log.d(TAG, "onComplete: ")
                    runOnUiThread {
                        val intent = Intent(this@SampleLottieAnimationActivity, SampleVideoViewActivity::class.java)
                        intent.putExtra("video", targetFile.absolutePath)
                        this@SampleLottieAnimationActivity.startActivity(intent)
                    }
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
}