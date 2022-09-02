package com.tencent.qgame.playerproj

import android.graphics.*
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Size
import android.view.View
import androidx.annotation.WorkerThread
import androidx.lifecycle.coroutineScope
import com.airbnb.lottie.animation.keyframe.MaskKeyframeAnimation
import com.at.lottie.utils.logd
import com.at.lottie.utils.loge
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.ImageUtils
import com.blankj.utilcode.util.ResourceUtils
import com.blankj.utilcode.util.ToastUtils
import com.tencent.qgame.playerproj.databinding.ActivitySampleMaskColorBinding
import kotlinx.coroutines.*
import okio.buffer
import okio.source
import java.io.File
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.system.measureTimeMillis

class SampleMaskColorActivity : AppCompatActivity() {
    lateinit var bind: ActivitySampleMaskColorBinding

    companion object {
        private const val TAG = "SampleMaskColorActivity"
    }

    private val ivPreview get() = bind.ivPreview
    private val fgPreview get() = bind.fgPreview
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivitySampleMaskColorBinding.inflate(layoutInflater)
        setContentView(bind.root)

        bind.btnCopy.setOnClickListener { startCpoyMask() }
        bind.btnMask.setOnClickListener { startMaskColor() }

    }

    private val mvcolor =  "mvcolor_2"
    private val mvmask =  "mvmask_2"
    private val mvbg =  "mvbg_2"
    private val endFrames = if (mvcolor == "mvcolor_2") 191 else 240
    private val tempCache by lazy { File(this.cacheDir, "mask") }
    private val mvcolorCache by lazy { File(tempCache, mvcolor) }
    private val mvmaskCache by lazy { File(tempCache, mvmask) }
    private val mvbgCache by lazy { File(tempCache, mvbg) }
    private val tempResult by lazy { File(tempCache, "result") }
    private val img_suffix = "img_%03d"
    private fun startCpoyMask() {
        lifecycle.coroutineScope.launch {
            val copyResult = withContext(Dispatchers.IO) {
                val asyncColor = async {
                    mvcolorCache.deleteRecursively()
                    ResourceUtils.copyFileFromAssets(mvcolor, mvcolorCache.absolutePath)
                }
                val asyncMask = async {
                    mvmaskCache.deleteRecursively()
                    ResourceUtils.copyFileFromAssets(mvmask, mvmaskCache.absolutePath)
                }
                val asyncBg = async {
                    mvbgCache.deleteRecursively()
                    ResourceUtils.copyFileFromAssets(mvbg, mvbgCache.absolutePath)
                }
                asyncColor.await() && asyncMask.await() && asyncBg.await()
            }
            ToastUtils.showShort("copy mask completed!!!")
            logd(TAG, "copyResult:$copyResult")
            bind.btnMask.visibility = View.VISIBLE
        }
    }

    var decodeByte = true
    @WorkerThread private fun startMaskColor() {
        lifecycle.coroutineScope.launch {
            tempResult.deleteRecursively()
            withContext(Dispatchers.IO) {
                val start = 1
                val end = endFrames
                val rangeTo = start.rangeTo(end)
                val paint = Paint().apply { isAntiAlias = true;isFilterBitmap = true }
                var newIndex = 0
                val size = Size(720, 1280)
                val bufferSize: Int = size.width * size.height * 4
                var bitmap: Bitmap? = null
                var tempBmp :Bitmap? = null
                var canvas: Canvas? = null
                val dstOut = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                decodeByte = !decodeByte
                loge(TAG, "decodeByte:$decodeByte")
                val costime = measureTimeMillis {
                    for (index in rangeTo) {
                        val times = measureTimeMillis {
                            val imgIndex = index
                            val color_path = "${mvcolorCache.absolutePath}/${String.format(img_suffix, imgIndex)}.jpg"
                            if (decodeByte) {
                                // colorBmpByteArray = resetByteArray(colorBmpByteArray, bufferSize)
                                // colorBmp = resetBmp(colorBmp, size, decodeFileStream(color_path, colorBmpByteArray!!))
                                val bytes = color_path.steamBytes()
                                colorBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } else {
                                colorBmp = BitmapFactory.decodeFile(color_path, decodeOP)
                            }
                            val mask_path = "${mvmaskCache.absolutePath}/${String.format(img_suffix, imgIndex)}.jpg"
                            if (decodeByte) {
                                // maskBmpByteArray = resetByteArray(maskBmpByteArray, bufferSize)
                                // maskBmp = resetBmp(maskBmp, size, decodeFileStream(mask_path, maskBmpByteArray!!))

                                val bytes = mask_path.steamBytes()
                                maskBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } else {
                                maskBmp = BitmapFactory.decodeFile(mask_path, decodeOP)
                            }
                            bitmap = getMaskAlphaToDst(index, maskBmp!!, colorBmp!!)
                            val bg_path = "${mvbgCache.absolutePath}/${String.format(img_suffix, imgIndex)}.jpg"
                            if (FileUtils.isFileExists(bg_path)) {
                                val bytes = bg_path.steamBytes()
                                val bgbmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOP)
                                Canvas(bgbmp).also {
                                    it.drawBitmap(bitmap!!, 0f, 0f, null)
                                }
                                bitmap = bgbmp
                            }

                            // val savePath = "$tempResult/${String.format(img_suffix, imgIndex)}.jpeg"
                            // ImageUtils.save(bitmap, savePath, Bitmap.CompressFormat.JPEG, 100, false)
                            runOnUiThread {
                                val progress = (index / end.toFloat() * 100).toInt()
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    bind.progressCircular.setProgress(progress, true)
                                } else {
                                    bind.progressCircular.progress = progress
                                }
                                ivPreview.setImageBitmap(bitmap)
                            }

                        }
                        logd(TAG, "$index -> mask color:$times")
                    }
                }
                loge(TAG, "mask blend completed!!!, ${costime}")
                runOnUiThread { ToastUtils.showShort("mask blend completed!!!, $costime") }
            }
        }
    }

    private val decodeOP get() = BitmapFactory.Options().also {
        it.inMutable = true
    }
    private fun File.steamBytes():ByteArray = this.source().buffer().use { it.readByteArray() }
    private fun String.steamBytes():ByteArray = File(this).steamBytes()


    private var colorBmpByteArray: ByteArray? = null
    private var maskBmpByteArray: ByteArray? = null
    private var colorBmp:Bitmap? = null
    private var maskBmp:Bitmap? = null

    private fun decodeFileStream(path: String, byteArray: ByteArray): ByteArray {
        File(path).inputStream().use {
            it.read(byteArray, 0, it.available())
        }
        return byteArray
    }
    private fun resetBmp(bmp: Bitmap?, size: Size, byteArray: ByteArray): Bitmap {
        var out = bmp
        out = out ?: Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        out!!.copyPixelsFromBuffer(ByteBuffer.wrap(byteArray))
        return out
    }

    private fun resetByteArray(byteArray: ByteArray?, size: Int): ByteArray {
        var array = byteArray
        if (array == null || array.size != size) {
            array = ByteArray(size)
        } else {
            Arrays.fill(array, 0)
        }
        return array
    }





    /**
     * 提起遮罩R通道的作为color素材的alpha通道值
     */
    private var dstBmp:Bitmap? = null
    private var bgBmp:Bitmap? = null
    private fun getMaskAlphaToDst(index: Int, mask: Bitmap, color: Bitmap): Bitmap {
        val colorSize = Size(color.width, color.height)
        val maskSize = Size(mask.width, mask.height)
        if (colorSize != maskSize) throw IllegalStateException("size diff in $index")

        colorIntArray = resetIntArray(colorIntArray, color.width * color.height)
        color.copyPixelsToBuffer(IntBuffer.wrap(colorIntArray))
        color.recycle()
        maskIntArray = resetIntArray(maskIntArray, maskSize.width * maskSize.height)
        mask.copyPixelsToBuffer(IntBuffer.wrap(maskIntArray))

        dstBmp = dstBmp?.also { it.eraseColor(0) } ?:Bitmap.createBitmap(color.width, color.height, Bitmap.Config.ARGB_8888)
        colorIntArray!!.forEachIndexed {  colorIndex, argb ->
            val alpha = Color.red(maskIntArray!![colorIndex])
            val red = Color.red(argb)
            val green = Color.green(argb)
            val blue = Color.blue(argb)
            colorIntArray!![colorIndex] = Color.argb(alpha, red, green, blue)

        }
        dstBmp!!.copyPixelsFromBuffer(IntBuffer.wrap(colorIntArray))
        return dstBmp!!
    }
    private fun Float.fixcolor() = max(0, min(this.toInt(), 255))


    private var colorIntArray: IntArray? = null
    private var maskIntArray: IntArray? = null
    private var bgByteArray:ByteArray? = null
    private fun resetIntArray(intArray: IntArray?, size: Int): IntArray {
        var array = intArray
        if (array == null || array.size != size) {
            array = IntArray(size)
        } else {
            Arrays.fill(array, 0)
        }
        return array
    }
    private fun getImageWhiteToTransparent(index: Int, bitmap: Bitmap): Bitmap {
        val createBitmap = bitmap
        val mWidth = bitmap.width
        val mHeight = bitmap.height
        for (i in 0 until mHeight) {
            for (j in 0 until mWidth) {
                var color = bitmap.getPixel(j, i)
                val g = Color.green(color)
                val r = Color.red(color)
                val b = Color.blue(color)
                var a = Color.alpha(color)
                if (g >= 200 && r >= 200 && b >= 200) {
                    a = 0
                }
                color = Color.argb(a, r, g, b)
                createBitmap.setPixel(j, i, color)
            }
        }
        return createBitmap
    }
}