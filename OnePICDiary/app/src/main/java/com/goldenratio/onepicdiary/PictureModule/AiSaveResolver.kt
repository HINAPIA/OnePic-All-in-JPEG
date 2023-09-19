package com.goldenratio.onepicdiary.PictureModule

import android.util.Log
import com.goldenratio.onepic.AllinJPEGModule.Content.Picture
import com.goldenratio.onepic.PictureModule.AiContainer
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class AiSaveResolver(_aiContainer : AiContainer) {
    val aiContainer : AiContainer

    init {
        aiContainer = _aiContainer
    }


    fun createSingleJpegByteArray(picture: Picture) : ByteArray{
        val byteBuffer = ByteArrayOutputStream()
        byteBuffer.write(picture._mataData, 0, picture._mataData!!.size)
        byteBuffer.write(picture._pictureByteArray)
        byteBuffer.write(0xff)
        byteBuffer.write(0xd9)

        return byteBuffer.toByteArray()
    }

    /**
     * TODO
     *
     * @param isBurstMode
     * @return
     */
    suspend fun AiContainerToBytes(isBurstMode : Boolean): ByteArray = coroutineScope {
        var jpegByteArray : ByteArray

        // 일반 JPEG으로 저장
        if (!aiContainer.isAllinJPEG) {
            Log.d("save_test", "1. 표준 JPEG으로 저장")
            jpegByteArray = createJpegByteArray()
        }
        // All-in JPEG format으로 저장
        else {
            Log.d("save_test", "2. all in jpeg으로 저장")
            jpegByteArray = createAllinJpegByteArray(isBurstMode)

        }
        return@coroutineScope jpegByteArray
    }

    /**
     * TODO 표준 Jpeg 형식의 byteArray를 생성하여 리턴
     *
     * @return 표준 JPEG 형식의 byteArray [Header + Frame]
     */
    fun createJpegByteArray() : ByteArray{
        val byteBuffer = ByteArrayOutputStream()
        var picture = aiContainer.imageContent.getPictureAtIndex(0)

        // Header 부분 write
        byteBuffer.write(picture!!._mataData, 0, picture!!._mataData!!.size)
//        val jpegHeader = aiContainer.imageContent.jpegHeader
//        byteBuffer.write(jpegHeader, 0, jpegHeader.size)

        // Frame 부분 write
        byteBuffer.write(picture!!._pictureByteArray)

        // EOI 마커 write
        byteBuffer.write(0xff)
        byteBuffer.write(0xd9)

        return byteBuffer.toByteArray()
    }


    /**
     * TODO All-in Jpeg 형식의 byteArray를 생성하여 리턴
     *
     * @return All-in 형식의 byteArray [Header(+APP3) + Frame + extended Data]
     */
    suspend fun createAllinJpegByteArray(isBurstMode : Boolean) : ByteArray{

        // All-in JPEG's Header
        val headerByteArray = createAllinJpegHeaderByteArray()
        // All-in JPEG's Body
        val bodyByteArray = createAllinJpegBodyByteArray(isBurstMode)

        return  headerByteArray + bodyByteArray
    }

    /**
     * TODO All-in Jpeg 형식의 Header 데이터를 생성 후 byteArray로 리턴
     *
     * @return All-in Jpeg 형식의 header byteArray [Header + Frame]
     */
    suspend fun createAllinJpegHeaderByteArray() : ByteArray{
        val byteBuffer = ByteArrayOutputStream()
        val curJpegHeader = aiContainer.imageContent.jpegHeader

        // APP3 extension data 생성
        val App3ExtensionData = getApp3ExtensionByteData()

        // APP3 삽입 위치 찾기 - APP1 뒤에
        val (lastAppMarkerOffset, lastAppMarkerDataLength) = findInsertionApp3LocationAndLength(curJpegHeader)

        // Start Header Write...
        if(lastAppMarkerOffset == 0) {
            byteBuffer.write(curJpegHeader, 0, 2)
        }
        else {
            byteBuffer.write(curJpegHeader, 0, lastAppMarkerOffset + lastAppMarkerDataLength + 2)
        }
        byteBuffer.write(App3ExtensionData)
        //나머지 Header 데이터 write
        byteBuffer.write(
            curJpegHeader,
            lastAppMarkerOffset + 2 + lastAppMarkerDataLength,
            curJpegHeader.size - (lastAppMarkerOffset + 2 +lastAppMarkerDataLength)
        )
        Log.d("save_test", "작성한 APP3 크기 : ${App3ExtensionData.size}")
        Log.d("save_test", "나머지 메타 데이터 크기 : ${curJpegHeader.size - (lastAppMarkerOffset + lastAppMarkerDataLength + 4)}")
        Log.d("save_test", "총 작성한 메타 데이터 크기 : ${byteBuffer.size()}")

        return byteBuffer.toByteArray()
    }

    fun createAllinJpegBodyByteArray(isBurstMode : Boolean) : ByteArray{
        val byteBuffer = ByteArrayOutputStream()
        // Imgaes Data write
        for (i in 0..aiContainer.imageContent.pictureCount - 1) {
            var picture = aiContainer.imageContent.getPictureAtIndex(i)
            //byteBuffer.write(/* b = */ picture!!._pictureByteArray)
            if (i == 0) {
                byteBuffer.write(/* b = */ picture!!._pictureByteArray)
                //EOI 작성
                byteBuffer.write(0xff)
                byteBuffer.write(0xd9)

            } else{
                // XOI 마커
                byteBuffer.write(0xff)
                byteBuffer.write(0x10)
                byteBuffer.write(/* b = */ picture!!._mataData)
                byteBuffer.write(/* b = */ picture!!._pictureByteArray)
            }
        }

        // Text Data write
        for (i in 0..aiContainer.textContent.textList.size - 1) {
            var text = aiContainer.textContent.getTextAtIndex(i)

            // XOT 마커
            byteBuffer.write(0xff)
            byteBuffer.write(0x20)

            for (i in 0 until text!!.data.length) {
                val charValue = text!!.data[i].toInt()

                // 2개의 바이트로 쪼개기
                var tempByteBuffer : ByteBuffer = ByteBuffer.allocate(2)
                tempByteBuffer.put((charValue shr 8 and 0xFF).toByte())
                tempByteBuffer.put((charValue and 0xFF).toByte())

                byteBuffer.write(tempByteBuffer.array())
            }
        }
        // Audio Write
        if (aiContainer.audioContent.audio != null) {
            var audio = aiContainer.audioContent.audio
            // XOI 마커
            byteBuffer.write(0xff)
            byteBuffer.write(0x30)
            byteBuffer.write(/* b = */ audio!!._audioByteArray)
        }
        return byteBuffer.toByteArray()
    }



    fun findInsertionApp3LocationAndLength(jpegMetaData : ByteArray) : Pair<Int, Int>{
        // APP3 삽입 위치 찾기 ( APP0, APP1, APP2 중 가장 늦게 나온 마커 뒤에)
        var pos = 2
        var findMarker : Boolean = false
        var lastAppMarkerOffset = 0
        var lastAppMarkerDataLength = 0
        val SOFMarkerPosList = aiContainer.imageContent.getSOFMarkerPosList(jpegMetaData)

        while (pos < jpegMetaData.size -1) {

            if (jpegMetaData[pos] == 0xFF.toByte() && jpegMetaData[pos + 1] == 0xE1.toByte()){
                lastAppMarkerOffset = pos
                findMarker = true; break;
                Log.d("save_test", "APP1 find :  ${lastAppMarkerOffset}")
            }
            pos++
        }

        if(findMarker){
            lastAppMarkerDataLength = ((jpegMetaData[lastAppMarkerOffset + 2].toInt() and 0xFF) shl 8) or
                    ((jpegMetaData[lastAppMarkerOffset + 3].toInt() and 0xFF) shl 0)

            // APPn 데이터 크기가 없을 때
            if(lastAppMarkerOffset + 2 + lastAppMarkerDataLength > jpegMetaData.size || lastAppMarkerDataLength == 0){
                // 마커의 크기만 지정
                lastAppMarkerDataLength = -2
            }
        } else{
            lastAppMarkerDataLength = 0
        }
        return Pair(lastAppMarkerOffset, lastAppMarkerDataLength)
    }

    suspend fun getApp3ExtensionByteData() : ByteArray  = withContext(Dispatchers.Default) {

        // APP3 info 클래스 데이터 초기화
        aiContainer.settingHeaderInfo()
        return@withContext aiContainer.convertHeaderToBinaryData()
    }

}