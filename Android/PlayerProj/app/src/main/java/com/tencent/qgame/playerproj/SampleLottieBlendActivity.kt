package com.tencent.qgame.playerproj

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.at.lottie.*
import com.at.lottie.utils.BlendUtils
import com.at.lottie.utils.logd
import com.blankj.utilcode.util.UriUtils
import com.tencent.qgame.playerproj.databinding.ActivitySampleLottieBlendBinding
import java.io.File

class SampleLottieBlendActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "SampleLottieBlendActivi"
    }

    lateinit var model: LottieBlendModel
    lateinit var bind: ActivitySampleLottieBlendBinding


    var path: String = ""
    private val lottieBlendView get() = bind.lottieView
    private val progressBar get() = bind.progressCircular
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivitySampleLottieBlendBinding.inflate(layoutInflater)
        setContentView(bind.root)

        val tempOutputPath = "${application.cacheDir}/lottie_blend/merge.mp4"
        if (File(tempOutputPath).exists()) {
            val targetFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "blend_video.mp4").apply {
                delete()
            }
            if (com.blankj.utilcode.util.FileUtils.copy(File(tempOutputPath), targetFile)) {
                this.path = targetFile.absolutePath
            }
            bind.btnPlayVideo.visibility = View.VISIBLE
        }
        bind.btnPlayVideo.setOnClickListener {
            val intent = Intent(this, SampleVideoViewActivity::class.java)
            intent.putExtra("video", path)
            startActivity(intent)
        }

        model = ViewModelProvider(this).get(LottieBlendModel::class.java)
        model.blend = lottieBlendView
        lottieBlendView.postOnAnimation {

            progressBar.isIndeterminate = false
            progressBar.progress = 0
            progressBar.visibility = View.INVISIBLE

            BlendUtils.parseBlendAsset("lottie/blend.json", "lottie")?.also { blend ->
                model.addReplaceImageDelegate(ImageDelegate("img_0.jpg", R.drawable.r_img_1))
                model.addReplaceImageDelegate(ImageDelegate("img_1.png", R.drawable.r_img_2))

                //data 数据
                blend.bg?.apply {
                    model.setAssetFile(data, images, FrameType.Background)
                }
                blend.fg?.apply {
                    model.setAssetFile(data, images, FrameType.Foreground)
                }

                //滤镜
                blend.toBgFrameFilters()?.forEach {
                    model.addFrameFilterDelegate(it)
                }
                //音频
                blend.toAudioDelegate()?.apply {
                    model.addVideoAudioDelegate(this)
                }
            }
            lottieBlendView.showLottieAnimationView()
            model.initLottie()
        }

        model.blendFramesLiveData.observe(this, Observer {
            bind.ivPreview.setImageBitmap(it)
        })
        model.progressLiveData.observe(this, Observer {
            when (it) {
                is OnProgress -> {
                    setProcess(it.progress)
                }
                is OnComplete -> {
                    setProcess(100)
                    val targetFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "blend_video.mp4").apply {
                        delete()
                    }
                    if (com.blankj.utilcode.util.FileUtils.copy(File(it.path), targetFile)) {
                        this.path = targetFile.absolutePath
                        Toast.makeText(applicationContext, "已保存至${targetFile.absolutePath}", Toast.LENGTH_SHORT).show()
                    }
                    bind.btnPlayVideo.performClick()
                }
                is OnCancel -> {
                    Toast.makeText(applicationContext, "已取消", Toast.LENGTH_SHORT).show()
                }
                is OnError -> {
                    Toast.makeText(applicationContext, "失败，code:${it.errorCode}，msg:${it.errorMsg}", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        })
        bind.btnStart.setOnClickListener {
            model.startBlend()
        }
        bind.btnChooseImage.setOnClickListener {
            getContent.launch("image/*")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        getContent.unregister()
    }

    val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        logd(TAG, UriUtils.uri2File(uri).absolutePath)
    }

    private fun setProcess(progress: Int) {
        progressBar.visibility = View.VISIBLE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            progressBar.setProgress(progress, true)
        } else {
            progressBar.progress = progress
        }
    }
}