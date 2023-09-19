package com.goldenratio.onepic.EditModule

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.goldenratio.onepic.R

class FocusOverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private val boundingBoxList : ArrayList<ArrayList<Int>> = ArrayList()

    /**
     * 초점 변경시, 초점 변경 할 수 있는 객체를 캔버스에 그려 표시한다.
     *
     * @param canvas 객체를 그릴 캔버스
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val pen = Paint()

        pen.strokeWidth = 25f
        pen.style = Paint.Style.STROKE
        var lineLength = 130f

        for(box in boundingBoxList){
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
}
