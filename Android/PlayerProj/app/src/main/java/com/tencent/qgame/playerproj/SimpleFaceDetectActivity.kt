package com.tencent.qgame.playerproj

import android.graphics.*
import android.os.Build.VERSION_CODES.S
import android.os.Bundle
import android.util.Size
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.minus
import androidx.lifecycle.lifecycleScope
import com.at.lottie.utils.logd
import com.at.lottie.utils.loge
import com.at.lottie.utils.logi
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.tencent.qgame.playerproj.databinding.ActivitySimpleFaceDetectBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlin.random.Random

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
                    val index = Random.nextInt(2)
                    val dstBmp = resources.assets.open("aging/img_${index}.png").use { ips->
                        BitmapFactory.decodeStream(ips, null, BitmapFactory.Options().also { it.inMutable = true })
                    }
                    val srcFace = Tasks.await(detector.process(bmp!!, 0)).getOrNull(0)
                    val dstFace = Tasks.await(detector.process(dstBmp!!, 0)).getOrNull(0)
                    takeIf { srcFace!= null && dstFace != null }?.let {
                        srcFace!!;dstFace!!
                        val w = bmp.width.toFloat()
                        val h = bmp.height.toFloat()
                        val srcBox = srcFace.boundingBox
                        val dstBox = dstFace.boundingBox
                        loge("@@@", "face:${srcBox}, ${srcBox.centerX()} - ${srcBox.centerY()}")
                        loge("@@@", "l:${(srcBox.left / w).format()}, t:${(srcBox.top / h).format()}, r:${(srcBox.right / w).format()}, b:${(srcBox.bottom / h).format()}")

                        Canvas(bmp).also { cv ->
                            Canvas(dstBmp).also { dstcv ->
                                val paint = Paint().apply {
                                    color = Color.YELLOW
                                    style = Paint.Style.STROKE
                                    strokeWidth = 2f
                                }
                                val frame = ptsFToPath(Array(4) { PointF() }.also {
                                    it[0].set(dstBox.left.toFloat() - 2, dstBox.top.toFloat() - 2)
                                    it[1].set(dstBox.right.toFloat() + 2, dstBox.top.toFloat() - 2)
                                    it[2].set(dstBox.right.toFloat() + 2, dstBox.bottom.toFloat() + 2)
                                    it[3].set(dstBox.left.toFloat() - 2, dstBox.bottom.toFloat() + 2)
                                })
                                dstcv.drawPath(frame, paint)
                                paint.apply {
                                    color = Color.YELLOW
                                    style = Paint.Style.FILL
                                    strokeWidth = 2f
                                }
                                dstcv.drawCircle(dstBox.centerX().toFloat(), dstBox.centerY().toFloat(), 12f, paint)
                            }

                            var dstF:Size = if (srcBox.height() > dstBox.height()) {
                                //放大大人脸区域
                                getPuzzleOutSize(dstBox.width(), dstBox.height(), srcBox.height())
                            } else {
                                Size(dstBox.width(), dstBox.height())
                            }
                            var s: Float = (dstF.height / dstBox.height().toFloat())

                            var offsetY = 0f
                            val dstHeight = (dstBmp.height * s).toInt()
                            if (dstHeight > h) { //整体放大后已超过目标高度
                                offsetY = dstF.height * 0.1f
                                logd("@@@", "dstHeight:$dstHeight, h:$h, offsetY:$offsetY")
                            }

                            val x = (srcBox.centerX() - (dstBox.centerX() * s))
                            val y = (srcBox.centerY() - (dstBox.centerY() * s)) + offsetY
                            val m = Matrix()
                            m.postScale(s, s)
                            m.postTranslate(x,y)
                            cv.drawBitmap(dstBmp, m, null)

                            val path = ptsFToPath(Array(4) { PointF() }.also { srcBox.setPts(it) })
                            val paint = Paint().apply {
                                color = Color.RED
                                style = Paint.Style.FILL
                                strokeWidth = 3f
                            }
                            cv.drawCircle(srcFace.boundingBox.centerX().toFloat(), srcFace.boundingBox.centerY().toFloat(), 9f, paint)
                            paint.apply {
                                style = Paint.Style.STROKE
                                strokeWidth = 2f
                            }
                            cv.drawPath(path, paint)

                            val frame = ptsFToPath(Array(4) { PointF() }.also {
                                it[0].set(0f, 0f)
                                it[1].set(bmp.width.toFloat(), 0f)
                                it[2].set(bmp.width.toFloat(), bmp.height.toFloat())
                                it[3].set(0f, bmp.height.toFloat())
                            })
                            cv.drawPath(frame, paint)
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
    fun getPuzzleOutSize(previewW: Int, previewH: Int, maxSize: Int): Size {
        val ratio = previewW.toFloat() / previewH
        return if (ratio < 1) {
            Size((maxSize * ratio).toInt(), maxSize)
        } else {
            Size(maxSize, (maxSize / ratio).toInt())
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