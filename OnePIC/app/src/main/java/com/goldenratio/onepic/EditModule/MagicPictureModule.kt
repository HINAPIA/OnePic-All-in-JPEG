package com.goldenratio.onepic.EditModule

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.goldenratio.onepic.AllinJPEGModule.Content.ImageContent
import com.goldenratio.onepic.AllinJPEGModule.Content.Picture
import com.goldenratio.onepic.ImageToolModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MagicPictureModule(val imageContent: ImageContent, selectedPicture: Picture) {

    /** Magic picture 변수 **/
    var boundingBox: ArrayList<ArrayList<Int>> = arrayListOf()
    private var imageToolModule: ImageToolModule

    private var changeFaceStartX = 0
    private var changeFaceStartY = 0

    var pictureList : ArrayList<Picture> = arrayListOf()
    private var bitmapList: ArrayList<Bitmap> = arrayListOf()

    private var mainBitmap: Bitmap

    var isInit = false

    /**
     * All-in JPEG의 이미지의 비트맵이 다 제작됬는지 확인하고, 관련 변수를 설정한다.
     */
    init {
        while(!imageContent.checkPictureList) { }
        imageToolModule = ImageToolModule()
        mainBitmap = imageToolModule.byteArrayToBitmap(imageContent.getJpegBytes(selectedPicture))
        pictureList = imageContent.pictureList
        isInit = true
    }

    /**
     * 매직픽처에서 사용되는 이미지들의 App3 메타데이터를 알아내 매직픽처 재생할 때 필요한 overlayBitmap을 제작해 리스트로 반환한다.
     *
     * @return 매직픽처를 재생할 때 필요한 비트맵 리스트
     */
    suspend fun magicPictureProcessing(): ArrayList<Bitmap>  =
        suspendCoroutine { result ->
            val overlayImg: ArrayList<Bitmap> = arrayListOf()

            Log.d("faceBlending","while start")
            while (!isInit) {}
            Log.d("faceBlending","while end")

            // blending 가능한 연속 사진 속성의 picture list 얻음
            pictureList = imageContent.pictureList
            if (bitmapList.size == 0) {
//                val newBitmapList = getBitmapList()
                val newBitmapList = imageContent.getBitmapList()
                if (newBitmapList != null)
                    bitmapList = newBitmapList
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

                while (!checkFinish.all { it }) { }
            }
            result.resume(overlayImg)
        }

    /**
     * 매직픽처를 재생할 때 필요한 비트맵을 제작해 overlayBitmap에 추가한다.
     *
     * 매직픽처를 재생할 때 필요한 비트맵이란, 움직일 첫번째 이미지에 다른 이미지들의 움직일 얼굴들을 합성한 사진이다.
     *
     * @param ovelayBitmap 제작된 비트맵을 추가할 Bitmap 리스트
     * @param rect 비트맵을 자를 위치 정보
     * @param index bitmapList의 현재 이미지의 index
     */
    private fun createOverlayImg(ovelayBitmap: ArrayList<Bitmap>, rect: ArrayList<Int>, index: Int) {

        // 감지된 모든 boundingBox 출력
        println("=======================================================")

        // bitmap를 자르기
        if(rect.size >= 4 && bitmapList.size > index) {
            val cropImage = imageToolModule.cropBitmap(
                bitmapList[index],
                Rect(rect[0], rect[1], rect[2], rect[3])
            )

            val newImage = imageToolModule.circleCropBitmap(cropImage)
            ovelayBitmap.add(
                imageToolModule.overlayBitmap(mainBitmap, newImage, changeFaceStartX, changeFaceStartY)
            )
        }
    }

    @Synchronized
    fun getBitmapList() : ArrayList<Bitmap>? {
       Log.d("faceBlending", "getBitmapList 호출")

        try {
            Log.d("faceBlending", "checkPictureList bitmapList.size ${bitmapList.size}")
            if (bitmapList.size == 0) {
                Log.d("faceBlending", "checkPictureList while start")
                while (!imageContent.checkPictureList) { }
                val pictureListSize = pictureList.size
                Log.d("faceBlending", "pictureListSize : $pictureListSize")
                val checkFinish = BooleanArray(pictureListSize)

                val exBitmap = imageToolModule.byteArrayToBitmap(imageContent.getJpegBytes(pictureList[0]))
                for (i in 0 until pictureListSize) {

                    checkFinish[i] = false
                    bitmapList.add(exBitmap)
                }
                Log.d("faceBlending", "==============================")
                for (i in 0 until pictureListSize) {

                    CoroutineScope(Dispatchers.Default).launch {
                        try {
                            Log.d("faceBlending", "coroutine in pictureListSize : $pictureListSize")
                            val bitmap = imageToolModule.byteArrayToBitmap(imageContent.getJpegBytes(pictureList[i]))
                            bitmapList[i] = bitmap
                            checkFinish[i] = true
                        } catch (e: IndexOutOfBoundsException) {
                            e.printStackTrace() // 예외 정보 출력
                            Log.d("burst", "error : $pictureListSize")
                            bitmapList.clear()
                            checkFinish[i] = true
                        }
                    }
                }
                while (!checkFinish.all { it }) { }
            }
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