package com.at.lottie.utils

import android.content.Context
import android.graphics.*
import android.net.Uri
import com.at.lottie.Blend
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.ResourceUtils
import com.blankj.utilcode.util.Utils
import com.blankj.utilcode.util.ZipUtils
import com.google.gson.Gson
import okio.buffer
import okio.source
import java.io.File
import kotlin.math.roundToInt

/**
 * Created by linmaoxin on 2021/12/23
 */

public fun <E> MutableList<E>.append(e: E): MutableList<E> = apply { add(e) }

object BlendUtils {

    /**
     * todo 解析zip包文件
     */
    fun parseBlendFile(filePath: String): Blend? {
        if (!FileUtils.isFileExists(filePath) || !filePath.endsWith(".zip", true)) return null
        val zipName = filePath.substringAfterLast("/").substringBeforeLast(".")
        val dstFile = File(Utils.getApp().cacheDir, zipName).apply {
            deleteRecursively()
        }
        return runCatching {
            ZipUtils.unzipFile(File(filePath), dstFile)?.takeIf { it.isNotEmpty() }?.let {
                Gson().fromJson(
                    File(dstFile, "blend.json").source().buffer().readUtf8(),
                    Blend::class.java
                )
            }?.also { blend ->
                blend.audio?.apply {
                    if (data.isNotEmpty()) {
                        data = File(dstFile, data).absolutePath
                    }
                }
                blend.bg?.apply {
                    if (data.isNotEmpty()) data = File(dstFile, data).absolutePath
                    if (images.isNotEmpty()) images = File(dstFile, images).absolutePath
                }
                blend.fg?.apply {
                    if (data.isNotEmpty()) data = File(dstFile, data).absolutePath
                    if (images.isNotEmpty()) images = File(dstFile, images).absolutePath
                }
            }
        }.getOrNull()
    }

    /**
     * 解析asset下blend.json
     */
    fun parseBlendAsset(fileName: String, parentFolder: String = ""): Blend? {
        return runCatching {
            Gson().fromJson(ResourceUtils.readAssets2String(fileName), Blend::class.java)
        }.getOrElse {
            it.printStackTrace()
            null
        }?.also { blend->
            if (parentFolder.isNotEmpty()) {
                val parentFolderSuffix = if (!parentFolder.endsWith("/")) "$parentFolder/" else parentFolder
                blend.audio?.apply {
                    if (data.isNotEmpty()) {
                        data = "$parentFolderSuffix$data"
                    }
                }
                blend.bg?.apply {
                    if (data.isNotEmpty()) data = "$parentFolderSuffix$data"
                    if (images.isNotEmpty()) images = "$parentFolderSuffix$images"
                }
                blend.fg?.apply {
                    if (data.isNotEmpty()) data = "$parentFolderSuffix$data"
                    if (images.isNotEmpty()) images = "$parentFolderSuffix$images"
                }
            }
        }
    }

    fun scaleBitmap(context: Context, width: Int, height: Int, src: Any?): Bitmap? {
        src ?: return null
        return runCatching {
            when (src) {
                is Bitmap -> {
                    src.takeIf { it.isRecycled }?.let { null } ?: run {
                        if (width == src.width && height == src.height) return src
                        Bitmap.createBitmap(width, height, src.config).apply {
                            val matrix = Matrix()
                            val scale: Float
                            var dx = 0f
                            var dy = 0f
                            if (src.width * height > width * src.height) {
                                scale = height.toFloat() / src.height.toFloat()
                                dx = (width - src.width * scale) * 0.5f
                            } else {
                                scale = width.toFloat() / src.width.toFloat()
                                dy = (height - src.height * scale) * 0.5f
                            }
                            matrix.setScale(scale, scale)
                            matrix.postTranslate(
                                dx.roundToInt().toFloat(),
                                dy.roundToInt().toFloat()
                            )
                            Canvas(this).drawBitmap(
                                src,
                                matrix,
                                Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
                            )
                        }
                    }
                }
                is File -> scaleBitmap(
                    context,
                    width,
                    height,
                    BitmapFactory.decodeFile(src.absolutePath)
                )
                is String -> scaleBitmap(context, width, height, BitmapFactory.decodeFile(src))
                is Uri -> scaleBitmap(
                    context,
                    width,
                    height,
                    BitmapFactory.decodeStream(context.contentResolver.openInputStream(src))
                )
                is Int -> scaleBitmap(
                    context,
                    width,
                    height,
                    BitmapFactory.decodeResource(context.resources, src)
                )
                else -> null
            }
        }.getOrElse { it.printStackTrace();null }
    }

}