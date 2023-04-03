package com.example.camerax.PictureModule.Info

import com.example.camerax.PictureModule.Contents.Text
import com.example.camerax.PictureModule.TextContent
import java.nio.ByteBuffer

class TextContentInfo(textContent: TextContent , startOffset : Int) {

    var contentInfoSize: Int = 0
    var dataStartOffset : Int = 0
    var textCount : Int = 0
    var textInfoList : ArrayList<TextInfo> = arrayListOf()

    init{
        dataStartOffset = startOffset
        textCount = textContent.textCount
        textInfoList = fillTextInfoList(textContent.textList)
        contentInfoSize = getLength()
    }

    fun fillTextInfoList(textList : ArrayList<Text>): ArrayList<TextInfo> {
        var offset = 0
        var preSize = 0
        var textInfoList : ArrayList<TextInfo> = arrayListOf()
        for(i in 0..textList.size - 1){
            // 각 Text TextInfo 생성
            var textInfo : TextInfo = TextInfo(textList.get(i))
            if(i > 0){
                offset = offset + preSize
            }
            preSize = textInfo.dataSize
            // offset 지정
            textInfo.offset = offset
            //imageInfoList에 삽입
            textInfoList.add(textInfo)

        }
        return textInfoList
    }

    fun converBinaryData(): ByteArray {
        val buffer: ByteBuffer = ByteBuffer.allocate(getLength())
        //Image Content
        buffer.putInt(contentInfoSize)
        buffer.putInt(dataStartOffset)
        buffer.putInt(textCount)
        //Image Content - Image Info
        for(j in 0..textCount - 1){
            var textInfo = textInfoList.get(j)
            buffer.putInt(textInfo.offset)
            buffer.putInt(textInfo.dataSize)
            buffer.putInt(textInfo.attribute)
        }// end of Text Info
        // end of Text Content ...
        return buffer.array()
    }

    fun getLength() : Int {
        var size = 0
        for(i in 0..textInfoList.size -1 ){
            size += textInfoList.get(i).getImageInfoSize()
        }
        size += 12
        return size
    }

    fun getEndOffset():Int{
        if(textCount == 0) return dataStartOffset
        return dataStartOffset + textInfoList.get(textCount-1).offset + textInfoList.get(textCount-1).dataSize -1
    }
}