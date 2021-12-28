package com.tencent.qgame.playerproj

import android.graphics.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.annotation.WorkerThread
import androidx.core.graphics.BitmapCompat
import androidx.lifecycle.coroutineScope
import com.at.lottie.utils.logd
import com.blankj.utilcode.util.ImageUtils
import com.blankj.utilcode.util.ResourceUtils
import com.tencent.qgame.playerproj.databinding.ActivitySampleMaskColorBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.measureTimeMillis

class SampleMaskColorActivity : AppCompatActivity() {
    lateinit var bind: ActivitySampleMaskColorBinding

    companion object {
        private const val TAG = "SampleMaskColorActivity"
    }

    private val ivPreview get() = bind.ivPreview
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivitySampleMaskColorBinding.inflate(layoutInflater)
        setContentView(bind.root)

        bind.btnCopy.setOnClickListener { startCpoyMask() }
        bind.btnMask.setOnClickListener { startMaskColor() }

    }

    private val tempCache by lazy { File(this.cacheDir, "mask") }
    private val mvcolorCache by lazy { File(tempCache, "mvcolor") }
    private val mvmaskCache by lazy { File(tempCache, "mvmask") }
    private val tempResult by lazy { File(tempCache, "result") }
    private val mvcolor_suffix = "mvcolor_"
    private val mvmask_suffix = "mvmask_"
    private fun startCpoyMask() {
        lifecycle.coroutineScope.launch {
            val copyResult = withContext(Dispatchers.IO) {
                val asyncColor = async {
                    mvcolorCache.deleteRecursively()
                    ResourceUtils.copyFileFromAssets("mvcolor", mvcolorCache.absolutePath)
                }
                val asyncMask = async {
                    mvmaskCache.deleteRecursively()
                    ResourceUtils.copyFileFromAssets("mvmask", mvmaskCache.absolutePath)
                }
                asyncColor.await() && asyncMask.await()
            }
            logd(TAG, "copyResult:$copyResult")
            bind.btnMask.visibility = View.VISIBLE
        }
    }

    @WorkerThread
    private fun startMaskColor() {
        lifecycle.coroutineScope.launch {
            tempResult.deleteRecursively()
            //109-183
            withContext(Dispatchers.Default) {
                val start = 109
                val end = 183
                val paint = Paint().apply { isAntiAlias = true;isFilterBitmap = true }
                var newIndex = 0
                for (index in start..end) {
                    val times= measureTimeMillis {
                        val mvcolor = BitmapFactory.decodeFile("${mvcolorCache.absolutePath}/${mvcolor_suffix}${String.format("%03d", index)}.png")
                        var mvmask = BitmapFactory.decodeFile("${mvmaskCache.absolutePath}/${mvmask_suffix}${String.format("%03d", index)}.png")
                        mvmask = getImageWhiteToTransparent(mvmask)
                        val bitmap = Bitmap.createBitmap(mvcolor.width, mvcolor.height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        paint.xfermode = null
                        canvas.drawBitmap(mvcolor, 0f, 0f, paint)
                        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                        canvas.drawBitmap(mvmask, 0f, 0f, paint)
                        val savePath = "$tempResult/img_${newIndex++}.png"
                        ImageUtils.save(bitmap, savePath, Bitmap.CompressFormat.PNG, 100, false)
                        runOnUiThread { ivPreview.setImageBitmap(bitmap) }
                    }
                    logd(TAG, "mask color:$times")
                }
            }
        }
    }

    private fun getImageWhiteToTransparent(bitmap: Bitmap): Bitmap? {
        val createBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
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