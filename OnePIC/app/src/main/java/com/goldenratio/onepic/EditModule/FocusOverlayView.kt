package com.goldenratio.onepic.EditModule

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

class FocusOverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

//    private val rectList: MutableList<Rect> = mutableListOf()  // 그릴 Rect 객체들의 리스트
    private val boundingBoxList : ArrayList<Rect> = arrayListOf()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // ImageView의 크기에 맞게 Rect 좌표를 조정하여 그립니다.
        val imageWidth = width
        val imageHeight = height
        val paint = Paint()
        paint.color = Color.RED
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f

        for (rect in boundingBoxList) {
            val rectLeft = rect.left * imageWidth / 100
            val rectTop = rect.top * imageHeight / 100
            val rectRight = rect.right * imageWidth / 100
            val rectBottom = rect.bottom * imageHeight / 100
//            canvas.drawRect(rectLeft, rectTop, rectRight, rectBottom, paint)
        }
    }

    fun setRectList(rectList: List<Rect>) {
        this.boundingBoxList.clear()
        this.boundingBoxList.addAll(rectList)
        invalidate()
    }
}
