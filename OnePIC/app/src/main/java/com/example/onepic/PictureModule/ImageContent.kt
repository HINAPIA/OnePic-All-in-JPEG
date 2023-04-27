package com.example.onepic.PictureModule

import android.graphics.Bitmap
import android.util.Log
import com.example.onepic.ImageToolModule
import com.example.onepic.PictureModule.Contents.ActivityType
import com.example.onepic.PictureModule.Contents.ContentAttribute
import com.example.onepic.PictureModule.Contents.Picture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

    private var mainBitmap: Bitmap? = null
    private var bitmapList: ArrayList<Bitmap> = arrayListOf()
    private val attributeBitmapList: ArrayList<Bitmap> = arrayListOf()
    private var bitmapListAttribute : ContentAttribute? = null
    var activityType: ActivityType? = null

    private var checkGetBitmapList = false

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

    /**
     * getBitmapList()
     *      - bitmapList가 없다면 Picture의 ArrayList를 모두 Bitmap으로 전환해서 제공
     *          있다면 bitmapList 전달
     */
    // bitmapList getter
    fun getBitmapList(attribute: ContentAttribute) : ArrayList<Bitmap> {
        if(bitmapListAttribute == null || bitmapListAttribute != attribute) {
            attributeBitmapList.clear()
            bitmapListAttribute = attribute
        }

        if(attributeBitmapList.size == 0) {
            while(!checkGetBitmapList) {

            }

            for(i in 0 until pictureList.size){
                if(pictureList[i].contentAttribute != attribute)
                    attributeBitmapList.add(bitmapList[i])
            }
        }

        return attributeBitmapList
    }


    fun getBitmapList() : ArrayList<Bitmap> {
        if(bitmapList.size == 0){
            val checkFinish = BooleanArray(pictureList.size)

            val exBitmap = ImageToolModule().byteArrayToBitmap(getJpegBytes(pictureList[0]))

            for (i in 0 until pictureList.size) {
                checkFinish[i] = false
                bitmapList.add(exBitmap)
            }

            for (i in 0 until pictureList.size) {
                CoroutineScope(Dispatchers.Default).launch {
                    val bitmap = ImageToolModule().byteArrayToBitmap(getJpegBytes(pictureList[i]))
                    bitmapList[i] = bitmap
                    checkFinish[i] = true
                }
            }
            while (!checkFinish.all { it }) {
                // Wait for all tasks to finish
            }
        }

        checkGetBitmapList = true
        return bitmapList
    }

    fun setBitmapList(bitmapList: ArrayList<Bitmap>, bitmapListAttribute : ContentAttribute) {
        this.bitmapList = bitmapList
        this.bitmapListAttribute = bitmapListAttribute
    }


    fun getMainBitmap() : Bitmap {
        if(mainBitmap == null){
            mainBitmap = ImageToolModule().byteArrayToBitmap(getJpegBytes(mainPicture))
        }
        return mainBitmap!!
    }

    fun setMainBitmap(bitmap: Bitmap) {
        this.mainBitmap = mainBitmap
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
        mainPicture = pictureList[0]
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
        // 속성이 modified, magicPicture 가 아니면 2번째 JFIF(비트맵의 추가된 메타데이터)가 나오기 전까지 떼서 이용
        if(picture.contentAttribute != ContentAttribute.edited && picture.contentAttribute != ContentAttribute.magic){
            while (JFIF_startOffset < jpegMetaData.size - 1) {
                // JFIF 찾기
                if (jpegMetaData[JFIF_startOffset] == 0xFF.toByte() && jpegMetaData[JFIF_startOffset + 1] == 0xE0.toByte()) {
                    findCount++
                    Log.d("test_test", "getJpegBytes() JFIF(APP0) find - ${JFIF_startOffset}")
                    if(findCount == 2) {
                        break
                    }
                }
                JFIF_startOffset++
            }
            // 2번의 JFIF를 찾음 ->  main 사진은 수정된 사진이고 현재 picture는 Bitmap관련 byte를 떼서 사용해야 함
            if (JFIF_startOffset != jpegMetaData.size - 1) {
                // 2번 째 JFIF 전까지 떼어서 이용
                Log.d("test_test", "getJpegBytes() : main 사진은 수정된 사진이고 현재 picture는 일반 사진")
                val buffer: ByteBuffer = ByteBuffer.allocate(JFIF_startOffset + picture.size+2)
                buffer.put(jpegMetaData.copyOfRange(0, JFIF_startOffset))
                buffer.put(picture._pictureByteArray)
                buffer.put("ff".toInt(16).toByte())
                buffer.put("d9".toInt(16).toByte())
                return buffer.array()
            }
        }
        // 속성이 modified이거나 JFIF를 2번 못 찾으면 전체 MetaData 이용
        Log.d("test_test", "getJpegBytes() : 현재 picutre는 수정된 사진이거나 main이 수정된 사진 아님")
        val buffer: ByteBuffer = ByteBuffer.allocate(jpegMetaData.size + picture.size+2)
        buffer.put(jpegMetaData)
        buffer.put(picture._pictureByteArray)
        buffer.put("ff".toInt(16).toByte())
        buffer.put("d9".toInt(16).toByte())
        return buffer.array()

    }
    fun extractJpegMeta(jpegBytes: ByteArray) : ByteArray{

        var resultByte: ByteArray
        var startIndex = 0
        var isFindStartMarker = false // 시작 마커를 찾았는지 여부
        var pos = 0
        var extractPos = 0
        var app3DataLength = 0
        var app1StartPos = 0
        var app1DataLength = 0
        var findApp1 = false

        // app1 위치와 datalength 찾기
        while (app1StartPos < jpegBytes.size - 1) {
            // APP1 (-1,-31) 마커가 있는 경우
            if (jpegBytes[app1StartPos] == 0xFF.toByte() && jpegBytes[app1StartPos + 1] == 0xE1.toByte()) {
                app1DataLength = ((jpegBytes[app1StartPos+2].toInt() and 0xFF) shl 8) or
                            ((jpegBytes[app1StartPos+3].toInt() and 0xFF) shl 0)
                findApp1 = true
                Log.d("MCcontainer","extract metadata : APP1 find - pos :${app1StartPos}, length : ${app1DataLength}")
                break
            }
            app1StartPos++
        }

        // SOF 시작 offset 찾기
        while (pos < jpegBytes.size - 1) {

            // SOF(ffCO)
            if (jpegBytes[pos] == 0xFF.toByte() && jpegBytes[pos + 1] == 0xC0.toByte()) {
                if(findApp1){
                    Log.d("MCcontainer","extract metadata : SOF find - ${pos}")
                    // 썸네일의 sof가 아닐 때
                    if(pos >= app1StartPos + app1DataLength + 2){
                        Log.d("MCcontainer","extract metadata : 썸네일이 아닌 SOF find - ${pos}")
                        isFindStartMarker = true
                        break
                    }
                    // App1이 존재하지 않아서 썸네일이 없다면 첫 번째 찾은 sof마커의 위치에서 break
                } else{
                    break
                }
            }
            pos++
        }
        if (!isFindStartMarker) {
            println("startIndex :${startIndex}")
            Log.d("MCcontainer","extract metadata : SOF가 존재하지 않음")
            return ByteArray(0)
        }
        //end of SOF find

        // MC Format인지 확인 - MC Format일 경우 APP3 데이터 빼고 set
        while (extractPos < jpegBytes.size - 1) {
            // APP3 마커가 있는 경우
            if (jpegBytes[extractPos] == 0xFF.toByte() && jpegBytes[extractPos + 1] == 0xE3.toByte()) {
                //MC Format인지 확인
                if(jpegBytes[extractPos+6] == 0x4D.toByte() &&  jpegBytes[extractPos+7] == 0x43.toByte()
                    && jpegBytes[extractPos+8] == 0x46.toByte()){
                    app3DataLength = ((jpegBytes[extractPos+2].toInt() and 0xFF) shl 8) or
                            ((jpegBytes[extractPos+3].toInt() and 0xFF) shl 0)
                    Log.d("MCcontainer", "extract metadata : MC 포맷")
                    break
                }
            }
            extractPos++
        }
        // write
        val byteBuffer = ByteArrayOutputStream()
        if(extractPos <jpegBytes.size-1){
            Log.d("MCcontainer", "extract metadata : MC 포맷이여서 APP3 메타 데이터 뺴고 저장")
            byteBuffer.write(jpegBytes, 0, extractPos)
            byteBuffer.write(jpegBytes, extractPos + app3DataLength + 2, pos - (extractPos + app3DataLength + 2))
            resultByte = byteBuffer.toByteArray()
        }else{
            Log.d("MCcontainer", "extract metadata : 일반 JEPG처럼 저장 pos : ${pos}")
            // SOF 전까지 추출
            resultByte = jpegBytes.copyOfRange(0, pos)
        }
        return resultByte
    }

    fun extractSOI(jpegBytes: ByteArray): ByteArray{
        return jpegBytes.copyOfRange(2, jpegBytes.size)
    }
    // 한 파일에서 SOF~EOI 부분의 바이너리 데이터를 찾아 ByteArray에 담아 리턴
     fun extractFrame(jpegBytes: ByteArray): ByteArray {
        var resultByte: ByteArray
        var startIndex = 0
        var endIndex = jpegBytes.size
        var isFindStartMarker = false // 시작 마커를 찾았는지 여부
        var isFindEndMarker = false // 종료 마커를 찾았는지 여부
        var pos = 0
        var app1StartPos = 0
        var app1DataLength = 0
        var findApp1 = false

        // app1 위치와 datalength 찾기
        while (app1StartPos < jpegBytes.size - 1) {
            // APP1 마커가 있는 경우
            if (jpegBytes[app1StartPos] == 0xFF.toByte() && jpegBytes[app1StartPos + 1] == 0xE1.toByte()) {
                app1DataLength = ((jpegBytes[app1StartPos+2].toInt() and 0xFF) shl 8) or
                        ((jpegBytes[app1StartPos+3].toInt() and 0xFF) shl 0)
                findApp1 = true
                Log.d("MCcontainer","extract Frame : APP1 find - pos :${app1StartPos}, length : ${app1DataLength}")
                break
            }
            app1StartPos++
        }

        // SOF 시작 offset 찾기
        while (pos < jpegBytes.size - 1) {
            if (jpegBytes[pos] == 0xFF.toByte() && jpegBytes[pos + 1] == 0xC0.toByte()) {
                if(findApp1){
                    Log.d("MCcontainer","extract Frame : SOF find - ${pos}")
                    // 썸네일의 sof가 아닐 때
                    if(pos >= app1StartPos + app1DataLength + 2){
                        Log.d("MCcontainer","extract Frame : 진짜 SOF find - ${pos}")
                        isFindStartMarker = true
                        startIndex = pos
                        break
                    }
                }
            }
            pos++
        }
        if (!isFindStartMarker) {
            println("startIndex :${startIndex}")
            Log.d("MCcontainer","extract Frame : SOF가 존재하지 않음")
            return ByteArray(0)
        }

        pos = jpegBytes.size-2
        // EOI 시작 offset 찾기
        while (pos > 0) {
            if (jpegBytes[pos] == 0xFF.toByte() && jpegBytes[pos + 1] == 0xD9.toByte()) {
                endIndex = pos
                isFindEndMarker = true
                break
            }
            pos--
        }
        if (!isFindStartMarker || !isFindEndMarker) {
            //println("startIndex :${startIndex}")
            //println("endIndex :${endIndex}")
            Log.d("이미지","Error: 찾는 마커가 존재하지 않음")
            //println("Error: 찾는 마커가 존재하지 않음")
            return ByteArray(0)
        }
        // 추출
        resultByte = jpegBytes.copyOfRange(startIndex, endIndex )
        return resultByte

    }

}