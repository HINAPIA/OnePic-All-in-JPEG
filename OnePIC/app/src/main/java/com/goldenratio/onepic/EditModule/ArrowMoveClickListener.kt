package com.goldenratio.onepic.EditModule

import android.annotation.SuppressLint
import android.graphics.PointF
import android.os.Handler
import android.view.MotionEvent
import android.view.View

class ArrowMoveClickListener(private val myFunction: (x: Int, y: Int) -> Unit, maxView: View, view: View) : View.OnTouchListener {
    private var handler = Handler()
    private var runnable: Runnable? = null

    // 초기 위치 설정
    private var prevX : Float? = null
    private var prevY : Float? = null

    private val minX: Float
    private val minY: Float

    private val maxX: Float
    private val maxY: Float

    private val basicPointF: PointF
    private val checkPointF: PointF

    init {
//        minX = -(maxView.width/2).toFloat()
//        minY = -(maxView.height/2).toFloat()

//        maxX = (maxView.width/2).toFloat()
//        maxY = (maxView.height/2).toFloat()

        minX = -(maxView.width/2 - (view.width/2)).toFloat()
        minY = -(maxView.height/2 - (view.height/2)).toFloat()

        maxX = (maxView.width/2 - (view.width/2)).toFloat()
        maxY = (maxView.height/2 - (view.height/2)).toFloat()

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

                        handler.postDelayed(this, 100) // 100ms 간격으로 실행됩니다.
                    }
                }
                handler.postDelayed(runnable!!, 100)
                return true
            }
            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(runnable!!)

                v.x = basicPointF.x // 버튼의 위치 변경
                v.y = basicPointF.y

                prevX = null
                prevY = null

                return true
            }
            MotionEvent.ACTION_MOVE -> {
                println("point(${v.translationX}, ${v.translationY}) - x($minX, $maxX) y($minY, $maxY)  ")

                if(prevX != null && prevY != null) {

                    var dx = event.rawX - prevX!!
                    var dy = event.rawY - prevY!!

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

                    println("D point (${dx}, ${dy})")
//                myFunction(moveX.toInt(), moveY.toInt())

                    if (dx > 0)
                        dx = 4f
                    else if (dx < 0)
                        dx = -4f

                    if (dy > 0)
                        dy = 4f
                    else if (dy < 0)
                        dy = -4f

                    myFunction(dx.toInt(), dy.toInt())

                    return true
                }
                return false
            }
            else -> return false
        }
    }
}