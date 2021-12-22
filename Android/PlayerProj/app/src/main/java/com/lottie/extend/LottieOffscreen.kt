package com.lottie.extend

import android.animation.ValueAnimator
import com.airbnb.lottie.LottieDrawable

/**
 * Created by linmaoxin on 2021/12/22
 */
class LottieOffscreen private constructor() {

    class LottieOptions {
        var imageAssetFolder: String? = null

    }

    private var isInitialized = false
    private val lottieDrawable: LottieDrawable = LottieDrawable()

    private lateinit var animator: ValueAnimator


    public fun getLottie(option: LottieOptions): LottieOffscreen {

        return this
    }


}