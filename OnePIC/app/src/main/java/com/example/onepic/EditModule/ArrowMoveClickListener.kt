package com.example.onepic.EditModule

import android.annotation.SuppressLint
import android.graphics.Point
import android.graphics.PointF
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import android.view.View

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

    init {
        minX = maxView.x
        minY = maxView.y

        maxX = maxView.width + minX
        maxY = maxView.height + minY

        basicPointF = PointF(view.x, view.y)
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
                println("point(${event.rawX}, ${event.rawY}) - x($minX, $maxX) y($minY, $maxY)  ")
                //if (event.rawX > minX && event.rawX < maxX && event.rawY > minY && event.rawY < maxY) {
                    val dx = event.rawX - prevX
                    val dy = event.rawY - prevY

                    v.translationX += dx // 버튼의 위치 변경
                    v.translationY += dy

                    if (v.x + dx > maxX)
                        v.translationX = maxX
                    else if (v.x + dx < minX)
                        v.translationX = minX
                    if (v.y + dy > maxY)
                        v.translationY = maxY
                    else if (v.y + dy < minY)
                        v.translationY = minY

                    myFunction(dx.toInt(), dy.toInt())
                //}
                return true
            }
            else -> return false
        }
    }
}