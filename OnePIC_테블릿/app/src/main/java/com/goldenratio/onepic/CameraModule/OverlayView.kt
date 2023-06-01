package com.goldenratio.onepic.CameraModule

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import com.goldenratio.onepic.R
import java.util.LinkedList
import kotlin.math.max
import org.tensorflow.lite.task.vision.detector.Detection

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results: List<Detection> = LinkedList<Detection>()
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()

    private var scaleFactor: Float = 1f

    private var bounds = Rect()

    private var rectLength : Int = 0
    private var divide = 4

    init {
        initPaints()
    }

    fun clear() {
        textPaint.reset()
        textBackgroundPaint.reset()
        boxPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
//        textBackgroundPaint.color = Color.BLACK
//        textBackgroundPaint.style = Paint.Style.FILL
//        textBackgroundPaint.textSize = 50f
//
//        textPaint.color = Color.WHITE
//        textPaint.style = Paint.Style.FILL
//        textPaint.textSize = 50f

//        boxPaint.color = ContextCompat.getColor(context!!, R.color.focus)
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE
        rectLength = 120
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        for (result in results) {
            val boundingBox = result.boundingBox

            val top = boundingBox.top * scaleFactor
            val bottom = boundingBox.bottom * scaleFactor
            val left = boundingBox.left * scaleFactor
            val right = boundingBox.right * scaleFactor



            val width = right - left
            val height = bottom - top
            var lineLength = 50f

//            if(width < height) lineLength = width / 5f
//            else lineLength = height / 5f

            var path = Path()
            boxPaint.color = ContextCompat.getColor(context!!, R.color.focus)
            path.moveTo(left - boxPaint.strokeWidth/2f, top)
            path.lineTo(left + lineLength, top)
            canvas.drawPath(path, boxPaint);

            path = Path()
            boxPaint.color = ContextCompat.getColor(context!!, R.color.focus_30)
            path.moveTo(left + lineLength, top)
            path.lineTo(right - lineLength, top)
            canvas.drawPath(path, boxPaint);

            path = Path()
            boxPaint.color = ContextCompat.getColor(context!!, R.color.focus)
            path.moveTo(right - lineLength, top)
            path.lineTo(right, top)
            path.lineTo(right, top + lineLength)
            canvas.drawPath(path, boxPaint);

            path = Path()
            boxPaint.color = ContextCompat.getColor(context!!, R.color.focus_30)
            path.moveTo(right, top + lineLength)
            path.lineTo(right, bottom - lineLength)
            canvas.drawPath(path, boxPaint);

            path = Path()
            boxPaint.color = ContextCompat.getColor(context!!, R.color.focus)
            path.moveTo(right, bottom - lineLength)
            path.lineTo(right, bottom)
            path.lineTo(right - lineLength, bottom)
            canvas.drawPath(path, boxPaint);

            path = Path()
            boxPaint.color = ContextCompat.getColor(context!!, R.color.focus_30)
            path.moveTo(right - lineLength, bottom)
            path.lineTo(left + lineLength, bottom)
            canvas.drawPath(path, boxPaint);

            path = Path()
            boxPaint.color = ContextCompat.getColor(context!!, R.color.focus)
            path.moveTo(left + lineLength, bottom)
            path.lineTo(left, bottom)
            path.lineTo(left, bottom - lineLength)
            canvas.drawPath(path, boxPaint);

            path = Path()
            boxPaint.color = ContextCompat.getColor(context!!, R.color.focus_30)
            path.moveTo(left, bottom - lineLength)
            path.lineTo(left, top + lineLength)
            canvas.drawPath(path, boxPaint);

            path = Path()
            boxPaint.color = ContextCompat.getColor(context!!, R.color.focus)
            path.moveTo(left, top + lineLength)
            path.lineTo(left, top)
            canvas.drawPath(path, boxPaint);
            path.close()

            //********** Center 부분만 초점 표시 **********//

//            val centerX = locations.get(x+1)*w + (locations.get(x+3)*w - locations.get(x+1)*w)/2
//            val centerY = locations.get(x)*h + (locations.get(x+2)*h - locations.get(x)*h)/2
//
//            val centerX = left + (right - left)/2
//            val centerY = top + (bottom - top)/2
//
//            path.moveTo(centerX - rectLength/2 - boxPaint.strokeWidth/2, centerY - rectLength/2)
//            path.lineTo(centerX - rectLength/2 + rectLength/5, centerY - rectLength/2)
//            path.moveTo(centerX - rectLength/2 + 4*(rectLength/5), centerY - rectLength/2)
//            path.lineTo(centerX + rectLength/2, centerY - rectLength/2)
//
//            path.lineTo(centerX + rectLength/2, centerY - rectLength/2 + rectLength/5)
//            path.moveTo(centerX + rectLength/2, centerY - rectLength/2 + 4*(rectLength/5))
//            path.lineTo(centerX + rectLength/2, centerY + rectLength/2)
//
//            path.lineTo(centerX + rectLength/2 - rectLength/5, centerY + rectLength/2)
//            path.moveTo(centerX + rectLength/2 - 4*(rectLength/5), centerY + rectLength/2)
//            path.lineTo(centerX - rectLength/2, centerY + rectLength/2)
//
//            path.lineTo(centerX - rectLength/2, centerY + rectLength/2 - rectLength/5)
//            path.moveTo(centerX - rectLength/2, centerY + rectLength/2 - 4*(rectLength/5))
//            path.lineTo(centerX - rectLength/2, centerY - rectLength/2)


//            canvas.drawPath(path, boxPaint)

        }
    }

    fun setResults(
        detectionResults: MutableList<Detection>,
        imageHeight: Int,
        imageWidth: Int,
    ) {
        results = detectionResults

        // PreviewView is in FILL_START mode. So we need to scale up the bounding box to match with
        // the size that the captured images will be displayed.
        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}