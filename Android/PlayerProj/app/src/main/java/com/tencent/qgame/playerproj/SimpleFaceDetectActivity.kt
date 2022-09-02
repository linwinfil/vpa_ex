package com.tencent.qgame.playerproj

import android.content.res.AssetManager
import android.graphics.*
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.at.lottie.utils.logd
import com.at.lottie.utils.loge
import com.blankj.utilcode.util.Utils
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.tencent.qgame.playerproj.databinding.ActivitySimpleFaceDetectBinding
import com.tencent.qgame.playerproj.databinding.ActivitySimpleLottieBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * Created by linmaoxin on 2022/9/2
 */
class SimpleFaceDetectActivity : AppCompatActivity() {


    lateinit var binding: ActivitySimpleFaceDetectBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySimpleFaceDetectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val detector: FaceDetector by lazy {
            FaceDetection.getClient(FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST).build())
        }
        binding.root.postOnAnimation {
            lifecycleScope.launch {
                flow {
                    val bmp = resources.assets.open("lottie_aging/images/img_0.png").use { ips ->
                        BitmapFactory.decodeStream(ips, null, BitmapFactory.Options().also { it.inMutable = true })
                    }
                    Tasks.await(detector.process(bmp!!, 0)).getOrNull(0)?.let { face ->
                        val w = bmp.width.toFloat()
                        val h = bmp.height.toFloat()
                        val box = face.boundingBox
                        loge("@@@", "face:${box}, ${box.centerX()} - ${box.centerY()}")
                        loge("@@@", "l:${(box.left / w).format()}, t:${(box.top / h).format()}, r:${(box.right / w).format()}, b:${(box.bottom / h).format()}")
                        Canvas(bmp).also { cv ->
                            val path = ptsFToPath(Array(4) { PointF() }.also { box.setPts(it) })
                            val paint = Paint().apply {
                                color = Color.RED
                                style = Paint.Style.FILL
                                strokeWidth = 3f
                            }
                            cv.drawCircle(face.boundingBox.centerX().toFloat(), face.boundingBox.centerY().toFloat(), 9f, paint)
                            paint.apply {
                                style = Paint.Style.STROKE
                                strokeWidth = 2f
                            }
                            cv.drawPath(path, paint)
                        }
                        emit(bmp)
                    } ?: emit(null)
                }.flowOn(Dispatchers.IO).collect {
                    it?.apply {
                        binding.ivPreview.setImageBitmap(this)
                    }
                }
            }

        }
    }

    fun Float.format() = String.format("%.2f", this)
    fun ptsFToPath(pts: Array<PointF>): Path {
        return Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) {
                lineTo(pts[i].x, pts[i].y)
            }
            lineTo(pts[0].x, pts[0].y)
        }
    }

    fun Rect.setPts(pts: Array<PointF>) {
        if (pts.size == 4) {
            pts[0].set(left.toFloat(), top.toFloat())
            pts[1].set(right.toFloat(), top.toFloat())
            pts[2].set(right.toFloat(), bottom.toFloat())
            pts[3].set(left.toFloat(), bottom.toFloat())
        }
    }

}