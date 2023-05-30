package com.goldenratio.onepic.PictureModule

import android.graphics.Bitmap
import android.util.Log
import com.goldenratio.onepic.ImageToolModule
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
    var jpegConstant : JpegConstant = JpegConstant()
    var markerHashMap: HashMap<Int?, String?> = jpegConstant.nameHashMap
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
        Log.d("burst", "==================")
        Log.d("burst", "imageContnet 초기화")
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
     * ImageContent 리셋 후 초기화 - 카메라 찍을 때 호출되는 함수
     */
    suspend fun setContent(byteArrayList: ArrayList<ByteArray>, contentAttribute : ContentAttribute) : Boolean = withContext(Dispatchers.Default){
        init()
        // 메타 데이터 분리
        jpegMetaData = extractJpegMeta(byteArrayList.get(0),contentAttribute)
        for(i in 0 until byteArrayList.size){
            // frame 분리
            var frameBytes = async {
                extractFrame(byteArrayList.get(i),contentAttribute)
            }
            // Picture 객체 생성
            var picture = Picture(contentAttribute, frameBytes.await())
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
        while (!checkBitmapList || !checkPictureList) {
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
    fun resetBitmap() {
        mainBitmap = null
        bitmapList.clear()
        bitmapListAttribute = null
        attributeBitmapList.clear()
        isSetBitmapListStart = false
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
        var frameBytes : ByteArray = extractFrame(sourceByteArray,ContentAttribute.basic)
        // Picture 객체 생성
        var picture = Picture(ContentAttribute.basic, frameBytes)
        picture.waitForByteArrayInitialized()
        insertPicture(picture)
        mainPicture = pictureList[0]
        checkPictureList = true
        checkMain = true
    }
    fun addContent(byteArrayList: ArrayList<ByteArray>, contentAttribute : ContentAttribute){
        for(i in 0..byteArrayList.size-1){
            // Picture 객체 생성
            var picture = Picture(contentAttribute, byteArrayList.get(i))
            insertPicture(picture)
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
        val result = pictureList.remove(picture)
        pictureCount -= 1

        Log.d("picture remove", "reomve $index")

        while(!checkBitmapList || !checkPictureList) { }
        Log.d("picture remove", "reomve2 $index")
        bitmapList.removeAt(index)
        Log.d("picture remove", "reomve3 $index")

        return result
    }
    /**
     * PictureList의 index번째 요소를 찾아 반환
     */
    fun getPictureAtIndex(index : Int): Picture? {
        return pictureList.get(index) ?: null
    }

    /**
     * metaData와 Picture의 byteArray(frmae)을 붙여서 완전한 JEPG파일의 Bytes를 리턴하는 함수
     */
    fun getJpegBytes(picture : Picture) : ByteArray{
        Log.d("AiJPEG", "getJpegBytes : 호출")
        while(!checkPictureList) { }
        var buffer : ByteBuffer = ByteBuffer.allocate(0)
        // main 사진은 수정된 사진이 아니므로 MetaData를 수정하지 않는다
        buffer = ByteBuffer.allocate(jpegMetaData.size + picture.size+2)
        buffer.put(jpegMetaData)
        buffer.put(picture._pictureByteArray)
        buffer.put("ff".toInt(16).toByte())
        buffer.put("d9".toInt(16).toByte())
        return buffer.array()
    }



    /**
     * metaData + Frame로 이루어진 사진에서 metaData 부분만 리턴 하는 함수
     */
    fun extractJpegMeta(jpegBytes: ByteArray, attribute: ContentAttribute) : ByteArray {
        Log.d("AiJPEG", "extractJpegMeta =============================")
        var isFindStartMarker = false // 시작 마커를 찾았는지 여부
        var metaDataEndPos = 0
        var SOFList : ArrayList<Int> = arrayListOf()
        var APP0MarkerList : ArrayList<Int> = arrayListOf() //JFIF

        // 사진의 속성이 edited, magic이면 2번째 JFIF가 나오기 전까지를 메타데이터로
        if (attribute == ContentAttribute.edited || attribute == ContentAttribute.magic) {
            APP0MarkerList = findAPP0Makers(jpegBytes)
            if(APP0MarkerList.size >0){
                isFindStartMarker = true
                metaDataEndPos = APP0MarkerList[APP0MarkerList.size -1]
            }
        }

        // 위에서 2번째 JFIF를 못찾았거나 edited, magic속성이 아닐 때
        if(!isFindStartMarker) {
            // 마지막 SOF가 나오기 전 까지 메타 데이터로
            SOFList =findSOFMarkers(jpegBytes)
            if(SOFList.size >0){
                metaDataEndPos = SOFList[SOFList.size -1]
            }
            else {
                Log.d("AiJPEG", "extract metadata : SOF가 존재하지 않음")
                return ByteArray(0)
            }
        }

        // Ai JPEG Format 인지 체크
        val (APP3StartIndx, APP3DataLength) = findMCFormat(jpegBytes)

        // write
        var resultByte: ByteArray
        val byteBuffer = ByteArrayOutputStream()
        //  Ai JPEG Format 일 때
        if (APP3StartIndx > 0) {
            //  APP3 (Ai jpeg) 영역을 제외하고 metadata write
            Log.d("AiJPEG", "extract metadata : MC 포맷이여서 APP3 메타 데이터 뺴고 저장")
            byteBuffer.write(jpegBytes, 0, APP3StartIndx)
            byteBuffer.write(
                jpegBytes,
                APP3StartIndx + APP3DataLength + 2,
                metaDataEndPos - (APP3StartIndx + APP3DataLength + 2)
            )
            resultByte = byteBuffer.toByteArray()
        } else {
            Log.d("AiJPEG", "extract metadata : 일반 JEPG처럼 저장 pos : ${metaDataEndPos}")
            // SOF 전까지 추출
            resultByte = jpegBytes.copyOfRange(0, metaDataEndPos)
        }


        return resultByte
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
                startIndex = APP0MarkerList[APP0MarkerList.size - 1]
                Log.d("AiJPEG", "extract frame : JFIF 찾음 ${startIndex}")
            }
        }
        // 위에서 2번째 JFIF를 못찾았거나 edited, magic속성이 아닐 때
        if(!isFindStartMarker) {
            // 마지막 SOF가 나오기 전 까지 메타 데이터로
            SOFList =findSOFMarkers(jpegBytes)
            if(SOFList.size >0){
                isFindStartMarker = true
                startIndex = SOFList[SOFList.size -1]
                Log.d("AiJPEG", "extract frame : SOF 찾음 ${startIndex}")
            }
            else {
                Log.d("AiJPEG", "extract frame : SOF가 존재하지 않음")
                return ByteArray(0)
            }
        }


        // EOI 시작 offset 찾기
        pos = jpegBytes.size - 2
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

        var resultByte: ByteArray
        // 추출
        resultByte = jpegBytes.copyOfRange(startIndex, endIndex)
        return resultByte

    }

    fun findAPP0Makers (jpegBytes: ByteArray) : ArrayList<Int> {
        var JFIF_startOffset = 0
        var JFIFList : ArrayList<Int> = arrayListOf()
        // 속성이 modified, magicPicture 가 아니면 2번째 JFIF(비트맵의 추가된 메타데이터)가 나오기 전까지 떼서 이용
        while (JFIF_startOffset < jpegBytes.size - 1) {
            if (jpegBytes[JFIF_startOffset] == 0xFF.toByte() && jpegBytes[JFIF_startOffset + 1] == 0xE0.toByte()) {
                //isFindStartMarker = true
                //findCount++
                JFIFList.add(JFIF_startOffset)
                Log.d("AiJPEG", "extract metadata :  JIFI찾음 - ${JFIF_startOffset}")
            }
            JFIF_startOffset++
        }
        return JFIFList
    }

    fun findSOFMarkers (jpegBytes: ByteArray) : ArrayList<Int> {
        var SOFStartInex = 0
        var SOFList : ArrayList<Int> = arrayListOf()
        // SOF 시작 offset 찾기
        while (SOFStartInex < jpegBytes.size/2 - 1) {
            if (jpegBytes[SOFStartInex] == 0xFF.toByte() && jpegBytes[SOFStartInex + 1] == 0xC0.toByte()) {
                SOFList.add(SOFStartInex)
            }
            SOFStartInex++
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
        while (app3StartIndex < jpegBytes.size/2 - 1) {
            // APP3 마커가 있는 경우
            if (jpegBytes[app3StartIndex] == 0xFF.toByte() && jpegBytes[app3StartIndex + 1] == 0xE3.toByte()) {
                //MC Format인지 확인
                if (jpegBytes[app3StartIndex + 6] == 0x4D.toByte() && jpegBytes[app3StartIndex + 7] == 0x43.toByte()
                    && jpegBytes[app3StartIndex + 8] == 0x46.toByte()
                ) {
                    app3DataLength = ((jpegBytes[app3StartIndex + 2].toInt() and 0xFF) shl 8) or
                            ((jpegBytes[app3StartIndex + 3].toInt() and 0xFF) shl 0)
                    break
                }
            }
            app3StartIndex++
        }
        if(app3StartIndex == jpegBytes.size/2 - 1) app3StartIndex = 0
        return Pair(app3StartIndex, app3DataLength)
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
