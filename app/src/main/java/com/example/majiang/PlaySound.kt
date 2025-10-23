package com.example.majiang
import android.content.Context
import android.media.MediaPlayer

class PlaySound(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    /**
     * 直接播放指定的资源ID（如playSound(R.raw.ao)）
     * @param resourceId 音频资源ID
     */
    fun playSound(resourceId: Int) {
        // 停止当前播放
        stopSound()

        // 播放指定资源
        playSoundResource(resourceId)
    }

    /**
     * 停止播放并释放资源
     */
    fun stopSound() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
            mediaPlayer = null
        }
    }

    /**
     * 实际执行播放的私有方法
     */
    private fun playSoundResource(resourceId: Int) {
        mediaPlayer = MediaPlayer.create(context, resourceId).apply {
            setOnCompletionListener {
                stopSound()
            }
            start()
        }
    }
}
