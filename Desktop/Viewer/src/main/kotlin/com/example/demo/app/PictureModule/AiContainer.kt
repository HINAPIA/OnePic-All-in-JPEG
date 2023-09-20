package com.goldenratio.onepic.PictureModule

import com.goldenratio.onepic.AllinJPEGModule.Content.ImageContent
import com.goldenratio.onepic.AllinJPEGModule.Content.Picture
import com.goldenratio.onepic.AllinJPEGModule.Header
import com.goldenratio.onepic.AllinJPEGModule.TextContent
import com.goldenratio.onepic.AudioModule.AudioResolver


class AiContainer() {
    var audioResolver : AudioResolver = AudioResolver()

    var header : Header
    var imageContent : ImageContent = ImageContent()
    var audioContent : AudioContent = AudioContent()
    var textContent: TextContent = TextContent()

    var isBurst : Boolean = true // 연속 촬영 이미지 플래그
    var isAllinJPEG : Boolean = true  // 현재 All-in JPEG 인지 플래그

    init {
        header = Header(this)

    }

    fun init(){
        imageContent.init()
        audioContent.init()
        textContent.init()

    }

    fun setBasicJepg(sourceByteArray: ByteArray) {
        init()
        isAllinJPEG = false
        isBurst = false
        // 헤더 따로 프레임 따로 저장
        imageContent.setBasicContent(sourceByteArray)
    }


    /**
     * TODO 사진 파일을 로드할 때 호출되는 함수로 imageContent 업데이트
     *
     * @param _pictureList
     * @param isBurstMode
     */
    fun setImageContentAfterParsing(_pictureList : ArrayList<Picture>, isBurstMode : Int){
        isAllinJPEG = true
        imageContent.setContent(_pictureList)

    }
    fun setJpegMetaBytes(_jpegMetaData : ByteArray){
        imageContent.jpegHeader = _jpegMetaData
    }

    fun getJpegMetaBytes() : ByteArray{
        if(imageContent.jpegHeader.size == 0){
            System.out.println("JpegMetaData size가 0입니다.")
        }
        return imageContent.jpegHeader
    }

}

