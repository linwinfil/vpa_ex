package com.at.lottie.gpu

import com.at.lottie.IFilter
import com.at.lottie.gpu.gl.*

/** @author lmx
 * Created by lmx on 2021/12/28.
 */


class GpuFilter<T : IFilter>(val id: Int, val clazz: Class<T>, val intensity: Float, val name: String)
object GpuFilters {
    const val filter_glitch = 1
    const val filter_rgb_shift = 2
    const val filter_noise = 3
    const val filter_bad_tv = 4
    const val filter_shake = 5
    const val filter_rainbow = 6
    const val filter_dot_matrix = 7
    const val filter_blur = 8
    const val filter_monochrome = 9
    const val filter_vertigo = 10

    private val filters = arrayOf(
        GpuFilter(filter_glitch,     JitterFilter::class.java,         0.18f, "Glitch"),
        GpuFilter(filter_rgb_shift,  RGBShiftFilter::class.java,       0.28f, "RGB Shift"),
        GpuFilter(filter_noise,      ScanlinesFilter::class.java,      1.0f,  "Noise"),
        GpuFilter(filter_bad_tv,     BadTVFilter::class.java,          0.4f,  "BadTV"),
        GpuFilter(filter_shake,      ShakeFilter::class.java,          0.20f, "Shake"),
        GpuFilter(filter_rainbow,    RainbowFilter::class.java,        0.5f,  "Rainbow"),
        GpuFilter(filter_dot_matrix, DotMatrixFilter::class.java,      0.55f, "Dot Matrix"),
        GpuFilter(filter_blur,       BarrelBlurFilter::class.java,     0.44f, "Blur"),
        GpuFilter(filter_monochrome, HalftoneJitterFilter::class.java, 0.28f, "Manochrome"),
        GpuFilter(filter_vertigo,    RGBShiftShakeFilter::class.java,  0.11f, "Vertigo"),

        //todo 更多滤镜 coming soon
    )

    fun getGpuFilter(id: Int): IFilter? {
        return filters.find { it.id == id }?.clazz?.newInstance()
    }

    fun getGpuFilters(): MutableList<GpuFilter<*>> = filters.toMutableList()

    fun getIntensity(t: IFilter, def: Float = 0.2f): Float = filters.find { it.clazz == t::class.java }?.intensity ?: def
    fun <T : IFilter> getIntensity(clazz: Class<T>, def: Float = 0.2f): Float = filters.find { it.clazz == clazz }?.intensity ?: def

}