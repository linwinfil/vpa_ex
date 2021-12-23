package com.at.lottie.engine

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.net.Uri
import com.airbnb.lottie.*
import com.airbnb.lottie.utils.Logger
import com.airbnb.lottie.utils.Utils
import com.at.lottie.utils.Utils.scaleBitmap
import java.io.File
import java.util.*
import kotlin.math.roundToInt

/**
 * Created by linmaoxin on 2021/12/23
 */

/**
 * @param fileName 对应内置素材名
 * @param res 待替换的素材，File、Path、Bitmap、Uri、Int
 */
data class ImageDelegate(val fileName: String, val res: Any)

class LottieOptions private constructor() {
    companion object {
        val Default = LottieOptions()
    }

    internal val imageDelegates = mutableListOf<ImageDelegate>()
    internal var assetFileName: String = ""
    internal var assetFolder: String = ""
    fun addReplaceImageDelegate(delegate: ImageDelegate): LottieOptions = apply { imageDelegates.add(delegate) }
    fun setAnimation(assetName: String): LottieOptions = apply { assetFileName = assetName }
    fun setImageAssetsFolder(imageAssetsFolder: String) = apply { assetFolder = imageAssetsFolder }
}

@SuppressLint("RestrictedApi")
class LottieEngine private constructor(private val context: Context,
                                       private val option: LottieOptions = LottieOptions.Default) : Drawable.Callback, ImageAssetDelegate {
    class Builder(context: Context) {
        private val applicationContext: Context = context.applicationContext
        fun build(option: LottieOptions): LottieEngine {
            return LottieEngine(applicationContext, option)
        }
    }

    private val lottieOnCompositionLoadedListeners by lazy { HashSet<LottieOnCompositionLoadedListener>() }
    internal var failureListener: LottieListener<Throwable>? = null
    internal val duration: Float get() = lottieComposition?.duration ?: 0.0f
    internal val frameRate: Float get() = lottieComposition?.frameRate ?: 0f
    internal val endFrame: Float get() = lottieComposition?.endFrame ?: 0f
    internal val startFrame: Float get() = lottieComposition?.startFrame ?: 0f

    private val defaultFailureListener: LottieListener<Throwable> by lazy {
        LottieListener { throwable ->
            if (Utils.isNetworkException(throwable)) {
                Logger.warning("Unable to load composition.", throwable)
                return@LottieListener
            }
            throw IllegalStateException("Unable to parse composition", throwable)
        }

    }

    private val lottieDrawable = LottieDrawable()
    private var lottieComposition: LottieComposition? = null
    private var lottieCompositionTask: LottieTask<LottieComposition>? = null
    private val loadedListener: LottieListener<LottieComposition> = LottieListener { composition ->
        setComposition(composition)
    }
    private val wrappedFailureListener = LottieListener<Throwable> { result ->
        cancelLoaderTask()
        failureListener?.also { it.onResult(result) } ?: defaultFailureListener.onResult(result)
    }

    init {
        lottieDrawable.apply {
            callback = this
            setImageAssetDelegate(this@LottieEngine)
            setImagesAssetsFolder(option.assetFolder)
            LottieCompositionFactory.fromAsset(context, option.assetFileName, null)
                .addListener(loadedListener).addFailureListener(wrappedFailureListener)
        }
    }

    private fun cancelLoaderTask() {
        lottieCompositionTask?.apply {
            removeListener(loadedListener)
            removeFailureListener(wrappedFailureListener)
        }
    }

    private fun setComposition(composition: LottieComposition) {
        cancelLoaderTask()
        lottieComposition = composition
        lottieDrawable.composition = composition
        lottieOnCompositionLoadedListeners.forEach { it.onCompositionLoaded(composition) }
    }

    fun addLottieOnCompositionLoadedListener(lottieOnCompositionLoadedListener: LottieOnCompositionLoadedListener): Boolean {
        lottieComposition?.also {
            lottieOnCompositionLoadedListener.onCompositionLoaded(it)
        }
        return lottieOnCompositionLoadedListeners.add(lottieOnCompositionLoadedListener)
    }

    fun removeLottieOnCompositionLoadedListener(lottieOnCompositionLoadedListener: LottieOnCompositionLoadedListener): Boolean {
        return lottieOnCompositionLoadedListeners.remove(lottieOnCompositionLoadedListener)
    }

    fun removeAllLottieOnCompositionLoadedListener() {
        lottieOnCompositionLoadedListeners.clear()
    }

    override fun invalidateDrawable(who: Drawable) {
        TODO("Not yet implemented")
    }

    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
        TODO("Not yet implemented")
    }

    override fun unscheduleDrawable(who: Drawable, what: Runnable) {
        TODO("Not yet implemented")
    }

    override fun fetchBitmap(lottieImageAsset: LottieImageAsset?): Bitmap? {
        return lottieImageAsset?.let { asset ->
            option.imageDelegates.find { it.fileName == asset.fileName }?.let { imageAsset ->
                when (val res = imageAsset.res) {
                    is Bitmap, is String, is File, is Uri -> scaleBitmap(context, asset.width, asset.height, res)
                    else -> null
                }
            }
        }
    }

}