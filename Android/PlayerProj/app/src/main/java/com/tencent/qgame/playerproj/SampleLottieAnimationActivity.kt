package com.tencent.qgame.playerproj

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Picture
import android.os.*
import android.util.Log
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.ImageAssetDelegate
import com.blankj.utilcode.util.ImageUtils
import com.tencent.qgame.playerproj.databinding.ActivitySampleLottieAnimationBinding
import com.tencent.qgame.playerproj.gl.RGBShiftFilter
import jp.co.cyberagent.android.gpuimage.GPUImageRenderer
import jp.co.cyberagent.android.gpuimage.PixelBuffer
import java.io.File

class SampleLottieAnimationActivity : AppCompatActivity() {
    lateinit var bind: ActivitySampleLottieAnimationBinding

    companion object {
        private const val TAG = "SampleLottieAnimationAc"
    }

    var times = 0
    private val pictureArray = mutableListOf<Picture>()
    private val cacheFile by lazy {
        File(this.cacheDir, "cache_lottie_seq").apply {
            deleteOnExit()
            mkdirs()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivitySampleLottieAnimationBinding.inflate(layoutInflater)
        setContentView(bind.root)
        cacheFile.apply { }
        bind.root.postDelayed({
            if (false) {
                bind.lottieView.addAnimatorUpdateListener {
                    Log.d(TAG, "lottie update: ${it.animatedValue} , ${++times}")
                }
                bind.lottieView.playAnimation()
                return@postDelayed
            }
            bind.lottieView.isDrawingCacheEnabled = true
            ValueAnimator.ofFloat(0f, 1f).setDuration(7440).apply {
                interpolator = LinearInterpolator()
                addUpdateListener {
                    val elapseT = SystemClock.elapsedRealtime()
                    bind.lottieView.progress = it.animatedValue as Float
                    val picture = Picture()
                    val bitmap = bind.lottieView.getDrawingCache(false)
                    val canvas = picture.beginRecording(bind.lottieView.width, bind.lottieView.height)
                    bind.lottieView.draw(canvas)
                    picture.endRecording()
                    bind.ivPreview.setImageBitmap(bitmap)
                    pictureArray.add(picture)
                    Log.d(TAG, "onCreate: add picture ->${picture.hashCode()}, ${++times} sync:${SystemClock.elapsedRealtime() - elapseT}")
                }
                start()

                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        bind.lottieView.postOnAnimation { startProcess() }
                    }
                })
            }
        }, 1500)
    }

    private fun startProcess() {
        val handlerThread = HandlerThread("process_handler_thread").apply { start() }
        val processHandler = ProcessHandler(pictureArray, handlerThread.looper)
        processHandler.sendEmptyMessage(0)
    }

    private inner class ProcessHandler(val pictureArray: List<Picture>, looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            val times = SystemClock.elapsedRealtime()
            // val pixelBuffer = PixelBuffer(pictureArray[0].width, pictureArray[0].height)
            // val filter = RGBShiftFilter(this@SampleLottieAnimationActivity.applicationContext)
            // val renderer = GPUImageRenderer(filter)
            // pixelBuffer.setRenderer(renderer)
            pictureArray.forEachIndexed { index, picture ->
                val bitmap = Bitmap.createBitmap(picture.width, picture.height, Bitmap.Config.ARGB_8888)
                Canvas(bitmap).apply {
                    picture.draw(this)
                }
                val save = ImageUtils.save(bitmap, "${cacheFile.absolutePath}${File.separator}00${index}.jpg", Bitmap.CompressFormat.JPEG, 80, true)
                Log.d(TAG, "handleMessage: save $index -> $save")
            }
            Log.d(TAG, "handleMessage: finish!! ${(SystemClock.elapsedRealtime() - times) / 1000}")
        }
    }
}