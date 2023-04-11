package com.example.onepic.EditModule

import android.annotation.SuppressLint
import android.os.Handler
import android.view.MotionEvent
import android.view.View

class RudderKeyClickListener(private val myFunction: () -> Unit) : View.OnTouchListener {
    private var handler = Handler()
    private var runnable: Runnable? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if(event?.action == MotionEvent.ACTION_DOWN){
            runnable = object : Runnable {
                override fun run() {
                    // 롱 프레스 이벤트 발생 시 실행할 함수 작성
                    myFunction()
                    handler.postDelayed(this, 50) // 100ms 간격으로 실행됩니다.
                }
            }
            handler.postDelayed(runnable!!, 50)
        }
        else if (event?.action == MotionEvent.ACTION_UP || event?.action == MotionEvent.ACTION_CANCEL) {
            handler.removeCallbacks(runnable!!)
        }
        return false
    }
}
