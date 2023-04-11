package com.example.onepic.PictureModule

import android.util.Log
import com.example.onepic.PictureModule.Contents.ContentAttribute
import com.example.onepic.PictureModule.Contents.Picture
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer


// 하나 이상의 Picture(이미지)를 담는 컨테이너
class ImageContent {
    var jpegConstant : JpegConstant = JpegConstant()
    var markerHashMap: HashMap<Int?, String?> = jpegConstant.nameHashMap
    var pictureList : ArrayList<Picture> = arrayListOf()
    var pictureCount = 0
    lateinit var jpegMetaData : ByteArray

    lateinit var mainPicture : Picture
    fun init() {

        pictureList.clear()
        pictureCount = 0
        //jpegMetaData = ByteArray(0)
    }
    // ImageContent 리셋 후 초기화 - 카메라 찍을 때 호출되는 함수
    fun setContent(byteArrayList: ArrayList<ByteArray>, contentAttribute : ContentAttribute){
        init()
        // 메타 데이터 분리
        jpegMetaData = extractJpegMeta(byteArrayList.get(0))
        for(i in 0..byteArrayList.size-1){
            // frame 분리
            var frameBytes : ByteArray = extractFrame(byteArrayList.get(i))
            // Picture 객체 생성
            var picture = Picture( contentAttribute, frameBytes)
            insertPicture(picture)
            if(i == 0){
                mainPicture = picture
            }
        }
    }
    // ImageContent 리셋 후 초기화 - 파일을 parsing할 때 ImageContent를 생성
    fun setContent(_pictureList : ArrayList<Picture>){
        init()
        // frame만 있는 pictureList
        pictureList = _pictureList
        pictureCount = _pictureList.size
        mainPicture = pictureList.get(0)

    }

    // ImageContent 리셋 후 초기화 - 파일을 parsing할 때 일반 JPEG 생성
    fun setBasicContent(sourceByteArray: ByteArray){
        init()
        jpegMetaData = extractJpegMeta(sourceByteArray)
        var frameBytes : ByteArray = extractFrame(sourceByteArray)
        // Picture 객체 생성
        var picture = Picture(ContentAttribute.basic, frameBytes)
        picture.waitForByteArrayInitialized()
        insertPicture(picture)
        mainPicture = pictureList.get(0)
    }
    fun addContent(byteArrayList: ArrayList<ByteArray>, contentAttribute : ContentAttribute){
        for(i in 0..byteArrayList.size-1){
            // Picture 객체 생성
            var picture = Picture(contentAttribute, byteArrayList.get(i))
            insertPicture(picture)
        }
    }
    // PictureList에 Picture를 삽입
    fun insertPicture(picture : Picture){
        pictureList.add(picture)
        pictureCount = pictureCount + 1
    }
    fun insertPicture(index : Int, picture : Picture){
        pictureList.add(index, picture)
        pictureCount = pictureCount + 1
    }

    // PictureList의 index번째 요소를 찾아 반환
    fun getPictureAtIndex(index : Int): Picture? {
        return pictureList.get(index) ?: null
    }

    /**
     * MetaData와 Picture의 byteArray(frmae)을 붙여서 완전한 JEPG파일의 Bytes를 리턴하는 함수
     */
    fun getJpegBytes(picture : Picture) : ByteArray{
        var JFIF_startOffset = 0
        var findCount = 0
        // 속성이 modified가 아니면 2번 째 JFIF가 나오기 전까지 떼서 이용
        if(picture.contentAttribute != ContentAttribute.edited){
            while (JFIF_startOffset < jpegMetaData.size - 1) {
                if (jpegMetaData[JFIF_startOffset] == 0xFF.toByte() && jpegMetaData[JFIF_startOffset + 1] == 0xE0.toByte()) {
                    findCount++
                    if(findCount == 2)
                        break
                }
                JFIF_startOffset++
            }
            // 2번의 JFIF를 찾음
            if (JFIF_startOffset != jpegMetaData.size - 1) {
                // 2번 째 JFIF 전까지 이용
                val buffer: ByteBuffer = ByteBuffer.allocate(JFIF_startOffset + picture.size+2)
                buffer.put(jpegMetaData.copyOfRange(0, JFIF_startOffset))
                buffer.put(picture._pictureByteArray)
                buffer.put("ff".toInt(16).toByte())
                buffer.put("d9".toInt(16).toByte())
                return buffer.array()
            }
        }
        // 속성이 modified이거나 JFIF를 2번 못 찾으면 전체 MetaData이용

        Log.d("test_test", "2개의 JFIF를 찾지 못함")
        val buffer: ByteBuffer = ByteBuffer.allocate(jpegMetaData.size + picture.size+2)
        buffer.put(jpegMetaData)
        buffer.put(picture._pictureByteArray)
        buffer.put("ff".toInt(16).toByte())
        buffer.put("d9".toInt(16).toByte())
        return buffer.array()

       // val buffer: ByteBuffer = ByteBuffer.allocate(jpegMetaData.size + picture.size+2)


    }

//    fun getJpegBytes(frameByteArray : ByteArray) : ByteArray{
//        val buffer: ByteBuffer = ByteBuffer.allocate(jpegMetaData.size + frameByteArray.size+2)
//        buffer.put(jpegMetaData)
//        buffer.put(frameByteArray)
//        buffer.put("ff".toInt(16).toByte())
//        buffer.put("d9".toInt(16).toByte())
//        return buffer.array()
//    }

    fun extractJpegMeta(jpegBytes: ByteArray) : ByteArray{

        var resultByte: ByteArray
        var startIndex = 0
        var isFindStartMarker = false // 시작 마커를 찾았는지 여부
        var pos = 0
        var extractPos = 0
        var app3DataLength = 0
        while (extractPos < jpegBytes.size - 1) {
            // APP3 마커가 있는 경우
            if (jpegBytes[extractPos] == 0xFF.toByte() && jpegBytes[extractPos + 1] == 0xE3.toByte()) {
                //MC Format인지 확인
                if(jpegBytes[extractPos+6] == 0x4D.toByte() &&  jpegBytes[extractPos+7] == 0x43.toByte()
                    && jpegBytes[extractPos+8] == 0x46.toByte()){
                    app3DataLength = ((jpegBytes[extractPos+2].toInt() and 0xFF) shl 8) or
                            ((jpegBytes[extractPos+3].toInt() and 0xFF) shl 0)
                    break
                }

            }
            extractPos++
        }

        while (pos < jpegBytes.size - 1) {
            if (jpegBytes[pos] == 0xFF.toByte() && jpegBytes[pos + 1] == 0xC0.toByte()) {
                isFindStartMarker = true
                break
            }
            pos++
        }
        if (!isFindStartMarker) {
            println("startIndex :${startIndex}")
            Log.d("이미지","Error: SOF가 존재하지 않음")
            return ByteArray(0)
        }
        val byteBuffer = ByteArrayOutputStream()
        if(extractPos != jpegBytes.size-1){
            byteBuffer.write(jpegBytes, 0, extractPos)
            byteBuffer.write(jpegBytes, extractPos + app3DataLength + 2, pos - (extractPos + app3DataLength + 2))
            resultByte = byteBuffer.toByteArray()
        }else{
            // 추출
            resultByte = jpegBytes.copyOfRange(0, pos)
        }

        // start 마커부터 end 마커를 포함한 영역까지 복사해서 resultBytes에 저장
        // System.arraycopy(jpegBytes, startIndex, resultByte, 0, endIndex - startIndex + 2)
        return resultByte
    }

    fun extractSOI(jpegBytes: ByteArray): ByteArray{
        return jpegBytes.copyOfRange(2, jpegBytes.size)
    }
    // 한 파일에서 SOF~EOI 부분의 바이너리 데이터를 찾아 ByteArray에 담아 리턴
     fun extractFrame(jpegBytes: ByteArray): ByteArray {
        Log.d("이미지","extractFrame 시작")
        var n1: Int
        var n2: Int
        var resultByte: ByteArray
        var startIndex = 0
        var endIndex = jpegBytes.size
        var startCount = 0
        var endCount = 0
        var startMax = 1
        val endMax = 1
        var isFindStartMarker = false // 시작 마커를 찾았는지 여부
        var isFindEndMarker = false // 종료 마커를 찾았는지 여부


        for (i in 0 until jpegBytes.size - 1) {
            n1 = Integer.valueOf(jpegBytes[i].toInt())
            if (n1 < 0) {
                n1 += 256
            }
            n2 = Integer.valueOf(jpegBytes[i+1].toInt())
            if (n2 < 0) {
                n2 += 256
            }
            val twoByteToNum = n1 + n2
            if (markerHashMap.containsKey(twoByteToNum) && n1 == 255) {
                if (twoByteToNum == jpegConstant.SOF0_MARKER) {
                    Log.d("이미지","SOF 마커 찾음 : ${i}")
                    startCount++
                    if (startCount == startMax) {
                        startIndex = i
                        isFindStartMarker = true
                        break
                    }
                }
            }
        }
        for(j in jpegBytes.size-2 downTo 0 ){
            n1 = Integer.valueOf(jpegBytes[j].toInt())
            if (n1 < 0) {
                n1 += 256
            }
            n2 = Integer.valueOf(jpegBytes[j+1].toInt())
            if (n2 < 0) {
                n2 += 256
            }
            val twoByteToNum = n1 + n2
            if (markerHashMap.containsKey(twoByteToNum) && n1 == 255){
                if (isFindStartMarker) { // 조건에 부합하는 start 마커를 찾은 후, end 마커 찾기
                    if (twoByteToNum == jpegConstant.EOI_MARKER) {
                        Log.d("이미지","EOI 마커 찾음 : ${j}")
                        endCount++
                        if (endCount == endMax) {
                            endIndex =j
                            isFindEndMarker = true
                            break
                        }
                    }
                }
            }

        }
        if (!isFindStartMarker || !isFindEndMarker) {
            //println("startIndex :${startIndex}")
            //println("endIndex :${endIndex}")
            Log.d("이미지","Error: 찾는 마커가 존재하지 않음")
            //println("Error: 찾는 마커가 존재하지 않음")
            return ByteArray(0)
        }
        // 추출
        //resultByte = ByteArray(endIndex - startIndex)
        resultByte = jpegBytes.copyOfRange(startIndex, endIndex )
        // start 마커부터 end 마커를 포함한 영역까지 복사해서 resultBytes에 저장
       // System.arraycopy(jpegBytes, startIndex, resultByte, 0, endIndex - startIndex )
        return resultByte


    }

}