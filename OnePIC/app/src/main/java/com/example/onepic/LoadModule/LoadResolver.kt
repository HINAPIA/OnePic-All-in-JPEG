package com.example.onepic.LoadModule

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.onepic.PictureModule.Contents.ContentAttribute
import com.example.onepic.PictureModule.Contents.Text
import com.example.onepic.PictureModule.Contents.Picture
import com.example.onepic.PictureModule.MCContainer
import kotlinx.coroutines.*


class LoadResolver() {

    fun ByteArraytoInt(byteArray: ByteArray, stratOffset : Int): Int {
        var intNum :Int = ((byteArray[stratOffset].toInt() and 0xFF) shl 24) or
                ((byteArray[stratOffset+1].toInt() and 0xFF) shl 16) or
                ((byteArray[stratOffset+2].toInt() and 0xFF) shl 8) or
                ((byteArray[stratOffset+3].toInt() and 0xFF))
        return intNum
    }


    suspend fun imageContentParsing(MCContainer: MCContainer, sourceByteArray: ByteArray, imageInfoByteArray: ByteArray): ArrayList<Picture> = withContext(Dispatchers.Default) {
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
                val jpegBytes = sourceByteArray.copyOfRange(imageDataStartOffset + offset, imageDataStartOffset + offset + size - 1)
                // Jpeg Meta 데이터 떼기
                var jpegMetaData = MCContainer.imageContent.extractJpegMeta(sourceByteArray.copyOfRange(imageDataStartOffset + offset,
                    imageDataStartOffset + offset + size -1))
                MCContainer.setJpegMetaBytes(jpegMetaData)
                val frame =async {
                    MCContainer.imageContent.extractFrame(jpegBytes)
                }
                picture = Picture(offset, frame.await(), ContentAttribute.fromCode(attribute), embeddedDataSize, embeddedData)
                picture.waitForByteArrayInitialized()

            }else{
                // picture 생성
                picture = Picture(offset, sourceByteArray.copyOfRange(imageDataStartOffset + offset, imageDataStartOffset + offset + size - 1), ContentAttribute.fromCode(attribute), embeddedDataSize, embeddedData)
                picture.waitForByteArrayInitialized()

            }
            pictureList.add(picture)
            Log.d("test_test", "picutureList size : ${pictureList.size}")
        }
        return@withContext pictureList
    }


    suspend fun createMCContainer(
        MCContainer: MCContainer,
        sourceByteArray: ByteArray,
        isViewChanged: MutableLiveData<Boolean>
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            var APP3_startOffset = 4
            if(!(sourceByteArray[2].toInt() == -1 && sourceByteArray[3].toInt() == -29)){
                // 일반 JPEG
                Log.d("test_test", "일반 JPEG 생성")
                MCContainer.setBasicJepg(sourceByteArray)

            }
            else{
                Log.d("test_test", "MC JPEG 생성")
                // var header : Header = Header()
                var dataFieldLength = ByteArraytoInt(sourceByteArray, APP3_startOffset)
                // 1. ImageContent
                var imageContentInfoSize = ByteArraytoInt(sourceByteArray, APP3_startOffset + 4)
                var pictureList = async {
                    imageContentParsing(MCContainer,sourceByteArray, sourceByteArray.copyOfRange(APP3_startOffset + 8, APP3_startOffset + 12 + imageContentInfoSize))
                }
                Log.d("test_test", "PictureList await() 전")
                MCContainer.imageContent.setContent(pictureList.await())
                Log.d("test_test", "PictureList await()  후")
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

            }

            // MCContainer.setContainer(groupContentList)
            CoroutineScope(Dispatchers.Main).launch {
                Log.d("test_test","isViewChanged.value = true")
                isViewChanged.value = true
            }
            Log.d("test_test", "PictureList await() 리턴 전")
            return@launch
        }


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