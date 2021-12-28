package com.at.lottie

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import androidx.annotation.AttrRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.airbnb.lottie.LottieAnimationView

/**
 * Created by linmaoxin on 2021/12/23
 */

class LottieBlendView : ConstraintLayout, IBlend {
    companion object {
        private const val TAG = "${Constant.TAG}.BlendView"
    }

    constructor(context: Context) : super(context) {
        init(null, R.attr.lottieBlendViewStyle)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs, R.attr.lottieBlendViewStyle)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs, defStyleAttr)
    }

    private var lottieBgFileName: String = ""
    private var lottieFgFileName: String = ""

    private var lottieBgImageAssetFolder: String = ""
    private var lottieFgImageAssetFolder: String = ""

    private lateinit var lottieViewBg: LottieAnimationView
    private lateinit var lottieViewFg: LottieAnimationView

    private var initShowLottie = false
    private var createdLottieFlag: Int = 0
    private val isCreatedLottieFlag:Boolean get() = createdLottieFlag.and(1 shl 1) != 0 && createdLottieFlag.and(1 shl 2) != 0
    private fun addCreateLottieFlag(flag: Int) {
        createdLottieFlag = createdLottieFlag.or(1 shl flag)
    }

    private var initializedLottie = false

    private fun init(attrs: AttributeSet?, @AttrRes defStyleAttr: Int) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.LottieBlendView, defStyleAttr, 0)
        typedArray.getString(R.styleable.LottieBlendView_blend_bg_fileName)?.apply { lottieBgFileName = this }
        typedArray.getString(R.styleable.LottieBlendView_blend_fg_fileName)?.apply { lottieFgFileName = this }
        typedArray.getString(R.styleable.LottieBlendView_blend_bg_imageAssetsFolder)?.apply { lottieBgImageAssetFolder = this }
        typedArray.getString(R.styleable.LottieBlendView_blend_fg_imageAssetsFolder)?.apply { lottieFgImageAssetFolder = this }
        initShowLottie = typedArray.getBoolean(R.styleable.LottieBlendView_blend_show_lottie, initShowLottie)
        typedArray.recycle()
        setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        lottieViewBg = LottieAnimationView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            addView(this)
            loop(false)
            visibility = if (initShowLottie) VISIBLE else INVISIBLE
            postOnAnimation {
                addCreateLottieFlag(1)
                Log.d(TAG, "lottie bg: ${this.width}*${this.height}")
            }
        }
        lottieViewFg = LottieAnimationView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            addView(this)
            loop(false)
            visibility = if (initShowLottie) VISIBLE else INVISIBLE
            postOnAnimation {
                addCreateLottieFlag(2)
                Log.d(TAG, "lottie fg: ${this.width}*${this.height}")
            }
        }
    }

    override fun initLottie() {
        if (initializedLottie) return
        if (!isCreatedLottieFlag) {
            initializedLottie = true
            postOnAnimation {
                initializedLottie = false
                initLottie()
            }
            return
        }
        if (createdLottieFlag.and(1 shl 3) != 0) return

        if (lottieBgFileName.isEmpty()) {
            throw IllegalArgumentException("lottie bg file name cannot be empty!")
        }
        if (lottieBgImageAssetFolder.isEmpty()) {
            throw IllegalArgumentException("lottie bg image asset folder cannot be empty!")
        }
        if ((lottieFgFileName.isNotEmpty() && lottieFgImageAssetFolder.isEmpty()) ||
            (lottieFgFileName.isEmpty() && lottieFgImageAssetFolder.isNotEmpty())) {
            throw IllegalArgumentException("lottie [fg image asset folder] or [file name] cannot be empty!")
        }
        createdLottieFlag = createdLottieFlag.or(1 shl 3)
        lottieViewBg.apply {
            imageAssetsFolder = lottieBgImageAssetFolder
            setAnimation(lottieBgFileName)
        }
        lottieFgFileName.takeIf { it.isNotEmpty() }?.apply {
            lottieViewFg.apply {
                imageAssetsFolder = lottieFgImageAssetFolder
                setAnimation(lottieFgFileName)
            }
        }
        initializedLottie = true
    }

    override fun getLottieViewBg(): LottieAnimationView = lottieViewBg
    override fun getLottieViewFg(): LottieAnimationView = lottieViewFg
    override fun setAssetFile(fileName: String, imageAssetFolder: String, type: FrameType) {
        if (type == FrameType.Background) {
            lottieBgFileName = fileName
            lottieBgImageAssetFolder = imageAssetFolder
        } else if (type == FrameType.Foreground) {
            lottieFgFileName = fileName
            lottieFgImageAssetFolder = imageAssetFolder
        }
    }

    fun showLottieAnimationView() {
        lottieViewBg.visibility = VISIBLE
        lottieViewFg.visibility = VISIBLE
    }

    fun hideLottieAnimationView() {
        lottieViewBg.visibility = INVISIBLE
        lottieViewFg.visibility = INVISIBLE
    }
}