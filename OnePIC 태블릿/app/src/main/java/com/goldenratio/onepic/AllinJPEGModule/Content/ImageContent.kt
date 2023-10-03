package com.goldenratio.onepic.AllinJPEGModule.Content

import android.graphics.Bitmap
import android.util.Log
import com.goldenratio.onepic.ImageToolModule
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

    lateinit var jpegHeader : ByteArray
    lateinit var mainPicture : Picture
    private var mainBitmap: Bitmap? = null
    private var bitmapList: ArrayList<Bitmap> = arrayListOf()
    private var attributeBitmapList: ArrayList<Bitmap> = arrayListOf()
    private var bitmapListAttribute : ContentAttribute? = null
    private var checkBitmapList = false
    var checkPictureList = false
    var checkMain = false

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
        bitmapList.clear()
        mainBitmap = null
        attributeBitmapList.clear()
        bitmapListAttribute = null
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
     * TODO ImageContent 갱신 - 카메라 찍을 때 호출되는 함수
     *
     * @param byteArrayList
     * @param contentAttribute
     * @return
     */
    suspend fun setContent(byteArrayList: ArrayList<ByteArray>, contentAttribute : ContentAttribute) : Boolean = withContext(Dispatchers.Default){
        init()
        var sum = 0
        // 메타 데이터 분리
        jpegHeader = extractMetaDataFromFirstImage(byteArrayList.get(0),contentAttribute)
        for(i in 0 until byteArrayList.size){
            val singleJpegBytes = byteArrayList.get(i)
            sum += singleJpegBytes.size

            // 메타 데이터 분리
            val frameStartPos = getFrameStartPos(singleJpegBytes)
            val metaData = singleJpegBytes.copyOfRange(0, frameStartPos)

            // frame 분리
            var frameBytes = async {
                extractFrame(byteArrayList.get(i))
            }
             // Picture 객체 생성
            var picture = Picture(contentAttribute, metaData, frameBytes.await())
            picture.waitForByteArrayInitialized()
            insertPicture(picture)

            if(i == 0){
                mainPicture = picture
                checkMain = true
            }
        }
        Log.d("성능 평가", "전체 byte : ${sum}")
        checkPictureList = true
        return@withContext true
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
        var picture = Picture(ContentAttribute.basic, jpegHeader, frameBytes)
        picture.waitForByteArrayInitialized()
        insertPicture(picture)
        mainPicture = pictureList[0]
        checkPictureList = true
        checkMain = true
    }

    /**
        TODO picture들의 bitmap 데이터 초기화
     */
    fun resetBitmap() {
        checkBitmapList = false
        mainBitmap = null
        bitmapList.clear()
        bitmapListAttribute = null
        attributeBitmapList.clear()
        isSetBitmapListStart = false
        mainBitmap?.recycle()
    }

    fun setMainBitmap(bitmap: Bitmap?) {
        mainBitmap = bitmap
    }

    /**
     * TODO 매개변수 Picture의 APP1 데이터를 합쳐 온전한 JPEG 구조의 바이너리 데이터 리턴
     */
//    fun getChagedJpegBytes(picture: Picture) : ByteArray{
//        var newJpegMetaData : ByteArray? = null
//        while(!checkPictureList) { }
//        // 메타 데이터 변경
////        if(picture._mataData == null || picture._mataData!!.size <= 0)
////            newJpegMetaData = jpegHeader
////        else
////            newJpegMetaData = changeAPP1MetaData(picture._mataData!!)
//
//        // main 사진은 수정된 사진이 아니므로 MetaData를 수정하지 않는다
//        var buffer = ByteBuffer.allocate(picture._mataData!!.size + picture.imageSize+2)
//        buffer.put(picture._mataData)
//        buffer.put(picture._pictureByteArray)
//        buffer.put("ff".toInt(16).toByte())
//        buffer.put("d9".toInt(16).toByte())
//        return buffer.array()
//    }

    /**
     *  TODO metaData와 Picture의 byteArray(frmae)을 붙여서 완전한 JPEG파일의 Bytes를 리턴하는 함수
     */
    fun getJpegBytes(picture : Picture) : ByteArray{
        Log.d("AiJPEG", "getJpegBytes : 호출")
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
     * TODO 기존 metadata의 APP1 데이터를 new App1Data로 교체 후 변경 된 데이터 리턴
     */
//    fun changeAPP1MetaData(newApp1Data : ByteArray) : ByteArray{
//        var pos = 0
//        var app1DataSize = 0
//        var app1StartPos = 0
//        var findAPP1 = false
//        val byteBuffer = ByteArrayOutputStream()
//
//        while (pos < jpegHeader.size) {
//            // APP1 마커 위치 찾기
//            if (jpegHeader[pos] == 0xFF.toByte() && jpegHeader[pos + 1] == 0xE1.toByte()) {
//                app1DataSize = ((jpegHeader[pos + 2].toInt() and 0xFF) shl 8) or
//                        ((jpegHeader[pos + 3].toInt() and 0xFF) shl 0)
//                app1StartPos = pos
//                findAPP1 = true
//                break
//            }
//            pos++
//        }
//
//        if(findAPP1){
//            // 기존 APP1 segment를 newApp1Data로 교체 후 리턴
//            byteBuffer.write(jpegHeader, 0, app1StartPos)
//            byteBuffer.write(newApp1Data)
//            byteBuffer.write(
//                jpegHeader,
//                app1StartPos + app1DataSize + 2,
//                jpegHeader.size - (app1StartPos + app1DataSize +2 )
//            )
//            return byteBuffer.toByteArray()
//        }
//        else{
//            // 교체 안함
//            return jpegHeader
//        }
//    }


    /**
        TODO JPEG 파일의 데이터에서 metaData 부분을 찾아 리턴 하는 함수
     */
    fun extractMetaDataFromFirstImage(bytes: ByteArray, attribute: ContentAttribute) : ByteArray {
        Log.d("AiJPEG", "extractJpegMeta =============================")
        var metaDataEndPos = getFrameStartPos(bytes)
       // var SOFList = getSOFMarkerPosList(bytes)

//        // 사진의 속성이 edited, magic이면 JFIF가 나오기 전까지를 메타데이터로 - 비트맵으로 변환하기 때문에 APP0 마커가 반드시 존재
//        if (attribute == ContentAttribute.edited || attribute == ContentAttribute.magic) {
//            APP0MarkerList = findAPP0Makers(bytes)
//            if(APP0MarkerList.size > 0){
//                isFindStartMarker = true
//                metaDataEndPos = APP0MarkerList[APP0MarkerList.size -1]
//            }
//        }
//
//        // 위에서 2번째 JFIF를 못찾았거나 edited, magic속성이 아닐 때
//        if(!isFindStartMarker) {
//            // 마지막 SOF가 나오기 전 까지 메타 데이터로
//            if(SOFList.size > 0){
//                metaDataEndPos = SOFList[SOFList.size -1]
//            }
//            else {
//                Log.d("AiJPEG", "[meta]extract metadata : SOF가 존재하지 않음")
//                return ByteArray(0)
//            }
//        }

        // Ai JPEG Format 인지 체크
        val (APP3StartIndx, APP3DataLength) = findAiformat(bytes)
        Log.d("AiJPEG", "[meta]APP3StartIndx : ${APP3StartIndx}, APP3DataLength : ${APP3DataLength}" )
        // write
        var resultByte: ByteArray
        val byteBuffer = ByteArrayOutputStream()

        //  Ai JPEG Format 일 때
        if (APP3StartIndx > 0) {
            //  APP3 (Ai jpeg) 영역을 제외하고 metadata write
            Log.d("AiJPEG", "[meta]extract_metadata : MC 포맷이여서 APP3 메타 데이터 뺴고 저장")
            byteBuffer.write(bytes, 0, APP3StartIndx)
            byteBuffer.write(
                bytes,
                APP3StartIndx + APP3DataLength ,
                metaDataEndPos - (APP3StartIndx + APP3DataLength )
            )
            resultByte = byteBuffer.toByteArray()

        //  Ai JPEG Format이 아닐 때
        } else {
            Log.d("AiJPEG", "[meta]extract_metadata : 일반 JEPG처럼 저장 pos : ${metaDataEndPos}")
            // SOF 전까지 추출
            resultByte = bytes.copyOfRange(0, metaDataEndPos)

        }
        Log.d("AiJPEG", "[meta] 추출한 메타데이터 사이즈 ${resultByte.size}")
        return resultByte
    }

//    /**
//     * TODO JPEG 데이터 중 APP1 세그먼트를 찾고 해당 부분을 추출하여 리턴
//     */
//    fun extractAPP1(allBytes : ByteArray) : ByteArray {
//        var pos = 0
//        var app1StartPos = 0
//        var app1DataSize = 0
//        var findAPP1 = false
//        val byteBuffer = ByteArrayOutputStream()
//
//        // APP1 마커 위치와 APP1 data size 찾기
//        while(pos < allBytes.size - 1) {
//            // APP1 마커 찾음
//            if (allBytes[pos] == 0xFF.toByte() && allBytes[pos + 1] == 0xE1.toByte()) {
//                findAPP1 = true
//                app1StartPos = pos
//                app1DataSize = ((allBytes[pos + 2].toInt() and 0xFF) shl 8) or
//                        ((allBytes[pos + 3].toInt() and 0xFF) shl 0)
//                break
//            }
//            pos++
//        }
//
//        if(findAPP1){
//            Log.d("AiJPEG", "app1StartPos : ${app1StartPos}, app1DataSize : ${app1DataSize}")
//            byteBuffer.write(allBytes, app1StartPos, app1DataSize + 2)
//        }
//        else {
//            return ByteArray(0)
//        }
//
//        return byteBuffer.toByteArray()
//    }
//
//    fun extractSOI(jpegBytes: ByteArray): ByteArray {
//        return jpegBytes.copyOfRange(2, jpegBytes.size)
//    }


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
            Log.d("AiJPEG", "extract frame : SOF 찾음 ${startIndex}")
        }
        else {
            Log.d("AiJPEG", "extract frame : SOF가 존재하지 않음")
            return 0
        }
//        if (attribute == ContentAttribute.edited || attribute == ContentAttribute.magic) {
//            // 2번째 JFIF가 나오기 전 까지가 메타 데이터, Frame 보다 앞
//            APP0MarkerList = findAPP0Makers(jpegBytes)
//            if (APP0MarkerList.size > 0) {
//                startIndex = APP0MarkerList.last()
//                Log.d("AiJPEG", "extract frame : JFIF 찾음 ${startIndex}")
//            }
//        }
//        else{
//            // SOF가 나온 위치부터 프레임으로 추출
//            SOFList = getSOFMarkerPosList(jpegBytes)
//            if(SOFList.size > 0){
//                startIndex = SOFList.last()
//                Log.d("AiJPEG", "extract frame : SOF 찾음 ${startIndex}")
//            }
//            else {
//                Log.d("AiJPEG", "extract frame : SOF가 존재하지 않음")
//                return 0
//            }
//        }
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

//    /*
//        TODO JPEG 데이터의 APP0 마커 위치를 찾아 리턴
//     */
//    fun findAPP0Makers (jpegBytes: ByteArray) : ArrayList<Int> {
//        var JFIF_startOffset = 0
//        var JFIFList : ArrayList<Int> = arrayListOf()
//        // 속성이 modified, magicPicture 가 아니면 2번째 JFIF(비트맵의 추가된 메타데이터)가 나오기 전까지 떼서 이용
//        while (JFIF_startOffset < jpegBytes.size - 1) {
//            if (jpegBytes[JFIF_startOffset] == 0xFF.toByte() && jpegBytes[JFIF_startOffset + 1] == 0xE0.toByte()) {
//                JFIFList.add(JFIF_startOffset)
//                Log.d("AiJPEG", "extract metadata :  JIFI찾음 - ${JFIF_startOffset}")
//            }
//            JFIF_startOffset++
//        }
//        return JFIFList
//    }


    /*
       TODO JPEG 데이터의 EOI 마커 위치를 찾아 리턴
    */
    fun getEOIMarekrPosList(jpegBytes: ByteArray) : ArrayList<Int>{
        var EOIStartInex = 0
        var EOIList : ArrayList<Int> = arrayListOf()
        // SOF 시작 offset 찾기
        while (EOIStartInex < jpegBytes.size- 1) {
            if (jpegBytes[EOIStartInex] == 0xFF.toByte() && jpegBytes[EOIStartInex+1] == 0xD9.toByte()) {
                EOIList.add(EOIStartInex)
                Log.d("save_test", "EOIStartInex LIST[${EOIList.size}] ${EOIStartInex}")
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
                Log.d("save_test", "SOF LIST[${SOFList.size}] ${SOFStartInex}")
            }
            SOFStartInex++

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

    /*
         TODO  picture의 attrubuteType이 인자로 전달된 attribute가 아닌 것만 list로 제작 후 전달
     */
    fun getBitmapList(attribute: ContentAttribute) : ArrayList<Bitmap> {
        if (bitmapListAttribute == null || bitmapListAttribute != attribute) {
            attributeBitmapList.clear()
            bitmapListAttribute = attribute
        }
        if (attributeBitmapList.size == 0) {
            val newBitmapList = arrayListOf<Bitmap>()
            while (!checkBitmapList || !checkPictureList) {
                Log.d("faceblending", "!!!! $checkBitmapList || $checkPictureList")
                Thread.sleep(200)
            }
            for (i in 0 until pictureList.size) {
                if (pictureList[i].contentAttribute != attribute) {
                    Log.d("getPictureList", "index : $i  | pictureList size : ${pictureList.size} " +
                            "| bitmapList size : ${bitmapList.size}" )
                    newBitmapList.add(bitmapList[i])
                }
            }
            attributeBitmapList = newBitmapList
        }
        return attributeBitmapList
    }


    /*
     TODO bitmapList 생성 후 전달
     */
    fun  getBitmapList() : ArrayList<Bitmap> {
        while (!checkBitmapList || !checkPictureList) {
            Log.d("faceblending","!!!! $checkBitmapList || $checkPictureList")
            Thread.sleep(200)
        }
        return bitmapList
    }

    fun addBitmapList( index: Int, bitmap: Bitmap) {
        while (bitmapList.size < index) {
        }

        bitmapList.add(index, bitmap)
        attributeBitmapList.clear()
        bitmapListAttribute = null
    }
    fun addBitmapList( bitmap: Bitmap) {
        bitmapList.add(bitmap)
        attributeBitmapList.clear()
        bitmapListAttribute = null
    }


    fun setBitmapList() {
        isSetBitmapListStart = true
        val newBitmapList = arrayListOf<Bitmap>()

        try {
            while (!checkPictureList) { }
            val pictureListSize = pictureList.size
            val checkFinish = BooleanArray(pictureListSize)

            val exBitmap = ImageToolModule().byteArrayToBitmap(getJpegBytes(pictureList[0]))
            for (i in 0 until pictureListSize) {
                checkFinish[i] = false
                newBitmapList.add(exBitmap)
            }
            for (i in 0 until pictureListSize) {
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        Log.d("faceBlending", "coroutine in pictureListSize : $pictureListSize")
                        val bitmap =
                            ImageToolModule().byteArrayToBitmap(getJpegBytes(pictureList[i]))

                        newBitmapList[i] = bitmap
                        checkFinish[i] = true

                    } catch (e: IndexOutOfBoundsException) {
                        e.printStackTrace() // 예외 정보 출력
                        Log.d("burst", "error : $pictureListSize")
                        Log.d("burst", e.printStackTrace().toString())
                        bitmapList.clear()
                        checkFinish[i] = true
                    }
                }
            }
            while (!checkFinish.all { it }) {
                if (!isSetBitmapListStart) {
                    // 특정 조건을 만족할 때 함수를 강제로 종료시킴
                    try {
                        throw Exception("Function forcibly terminated")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            bitmapList = newBitmapList
            checkBitmapList = true
            isSetBitmapListStart = false
            Log.d("faceBlending", "getBitmap end!!!")
        } catch (e: IndexOutOfBoundsException) {
            // 예외가 발생한 경우 처리할 코드
            bitmapList.clear()
        }
    }


    /*
        TODO mainBitmap 전달
     */
    fun getMainBitmap() : Bitmap? {
        return try {
            if(mainBitmap == null){
                Log.d("checkPictureList", "!!!!!!!!!!!!!!!!!!! while out")
                mainBitmap = ImageToolModule().byteArrayToBitmap(getJpegBytes(mainPicture))
                Log.d("checkPictureList", "!!!!!!!!!!!!!!!!!!! return main Bitmap")
            }
            mainBitmap
        } catch (e: IOException) {
            // 예외가 발생한 경우 처리할 코드
            e.printStackTrace() // 예외 정보 출력
            null
        }
    }

    fun insertPicture(picture : Picture){
        pictureList.add(picture)
        pictureCount += 1
    }

    fun insertPicture(index : Int, picture : Picture){
        pictureList.add(index, picture)
        pictureCount += 1
        Log.d("error 잡기", "insertPicture pictureCount= ${pictureCount} ")
    }

    fun removePicture(picture: Picture) : Boolean{
        val index = pictureList.indexOf(picture)
        if(index > 0) {
            val result = pictureList.remove(picture)
            pictureCount -= 1
            if(bitmapList.size > index) {
                bitmapList.removeAt(index)
            }
            return result
        }

        return false
    }
    /**
     * PictureList의 index번째 요소를 찾아 반환
     */
    fun getPictureAtIndex(index : Int): Picture? {
        return pictureList.get(index) ?: null
    }
}
