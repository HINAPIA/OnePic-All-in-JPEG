package com.example.camerax.PictureModule.Info

import com.example.onepic.PictureModule.Contents.Text
import com.example.onepic.PictureModule.TextContent
import java.nio.ByteBuffer

class TextContentInfo(textContent: TextContent) {

    var contentInfoSize: Int = 0
    var textCount : Int = 0
    var textInfoList : ArrayList<TextInfo> = arrayListOf()

    init{
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
            //imageInfoList에 삽입
            textInfoList.add(textInfo)

        }
        return textInfoList
    }

    fun converBinaryData(): ByteArray {
        val buffer: ByteBuffer = ByteBuffer.allocate(getLength())
        buffer.putInt(contentInfoSize)
        buffer.putInt(textCount)
        for(j in 0..textCount - 1){
            var textInfo = textInfoList.get(j)
            buffer.putInt(textInfo.attribute)
            buffer.putInt(textInfo.dataSize)
            for(p in 0..textInfo.dataSize- 1){
                buffer.putChar(textInfo.data.get(p))
            }
        }
        return buffer.array()
    }

    fun getLength() : Int {
        var size = 0
        for(i in 0..textInfoList.size -1 ){
            size += textInfoList.get(i).getTextInfoSize()
        }
        size += 8
        return size
    }


}