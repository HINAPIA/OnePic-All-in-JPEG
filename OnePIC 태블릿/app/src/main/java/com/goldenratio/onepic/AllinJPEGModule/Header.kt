package com.goldenratio.onepic.AllinJPEGModule

import com.goldenratio.camerax.PictureModule.Info.AudioContentInfo
import com.goldenratio.camerax.PictureModule.Info.ImageContentInfo
import com.goldenratio.camerax.PictureModule.Info.TextContentInfo
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
        val BURST_MODE = 1
    }

    /**
     * TODO Ai Container 데이터를 통해 Content Info(image, text, audio) 객체 갱신
     */
    fun settingHeaderInfo(){
        imageContentInfo = ImageContentInfo(AiContainer.imageContent)
        textContentInfo = TextContentInfo(AiContainer.textContent, imageContentInfo.getEndOffset())
        audioContentInfo = AudioContentInfo(AiContainer.audioContent, textContentInfo.getEndOffset())
        headerDataLength = getAPP3FieldLength()
        applyAddedAPP3DataSize()
    }

    /**
     * TODO 추가한 APP3 extension + JpegMeta data 만큼 offset 변경
     */
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

    /**
     * TODO 생성하는 APP3 segment 크기를 구한 후 리턴
     *
     * @return 생성될 APP3 segment 크기
     */
    fun getAPP3FieldLength(): Short{
        var size = getAPP3CommonDataLength()
        size += imageContentInfo.getLength()
        size += textContentInfo.getLength()
        size += audioContentInfo.getLength()
        return size.toShort()
    }

    fun getAPP3CommonDataLength() : Int {
        return APP3_MARKER_SIZE + APP3_LENGTH_FIELD_SIZE + IDENTIFIER_FIELD_SIZE + BURST_MODE
    }

    fun convertBinaryData(isBurst : Boolean) : ByteArray {
        val buffer: ByteBuffer = ByteBuffer.allocate(getAPP3FieldLength() + 2)
        buffer.put("ff".toInt(16).toByte())
        buffer.put("e3".toInt(16).toByte())
        buffer.putShort(headerDataLength)
        // A, i, F, 0
        buffer.put(0x41.toByte())
        buffer.put(0x69.toByte())
        buffer.put(0x46.toByte())
        buffer.put(0x00.toByte())
        if(isBurst)
            buffer.put(1.toByte())
        else
            buffer.put(0.toByte())
        buffer.put(imageContentInfo.converBinaryData(isBurst))
        buffer.put(textContentInfo.convertBinaryData())
        buffer.put(audioContentInfo.converBinaryData())
        return buffer.array()
    }

}