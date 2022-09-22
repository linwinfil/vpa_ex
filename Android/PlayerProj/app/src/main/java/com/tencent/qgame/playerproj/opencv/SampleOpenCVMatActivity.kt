package com.tencent.qgame.playerproj.opencv

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.at.lottie.utils.logd
import com.at.lottie.utils.logi
import com.blankj.utilcode.util.ResourceUtils
import com.blankj.utilcode.util.ToastUtils
import com.blankj.utilcode.util.UriUtils
import com.tencent.qgame.playerproj.R
import com.tencent.qgame.playerproj.databinding.ActivitySampleOpenCvmatBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.opencv.android.Constant
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.lang.IllegalStateException

class SampleOpenCVMatActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "SampleOpenCVMatActivity"
    }
    lateinit var bind: ActivitySampleOpenCvmatBinding

    var scalarDoubles: DoubleArray = doubleArrayOf(0.0, 0.0, 0.0, 1.0)
    var bitmap: Bitmap? = null
    val mat: Mat by lazy { Mat(bind.ivPreview.width, bind.ivPreview.height, CvType.CV_8UC4) }
    val grayMat: Mat by lazy { Mat(bind.ivPreview.width, bind.ivPreview.height, CvType.CV_8UC4) }

    private val getContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            UriUtils.uri2File(uri)?.apply {
                if (bind.btnGrayImg.tag as Boolean) {
                    bind.btnGrayImg.tag = false
                    val bitmap = BitmapFactory.decodeFile(this.absolutePath)
                    applyGrayImage(bitmap)
                } else if (bind.btnDetectFaces.tag as Boolean) {
                    bind.btnDetectFaces.tag = false
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
            btnGrayImg.setOnClickListener {
                btnGrayImg.tag = true
                getContent.launch("image/*")
            }
            btnDetectFaces.setOnClickListener {
                btnDetectFaces.tag = true
                getContent.launch("image/*")
            }
            seekBar1.setOnSeekBarChangeListener(seekBarChangeListener)
            seekBar2.setOnSeekBarChangeListener(seekBarChangeListener)
            seekBar3.setOnSeekBarChangeListener(seekBarChangeListener)

            btnLoadPng.setOnClickListener {
                applyLoadImg()
            }
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

    /**
     * [人脸检测](https://juejin.cn/post/7037842278580387877#heading-5)
     */
    private fun applyDetectFaces() {
        val lbpcascade_frontalface = File(cacheDir, "lbpcascade_frontalface.xml")
        ResourceUtils.copyFileFromRaw(
            R.raw.lbpcascade_frontalface,
            lbpcascade_frontalface.absolutePath
        )
        val cascadeClassifier = CascadeClassifier(lbpcascade_frontalface.absolutePath)
        if (cascadeClassifier.empty()) {
            throw IllegalStateException("cascadeClassifier empty!!!")
        }
        val bmp = Bitmap.createBitmap(
            bind.ivPreview2.width,
            bind.ivPreview2.height,
            Bitmap.Config.ARGB_8888
        ).apply {
            bind.ivPreview2.draw(Canvas(this))
        }
        val srcMat = Mat(bmp.width, bmp.height, CvType.CV_USRTYPE1)
        Utils.bitmapToMat(bmp, srcMat)
        val grayMat = Mat(srcMat.width(), srcMat.height(), srcMat.type())
        Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_RGBA2GRAY)

        val detectSize = /*(grayMat.rows() * 0.2f).toDouble()*/0.0
        val matFaces = MatOfRect()
        cascadeClassifier.detectMultiScale(
            grayMat, //灰度图像加快检测，CV_8U类型的矩阵
            matFaces, //被检测物体的矩形框向量组
            1.1, //表示在前后两次相继的扫描中，搜索窗口的比例系数。默认为1.1即每次搜索窗口依次扩大10%;
            3,//表示构成检测目标的相邻矩形的最小个数(默认为3个)。
            0, //如果设置为 CV_HAAR_DO_CANNY_PRUNING，那么函数将会使用Canny边缘检测来排除边缘过多或过少的区域， 因此这些区域通常不会是人脸所在区域
            Size(detectSize, detectSize), Size()//minSize和maxSize用来限制得到的目标区域的范围， 小于或大于该值的对象将被忽略
        )
        logi(Constant.TAG, "matFaces:${matFaces.size()}")

        matFaces.toArray().forEach { faceRect ->
            val faceROI: Mat = grayMat.submat(faceRect)
            Imgproc.rectangle(
                srcMat, faceRect.tl(), faceRect.br(),
                Scalar(0.0, 255.0, 0.0, 255.0), 3
            )
        }
        Utils.matToBitmap(srcMat, bmp)
        bind.ivPreview2.setImageBitmap(bmp)
    }

    private fun applyGrayImage(bitmap: Bitmap) {
        /*bind.ivPreview.setImageBitmap(bitmap)*/
        val srcMat = Mat(bitmap.width, bitmap.height, CvType.CV_USRTYPE1)
        Utils.bitmapToMat(bitmap, srcMat)
        val grayMat = Mat(srcMat.width(), srcMat.height(), srcMat.type())
        Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_RGBA2GRAY)
        Utils.matToBitmap(grayMat, bitmap)
        bind.ivPreview.setImageBitmap(bitmap)
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


    private fun applyLoadImg() {
        lifecycleScope.launch(Dispatchers.Default) {
            val cost = System.currentTimeMillis()
            val color_name = "mvcolor_2/img_001.jpg"
            val mask_name = "mvmask_2/img_001.jpg"
            val tempColorPath = File(this@SampleOpenCVMatActivity.cacheDir.absolutePath, color_name)
            val tempMaskPath = File(this@SampleOpenCVMatActivity.cacheDir.absolutePath, mask_name)
            ResourceUtils.copyFileFromAssets(color_name, tempColorPath.absolutePath)
            ResourceUtils.copyFileFromAssets(mask_name, tempMaskPath.absolutePath)
            val srcColorMat = Imgcodecs.imread(tempColorPath.absolutePath)
            val srcMaskMat = Imgcodecs.imread(tempMaskPath.absolutePath)
            srcColorMat.also {
                logd(TAG, "src_mat w:${it.width()}, h:${it.height()}, dims:${it.dims()}, channels:${it.channels()}, depth:${it.depth()}, type:${it.type()}")
            }

            val dstColorMat = Mat()
            Imgproc.cvtColor(srcColorMat, dstColorMat, Imgproc.COLOR_BGR2RGBA)//rgb->rgba
            dstColorMat.also {
                logd(TAG, "dstColorMat w:${it.width()}, h:${it.height()}, dims:${it.dims()}, channels:${it.channels()}, depth:${it.depth()}, type:${it.type()}")
            }

            val dstMaskMat = Mat()
            Imgproc.cvtColor(srcMaskMat, dstMaskMat, Imgproc.COLOR_BGR2RGB)//rgb->rgb
            dstMaskMat.also {
                logd(TAG, "dstMaskMat w:${it.width()}, h:${it.height()}, dims:${it.dims()}, channels:${it.channels()}, depth:${it.depth()}, type:${it.type()}")
            }
            val cols = dstMaskMat.cols()
            val rows = dstMaskMat.rows()
            for (col in 0 until cols) {
                for (row in 0 until rows) {
                    val alpha:Double = dstMaskMat.get(row, col)[0]
                    val dstColor:DoubleArray = dstColorMat.get(row, col).also { it[3] = alpha }
                    dstColorMat.put(row, col, dstColor[0], dstColor[1], dstColor[2], dstColor[3])
                }
            }

            val bitmap = Bitmap.createBitmap(dstColorMat.width(), dstColorMat.height(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(dstColorMat, bitmap)
            srcColorMat.release()
            dstColorMat.release()
            logd(TAG, "matToBitmap:${System.currentTimeMillis() - cost}")
            runOnUiThread {
                bind.ivPreview.setImageBitmap(bitmap)
            }
        }
    }
}