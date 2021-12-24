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

    private fun init(attrs: AttributeSet?, @AttrRes defStyleAttr: Int) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.LottieBlendView, defStyleAttr, 0)
        typedArray.getString(R.styleable.LottieBlendView_blend_bg_fileName)?.apply { lottieBgFileName = this }
        typedArray.getString(R.styleable.LottieBlendView_blend_fg_fileName)?.apply { lottieFgFileName = this }
        if (lottieFgFileName.isEmpty() || lottieBgFileName.isEmpty()) {
            throw IllegalArgumentException("lottie bg or fg file name cannot be empty!")
        }
        typedArray.getString(R.styleable.LottieBlendView_blend_bg_imageAssetsFolder)?.apply { lottieBgImageAssetFolder = this }
        typedArray.getString(R.styleable.LottieBlendView_blend_fg_imageAssetsFolder)?.apply { lottieFgImageAssetFolder = this }
        if (lottieFgImageAssetFolder.isEmpty() || lottieBgImageAssetFolder.isEmpty()) {
            throw IllegalArgumentException("lottie bg or fg image asset folder cannot be empty!")
        }
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
                createdLottieFlag = createdLottieFlag.or(1 shl 1)
                Log.d(TAG, "lottie bg: ${this.width}*${this.height}")
            }
        }
        lottieViewFg = LottieAnimationView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            addView(this)
            loop(false)
            visibility = if (initShowLottie) VISIBLE else INVISIBLE
            postOnAnimation {
                createdLottieFlag = createdLottieFlag.or(1 shl 2)
                Log.d(TAG, "lottie fg: ${this.width}*${this.height}")
            }
        }
    }

    override fun initLottie() {
        if (createdLottieFlag.and(1 shl 1) == 0 || createdLottieFlag.and(1 shl 2) == 0) {
            postOnAnimation {
                initLottie()
            }
            return
        }
        if (createdLottieFlag.and(1 shl 3) != 0) return
        createdLottieFlag = createdLottieFlag.or(1 shl 3)
        lottieViewBg.apply {
            imageAssetsFolder = lottieBgImageAssetFolder
            setAnimation(lottieBgFileName)
        }
        lottieViewFg.apply {
            imageAssetsFolder = lottieFgImageAssetFolder
            setAnimation(lottieFgFileName)
        }
    }

    override fun getLottieViewBg(): LottieAnimationView = lottieViewBg
    override fun getLottieViewFg(): LottieAnimationView = lottieViewFg


    fun showLottieAnimationView() {
        lottieViewBg.visibility = VISIBLE
        lottieViewFg.visibility = VISIBLE
    }

    fun hideLottieAnimationView() {
        lottieViewBg.visibility = INVISIBLE
        lottieViewFg.visibility = INVISIBLE
    }
}