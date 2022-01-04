package com.tencent.qgame.playerproj.gpu

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.at.lottie.gpu.gl.*
import com.tencent.qgame.playerproj.databinding.ActivitySampleGpuMainBinding

class SampleGpuMainActivity : AppCompatActivity() {
    lateinit var bind: ActivitySampleGpuMainBinding
    private val recyclerView get() = bind.recyclerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivitySampleGpuMainBinding.inflate(layoutInflater)
        setContentView(bind.root)

        recyclerView.postOnAnimationDelayed( {
            recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
            recyclerView.adapter = Adapter {
                val p = it.tag as Int
                Intent(this, SampleGpuViewActivity::class.java).apply {
                    SampleGpuViewActivity.filter = list[p]
                    startActivity(this)
                }
            }
        }, 250)
    }

    private val list = mutableListOf<FilterAmount<*>>().apply {
        add(FilterAmount(VignetteFilter(), "Vignette", { f, n, v ->
            when (n) {
                "offset" ->  f.setOffset(v)
                "darkness" -> f.setDarkness(v)
            }
        }).apply {
            addArg(RangeArgs("offset", 0, 200, 2))
            addArg(RangeArgs("darkness", 0, 200, 2))
        })

        add(FilterAmount(TiltShiftFilter(), "Tilt Shift", { f, n, v ->
            when (n) {
                "amount" -> { f.setAmount(v); }
                "position" -> {f.setPosition(v); }
            }
        }).apply {
            addArg(RangeArgs("amount", 0, 200, 4))
            addArg(RangeArgs("position", 0, 100, 2))
        })

        add(FilterAmount(VHSFilter(), "VHS", { f, n, v ->
            when (n) {
                "time" -> { f.setTime(v)}
            }
        }).apply { isAutoValue = true;
            addArg(RangeArgs("time", 0, 1000, 1)) })

        add(FilterAmount(JitterFilter(), "Jitter(Glitch)", { f, n, v ->
            when (n) {
                "time" -> {f.setTime(v);}
                "amount" -> {f.setAmount(v);}
                "speed" -> {f.setSpeed(v);}
            }
        }).apply {
            isAutoValue = true;
            addArg("time", 0, 100, 2)
            addArg("amount", 0, 50, 2)
            addArg("speed", 0, 100, 2)
        })

        add(FilterAmount(RGBShiftFilter(), "RGB Shift", { f, n, v -> when(n) {
            "time" -> {f.setTime(v)}
            "angle" -> {f.setAngle(v);}
            "amount" -> {f.setAmount(v);}
        } }).apply {
            isAutoValue = true
            addArg("time", 0, 100, 2)
            addArg("angle", 0, 628, 2)
            addArg("amount", 0, 100, 3)
        })

        add(FilterAmount(ScanlinesFilter(), "Noise", { f, n, v ->
            when (n) {
                "time" -> f.setTime(v)
                "count" -> f.setCount(v)
                "noiseAmount" -> f.setNoiseAmount(v)
                "lineAmount" -> f.setLineAmount(v)
        } }).apply {
            isAutoValue = true
            addArg("time", 0, 100, 0)
            addArg("count", 0, 100, 2)
            addArg("noiseAmount", 0, 200, 2)
            addArg("lineAmount", 0, 200, 2)
        })

        add(FilterAmount(BadTVFilter(), "BadTv", {f, n, v -> when(n) {
            "time" -> f.setTime(v)
           "distortion" -> f.setDistortion(v)
           "distortion2" -> f.setDistortion2(v)
           "speed" -> f.setSpeed(v)
           "rollSpeed" -> f.setRollSpeed(v)
        } }).apply {
            isAutoValue = true
            addArg("time", 0, 100, 2)
            addArg("distortion", 0, 600, 2)
            addArg("distortion2", 0, 600, 2)
            addArg("speed", 0, 100, 2)
            addArg("rollSpeed", 0, 300, 2)
        })

        add(FilterAmount(ShakeFilter(), "Shake", { f, n, v -> when (n) {
            "time"-> {f.setTime(v)}
            "amount" -> {f.setAmount(v)}
        }}).apply {
            isAutoValue = true
            addArg("time", 0, 100, 2)
            addArg("amount", 0, 200, 3)
        })

        add(FilterAmount(RainbowFilter(), "Rainbow", { f, n, v -> when (n) {
            "time" -> f.setTime(v)
            "amount" -> f.setAmount(v)
            "offset" -> f.setOffset(v)
        } }).apply {
            isAutoValue = true
            addArg("time", 0, 100, 2)
            addArg("amount", 0, 800, 3)
            addArg("offset", 0, 200, 2)
        })

        add(FilterAmount(DotMatrixFilter(), "Dot Matrix", { f, n, v -> when (n) {
            "dots" -> f.setDots(v)
            "size" -> f.setSize(v)
            "blur" -> f.setBlur(v)
        } }).apply {
            addArg("dots", 0, 200, 0)
            addArg("size", 0, 100, 2)
            addArg("blur", 0, 100, 2)
        })

        add(FilterAmount(BarrelBlurFilter(), "Barrel", { f, n, v -> when (n) {
            "amount" -> f.setAmount(v)
        } }).apply {
            addArg("amount", 0, 200, 3)
        })

        add(FilterAmount(HalfToneFilter(), "Half tone", { f, n, v -> when (n) {
           "center" -> f.setCenter(v)
           "angle" -> f.setAngle(v)
           "scale" -> f.setScale(v)
           "tSize" -> f.setTSize(v)
        } }).apply {
            addArg("center", 0, 100, 0)
            addArg("angle", 0, 100, 2)
            addArg("scale", 0, 100, 2)
            addArg("tSize", 0, 100, 2)
        })
    }


    private inner class Adapter(private val click: View.OnClickListener) :
        RecyclerView.Adapter<Holder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val layout = ConstraintLayout(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
                )
                addView(AppCompatButton(context).apply {
                    layoutParams = ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                    ).also {
                        it.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                        it.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                        it.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                        it.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    }
                    setTextColor(Color.WHITE)
                    isAllCaps = false
                })
            }
            return Holder(layout)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.button.tag = position
            holder.button.setOnClickListener(click)
            holder.button.text = list[position].fname
        }

        override fun getItemCount(): Int = list.size

    }

    private class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val button = (itemView as ViewGroup).getChildAt(0) as AppCompatButton
    }
}