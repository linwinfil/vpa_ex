package com.at.lottie.utils

import android.content.Context
import android.graphics.*
import android.net.Uri
import java.io.File
import kotlin.math.roundToInt

/**
 * Created by linmaoxin on 2021/12/23
 */
object Utils {
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
                            matrix.postTranslate(dx.roundToInt().toFloat(), dy.roundToInt().toFloat())
                            Canvas(this).drawBitmap(src, matrix, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
                        }
                    }
                }
                is File -> scaleBitmap(context, width, height, BitmapFactory.decodeFile(src.absolutePath))
                is String -> scaleBitmap(context, width, height, BitmapFactory.decodeFile(src))
                is Uri -> scaleBitmap(context, width, height, BitmapFactory.decodeStream(context.contentResolver.openInputStream(src)))
                is Int -> scaleBitmap(context, width, height, BitmapFactory.decodeResource(context.resources, src))
                else -> null
            }
        }.getOrElse { it.printStackTrace();null }
    }

}