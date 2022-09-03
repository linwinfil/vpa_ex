package com.tencent.qgame.playerproj

import android.graphics.*
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.ImageAssetDelegate
import com.at.lottie.utils.logd
import com.blankj.utilcode.util.ToastUtils
import com.tencent.qgame.playerproj.databinding.ActivitySimpleLottieBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
        // bind.lottieView.apply {
        //     logd("@@@", "duration:${duration}, maxframe:${maxFrame.roundToInt()}")
        //     playAnimation()
        // }


        bind.lottieSpaceView.initLottie(lottie)
        bind.lottieSpaceView.postLottieDraw(0)
    }

    private val lottieFrameArr = mutableListOf<LottieFrame>()
    private var tempLottieBmp: Bitmap? = null
    private var tempLottieCanvas: Canvas? = null
    private val lottie = object : ILottie {
        override val lottieAssetDelegate: ImageAssetDelegate?
            get() = /*ImageAssetDelegate { null }*/null
        override val lottieAssetImg: String
            get() = "lottie_aging2/images"
        override val lottieAssetFile: String
            get() = "lottie_aging2/data.json"

        override fun onDrawPicture(view: LottieSpaceView, picture: LottieFrame) {
            lottieFrameArr.add(picture)
            val prc = (picture.frame / (picture.endFrame - picture.startFrame).toFloat() * 100).toInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                bind.progressCircular.setProgress(prc, true)
            } else bind.progressCircular.progress = prc
            logd("@@@", "lottie draw frame picture in:${picture.frame}, end:${picture.endFrame}, start:${picture.startFrame}")
            if (!picture.isTail) {
                view.postLottieDraw(picture.nextFrame)
            } else {
                startPictureFrame()
            }
        }
    }

    private fun startPictureFrame() {
        logd("@@@", "startPictureFrame")
        lifecycleScope.launch {
            flow {
                lottieFrameArr.onEachIndexed { index, picture ->
                    tempLottieBmp = tempLottieBmp ?: Bitmap.createBitmap(picture.width, picture.height, Bitmap.Config.ARGB_8888)
                    tempLottieCanvas = tempLottieCanvas?.also { it.drawColor(0, PorterDuff.Mode.CLEAR) } ?: Canvas(tempLottieBmp!!)
                    picture.drawTo(tempLottieCanvas!!)
                    emit(tempLottieBmp!!)
                }
            }.flowOn(Dispatchers.IO).collect {
                bind.image.setImageBitmap(tempLottieBmp)
            }
        }
    }
}