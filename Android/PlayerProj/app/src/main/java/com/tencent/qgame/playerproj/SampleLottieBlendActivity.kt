package com.tencent.qgame.playerproj

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.at.lottie.LottieBlendModel
import com.at.lottie.OnComplete
import com.at.lottie.engine.ImageDelegate
import com.tencent.qgame.playerproj.databinding.ActivitySampleLottieBlendBinding

class SampleLottieBlendActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "SampleLottieBlendActivi"
    }

    lateinit var model: LottieBlendModel
    lateinit var bind: ActivitySampleLottieBlendBinding

    val lottieBlendView get() = bind.lottieView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivitySampleLottieBlendBinding.inflate(layoutInflater)
        setContentView(bind.root)

        model = ViewModelProviders.of(this).get(LottieBlendModel::class.java)
        model.blend = lottieBlendView
        lottieBlendView.postOnAnimation {
            model.replaceImageDelegate(ImageDelegate("img_0.jpg", R.drawable.r_img_1))
            model.replaceImageDelegate(ImageDelegate("img_1.png", R.drawable.r_img_2))
            lottieBlendView.showLottieAnimationView()
            model.initLottie()
        }

        model.scanFramesLiveData.observe(this, Observer {

        })
        model.mergeFramesLiveData.observe(this, Observer {
            when (it) {
                is OnComplete -> {
                    Log.d(TAG, "onCreate: ${it.path}")
                }
                else -> {}
            }
        })
        bind.btnStart.setOnClickListener {
            model.startScanFramesStep()
        }
    }
}