package com.goldenratio.onepic.EditModule

import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.core.CvType.CV_8U
import org.opencv.core.CvType.CV_8UC3
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc

class ObjectExtractModule {

    init {
        val isIntialized = OpenCVLoader.initDebug()
    }

    fun extractObjFromBitmap(bitmap : Bitmap): Bitmap{
        // 입력 이미지
        val mat = bitmapToMat(bitmap)
        // 객체를 나타내는 영역 지정 rect
        val rect = Rect(0, 0, mat.cols(), mat.rows())

        Log.v("obj extract", "mat. cols x rows : ${mat.cols()} x ${mat.rows()}")

        // 객체와 배경 구분을 위한 mask ( Gray Scale )
        val mask = Mat.zeros(mat.size(), CvType.CV_8UC1)
        // 초기 배경 & 전경 모델
//        val bgModel = Mat()
//        val fgModel = Mat()
        val bgModel = Mat.zeros(1, 65, CvType.CV_64F)
        val fgModel = Mat.zeros(1, 65, CvType.CV_64F)

        Imgproc.grabCut(mat, mask, rect, bgModel, fgModel, 5, Imgproc.GC_INIT_WITH_MASK)

        // ( Color )
        val mask2 = Mat.zeros(mat.size(), CV_8U)
        for( i in 0 until mask.rows()) {
            for(j in 0 until mask.cols()) {
                val maskValue = mask.get(i, j)[0]
                // 배경이거나 배경으로 예상되는 부분 > 검정
                if(maskValue == Imgproc.GC_PR_BGD.toDouble()
                    || maskValue == Imgproc.GC_BGD.toDouble()) {
                    mask2.put(i, j, floatArrayOf(0.0f, 0.0f, 0.0f))
                } else {
                    // 그렇지 않으면 > 흰색
                    mask2.put(i, j, floatArrayOf(1.0f, 1.0f, 1.0f))
                }
            }
        }

        val dst = Mat.zeros(mat.size(), mat.type())
        // mat 이미지에서 객체 영역을 제외한 모든 부분이 검은색으로 처리된 결과 이미지
        Core.multiply(mat, mask2, dst)

        val extractedObj = matToBitmap(dst)
        return extractedObj

//        // Matrices that OpenCV will be using internally
//        val bgModel = Mat()
//        val fgModel = Mat()
//
//        val srcImage = Mat()
//        Utils.bitmapToMat(bitmap, srcImage)
//        val iterations = 5
//
//        // Mask image where we specify which areas are background, foreground or probable background/foreground
//        val firstMask = Mat()
//
//        val source = Mat(1, 1, CvType.CV_8UC3, Scalar(Imgproc.GC_PR_FGD.toDouble()))
//        val firstPoint = Point(0.0, 0.0)
//        val secondPoint = Point(bitmap.width.toDouble(), bitmap.height.toDouble())
//        val rect = Rect(firstPoint, secondPoint)
//
//        // Run the grab cut algorithm with a rectangle (for subsequent iterations with touch-up strokes,
//        // flag should be Imgproc.GC_INIT_WITH_MASK)
//        Imgproc.grabCut(srcImage, firstMask, rect, bgModel, fgModel, iterations, Imgproc.GC_INIT_WITH_RECT)
//
//        // Create a matrix of 0s and 1s, indicating whether individual pixels are equal
//        // or different between "firstMask" and "source" objects
//        // Result is stored back to "firstMask"
//        Core.compare(firstMask, source, firstMask, Core.CMP_EQ)
//
//        // Create a matrix to represent the foreground, filled with white color
//        val foreground = Mat(srcImage.size(), CvType.CV_8UC3, Scalar(255.0, 255.0, 255.0))
//
//        // Copy the foreground matrix to the first mask
//        srcImage.copyTo(foreground, firstMask)
//
//        // Create a red color
//        val color = Scalar(255.0, 0.0, 0.0, 255.0)
//        // Draw a rectangle using the coordinates of the bounding box that surrounds the foreground
//        Imgproc.rectangle(srcImage, firstPoint, secondPoint, color)
//
//        // Create a new matrix to represent the background, filled with white color
//        val background = Mat(srcImage.size(), CvType.CV_8UC3, Scalar(0.0, 0.0, 0.0))
//
//        val mask = Mat(foreground.size(), CvType.CV_8UC1, Scalar(255.0, 255.0, 255.0))
//        // Convert the foreground's color space from BGR to grayscale
//        Imgproc.cvtColor(foreground, mask, Imgproc.COLOR_BGR2GRAY)
//
//        // Separate out regions of the mask by comparing the pixel intensity with respect to a threshold value
//        Imgproc.threshold(mask, mask, 254.0, 255.0, Imgproc.THRESH_BINARY_INV)
//
//        // Create a matrix to hold the final image
//        val dst = Mat()
//        // Copy the background matrix onto the matrix that represents the final result
//        background.copyTo(dst)
//
//        val vals = Mat(1, 1, CvType.CV_8UC3, Scalar(0.0))
//        // Replace all 0 values in the background matrix given the foreground mask
//        background.setTo(vals, mask)
//
//        // Add the sum of the background and foreground matrices by applying the mask
//        Core.add(background, foreground, dst, mask)
//
//        // Convert the result matrix to bitmap
//        val resultBitmap = Bitmap.createBitmap(dst.cols(), dst.rows(), Bitmap.Config.ARGB_8888)
//        Utils.matToBitmap(dst, resultBitmap)
//
//        // Clean up used resources
//        srcImage.release()
//        firstMask.release()
//        source.release()
//        bgModel.release()
//        fgModel.release()
//        vals.release()
//        dst.release()
//
//        return resultBitmap

    }

//    fun bitmapToMat(bitmap: Bitmap): Mat {
//        val rgbBitmap = bitmap.copy(Bitmap.Config.RGB_565, true)
//        val mat = Mat(rgbBitmap.height, rgbBitmap.width, CvType.CV_8UC3)
////        val mat = Mat(bitmap.height, bitmap.width, CvType.CV_8UC3)
//        Utils.bitmapToMat(bitmap, mat)
//        return mat
//    }

    fun bitmapToMat(bitmap: Bitmap): Mat {
        val mat = Mat(bitmap.height, bitmap.width, CvType.CV_8UC3)
        val tempBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        Utils.bitmapToMat(tempBitmap, mat)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2BGR) // 이미지를 BGR 형식으로 변환
        return mat
    }


    fun matToBitmap(mat: Mat): Bitmap {
        val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bitmap)
        return bitmap
    }
}