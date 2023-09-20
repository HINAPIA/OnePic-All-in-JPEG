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

class FaceDetectionModule {

    private lateinit var detector: FaceDetector
    private var modelCoroutine: ArrayList<Job> = arrayListOf()
    private val imageToolModule: ImageToolModule = ImageToolModule()
    private var faceArraylist: ArrayList<ArrayList<Face>?> = arrayListOf()
    private var checkFaceDetection = false
    private var checkFaceDetectionCall = false

    private var eyesAnalysisResults = arrayListOf<Double>()
    private var smilingAnalysisResults = arrayListOf<Double>()

    init {
        setFaceDetector()
    }

    /**
     * 얼굴 감지 모델 실행이 완료 여부를 반환한다.
     *
     * @return 얼굴 감지 모델 실행 완료 여부
     */
    fun getCheckFaceDetection(): Boolean {
        return checkFaceDetection
    }

    /**
     * 현재 얼굴 감지 모델이 실행되는 Coroutine을 종료한다.
     */
    fun deleteModelCoroutine() {
        if (modelCoroutine.isNotEmpty()) {
            for (i in 0 until modelCoroutine.size)
                modelCoroutine[i].cancel()

            modelCoroutine.clear()
        }
    }

    /**
     * 모든 이미지에 얼굴 감지 모델을 실행시킨다.
     *
     * @param bitmapList 얼굴 감지 모델을 실행시킬 [Bitmap] 리스트
     */
    fun allFaceDetection(bitmapList: ArrayList<Bitmap>) {
        Log.d("faceBlending", "allFaceDetection call")
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
            Log.d("FaceDetectionModule", "start = 0")
            // j 번째 사진 faces 정보 얻기
            if (bitmapList.size > 0) {
                faceArraylist[0] = runFaceContourDetection(bitmapList[0])
                Log.d("FaceDetectionModule", "end = 0")
                checkFinish[0] = true
            }

            for (j in 1 until bitmapList.size) {
                modelCoroutine.add(CoroutineScope(Dispatchers.IO).launch {
                    Log.d("FaceDetectionModule", "start = $j")
                    // j 번째 사진 faces 정보 얻기
                    try {
                        faceArraylist[j] = runFaceContourDetection(bitmapList[j])
                    } catch (e: IndexOutOfBoundsException) {
                        e.printStackTrace()
                    }
                    Log.d("FaceDetectionModule", "end = $j")
                    checkFinish[j] = true
                })
            }
        }

        while (!checkFinish.all { it }) {

        }
        checkFaceDetection = true
    }

    /**
     * 하나의 이미지에 얼굴 감지 모델을 실행하고 결과를 반환한다.
     *
     * @param bitmap 얼굴 감지 모델을 실행시킬 Bitmap
     * @return 감지 결과로 [Face] 리스트 반환
     */
    private suspend fun runFaceContourDetection(bitmap: Bitmap): ArrayList<Face> =
        suspendCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)

            detector.process(image)
                .addOnSuccessListener { faces ->
                    //faceArraylist.add(faces as ArrayList<Face>)
                    Log.d("FaceDetectionModule", "success")
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
     * 얼굴 감지를 위해 필요한 분석기에 옵션을 설정한다.
     */
    private fun setFaceDetector() {
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
     * 원하는 사진의 얼굴 감지 정보를 반환한다.
     *
     * @param index 원하는 사진의 index 정보 (얼굴 감지 정보 [faceArraylist] 중 원하는 인덱스 정보)
     * @return 해당 사진의 얼굴 감지 정보 반환
     */
    fun getFaces(index: Int): ArrayList<Face> {
        Log.d("magic", "getFaces $index")
        while (!checkFaceDetection) {
            if (faceArraylist.size > index && faceArraylist[index] != null) {
                break
            }
        }
        Log.d("magic", "ok getFaces $index")
        //TODO: Error 발생 NullPointerException , IndexOutOfBoundsException
        return if (faceArraylist.size > index)
            faceArraylist[index]!!
        else
            arrayListOf()
    }

    /**
     * 선택한 사진 속 클릭된 좌표에 있는 얼굴 위치 정보(boundingBox)를 반환한다.
     *
     * @param index 선택한 사진의 index 정보
     * @param point 터치된 좌표 정보
     * @return 해당 위치에 있는 얼굴 위치 정보(boundingBox) 반환
     */
    suspend fun getClickPointBoundingBox(index: Int, point: Point): List<Int>? =
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
     * 전체 이미지를 분석하여, 선택한 이미지 위에 각 인물 별로 가장 잘 나온 얼굴을 합성해 Bitmap을 제작한 후 반환한다.
     *
     * @param bitmapList 이미지 리스트 [Bitmap]
     * @param selectedIndex 선택한 사진의 index 정보
     * @return 가장 잘 나온 얼굴들로 합성된 Bitmap 반환
     */
    suspend fun autoBestFaceChange(bitmapList: ArrayList<Bitmap>, selectedIndex: Int): Bitmap =
        suspendCoroutine { continuation ->
            lateinit var basicInformation: List<Face>

            val bestFaceIndex: ArrayList<Int?> = ArrayList()
            val bestFace: ArrayList<Face> = ArrayList()
            var resultBitmap = bitmapList[selectedIndex]

            val checkFinish = BooleanArray(bitmapList.size)
            for (i in 0 until bitmapList.size) {
                checkFinish[i] = false
            }

            while (!checkFaceDetection) {

            }

            Log.d("autoBlending", "2. selectedImage = ${selectedIndex}")

            CoroutineScope(Dispatchers.Default).launch {
                val facesResult = faceArraylist[selectedIndex]
                // 선택한 인덱스일 경우 비교를 위한 변수 초기화
                if (facesResult != null) {
                    for (i in 0 until facesResult.size) {
                        bestFaceIndex.add(selectedIndex)
                        bestFace.add(facesResult[i])
                    }
                }
                if (facesResult != null) {
                    basicInformation = facesResult
                }
                Log.d("autoBlending", "3. selectedImage = ${basicInformation}")

                for (j in 0 until bitmapList.size) {

                    if (j == selectedIndex) {
                        // 선택한 인덱스일 경우 비교를 위한 변수 초기화
                        checkFinish[j] = true
                    } else {
                        val facesResult = faceArraylist[j]
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
                Log.d("FaceDetectionModule", bestFaceIndex.toString())

                while (!checkFinish.all { it }) {
                }

                for (i in 0 until bestFace.size) {
                    // 현재 face가 비교될 face 배열의 index 값
                    val index = getBoxComparisonIndex(bestFace[i], basicInformation)
                    Log.d("autoBlending", "3. selectedImage = ${basicInformation[index].allLandmarks}")

                    resultBitmap = getFaceChangeBitmap(
                        resultBitmap, bitmapList[bestFaceIndex[i]!!],
                        basicInformation[index].allLandmarks, bestFace[i].allLandmarks
                    )
                }
                continuation.resume(resultBitmap)
            }
        }

    /**
     * 원하는 얼굴 [Face] 정보와 동일한 위치에 있는 얼굴 index 번호를 반환한다. (이를 동일 얼굴이라 판단한다.)
     *
     * @param check 원한는 얼굴 [Face]
     * @param original 원하는 얼굴과 동일한 위치에 얼굴을 알아내고자 하는 [Face] 리스트
     * @return 동일한 위치에 있는 얼굴 index 번호 반환
     */
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

    /**
     * 원본 이미지 위에 바꾸고 싶은 이미지의 원하는 얼굴과 동일한 위치에 있는 얼굴 위치에 합성하여 반환한다.
     *
     * @param originalImg 원본 이미지 Bitmap
     * @param changeImg 바꾸고 싶은 이미지 Bitmap
     * @param originalFaceLandmarks 원하는 얼굴과 동일한 위치에 있는 원본 얼굴 위치 정보
     * @param changeFaceLandmarks 바꾸고 싶은 이미지에 있는 원하는 얼굴 위치 정보
     * @return 합성된 이미지 반환
     */
    private fun getFaceChangeBitmap(
        originalImg: Bitmap,
        changeImg: Bitmap,
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


    /**
     * 이미지 리스트를 받아, 이미지 속 얼굴들의 눈 뜬 정도 값을 리스트로 만들어 반환한다.
     *
     * @param bitmapList 각 이미지 속 얼굴들의 눈 뜬 정도 값을 알아낼 이미지 리스트
     * @return 눈 뜬 정도 값 리스트 반환
     */
    suspend fun getEyesAnalysisResults(bitmapList: ArrayList<Bitmap>): ArrayList<Double> =
        suspendCoroutine { continuation ->

            eyesAnalysisResults = arrayListOf()
            smilingAnalysisResults = arrayListOf()

            val checkFinish = BooleanArray(bitmapList.size)
            var faceMaxIndex = 0

            for (i in 0 until bitmapList.size) {
                eyesAnalysisResults.add(0.0)
                smilingAnalysisResults.add(0.0)
                checkFinish[i] = false
            }

            while (!checkFaceDetection) {

            }
            for (j in 0 until bitmapList.size) {
                CoroutineScope(Dispatchers.Default).launch {
                    val facesResult = faceArraylist[j]
                    var eyesSum = 0.0f
                    var smilingSum = 0.0f

                    // 그 이후 인덱스일 경우, 각 face을 비교
                    if (facesResult != null) {
                        for (i in 0 until facesResult.size) {
                            // 비교할 face = checkFace
                            val checkFace = facesResult[i]

                            eyesSum +=
                                if (checkFace.leftEyeOpenProbability!! > checkFace.rightEyeOpenProbability!!) {
                                    checkFace.leftEyeOpenProbability!!
                                    checkFace.leftEyeOpenProbability!!
                                } else {
                                    checkFace.rightEyeOpenProbability!!
                                }

                            smilingSum += checkFace.smilingProbability!!

                        }
                        eyesAnalysisResults[j] = eyesSum / facesResult.size.toDouble()
                        smilingAnalysisResults[j] = smilingSum / facesResult.size.toDouble()

                        if (eyesAnalysisResults[j].isNaN()) {
                            eyesAnalysisResults[j] = 0.0
                            Log.d("anaylsis", "isNan")
                        }

                        if (smilingAnalysisResults[j].isNaN()) {
                            smilingAnalysisResults[j] = 0.0
                            Log.d("anaylsis", "isNan")
                        }

                        Log.d(
                            "anaylsis",
                            "[$j] = faceDetection FaceDetectionModule = ${eyesAnalysisResults[j]} , ${smilingAnalysisResults[j]}"
                        )

                    }
                    checkFinish[j] = true
                }
            }

            while (!checkFinish.all { it }) {
            }

            continuation.resume(imageToolModule.adjustMinMaxValues(eyesAnalysisResults, 0.0,1.0))
        }

    /**
     * 이미지 속 얼굴들의 웃고 있는 정도 값을 리스트로 만들어 반환한다.
     *
     * @return 웃고 있는 정도 값 리스트 반환
     */
    fun getSmilingAnalysisResults(): ArrayList<Double> {
        return imageToolModule.adjustMinMaxValues(smilingAnalysisResults, 0.0,1.0)
    }
}