package com.example.onepic

import android.graphics.*
import com.google.mlkit.vision.face.Face
import java.io.ByteArrayOutputStream

class ImageToolModule {
    /**
     * bitmapToByteArray(bitmap: Bitmap): ByteArray
     *      - bitmap을 byteArray로 변환해서 제공
     */
    fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        var outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        return outputStream.toByteArray()
    }

    /**
     * byteArrayToBitmap(byteArray: ByteArray): Bitmap
     *      - byteArray를 bitmap으로 변환해서 제공
     */
    fun byteArrayToBitmap(byteArray: ByteArray): Bitmap {
        val bitmap = BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
        return bitmap
    }


    /**
     * cropBitmap(original: Bitmap, cropRect: Rect): Bitmap
     *      - original 이미지를 cropRect에 맞게 잘르 이미지를 만들어 반환
     */
    private fun cropBitmap(original: Bitmap, cropRect: Rect): Bitmap {

        var width = (cropRect.right - cropRect.left)
        var height = cropRect.bottom - cropRect.top
        var startX = cropRect.left
        var startY = cropRect.top
        if (startX < 0)
            startX = 0
        else if (startX+width > original.width)
            width = original.width-startX
        if (startY < 0)
            startY = 0
        else if(startY+height > original.height)
            height = original.height-startY

        val result = Bitmap.createBitmap(
            original
            , startX         // X 시작위치
            , startY         // Y 시작위치
            , width          // 넓이
            , height         // 높이
        )
        if (result != original) {
            original.recycle()
        }
        return result
    }

    /**
     * overlayBitmap(original: Bitmap, add: Bitmap, optimizationX:Int, optimizationY:Int): Bitmap
     *      - original 이미지에 add 이미지를 (x, y) 좌표에 추가,
     *        만들어진 bitmap을 반환
     */
    fun overlayBitmap(original: Bitmap, add: Bitmap, x:Int, y:Int): Bitmap {

        var startX = x
        var startY = y

        if (startX < 0) {
            startX = 0
        }
        if (startY < 0) {
            startY = 0
        }

        //결과값 저장을 위한 Bitmap
        val resultOverlayBmp = Bitmap.createBitmap(
            original.width, original.height, original.config
        )

        //캔버스를 통해 비트맵을 겹치기한다.
        val canvas = Canvas(resultOverlayBmp)
        canvas.drawBitmap(original, Matrix(), null)
        canvas.drawBitmap(add, startX.toFloat(), startY.toFloat(), null)

        return resultOverlayBmp
    }

    /**
     * drawDetectionResult(bitmap: Bitmap, detectionResults: ArrayList<Face>, customColor: Int) : Bitmap
     * drawDetectionResult(bitmap: Bitmap, detectionResults: List<DetectionResult>, customColor: Int) : Bitmap
     *      - detection 결과를 통해 해당 detection을 이미지 위에 그린 후 bitmap 결과를 받환
     *          ( detectionResult가 ArrayList<Face>인 경우는 얼굴 분석 결과를 그림
     *                              List<DetectionResult>인 경우는 객체 분석 결과를 그림 )
     */
    fun drawDetectionResult(
        bitmap: Bitmap,
        detectionResults: ArrayList<Face>,
        customColor: Int
    ): Bitmap {
        val outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(outputBitmap)
        val pen = Paint()
        pen.textAlign = Paint.Align.LEFT

        detectionResults.forEach {
            // draw bounding box
            pen.color = customColor
            pen.strokeWidth = 8F
            pen.style = Paint.Style.STROKE
            val box = it.boundingBox
            canvas.drawRect(box, pen)
        }
        return outputBitmap
    }

}