package com.goldenratio.onepic.EditModule

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import androidx.core.graphics.toRectF
import com.goldenratio.onepic.ImageToolModule
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class RewindModule() {

    private var modelCoroutine: ArrayList<Job> = arrayListOf()
    private lateinit var detector: FaceDetector
    private val imageToolModule: ImageToolModule
    private var faceArraylist: ArrayList<ArrayList<Face>?> = arrayListOf()
    private var checkFaceDetection = false
    private var checkFaceDetectionCall = false

    init {
        setFaceDetecter()
        imageToolModule = ImageToolModule()
    }

    fun getCheckFaceDetection(): Boolean {
        return checkFaceDetection
    }

    fun deleteModelCoroutine() {
        if (modelCoroutine.isNotEmpty()) {
            for (i in 0 until modelCoroutine.size)
                modelCoroutine[i].cancel()

            modelCoroutine.clear()
        }
    }

    fun allFaceDetection(bitmapList: ArrayList<Bitmap>) {
        Log.d("faceRewind", "allFaceDetection call")
        checkFaceDetectionCall = true
        checkFaceDetection = false

        faceArraylist.clear()
        deleteModelCoroutine()

        val checkFinish = BooleanArray(bitmapList.size)

        for (i in 0 until bitmapList.size) {
            checkFinish[i] = false
            faceArraylist.add(null)
        }
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("RewindModule", "start = 0")
            // j 번째 사진 faces 정보 얻기
            faceArraylist[0] = runFaceContourDetection(bitmapList[0])
            Log.d("RewindModule", "end = 0")
            checkFinish[0] = true

            for (j in 1 until bitmapList.size) {
                modelCoroutine.add(CoroutineScope(Dispatchers.IO).launch {
                    Log.d("RewindModule", "start = $j")
                    // j 번째 사진 faces 정보 얻기
                    faceArraylist[j] = runFaceContourDetection(bitmapList[j])
                    Log.d("RewindModule", "end = $j")
                    checkFinish[j] = true
                })
            }
        }

        while (!checkFinish.all { it }) {

        }
        checkFaceDetection = true
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

    fun getFaces(index: Int): ArrayList<Face> {
        Log.d("magic", "getFaces $index")
        while (!checkFaceDetection) {
            if (faceArraylist.size > index && faceArraylist[index] != null) {
                break
            }
        }
        Log.d("magic", "ok getFaces $index")
        //faces = runFaceContourDetection(bitmap)
        return faceArraylist[index]!!
    }


    /**
     * runFaceDetection(bitmap: Bitmap)
     *      - bitmap에서 얼굴 detection을 실행 및
     *        감지된 얼굴에 사각형 그리기
     */
    suspend fun runFaceDetection(index: Int): ArrayList<Face> = suspendCoroutine { bitmapResult ->

        // face Detection
        var faces: ArrayList<Face>
        CoroutineScope(Dispatchers.Default).launch {
            Log.d("magic", "runFaceDetection !!!!!!!!!")
            faces = getFaces(index)
            Log.d("magic", "end runFaceDetection !!!!!!!!!")
            bitmapResult.resume(faces)
            //bitmapResult.resume(ImageToolModule().drawDetectionResult(bitmap, faces, R.color.main_color))
        }
    }

    /**
     * getClickPointCropBitmap(bitmap: Bitmap)
     *      - bitmap에서 얼굴 detection을 실행 및
     *        감지된 얼굴에 사각형 그리기
     */
    suspend fun getClickPointBoundingBox(bitmap: Bitmap, index: Int, point: Point): List<Int>? =
        suspendCoroutine { bitmapResult ->

            // face Detection
            val faces: ArrayList<Face> = getFaces(index)
            Log.d("magic", "getClickPointBoundingBox getFaces $faces")
            if (faces.isEmpty())
                bitmapResult.resume(null)
            else {
                var checkResume = false
                for (i in 0 until faces.size) {
                    val face = faces[i]
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
                        Log.d("magic", "getClickPointBoundingBox boundingBox resume")
                        bitmapResult.resume(boundingBox)
                    }
                }
                if (!checkResume) bitmapResult.resume(null)
            }
        }

    /**
     * runFaceContourDetection(bitmap: Bitmap): ArrayList<Face>
     *     - face detection을 실행하고 결과를 return
     */
    private suspend fun runFaceContourDetection(bitmap: Bitmap): ArrayList<Face> =
        suspendCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)

            detector.process(image)
                .addOnSuccessListener { faces ->
                    //faceArraylist.add(faces as ArrayList<Face>)
                    Log.d("RewindModule", "success")
                    // 얼굴 검출 성공 시 콜백 함수
                    continuation.resume(faces as ArrayList<Face>)
                }
                .addOnFailureListener { e ->
                    // 얼굴 검출 실패 시 콜백 함수
                    e.printStackTrace()
                    continuation.resumeWithException(e)
                }
        }


    /**
     *  autoBestFaceChange
     *  (bestFaceStandard: Int, mainBitmap: Bitmap, bitmapList: ArrayList<Bitmap>): Bitmap
     *      - 여러 사진을 분석하여 각 인물 별로 가장 잘 나온 얼굴로 변환해 bitmap return
     */
    suspend fun autoBestFaceChange(bitmapList: ArrayList<Bitmap>): Bitmap =
        suspendCoroutine { continuation ->
            lateinit var basicInformation: List<Face>

            val bestFaceIndex: ArrayList<Int?> = ArrayList()
            val bestFace: ArrayList<Face> = ArrayList()
            var resultBitmap = bitmapList[0]

            val checkFinish = BooleanArray(bitmapList.size)
            for (i in 0 until bitmapList.size) {
                checkFinish[i] = false
            }

            while (!checkFaceDetection) {

            }

            CoroutineScope(Dispatchers.Default).launch {
                for (j in 0 until bitmapList.size) {
                    val facesResult = faceArraylist[j]
                    if (j == 0) {
                        // 첫번째 인덱스일 경우 비교를 위한 변수 초기화
                        if (facesResult != null) {
                            for (i in 0 until facesResult.size) {
                                bestFaceIndex.add(0)
                                bestFace.add(facesResult[i])
                            }
                        }
                        if (facesResult != null) {
                            basicInformation = facesResult
                        }
                    } else {
                        // 그 이후 인덱스일 경우, 각 face을 비교
                        if (facesResult != null) {
                            for (i in 0 until facesResult.size) {
                                // 비교할 face = checkFace
                                val checkFace = facesResult[i]

                                // 현재 face가 비교될 face 배열의 index 값
                                val index = getBoxComparisonIndex(checkFace, bestFace)

                                if (bestFace.size > index) {
                                    // bestFace 정보 알아 내기
                                    val best = bestFace[index]

                                    if (best.leftEyeOpenProbability!! < checkFace.leftEyeOpenProbability!! ||
                                        best.rightEyeOpenProbability!! < checkFace.rightEyeOpenProbability!!
                                    ) {
                                        if (best.smilingProbability!! < checkFace.smilingProbability!!) {
                                            bestFace[index] = checkFace
                                            bestFaceIndex[index] = j
                                        }
                                    }
                                    if (checkFace.rightEyeOpenProbability!! >= 0.7 || checkFace.leftEyeOpenProbability!! >= 0.7
                                        && best.smilingProbability!! < checkFace.smilingProbability!!
                                    ) {
                                        bestFace[index] = checkFace
                                        bestFaceIndex[index] = j
                                    }
                                }
                            }
                        }
                    }
                    checkFinish[j] = true
                }
                Log.d("RewindModule", bestFaceIndex.toString())

                while (!checkFinish.all { it }) { }

                for (i in 0 until bestFace.size) {
                    // 현재 face가 비교될 face 배열의 index 값
                    val index = getBoxComparisonIndex(bestFace[i], basicInformation)
                    resultBitmap = getFaceChangeBitmap(
                        resultBitmap, bitmapList[bestFaceIndex[i]!!],
                        basicInformation[index].boundingBox.toRectF(),
                        basicInformation[index].allLandmarks, bestFace[i].allLandmarks
                    )
                }
                continuation.resume(resultBitmap)
            }
        }

    private fun getBoxComparisonIndex(check: Face, original: List<Face>): Int {
        var index = 0
        // 현재 face가 어떤 face인지 알기 도와주는 중간 값
        val checkPointX =
            check.boundingBox.left + (check.boundingBox.right - check.boundingBox.left) / 2
        val checkPointY =
            check.boundingBox.top + (check.boundingBox.bottom - check.boundingBox.top) / 2
        for (k in 0 until original.size) {
            if (checkPointX >= original[k].boundingBox.left && checkPointX <= original[k].boundingBox.right &&
                checkPointY >= original[k].boundingBox.top && checkPointY <= original[k].boundingBox.bottom
            ) {
                index = k
                break
            }
        }
        return index
    }

    private fun getFaceChangeBitmap(
        originalImg: Bitmap,
        changeImg: Bitmap,
        originalFaceBoundingBox: RectF,
        originalFaceLandmarks: List<FaceLandmark>,
        changeFaceLandmarks: List<FaceLandmark>
    ): Bitmap {
        val addStartY =
            ((changeFaceLandmarks[0].position.y - changeFaceLandmarks[3].position.y) / 2).toInt()
        val addEndY =
            ((changeFaceLandmarks[0].position.y - changeFaceLandmarks[5].position.y) / 2).toInt()

        val cropImgRect = Rect(
            changeFaceLandmarks[2].position.x.toInt(), // left
            changeFaceLandmarks[3].position.y.toInt() - addStartY, // top
            changeFaceLandmarks[7].position.x.toInt(), // right
            changeFaceLandmarks[0].position.y.toInt() + addEndY  // bottom
        )

        var cropImg = imageToolModule.cropBitmap(changeImg, cropImgRect)
        cropImg = imageToolModule.circleCropBitmap(cropImg)
        val overlayImg = imageToolModule.overlayBitmap(
            originalImg, cropImg,
            (originalFaceLandmarks[2].position.x).toInt(),
            (originalFaceLandmarks[3].position.y - addStartY).toInt()
        )
        return overlayImg
    }

    suspend fun choiseBestImage(bitmapList: ArrayList<Bitmap>): Int =
        suspendCoroutine { continuation ->

            val analysisResults = arrayListOf<Float>()
            val checkFinish = BooleanArray(bitmapList.size)
            for (i in 0 until bitmapList.size) {
                analysisResults.add(0.0f)
                checkFinish[i] = false
            }

            while (!checkFaceDetection) {

            }
                for (j in 0 until bitmapList.size) {
                    CoroutineScope(Dispatchers.Default).launch {
                        val facesResult = faceArraylist[j]
                        var sum = 0.0f
                        // 그 이후 인덱스일 경우, 각 face을 비교
                        if (facesResult != null) {
                            for (i in 0 until facesResult.size) {
                                // 비교할 face = checkFace
                                val checkFace = facesResult[i]

                                val eyes =
                                    if (checkFace.leftEyeOpenProbability!! > checkFace.rightEyeOpenProbability!!) {
                                        checkFace.leftEyeOpenProbability!!
                                    } else {
                                        checkFace.rightEyeOpenProbability!!
                                    }

                                sum += checkFace.smilingProbability!! + eyes
                            }
                            analysisResults[j] = sum / facesResult.size
                        }
                        checkFinish[j] = true
                    }
                }

            while (!checkFinish.all { it }) { }

            var bestIndex = 0
            for(i in 1 until analysisResults.size) {
                if(analysisResults[bestIndex] < analysisResults[i]){
                    bestIndex = i
                }
            }
            continuation.resume(bestIndex)
        }
}