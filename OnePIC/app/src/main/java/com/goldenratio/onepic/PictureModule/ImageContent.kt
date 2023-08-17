package com.goldenratio.onepic.PictureModule

import android.graphics.Bitmap
import android.util.Log
import com.goldenratio.onepic.ImageToolModule
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.PictureModule.Contents.ContentAttribute
import com.goldenratio.onepic.PictureModule.Contents.Picture
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

    lateinit var jpegMetaData : ByteArray
    lateinit var mainPicture : Picture
    private var mainBitmap: Bitmap? = null
    private var bitmapList: ArrayList<Bitmap> = arrayListOf()
    private var attributeBitmapList: ArrayList<Bitmap> = arrayListOf()
    private var bitmapListAttribute : ContentAttribute? = null
    private var checkBitmapList = false
    var checkPictureList = false
    var checkMain = false
//    private var checkTransformBitmap = false
//    private var checkTransformAttributeBitmap = false
//    private var checkTransformMain = false

    var checkMagicCreated = false
    var checkRewind = false
    var checkAdded = false
    var checkMainChanged = false
    var checkEditChanged = false

    var isSetBitmapListStart = false

    constructor()

    fun init() {
        checkBitmapList = false
        checkPictureList = false
        checkMain = false
//        checkTransformBitmap = false
//        checkTransformAttributeBitmap = false
//        checkTransformMain = false

        setCheckAttribute()

        pictureList.clear()
        pictureCount = 0
        bitmapList.clear()
        mainBitmap = null
        attributeBitmapList.clear()
        bitmapListAttribute = null
        isSetBitmapListStart = false
        //jpegMetaData = ByteArray(0)

//        CoroutineScope(Dispatchers.Default).launch {
//            setBitmapList()
//        }
    }

    fun setCheckAttribute() {
        checkMagicCreated = false
        checkRewind = false
        checkAdded = false
        checkMainChanged = false
        checkEditChanged = false
    }

    /**
     * ImageContent 초기화 - 카메라 찍을 때 호출되는 함수
     */
    suspend fun setContent(byteArrayList: ArrayList<ByteArray>, contentAttribute : ContentAttribute) : Boolean = withContext(Dispatchers.Default){
        init()
        // 메타 데이터 분리
        jpegMetaData = extractJpegMeta(byteArrayList.get(0),contentAttribute)
        for(i in 0 until byteArrayList.size){
            // APP1 데이터 분리
            var app1Bytes : ByteArray = extractAPP1(byteArrayList.get(i))
            // frame 분리
            var frameBytes = async {
                extractFrame(byteArrayList.get(i),contentAttribute)
            }
            // Picture 객체 생성
            var picture = Picture(contentAttribute, app1Bytes, frameBytes.await())
            picture.waitForByteArrayInitialized()
            insertPicture(picture)
            Log.d("AiJPEG", "setImageContnet: picture[$i] 완성")
            if(i == 0){
                mainPicture = picture
                checkMain = true
            }
        }
        Log.d("AiJPEG", "setImageContnet: 완성 size =${pictureList.size}")
        checkPictureList = true
        return@withContext true
    }

    /**
     *    ImageContent 리셋 후 초기화 - 파일을 parsing할 때 ImageContent를 생성
     */
    fun setContent(_pictureList : ArrayList<Picture>){
        init()
        // frame만 있는 pictureList
        pictureList = _pictureList
        pictureCount = _pictureList.size
        mainPicture = pictureList.get(0)
        checkPictureList = true
        checkMain = true
    }

    fun resetBitmap() {
        mainBitmap = null
        bitmapList.clear()
        bitmapListAttribute = null
        attributeBitmapList.clear()
        isSetBitmapListStart = false
        mainBitmap?.recycle()
//        setBitmapList()
    }

    fun setMainBitmap(bitmap: Bitmap?) {
        mainBitmap = bitmap
    }

    /**
    ImageContent 리셋 후 초기화 - 파일을 parsing할 때 일반 JPEG 생성
     */
    fun setBasicContent(sourceByteArray: ByteArray){
        init()
        jpegMetaData = extractJpegMeta(sourceByteArray, ContentAttribute.basic)
        // APP1 데이터 분리x
        var app1Bytes : ByteArray = extractAPP1(sourceByteArray)
        var frameBytes : ByteArray = extractFrame(sourceByteArray,ContentAttribute.basic)
        // Picture 객체 생성
        var picture = Picture(ContentAttribute.basic, app1Bytes, frameBytes)
        picture.waitForByteArrayInitialized()
        insertPicture(picture)
        mainPicture = pictureList[0]
        checkPictureList = true
        checkMain = true
    }


    fun getChagedJpegBytes(picture: Picture) : ByteArray{
        var newJpegMetaData : ByteArray? = null
        Log.d("AiJPEG", "getChagedJpegBytes : 호출")
        while(!checkPictureList) { }
        // 메타 데이터 변경
        if(picture._app1Segment == null || picture._app1Segment!!.size <= 0)
            newJpegMetaData = jpegMetaData
        else
            newJpegMetaData = chageMetaData(picture._app1Segment!!)
        // byteBuffer.write(newJpegMetaData, 0, newJpegMetaData.size)
        //ar buffer : ByteBuffer = ByteBuffer.allocate(0)
        // main 사진은 수정된 사진이 아니므로 MetaData를 수정하지 않는다
        var buffer = ByteBuffer.allocate(newJpegMetaData.size + picture.imageSize+2)
        buffer.put(newJpegMetaData)
        buffer.put(picture._pictureByteArray)
        buffer.put("ff".toInt(16).toByte())
        buffer.put("d9".toInt(16).toByte())
        return buffer.array()
    }

    /**
     * metaData와 Picture의 byteArray(frmae)을 붙여서 완전한 JPEG파일의 Bytes를 리턴하는 함수
     */
    // TODO("APP1 삭제 후 변경 필요")
    fun getJpegBytes(picture : Picture) : ByteArray{
        Log.d("AiJPEG", "getJpegBytes : 호출")
        while(!checkPictureList) { }
        var buffer : ByteBuffer = ByteBuffer.allocate(0)
        // main 사진은 수정된 사진이 아니므로 MetaData를 수정하지 않는다
        buffer = ByteBuffer.allocate(jpegMetaData.size + picture.imageSize+2)
        buffer.put(jpegMetaData)
        buffer.put(picture._pictureByteArray)
        buffer.put("ff".toInt(16).toByte())
        buffer.put("d9".toInt(16).toByte())
        return buffer.array()
    }

    /**
     * 기존 metadata의 APP1 segment를 newApp1Data로 교체 후 변경 된 metadata 리턴
     */
    fun chageMetaData(newApp1Data : ByteArray) : ByteArray{
        var pos = 0
        var app1DataSize = 0
        var app1StartPos = 0
        var findAPP1 = false
        val byteBuffer = ByteArrayOutputStream()

        while (pos < jpegMetaData.size) {
            // APP1 마커 위치 찾기
            if (jpegMetaData[pos] == 0xFF.toByte() && jpegMetaData[pos + 1] == 0xE1.toByte()) {
                app1DataSize = ((jpegMetaData[pos + 2].toInt() and 0xFF) shl 8) or
                        ((jpegMetaData[pos + 3].toInt() and 0xFF) shl 0)
                app1StartPos = pos
                findAPP1 = true
                break
            }
            pos++
        }

        if(findAPP1){
            // 기존 APP1 segment를 newApp1Data로 교체 후 리턴
            byteBuffer.write(jpegMetaData, 0, app1StartPos)
            byteBuffer.write(newApp1Data)
            byteBuffer.write(
                jpegMetaData,
                app1StartPos + app1DataSize + 2,
                jpegMetaData.size - (app1StartPos + app1DataSize +2 )
            )
            return byteBuffer.toByteArray()
        }
        else{
            // 교체 안함
            return jpegMetaData
        }
    }

    /**
     * metaData + Frame로 이루어진 사진에서 metaData 부분만 리턴 하는 함수
     */
    fun extractJpegMeta(bytes: ByteArray, attribute: ContentAttribute) : ByteArray {
        Log.d("AiJPEG", "extractJpegMeta =============================")
        var isFindStartMarker = false // 시작 마커를 찾았는지 여부
        var metaDataEndPos = 0
        var SOFList : ArrayList<Int> = arrayListOf()
        var APP0MarkerList : ArrayList<Int> = arrayListOf() //JFIF
        SOFList = getSOFMarkerPosList(bytes)

        // 사진의 속성이 edited, magic이면 JFIF가 나오기 전까지를 메타데이터로 - 비트맵으로 변환하기 때문에 APP0 마커가 반드시 존재
        if (attribute == ContentAttribute.edited || attribute == ContentAttribute.magic) {
            APP0MarkerList = findAPP0Makers(bytes)
            if(APP0MarkerList.size > 0){
                isFindStartMarker = true
                metaDataEndPos = APP0MarkerList[APP0MarkerList.size -1]
            }
        }

        // 위에서 2번째 JFIF를 못찾았거나 edited, magic속성이 아닐 때
        if(!isFindStartMarker) {
            // 마지막 SOF가 나오기 전 까지 메타 데이터로

            if(SOFList.size > 0){
                metaDataEndPos = SOFList[SOFList.size -1]
            }
            else {
                Log.d("AiJPEG", "[meta]extract metadata : SOF가 존재하지 않음")
                return ByteArray(0)
            }
        }

        // Ai JPEG Format 인지 체크
        val (APP3StartIndx, APP3DataLength) = findMCFormat(bytes)
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
                SOFList.last() - (APP3StartIndx + APP3DataLength )
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

    /**
     * 이미지 데이터 중 APP1 세그먼트를 찾고 해당 부분을 추출하여 리턴
     */
    fun extractAPP1(allBytes : ByteArray) : ByteArray {
        var pos = 0
        var app1StartPos = 0
        var app1DataSize = 0
        var findAPP1 = false
        val byteBuffer = ByteArrayOutputStream()

        // APP1 마커 위치와 APP1 data size 찾기
        while(pos < allBytes.size - 1) {
            // APP1 마커 찾음
            if (allBytes[pos] == 0xFF.toByte() && allBytes[pos + 1] == 0xE1.toByte()) {
                findAPP1 = true
                app1StartPos = pos
                app1DataSize = ((allBytes[pos + 2].toInt() and 0xFF) shl 8) or
                        ((allBytes[pos + 3].toInt() and 0xFF) shl 0)
                break
            }
            pos++
        }

        if(findAPP1){
            Log.d("AiJPEG", "app1StartPos : ${app1StartPos}, app1DataSize : ${app1DataSize}")
            byteBuffer.write(allBytes, app1StartPos, app1DataSize + 2)
        }
        else {
            return ByteArray(0)
        }

        return byteBuffer.toByteArray()
        // 썸네일 부분 추출
//        if(findAPP1){
//            while(pos < app1StartPos + APP1DataSize-1) {
//                // sof 찾기 (썸네일)
//                if (allBytes[pos] == 0xFF.toByte() && allBytes[pos + 1] == 0xD8.toByte()) {
//                    val newSize = pos - app1StartPos
//                    // 사이즈를 변경한 데이터로 대체
//                    byteBuffer.write(0xFF)
//                    byteBuffer.write(0xE1)
//                    byteBuffer.write((newSize shr 8) and 0xFF)
//                    byteBuffer.write(newSize and 0xFF)
//                    byteBuffer.write(
//                        allBytes,
//                        app1StartPos + 4,
//                        pos - (app1StartPos + 4)
//                    ) // APP1 영역에서 썸네일의 sof가 나오기 전까지
////                    byteBuffer.write(
////                        allBytes,
////                        app1StartPos + APP1DataSize,
////                        allBytes.size - (app1StartPos + APP1DataSize)
////                    )
//                    Log.d("AiJPEG", "썸네일 추출 성공 allBytes : ${allBytes.size}, app1 ; ${byteBuffer.size()}")
//                    return byteBuffer.toByteArray()
//                }
//                pos++
//            }
//            Log.d("AiJPEG", "APP1은 존재하지만 썸네일 없음")
//            byteBuffer.write(allBytes, app1StartPos, APP1DataSize)
//            return byteBuffer.toByteArray()

      //  }else{
//            Log.d("AiJPEG", "APP1 없음")
//            return ByteArray(0)
       // }
    }

    /**
     * return true for Complicated Picute or false for others
     * Complicated Picture : App0와 App1가 같이 있는 사진
     */
    fun isComplicatedPictue(bytes: ByteArray): Boolean {
        var findApp0 = false
        var findApp1 = false
        var pos = 0
        var isAiFormat = false

        val (APP3StartIndx, APP3DataLength) = findMCFormat(bytes)
        Log.d("testTest", "APP3StartIndx, ${APP3StartIndx}")
        if(APP3StartIndx != 0) {
            isAiFormat = true
            Log.d("testTest", "All in JPEG 임, ${pos}")
        }
        while(pos < bytes.size -1){
            if (bytes[pos] == 0xFF.toByte() && bytes[pos + 1] == 0xE1.toByte()){
                findApp1 = true
                Log.d("testTest", "isComplicatedPictue : App1 발견, ${pos}")
            }
            if (bytes[pos] == 0xFF.toByte() && bytes[pos + 1] == 0xE0.toByte()){
                Log.d("testTest", "isComplicatedPictue : App0 발견, ${pos}")
                findApp0 = true
            }
            if(findApp0 && findApp1) break
            pos++
        }

        return isAiFormat || (!findApp1 && findApp0)
    }

    fun isBasicPicture(bytes: ByteArray): Boolean{
        // APP0이 없고 APP1만 있어야 함
        var findApp0 = false
        var findApp1 = false
        var isAiFormat = false
        var pos = 0

        val (APP3StartIndx, APP3DataLength) = findMCFormat(bytes)

        while(pos < bytes.size -1){
            if (bytes[pos] == 0xFF.toByte() && bytes[pos + 1] == 0xE1.toByte()){
                findApp1 = true
                Log.d("testTest", "isBasicPicture : App1 발견, ${pos}")
            }
            if (bytes[pos] == 0xFF.toByte() && bytes[pos + 1] == 0xE0.toByte()){
                Log.d("testTest", "isBasicPicture : App0 발견, ${pos}")
                findApp0 = true
            }

            pos++
        }

        //return findApp1 && !isAiFormat || !findApp0 && findApp1
        return findApp1 && !JpegViewModel.AllInJPEG
    }

    fun extractSOI(jpegBytes: ByteArray): ByteArray {
        return jpegBytes.copyOfRange(2, jpegBytes.size)
    }

    // 한 파일에서 SOF~EOI 부분의 바이너리 데이터를 찾아 ByteArray에 담아 리턴
    fun extractFrame(jpegBytes: ByteArray, attribute: ContentAttribute): ByteArray {
        var pos = 0
        var startIndex = 0
        var endIndex = jpegBytes.size

        var isFindStartMarker = false // 시작 마커를 찾았는지 여부
        var isFindEndMarker = false // 종료 마커를 찾았는지 여부

        var SOFList : ArrayList<Int> = arrayListOf()
        var APP0MarkerList : ArrayList<Int> = arrayListOf()

        // 2번째 JFIF가 나오기 전까지가 메타데이터
        if (attribute == ContentAttribute.edited || attribute == ContentAttribute.magic) {
            APP0MarkerList = findAPP0Makers(jpegBytes)
            if (APP0MarkerList.size > 0) {
                isFindStartMarker = true
                startIndex = APP0MarkerList.last()
                Log.d("AiJPEG", "extract frame : JFIF 찾음 ${startIndex}")
            }
        }
        // edited, magic속성이 아닐 때
        if(!isFindStartMarker) {
            // SOF가 나온 위치부터 프레임으로 추출
            SOFList = getSOFMarkerPosList(jpegBytes)
            if(SOFList.size > 0){
                isFindStartMarker = true
                startIndex = SOFList.last()
                Log.d("AiJPEG", "extract frame : SOF 찾음 ${startIndex}")
            }
            else {
                Log.d("AiJPEG", "extract frame : SOF가 존재하지 않음")
                return ByteArray(0)
            }
        }

        // EOI 시작 offset 찾기
        pos = jpegBytes.size-2
        while (pos > 0) {
            if (jpegBytes[pos] == 0xFF.toByte() && jpegBytes[pos + 1] == 0xD9.toByte()) {
                endIndex = pos
                isFindEndMarker = true
                break
            }
            pos--
        }

        if (!isFindStartMarker || !isFindEndMarker) {
            Log.d("AiJPEG", "Error: 찾는 마커가 존재하지 않음")
            return ByteArray(0)
        }
        else{
            var resultByte: ByteArray
            Log.d("startIndex", "bytes[${jpegBytes.size}, start[${startIndex}, end[${endIndex}")
            // 프레임 추출
            resultByte = jpegBytes.copyOfRange(startIndex, endIndex)
            return resultByte
        }
    }

    fun findAPP0Makers (jpegBytes: ByteArray) : ArrayList<Int> {
        var JFIF_startOffset = 0
        var JFIFList : ArrayList<Int> = arrayListOf()
        // 속성이 modified, magicPicture 가 아니면 2번째 JFIF(비트맵의 추가된 메타데이터)가 나오기 전까지 떼서 이용
        while (JFIF_startOffset < jpegBytes.size - 1) {
            if (jpegBytes[JFIF_startOffset] == 0xFF.toByte() && jpegBytes[JFIF_startOffset + 1] == 0xE0.toByte()) {
                JFIFList.add(JFIF_startOffset)
                Log.d("AiJPEG", "extract metadata :  JIFI찾음 - ${JFIF_startOffset}")
            }
            JFIF_startOffset++
        }
        return JFIFList
    }

    fun getEOIMarekrPosList(jpegBytes: ByteArray) : ArrayList<Int>{
        var EOIStartInex = 0
        var EOIList : ArrayList<Int> = arrayListOf()
        // SOF 시작 offset 찾기
        while (EOIStartInex < jpegBytes.size- 1) {
            var countFindingEOI = 0
            if (jpegBytes[EOIStartInex] == 0xFF.toByte() && jpegBytes[EOIStartInex+1] == 0xD9.toByte()) {
                EOIList.add(EOIStartInex)
                Log.d("save_test", "EOIStartInex LIST[${EOIList.size}] ${EOIStartInex}")
            }
            EOIStartInex++
        }
        return EOIList
    }

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

            //
            if(EOIPosList.size > 0){
                if (SOFStartInex == EOIPosList.last())
                    break
            }

        }
        return SOFList
//                if (findApp1) {
//                    Log.d("MCcontainer", "extract metadata : SOF find - ${pos}")
//                    // 썸네일의 sof가 아닐 때
//                    if (pos >= app1StartPos + app1DataLength + 2) {
//                        Log.d("MCcontainer", "extract metadata : SOF find - ${pos}")
//                        //startIndex = pos
//                        //break
//                    }
//                } else {
//                    Log.d("MCcontainer", "extract metadata : SOF find - ${pos}")
//                    //break
//                }


    }


    fun findMCFormat(jpegBytes: ByteArray) : Pair<Int,Int>{
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

    fun byteArraytoInt(byteArray: ByteArray, stratOffset : Int): Int {
        var intNum :Int = ((byteArray[stratOffset].toInt() and 0xFF) shl 24) or
                ((byteArray[stratOffset+1].toInt() and 0xFF) shl 16) or
                ((byteArray[stratOffset+2].toInt() and 0xFF) shl 8) or
                ((byteArray[stratOffset+3].toInt() and 0xFF))
        return intNum
    }
    /**
     *  checkAttribute(attribute: ContentAttribute): Boolean
     *      해당 attribute가 포함된 pictureContainer인지 확인
     *      포함 유무를 리턴
     */
    fun checkAttribute(attribute: ContentAttribute): Boolean {
//        while(!checkPictureList){}
        for(i in 0 until pictureList.size){
            if(pictureList[i].contentAttribute == attribute)
                return true
        }
        return false
    }

    /**
     * getBitmapList(attribute: ContentAttribute) : ArrayList<Bitmap>
     *      - picture의 attrubuteType이 인자로 전달된 attribute가 아닌 것만 list로 제작 후 전달
     */
    // bitmapList getter
    fun getBitmapList(attribute: ContentAttribute) : ArrayList<Bitmap>? {
        if (bitmapListAttribute == null || bitmapListAttribute != attribute) {
            attributeBitmapList.clear()
            bitmapListAttribute = attribute
        }
        if (attributeBitmapList.size == 0) {
            val newBitmapList = arrayListOf<Bitmap>()
            while (!checkBitmapList || !checkPictureList) {
                Log.d("faceRewind", "!!!! $checkBitmapList || $checkPictureList")
                Thread.sleep(200)
            }
            for (i in 0 until pictureList.size) {
//                if(!checkTransformAttributeBitmap)
//                    return null
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

    /**
     * getBitmapList() : ArrayList<Bitmap>
     *      - bitmapList가 없다면 Picture의 ArrayList를 모두 Bitmap으로 전환해서 제공
     *          있다면 bitmapList 전달
     */

    fun  getBitmapList() : ArrayList<Bitmap>? {
        while (!checkBitmapList || !checkPictureList) {
            Log.d("faceRewind","!!!! $checkBitmapList || $checkPictureList")
            Thread.sleep(200)
        }
        return bitmapList
    }

    fun addBitmapList( index: Int, bitmap: Bitmap) {
        while (!checkBitmapList || !checkPictureList || bitmapList.size < index) {
            Log.d("faceRewind", "!!!! $checkBitmapList || $checkPictureList")
        }
        bitmapList.add(index, bitmap)
        attributeBitmapList.clear()
        bitmapListAttribute = null
    }
    fun addBitmapList( bitmap: Bitmap) {
        while (!checkBitmapList || !checkPictureList) {
            Log.d("faceRewind", "!!!! $checkBitmapList || $checkPictureList")
        }
        bitmapList.add(bitmap)
        attributeBitmapList.clear()
        bitmapListAttribute = null
    }


    fun setBitmapList() {
        isSetBitmapListStart = true

        Log.d("faceRewind", "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@")
        Log.d("faceRewind", "getBitmapList 호출")

        val newBitmapList = arrayListOf<Bitmap>()

        try {
//            Log.d("faceRewind", "checkPictureList bitmapList.size ${bitmapList.size}")
//
//            Log.d("faceRewind", "checkPictureList while start")
//
            while (!checkPictureList) { }

//            Log.d("faceRewind", "checkPictureList $checkPictureList")
            val pictureListSize = pictureList.size
//            Log.d("faceRewind", "pictureListSize : $pictureListSize")
            val checkFinish = BooleanArray(pictureListSize)

            val exBitmap = ImageToolModule().byteArrayToBitmap(getJpegBytes(pictureList[0]))
            for (i in 0 until pictureListSize) {

                checkFinish[i] = false
                newBitmapList.add(exBitmap)
            }
            Log.d("faceRewind", "==============================")
            for (i in 0 until pictureListSize) {

                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        Log.d("faceRewind", "coroutine in pictureListSize : $pictureListSize")
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
            Log.d("faceRewind", "getBitmap end!!!")
//        checkTransformBitmap = fals
        } catch (e: IndexOutOfBoundsException) {
            // 예외가 발생한 경우 처리할 코드
            bitmapList.clear()
        }
    }

    /**
     * getMainBitmap() : Bitmap
     *      - mainBitmap 전달
     */
    fun getMainBitmap() : Bitmap? {
        return try {
//            while (!checkMain) {
//                Log.d("checkPictureList", "!!!!!!!!!!!!!!!!!!! while in")
//            }

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

    /**
     *  PictureList에 Picture를 삽입
     */
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

            Log.d("picture remove", "reomve $index")

            while (!checkBitmapList || !checkPictureList) {
            }
            Log.d("picture remove", "reomve2 $index")

            if(bitmapList.size > index) {
                bitmapList.removeAt(index)
            }
            Log.d("picture remove", "reomve3 $index")
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

/**
 * metaData와 Picture의 byteArray(frmae)을 붙여서 완전한 JEPG파일의 Bytes를 리턴하는 함수
 */
//    fun getJpegBytes(picture : Picture) : ByteArray{
//        var buffer : ByteBuffer = ByteBuffer.allocate(0)
//        var JFIF_startOffset = 0
//        var findCount = 0
//        var isFindSecondAPP0 : Boolean = false
//        // 비트맵으로 변환된 사진이 아니라면  2번째 JFIF(비트맵의 추가된 메타데이터)가 나오기 전까지 떼서 이용
//        if(picture.contentAttribute != ContentAttribute.edited && picture.contentAttribute != ContentAttribute.magic){
//            while (JFIF_startOffset < jpegMetaData.size - 1) {
//                // JFIF 찾기
//                if (jpegMetaData[JFIF_startOffset] == 0xFF.toByte() && jpegMetaData[JFIF_startOffset + 1] == 0xE0.toByte()) {
//                    findCount++
//                    Log.d("test_test", "getJpegBytes() JFIF(APP0) find - ${JFIF_startOffset}")
//                    if(findCount == 2) {
//                        isFindSecondAPP0 = true
//                        break
//                    }
//                }
//                JFIF_startOffset++
//            }
//            // 2번의 JFIF를 찾음 ->  main 사진은 수정된 사진이고 현재 picture는 Bitmap관련 MteaData를 떼서 사용해야 함
//            if(isFindSecondAPP0){
//                // 2번 째 JFIF 전까지 떼어서 이용
//                Log.d("test_test", "getJpegBytes() : main 사진은 수정된 사진이고 현재 picture는 일반 사진")
//                buffer = ByteBuffer.allocate(JFIF_startOffset + picture.size+2)
//                buffer.put(jpegMetaData.copyOfRange(0, JFIF_startOffset))
//                buffer.put(picture._pictureByteArray)
//                buffer.put("ff".toInt(16).toByte())
//                buffer.put("d9".toInt(16).toByte())
//            } else {
//                // main 사진은 수정된 사진이 아니므로 MetaData를 수정하지 않는다
//                Log.d("test_test", "getJpegBytes() : 현재 picture는 일반 사진")
//                buffer = ByteBuffer.allocate(jpegMetaData.size + picture.size+2)
//                buffer.put(jpegMetaData)
//                buffer.put(picture._pictureByteArray)
//                buffer.put("ff".toInt(16).toByte())
//                buffer.put("d9".toInt(16).toByte())
//            }
//        }
//        // picture가 bitmap 변환 작업이 있었던 사진
//        else{
//            // 속성이 modified이거나 JFIF를 2번 못 찾으면 전체 MetaData 이용
//            Log.d("test_test", "getJpegBytes() : 현재 picture는 수정된 사진")
//            buffer = ByteBuffer.allocate(jpegMetaData.size + picture.size+2)
//            buffer.put(jpegMetaData)
//            buffer.put(picture._pictureByteArray)
//            buffer.put("ff".toInt(16).toByte())
//            buffer.put("d9".toInt(16).toByte())
//        }
//        return buffer.array()
//    }

//fun removeThumbnail(bytes : ByteArray) : ByteArray{
//    var pos = 0
//    var app1StartPos = 0
//    val byteBuffer = ByteArrayOutputStream()
//    var APP1DataSize = 0
//    var findAPP1 = false
//    var findThumbnail = false
//
//    Log.d("AiJPEG", "썸네일 추출 시작")
//    while(pos < bytes.size) {
//        // APP1 마커 위치 찾기
//        if (bytes[pos] == 0xFF.toByte() && bytes[pos + 1] == 0xE1.toByte()) {
//            APP1DataSize = ((bytes[pos + 2].toInt() and 0xFF) shl 8) or
//                    ((bytes[pos + 3].toInt() and 0xFF) shl 0)
//            app1StartPos = pos
//            findAPP1 = true
//            break
//        }
//        pos++
//    }
//    // 썸네일 부분 추출
//    if(findAPP1){
//        while(pos < app1StartPos + APP1DataSize-1){
//            // SOI 찾기 (썸네일)
//            if (bytes[pos] == 0xFF.toByte() && bytes[pos + 1] == 0xD8.toByte()){
//                val newSize = pos - app1StartPos
//
//                // 썸네일 부분 추출
//                byteBuffer.write(bytes, 0, app1StartPos)
//                // 사이즈를 변경한 데이터로 대체
//                byteBuffer.write(0xFF)
//                byteBuffer.write(0xE1)
//                byteBuffer.write((newSize shr 8) and 0xFF)
//                byteBuffer.write(newSize and 0xFF)
//                byteBuffer.write(bytes, app1StartPos+4, pos-(app1StartPos+4)) // APP1 영역에서 썸네일의 sof가 나오기 전까지
//                byteBuffer.write(bytes, app1StartPos+APP1DataSize, bytes.size-(app1StartPos+APP1DataSize))
//                Log.d("AiJPEG", "썸네일 추출 성공")
//                findThumbnail = true
//                break
//            }
//            pos++
//        }
//    }else{
//        Log.d("AiJPEG", "APP1 없음")
//        return bytes
//    }
//
//    if(findThumbnail){
//        return byteBuffer.toByteArray()
//    }
//    else {
//        Log.d("AiJPEG", "썸네일 없음")
//        return bytes
//    }
//}

