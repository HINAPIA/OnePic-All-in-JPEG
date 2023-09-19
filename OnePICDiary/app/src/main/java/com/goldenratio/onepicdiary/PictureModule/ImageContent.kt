package com.goldenratio.onepic.AllinJPEGModule.Content

import com.goldenratio.onepic.AllinJPEGModule.Contents.ContentAttribute
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer


/**
 * 하나 이상의 Picture(이미지)를 담는 컨테이너
 */
class ImageContent {

    var pictureList : ArrayList<Picture> = arrayListOf()
    var pictureCount = 0
    var orientation = 0

    lateinit var jpegHeader : ByteArray
    lateinit var mainPicture : Picture
    private var checkBitmapList = false
    var checkPictureList = false
    var checkMain = false
//    private var checkTransformBitmap = false
//    private var checkTransformAttributeBitmap = false
//    private var checkTransformMain = false

    var checkMagicCreated = false
    var checkBlending = false
    var checkAdded = false
    var checkMainChanged = false
    var checkEditChanged = false

    var isSetBitmapListStart = false

    constructor()

    fun init() {
        setCheckAttribute()
        checkBitmapList = false
        checkPictureList = false
        checkMain = false
        pictureList.clear()
        pictureCount = 0
        isSetBitmapListStart = false

    }

    fun setCheckAttribute() {
        checkMagicCreated = false
        checkBlending = false
        checkAdded = false
        checkMainChanged = false
        checkEditChanged = false
    }



    /**
        TODO   ImageContent 리셋 후 초기화 - 파일을 parsing할 때 ImageContent를 생성
     */
    fun setContent(_pictureList : ArrayList<Picture>){
        init()
        //isAllinJPEG = ture
        // frame만 있는 pictureList
        pictureList = _pictureList
        pictureCount = _pictureList.size
        mainPicture = pictureList.get(0)
        checkPictureList = true
        checkMain = true
    }

    /**
        TODO ImageContent 리셋 후 초기화 - 파일을 parsing할 때 일반 JPEG 생성
     */
    fun setBasicContent(singleJpegBytes: ByteArray){
        init()
        //jpegHeader = extractJpegMeta(sourceByteArray, ContentAttribute.basic)
        // 메타 데이터 분리
        val frameStartPos = getFrameStartPos(singleJpegBytes)
        jpegHeader = singleJpegBytes.copyOfRange(0, frameStartPos)

        var frameBytes : ByteArray = extractFrame(singleJpegBytes)
        // Picture 객체 생성
        var picture = Picture(ContentAttribute.Basic, jpegHeader, frameBytes)
        picture.waitForByteArrayInitialized()
        insertPicture(picture)
        mainPicture = pictureList[0]
        checkPictureList = true
        checkMain = true
    }




    /**
     *  TODO metaData와 Picture의 byteArray(frmae)을 붙여서 완전한 JPEG파일의 Bytes를 리턴하는 함수
     */
    // TODO("APP1 삭제 후 변경 필요")
    fun getJpegBytes(picture : Picture) : ByteArray{
        while(!checkPictureList) { }
        // main 사진은 수정된 사진이 아니므로 MetaData를 수정하지 않는다
        var buffer = ByteBuffer.allocate(picture._mataData!!.size + picture.imageSize+2)
        buffer.put(picture._mataData)
        buffer.put(picture._pictureByteArray)
        buffer.put("ff".toInt(16).toByte())
        buffer.put("d9".toInt(16).toByte())
        return buffer.array()
    }




    /**
        TODO JPEG 파일의 데이터에서 metaData 부분을 찾아 리턴 하는 함수
     */
    fun extractMetaDataFromFirstImage(bytes: ByteArray, attribute: ContentAttribute) : ByteArray {
        var metaDataEndPos = 0
        var SOFList : ArrayList<Int> = arrayListOf()
        SOFList = getSOFMarkerPosList(bytes)
        metaDataEndPos = getFrameStartPos(bytes)

        // Ai JPEG Format 인지 체크
        val (APP3StartIndx, APP3DataLength) = findAiformat(bytes)
        // write
        var resultByte: ByteArray
        val byteBuffer = ByteArrayOutputStream()

        //  Ai JPEG Format 일 때
        if (APP3StartIndx > 0) {
            //  APP3 (Ai jpeg) 영역을 제외하고 metadata write
            byteBuffer.write(bytes, 0, APP3StartIndx)
            byteBuffer.write(
                bytes,
                APP3StartIndx + APP3DataLength ,
                SOFList.last() - (APP3StartIndx + APP3DataLength )
            )
            resultByte = byteBuffer.toByteArray()

        //  Ai JPEG Format이 아닐 때
        } else {
             // SOF 전까지 추출
            resultByte = bytes.copyOfRange(0, metaDataEndPos)

        }
        return resultByte
    }


    /**
     * TODO Frame(SOF 시작 or 2 번째 JFIF) 시작 위치를 리턴
     *
     * @param jpegBytes
     * @param attribute
     * @return
     */
    //fun getFrameStartPos(jpegBytes: ByteArray, attribute: ContentAttribute)
    fun getFrameStartPos(jpegBytes: ByteArray) : Int{
        var startIndex = 0

        var SOFList : ArrayList<Int>
        var APP0MarkerList : ArrayList<Int>

        // Frame Start pos 찾기
        // SOF가 나온 위치부터 프레임으로 추출
        SOFList = getSOFMarkerPosList(jpegBytes)
        if(SOFList.size > 0){
            startIndex = SOFList.last()
            }
        else {
              return 0
        }
        return startIndex
    }


    /**
     * TODO JPEG 파일 데이터의 프레임(SOF ~EOI 전) 데이터를 찾아 ByteArray에 담아 리턴
     */
    fun extractFrame(jpegBytes: ByteArray): ByteArray {
        var pos = 0
        var endIndex = jpegBytes.size

        // Frame Start pos 찾기
        val frameStartPos = getFrameStartPos(jpegBytes)

        // Frame end Pos 찾기
        pos = jpegBytes.size-2
        while (pos > 0) {
            if (jpegBytes[pos] == 0xFF.toByte() && jpegBytes[pos + 1] == 0xD9.toByte()) {
                endIndex = pos
                break
            }
            pos--
        }

        // 프레임 추출
        val frameBytes = jpegBytes.copyOfRange(frameStartPos, endIndex)
        return frameBytes
    }

    /*
       TODO JPEG 데이터의 EOI 마커 위치를 찾아 리턴
    */
    fun getEOIMarekrPosList(jpegBytes: ByteArray) : ArrayList<Int>{
        var EOIStartInex = 0
        var EOIList : ArrayList<Int> = arrayListOf()
        // SOF 시작 offset 찾기
        while (EOIStartInex < jpegBytes.size- 1) {
            var countFindingEOI = 0
            if (jpegBytes[EOIStartInex] == 0xFF.toByte() && jpegBytes[EOIStartInex+1] == 0xD9.toByte()) {
                EOIList.add(EOIStartInex)
            }
            EOIStartInex++
        }
        return EOIList
    }

    /*
       TODO JPEG 데이터의 SOF 마커들 위치를 찾아 리스트로 리턴
    */
    fun getSOFMarkerPosList (jpegBytes: ByteArray) : ArrayList<Int> {
        val EOIPosList = getEOIMarekrPosList(jpegBytes)

        var SOFStartInex = 0
        var SOFList : ArrayList<Int> = arrayListOf()
        // SOF 시작 offset 찾기
        while (SOFStartInex < jpegBytes.size/2 - 1) {
            var countFindingEOI = 0
            if (jpegBytes[SOFStartInex] == 0xFF.toByte() && jpegBytes[SOFStartInex+1] == 0xC0.toByte()) {
                SOFList.add(SOFStartInex)
            }
            SOFStartInex++

            //
            if(EOIPosList.size > 0){
                if (SOFStartInex == EOIPosList.last())
                    break
            }

        }
        return SOFList
    }


    /*
        TODO All-in 포맷인지 식별후 APP3 segment 시작 위치와 크기 리턴
     */
    fun findAiformat(jpegBytes: ByteArray) : Pair<Int,Int>{
        var app3StartIndex = 0
        var app3DataLength = 0
        // MC Format인지 확인 - MC Format일 경우 APP3 데이터 빼고 set
        while (app3StartIndex < jpegBytes.size - 1) {
            // APP3 마커가 있는 경우
            if (jpegBytes[app3StartIndex] == 0xFF.toByte() && jpegBytes[app3StartIndex + 1] == 0xE3.toByte()) {
                //MC Format인지 확인
                if (jpegBytes[app3StartIndex + 4] == 0x4D.toByte() && jpegBytes[app3StartIndex + 5] == 0x43.toByte()
                    && jpegBytes[app3StartIndex + 6] == 0x46.toByte() ||
                    jpegBytes[app3StartIndex+4] == 0x41.toByte() &&  jpegBytes[app3StartIndex+5] == 0x69.toByte()
                    && jpegBytes[app3StartIndex+6] == 0x46.toByte()
                ) {
                    //app3DataLength = byteArraytoInt(jpegBytes, app3StartIndex +2)
                    app3DataLength = ((jpegBytes[app3StartIndex +2].toInt() and 0xFF) shl 8) or
                        ((jpegBytes[app3StartIndex +3].toInt() and 0xFF) shl 0)
                    break
                }
            }
            app3StartIndex++
        }
        if(app3StartIndex == jpegBytes.size - 1) app3StartIndex = 0
        return Pair(app3StartIndex, app3DataLength)
    }

    fun checkAttribute(attribute: ContentAttribute): Boolean {
        for(i in 0 until pictureList.size){
            if(pictureList[i].contentAttribute == attribute)
                return true
        }
        return false
    }

    fun insertPicture(picture : Picture){
        pictureList.add(picture)
        pictureCount += 1
    }

    /**
     * PictureList의 index번째 요소를 찾아 반환
     */
    fun getPictureAtIndex(index : Int): Picture? {
        return pictureList.get(index) ?: null
    }
}
