package com.example.majiang

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

const val col_Number = 10
const val row_Number  =8

class ImageAdapter(
    private val recyclerView: RecyclerView,
) :    RecyclerView.Adapter<ImageAdapter.ViewHolder>() {

    private val tag = "Fandy_ImageAdapter"
    private val dataList: MutableList<Int> = mutableListOf()
    private var backDataList:List<Int> = listOf()
    // 拖拽相关变量
    private var isDragging = false // 是否正在拖拽
    private var draggedPosition = -1 // 被拖拽的item位置
    private var startX = 0f // 触摸起始X（相对于屏幕）
    private var startY = 0f // 触摸起始Y（相对于屏幕）
    private var direct = -1 //1 左2右 3上 4下
    private var maxMoveWidth = 0f  //最大可移动的距离
    private var haveMoved = 0f   //已经移动的距离
    private var moveTogether = 0 //一起移动的块数(包括自身)
    private var draggedRow = -1
    private var draggedCol = -1
    private var draggedView: View? = null // 被拖拽的ImageView
    private var changeColorView: View? = null // 被拖拽的ImageView


    private var soundPlayer: PlaySound? = null
    private var boxLeft = col_Number*row_Number

    var booleanArrays = Array(row_Number) { BooleanArray(col_Number) }

    interface OnAdapterListener {
        fun onGameSucceed()
        fun onGameFailed()
    }

    private var listener: OnAdapterListener? = null
    fun setOnAdapterListener(listener: OnAdapterListener) {
        this.listener = listener
    }

    fun resetGame() {
        generateRandomList() // 重新生成数据
        boxLeft = col_Number * row_Number
        notifyDataSetChanged() // 通知列表刷新
    }
    init {
        resetGame()
        setRecyclerViewTouchListener()
        soundPlayer = PlaySound(recyclerView.context)
    }
    // 新增：RecyclerView触摸监听逻辑
    @SuppressLint("ClickableViewAccessibility")
    private fun setRecyclerViewTouchListener() {
        recyclerView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // 1. 定位触摸点对应的Item视图和position
                    draggedView = recyclerView.findChildViewUnder(event.x, event.y)
                    if (draggedView != null) {
                        val draggedPosition = recyclerView.getChildAdapterPosition(draggedView!!)
                        // 2. 校验：仅处理dataList[position] != 0的Item（原逻辑）
                        if (draggedPosition != RecyclerView.NO_POSITION && dataList[draggedPosition] != 0) {
                            val rc = pushPosition(draggedPosition)
                            Log.d(tag, "ACTION_DOWN rc=$rc, draggedPosition=$draggedPosition")
                            if (!rc) {
                                Log.d(tag, "drag start")
                                // 3. 初始化拖拽参数（复用原逻辑）
                                startX = event.rawX
                                startY = event.rawY
                                draggedRow = draggedPosition / col_Number
                                draggedCol = draggedPosition % col_Number
                                direct = -1
                                isDragging = true
                            }
                        }
                    }
                }
                MotionEvent.ACTION_MOVE -> {
//                    Log.d(tag, "on move, isDragging=$isDragging")
                    if (isDragging && draggedView != null && toPosition(draggedRow,draggedCol) != RecyclerView.NO_POSITION) {
                        var newX = event.rawX - startX
                        var newY = event.rawY - startY
//                        Log.d(tag, "drag to dx:$newX, dy:$newY")
                        // 1. 初始化拖拽方向和最大移动距离（复用原逻辑）
                        if (direct == -1 && (abs(newX) > 5 || abs(newY) > 5)) {
                            maxMoveWidth = 0f
                            moveTogether = 1
                            var maxMove = 0
                            haveMoved = 0f
                            if (abs(newX) > abs(newY)) { // 水平移动
                                if (newX > 0) { // 向右
                                    direct = 2
                                    for (i in draggedCol + 1 until col_Number) {
                                        if (dataList[toPosition(draggedRow, i)] != 0) moveTogether++ else break
                                    }
                                    for (i in draggedCol + moveTogether until col_Number) {
                                        if (dataList[toPosition(draggedRow, i)] == 0) maxMove++ else break
                                    }
                                    maxMoveWidth = (recyclerView.width.toFloat() * maxMove / col_Number)
                                    draggedPosition = draggedCol
                                } else { // 向左
                                    direct = 1
                                    for (i in draggedCol - 1 downTo 0) {
                                        if (dataList[toPosition(draggedRow, i)] != 0) moveTogether++ else break
                                    }
                                    for (i in draggedCol - moveTogether downTo 0) {
                                        if (dataList[toPosition(draggedRow, i)] == 0) maxMove++ else break
                                    }
                                    maxMoveWidth = -(recyclerView.width.toFloat() * maxMove / col_Number)
                                    draggedPosition = draggedCol
                                }
                            } else { // 垂直移动
                                if (newY > 0) { // 向下
                                    direct = 4
                                    for (i in draggedRow + 1 until row_Number) {
                                        if (dataList[toPosition(i, draggedCol)] != 0) moveTogether++ else break
                                    }
                                    for (i in draggedRow + moveTogether until row_Number) {
                                        if (dataList[toPosition(i, draggedCol)] == 0) maxMove++ else break
                                    }
                                    maxMoveWidth = (recyclerView.height.toFloat() * maxMove / row_Number)
                                    draggedPosition = draggedRow
                                } else { // 向上
                                    direct = 3
                                    for (i in draggedRow - 1 downTo 0) {
                                        if (dataList[toPosition(i, draggedCol)] != 0) moveTogether++ else break
                                    }
                                    for (i in draggedRow - moveTogether downTo 0) {
                                        if (dataList[toPosition(i, draggedCol)] == 0) maxMove++ else break
                                    }
                                    maxMoveWidth = -(recyclerView.height.toFloat() * maxMove / row_Number)
                                    draggedPosition = draggedRow
                                }
                            }
                            Log.d(tag, "direct=$direct, maxMoveWidth=$maxMoveWidth, moveTogether=$moveTogether")
                        }

                        // 2. 执行Item移动（复用原逻辑，基于draggedPosition计算）
                        when (direct) {
                            1 -> { // 左移
                                Log.d(tag,"maxMoveWidth=$maxMoveWidth,newX=$newX,haveMoved=$haveMoved")
                                if(newX + haveMoved < maxMoveWidth){
                                    newX = maxMoveWidth - haveMoved
                                }else if(newX+haveMoved > 0){
                                    newX = -haveMoved
                                }
                                for (i in draggedCol downTo draggedCol - moveTogether + 1) {
                                    moveItemByRelativeDistance(
                                        toPosition(draggedRow, i),
                                        newX,
                                        0f
                                    )
                                }
                                haveMoved = haveMoved + newX
                                startX = event.rawX
                            }
                            2 -> { // 右移
                                if(newX + haveMoved > maxMoveWidth){
                                    newX = maxMoveWidth - haveMoved
                                }else if(newX+haveMoved < 0){
                                    newX = -haveMoved
                                }
                                for (i in draggedCol until draggedCol + moveTogether) {
                                    moveItemByRelativeDistance(
                                        toPosition(draggedRow, i),
                                        newX,
                                        0f
                                    )
                                }
                                haveMoved += newX
                                startX = event.rawX

                            }
                            3 -> { // 上移（修复原循环变量错误：用draggedRow而非draggedCol）
                                if(newY + haveMoved < maxMoveWidth){
                                    newY = maxMoveWidth - haveMoved
                                }else if(newY+haveMoved > 0){
                                    newY = -haveMoved
                                }
                                for (i in draggedRow downTo draggedRow - moveTogether + 1) {
                                    moveItemByRelativeDistance(
                                        toPosition(i, draggedCol),
                                        0f,
                                        newY
                                    )
                                }
                                haveMoved += newY
                                startY = event.rawY
                            }
                            4 -> { // 下移（修复原循环变量错误：用draggedRow而非draggedCol）
                                if(newY + haveMoved > maxMoveWidth){
                                    newY = maxMoveWidth - haveMoved
                                }else if(newY+haveMoved < 0){
                                    newY = -haveMoved
                                }
                                for (i in draggedRow until draggedRow + moveTogether) {
                                    moveItemByRelativeDistance(
                                        toPosition(i, draggedCol),
                                        0f,
                                        newY
                                    )
                                }
                                haveMoved += newY
                                startY = event.rawY
                            }
                        }
                        if(direct == 1 || direct == 2) {
                            val heartX = draggedView!!.x + draggedView!!.width / 2
                            val newCol = (heartX * col_Number / recyclerView.width).toInt()
                            if(newCol !=draggedPosition){
                                draggedPosition = newCol
                                soundPlayer?.playSound(R.raw.move)
                                var theRow = findV(draggedRow,newCol,draggedCol)
                                if(theRow != -1){
                                    draggedView!!.alpha = 0.5f
                                    changeColorView = recyclerView.layoutManager?.findViewByPosition(toPosition(theRow,newCol))
                                    changeColorView?.alpha = 0.5f
                                }else{
                                    draggedView!!.alpha = 1.0f
                                    changeColorView?.alpha = 1.0f
                                    changeColorView = null
                                }
                            }
                        }else{
                            val heartY = draggedView!!.y + draggedView!!.height / 2
                            val newRow = (heartY * row_Number / recyclerView.height).toInt()
                            if(newRow !=draggedPosition){
                                draggedPosition = newRow
                                soundPlayer?.playSound(R.raw.move)
                                var theCol = findH(newRow,draggedCol,draggedRow)
                                if(theCol != -1){
                                    draggedView!!.alpha = 0.5f
                                    changeColorView = recyclerView.layoutManager?.findViewByPosition(toPosition(newRow,theCol))
                                    changeColorView?.alpha = 0.5f
                                }else{
                                    draggedView!!.alpha = 1.0f
                                    changeColorView?.alpha = 1.0f
                                    changeColorView = null
                                }
                            }
                        }
                    }
                }

                MotionEvent.ACTION_UP -> {
                    Log.d(tag, "release, isDragging=$isDragging")
                    if (isDragging && draggedView != null && toPosition(draggedRow,draggedCol) != RecyclerView.NO_POSITION) {
                        isDragging = false
                        Log.d(tag, "release isDragging=$isDragging")
                        // 1. 计算拖拽后的目标位置（复用原逻辑）
                        val heartX = draggedView!!.x + draggedView!!.width / 2
                        val heartY = draggedView!!.y + draggedView!!.height / 2
                        val newRow = (heartY * row_Number / recyclerView.height).toInt()
                        val newCol = (heartX * col_Number / recyclerView.width).toInt()
                        Log.d(tag, "dragposition($draggedRow,$draggedCol),newposition($newRow,$newCol)")

                        // 2. 水平/垂直方向处理（复用原逻辑，修复部分错误）
                        if (direct == 1 || direct == 2) {
                            Log.d(tag,"go findV ${dataList[toPosition(draggedRow,draggedCol)]}")
                            val findRow = findV(newRow, newCol, draggedCol)
                            Log.d(tag, "findRow=$findRow")
                            if (findRow == -1) {
                                // 回位逻辑（修复原循环范围错误）
                                val startCol = if (direct == 1) draggedCol - moveTogether + 1 else draggedCol
                                val endCol = if (direct == 1) draggedCol + 1 else draggedCol + moveTogether
                                for (i in startCol until endCol) {
                                    Log.d(tag, "回位（$draggedRow,$i）")
                                    notifyItemChanged(toPosition(draggedRow, i))
                                }
                            } else {
                                backDataList = dataList.toMutableList()
                                val moveStep = newCol - draggedCol
                                // 移动数据（复用原逻辑）
                                if (direct == 1) {
                                    for (i in 0 until moveTogether - 1) {
                                        movePosition(
                                            draggedRow, draggedCol - moveTogether + 1 + i,
                                            draggedRow, draggedCol - moveTogether + 1 + moveStep + i
                                        )
                                    }
                                } else {
                                    for (i in 0 until moveTogether - 1) {
                                        movePosition(
                                            draggedRow, draggedCol + moveTogether - 1 - i,
                                            draggedRow, draggedCol + moveTogether - 1 + moveStep - i
                                        )
                                    }
                                }
                                clearPosition(toPosition(findRow, newCol), toPosition(draggedRow, draggedCol))
                            }
                        } else if (direct == 3 || direct == 4) {
                            Log.d(tag,"go findH ${dataList[toPosition(draggedRow,draggedCol)]}")
                            val findCol = findH(newRow, newCol, draggedRow) // 原逻辑：垂直方向用findH，暂保留
                            Log.d(tag, "findCol=$findCol")
                            if (findCol == -1) {
                                // 回位逻辑（补充：垂直方向回位）
                                val startRow = if (direct == 3) draggedRow - moveTogether + 1 else draggedRow
                                val endRow = if (direct == 3) draggedRow + 1 else draggedRow + moveTogether
                                for (i in startRow until endRow) {
                                    notifyItemChanged(toPosition(i, draggedCol))
                                }
                            } else {
                                backDataList = dataList.toMutableList()
                                val moveStep = newRow - draggedRow
                                // 移动数据（复用原逻辑）
                                if (direct == 3) {
                                    for (i in 0 until moveTogether - 1) {
                                        movePosition(
                                            draggedRow - moveTogether + 1 + i, draggedCol,
                                            draggedRow - moveTogether + 1 + moveStep + i, draggedCol
                                        )
                                    }
                                } else {
                                    for (i in 0 until moveTogether - 1) {
                                        movePosition(
                                            draggedRow + moveTogether - 1 - i, draggedCol,
                                            draggedRow + moveTogether - 1 + moveStep - i, draggedCol
                                        )
                                    }
                                }
                                clearPosition(toPosition(newRow, findCol), toPosition(draggedRow, draggedCol))
                            }
                        }
                    }
                }
            }
            true // 消费所有触摸事件，防止RecyclerView默认滚动干扰
        }
    }

    private fun generateRandomList() {
/*        dataList.clear()
        dataList.addAll(mutableListOf(  0,20,13,18,6,0,0,12,7,6,
        8,5,2,12,10,0,8,19,10,15,
        0,15,9,13,16,19,9,4,17,0,
        0,0,1,8,10,4,6,11,10,0,
        0,0,12,9,13,19,16,2,1,0,
        20,0,19,6,3,1,3,20,0,0,
        8,4,18,9,7,5,11,7,2,4,
        7,2,20,15,12,17,13,1,15,0,
        ))
        val row1 = 4
        val col1 = 7
        val row2 = 6
        val col2 = 8
        if (checkpare_h(row1, col1, row2, col2) || checkpare_v(row1, col1, row2, col2)) {
            Log.d(tag, "找到($row1,$col1,-$row2,$col2)")
            if (checkpare_h(row1, col1, row2, col2)) {
                Log.d(tag, "找到checkpare_h")
            }
            if (checkpare_v(row1, col1, row2, col2)) {
                Log.d(tag, "checkpare_v")
            }
        }
*/
        dataList.clear() // 清空旧数据
        dataList.addAll((1..20).flatMap { num ->
            List(4) { _ -> num }
        }.shuffled().toMutableList())
        while(!findpare()) {
            dataList.clear() // 清空旧数据
            dataList.addAll((1..20).flatMap { num ->
                List(4) { _ -> num }
            }.shuffled().toMutableList())
        }
    }
    // ViewHolder：缓存item视图
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
        val value = dataList[position]
        if(value != 0) {
            val resourceId = holder.itemView.context.resources.getIdentifier(
                "img_$value",
                "drawable",
                holder.itemView.context.packageName
            )
            holder.imageView.setImageResource(resourceId)
        }else{
            holder.imageView.setImageDrawable(null)
            holder.imageView.setBackgroundColor(Color.TRANSPARENT)
        }
    }
    private fun findV(row:Int,col:Int,draggedCol:Int):Int{   //在垂直方向找
        var find = -1
        for (i in row - 1 downTo  0) {
            if (dataList[toPosition(i, col)] != 0) {
                find = i
                break
            }
        }
        if((find != -1)&&dataList[toPosition(row,draggedCol)] == dataList[toPosition(find,col)])
            return find
        find = -1
        for (i in row + 1 until row_Number) {
            if (dataList[toPosition(i, col)] != 0) {
                find = i
                break
            }
        }
        if((find != -1)&&dataList[toPosition(row,draggedCol)] == dataList[toPosition(find,col)])
            return find
        return -1
    }
    private fun findH(row:Int,col:Int,draggedRow:Int):Int{
        var find = -1
        for (i in col - 1 downTo  0) {
            if (dataList[toPosition(row, i)] != 0) {
                Log.d(tag,"左边不为零的位置是 $i")
                find = i
                break
            }
        }
        if((find != -1)&&dataList[toPosition(draggedRow,col)] == dataList[toPosition(row,find)])
            return find
        find = -1
        for (i in col + 1 until col_Number) {
            if (dataList[toPosition(row, i)] != 0) {
                Log.d(tag,"右边不为零的位置是 $i")
                find = i
                break
            }
        }
        if((find != -1)&&dataList[toPosition(draggedRow,col)] == dataList[toPosition(row,find)])
            return find
        return -1
    }
    //把指定的块移动一段距离
    fun moveItemByRelativeDistance(position: Int, dx: Float, dy: Float): Boolean {
        // 1. 校验position合法性
        if (position < 0 || position >= dataList.size) {
            return false
        }

        // 2. 获取该position对应的视图
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(position) ?: return false
        val itemView = viewHolder.itemView // 要移动的视图（item根布局或ImageView）

        // 5. 应用新位置
        itemView.x += dx
        itemView.y += dy
        return true
    }
    //从当前列到左底，看看是否与原块相同找到返回列号，没有返回-1.原块可能不在当前行（也许移动过）
    private fun  getLeft(row: Int,col: Int,sourceRow:Int):Int{
        var rc = -1
        for (i in col - 1 downTo 0){
            if(dataList[toPosition(row,i)] != 0){
                rc = i
                break
            }
        }
        if((rc != -1)&&(dataList[toPosition(sourceRow,col)] == dataList[toPosition(row,rc)])){
            return rc
        }
        return -1
    }
    //从当前列到右底，看看是否与原块相同找到返回列号，没有返回-1.原块可能不在当前行（也许移动过）
    private fun  getRight(row: Int,col: Int,sourceRow:Int):Int{
        var rc = -1
        for (i in col+1 until  col_Number){
            if(dataList[toPosition(row,i)] != 0){
                rc = i
                break
            }
        }
        if((rc != -1)&&(dataList[toPosition(sourceRow,col)] == dataList[toPosition(row,rc)])){
            return rc
        }
        return -1
    }
    //从当前行到顶，看看是否与原块相同找到返回行号，没有返回-1.原块可能不在当前列（也许移动过）
    private fun  getTop(row: Int,col: Int,sourceCol :Int):Int{
        var rc = -1
        for (i in row - 1 downTo 0){
            if(dataList[toPosition(i,col)] != 0){
                rc = i
                break
            }
        }
        if((rc != -1)&&(dataList[toPosition(row,sourceCol)] == dataList[toPosition(rc,col)])){
            return rc
        }
        return -1
    }
    //从当前行到底，看看是否与原块相同找到返回行号，没有返回-1.原块可能不在当前列（也许移动过）
    private fun  getBottom(row: Int,col: Int,sourceCol:Int):Int{
        var rc = -1
        for (i in row + 1 until  row_Number){
            if(dataList[toPosition(i,col)] != 0){
                rc = i
                break
            }
        }
        if((rc != -1)&&(dataList[toPosition(row,sourceCol)] == dataList[toPosition(rc,col)])){
            return rc
        }
        return -1
    }
    //点击了某个位置
    private fun  pushPosition(position:Int):Boolean{

        val row1 =  position / col_Number
        val col1 = position % col_Number
        Log.d(tag, "点击了第 $row1 行，第 $col1 列")
        //看左
        var col2 = getLeft(row1,col1,row1)
        if(col2 == -1)
            col2 = getRight(row1,col1,row1)
        if(col2 != -1){
            backDataList = dataList.toMutableList()
            clearPosition(toPosition(row1,col1),toPosition(row1,col2))
            return true
        }
        var row2 = getTop(row1,col1,col1)
        if(row2 == -1)
            row2 = getBottom(row1,col1,col1)
        if(row2 != -1){
            backDataList = dataList.toMutableList()
            clearPosition(toPosition(row1,col1),toPosition(row2,col1))
            return true
        }
        return false
    }
    //把源块移动到另一个位置
    private fun movePosition(sourceRow:Int,sourceCol :Int,targetRow:Int,targetCol:Int){
        val source = toPosition(sourceRow,sourceCol)
        val target = toPosition(targetRow,targetCol)
        Log.d(tag,"move ($sourceRow,$sourceCol) to ($targetRow,$targetCol)")
        dataList[target] = dataList[source]
        dataList[source] = 0
        notifyItemChanged(source)
        notifyItemChanged(target)
        logDataList()
    }
    //退回到上一次的移动，由用户触发
    fun goBack() :Boolean{
        if(!backDataList.isEmpty()) {
            dataList.clear()
            dataList.addAll(backDataList)
            backDataList = emptyList()
            notifyDataSetChanged()
            boxLeft += 2
            logDataList()
            return true
        }
        return false
    }
    fun shuffle(){
        dataList.shuffle()
        notifyDataSetChanged()
    }
    //消除两块，并根据结果进行处理。赢了，输了
    private fun clearPosition(p1:Int,p2:Int){
        dataList[p1] = 0
        dataList[p2] = 0
        notifyItemChanged(p1)
        notifyItemChanged(p2)
        boxLeft -= 2
        if(boxLeft == 0){
            soundPlayer?.playSound(R.raw.clap)
            listener?.onGameSucceed()
        }else if(!findpare()) {
            soundPlayer?.playSound(R.raw.ao)
            listener?.onGameFailed()
        }else{
          //  soundPlayer?.playSound(R.raw.clap)
            soundPlayer?.playSound(R.raw.clear)
            //listener?.onGameSucceed()
        }
    }
    // 数据总量（80个元素）
    override fun getItemCount(): Int = dataList.size
    private fun toPosition(row:Int,col:Int):Int{
        return row*col_Number+col
    }
    private fun logDataList() {
        Log.d(tag,"datalist length${dataList.size}")
        for(i in 0 until row_Number){
            Log.d(tag,"${dataList[toPosition(i,0)]},${dataList[toPosition(i,1)]},${dataList[toPosition(i,2)]},${dataList[toPosition(i,3)]},${dataList[toPosition(i,4)]}," +
                    "${dataList[toPosition(i,5)]},${dataList[toPosition(i,6)]},${dataList[toPosition(i,7)]},${dataList[toPosition(i,8)]},${dataList[toPosition(i,9)]},")
        }
    }
    private fun findpare():Boolean{
        var rc = false
        Log.d(tag, "找pare")
        logDataList()
        for (row1 in 0 until row_Number) {    //i是行
            for (col1 in 0 until col_Number) {  //j是列
                for (row2 in 0 until row_Number) {
                    for (col2 in 0 until col_Number) {
                        if((row1 != row2)||(col1 != col2)) {
                            if ((dataList[toPosition(row1,col1)] == dataList[toPosition(row2,col2)])&&(dataList[toPosition(row2,col2)] != 0)) {
                                if (checkpare_h(row1, col1, row2, col2) || checkpare_v(row1, col1, row2, col2)) {
                                    Log.d(tag, "找到($row1,$col1,-$row2,$col2)")
                                    return true
                                }
                            }
                        }
                    }
                }
            }
        }
        Log.d(tag,"nothing")
        return rc
    }
    fun findpareDsp():Array<BooleanArray>{
        booleanArrays = Array(row_Number) { BooleanArray(col_Number) }
        for (row1 in 0 until row_Number) {    //i是行
            for (col1 in 0 until col_Number) {  //j是列
                for (row2 in 0 until row_Number) {
                    for (col2 in 0 until col_Number) {
                        if((row1 != row2)||(col1 != col2)) {
                            if ((dataList[toPosition(row1,col1)] == dataList[toPosition(row2,col2)])&&(dataList[toPosition(row2,col2)] != 0)) {
                                if (checkpare_h(row1, col1, row2, col2) || checkpare_v(row1, col1, row2, col2)) {
                                    booleanArrays[row1][col1] = true
                                    booleanArrays[row2][col2] = true
                                    Log.d(tag, "找到($row1,$col1,-$row2,$col2)")
                                }
                            }
                        }
                    }
                }
            }
        }
        return booleanArrays
    }
    //横移是否可以
    private fun checkpare_h(row1:Int,col1:Int,row2:Int,col2:Int):Boolean{
//        Log.d(tag,"横移可以吗？($row1,$col1-$row2,$col2) ${dataList[toPosition(row1,col1)]}")
        if(row1 == row2) return false
        val start = minOf(row1, row2)
        val end = maxOf(row1, row2)
        for (i in (start + 1) until end) {
            if(dataList[toPosition(i,col2)] != 0) {
//                Log.d(tag,"阻挡在（$i,$col2）")
                return false
            }
        }
//        Log.d(tag,"没阻挡")
        if(col1 > col2) {//需要左移
//            Log.d(tag,"需要左移")
            var blankStart = -1
            for (i in col1 - 1 downTo 0){
                if (dataList[toPosition(row1,i)] == 0) {
//                    Log.d(tag,"找到空（$row1,$i）")
                    blankStart = i
                    break
                }
            }
            if(blankStart == -1) {
//                Log.d(tag,"没空")
                return false
            }
            var blankNumber = 1
            if(col1-col2 == 1) return true
            for(i in (blankStart -1) downTo 0){
                if (dataList[toPosition(row1,i)] == 0) {
                    blankNumber += 1
                    if(col1 - col2 == blankNumber) {
                        Log.d(tag,"($row1,$col1-$row2,$col2)")
//                        Log.d(tag,"找到$blankNumber 个空")
                        return true
                    }
                }else return false
            }
            return false
        }else if(col1 < col2){
//            Log.d(tag,"需要右移")
            var blankStart = -1
            for (i in col1 + 1 until 10){
                if (dataList[toPosition(row1,i)] == 0) {
//                    Log.d(tag,"找到空（$row1,$i）")
                    blankStart = i
                    break
                }
            }
            if(blankStart == -1) {
//                Log.d(tag,"没空")
                return false
            }
            var blankNumber = 1
            if(col2 - col1 == 1) return true
            for(i in (blankStart + 1) until  10){
                if (dataList[toPosition(row1,i)] == 0) {
                    blankNumber += 1
                    if(col2 - col1 == blankNumber) {
                        Log.d(tag,"($row1,$col1-$row2,$col2)")
                        return true
                    }
                }else return false
            }
//            Log.d(tag,"同列")
            return false
        }
        return true
    }
    //竖移是否可以
    private fun checkpare_v(row1: Int,col1: Int,row2: Int,col2: Int):Boolean{
//        Log.d(tag,"竖移可以吗？($row1,$col1-$row2,$col2) ${dataList[toPosition(row1,col1)]}")
        if(col1 == col2) return false
        val start = minOf(col1, col2)
        val end = maxOf(col1, col2)
        for (i in (start + 1) until end) {
            if(dataList[toPosition(row2,i)] != 0) {
//                Log.d(tag,"阻挡在（$row2,$i）")
                return false
            }
        }
//        Log.d(tag,"没阻挡")
        if(row1 > row2) {//需要上移
//            Log.d(tag,"需要上移")
            var blankStart = -1
            for (i in row1 - 1 downTo 0){
                if (dataList[toPosition(i,col1)] == 0) {
//                    Log.d(tag,"找到空（$i,$col1）")
                    blankStart = i
                    break
                }
            }
            if(blankStart == -1) {
//                Log.d(tag,"没空")
                return false
            }
            var blankNumber = 1
            if(row1-row2 == 1) return true
            for(i in (blankStart -1) downTo 0){
                if (dataList[toPosition(i,col1)] == 0) {
                    blankNumber += 1
                    if(row1 - row2 == blankNumber) {
//                        Log.d(tag,"找到$blankNumber 个空")
                        Log.d(tag,"($row1,$col1-$row2,$col2)")
                        return true
                    }
                }else return false
            }
            return false
        }else if(row1 < row2){
//            Log.d(tag,"需要下移")
            var blankStart = -1
            for (i in row1 + 1 until 8){
                if (dataList[toPosition(i,col1)] == 0) {
//                    Log.d(tag,"找到空（$i,$col1）")
                    blankStart = i
                    break
                }
            }
            if(blankStart == -1) {
//                Log.d(tag,"没空")
                return false
            }
            var blankNumber = 1
            if(row2 - row1 == 1) return true
            for(i in (blankStart + 1) until  8){
                if (dataList[toPosition(i,col1)] == 0) {
                    blankNumber += 1
                    if(row2 - row1 == blankNumber) {
//                        Log.d(tag,"找到$blankNumber 个空")
                        Log.d(tag,"($row1,$col1-$row2,$col2)")
                        return true
                    }
                }else return false
            }
            return false
        }
//        Log.d(tag,"同行")
        return true
    }
}