package com.example.camerax.PictureModule.Info

import com.example.onepic.PictureModule.Contents.Text
import com.example.onepic.PictureModule.TextContent
import java.nio.ByteBuffer

class TextContentInfo(textContent: TextContent, startOffset : Int) {

    var contentInfoSize: Int = 0
    // var dataStartOffset : Int = 0
    var textCount : Int = 0
    var textInfoList : ArrayList<TextInfo> = arrayListOf()

    init{
        //  dataStartOffset = startOffset
        textCount = textContent.textCount
        textInfoList = fillTextInfoList(textContent.textList)
        contentInfoSize = getLength()
    }

    fun fillTextInfoList(textList : ArrayList<Text>): ArrayList<TextInfo> {
        var textInfoList : ArrayList<TextInfo> = arrayListOf()
        for(i in 0..textList.size - 1){
            // 각 Text TextInfo 생성
            var textInfo : TextInfo = TextInfo(textList.get(i))
            //imageInfoList에 삽입
            textInfoList.add(textInfo)
        }
        return textInfoList
    }

    fun converBinaryData(): ByteArray {
        val buffer: ByteBuffer = ByteBuffer.allocate(getLength())
        //text Content
        buffer.putInt(contentInfoSize)
        buffer.putInt(textCount)
        //text Content - text Info
        for(j in 0..textCount - 1){
            var textInfo = textInfoList.get(j)
            buffer.putInt(textInfo.attribute)
            buffer.putInt(textInfo.dataSize)
            for(p in 0..textInfo.dataSize-1){
                buffer.putChar(textInfo.data.get(p))
            }
        }// end of Text Info
        // end of Text Content ...
        return buffer.array()
    }

    fun getLength() : Int {
        var size = 0
        for(i in 0..textInfoList.size -1 ){
            size += textInfoList.get(i).getImageInfoSize()
        }
        size += 8
        return size
    }

}