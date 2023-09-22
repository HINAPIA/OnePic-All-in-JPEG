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

    /**
     * 객체 인식을 위한 객체감지기를 설정한다.
     */
    private fun setDetecter() {
        // High-accuracy landmark detection and face classification
        val highFastOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()

        faceDetector = FaceDetection.getClient(highFastOpts)

        // Initialize the detector object
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(3)          // 최대 결과 (모델에서 감지해야 하는 최대 객체 수)
            .setScoreThreshold(0.1f)    // 점수 임계값 (감지된 객체를 반환하는 객체 감지기의 신뢰도)
            .build()
        customObjectDetector = ObjectDetector.createFromFileAndOptions(
            context,
            "lite-model_efficientdet_lite0_detection_metadata_1.tflite",
            options
        )
    }

    /**
     * TFLite Object Detection function
     * 이미지 속 객체를 감지하고, 감지된 객체 결과 중 boundingBox를 이미지에 표시해 반환한다.
     *
     * @param bitmap 객체를 감지한 이미지
     * @return 감지 결과가 표시된 이미지 반환
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
     * ObjectDetection 결과(bindingBox)를 반환한다.
     *
     * @param bitmap 객체를 감지한 이미지
     * @return 감지 결과 반환
     */
    private fun getObjectDetection(bitmap: Bitmap): List<DetectionResult> {
        // Step 1: Create TFLite's TensorImage object
        val image = TensorImage.fromBitmap(bitmap)

        // Step 2: Feed given image to the detector
        val results = customObjectDetector.detect(image)

        // Step 3: Parse the detection result and show it
        val resultToDisplay = results.map {
            // Create a data object to display the detection result
            DetectionResult(it.boundingBox)
        }
        return resultToDisplay
    }

    /**
     * 객체 분석 된 boundingBox에 맞춰 이미지 위에 표시한 후 표시된 이미지를 반환한다.
     *
     * @param bitmap 객체를 감지한 이미지이자, 감지 결과를 그릴 이미지
     * @param detectionResults 감지 결과
     * @return 감지 결과가 표시된 이미지 반환
     */
    private fun drawDetectionResult(
        bitmap: Bitmap,
        detectionResults: List<DetectionResult>
    ): Bitmap? {
        val outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(outputBitmap)
        val pen = Paint()
        pen.textAlign = Paint.Align.LEFT

        // 비트맵 지우기
        outputBitmap.eraseColor(Color.TRANSPARENT)

        detectionResults.forEach {

            val boundingBox = it.boundingBox

            val top = boundingBox.top * scaleFactor
            val bottom = boundingBox.bottom * scaleFactor
            val left = boundingBox.left * scaleFactor
            val right = boundingBox.right * scaleFactor

            val lineLength = 50f

            var path = Path()
            boxPaint.color = ContextCompat.getColor(context, R.color.focus)
            path.moveTo(left - boxPaint.strokeWidth/2f, top)
            path.lineTo(left + lineLength, top)
            canvas.drawPath(path, boxPaint)

            path = Path()
            boxPaint.color = ContextCompat.getColor(context, R.color.focus_30)
            path.moveTo(left + lineLength, top)
            path.lineTo(right - lineLength, top)
            canvas.drawPath(path, boxPaint)

            path = Path()
            boxPaint.color = ContextCompat.getColor(context, R.color.focus)
            path.moveTo(right - lineLength, top)
            path.lineTo(right, top)
            path.lineTo(right, top + lineLength)
            canvas.drawPath(path, boxPaint)

            path = Path()
            boxPaint.color = ContextCompat.getColor(context, R.color.focus_30)
            path.moveTo(right, top + lineLength)
            path.lineTo(right, bottom - lineLength)
            canvas.drawPath(path, boxPaint)

            path = Path()
            boxPaint.color = ContextCompat.getColor(context, R.color.focus)
            path.moveTo(right, bottom - lineLength)
            path.lineTo(right, bottom)
            path.lineTo(right - lineLength, bottom)
            canvas.drawPath(path, boxPaint)

            path = Path()
            boxPaint.color = ContextCompat.getColor(context, R.color.focus_30)
            path.moveTo(right - lineLength, bottom)
            path.lineTo(left + lineLength, bottom)
            canvas.drawPath(path, boxPaint)

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
            canvas.drawPath(path, boxPaint)

            path = Path()
            boxPaint.color = ContextCompat.getColor(context, R.color.focus)
            path.moveTo(left, top + lineLength)
            path.lineTo(left, top)
            canvas.drawPath(path, boxPaint)
            path.close()
        }
        return outputBitmap
    }

    /**
     * 이미지의 객체 감지 결과 전체를 반환한다.
     *
     * @return 이미지의 객체 감지 결과 전체
     */
    fun getDetectionResults() : List<DetectionResult> {
        return detectionResult
    }

    /**
     * 감지를 중지시키고, 현재 이미지 객체 감지 결과 일부를 순서대로 반환한다.
     * 더이상 반환할 결과가 없을 경우 null을 반환한다.
     *
     * @return 이미지의 객체 감지 결과 일부 혹은 null
     */
    fun getDetectionResult() : DetectionResult? {
        isGetDetectionResult = true

        if(detectionResult.size > detectionResultIndex) {
            return detectionResult[detectionResultIndex++]
        }
        else {
            return null
        }
    }

    /**
     * 감지 결과를 초기화한다.
     */
    fun resetDetectionResult() {
        isGetDetectionResult = false
        detectionResultIndex = 0
    }

    /**
     * 현재 감지를 중지한 상태인지 상태 값을 반환한다.
     *
     * @return 감지 상태 값 반환
     */
    fun getIsDetectionStop() : Boolean {
        return isGetDetectionResult
    }

    /**
     * 현재 이미지 속 감지된 객체의 수를 반환한다.
     *
     * @return 감지된 객체 수
     */
    fun getDetectionSize() : Int {
        return detectionResult.size
    }
}
data class DetectionResult(val boundingBox: RectF)
