package com.tencent.qgame.playerproj

import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.util.Size
import android.view.View
import android.view.WindowId
import androidx.annotation.FloatRange
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.BitmapCompat
import androidx.core.graphics.toColor
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import com.at.lottie.utils.logd
import com.at.lottie.utils.loge
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.ImageUtils
import com.blankj.utilcode.util.ResourceUtils
import com.blankj.utilcode.util.ToastUtils
import com.bumptech.glide.Glide
import com.tencent.qgame.playerproj.databinding.ActivitySampleMaskColorBinding
import kotlinx.coroutines.*
import okio.Okio
import okio.buffer
import okio.sink
import okio.source
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.get
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.opencv.videoio.VideoCapture
import org.xml.sax.ext.LexicalHandler
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.lang.Math.abs
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
        bind.btnBlend.setOnClickListener { startBlendPicture() }
        bind.btn10Bitmap.setOnClickListener { startCreateBitmap() }
        bind.btnMaskBgDst.setOnClickListener { startMaskBgDst() }

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

    private fun startCreateBitmap() {
        lifecycleScope.launch(Dispatchers.IO) {
            val createBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
            val range = 0.rangeTo(9)
            for (i in range) {
                for (j in range) {
                    val random = Random()
                    val r = random.nextInt(256)
                    val g = random.nextInt(256)
                    val b = random.nextInt(256)
                    val a = random.nextInt(256)
                    createBitmap.setPixel(i, j, Color.argb(a, r, g, b))
                }
            }

            //copyPixelsToBuffer获取的数据中的rgb分量是经过alpha处理

            var intArray = IntArray(10 * 10)
            createBitmap.getPixels(intArray, 0, createBitmap.width, 0, 0, createBitmap.width, createBitmap.height)
            val index = Random().let { it.nextInt(10*10+1) }
            intArray.get(index).let { argb->
                val red = Color.red(argb)
                val green = Color.green(argb)
                val blue = Color.blue(argb)
                val alpha = Color.alpha(argb)
                logd(TAG, "get pixel $index, argb:$argb->$red, $green, $blue, alpha:$alpha")
            }

            intArray = IntArray(10*10)
            createBitmap.copyPixelsToBuffer(IntBuffer.wrap(intArray).rewind())
            intArray.get(index).let { rgba->
                val red = Color.red(rgba)
                val green = Color.green(rgba)
                val blue = Color.blue(rgba)
                val alpha = Color.alpha(rgba)
                logd(TAG, "get buffer $index, rgba:$rgba->$red, $green, $blue, alpha:$alpha")
            }

            runOnUiThread {
                ivPreview.setImageBitmap(createBitmap)
            }
        }
    }


    private fun startBlendPicture() {
        lifecycleScope.launch(Dispatchers.IO) {
            val imgIndex = endFrames
            val picture = Picture()
            val size = Size(720, 1280)
            var tempBmp :Bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
            val tempCanvas = Canvas(tempBmp)
            val color_path = "${mvcolorCache.absolutePath}/${String.format(img_suffix, imgIndex)}.jpg"
            colorBmp = BitmapFactory.decodeFile(color_path, decodeOP)
            colorBmp!!.setHasAlpha(true)
            logd(TAG, "color has alpha:${colorBmp!!.hasAlpha()}")
            val mask_path = "${mvmaskCache.absolutePath}/${String.format(img_suffix, imgIndex)}.jpg"
            maskBmp = BitmapFactory.decodeFile(mask_path, decodeOP)
            logd(TAG, "mask has alpha:${maskBmp!!.hasAlpha()}")
            maskBmp = maskBmp!!.copy(Bitmap.Config.ARGB_8888, true)

            var bitmap = getMaskAlphaToDst(imgIndex, maskBmp!!, colorBmp!!)

            // var cost = System.currentTimeMillis()
            // val ops = ByteArrayOutputStream()
            // bitmap.compress(Bitmap.CompressFormat.PNG, 100, ops)
            // bitmap.recycle()
            // val ips = ByteArrayInputStream(ops.toByteArray())
            // ops.close()
            // bitmap = BitmapFactory.decodeStream(ips)
            // ips.close()
            //
            // val tempCOlorIntArray = IntArray(bitmap.width * bitmap.height)
            // bitmap.copyPixelsToBuffer(IntBuffer.wrap(tempCOlorIntArray).rewind())
            // tempCOlorIntArray.getOrNull(bitmap.width / 2)?.also { rgba ->
            //     val red = Color.red(rgba)
            //     val green = Color.green(rgba)
            //     val blue = Color.blue(rgba)
            //     val alpha = Color.alpha(rgba)
            //     logd(TAG, "png buffer rgba:$red, $green, $blue, $alpha")
            // }
            // logd(TAG, "decode png cost:${(System.currentTimeMillis() - cost)}")

            val drawBg = true
            val bg_path = "${mvbgCache.absolutePath}/${String.format(img_suffix, imgIndex)}.jpg"
            if (drawBg && FileUtils.isFileExists(bg_path)) {
                val bytes = bg_path.steamBytes()
                val bgbmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOP)
                bgbmp.setHasAlpha(true)

                val canvas = picture.beginRecording(size.width, size.height)
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                canvas.drawBitmap(bgbmp!!, 0f, 0f, null)
                canvas.drawBitmap(bitmap!!, 0f, 0f, null)
                picture.endRecording()
                picture.draw(tempCanvas)

                bitmap = tempBmp
            }
            val savePath = "$tempResult/${String.format(img_suffix, imgIndex)}.jpeg"
            ImageUtils.save(bitmap, savePath, Bitmap.CompressFormat.JPEG, 100, false)
            runOnUiThread {
                val progress = 100
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    bind.progressCircular.setProgress(progress, true)
                } else {
                    bind.progressCircular.progress = progress
                }
                ivPreview.setImageBitmap(bitmap)
            }
        }
    }


    private fun startMatColor() {
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
                val costime = measureTimeMillis {
                    for (index in rangeTo) {
                        val times = measureTimeMillis {
                            val imgIndex = index
                            val color_path = "${mvcolorCache.absolutePath}/${String.format(img_suffix, imgIndex)}.jpg"
                            val mask_path = "${mvmaskCache.absolutePath}/${String.format(img_suffix, imgIndex)}.jpg"
                            val colorSrcMat: Mat = Imgcodecs.imread(color_path)
                            val maskSrcMat: Mat = Imgcodecs.imread(mask_path)
                            bitmap = getMaskAlphaToDst4Mat(imgIndex, maskSrcMat, colorSrcMat)


                            val drawBg = true
                            val bg_path = "${mvbgCache.absolutePath}/${String.format(img_suffix, imgIndex)}.jpg"
                            if (drawBg && FileUtils.isFileExists(bg_path)) {
                                val bytes = bg_path.steamBytes()
                                val bgbmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOP)

                                val picture = Picture()
                                val canvas = picture.beginRecording(size.width, size.height)
                                canvas.drawBitmap(bgbmp, 0f, 0f, null)
                                picture.endRecording()

                                tempCanvas.drawColor(0, PorterDuff.Mode.CLEAR)
                                tempCanvas.drawPicture(picture)
                                tempCanvas.drawBitmap(bitmap!!, 0f, 0f, null)
                                bitmap =tempBmp
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
                                fgPreview.setImageBitmap(bitmap)
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

    var decodeByte = true
    val decodePng = true
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
                val costime = measureTimeMillis {
                    for (index in rangeTo) {
                        val times = measureTimeMillis {
                            val imgIndex = index

                            if (!decodePng) {
                                val color_path = "${mvcolorCache.absolutePath}/${String.format(img_suffix, imgIndex)}.jpg"
                                if (decodeByte) {
                                    val bytes = color_path.steamBytes()
                                    colorBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOP)
                                } else {
                                    colorBmp = BitmapFactory.decodeFile(color_path, decodeOP)
                                }
                                val mask_path = "${mvmaskCache.absolutePath}/${String.format(img_suffix, imgIndex)}.jpg"
                                if (decodeByte) {
                                    val bytes = mask_path.steamBytes()
                                    maskBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOP)
                                } else {
                                    maskBmp = BitmapFactory.decodeFile(mask_path, decodeOP)
                                }
                                bitmap = getMaskAlphaToDst(imgIndex, maskBmp!!, colorBmp!!)
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

                            // val ops = ByteArrayOutputStream()
                            // bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, ops)
                            // val ips = ByteArrayInputStream(ops.toByteArray())
                            // ops.close()
                            // bitmap = BitmapFactory.decodeStream(ips, null, decodeOP)
                            // ips.close()

                            val drawBg = true
                            val bg_path = "${mvbgCache.absolutePath}/${String.format(img_suffix, imgIndex)}.jpg"
                            if (drawBg && FileUtils.isFileExists(bg_path)) {
                                val bytes = bg_path.steamBytes()
                                val bgbmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOP)

                                val picture = Picture()
                                val canvas = picture.beginRecording(size.width, size.height)
                                canvas.drawBitmap(bgbmp, 0f, 0f, null)
                                picture.endRecording()

                                tempCanvas.drawColor(0, PorterDuff.Mode.CLEAR)
                                tempCanvas.drawPicture(picture)
                                tempCanvas.drawBitmap(bitmap!!, 0f, 0f, null)
                                bitmap =tempBmp
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
                                fgPreview.setImageBitmap(bitmap)
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
        it.inPreferredConfig = Bitmap.Config.ARGB_8888
        it.inPreferQualityOverSpeed =  true
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


    private fun startMaskBgDst() {
        lifecycle.coroutineScope.launch {
            tempResult.deleteRecursively()
            withContext(Dispatchers.IO) {
                val start = 1
                val end = endFrames
                val rangeTo = start.rangeTo(end)
                val size = Size(720, 1280)
                var bitmap: Bitmap? = null
                var tempBmp :Bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
                val tempCanvas = Canvas(tempBmp)
                decodeByte = !decodeByte
                loge(TAG, "decodeByte:$decodeByte")
                val costime = measureTimeMillis {
                    for (index in rangeTo) {
                        val times = measureTimeMillis {
                            val imgIndex = index
                            val color_path = "${mvcolorCache.absolutePath}/${String.format(img_suffix, imgIndex)}.jpg"
                            var bytes = color_path.steamBytes()
                            colorBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOP)
                            val mask_path = "${mvmaskCache.absolutePath}/${String.format(img_suffix, imgIndex)}.jpg"
                            bytes = mask_path.steamBytes()
                            maskBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOP)

                            val bg_path = "${mvbgCache.absolutePath}/${String.format(img_suffix, imgIndex)}.jpg"
                            bytes = bg_path.steamBytes()
                            val bgbmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOP)

                            val out = getMaskBgToDst(imgIndex, maskBmp!!, colorBmp!!, bgbmp)

                            tempCanvas.drawBitmap(out.first, 0f, 0f, null)
                            tempCanvas.drawBitmap(out.second, 0f, 0f, null)

                            bitmap = tempBmp
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


    private fun getMaskBgToDst(index: Int, mask: Bitmap, color: Bitmap, bg: Bitmap):Pair<Bitmap, Bitmap> {
        val colorSize = Size(color.width, color.height)
        val maskSize = Size(mask.width, mask.height)
        val bgSize = Size(bg.width, bg.height)
        if (colorSize != maskSize) throw IllegalStateException("size diff in $index")

        maskIntArray = resetIntArray(maskIntArray, maskSize.width * maskSize.height)
        mask.copyPixelsToBuffer(IntBuffer.wrap(maskIntArray).rewind())


        colorIntArray = resetIntArray(colorIntArray, colorSize.width * colorSize.height)
        color.copyPixelsToBuffer(IntBuffer.wrap(colorIntArray).rewind())

        colorIntArray!!.forEachIndexed { colorIndex, rgba ->
            val newAlpha = Color.red(maskIntArray!![colorIndex])
            var red = Color.red(rgba)
            var green = Color.green(rgba)
            var blue = Color.blue(rgba)
            colorIntArray!![colorIndex] = Color.argb(newAlpha, red, green, blue)
        }
        //fg
        dstBmp = dstBmp?.also { it.eraseColor(0) } ?:Bitmap.createBitmap(color.width, color.height, Bitmap.Config.ARGB_8888)
        dstBmp!!.copyPixelsFromBuffer(IntBuffer.wrap(colorIntArray).rewind())


        maskIntArray!!.onEachIndexed { index, argb->
            val alpha = abs(255 - Color.red(argb)) //白色转透明
            maskIntArray!![index] =  Color.argb(alpha, 0, 0,0)
        }
        mask.copyPixelsFromBuffer(IntBuffer.wrap(maskIntArray).rewind())

        dstBg = dstBg ?:Bitmap.createBitmap(bg.width, bg.height, Bitmap.Config.ARGB_8888)
        dstBgCanvas = dstBgCanvas?.also { it.drawColor(0, PorterDuff.Mode.CLEAR) } ?: Canvas(dstBg!!)
        dstBgCanvas?.also {
            dstBgPaint.xfermode = dstBgPaint.xfermode?:PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
            it.drawBitmap(bg, 0f,0f, null)
            it.drawBitmap(mask, 0f, 0f, dstBgPaint)
        }

        return dstBg!! to dstBmp!!
    }


    /**
     * 提起遮罩R通道的作为color素材的alpha通道值
     */
    private var dstBmp:Bitmap? = null
    private var dstCanvas: Canvas? = null
    private val dstPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

    private var dstBg:Bitmap? = null
    private var dstBgCanvas: Canvas? = null
    private val dstBgPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)


    private var maskDstMat: Mat? = null
    private var colorDstMat: Mat? = null
    private fun getMaskAlphaToDst4Mat(index: Int, maskSrcMat: Mat, colorSrcMat: Mat): Bitmap {
        maskDstMat = maskDstMat ?: Mat()
        colorDstMat = colorDstMat ?: Mat()

        TODO("")
    }

    private var bgBmp:Bitmap? = null
    private fun getMaskAlphaToDst(index: Int, mask: Bitmap, color: Bitmap): Bitmap {
        val colorSize = Size(color.width, color.height)
        val maskSize = Size(mask.width, mask.height)
        if (colorSize != maskSize) throw IllegalStateException("size diff in $index")

        val useColor = false

        color.setHasAlpha(true)

        // 方式1
        // colorIntBuffer = colorIntBuffer ?: IntBuffer.allocate(color.width * color.height)
        // colorIntBuffer!!.rewind()
        // color.copyPixelsToBuffer(colorIntBuffer!!)
        // colorIntBuffer!!.rewind()
        // colorIntArray = colorIntBuffer!!.array()
        //
        // maskIntBuffer = maskIntBuffer ?: IntBuffer.allocate(maskSize.width * maskSize.height)
        // maskIntBuffer!!.rewind()
        // mask.copyPixelsToBuffer(maskIntBuffer!!.rewind())
        // maskIntBuffer!!.rewind()
        // maskIntArray = maskIntBuffer!!.array()



        // 方式2 采用buffer 4位 RGBA通道
        // colorByteArray = resetByteArray(colorBmpByteArray, color.width * color.height * 4)
        // color.copyPixelsToBuffer(ByteBuffer.wrap(colorByteArray!!).rewind())
        // if (!useColor) /*color.recycle()*/
        //
        // maskBytearray = resetByteArray(maskBmpByteArray, maskSize.width * maskSize.height * 4)
        // mask.copyPixelsToBuffer(ByteBuffer.wrap(maskBytearray!!).rewind())
        // /*mask.recycle()*/
        //
        // var alpha:Byte = 255.toByte()
        // colorByteArray!!.forEachIndexed { colorIndex, rgba ->
        //     if (colorIndex % 4 == 0/*red通道*/) {
        //         alpha = maskBytearray!![colorIndex]
        //     }
        //     if (colorIndex % 4 == 3/*alpha通道*/) {
        //         colorByteArray!![colorIndex] = alpha
        //     }
        // }
        //
        // if (!useColor) {
        //     dstBmp = dstBmp?.also { it.eraseColor(0) } ?:Bitmap.createBitmap(color.width, color.height, Bitmap.Config.ARGB_8888)
        //     dstBmp!!.copyPixelsFromBuffer(ByteBuffer.wrap(colorByteArray!!).rewind())
        //     return dstBmp!!
        // }else {
        //     color.copyPixelsFromBuffer(ByteBuffer.wrap(colorByteArray).rewind())
        //     return color
        // }



        // 方式3
        // colorIntArray = resetIntArray(colorIntArray, color.width * color.height)
        // color.copyPixelsToBuffer(IntBuffer.wrap(colorIntArray).rewind())
        //
        // maskIntArray = resetIntArray(maskIntArray, maskSize.width * maskSize.height)
        // mask.copyPixelsToBuffer(IntBuffer.wrap(maskIntArray).rewind())
        // mask.recycle()

        // 方式4
        colorIntArray = resetIntArray(colorIntArray, color.width * color.height)
        val colorBUffer = IntBuffer.wrap(colorIntArray)
        color.copyPixelsToBuffer(colorBUffer.rewind())
        colorBUffer.rewind()

        maskIntArray = resetIntArray(maskIntArray, maskSize.width * maskSize.height)
        mask.getPixels(maskIntArray, 0, mask.width, 0 ,0, mask.width, mask.height)

        colorIntArray!!.forEachIndexed { colorIndex, rgba ->
            val newAlpha = Color.red(maskIntArray!![colorIndex])
            val red = Color.red(rgba)
            val green = Color.green(rgba)
            val blue = Color.blue(rgba)
            // var alpha = Color.alpha(rgba)
            // val fac = newAlpha.toFloat() / alpha
            // red = (red * fac + 0.5f).toInt()
            // green = (green * fac + 0.5f).toInt()
            // blue = (blue * fac + 0.5f).toInt()
            colorIntArray!![colorIndex] = Color.argb(newAlpha, red, green, blue)
        }
        if (!useColor) {
            dstBmp = dstBmp?.also { it.eraseColor(0) } ?:Bitmap.createBitmap(color.width, color.height, Bitmap.Config.ARGB_8888)
            dstBmp!!.copyPixelsFromBuffer(IntBuffer.wrap(colorIntArray).rewind())
            return dstBmp!!
        }else {
            color.copyPixelsFromBuffer(IntBuffer.wrap(colorIntArray).rewind())
            return color
        }

        //
        // // 方式5
        // colorIntArray = resetIntArray(colorIntArray, color.width * color.height)
        // maskIntArray = resetIntArray(maskIntArray, maskSize.width * maskSize.height)
        // color.getPixels(colorIntArray!!, 0, color.width, 0, 0, color.width, color.height)
        // mask.getPixels(maskIntArray!!, 0, mask.width, 0, 0, mask.width, mask.height)
        //
        // val tempCOlorIntArray = IntArray(color.width * color.height)
        // color.copyPixelsToBuffer(IntBuffer.wrap(tempCOlorIntArray).rewind())
        //
        // colorIntArray!!.forEachIndexed { colorIndex, argb -> //getpixels的像素按ARGB分量排序
        //     val newAlpha = Color.red(maskIntArray!![colorIndex])
        //     val red = Color.red(argb)
        //     val green = Color.green(argb)
        //     val blue = Color.blue(argb)
        //     val alpha = Color.alpha(argb)
        //     val newColor = Color.argb(newAlpha, red, green, blue)
        //     colorIntArray!![colorIndex] = newColor
        //     if (colorIndex == color.width / 2) {
        //         logd(TAG, "pixels rgba:$red, $green, $blue, $alpha, new alpha:${newAlpha}")
        //     }
        // }
        // color.setPixels(colorIntArray!!, 0, color.width, 0, 0, color.width, color.height)
        //
        // return color

        // //方式6
        // maskIntArray = resetIntArray(maskIntArray, maskSize.width * maskSize.height)
        // mask.copyPixelsToBuffer(IntBuffer.wrap(maskIntArray).rewind())
        // maskIntArray!!.onEachIndexed { index, argb->
        //     val alpha = abs(255 - Color.red(argb))
        //     maskIntArray!![index] =  Color.argb(alpha, 0, 0,0)
        // }
        // mask.copyPixelsFromBuffer(IntBuffer.wrap(maskIntArray).rewind())
        //
        // dstBmp = dstBmp ?:Bitmap.createBitmap(color.width, color.height, Bitmap.Config.ARGB_8888)
        // dstCanvas = dstCanvas?.also { it.drawColor(0, PorterDuff.Mode.CLEAR) } ?: Canvas(dstBmp!!)
        // dstCanvas?.also {
        //     dstPaint.xfermode = dstPaint.xfermode?:PorterDuffXfermode(PorterDuff.Mode.XOR)
        //     it.drawBitmap(mask, 0f,0f, null)
        //     it.drawBitmap(color, 0f, 0f, dstPaint)
        // }
        // return dstBmp!!

    }

    private fun Float.fixcolor() = max(0, min(this.toInt(), 255))


    private var colorByteArray: ByteArray? = null
    private var maskBytearray: ByteArray? = null

    private var colorIntArray: IntArray? = null
    private var maskIntArray: IntArray? = null
    private var colorIntBuffer:IntBuffer? = null
    private var maskIntBuffer:IntBuffer? = null
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