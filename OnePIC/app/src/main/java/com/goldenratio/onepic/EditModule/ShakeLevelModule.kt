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

    fun calculateSharpness(mat: Mat): Double {
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

    fun bitmapToMat(bitmap: Bitmap): Mat {
        val mat = Mat(bitmap.height, bitmap.width, CvType.CV_8UC4)
        Utils.bitmapToMat(bitmap, mat)
        return mat
    }

    fun shakeLevelDetection(bitmapList: ArrayList<Bitmap>) : ArrayList<Double> {
        Log.d("anaylsos", "shakeDetection start")
        var shakeLevelList = arrayListOf<Double>()
        var shakeMaxIndex = 0

        for (i in 0 until bitmapList.size) {
            Log.d("anaylsos", "for $i start")
            val map = bitmapToMat(bitmapList[i])
            Log.d("anaylsos", "for $i map 완성")
            val shakeLevel = calculateSharpness(map)
            Log.d("openCV", "shakeLevel [ $i ] : $shakeLevel ")
            shakeLevelList.add(shakeLevel)

            if (i != 0)
                if (shakeLevelList[shakeMaxIndex] < shakeLevel) {
                    shakeMaxIndex = i
                }
        }

        shakeLevelList = ImageToolModule().adjustMinMaxValues(shakeLevelList, 0.0,2.0)

        return shakeLevelList
    }
}