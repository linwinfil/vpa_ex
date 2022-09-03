package com.tencent.qgame.playerproj

import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.util.Size
import android.view.View
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.BitmapCompat
import androidx.lifecycle.coroutineScope
import com.at.lottie.utils.logd
import com.at.lottie.utils.loge
import com.blankj.utilcode.util.FileUtils
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

    private val mv_type = 2
    private val mvcolor =  "mvcolor_${mv_type}"
    private val mvmask =  "mvmask_${mv_type}"
    private val mvbg =  "mvbg_${mv_type}"
    private val mvalpha =  "mvalpha_${mv_type}"
    private val endFrames = if (mvcolor.contains("_$mv_type")) 191 else 240
    private val tempCache by lazy { File(this.cacheDir, "mask") }
    private val mvcolorCache by lazy { File(tempCache, mvcolor) }
    private val mvmaskCache by lazy { File(tempCache, mvmask) }
    private val mvbgCache by lazy { File(tempCache, mvbg) }
    private val mvalphaCache by lazy { File(tempCache, mvalpha) }
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
                val asyncAlpha = async {
                    mvalphaCache.deleteRecursively()
                    ResourceUtils.copyFileFromAssets(mvalpha, mvalphaCache.absolutePath)
                }
                asyncColor.await() && asyncMask.await() && asyncBg.await() && asyncAlpha.await()
            }
            ToastUtils.showShort("copy mask completed!!!")
            logd(TAG, "copyResult:$copyResult")
            bind.btnMask.visibility = View.VISIBLE
        }
    }

    var decodeByte = true
    val decodePng = false
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
                var tempBmp :Bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
                val tempCanvas = Canvas(tempBmp)
                val dstOut = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                val picture = Picture()
                decodeByte = !decodeByte
                loge(TAG, "decodeByte:$decodeByte")
                val costime = measureTimeMillis {
                    for (index in rangeTo) {
                        val times = measureTimeMillis {
                            val imgIndex = index

                            if (!decodePng) {
                                val color_path = "${mvcolorCache.absolutePath}/${String.format(img_suffix, imgIndex)}.jpg"
                                if (decodeByte) {
                                    val bytes = color_path.steamBytes()
                                    colorBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                } else {
                                    colorBmp = BitmapFactory.decodeFile(color_path, decodeOP)
                                }
                                val mask_path = "${mvmaskCache.absolutePath}/${String.format(img_suffix, imgIndex)}.jpg"
                                if (decodeByte) {
                                    val bytes = mask_path.steamBytes()
                                    maskBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                } else {
                                    maskBmp = BitmapFactory.decodeFile(mask_path, decodeOP)
                                }
                                bitmap = getMaskAlphaToDst(index, maskBmp!!, colorBmp!!)
                            } else {
                                val alpha_path = "${mvalphaCache.absolutePath}/${String.format(img_suffix, imgIndex)}.png"
                                if (decodeByte) {
                                    val bytes = alpha_path.steamBytes()
                                    alphaBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                } else {
                                    alphaBmp = BitmapFactory.decodeFile(alpha_path, decodeOP)
                                }
                                bitmap = alphaBmp
                            }

                            val drawBg = true
                            val bg_path = "${mvbgCache.absolutePath}/${String.format(img_suffix, imgIndex)}.jpg"
                            if (drawBg && FileUtils.isFileExists(bg_path)) {
                                val bytes = bg_path.steamBytes()
                                val bgbmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOP)

                                val canvas = picture.beginRecording(size.width, size.height)
                                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                                canvas.drawBitmap(bgbmp, 0f, 0f, null)
                                // paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
                                canvas.drawBitmap(bitmap!!, 0f, 0f, paint)
                                picture.endRecording()
                                picture.draw(tempCanvas)

                                bitmap = tempBmp
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
                            delay(16)

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
    private var alphaBmp:Bitmap? = null

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

        val useColor = false

        colorIntArray = resetIntArray(colorIntArray, color.width * color.height)
        color.copyPixelsToBuffer(IntBuffer.wrap(colorIntArray).rewind())
        if (!useColor) color.recycle()

        maskIntArray = resetIntArray(maskIntArray, maskSize.width * maskSize.height)
        mask.copyPixelsToBuffer(IntBuffer.wrap(maskIntArray).rewind())
        mask.recycle()


        colorIntArray!!.forEachIndexed { colorIndex, argb ->
            val alpha = Color.red(maskIntArray!![colorIndex])
            val red = Color.red(argb)
            val green = Color.green(argb)
            val blue = Color.blue(argb)
            val newColor = Color.argb(alpha, red, green, blue)

            colorIntArray!![colorIndex] = newColor
        }

        if (!useColor) {
            dstBmp = dstBmp?.also { it.eraseColor(0) } ?:Bitmap.createBitmap(color.width, color.height, Bitmap.Config.ARGB_8888)
            dstBmp!!.copyPixelsFromBuffer(IntBuffer.wrap(colorIntArray).rewind())
            return dstBmp!!
        }else {
            color.copyPixelsFromBuffer(IntBuffer.wrap(colorIntArray).rewind())
            return color
        }
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