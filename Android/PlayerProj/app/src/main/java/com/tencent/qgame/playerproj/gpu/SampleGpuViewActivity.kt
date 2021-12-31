package com.tencent.qgame.playerproj.gpu

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.Filterable
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.at.lottie.gpu.gl.*
import com.at.lottie.utils.logd
import com.blankj.utilcode.util.UriUtils
import com.tencent.qgame.playerproj.R
import com.tencent.qgame.playerproj.databinding.ActivitySampleGpuViewBinding
import com.tencent.qgame.playerproj.databinding.GpuViewArgsItemViewBinding
import java.io.Serializable
import java.util.*
import kotlin.math.roundToInt

class FilterAmount<F : BaseGlitchFilter>(
    val filter: F,
    val fname: String,
    var argsCall: ((F, String, Float) -> Unit)? = null
) : Serializable {
    val args = mutableListOf<RangeArgs>()
    fun addArg(arg: RangeArgs) = apply { args.add(arg) }
    fun addArg(argName: String, min: Int, max: Int, divCount: Int = 0) =
        apply { args.add(RangeArgs(argName, min, max, divCount)) }

    var isAutoValue = false

    fun seekBarProgress(argName: String, process: Int, max: Int = 100) {
        val scalePr = (process.toFloat() / max)
        seekBarProgress(argName, scalePr)
    }

    fun seekBarProgress(argName: String, scalePr: Float) {
        args.find { it.argName == argName }?.apply {
            this.argValue = getArgValue(scalePr)
            this@FilterAmount.argsCall?.invoke(filter, argName, argValue)
        }
    }

    fun argValue(argName: String, argValue: Float) {
        this@FilterAmount.argsCall?.invoke(filter, argName, argValue)
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

    companion object {
        private const val TAG = "SampleGpuViewActivity"
    }

    lateinit var bind: ActivitySampleGpuViewBinding

    private val gpuView get() = bind.gpuView
    private val btnLoadImg get() = bind.btnLoadImage
    private val btnAutoValue get() = bind.btnStartAutoValue
    private val recyclerView get() = bind.recycler


    private val getFilter /*by lazy { intent.getSerializableExtra("filter") as FilterAmount<*> }*/
        get() = list[0]

    private val list = mutableListOf<FilterAmount<*>>().apply {
//        add(FilterAmount(VignetteFilter(), "Vignette", { f, n, v ->
//            when (n) {
//                "offset" -> {
//                    f.setOffset(v);gpuView.requestRender()
//                }
//                "darkness" -> {
//                    f.setDarkness(v);gpuView.requestRender()
//                }
//            }
//        }).apply {
//            addArg(RangeArgs("offset", 0, 200, 2));addArg(
//            RangeArgs(
//                "darkness",
//                0,
//                200,
//                2
//            ))
//        })


//        add(FilterAmount(TiltShiftFilter(), "Tilt Shift", { f, n, v ->
//            when (n) {
//                "amount" -> {
//                    f.setAmount(v);gpuView.requestRender()
//                }
//                "position" -> {
//                    f.setPosition(v);gpuView.requestRender()
//                }
//            }
//        }).apply {
//            addArg(RangeArgs("amount", 0, 200, 4));addArg(RangeArgs("position", 0, 100, 2))
//        })


//        add(FilterAmount(VHSFilter(), "VHS", { f, n, v ->
//            when (n) {
//                "time" -> {
//                    f.setTime(v);gpuView.requestRender()
//                }
//            }
//        }).apply { isAutoValue = true;addArg(RangeArgs("time", 0, 1000, 1)) })

//        add(FilterAmount(JitterFilter(), "Jitter(Glitch)", { f, n, v ->
//            when (n) {
//                "time" -> {
//                    f.setTime(v);gpuView.requestRender()
//                }
//                "amount" -> {
//                    f.setAmount(v);gpuView.requestRender()
//                }
//                "speed" -> {
//                    f.setSpeed(v);gpuView.requestRender()
//                }
//            }
//        }).apply {
//            isAutoValue = true;
//            addArg("time", 0, 100, 2)
//            addArg("amount", 0, 50, 2)
//            addArg("speed", 0, 100, 2)
//        })


        add(FilterAmount(RGBShiftFilter(), "RGB Shift", { f, n, v -> when(n) {
            "angle" -> {f.setAngle(v);gpuView.requestRender()}
            "amount" -> {f.setAmount(v);gpuView.requestRender()}
        } }).apply {
            isAutoValue = true
            addArg("angle", 0, 628, 2)
            addArg("amount", 0, 100, 3)
        })
    }


    private val getContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            UriUtils.uri2File(uri)?.apply {
                gpuView.setImage(this)
            }
        }

    private val valueAnimator by lazy {
        ValueAnimator.ofFloat(0f, 100f).setDuration(1000).apply {
            var times = 0f
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            addUpdateListener { anim ->
                val value = (anim.animatedValue as Float)
                logd(TAG, "auto value:${value}")
                getFilter.apply {
                    times = value / 100
                    argValue(args[0].argName, times)
                }
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?) {
                    times = 0f
                }
            })
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivitySampleGpuViewBinding.inflate(layoutInflater)
        setContentView(bind.root)

        gpuView.postOnAnimation {
            onRenderFilter()
        }

        btnLoadImg.setOnClickListener { getContent.launch("image/*") }
        btnAutoValue.setOnClickListener {
            if (!valueAnimator.isRunning) {
                valueAnimator.start()
                btnAutoValue.text = "stop auto value"
            } else {
                btnAutoValue.text = "start auto value"
                valueAnimator.cancel()
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        recyclerView.adapter = Adapter(getFilter)
    }


    private fun onRenderFilter() {
        btnLoadImg.visibility = View.VISIBLE
        btnAutoValue.visibility = if (getFilter.isAutoValue) View.VISIBLE else View.GONE
        gpuView.filter = getFilter.filter
        gpuView.requestRender()
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
                filterAmount.seekBarProgress(seekBar.tag as String, progress, seekBar.max)
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
        valueAnimator.removeAllUpdateListeners()
        valueAnimator.cancel()
        getContent.unregister()
    }
}