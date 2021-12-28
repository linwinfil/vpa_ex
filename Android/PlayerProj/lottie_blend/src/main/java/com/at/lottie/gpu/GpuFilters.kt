package com.at.lottie.gpu

import com.at.lottie.IFilter
import com.at.lottie.gpu.gl.*

/** @author lmx
 * Created by lmx on 2021/12/28.
 */


internal class GpuFilter<T : IFilter>(val id: Int, val clazz: Class<T>, val intensity: Float)
object GpuFilters {
    private const val filter_glitch = 1
    private const val filter_rgb_shift = 2
    private const val filter_noise = 3
    private const val filter_bad_tv = 4
    private const val filter_shake = 5
    private const val filter_rainbow = 6
    private const val filter_dot_matrix = 7
    private const val filter_blur = 8
    private const val filter_monochrome = 9
    private const val filter_vertigo = 10

    private val filters = arrayOf(
        GpuFilter(filter_glitch, JitterFilter::class.java, 0.18f),
        GpuFilter(filter_rgb_shift, RGBShiftFilter::class.java, 0.28f),
        GpuFilter(filter_noise, ScanlinesFilter::class.java, 1.0f),
        GpuFilter(filter_bad_tv, BadTVFilter::class.java, 0.4f),
        GpuFilter(filter_shake, ShakeFilter::class.java, 0.44f),
        GpuFilter(filter_rainbow, RainbowFilter::class.java, 0.5f),
        GpuFilter(filter_dot_matrix, DotMatrixFilter::class.java, 0.55f),
        GpuFilter(filter_blur, BarrelBlurFilter::class.java, 0.44f),
        GpuFilter(filter_monochrome, HalftoneJitterFilter::class.java, 0.28f),
        GpuFilter(filter_vertigo, RGBShiftShakeFilter::class.java, 0.11f),

        //更多滤镜 coming soon
    )

    fun getGpuFilter(id: Int): IFilter? {
        return filters.find { it.id == id }?.clazz?.newInstance()
    }

    fun getIntensity(t: IFilter): Float = filters.find { it.clazz == t::class.java }?.intensity ?: 0f

}