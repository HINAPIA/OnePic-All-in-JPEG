package com.example.camerax.PictureModule.Info

import com.example.camerax.PictureModule.ImageContent
import com.example.camerax.PictureModule.Picture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

class ImageContentInfo(imageContent: ImageContent, startOffset : Int) {
    var contentInfoSize: Int = 0
    var dataStartOffset : Int = 0
    var imageCount : Int = 0
    var imageInfoList : ArrayList<ImageInfo> = arrayListOf()

    init{
        dataStartOffset = 0
        imageCount = imageContent.pictureCount
        imageInfoList = fillImageInfoList(imageContent.pictureList)
        contentInfoSize = getLength()
    }

    fun fillImageInfoList(pictureList : ArrayList<Picture>): ArrayList<ImageInfo> {
        var offset = 0
        var preSize = 0
        var imageInfoList : ArrayList<ImageInfo> = arrayListOf()
        for(i in 0..pictureList.size - 1){
            // 각 Picture의 ImageInfo 생성
            var imageInfo : ImageInfo = ImageInfo(pictureList.get(i))
            if(i > 0){
                offset = offset + preSize
            }
            preSize = imageInfo.dataSize
            // offset 지정
            imageInfo.offset = offset
            //imageInfoList에 삽입
            imageInfoList.add(imageInfo)
        }
        return imageInfoList
    }

    fun getLength() : Int {
        var size = 0
        for(i in 0..imageInfoList.size -1 ){
            size += imageInfoList.get(i).getImageInfoSize()
        }
        size += 12
        contentInfoSize = size
        return size
    }

    fun converBinaryData(): ByteArray {
        val buffer: ByteBuffer = ByteBuffer.allocate(getLength())
        //Image Content
        buffer.putInt(contentInfoSize)
        buffer.putInt(dataStartOffset)
        buffer.putInt(imageCount)
        //Image Content - Image Info
        for(j in 0..imageCount - 1){
            var imageInfo = imageInfoList.get(j)
            buffer.putInt(imageInfo.offset)
            buffer.putInt(imageInfo.dataSize)
            buffer.putInt(imageInfo.attribute)
            buffer.putInt(imageInfo.embeddedDataSize)
            // Image Content - Image Info - embeddedData
            if(imageInfo.embeddedDataSize > 0){
                for(p in 0..imageInfo.embeddedDataSize/4 -1){
                    buffer.putInt(imageInfo.embeddedData.get(p))
                }
            } // end of embeddedData
        }// end of Image Info
        // end of Image Content ...
        return buffer.array()
    }
    fun getEndOffset():Int{
        return imageInfoList.get(imageCount-1).offset + imageInfoList.get(imageCount-1).dataSize -1
    }
}