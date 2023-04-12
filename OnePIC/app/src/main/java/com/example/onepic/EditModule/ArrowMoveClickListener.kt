package com.example.onepic.EditModule

import android.annotation.SuppressLint
import android.graphics.Point
import android.graphics.PointF
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlin.math.max

class ArrowMoveClickListener(private val myFunction: (x: Int, y: Int) -> Unit, maxView: View, view: View) : View.OnTouchListener {
    private var handler = Handler()
    private var runnable: Runnable? = null

    // 초기 위치 설정
    private var prevX = 0f
    private var prevY = 0f

    private val minX: Float
    private val minY: Float

    private val maxX: Float
    private val maxY: Float

    private val basicPointF: PointF
    private val checkPointF: PointF

    init {
        minX = -(maxView.width/2).toFloat()
        minY = -(maxView.height/2).toFloat()

        maxX = (maxView.width/2).toFloat()
        maxY = (maxView.height/2).toFloat()


        checkPointF = PointF(view.x+(view.width/2), view.y+(view.height/2))
        basicPointF = PointF(view.x, view.y)
        println("=========== point(${view.x}, ${view.y}) - point(${maxView.x},${maxView.y})  ")

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                runnable = object : Runnable {
                    override fun run() {

                        prevX = event.rawX
                        prevY = event.rawY

                        handler.postDelayed(this, 50) // 100ms 간격으로 실행됩니다.
                    }
                }
                handler.postDelayed(runnable!!, 50)
                return true
            }
            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(runnable!!)

                v.x = basicPointF.x // 버튼의 위치 변경
                v.y = basicPointF.y
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                println("point(${v.translationX}, ${v.translationY}) - x($minX, $maxX) y($minY, $maxY)  ")

                val dx = event.rawX - prevX
                val dy = event.rawY - prevY

                var moveX = v.translationX + dx // 버튼의 위치 변경
                var moveY = v.translationY + dy

                if (moveX > maxX)
                    moveX = maxX
                else if (moveX < minX)
                    moveX = minX

                if (moveY > maxY)
                    moveY = maxY
                else if (moveY < minY)
                    moveY = minY

                v.translationX = moveX
                v.translationY = moveY

                myFunction(dx.toInt(), dy.toInt())
                return true
            }
            else -> return false
        }
    }
}