package com.example.onepic.Edit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.onepic.ImageToolModule
import com.example.onepic.R
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions

class RewindModule() {

    private lateinit var detector: FaceDetector

    init {
        setFaceDetecter()
    }

    /**
     * setFaceDetecter()
     *      - face Detection을 하기 위해 필요한 분석기 옵션 설정
     */
    private fun setFaceDetecter() {
        // High-accuracy landmark detection and face classification
        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .enableTracking()
            .build()
        detector = FaceDetection.getClient(highAccuracyOpts)
    }

    fun getDrawFaceBoxBitmap(bitmap: Bitmap): Bitmap {
        return runFaceDetection(bitmap)
    }

    /**
     * runFaceDetection(bitmap: Bitmap)
     *      - bitmap에서 얼굴 detection을 실행 및
     *        감지된 얼굴에 사각형 그리기
     */
    private fun runFaceDetection(bitmap: Bitmap): Bitmap{

        // face Detection
        val faceResultToDisplay = getFaceDetectionResult(bitmap)

        // faceDetection 결과(bindingBox) 그리기
        return ImageToolModule().drawDetectionResult(bitmap, faceResultToDisplay, R.color.main_color)
    }

    /**
     * getFaceDetectionResult(bitmap: Bitmap): ArrayList<Face>
     *     - face detection을 실행하고 결과를 return
     */
    private fun getFaceDetectionResult(bitmap: Bitmap): ArrayList<Face> {
        var facesResult : ArrayList<Face>? = null
        var returnState = false

        val image = InputImage.fromBitmap(bitmap, 0)

        detector.process(image)
            .addOnSuccessListener { faces ->
                facesResult = faces as ArrayList<Face>
                returnState = true
            }
            .addOnFailureListener {
                println("fail")
                returnState = true
            }
        while(!returnState) {
            System.out.println("wait || ")
            Thread.sleep(500)
        }
        return facesResult!!

    }





}