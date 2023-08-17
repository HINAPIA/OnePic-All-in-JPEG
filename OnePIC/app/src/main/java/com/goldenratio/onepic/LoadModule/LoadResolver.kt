package com.goldenratio.onepic.LoadModule

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.PictureModule.Contents.Audio
import com.goldenratio.onepic.PictureModule.Contents.ContentAttribute
import com.goldenratio.onepic.PictureModule.Contents.Picture
import com.goldenratio.onepic.PictureModule.Contents.Text
import com.goldenratio.onepic.PictureModule.MCContainer
import kotlinx.coroutines.*
import java.io.IOException


class LoadResolver() {
    companion object{
        const val MARKER_SIZE = 2
        const val APP3_FIELD_LENGTH_SIZE = 2
        const val FIELD_SIZE = 4
    }
    fun findAPP3StartPos(sourceByteArray: ByteArray) : Int {
        // APP3 - ALL in JPEG 세그먼트의 시작 위치를 찾음
        var APP3_startOffset = 2
        while (APP3_startOffset < sourceByteArray.size - 1) {
            if (sourceByteArray[APP3_startOffset] == 0xFF.toByte() && sourceByteArray[APP3_startOffset + 1] == 0xE3.toByte()) {
                //All in Format인지 확인 TODO("All-in 과 MC과 합쳐져 있음. 후에 MC 지워주기")
                if(sourceByteArray[APP3_startOffset+4] == 0x4D.toByte() &&  sourceByteArray[APP3_startOffset+5] == 0x43.toByte()
                    && sourceByteArray[APP3_startOffset+6] == 0x46.toByte() ||
                    sourceByteArray[APP3_startOffset+4] == 0x41.toByte() &&  sourceByteArray[APP3_startOffset+5] == 0x69.toByte()
                    && sourceByteArray[APP3_startOffset+6] == 0x46.toByte()  ){
                    //APP3_startOffset +=2
                    return APP3_startOffset
                }else {
                    // APP3 마커가 있지만 MC Format이 아님
                    return -1
                }
                //break`
            }
            APP3_startOffset++
        }
        // APP3 마커가 없음
        return -1
    }

    suspend fun createMCContainer(
        MCContainer: MCContainer,
        sourceByteArray: ByteArray
    ) {
        MCContainer.exploreMarkers(sourceByteArray)

        Log.d("MCContainer", "createMCContainer() sourceByreArray.Size : ${sourceByteArray.size}")
        CoroutineScope(Dispatchers.IO).launch {
            // APP3 세그먼트의 시작 위치를 찾음
            var APP3_startOffset = 2
            APP3_startOffset = findAPP3StartPos(sourceByteArray)
            if (APP3_startOffset == - 1) {
                try{
                    // APP3 세그먼트를 찾지 못함
                    // 일반 JPEG
                    Log.d("MCContainer", "createMCContainer() 일반 JPEG 생성")
                    MCContainer.setBasicJepg(sourceByteArray)
                    JpegViewModel.AllInJPEG = false
                }catch (e : IOException){
                    Log.e("MCcontainer", "createMCContainer() Basic JPEG Parsing 불가")
                }
            }
            else {
                try{
                    Log.d("MCcontainer", "createMCContainer() MC JPEG 생성")
                    Log.d("MCcontainer", "createMCContainer() App3 Start Offset : ${APP3_startOffset}")
                    JpegViewModel.AllInJPEG = true
                    // var header : Header = Header()
                    //var dataFieldLength = ByteArraytoInt(sourceByteArray, APP3_startOffset + MARKER_SIZE)

                    // 1. ImageContent Pasrsing
                    var imageContentStartOffset = APP3_startOffset + MARKER_SIZE + APP3_FIELD_LENGTH_SIZE +  FIELD_SIZE
                    var imageContentInfoSize = ByteArraytoInt(sourceByteArray, imageContentStartOffset)
                    var pictureList = async {
                        imageContentParsing(
                            MCContainer,
                            sourceByteArray,
                            sourceByteArray.copyOfRange(
                                imageContentStartOffset,
                                imageContentStartOffset + imageContentInfoSize
                            )
                        )
                    }
                    MCContainer.imageContent.setContent(pictureList.await())

                    // 2. TextContent Pasrsing
                    var textContentStartOffset = APP3_startOffset + MARKER_SIZE + APP3_FIELD_LENGTH_SIZE+  FIELD_SIZE +  imageContentInfoSize
                    var textContentInfoSize = ByteArraytoInt(sourceByteArray, textContentStartOffset)
                    if (textContentInfoSize > 0) {
                        var textList = textContentParsing(
                            MCContainer,  sourceByteArray,
//                            sourceByteArray.copyOfRange(
//                                textContentStartOffset + 4,
//                                textContentStartOffset + 8 + textContentInfoSize
//                            )
                            sourceByteArray.copyOfRange(
                                textContentStartOffset ,
                                textContentStartOffset + 4 + textContentInfoSize
                            )
                        )
                        MCContainer.textContent.setContent(textList)
                    }

                    // 3. AudioContent Pasrsing
                    var audioContentStartOffset = textContentStartOffset + textContentInfoSize
                    var audioContentInfoSize = ByteArraytoInt(sourceByteArray, audioContentStartOffset)
                    Log.d("AudioModule" , "audioContentInfoSize : ${audioContentInfoSize}")
                    if (audioContentInfoSize > 0) {
                        var audioDataStartOffset = ByteArraytoInt(sourceByteArray, audioContentStartOffset + 4)
                        Log.d("AudioModule" , "audioDataStartOffset : ${audioDataStartOffset}")

                        var audioAttribute = ByteArraytoInt(sourceByteArray, audioContentStartOffset + 8)
                        Log.d("AudioModule" , "audioAttribute : ${audioAttribute}")

                        var audioDataLength = ByteArraytoInt(sourceByteArray, audioContentStartOffset + 12)
                        Log.d("AudioModule" , "audioDataLength : ${audioDataLength}")
                        if(audioDataLength > 0){
                            var audioBytes = sourceByteArray.copyOfRange(audioDataStartOffset + MARKER_SIZE, audioDataStartOffset  + audioDataLength)
                            Log.d("AudioModule" , "audioBytes : ${audioBytes.size}")
                            var audio = Audio(audioBytes, ContentAttribute.fromCode(audioAttribute))
                            MCContainer.audioContent.setContent(audio)
                            MCContainer.audioResolver.saveByteArrToAacFile(audio._audioByteArray!!,"viewer_record")

                        }
                        // MCContainer.audioResolver.saveByteArrToAacFile(audioBytes)
                    }
                }catch (e : IOException){
                    Log.e("MCcontainer", "MC JPEG Parsing 불가")
                }

            }
//            CoroutineScope(Dispatchers.Main).launch {
//                isViewChanged.value = true
//            }
            return@launch
        }
    }
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
        var imageContentSize = ByteArraytoInt(imageInfoByteArray, startIndex)
        startIndex++
        var imageCount = ByteArraytoInt(imageInfoByteArray, startIndex*4)
        startIndex++
        for(i in 0..imageCount -1){
            var offset = ByteArraytoInt(imageInfoByteArray, (startIndex*4))
            startIndex++
            var app1DataSize = ByteArraytoInt(imageInfoByteArray, (startIndex*4))
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
                val jpegBytes = sourceByteArray.copyOfRange(offset,  offset + size - 1)
                // Jpeg Meta 데이터 떼기
                var jpegMetaData = MCContainer.imageContent.extractJpegMeta(sourceByteArray.copyOfRange(offset,
                    offset + size -1), ContentAttribute.fromCode(attribute))
                MCContainer.setJpegMetaBytes(jpegMetaData)
                val app1Segment = MCContainer.imageContent.extractAPP1(jpegBytes)
                val frame =async {
                    MCContainer.imageContent.extractFrame(jpegBytes,ContentAttribute.fromCode(attribute))
                }
                picture = Picture(offset, app1Segment, frame.await(), ContentAttribute.fromCode(attribute), embeddedDataSize, embeddedData)
                picture.waitForByteArrayInitialized()
            }else{
                val app1Segment = sourceByteArray.copyOfRange(offset + 2, offset + 2 + app1DataSize)
                val imageData = sourceByteArray.copyOfRange(offset + 2 + app1DataSize, offset + 2 + app1DataSize + size)
                // picture 생성
                picture = Picture(offset, app1Segment, imageData, ContentAttribute.fromCode(attribute), embeddedDataSize, embeddedData)

                picture.waitForByteArrayInitialized()
            }
            pictureList.add(picture)
            Log.d("Image_Parsing", picture.toString())
            Log.d("Load_Module", "picutureList size : ${pictureList.size}")
        }
        return@withContext pictureList
    }

    fun textContentParsing(MCContainer: MCContainer, sourceByteArray: ByteArray, textInfoByteArray: ByteArray) : ArrayList<Text>{
        var textList : ArrayList<Text> = arrayListOf()
        var startIndex = 0
        var textContentInfoSize = ByteArraytoInt(textInfoByteArray, startIndex)
        startIndex++
        var textCount = ByteArraytoInt(textInfoByteArray, startIndex * FIELD_SIZE)
        startIndex++
        for(i in 0..textCount -1){
            var offset = ByteArraytoInt(textInfoByteArray, startIndex * FIELD_SIZE)
            startIndex++
            var attribute = ByteArraytoInt(textInfoByteArray, startIndex * FIELD_SIZE)
            startIndex++
            var size = ByteArraytoInt(textInfoByteArray,  startIndex * FIELD_SIZE)
            startIndex++

            val charArray = CharArray(size/2) // 변환된 char 값들을 담을 배열
            if (size > 0){
                if(sourceByteArray[offset] == 0xFF.toByte() && sourceByteArray[offset+1] == 0x20.toByte()){
                    for(j in 0..size -1 step 2){
                        // 4개씩 쪼개서 Int 생성
                        val charValue = ((sourceByteArray[offset + MARKER_SIZE + j].toInt() and 0xFF) shl 8) or
                                ((sourceByteArray[offset + MARKER_SIZE + j+1].toInt() and 0xFF) shl 0)
                        charArray[j / 2] = charValue.toChar() // char로 변환 후 배열에 저장
                    }
                }
            }
            var string : String = String(charArray)
            var text = Text(string, ContentAttribute.fromCode(attribute))
            Log.d("text_parsing", text.toString())
            textList.add(text)
        }
        return textList
    }


}