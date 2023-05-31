package com.goldenratio.onepic.EditModule

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.goldenratio.onepic.R

class FocusOverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

//    private val rectList: MutableList<Rect> = mutableListOf()  // 그릴 Rect 객체들의 리스트
    private val boundingBoxList : ArrayList<ArrayList<Int>> = ArrayList<ArrayList<Int>>()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // ImageView의 크기에 맞게 Rect 좌표를 조정하여 그립니다.
//        val imageWidth = width
//        val imageHeight = height
//        val paint = Paint()
//        paint.color = Color.RED
//        paint.style = Paint.Style.STROKE
//        paint.strokeWidth = 5f

//        for (rect in boundingBoxList) {
//            val rectLeft = rect.left * imageWidth / 100f
//            val rectTop = rect.top * imageHeight / 100f
//            val rectRight = rect.right * imageWidth / 100f
//            val rectBottom = rect.bottom * imageHeight / 100f
//            canvas.drawRect(rectLeft, rectTop, rectRight, rectBottom, paint)
//        }

        val pen = Paint()

        pen.strokeWidth = 25f
        pen.style = Paint.Style.STROKE
        var lineLength = 130f

        for(box in boundingBoxList){
//            val top = box.top.toFloat()
//            val bottom = box.bottom.toFloat()
//            val left = box.left.toFloat()
//            val right = box.right.toFloat()

            val left = box[0].toFloat()
            val top = box[1].toFloat()
            val right = box[2].toFloat()
            val bottom = box[3].toFloat()

            var path = Path()
            pen.color = ContextCompat.getColor(context!!, R.color.focus)
            path.moveTo(left - pen.strokeWidth/2f, top)
            path.lineTo(left + lineLength, top)
            canvas.drawPath(path, pen);

            path = Path()
            pen.color = ContextCompat.getColor(context!!, R.color.focus_30)
            path.moveTo(left + lineLength, top)
            path.lineTo(right - lineLength, top)
            canvas.drawPath(path, pen);

            path = Path()
            pen.color = ContextCompat.getColor(context!!, R.color.focus)
            path.moveTo(right - lineLength, top)
            path.lineTo(right, top)
            path.lineTo(right, top + lineLength)
            canvas.drawPath(path, pen);

            path = Path()
            pen.color = ContextCompat.getColor(context!!, R.color.focus_30)
            path.moveTo(right, top + lineLength)
            path.lineTo(right, bottom - lineLength)
            canvas.drawPath(path, pen);

            path = Path()
            pen.color = ContextCompat.getColor(context!!, R.color.focus)
            path.moveTo(right, bottom - lineLength)
            path.lineTo(right, bottom)
            path.lineTo(right - lineLength, bottom)
            canvas.drawPath(path, pen);

            path = Path()
            pen.color = ContextCompat.getColor(context!!, R.color.focus_30)
            path.moveTo(right - lineLength, bottom)
            path.lineTo(left + lineLength, bottom)
            canvas.drawPath(path, pen);

            path = Path()
            pen.color = ContextCompat.getColor(context!!, R.color.focus)
            path.moveTo(left + lineLength, bottom)
            path.lineTo(left, bottom)
            path.lineTo(left, bottom - lineLength)
            canvas.drawPath(path, pen);

            path = Path()
            pen.color = ContextCompat.getColor(context!!, R.color.focus_30)
            path.moveTo(left, bottom - lineLength)
            path.lineTo(left, top + lineLength)
            canvas.drawPath(path, pen);

            path = Path()
            pen.color = ContextCompat.getColor(context!!, R.color.focus)
            path.moveTo(left, top + lineLength)
            path.lineTo(left, top)
            canvas.drawPath(path, pen);
            path.close()
        }

    }

    fun setBoundingBoxList(boundingBoxList: ArrayList<ArrayList<Int>>) {
        this.boundingBoxList.clear()
        this.boundingBoxList.addAll(boundingBoxList)
        invalidate()
    }
}
