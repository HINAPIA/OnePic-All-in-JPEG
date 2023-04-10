package com.example.onepic.EditModule

import android.graphics.Bitmap
import android.graphics.Point
import com.example.onepic.ImageToolModule
import com.example.onepic.R
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class RewindModule() {

    private lateinit var detector: FaceDetector

    private val imageToolModule: ImageToolModule

    init {
        setFaceDetecter()
        imageToolModule = ImageToolModule()
    }

    /**
     * setFaceDetecter()
     *      - face Detection을 하기 위해 필요한 분석기 옵션 설정
     */
    private fun setFaceDetecter() {
         //High-accuracy landmark detection and face classification
        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .enableTracking()
            .build()
        detector = FaceDetection.getClient(highAccuracyOpts)
    }


    /**
     * runFaceDetection(bitmap: Bitmap)
     *      - bitmap에서 얼굴 detection을 실행 및
     *        감지된 얼굴에 사각형 그리기
     */
    suspend fun runFaceDetection(bitmap: Bitmap): Bitmap = suspendCoroutine { bitmapResult ->

        // face Detection
        var faces : ArrayList<Face>? = null
        CoroutineScope(Dispatchers.Default).launch {
            try {
                faces = runFaceContourDetection(bitmap)
                // 얼굴 검출 결과값을 이용하여 다음 작업을 수행합니다.
            } catch (e: Exception) {
                e.printStackTrace()
                // 예외 처리를 수행합니다.
            }

            if (faces == null)
                bitmapResult.resume(bitmap)
            else
            // faceDetection 결과(bindingBox) 그리기
                bitmapResult.resume(ImageToolModule().drawDetectionResult(bitmap, faces!!, R.color.main_color))

        }
    }

    /**
     * getDrawRectFaceBitmap(bitmap: Bitmap)
     *      - bitmap에서 얼굴 detection을 실행 및
     *        감지된 얼굴에 사각형 그리기
     */
    suspend fun getDrawRectFaceBitmap(bitmap: Bitmap): Bitmap = suspendCoroutine { bitmapResult ->

        // face Detection
        var faces : ArrayList<Face>? = null
        CoroutineScope(Dispatchers.Default).launch {
            try {
                faces = runFaceContourDetection(bitmap)
                // 얼굴 검출 결과값을 이용하여 다음 작업을 수행합니다.
            } catch (e: Exception) {
                e.printStackTrace()
                // 예외 처리를 수행합니다.
            }

            if (faces == null)
                bitmapResult.resume(bitmap)
            else
            // faceDetection 결과(bindingBox) 그리기
                bitmapResult.resume(ImageToolModule().drawDetectionResult(bitmap, faces!!, R.color.main_color))

        }
    }

    /**
     * getClickPointCropBitmap(bitmap: Bitmap)
     *      - bitmap에서 얼굴 detection을 실행 및
     *        감지된 얼굴에 사각형 그리기
     */
    suspend fun getClickPointBoundingBox(bitmap: Bitmap, point: Point): List<Int>? = suspendCoroutine { bitmapResult ->

        // face Detection
        var faces : ArrayList<Face>? = null
        CoroutineScope(Dispatchers.Default).launch {
            try {
                faces = runFaceContourDetection(bitmap)
                // 얼굴 검출 결과값을 이용하여 다음 작업을 수행합니다.
            } catch (e: Exception) {
                e.printStackTrace()
                // 예외 처리를 수행합니다.
            }

            if (faces == null)
                bitmapResult.resume(null)
            else {
                var checkResume = false
                for (i in 0 until faces!!.size) {
                    val face = faces!![i]
                    if (imageToolModule.checkPointInRect(point, face.boundingBox)) {
                        checkResume = true

                        val landmarks = face.allLandmarks

                        val addStartY =
                            ((landmarks[0].position.y - landmarks[3].position.y) / 2).toInt()
                        val addEndY =
                            ((landmarks[0].position.y - landmarks[5].position.y) / 2).toInt()

                        val boundingBox = listOf<Int>(
                            face.boundingBox.left, // left: 진짜 얼굴이 인지된 left 값
                            face.boundingBox.top, // top
                            face.boundingBox.right, // right
                            face.boundingBox.bottom, // bottom
                            landmarks[2].position.x.toInt(), // cropLeft: 실질적으로 이미지에 덮붙이기 위해 자를 left 값
                            landmarks[3].position.y.toInt() - addStartY, // cropTop
                            landmarks[7].position.x.toInt(), // cropRight
                            landmarks[0].position.y.toInt() + addEndY  // cropBottom
                        )
                        bitmapResult.resume(boundingBox)
                    }
                }
                if (!checkResume) bitmapResult.resume(null)
            }
        }
    }

    /**
     * runFaceContourDetection(bitmap: Bitmap): ArrayList<Face>
     *     - face detection을 실행하고 결과를 return
     */
    suspend fun runFaceContourDetection(bitmap: Bitmap): ArrayList<Face> = suspendCoroutine { continuation ->
        val image = InputImage.fromBitmap(bitmap,90)

        detector.process(image)
            .addOnSuccessListener { faces ->
                println("success")
                // 얼굴 검출 성공 시 콜백 함수
                continuation.resume(faces as ArrayList<Face>)
            }
            .addOnFailureListener { e ->
                // 얼굴 검출 실패 시 콜백 함수
                e.printStackTrace()
                continuation.resumeWithException(e)
            }
    }
}