package com.tencent.qgame.playerproj

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.tencent.qgame.playerproj.databinding.ActivitySampleVideoViewBinding

class SampleVideoViewActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "SampleVideoViewActivity"
    }

    lateinit var bind: ActivitySampleVideoViewBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivitySampleVideoViewBinding.inflate(layoutInflater)
        setContentView(bind.root)

        val path = intent.getStringExtra("video")
        bind.videoView.apply {
            setVideoPath(path)
            setOnPreparedListener {
                it.start()
            }
            setOnErrorListener { mp, what, extra ->
                Log.d(TAG, "onCreate: $what, $extra")
                true
            }
        }
    }
}