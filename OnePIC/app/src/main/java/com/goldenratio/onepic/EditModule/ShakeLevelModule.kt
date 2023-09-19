package com.goldenratio.onepic.EditModule

import android.graphics.Bitmap
import android.util.Log
import com.goldenratio.onepic.ImageToolModule
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

class ShakeLevelModule {

    init {
        val isIntialized = OpenCVLoader.initDebug()
        Log.d("openCV", "isIntialized = $isIntialized")
    }

    /**
     * [Bitmap]을 [Mat] 으로 변환하여 반환한다.
     *
     * @param bitmap Map으로 변환할 Bitmap
     * @return 변환된 Map 반환
     */
    private fun bitmapToMat(bitmap: Bitmap): Mat {
        val mat = Mat(bitmap.height, bitmap.width, CvType.CV_8UC4)
        Utils.bitmapToMat(bitmap, mat)
        return mat
    }

    /**
     * 이미지 리스트를 받아, 이미지들의 샤프니스 값을 리스트로 만들어 반환한다.
     *
     * @param bitmapList 샤프니스 값을 알아낼 이미지 리스트
     * @return 샤프니스 값 리스트 반환
     */
    fun shakeLevelDetection(bitmapList: ArrayList<Bitmap>) : ArrayList<Double> {
        Log.d("anaylsos", "shakeDetection start")
        var shakeLevelList = arrayListOf<Double>()
        var shakeMaxIndex = 0

        for (i in 0 until bitmapList.size) {
            val map = bitmapToMat(bitmapList[i])
            val shakeLevel = calculateSharpness(map)
            Log.d("openCV", "shakeLevel [ $i ] : $shakeLevel ")
            shakeLevelList.add(shakeLevel)

            if (i != 0)
                if (shakeLevelList[shakeMaxIndex] < shakeLevel) {
                    shakeMaxIndex = i
                }
        }

        shakeLevelList = ImageToolModule().adjustMinMaxValues(shakeLevelList, 0.0,1.0)

        return shakeLevelList
    }

    /**
     * Map을 그레이 스케일로 변환한 후 라폴라시안 필터를 적용하여 샤프니스 값을 계산하여 반환한다.
     *
     * @param mat 샤프니스 값을 알아낼 Map
     * @return 샤프니스 값 반환
     */
    private fun calculateSharpness(mat: Mat): Double {
        Log.d("anaylsos", "calculateSharpness start")
        // 이미지를 그레이스케일로 변환
        val grayMat = Mat()
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY)

        Log.d("anaylsos", "calculateSharpness gray")
        // 라플라시안 필터를 적용하여 샤프니스 계산
        val laplacianMat = Mat()
        Log.d("anaylsos", "calculateSharpness map create")
        Imgproc.Laplacian(grayMat, laplacianMat, CvType.CV_64F)

        Log.d("anaylsos", "calculateSharpness Laplacian")
        val sharpness = Core.mean(laplacianMat)
        Log.d("anaylsos", "calculateSharpness sharpness")

        mat.release()
        grayMat.release()
        laplacianMat.release()
        return sharpness.`val`[0]
    }
}