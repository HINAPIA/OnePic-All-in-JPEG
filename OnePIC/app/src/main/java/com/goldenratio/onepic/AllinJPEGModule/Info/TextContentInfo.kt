package com.goldenratio.camerax.PictureModule.Info

import com.goldenratio.onepic.AllinJPEGModule.Contents.Text
import com.goldenratio.onepic.AllinJPEGModule.TextContent
import java.nio.ByteBuffer

class TextContentInfo(textContent: TextContent, startOffset : Int) {

    var contentInfoSize: Int = 0
    var textCount : Int = 0
    var textInfoList : ArrayList<TextInfo> = arrayListOf()
    var dataStartOffset : Int = 0

    companion object {
        const val XOT_MARKER_SIZE = 2
        const val TEXT_CONTENT_SIZE_FIELD_SIZE = 4
        const val TEXT_COUNT_FIELD_SIZE = 4
        const val OFFSET_FIELD_SIZE = 4
        const val ATTRIBUTE_FIELD_SIZE = 4
        const val TEXT_DATA_SIZE = 4
    }

    init{
        dataStartOffset = startOffset
        textCount = textContent.textCount
        textInfoList = fillTextInfoList(textContent.textList)
        contentInfoSize = getLength()
    }

    /**
     * Text의 APP3 메타 데이터 객체 생성
     */
    fun fillTextInfoList(textList : ArrayList<Text>): ArrayList<TextInfo> {
        var preSize = 0
        var textInfoList : ArrayList<TextInfo> = arrayListOf()
        for(i in 0..textList.size - 1){
            // 각 Text TextInfo 생성
            var textInfo : TextInfo = TextInfo(textList.get(i))
            var textDataStartOffset = dataStartOffset
            if(i > 0){
                textDataStartOffset = textDataStartOffset + preSize
            }
            textInfo.offset = textDataStartOffset
            preSize = XOT_MARKER_SIZE + textInfo.dataSize
            //imageInfoList에 삽입
            textInfoList.add(textInfo)
        }
        return textInfoList
    }

    // TextContentInfo의 데이터를 바이트 데이터로 변환
    fun convertBinaryData(): ByteArray {
        val buffer: ByteBuffer = ByteBuffer.allocate(getLength())
        buffer.putInt(contentInfoSize)
        buffer.putInt(textCount)
        for(j in 0..textCount - 1){
            var textInfo = textInfoList.get(j)
            buffer.putInt(textInfo.offset)
            buffer.putInt(textInfo.attribute)
            buffer.putInt(textInfo.dataSize)
        }
        return buffer.array()
    }

    /**
     * APP3 extension 중 TextContentInfo의 사이즈를 리턴
     */
    fun getLength() : Int {
        var size = 0
        size += TEXT_CONTENT_SIZE_FIELD_SIZE + TEXT_COUNT_FIELD_SIZE
        for(i in 0..textInfoList.size -1 ){
            size += OFFSET_FIELD_SIZE + ATTRIBUTE_FIELD_SIZE + TEXT_DATA_SIZE
        }
        return size
    }

    fun getEndOffset():Int{
        if(textInfoList.size == 0){
            return dataStartOffset
        }
        else{
            var lastTextInfo = textInfoList.last()
            return lastTextInfo.offset + XOT_MARKER_SIZE + lastTextInfo.dataSize
        }
    }
}