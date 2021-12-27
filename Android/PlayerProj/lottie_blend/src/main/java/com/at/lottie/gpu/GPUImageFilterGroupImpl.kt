/*
 * Copyright (C) 2018 CyberAgent, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.at.lottie.gpu

import android.annotation.SuppressLint
import android.opengl.GLES20
import com.at.lottie.IFilter
import jp.co.cyberagent.android.gpuimage.GPUImageRenderer
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.util.Rotation
import jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Resembles a filter that consists of multiple filters applied after each
 * other.
 */
class GPUImageFilterGroupImpl @JvmOverloads constructor(filterList: List<IFilter>? = null) : GPUImageFilter() {
    private val tempMergedFilters: MutableList<IFilter> = mutableListOf()
    private var mergedFilters: MutableList<IFilter> = mutableListOf()
    private var frameBuffers: IntArray? = null
    private var frameBufferTextures: IntArray? = null
    private val glCubeBuffer: FloatBuffer
    private val glTextureBuffer: FloatBuffer
    private val glTextureFlipBuffer: FloatBuffer
    fun addFilter(aFilter: IFilter?) {
        if (aFilter == null) {
            return
        }
        filters.add(aFilter)
        updateMergedFilters()
    }

    /*
     * (non-Javadoc)
     * @see jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter#onInit()
     */
    override fun onInit() {
        super.onInit()
        for (filter in filters) {
            filter.getFilter().ifNeedInit()
        }
    }

    /*
     * (non-Javadoc)
     * @see jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter#onDestroy()
     */
    override fun onDestroy() {
        destroyFrameBuffers()
        for (filter in filters) {
            filter.getFilter().destroy()
        }
        super.onDestroy()
    }

    private fun destroyFrameBuffers() {
        frameBufferTextures?.also {
            GLES20.glDeleteTextures(it.size, frameBufferTextures, 0)
        }
        frameBufferTextures = null

        frameBuffers?.also {
            GLES20.glDeleteFramebuffers(it.size, frameBuffers, 0)
        }
        frameBuffers = null
    }

    /*
     * (non-Javadoc)
     * @see
     * jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter#onOutputSizeChanged(int,
     * int)
     */
    override fun onOutputSizeChanged(width: Int, height: Int) {
        super.onOutputSizeChanged(width, height)
        if (frameBuffers != null) {
            destroyFrameBuffers()
        }
        var size = filters.size
        for (i in 0 until size) {
            filters[i].getFilter().onOutputSizeChanged(width, height)
        }
        if (mergedFilters.size > 0) {
            size = mergedFilters.size
            frameBuffers = IntArray(size - 1)
            frameBufferTextures = IntArray(size - 1)
            for (i in 0 until size - 1) {
                GLES20.glGenFramebuffers(1, frameBuffers, i)
                GLES20.glGenTextures(1, frameBufferTextures, i)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameBufferTextures!![i])
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat())
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat())
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers!![i])
                GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, frameBufferTextures!![i], 0)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter#onDraw(int,
     * java.nio.FloatBuffer, java.nio.FloatBuffer)
     */
    @SuppressLint("WrongCall")
    override fun onDraw(textureId: Int, cubeBuffer: FloatBuffer, textureBuffer: FloatBuffer) {
        runPendingOnDrawTasks()
        if (!isInitialized || frameBuffers == null || frameBufferTextures == null) {
            return
        }

        tempMergedFilters.clear()
        mergedFilters.mapNotNullTo(tempMergedFilters) { iFilter ->
            if (iFilter.isEnableDraw()) iFilter else null
        }
        tempMergedFilters.also {
            val size = it.size
            var previousTexture = textureId
            for (i in 0 until size) {
                val filter = it[i]
                val isNotLast = i < size - 1
                if (isNotLast) {
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers!![i])
                    GLES20.glClearColor(0f, 0f, 0f, 0f)
                }
                when (i) {
                    0 -> filter.getFilter().onDraw(previousTexture, cubeBuffer, textureBuffer)
                    size - 1 -> filter.getFilter().onDraw(previousTexture, glCubeBuffer, if (size % 2 == 0) glTextureFlipBuffer else glTextureBuffer)
                    else -> filter.getFilter().onDraw(previousTexture, glCubeBuffer, glTextureBuffer)
                }
                if (isNotLast) {
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
                    previousTexture = frameBufferTextures!![i]
                }
            }
        }
    }

    /**
     * Gets the filters.
     *
     * @return the filters
     */
    fun getFilters(): List<IFilter> {
        return filters
    }

    fun getMergedFilters(): List<IFilter> {
        return mergedFilters
    }

    fun updateMergedFilters() {
        mergedFilters.clear()
        for (iFilter in this.filters) {
            val filter = iFilter.getFilter()
            if (filter is GPUImageFilterGroupImpl) {
                filter.updateMergedFilters()
                val filters = filter.getMergedFilters()
                if (filters.isEmpty()) continue
                mergedFilters.addAll(filters)
                continue
            }
            mergedFilters.add(iFilter)
        }
    }
    /**
     * Instantiates a new GPUImageFilterGroup with the given filters.
     *
     * @param filters the filters which represent this filter
     */
    /**
     * Instantiates a new GPUImageFilterGroup with no filters.
     */
    private var filters: MutableList<IFilter> = mutableListOf()

    init {

        filterList?.also {
            filters.addAll(it)
            updateMergedFilters()
        }
        glCubeBuffer = ByteBuffer.allocateDirect(GPUImageRenderer.CUBE.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        glCubeBuffer.put(GPUImageRenderer.CUBE).position(0)
        glTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        glTextureBuffer.put(TextureRotationUtil.TEXTURE_NO_ROTATION).position(0)
        val flipTexture = TextureRotationUtil.getRotation(Rotation.NORMAL, false, true)
        glTextureFlipBuffer = ByteBuffer.allocateDirect(flipTexture.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        glTextureFlipBuffer.put(flipTexture).position(0)
    }
}