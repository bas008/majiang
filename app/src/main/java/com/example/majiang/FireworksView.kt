package com.example.fireworks

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.*
import kotlin.math.*
import kotlin.random.Random

class FireworksView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 画笔
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // 存储所有烟花
    private val fireworks = mutableListOf<Firework>()

    // 随机数生成器
    private val random = Random.Default

    // 动画控制器
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 16
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { invalidate() }
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 随机添加新烟花
        if (random.nextFloat() < 0.15f) { // 2%的概率生成新烟花
            fireworks.add(createRandomFirework())
        }

        // 更新并绘制所有烟花
        val iterator = fireworks.iterator()
        while (iterator.hasNext()) {
            val firework = iterator.next()
            firework.update()
            firework.draw(canvas)
            if (firework.isFinished) {
                iterator.remove()
            }
        }
    }

    // 创建随机烟花
    private fun createRandomFirework(): Firework {
        val startX = random.nextInt(width).toFloat()
        val startY = height.toFloat()
        val endX = random.nextInt(width).toFloat()
        val endY = random.nextInt(height / 2).toFloat() - height / 4

        // 随机颜色
        val color = Color.rgb(
            max(150, random.nextInt(256)),  // 确保颜色亮度足够（最低150）
            max(150, random.nextInt(256)),
            max(150, random.nextInt(256))
        )

        return Firework(
            startX = startX,
            startY = startY,
            endX = endX,
            endY = endY,
            color = color,
            particleCount = random.nextInt(60) + 80 // 50-100个粒子
        )
    }

    // 烟花类
    inner class Firework(
        private val startX: Float,
        private val startY: Float,
        private val endX: Float,
        private val endY: Float,
        private val color: Int,
        private val particleCount: Int
    ) {
        // 发射阶段动画
        private val launchAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = random.nextLong(500) + 1000L // 1-1.5秒
            interpolator = DecelerateInterpolator(1.5f)
        }

        // 粒子
        private val particles = mutableListOf<Particle>()

        // 状态
        var isFinished = false
            private set

        init {
            launchAnimator.start()
        }

        fun update() {
            if (launchAnimator.isRunning) {
                // 还在发射阶段
                return
            }

            if (particles.isEmpty()) {
                // 发射结束，创建粒子
                createParticles()
            } else {
                // 更新粒子
                particles.forEach { it.update() }
                // 检查是否所有粒子都已消失
                isFinished = particles.all { it.alpha <= 0 }
            }
        }

        fun draw(canvas: Canvas) {
            if (launchAnimator.isRunning) {
                // 绘制发射轨迹
                val progress = launchAnimator.animatedValue as Float
                val currentX = startX + (endX - startX) * progress
                val currentY = startY + (endY - startY) * progress

                paint.color = color
                canvas.drawCircle(currentX, currentY, 6f, paint)

                // 绘制尾迹
                for (i in 1..5) {
                    val trailProgress = max(0f, progress - i * 0.05f)
                    val trailX = startX + (endX - startX) * trailProgress
                    val trailY = startY + (endY - startY) * trailProgress
                    paint.alpha = (120 - i * 20).coerceAtLeast(0)
                    canvas.drawCircle(trailX, trailY, 5f - i * 0.5f, paint)
                }
                paint.alpha = 255
            } else {
                // 绘制粒子
                particles.forEach { it.draw(canvas) }
            }
        }

        // 创建爆炸粒子
        private fun createParticles() {
            for (i in 0 until particleCount) {
                // 随机方向和速度
                val angle = random.nextFloat() * 2 * PI.toFloat()
                val speed = random.nextFloat() * 5 + 3

                particles.add(
                    Particle(
                        x = endX,
                        y = endY,
                        vx = cos(angle) * speed,
                        vy = sin(angle) * speed,
                        color = color,
                        size = random.nextFloat() * 2 + 2
                    )
                )
            }
        }
    }

    // 粒子类
    inner class Particle(
        var x: Float,
        var y: Float,
        private var vx: Float,
        private var vy: Float,
        private val color: Int,
        private val size: Float
    ) {
        var alpha = 255
        private val gravity = 0.05f // 重力效果
        private val fadeRate = random.nextInt(3) + 2 // 2-4的消失速度

        fun update() {
            // 应用速度和重力
            x += vx
            y += vy
            vy += gravity

            // 逐渐消失
            alpha -= fadeRate
            if (alpha < 0) alpha = 0
        }

        fun draw(canvas: Canvas) {
            paint.color = color
            paint.alpha = alpha
            canvas.drawCircle(x, y, size, paint)
            paint.alpha = 255 // 重置透明度
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.end() // 停止动画
    }
    fun startFireworks() {
        visibility = View.VISIBLE
        fireworks.clear() // 清除现有烟花
        animator.start()
    }

    fun stopFireworks() {
        animator.end()
        fireworks.clear()
        visibility = View.GONE
    }
}