package com.example.camerax.LoadModule

import android.app.Activity
import android.media.Image
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.camerax.PictureModule.*
import com.example.camerax.PictureModule.Contents.ContentAttribute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class LoadResolver() {

    fun ByteArraytoInt(byteArray: ByteArray, stratOffset : Int): Int {
        var intNum :Int = ((byteArray[stratOffset].toInt() and 0xFF) shl 24) or
                ((byteArray[stratOffset+1].toInt() and 0xFF) shl 16) or
                ((byteArray[stratOffset+2].toInt() and 0xFF) shl 8) or
                ((byteArray[stratOffset+3].toInt() and 0xFF))
        return intNum
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    fun imageContentParsing(MCContainer: MCContainer, sourceByteArray: ByteArray, imageInfoByteArray: ByteArray) : ArrayList<Picture> {
        var picture : Picture
        var pictureList : ArrayList<Picture> = arrayListOf()
        var startIndex = 0
        var imageDataStartOffset = ByteArraytoInt(imageInfoByteArray, startIndex)
        startIndex++
        var imageCount = ByteArraytoInt(imageInfoByteArray, startIndex*4)
        startIndex++
        for(i in 0..imageCount -1){
            var offset = ByteArraytoInt(imageInfoByteArray, (startIndex*4))
            startIndex++
            var size = ByteArraytoInt(imageInfoByteArray, startIndex*4)
            startIndex++
            var attribute = ByteArraytoInt(imageInfoByteArray, startIndex*4)
            startIndex++
            var embeddedDataSize = ByteArraytoInt(imageInfoByteArray, startIndex*4)
            startIndex++
            var embeddedData : ArrayList<Int> = arrayListOf()
            if (embeddedDataSize > 0){
                var curInt : Int = 0
                for(j in 0..embeddedDataSize/4 -1){
                    // 4개씩 쪼개서 Int 생성
                    curInt = ByteArraytoInt(imageInfoByteArray, startIndex*4)
                    embeddedData.add(curInt)
                    startIndex++
                }
            }
            if(i==0){
                // Jpeg Meta 데이터 떼기
                var jpegMetaData = MCContainer.imageContent.extractJpegMeta(sourceByteArray.copyOfRange(imageDataStartOffset + offset,
                    imageDataStartOffset + offset + size -1))
                MCContainer.setJpegMetaBytes(jpegMetaData)
                picture = Picture(offset,MCContainer.imageContent.extractFrame(sourceByteArray.copyOfRange(imageDataStartOffset + offset,
                    imageDataStartOffset + offset + size -1)) , ContentAttribute.fromCode(attribute), embeddedDataSize,embeddedData)

            }else{
                // picture 생성
                picture = Picture(offset, sourceByteArray.copyOfRange(imageDataStartOffset + offset,
                    imageDataStartOffset + offset + size -1), ContentAttribute.fromCode(attribute), embeddedDataSize,embeddedData)

            }
            pictureList.add(picture)
        }

        return pictureList
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun createMCContainer(MCContainer: MCContainer, sourceByteArray: ByteArray) {
        var APP3_startOffset = 4
        // var header : Header = Header()
        var dataFieldLength = ByteArraytoInt(sourceByteArray, APP3_startOffset)
        // 1. ImageContent
        var imageContentInfoSize = ByteArraytoInt(sourceByteArray, APP3_startOffset + 4)
        var pictureList = imageContentParsing(MCContainer,sourceByteArray, sourceByteArray.copyOfRange(APP3_startOffset + 8, APP3_startOffset + 12 + imageContentInfoSize))
        MCContainer.imageContent.setContent(pictureList)

        // 2. TextContent
        // 3. AudioContent
       // MCContainer.saveResolver.saveImageOnAboveAndroidQ(MCContainer.imageContent.getJpegBytes(pictureList.get(0)))
       // MCContainer.saveResolver.saveImageOnAboveAndroidQ(MCContainer.imageContent.getJpegBytes(pictureList.get(1)))

       // MCContainer.setContainer(groupContentList)

    }


}