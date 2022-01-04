package com.at.lottie.engine

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.Matrix.ScaleToFit
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.Choreographer
import android.widget.ImageView
import androidx.annotation.Nullable
import com.airbnb.lottie.*
import com.airbnb.lottie.utils.Logger
import com.airbnb.lottie.utils.Utils
import com.at.lottie.ImageDelegate
import com.at.lottie.utils.BlendUtils.scaleBitmap
import java.io.File
import java.util.*
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Created by linmaoxin on 2021/12/23
 */


class LottieOptions private constructor() {
    companion object {
        val Default = LottieOptions()
    }

    internal val imageDelegates = mutableListOf<ImageDelegate>()
    internal var assetFileName: String = ""
    internal var assetFolder: String = ""
    internal var dstWidth: Int = 0
        private set
    internal var dstHeight: Int = 0
        private set

    fun setOutputSize(w: Int, h: Int) {
        dstWidth = w
        dstHeight = h
    }

    fun addReplaceImageDelegate(delegate: ImageDelegate): LottieOptions =
        apply { imageDelegates.add(delegate) }

    fun setAnimation(assetName: String): LottieOptions = apply { assetFileName = assetName }
    fun setImageAssetsFolder(imageAssetsFolder: String) = apply { assetFolder = imageAssetsFolder }
}


class LottieDrawableImpl(private val dstWidth: Int, private val dstHeight: Int) :
    Drawable.Callback {
    private val drawable = LottieDrawable()
    var composition: LottieComposition? = null
        get() = drawable.composition
        set(value) {
            drawable.composition = value
            field = value
        }

    fun setImageAssetDelegate(assetDelegate: ImageAssetDelegate) {
        drawable.setImageAssetDelegate(assetDelegate)
    }

    fun setImagesAssetsFolder(@Nullable imageAssetsFolder: String) {
        drawable.setImagesAssetsFolder(imageAssetsFolder)
    }

    private var drawMatrix: Matrix? = Matrix()
    private var drawableWidth: Int = 0
    private var drawableHeight: Int = 0
    private var scaleType = ImageView.ScaleType.FIT_CENTER

    override fun invalidateDrawable(who: Drawable) {
        // update cached drawable dimensions if they've changed
        val w: Int = who.intrinsicWidth
        val h: Int = who.intrinsicHeight
        if (w != drawableWidth || h != drawableHeight) {
            drawableWidth = w
            drawableHeight = h
            // updates the matrix, which is dependent on the bounds
            configureBounds()
        }
    }

    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
        TODO("Not yet implemented")
    }

    override fun unscheduleDrawable(who: Drawable, what: Runnable) {
        TODO("Not yet implemented")
    }

    private fun configureBounds() {
        val dwidth: Int = drawableWidth
        val dheight: Int = drawableWidth
        val vwidth: Int = dstWidth
        val vheight: Int = dstHeight
        val fits = ((dwidth < 0 || vwidth == dwidth) && (dheight < 0 || vheight == dheight))
        if (dwidth <= 0 || dheight <= 0 || ImageView.ScaleType.FIT_XY == scaleType) {
            /* If the drawable has no intrinsic size, or we're told to
                scaletofit, then we just fill our entire view.
            */
            drawable.setBounds(0, 0, vwidth, vheight)
            drawMatrix = null
        } else {
            // We need to do the scaling ourself, so have the drawable
            // use its native size.
            drawable.setBounds(0, 0, dwidth, dheight)
            when {
                fits -> {
                    drawMatrix = null// The bitmap fits exactly, no transform needed.
                }
                ImageView.ScaleType.CENTER == scaleType -> {
                    // Center bitmap in view, no scaling.
                    drawMatrix = drawMatrix ?: Matrix()
                    drawMatrix?.apply {
                        setTranslate(
                            ((vwidth - dwidth) * 0.5f).roundToLong().toFloat(),
                            ((vheight - dheight) * 0.5f).roundToLong().toFloat()
                        )
                    }
                }
                ImageView.ScaleType.CENTER_CROP == scaleType -> {
                    drawMatrix = drawMatrix ?: Matrix()
                    drawMatrix?.apply {
                        val scale: Float
                        var dx = 0f
                        var dy = 0f
                        if (dwidth * vheight > vwidth * dheight) {
                            scale = vheight.toFloat() / dheight.toFloat()
                            dx = (vwidth - dwidth * scale) * 0.5f
                        } else {
                            scale = vwidth.toFloat() / dwidth.toFloat()
                            dy = (vheight - dheight * scale) * 0.5f
                        }
                        setScale(scale, scale)
                        postTranslate(dx.roundToInt().toFloat(), dy.roundToInt().toFloat())
                    }

                }
                ImageView.ScaleType.CENTER_INSIDE == scaleType -> {
                    drawMatrix = drawMatrix ?: Matrix()
                    drawMatrix?.apply {
                        val scale: Float = if (dwidth <= vwidth && dheight <= vheight) {
                            1.0f
                        } else {
                            min(
                                vwidth.toFloat() / dwidth.toFloat(),
                                vheight.toFloat() / dheight.toFloat()
                            )
                        }
                        val dx = ((vwidth - dwidth * scale) * 0.5f).roundToInt().toFloat()
                        val dy = ((vheight - dheight * scale) * 0.5f).roundToInt().toFloat()
                        setScale(scale, scale)
                        postTranslate(dx, dy)
                    }

                }
                else -> {
                    // Generate the required transform.
                    val mTempSrc = RectF().apply {
                        set(0f, 0f, dwidth.toFloat(), dheight.toFloat())
                    }
                    val mTempDst = RectF().apply {
                        set(0f, 0f, vwidth.toFloat(), vheight.toFloat())
                    }
                    drawMatrix = drawMatrix ?: Matrix()
                    drawMatrix?.apply {
                        setRectToRect(
                            mTempSrc,
                            mTempDst,
                            scaleTypeToScaleToFit(scaleType)
                        )
                    }
                }
            }
        }
    }

    private fun scaleTypeToScaleToFit(st: ImageView.ScaleType): ScaleToFit = arrayOf(
        ScaleToFit.FILL,
        ScaleToFit.START,
        ScaleToFit.CENTER,
        ScaleToFit.END
    )[st.ordinal - 1]
}

@SuppressLint("RestrictedApi")
class LottieEngine private constructor(
    private val context: Context,
    private val option: LottieOptions = LottieOptions.Default
) : Drawable.Callback, ImageAssetDelegate {
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

    private val lottieDrawable = LottieDrawableImpl(option.dstWidth, option.dstHeight)
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
        lottieDrawable.invalidateDrawable(who)
    }

    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
        lottieDrawable.scheduleDrawable(who, what, `when`)
    }

    override fun unscheduleDrawable(who: Drawable, what: Runnable) {
        lottieDrawable.unscheduleDrawable(who, what)
    }

    override fun fetchBitmap(lottieImageAsset: LottieImageAsset?): Bitmap? {
        return lottieImageAsset?.let { asset ->
            option.imageDelegates.find { it.fileName == asset.fileName }?.let { imageAsset ->
                when (val res = imageAsset.res) {
                    is Bitmap, is String, is File, is Uri -> scaleBitmap(
                        context,
                        asset.width,
                        asset.height,
                        res
                    )
                    else -> null
                }
            }
        }
    }

}