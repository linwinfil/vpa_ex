package com.tencent.qgame.playerproj.gpu

import android.annotation.SuppressLint
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.at.lottie.gpu.gl.BaseGlitchFilter
import com.at.lottie.gpu.gl.TiltShiftFilter
import com.at.lottie.gpu.gl.VignetteFilter
import com.blankj.utilcode.util.UriUtils
import com.tencent.qgame.playerproj.R
import com.tencent.qgame.playerproj.databinding.ActivitySampleGpuViewBinding
import com.tencent.qgame.playerproj.databinding.GpuViewArgsItemViewBinding
import java.util.*
import kotlin.math.roundToInt

class FilterAmount<F : BaseGlitchFilter>(
    val filter: F,
    val fname: String,
    val argsCall: (F, String, Float) -> Unit
) {
    val args = mutableListOf<RangeArgs>()
    fun addArg(arg: RangeArgs) = apply { args.add(arg) }

    fun seekBarProgress(seekBar: SeekBar) {
        val argName = seekBar.tag as String
        val scalePr = (seekBar.progress.toFloat() / seekBar.max)
        args.find { it.argName == argName }?.apply {
            this.argValue = getArgValue(scalePr)
            argsCall(filter, argName, argValue)

        }
    }
}

data class RangeArgs(
    val argName: String,
    val min: Int,
    val max: Int,
    val divCount: Int = 0//除以位数
) {
    fun getArgValue(scalePr: Float): Float {
        return if (divCount <= 0) (min + ((max - min) * scalePr).roundToInt()).toFloat()
        else {
            val sb = StringBuilder("1")
            for (i in 0 until divCount) {
                sb.append("0")
            }
            (min + ((max - min) * scalePr)) / sb.toString().toInt()
        }
    }

    @Transient
    var argValue: Float = 0f

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RangeArgs
        if (argName != other.argName) return false
        return true
    }

    override fun hashCode(): Int {
        return argName.hashCode()
    }
}

class SampleGpuViewActivity : AppCompatActivity() {
    lateinit var bind: ActivitySampleGpuViewBinding

    private val gpuView get() = bind.gpuView
    private val btnLoadImg get() = bind.btnLoadImage
    private val recyclerView get() = bind.recycler


    private val filters = mutableListOf<FilterAmount<*>>().apply {
        var f: FilterAmount<*> = FilterAmount(VignetteFilter(), "Vignette", { f, n, v ->
            when (n) {
                "offset" -> {
                    f.setOffset(v)
                    gpuView.requestRender()
                }
                "darkness" -> {
                    f.setDarkness(v)
                    gpuView.requestRender()
                }
            }
        }).apply {
            addArg(RangeArgs("offset", 0, 200, 2))
            addArg(RangeArgs("darkness", 0, 200, 2))
        }

        f = FilterAmount(TiltShiftFilter(), "Tilt Shift", { f, n, v ->
            when (n) {
                "amount" -> {
                    f.setAmount(v)
                    gpuView.requestRender()
                }
                "position" -> {
                    f.setPosition(v)
                    gpuView.requestRender()
                }
            }
        }).apply {
            addArg(RangeArgs("amount", 0, 200, 4))
            addArg(RangeArgs("position", 0, 100, 2))
        }
        add(f)

    }
    private val getFilter get() = filters[0]

    private val getContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            UriUtils.uri2File(uri)?.apply {
                gpuView.setImage(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivitySampleGpuViewBinding.inflate(layoutInflater)
        setContentView(bind.root)

        gpuView.postOnAnimation {
            gpuView.filter = getFilter.filter
            gpuView.requestRender()
            btnLoadImg.visibility = View.VISIBLE
        }

        btnLoadImg.setOnClickListener { getContent.launch("image/*") }

        recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        recyclerView.adapter = Adapter(getFilter)
    }

    class NotifyRunnable(var position: Int, private val runBlock: (Int) -> Unit) : Runnable {
        override fun run() {
            runBlock(position)
        }
    }

    class Adapter(
        private val filterAmount: FilterAmount<*>
    ) :
        RecyclerView.Adapter<Holder>() {

        val uiHandler = Handler(Looper.getMainLooper())
        @SuppressLint("NotifyDataSetChanged")
        val notifyRunnable = NotifyRunnable(-1) { position ->
            if (position >= 0) {
                this.notifyItemChanged(position)
            } else {
                this.notifyDataSetChanged()
            }
        }

        val seek = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                filterAmount.seekBarProgress(seekBar)
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val argName = seekBar.tag as String
                filterAmount.args.indexOfFirst { it.argName == argName }.takeIf { it >= 0 }?.apply {
                    uiHandler.removeCallbacks(notifyRunnable)
                    uiHandler.postDelayed(notifyRunnable, 400)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            return Holder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.gpu_view_args_item_view, parent, false)
            )
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(filterAmount.args[position], seek)
        }

        override fun getItemCount(): Int {
            return filterAmount.args.size
        }

    }

    class Holder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val bind = GpuViewArgsItemViewBinding.bind(itemView)
        @SuppressLint("SetTextI18n")
        fun bind(args: RangeArgs, seek: SeekBar.OnSeekBarChangeListener) {
            bind.tvTitle.text = "${args.argName}:${String.format(Locale.US, "%.2f", args.argValue)}"
            bind.seekBar.tag = args.argName
            bind.seekBar.setOnSeekBarChangeListener(seek)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        getContent.unregister()
    }
}