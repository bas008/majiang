package com.example.majiang

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View

class GridBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val tag = "Fandy_GridBackgroundView"
    // 画笔：绘制网格线
    private val gridPaint = Paint().apply {
        color = Color.BLACK // 网格线颜色（黑色）
        strokeWidth = 3.dpToPx() // 线宽
        style = Paint.Style.STROKE // 仅描边
        isAntiAlias = true // 抗锯齿
    }
    val yellowBorderPaint = Paint().apply {
        color = Color.YELLOW // 边框颜色：黄色
        strokeWidth = 5.dpToPx() // 边框宽度：5px
        style = Paint.Style.STROKE // 仅描边（不填充）
        isAntiAlias = true // 抗锯齿，让边框更平滑
    }
    // 底色画笔（绿色）
    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#61e67d") // 绿色底色
        style = Paint.Style.FILL // 填充
    }

    // 网格行列数（10列8行）
    private val columnCount = 10
    private val rowCount = 8

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 1. 绘制绿色底色
        Log.d(tag,"GridBackgroundView size ($width,$height)")
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        // 2. 计算每个小矩形的宽高
        val cellWidth = (width.toFloat() - 10.dpToPx()) / columnCount
        val cellHeight = (height.toFloat()- 10.dpToPx()) / rowCount

        // 3. 绘制垂直线（10列需要11条线，左右边缘各1条）
        for (col in 0..columnCount) {
            val x = col * cellWidth
            canvas.drawLine(x+5.dpToPx(), 0f, x+5.dpToPx(), height.toFloat(), gridPaint)
        }

        // 4. 绘制水平线（8行需要9条线，上下边缘各1条）
        for (row in 0..rowCount) {
            val y = row * cellHeight
            canvas.drawLine(0f, y+5.dpToPx(), width.toFloat(), y+5.dpToPx(), gridPaint)
        }
        // 绘制边框矩形（注意：留出一半线宽，避免边框超出View范围）
        // 原因：strokeWidth是向两边扩展的，若从0,0开始画，会有一半线宽超出View显示范围
        val borderOffset = yellowBorderPaint.strokeWidth / 2f
        canvas.drawRect(
            borderOffset, // 左边界（向右偏移一半线宽）
            borderOffset, // 上边界（向下偏移一半线宽）
            width.toFloat() - borderOffset, // 右边界（向左偏移一半线宽）
            height.toFloat() - borderOffset, // 下边界（向上偏移一半线宽）
            yellowBorderPaint
        )
    }
    private fun Int.dpToPx(): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, // 单位为dp
            this.toFloat(),
            context.resources.displayMetrics // 屏幕密度信息
        )
    }
}