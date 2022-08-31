package com.tencent.qgame.playerproj

import android.graphics.*
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.at.lottie.utils.logd
import com.blankj.utilcode.util.ToastUtils
import com.tencent.qgame.playerproj.databinding.ActivitySimpleLottieBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.roundToInt

/**
 * Created by linmaoxin on 2022/8/30
 */
class SimpleLottieActivity : AppCompatActivity() {
    private lateinit var bind: ActivitySimpleLottieBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        bind = ActivitySimpleLottieBinding.inflate(layoutInflater)
        setContentView(bind.root)
        bind.root.postDelayed({
            playLottie()
        }, 1000)

        bind.btnTest.setOnClickListener {
            testBlend()
        }
    }

    private fun testBlend() {
        flow {
            val img = BitmapFactory.decodeResource(resources, R.raw.mvmask_067, BitmapFactory.Options().also {
                it.inMutable = true
                it.inSampleSize = 1
                it.inScaled = false
            })
            val whitebmp = Bitmap.createBitmap(img.width, img.height, Bitmap.Config.ARGB_8888).also {
                it.eraseColor(Color.WHITE)
            }
            val out = Bitmap.createBitmap(img.width, img.height, Bitmap.Config.ARGB_8888)
            Canvas(out).also {
                val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OUT) }
                it.drawBitmap(whitebmp, 0f, 0f, null)
                it.drawBitmap(img, 0f, 0f, paint)
            }
            emit(out)
        }.flowOn(Dispatchers.IO).onEach {
            bind.image.setImageBitmap(it)
            ToastUtils.showShort(it.toString())
        }.launchIn(lifecycleScope)
    }

    private fun playLottie() {
        bind.lottieView.apply {
            logd("@@@", "duration:${duration}, maxframe:${maxFrame.roundToInt()}")
            playAnimation()
        }
    }
}