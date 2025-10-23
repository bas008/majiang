package com.example.majiang

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView


class PromptAdapter (private val recyclerView: RecyclerView):    RecyclerView.Adapter<PromptAdapter.ViewHolder>() {
    var booleanArrays = Array(row_Number) { BooleanArray(col_Number) }
    private val handler = Handler(Looper.getMainLooper())
    override fun getItemCount(): Int = row_Number * col_Number

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.item_image)
    }

    // 加载item布局（单个图片的布局）
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false) // 引入item布局
        return ViewHolder(view)
    }

    // 绑定数据到item
    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = position / col_Number
        val col = position % col_Number
        val value = booleanArrays[row][col]
        if(value) {
            holder.imageView.setBackgroundResource(R.drawable.circle_prompt)
            holder.imageView.setImageDrawable(null)
        }else{
            holder.imageView.setImageDrawable(null)
            holder.imageView.setBackgroundColor(Color.TRANSPARENT)
        }
    }
    fun updateData(newBooleanArrays: Array<BooleanArray>) {
        booleanArrays = newBooleanArrays

        // 通知列表刷新
        notifyDataSetChanged()
        recyclerView.visibility = View.VISIBLE // 直接操作
        handler.postDelayed ({ recyclerView.visibility = View.GONE },3000)
    }
}