package com.example.majiang

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.View
import com.example.fireworks.FireworksView


class MainActivity : AppCompatActivity() {
    val tag = "Fandy_MainActivity"

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ImageAdapter
    private lateinit var hintView: RecyclerView
    private lateinit var hintAdapter: PromptAdapter
    private lateinit var btn_new_game: Button
    private lateinit var hintButton: Button
    private lateinit var backButton: Button
    private lateinit var shuffleButton: Button
    private lateinit var fireworksView: FireworksView
//    private lateinit var gameOver:TextView
    private lateinit var victory:TextView

    private val handler = Handler(Looper.getMainLooper())
    private var hintLeft = 5
    private var backLeft = 3
    private var shuffleLeft = 3

    private lateinit var timerText: TextView
    private lateinit var timer :SecondTimer

    private val PREF_NAME = "BestTimePref"
    private val KEY_BEST_TIME = "best_victory_time"
    private var bastTime = Int.MAX_VALUE
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        //gameOver = findViewById(R.id.game_over_tv)
        recyclerView = findViewById(R.id.recyclerView)
        fireworksView = findViewById(R.id.fireworksView)
        // 设置网格布局管理器：10列（8行会自动根据80个元素生成）
        recyclerView.layoutManager = GridLayoutManager(this, col_Number, // 列数=10
            GridLayoutManager.VERTICAL, // 垂直方向排列
            false) // 不反转布局)
            .apply {
            // 可选：设置行列间距（如需统一边距）
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int = 1 // 每个item占1列
            }
        }
        // 设置适配器
        adapter = ImageAdapter(recyclerView)
        adapter.setOnAdapterListener(object : ImageAdapter.OnAdapterListener {
            override fun onGameSucceed() {
                val seconds = timer.stop()
                if(seconds < bastTime){
                    saveBestTime(seconds)
                    bastTime = seconds
                    victory.text = "       你赢啦！！！\n共用时：" + formatSecond(seconds) + "\n你创造了历史记录"
                }else{
                    victory.text = "       你赢啦！！！\n共用时：" + formatSecond(seconds)
                }
                victory.visibility = View.VISIBLE
                startFireworks()
            }
            override fun onGameFailed() {
                val seconds = timer.stop()
                victory.visibility = View.VISIBLE
                victory.text = "       你输啦！！！\n共用时：" + formatSecond(seconds)
                hintButton.isEnabled = false
                backButton.isEnabled = false
                shuffleButton.isEnabled = false
            }
        })
        recyclerView.adapter = adapter
        //------------------------------------------------------
        hintView = findViewById(R.id.promptView)

        // 设置网格布局管理器：10列（8行会自动根据80个元素生成）
        hintView.layoutManager = GridLayoutManager(this, col_Number, // 列数=10
            GridLayoutManager.VERTICAL, // 垂直方向排列
            false) // 不反转布局)
            .apply {
                // 可选：设置行列间距（如需统一边距）
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int = 1 // 每个item占1列
                }
            }
        // 设置适配器
        hintAdapter = PromptAdapter(hintView)
        hintView.adapter = hintAdapter

        btn_new_game = findViewById(R.id.btn_new_game)
        btn_new_game.setOnClickListener {
            resetGame()
        }
        findViewById<Button>(R.id.btn_exit).setOnClickListener {
            finish()
        }
        hintButton = findViewById<Button>(R.id.btn_hint)
        hintButton.setOnClickListener {
            hintLeft -= 1
            hintButton.setText("提示($hintLeft)")
            if(hintLeft == 0)
                hintButton.isEnabled = false
            val booleanArray = adapter.findpareDsp()
            hintAdapter.updateData(booleanArray)
        }

        backButton = findViewById<Button>(R.id.btn_back)
        backButton.setOnClickListener {
            if (adapter.goBack()) {
                backLeft -= 1
                backButton.setText("悔一步($backLeft)")
                if (backLeft == 0)
                    backButton.isEnabled = false
            }
        }
        shuffleButton = findViewById<Button>(R.id.btn_shuffle)
        shuffleButton.setOnClickListener {
            shuffleLeft -= 1
            shuffleButton.setText("打乱($shuffleLeft)")
            if(shuffleLeft == 0)
                shuffleButton.isEnabled = false
            adapter.shuffle()
        }
        victory = findViewById<TextView>(R.id.victoryTime)
        victory.visibility = View.GONE
        timerText = findViewById<TextView>(R.id.timer)
        timer = SecondTimer(object : TimerCallback {
            override fun onTick(seconds: Int) {
                timerText.text = formatSecond(seconds)
            }
        })
        prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        bastTime = getBestTime()
        timer.start()
    }
    private fun formatSecond(seconds: Int):String{
        val hour = seconds / 3600
        val minute = (seconds / 60) % 60
        val second = seconds % 60
        return String.format("%02d:%02d:%02d", hour, minute, second)
    }
    private fun resetGame(){
        hintLeft = 5
        backLeft = 3
        shuffleLeft = 3
        hintButton.isEnabled = true
        backButton.isEnabled = true
        shuffleButton.isEnabled = true
        hintButton.setText("提示($hintLeft)")
        backButton.setText("悔一步($backLeft)")
        shuffleButton.setText("打乱($shuffleLeft)")
        timer.start()
        //gameOver.visibility = View.GONE
        victory.visibility = View.GONE
        adapter.resetGame()
    }
    private fun toPosition(row:Int,col:Int):Int{
        return row*col_Number+col
    }
    private fun logDataList(dataList:Array<BooleanArray>) {
        val rowCount = dataList.size
        val colCount = if (rowCount > 0 && dataList[0].isNotEmpty()) {
            dataList[0].size  // 假设所有行的列数一致（游戏网格通常是规整的）
        } else {
            0  // 空数组时返回0，避免崩溃
        }
        Log.d(tag,"BooleanArray ($rowCount,$colCount)")
        for(i in 0 until row_Number){
            Log.d(tag,"${dataList[i][0]},${dataList[i][1]},${dataList[i][2]},${dataList[i][3]},${dataList[i][4]}," +
                    "${dataList[i][5]},${dataList[i][6]},${dataList[i][7]},${dataList[i][8]},${dataList[i][9]},")
        }
    }
    private fun startFireworks() {
        // 显示烟花视图并开始动画
        fireworksView.visibility = android.view.View.VISIBLE
        fireworksView.startFireworks()

        // 3秒后自动停止并隐藏
        handler.postDelayed({
            fireworksView.stopFireworks()
            fireworksView.visibility = android.view.View.GONE
            resetGame()
        }, 4000) // 3000毫秒 = 3秒
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清除延迟任务，避免内存泄漏
        handler.removeCallbacksAndMessages(null)
    }
    private fun saveBestTime(seconds: Int) {
        prefs.edit().putInt(KEY_BEST_TIME, seconds).apply()
    }

    // 读取最佳胜利时间（返回总秒数，-1表示未存储）
    private fun getBestTime(): Int {
        Log.d(tag,"KEY_BEST_TIME=${prefs.getInt(KEY_BEST_TIME, Int.MAX_VALUE)}")
        return prefs.getInt(KEY_BEST_TIME, Int.MAX_VALUE)
    }
}