package com.tencent.qgame.playerproj.opencv

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.SeekBar
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatButton
import com.blankj.utilcode.util.ResourceUtils
import com.blankj.utilcode.util.ToastUtils
import com.blankj.utilcode.util.UriUtils
import com.tencent.qgame.playerproj.R
import com.tencent.qgame.playerproj.databinding.ActivitySampleOpenCvmatBinding
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Scalar
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException

class SampleOpenCVMatActivity : AppCompatActivity() {
    lateinit var bind: ActivitySampleOpenCvmatBinding

    var scalarDoubles: DoubleArray = doubleArrayOf(0.0, 0.0, 0.0, 1.0)
    var bitmap: Bitmap? = null
    val mat: Mat by lazy { Mat(bind.ivPreview.width, bind.ivPreview.height, CvType.CV_8UC4) }
    val grayMat: Mat by lazy { Mat(bind.ivPreview.width, bind.ivPreview.height, CvType.CV_8UC4) }

    private val getContent by lazy {
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            UriUtils.uri2File(uri)?.apply {
                bind.btnDetectFaces.tag = true
                bind.ivPreview2.setImageBitmap(BitmapFactory.decodeFile(this.absolutePath))
                applyDetectFaces()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivitySampleOpenCvmatBinding.inflate(layoutInflater)
        setContentView(bind.root)

        bind.apply {
            btnGenerate.setOnClickListener { applyMat() }
            btnGray.setOnClickListener { applyGray(mat) }
            btnDetectFaces.setOnClickListener {
                if (it.tag == null) {
                    getContent.launch("image/*")
                }
            }
            seekBar1.setOnSeekBarChangeListener(seekBarChangeListener)
            seekBar2.setOnSeekBarChangeListener(seekBarChangeListener)
            seekBar3.setOnSeekBarChangeListener(seekBarChangeListener)
        }
    }

    private val seekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            when (seekBar) {
                bind.seekBar1 -> scalarDoubles[0] = progress.toDouble()
                bind.seekBar2 -> scalarDoubles[1] = progress.toDouble()
                bind.seekBar3 -> scalarDoubles[2] = progress.toDouble()
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        getContent.unregister()
    }

    private fun applyDetectFaces() {

        val lbpcascade_frontalface = File(cacheDir, "lbpcascade_frontalface.xml")
        ResourceUtils.copyFileFromRaw(R.raw.lbpcascade_frontalface, lbpcascade_frontalface.absolutePath)
        val cascadeClassifier = CascadeClassifier(lbpcascade_frontalface.absolutePath)
        if (cascadeClassifier.empty()) {
            throw IllegalStateException("cascadeClassifier empty!!!")
        }
        val bmp = Bitmap.createBitmap(bind.ivPreview2.width, bind.ivPreview2.height, Bitmap.Config.ARGB_8888).apply {
            bind.ivPreview2.draw(Canvas(this))
        }
        val srcMat = Mat(bmp.width, bmp.height, CvType.CV_USRTYPE1)
        Utils.bitmapToMat(bmp, srcMat)
        val grayMat = Mat(srcMat.width(), srcMat.height(), srcMat.type())
        Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_RGBA2GRAY)

        val matOfRect = MatOfRect()
//        https://juejin.cn/post/7037842278580387877#heading-5
//        cascadeClassifier.detectMultiScale(grayMat, matOfRect, )
    }

    private fun applyGray(srcMat: Mat) {
        Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_RGBA2GRAY)
        val bmp = getBmp
        Utils.matToBitmap(grayMat, bmp)
        bind.ivPreview.setImageBitmap(bmp)
    }

    private fun applyMat() {
        ToastUtils.showShort(scalarDoubles.contentToString())
        val bmp = getBmp
        mat.setTo(Scalar(scalarDoubles))
        Utils.matToBitmap(mat, bmp)
        bind.ivPreview.setImageBitmap(bmp)
    }

    private val getBmp: Bitmap
        get() = bitmap?.also { it.eraseColor(Color.TRANSPARENT) } ?: run {
            Bitmap.createBitmap(
                bind.ivPreview.width,
                bind.ivPreview.height,
                Bitmap.Config.ARGB_8888
            ).also {
                bitmap = it
            }
        }
}