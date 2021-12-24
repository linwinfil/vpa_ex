package com.tencent.qgame.playerproj

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.at.lottie.*
import com.tencent.qgame.playerproj.databinding.ActivitySampleLottieBlendBinding
import com.tencent.qgame.playerproj.gl.RGBShiftFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import okio.buffer
import okio.source

class SampleLottieBlendActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "SampleLottieBlendActivi"
    }

    lateinit var model: LottieBlendModel
    lateinit var bind: ActivitySampleLottieBlendBinding

    private val lottieBlendView get() = bind.lottieView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivitySampleLottieBlendBinding.inflate(layoutInflater)
        setContentView(bind.root)

        model = ViewModelProviders.of(this).get(LottieBlendModel::class.java)
        model.blend = lottieBlendView
        lottieBlendView.postOnAnimation {
            model.addReplaceImageDelegate(ImageDelegate("img_0.jpg", R.drawable.r_img_1))
            model.addReplaceImageDelegate(ImageDelegate("img_1.png", R.drawable.r_img_2))

            model.addFrameFilterDelegate(FrameFilter(0, 100,
                RGBShiftFilter(GPUImageFilter.NO_FILTER_VERTEX_SHADER, resources.openRawResource(R.raw.rgb_shift_fragment).source().buffer().readUtf8())))

            lottieBlendView.showLottieAnimationView()
            model.initLottie()
        }

        model.scanFramesLiveData.observe(this, Observer {
            Log.d(TAG, "scan frames : $it")
        })
        model.mergeFramesLiveData.observe(this, Observer {
            when (it) {
                is OnComplete -> {
                    Log.d(TAG, "merge frames: ${it.path}")
                }
                is OnError -> {
                    Log.d(TAG, "onerror ${it.errorCode}, ${it.errorMsg}")
                }
                else -> {}
            }
        })
        bind.btnStart.setOnClickListener {
            model.startFrameBlend()
        }
    }
}