package com.example.majiang

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 定义回调接口
interface TimerCallback {
    fun onTick(seconds: Int) // 每秒触发，参数为已运行秒数
}

class SecondTimer(private val callback: TimerCallback) {
    private var elapsedSeconds = 0 // 已运行秒数
    private var isRunning = false
    private var timerJob: Job? = null // 协程任务（使用kotlinx-coroutines）

    // 开始定时器
    fun start() {
        if (isRunning) return
        isRunning = true
        elapsedSeconds = 0 // 重置秒数（如需继续计时可移除此行）

        // 启动协程定时器
        timerJob = CoroutineScope(Dispatchers.Main).launch {
            while (isRunning) {
                callback.onTick(elapsedSeconds) // 触发回调
                elapsedSeconds++
                delay(1000) // 等待1秒
            }
        }
    }

    // 暂停定时器
    fun pause() {
        isRunning = false
        timerJob?.cancel()
    }

    // 停止并重置定时器
    fun stop() {
        isRunning = false
        timerJob?.cancel()
        elapsedSeconds = 0
    }
}