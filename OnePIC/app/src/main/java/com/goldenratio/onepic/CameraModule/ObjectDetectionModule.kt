package com.goldenratio.onepic.CameraModule

import android.content.Context
import android.graphics.*
import android.view.LayoutInflater
import android.widget.Button
import androidx.core.content.ContextCompat
import com.goldenratio.onepic.R
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector

class ObjectDetectionModule(
   val context: Context
) {

    private lateinit var detectionResult: List<DetectionResult>
    private lateinit var customObjectDetector: ObjectDetector
    private lateinit var faceDetector: FaceDetector
    private var isGetDetectionResult = false
    var detectionResultIndex = 0

    private var rectLength : Int = 0
    private var scaleFactor: Float = 1f
    private var boxPaint = Paint()

    var bitmapWidth = 0

    init {
        setDetecter()
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE
        rectLength = 120
    }

    private fun setDetecter() {
        // High-accuracy landmark detection and face classification
        val highFastOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()

        faceDetector = FaceDetection.getClient(highFastOpts)

        // Initialize the detector object
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(5)          // 최대 결과 (모델에서 감지해야 하는 최대 객체 수)
            .setScoreThreshold(0.5f)    // 점수 임계값 (감지된 객체를 반환하는 객체 감지기의 신뢰도)
            .build()
        customObjectDetector = ObjectDetector.createFromFileAndOptions(
            context,
            "lite-model_efficientdet_lite0_detection_metadata_1.tflite",
            options
        )

    }

    /**
     * runObjectDetection(bitmap: Bitmap)
     *      TFLite Object Detection function
     *      사진 속 객체를 감지하고, 감지된 객체에 boundingBox를 표시해 반환한다.
     */
    fun runObjectDetection(bitmap: Bitmap): Bitmap {

        if(!isGetDetectionResult) {
            bitmapWidth = bitmap.width

            // Object Detection
            detectionResult = getObjectDetection(bitmap)
        }

        // ObjectDetection 결과(bindingBox) 그리기
        val objectDetectionResult =
            drawDetectionResult(bitmap, detectionResult)

        return objectDetectionResult!!
    }

    /**
     * getObjectDetection(bitmap: Bitmap):
     *         ObjectDetection 결과(bindingBox)를 반환한다.
     */
    private fun getObjectDetection(bitmap: Bitmap): List<DetectionResult> {
        // Step 1: Create TFLite's TensorImage object
        val image = TensorImage.fromBitmap(bitmap)

        // Step 2: Feed given image to the detector
        val results = customObjectDetector.detect(image)

        // Step 3: Parse the detection result and show it
        val resultToDisplay = results.map {
            // Get the top-1 category and craft the display text
            val category = it.categories.first()
            val text = "${category.label}"

            // Create a data object to display the detection result
            DetectionResult(it.boundingBox, text)
        }
        return resultToDisplay
    }

    /**
     * drawDetectionResult(bitmap: Bitmap, detectionResults: List<DetectionResult>
     *      객체 분석 된 boundingBox에 맞춰 이미지 위에 표시한 후 표시된 이미지를 반환한다.
     */
    private fun drawDetectionResult(
        bitmap: Bitmap,
        detectionResults: List<DetectionResult>
    ): Bitmap? {
        val outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(outputBitmap)
        val pen = Paint()
        pen.textAlign = Paint.Align.LEFT


        detectionResults.forEach {
            // draw bounding box
//            pen.color = Color.parseColor("#B8C5BB")
//            pen.strokeWidth = 10F
//            pen.style = Paint.Style.STROKE
//            val box = it.boundingBox
//            canvas.drawRoundRect(box, 10F, 10F, pen)

            val boundingBox = it.boundingBox

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
            boxPaint.color = ContextCompat.getColor(context, R.color.focus)
            path.moveTo(left - boxPaint.strokeWidth/2f, top)
            path.lineTo(left + lineLength, top)
            canvas.drawPath(path, boxPaint);

            path = Path()
            boxPaint.color = ContextCompat.getColor(context, R.color.focus_30)
            path.moveTo(left + lineLength, top)
            path.lineTo(right - lineLength, top)
            canvas.drawPath(path, boxPaint);

            path = Path()
            boxPaint.color = ContextCompat.getColor(context, R.color.focus)
            path.moveTo(right - lineLength, top)
            path.lineTo(right, top)
            path.lineTo(right, top + lineLength)
            canvas.drawPath(path, boxPaint);

            path = Path()
            boxPaint.color = ContextCompat.getColor(context, R.color.focus_30)
            path.moveTo(right, top + lineLength)
            path.lineTo(right, bottom - lineLength)
            canvas.drawPath(path, boxPaint);

            path = Path()
            boxPaint.color = ContextCompat.getColor(context, R.color.focus)
            path.moveTo(right, bottom - lineLength)
            path.lineTo(right, bottom)
            path.lineTo(right - lineLength, bottom)
            canvas.drawPath(path, boxPaint);

            path = Path()
            boxPaint.color = ContextCompat.getColor(context, R.color.focus_30)
            path.moveTo(right - lineLength, bottom)
            path.lineTo(left + lineLength, bottom)
            canvas.drawPath(path, boxPaint);

            path = Path()
            boxPaint.color = ContextCompat.getColor(context, R.color.focus)
            path.moveTo(left + lineLength, bottom)
            path.lineTo(left, bottom)
            path.lineTo(left, bottom - lineLength)
            canvas.drawPath(path, boxPaint);

            path = Path()
            boxPaint.color = ContextCompat.getColor(context, R.color.focus_30)
            path.moveTo(left, bottom - lineLength)
            path.lineTo(left, top + lineLength)
            canvas.drawPath(path, boxPaint);

            path = Path()
            boxPaint.color = ContextCompat.getColor(context, R.color.focus)
            path.moveTo(left, top + lineLength)
            path.lineTo(left, top)
            canvas.drawPath(path, boxPaint);
            path.close()
        }
        return outputBitmap
    }

    fun getDetectionResults() : List<DetectionResult> {
        return detectionResult
    }

    fun getDetectionResult() : DetectionResult? {
        isGetDetectionResult = true

        if(detectionResult.size > detectionResultIndex) {
            return detectionResult[detectionResultIndex++]
        }
        else {
//            resetDetectionResult()
            return null
        }
    }

    fun resetDetectionResult() {
        isGetDetectionResult = false
        detectionResultIndex = 0
    }

    fun getIsDetectionStop() : Boolean {
        return isGetDetectionResult
    }

    fun getDetectionSize() : Int {
        return detectionResult.size
    }
}
data class DetectionResult(val boundingBox: RectF, val text: String)
