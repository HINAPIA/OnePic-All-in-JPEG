package com.goldenratio.onepic.EditModule

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.goldenratio.onepic.ImageToolModule
import com.goldenratio.onepic.PictureModule.Contents.Picture
import com.goldenratio.onepic.PictureModule.ImageContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MagicPictureModule(val imageContent: ImageContent) {

    /* Magic picture 변수 */
    var boundingBox: ArrayList<ArrayList<Int>> = arrayListOf()
    private var imageToolModule: ImageToolModule

    private var changeFaceStartX = 0
    private var changeFaceStartY = 0

    var pictureList : ArrayList<Picture> = arrayListOf()
    private var bitmapList: ArrayList<Bitmap> = arrayListOf()

    private var mainBitmap: Bitmap

    private var isInit = false

    init {
        while(!imageContent.checkPictureList) {

        }
        imageToolModule = ImageToolModule()
        mainBitmap = imageToolModule.byteArrayToBitmap(imageContent.getJpegBytes(imageContent.mainPicture))
        pictureList = imageContent.pictureList
        isInit = true
    }

    suspend fun magicPictureProcessing(): ArrayList<Bitmap>  =
        suspendCoroutine { result ->
            val overlayImg: ArrayList<Bitmap> = arrayListOf()

            Log.d("faceRewind","while start")
            while (!isInit) {}
            Log.d("faceRewind","while end")

//             val overlayImg: ArrayList<Bitmap> = arrayListOf()
            // rewind 가능한 연속 사진 속성의 picture list 얻음
            pictureList = imageContent.pictureList
            if (bitmapList.size == 0) {
                val newBitmapList = getBitmapList()
                if(newBitmapList!=null) {
                    bitmapList = newBitmapList
                }
            }

            var basicIndex = 0
            var checkEmbedded = false
            for (i in 0 until pictureList.size) {
                if (pictureList[basicIndex].embeddedData?.size!! > 0) {
                    checkEmbedded = true
                    break
                }
                basicIndex++
            }
            if (checkEmbedded) {
                changeFaceStartX = (pictureList[basicIndex].embeddedData?.get(4) ?: Int) as Int
                changeFaceStartY = (pictureList[basicIndex].embeddedData?.get(5) ?: Int) as Int

                val checkFinish = BooleanArray(pictureList.size - basicIndex)
                for (i in basicIndex until pictureList.size) {
                    checkFinish[i - basicIndex] = false
                    pictureList[i].embeddedData?.let { boundingBox.add(it) }
                }

                for (i in basicIndex until pictureList.size) {
                    CoroutineScope(Dispatchers.Default).launch {
                        createOverlayImg(overlayImg, boundingBox[i - basicIndex], i)
                        checkFinish[i - basicIndex] = true
                    }
                }

                while (!checkFinish.all { it }) {
                    // Wait for all tasks to finish
                }
            }
            result.resume(overlayImg)
        }


    private fun createOverlayImg(ovelapBitmap: ArrayList<Bitmap>, rect: ArrayList<Int>, index: Int) {

        // 감지된 모든 boundingBox 출력
        println("=======================================================")

        // bitmap를 자르기
        if(rect.size >= 4 && bitmapList.size > index) {
            val cropImage = imageToolModule.cropBitmap(
                bitmapList[index],
                Rect(rect[0], rect[1], rect[2], rect[3])
            )

            val newImage = imageToolModule.circleCropBitmap(cropImage)
            ovelapBitmap.add(
                imageToolModule.overlayBitmap(mainBitmap, newImage, changeFaceStartX, changeFaceStartY)
            )
        }
    }
    @Synchronized
    fun  getBitmapList() : ArrayList<Bitmap>? {
        Log.d("faceRewind", "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@")
        Log.d("faceRewind", "getBitmapList 호출")

//        checkTransformBitmap = true
        try {
            Log.d("faceRewind", "checkPictureList bitmapList.size ${bitmapList.size}")
            if (bitmapList.size == 0) {
                Log.d("faceRewind", "checkPictureList while start")
                while (!imageContent.checkPictureList) {
//                if(!checkTransformBitmap)
//                    return null
                }
                val pictureListSize = pictureList.size
                Log.d("faceRewind", "pictureListSize : $pictureListSize")
                val checkFinish = BooleanArray(pictureListSize)

                val exBitmap = imageToolModule.byteArrayToBitmap(imageContent.getJpegBytes(pictureList[0]))
                for (i in 0 until pictureListSize) {

                    checkFinish[i] = false
                    bitmapList.add(exBitmap)
                }
                Log.d("faceRewind", "==============================")
                for (i in 0 until pictureListSize) {

                    CoroutineScope(Dispatchers.Default).launch {
                        try {
                            Log.d("faceRewind", "coroutine in pictureListSize : $pictureListSize")
                            val bitmap =
                                imageToolModule.byteArrayToBitmap(imageContent.getJpegBytes(pictureList[i]))
//                    if(checkTransformBitmap) {
                            bitmapList[i] = bitmap
                            checkFinish[i] = true
//                    }
                        } catch (e: IndexOutOfBoundsException) {
                            e.printStackTrace() // 예외 정보 출력
                            Log.d("burst", "error : $pictureListSize")
                            bitmapList.clear()
                            checkFinish[i] = true
                        }
                    }
                }
                while (!checkFinish.all { it }) {

                }
            }
//        checkTransformBitmap = false
            return bitmapList
        }catch (e: IndexOutOfBoundsException) {
            // 예외가 발생한 경우 처리할 코드
            e.printStackTrace() // 예외 정보 출력
            Log.d("burst", "error ")
            bitmapList.clear()
            return null
        }
    }
}