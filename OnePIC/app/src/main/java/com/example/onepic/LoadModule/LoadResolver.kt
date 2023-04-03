package com.example.camerax.LoadModule

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.camerax.PictureModule.*
import com.example.camerax.PictureModule.Contents.ContentAttribute
import com.example.camerax.PictureModule.Contents.Text


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
//        if(sourceByteArray.copyOfRange(APP3_startOffset, APP3_startOffset+2)!= byteArrayOf(0xff.toByte(), 0xe3.toByte())){
//            // 일반 JPEG
//            MCContainer.createBasicJpeg(sourceByteArray)
//            return
//        }
        // var header : Header = Header()
        var dataFieldLength = ByteArraytoInt(sourceByteArray, APP3_startOffset)
        // 1. ImageContent
        var imageContentInfoSize = ByteArraytoInt(sourceByteArray, APP3_startOffset + 4)
//        var pictureList = imageContentParsing(MCContainer,sourceByteArray, sourceByteArray.copyOfRange(APP3_startOffset + 8, APP3_startOffset + 12 + imageContentInfoSize))
//        MCContainer.imageContent.setContent(pictureList)

        // 2. TextContent
        var textContentStartOffset = APP3_startOffset + 4 + imageContentInfoSize
        var textContentInfoSize = ByteArraytoInt(sourceByteArray, textContentStartOffset)
        if(textContentInfoSize > 0){
            var textList = textContentParsing(MCContainer,sourceByteArray.copyOfRange(textContentStartOffset +4, textContentStartOffset + 8 + textContentInfoSize))
            MCContainer.textContent.setContent(textList)
        }

        // 3. AudioContent
        // MCContainer.saveResolver.saveImageOnAboveAndroidQ(MCContainer.imageContent.getJpegBytes(pictureList.get(0)))
        //  MCContainer.saveResolver.saveImageOnAboveAndroidQ(MCContainer.imageContent.getJpegBytes(pictureList.get(1)))

        // MCContainer.setContainer(groupContentList)

    }

    fun textContentParsing(MCContainer: MCContainer, textInfoByteArray: ByteArray) : ArrayList<Text>{
        var textList : ArrayList<Text> = arrayListOf()
        var startIndex = 4
        var textCount = ByteArraytoInt(textInfoByteArray, 0)

        for(i in 0..textCount -1){
            var attribute = ByteArraytoInt(textInfoByteArray, startIndex)
            startIndex += 4
            var size = ByteArraytoInt(textInfoByteArray,  startIndex)
            startIndex += 4
            val charArray = CharArray(size) // 변환된 char 값들을 담을 배열
            if (size > 0){
                for(j in 0..size*2 -1 step 2){
                    // 4개씩 쪼개서 Int 생성
                    val charValue = ((textInfoByteArray[startIndex+j].toInt() and 0xFF) shl 8) or
                            ((textInfoByteArray[startIndex+j+1].toInt() and 0xFF) shl 0)
                    charArray[j / 2] = charValue.toChar() // char로 변환 후 배열에 저장
                }
                startIndex += size*2
            }
            var text = Text(charArray.contentToString(), ContentAttribute.fromCode(attribute))
            textList.add(text)
        }

        return textList
    }


}