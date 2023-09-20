//package com.goldenratio.onepic.LoadModule
//
//import com.goldenratio.onepic.AllinJPEGModule.Contents.Audio
//import com.goldenratio.onepic.AllinJPEGModule.Contents.ContentAttribute
//import com.goldenratio.onepic.AllinJPEGModule.Contents.Picture
//import com.goldenratio.onepic.AllinJPEGModule.Contents.Text
//import com.goldenratio.onepic.PictureModule.AiContainer
//import kotlinx.coroutines.*
//import java.io.IOException
//
//
//class LoadResolver() {
//    companion object{
//        const val MARKER_SIZE = 2
//        const val APP3_FIELD_LENGTH_SIZE = 2
//        const val FIELD_SIZE = 4
//        const val BURST_MODE_SIZE = 1
//    }
//
//    fun isAllinJpegFormat(sourceByteArray : ByteArray, APP3_startOffset: Int) : Boolean{
//        return (sourceByteArray[APP3_startOffset+4] == 0x4D.toByte() &&  sourceByteArray[APP3_startOffset+5] == 0x43.toByte()
//                && sourceByteArray[APP3_startOffset+6] == 0x46.toByte() ||
//                sourceByteArray[APP3_startOffset+4] == 0x41.toByte() &&  sourceByteArray[APP3_startOffset+5] == 0x69.toByte()
//                && sourceByteArray[APP3_startOffset+6] == 0x46.toByte()  )
//    }
//
//
//    /**
//     * TODO All-in JPEG 포맷인지 구별 후 APP3 pos 리턴
//     *
//     * @param sourceByteArray 사진 파일의 바이너리 데이터
//     * @return All-in JPEG 포맷이면 APP3 시작 pos 리턴, 그렇지 않으면 -1 리턴
//     */
//    fun findAPP3StartPos(sourceByteArray: ByteArray) : Int {
//        // APP3 - ALL in JPEG 세그먼트의 시작 위치를 찾음
//        var APP3_startOffset = 2
//        while (APP3_startOffset < sourceByteArray.size - 1) {
//            if (sourceByteArray[APP3_startOffset] == 0xFF.toByte() && sourceByteArray[APP3_startOffset + 1] == 0xE3.toByte()) {
//                val isAllinJpegFormat =  isAllinJpegFormat(sourceByteArray, APP3_startOffset)
//                //All in Format인지 확인 TODO("All-in 과 MC과 합쳐져 있음. 후에 MC 지워주기")
//                if(isAllinJpegFormat)
//                    return APP3_startOffset
//                else {
//                    // APP3 마커가 있지만 All in Format이 아님
//                    return -1
//                }
//                //break`
//            }
//            APP3_startOffset++
//        }
//        // APP3 마커가 없음
//        return -1
//    }
//
//    /**
//     * TODO JPEG과 All-in JPEG 포맷 구별 후, 포맷에 따라 AiContainer를 채우는 함수 호출
//     *      JPEG -> setBasicJPEG 호출
//     *      All-in JPEG -> parsingAllinJPEG 호출
//     * @param AiContainer 갱신할 AiConatainer 객체
//     * @param sourceByteArray 사진 파일의 바이너리 데이터
//     */
//    suspend fun createAiContainer(
//        AiContainer: AiContainer,
//        sourceByteArray: ByteArray
//    ) {
//         // APP3 세그먼트의 시작 위치를 찾음
//        var APP3_startOffset = 0
//        APP3_startOffset = findAPP3StartPos(sourceByteArray)
//        // APP3 세그먼트를 찾지 못함
//        if (APP3_startOffset == - 1) {
//            try{
//                // 일반 JPEG
//                createAiConaterInJPEG(AiContainer, sourceByteArray)
//
//            }catch (e : IOException){
//                  }
//        }
//        else {
//            createAiConainerInAllinJPEG(AiContainer, sourceByteArray)
//        }
//
//    }
//
//    fun createAiConaterInJPEG(AiContainer: AiContainer, sourceByteArray: ByteArray){
//        AiContainer.setBasicJepg(sourceByteArray)
//    }
//
//    suspend fun createAiConainerInAllinJPEG(AiContainer: AiContainer,
//                                            sourceByteArray: ByteArray){
//        CoroutineScope(Dispatchers.IO).launch {
//            // APP3 세그먼트의 시작 위치를 찾음
//            var APP3_startOffset = 2
//            APP3_startOffset = findAPP3StartPos(sourceByteArray)
//
//            try {
//
//                val isBurstMode = sourceByteArray.get( APP3_startOffset + MARKER_SIZE + APP3_FIELD_LENGTH_SIZE + FIELD_SIZE)
//                var burst_mode_size = 1
//                if(isBurstMode.toInt() == 1){
//                    AiContainer.isBurst = true
//                }
//                else if (isBurstMode.toInt() == 0){
//                    AiContainer.isBurst = false
//
//                } else{
//                    AiContainer.isBurst = true
//                    burst_mode_size = 0
//                }
//
//
//                // 1. ImageContent Pasrsing
//                var imageContentStartOffset =
//                    APP3_startOffset + MARKER_SIZE + APP3_FIELD_LENGTH_SIZE + FIELD_SIZE + burst_mode_size
//                var imageContentInfoSize = ByteArraytoInt(sourceByteArray, imageContentStartOffset)
//                var pictureList = async {
//                    imageContentParsing(
//                        AiContainer,
//                        sourceByteArray,
//                        sourceByteArray.copyOfRange(
//                            imageContentStartOffset,
//                            imageContentStartOffset + imageContentInfoSize
//                        ),
//                        isBurstMode.toInt()
//                    )
//                }
//                AiContainer.imageContent.setContent(pictureList.await())
//
//                // 2. TextContent Pasrsing
//                var textContentStartOffset =
//                    APP3_startOffset + MARKER_SIZE + APP3_FIELD_LENGTH_SIZE + FIELD_SIZE + BURST_MODE_SIZE+ imageContentInfoSize
//                var textContentInfoSize = ByteArraytoInt(sourceByteArray, textContentStartOffset)
//                if (textContentInfoSize > 0) {
//                    var textList = textContentParsing(
//                        AiContainer, sourceByteArray,
//                        sourceByteArray.copyOfRange(
//                            textContentStartOffset,
//                            textContentStartOffset + 4 + textContentInfoSize
//                        )
//                    )
//                    AiContainer.textContent.setContent(textList)
//                }
//
//                // 3. AudioContent Pasrsing
//                var audioContentStartOffset = textContentStartOffset + textContentInfoSize
//                var audioContentInfoSize = ByteArraytoInt(sourceByteArray, audioContentStartOffset)
//                if (audioContentInfoSize > 0) {
//                    var audioDataStartOffset =
//                        ByteArraytoInt(sourceByteArray, audioContentStartOffset + 4)
//
//                    var audioAttribute =
//                        ByteArraytoInt(sourceByteArray, audioContentStartOffset + 8)
//
//                    var audioDataLength =
//                        ByteArraytoInt(sourceByteArray, audioContentStartOffset + 12)
//
//                    if (audioDataLength > 0) {
//                        var audioBytes = sourceByteArray.copyOfRange(
//                            audioDataStartOffset + MARKER_SIZE,
//                            audioDataStartOffset + audioDataLength
//                        )
//                        var audio = Audio(audioBytes, ContentAttribute.fromCode(audioAttribute))
//                        AiContainer.audioContent.setContent(audio)
//                        if(AiContainer.audioResolver !=null ){
//                            AiContainer.audioResolver!!.saveByteArrToAacFile(
//                                audio._audioByteArray!!)
//                        }
//                    }
//                    // MCContainer.audioResolver.saveByteArrToAacFile(audioBytes)
//                }
//            } catch (e: IOException) {
//
//            }
//            return@launch
//        }
//    }
//
//    /**
//     * TODO 4 바이트를 Int 형으로 해석하여 리턴
//     *
//     * @param byteArray
//     * @param stratOffset
//     * @return
//     */
//    fun ByteArraytoInt(byteArray: ByteArray, stratOffset : Int): Int {
//        var intNum :Int = ((byteArray[stratOffset].toInt() and 0xFF) shl 24) or
//                ((byteArray[stratOffset+1].toInt() and 0xFF) shl 16) or
//                ((byteArray[stratOffset+2].toInt() and 0xFF) shl 8) or
//                ((byteArray[stratOffset+3].toInt() and 0xFF))
//        return intNum
//    }
//
//    /**
//     * TODO APP3에 저장된 이미지 콘텐츠 정보를 읽어 List<Picture> 생성 후 리턴
//     *
//     * @param AiContainer
//     * @param sourceByteArray 파일 바이너리 데이터
//     * @param imageInfoByteArray Image Content 영역 바이너리 데이터
//     * @param isBurstMode 연속 촬영 사진 플래그
//     * @return 파싱하여 생성된 List<Picture>
//     */
//    suspend fun imageContentParsing(AiContainer: AiContainer, sourceByteArray: ByteArray, imageInfoByteArray: ByteArray, isBurstMode : Int): ArrayList<Picture> = withContext(Dispatchers.Default) {
//        var picture : Picture
//        var pictureList : ArrayList<Picture> = arrayListOf()
//
//        var startIndex = 0
//        startIndex++
//        var imageCount = ByteArraytoInt(imageInfoByteArray, startIndex*4)
//        startIndex++
//        for(i in 0..imageCount -1) {
//            var offset = ByteArraytoInt(imageInfoByteArray, (startIndex * 4))
//            startIndex++
//            var app1DataSize = ByteArraytoInt(imageInfoByteArray, (startIndex * 4))
//            startIndex++
//            var size = ByteArraytoInt(imageInfoByteArray, startIndex * 4)
//            startIndex++
//            var attribute = ByteArraytoInt(imageInfoByteArray, startIndex * 4)
//            startIndex++
//            var embeddedDataSize = ByteArraytoInt(imageInfoByteArray, startIndex * 4)
//            startIndex++
//            var embeddedData: ArrayList<Int> = arrayListOf()
//            if (embeddedDataSize > 0) {
//                var curInt: Int = 0
//                for (j in 0..embeddedDataSize / 4 - 1) {
//                    curInt = ByteArraytoInt(imageInfoByteArray, startIndex * 4)
//                    embeddedData.add(curInt)
//                    startIndex++
//                }
//            }
//            if (i == 0) {
//                val jpegBytes = sourceByteArray.copyOfRange(offset, offset + size - 1)
//                // Jpeg Meta 데이터 추출
//                var jpegMetaData = AiContainer.imageContent.extractJpegMeta(
//                    sourceByteArray.copyOfRange(
//                        offset,
//                        offset + size - 1
//                    ), ContentAttribute.fromCode(attribute)
//                )
//                AiContainer.setJpegMetaBytes(jpegMetaData)
//
//                var app1Segment: ByteArray?
//                // 연속 모드일 때 APP1 파싱 안함
//                if (isBurstMode == 1) {
//                    app1Segment = null
//                } else {
//                    app1Segment = AiContainer.imageContent.extractAPP1(jpegBytes)
//                }
//                // 프레임 추출
//                val frame = async {
//                    AiContainer.imageContent.extractFrame(jpegBytes, ContentAttribute.fromCode(attribute))
//                }
//                picture = Picture(
//                    offset,
//                    app1Segment,
//                    frame.await(),
//                    ContentAttribute.fromCode(attribute),
//                    embeddedDataSize,
//                    embeddedData
//                )
//                picture.waitForByteArrayInitialized()
//            } else {
//                val app1Segment = sourceByteArray.copyOfRange(offset + 2, offset + 2 + app1DataSize)
//                val imageData = sourceByteArray.copyOfRange(offset + 2 + app1DataSize, offset + 2 + app1DataSize + size)
//                // picture 생성
//                picture = Picture(
//                    offset,
//                    app1Segment,
//                    imageData,
//                    ContentAttribute.fromCode(attribute),
//                    embeddedDataSize,
//                    embeddedData
//                )
//                picture.waitForByteArrayInitialized()
//            }
//            pictureList.add(picture)
//        }
//        return@withContext pictureList
//    }
//
//    /**
//     * TODO APP3에 저장된 텍스트 콘텐츠 정보를 읽어 List<Text> 생성 후 리턴
//     *
//     * @param AiContainer
//     * @param sourceByteArray 파일 바이너리 데이터
//     * @param textInfoByteArray Text Content 영역 바이너리 데이터
//     * @return 파싱하여 생성된 List<Text>
//     */
//    fun textContentParsing(AiContainer: AiContainer, sourceByteArray: ByteArray, textInfoByteArray: ByteArray) : ArrayList<Text>{
//        var textList : ArrayList<Text> = arrayListOf()
//        var startIndex = 0
//        var textContentInfoSize = ByteArraytoInt(textInfoByteArray, startIndex)
//        startIndex++
//        var textCount = ByteArraytoInt(textInfoByteArray, startIndex * FIELD_SIZE)
//        startIndex++
//        for(i in 0..textCount -1){
//            var offset = ByteArraytoInt(textInfoByteArray, startIndex * FIELD_SIZE)
//            startIndex++
//            var attribute = ByteArraytoInt(textInfoByteArray, startIndex * FIELD_SIZE)
//            startIndex++
//            var size = ByteArraytoInt(textInfoByteArray,  startIndex * FIELD_SIZE)
//            startIndex++
//
//            val charArray = CharArray(size/2) // 변환된 char 값들을 담을 배열
//            if (size > 0){
//                if(sourceByteArray[offset] == 0xFF.toByte() && sourceByteArray[offset+1] == 0x20.toByte()){
//                    for(j in 0..size -1 step 2){
//                        // 4개씩 쪼개서 Int 생성
//                        val charValue = ((sourceByteArray[offset + MARKER_SIZE + j].toInt() and 0xFF) shl 8) or
//                                ((sourceByteArray[offset + MARKER_SIZE + j+1].toInt() and 0xFF) shl 0)
//                        charArray[j / 2] = charValue.toChar() // char로 변환 후 배열에 저장
//                    }
//                }
//            }
//            var string : String = String(charArray)
//            var text = Text(string, ContentAttribute.fromCode(attribute))
//            textList.add(text)
//        }
//        return textList
//    }
//
//
//}