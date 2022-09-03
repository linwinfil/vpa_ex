package com.tencent.qgame.playerproj

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Picture
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.airbnb.lottie.ImageAssetDelegate
import com.airbnb.lottie.LottieAnimationView
interface ILottie {
    val lottieAssetDelegate: ImageAssetDelegate?
    val lottieAssetImg: String
    val lottieAssetFile: String
    fun onDrawPicture(view: LottieSpaceView, picture: LottieFrame)
}
class LottieFrame(val frame: Int, val endFrame: Int, val startFrame: Int) {
    private val picture = Picture()
    val width: Int get() = picture.width
    val height: Int get() = picture.height
    fun drawTo(canvas: Canvas) = picture.draw(canvas)
    fun drawOn(view: View) {
        val canvas = picture.beginRecording(view.width, view.height)
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)
        view.draw(canvas)
        picture.endRecording()
    }

    val nextFrame = frame + 1
    val isTail = frame == endFrame
    val isHead = frame == 0
}

class LottieSpaceView : ConstraintLayout {
    lateinit var lottieView: LottieAnimationView
    private var lottie: ILottie? = null

    private val duration: Float get() = lottieView.composition?.duration ?: 0.0f
    private val frameRate: Float get() = lottieView.composition?.frameRate ?: 0f
    private val endFrame: Int get() = lottieView.composition?.endFrame?.toInt() ?: 0
    private val startFrame: Int get() = lottieView.composition?.startFrame?.toInt() ?: 0
    private val frameCount: Int get() = (endFrame - startFrame)

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        lottieView = LottieAnimationView(context).also {
            it.visibility = INVISIBLE
            addView(it, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            it.repeatCount = 0
        }
        lottieView.addAnimatorUpdateListener {
            lottie?.also {
                val frame = LottieFrame(lottieView.frame, endFrame, startFrame).apply { drawOn(lottieView) }
                it.onDrawPicture(this@LottieSpaceView, frame)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
    }

    fun postLottieDraw(frame: Int) {
        post {
            lottieView.frame = frame
        }
    }

    fun initLottie(iLottie: ILottie) {
        lottie = iLottie
        lottieView.setImageAssetDelegate(iLottie.lottieAssetDelegate)
        lottieView.imageAssetsFolder = iLottie.lottieAssetImg
        lottieView.setAnimation(iLottie.lottieAssetFile)
    }
}