package com.tencent.qgame.playerproj.gpu

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Filterable
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.at.lottie.gpu.gl.JitterFilter
import com.at.lottie.gpu.gl.TiltShiftFilter
import com.at.lottie.gpu.gl.VHSFilter
import com.at.lottie.gpu.gl.VignetteFilter
import com.tencent.qgame.playerproj.R
import com.tencent.qgame.playerproj.databinding.ActivitySampleGpuMainBinding

class SampleGpuMainActivity : AppCompatActivity() {
    lateinit var bind: ActivitySampleGpuMainBinding
    private val recyclerView get() = bind.recyclerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivitySampleGpuMainBinding.inflate(layoutInflater)
        setContentView(bind.root)

        recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        recyclerView.adapter = Adapter() {
            val p = it.tag as Int
            Intent(this, SampleGpuViewActivity::class.java).apply {
                putExtra("filter", list[p])
                startActivity(this)
            }
        }
    }

    private val list = mutableListOf<FilterAmount<*>>()

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
            holder.itemView.tag = position
            holder.itemView.setOnClickListener(click)
            ((holder.itemView as ViewGroup).getChildAt(0) as AppCompatButton).text =
                list[position].fname
        }

        override fun getItemCount(): Int = list.size

    }

    private class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {}
}