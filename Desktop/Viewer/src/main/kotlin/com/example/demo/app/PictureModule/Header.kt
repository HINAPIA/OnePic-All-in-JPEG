package com.goldenratio.onepic.AllinJPEGModule

import com.goldenratio.camerax.PictureModule.Info.AudioContentInfo
import com.goldenratio.camerax.PictureModule.Info.ImageContentInfo
import com.goldenratio.camerax.PictureModule.Info.TextContentInfo
import com.goldenratio.onepic.PictureModule.AiContainer
import java.nio.ByteBuffer

class Header(_MC_container : AiContainer) {

    var headerDataLength : Short = 0
    private var AiContainer : AiContainer
    lateinit var imageContentInfo : ImageContentInfo
    lateinit var audioContentInfo : AudioContentInfo
    lateinit var textContentInfo: TextContentInfo

    init {
        AiContainer =_MC_container
    }

    companion object{
        val APP3_MARKER_SIZE = 2
        val APP3_LENGTH_FIELD_SIZE = 2
        val IDENTIFIER_FIELD_SIZE = 4
    }
    // MC Container에 채워진 Content의 정보를 Info 클래스들로 생성
    fun settingHeaderInfo(){
        imageContentInfo = ImageContentInfo(AiContainer.imageContent)
//        textContentInfo = TextContentInfo(MCContainer.textContent)
        textContentInfo = TextContentInfo(AiContainer.textContent, imageContentInfo.getEndOffset())
        audioContentInfo = AudioContentInfo(AiContainer.audioContent, textContentInfo.getEndOffset())
        headerDataLength = getAPP3FieldLength()
        applyAddedAPP3DataSize()
    }

    //추가한 APP3 extension + JpegMeta data 만큼 offset 변경
    fun applyAddedAPP3DataSize(){
        // 추가할 APP3 extension 만큼 offset 변경 - APP3 marker(2) + APP3 Data field length + EOI
        var headerLength = getAPP3FieldLength() + 2
        var jpegMetaLength = AiContainer.getJpegMetaBytes().size
        for(i in 0 until  imageContentInfo.imageCount){
            var pictureInfo = imageContentInfo.imageInfoList.get(i)
            if(i == 0){
                pictureInfo.imageDataSize += (headerLength+jpegMetaLength) + 3
            }else{
                pictureInfo.offset += (headerLength+jpegMetaLength) + 2
            }
        }

        for(i in 0 until textContentInfo.textCount){
            var textInfo = textContentInfo.textInfoList.get(i)
            textInfo.offset += (headerLength + jpegMetaLength) + 2
        }

        audioContentInfo.dataStartOffset += (headerLength+jpegMetaLength)+ 2
    }

    fun getAPP3FieldLength(): Short{
        var size = getAPP3CommonDataLength()
        size += imageContentInfo.getLength()
        size += textContentInfo.getLength()
        size += audioContentInfo.getLength()
        return size.toShort()
    }

    fun getAPP3CommonDataLength() : Int {
        return APP3_MARKER_SIZE + APP3_LENGTH_FIELD_SIZE + IDENTIFIER_FIELD_SIZE
    }

    fun convertBinaryData() : ByteArray {
        val buffer: ByteBuffer = ByteBuffer.allocate(getAPP3FieldLength() + 2)
        buffer.put("ff".toInt(16).toByte())
        buffer.put("e3".toInt(16).toByte())
        buffer.putShort(headerDataLength)
        // A, i, F, 0
        buffer.put(0x41.toByte())
        buffer.put(0x69.toByte())
        buffer.put(0x46.toByte())
        buffer.put(0x00.toByte())
        buffer.put(imageContentInfo.converBinaryData())
        buffer.put(textContentInfo.convertBinaryData())
        buffer.put(audioContentInfo.converBinaryData())
        return buffer.array()
    }
    //헤더의 내용을 바이너리 데이터로 변환하는 함수
//    fun convertBinaryData(): ByteArray{
//         var bufferSize  = pictureInfoList?.size!! * INFO_SIZE +6
//        //App3 마커
//        val buffer: ByteBuffer = ByteBuffer.allocate(bufferSize)
//        buffer.put("ff".toInt(16).toByte())
//        buffer.put("e3".toInt(16).toByte())
//        // 리스트 개수
//        buffer.putInt(pictureInfoList?.size!!)
//        //infoList
//        for(i in 0..(pictureInfoList?.size?.minus(1) ?:1 )){
//            var pictureInfo = pictureInfoList?.get(i)
//            buffer.putInt(pictureInfo!!.groupID!!)
//            buffer.putInt(pictureInfo!!.typeCode!!)
//            //작성한 APP3의 크기만큼 데이터 변경. 이 작업을 여기서 해도 괜찮은지 의논 필요
//            if(i == 0){
//                buffer.putInt(pictureInfo!!.offset!!)
//                buffer.putInt(pictureInfo!!.size!! + (bufferSize))
//
//            }else{
//                buffer.putInt(pictureInfo!!.offset!!+bufferSize-1)
//                buffer.putInt(pictureInfo!!.size!!)
//            }
//        }
//        val byteArray = buffer.array()
//        return byteArray
//    }
}