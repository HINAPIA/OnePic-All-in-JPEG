package com.goldenratio.onepic.PictureModule


//import com.goldenratio.onepic.AudioModule.AudioResolver
import com.goldenratio.onepic.AllinJPEGModule.Contents.ContentAttribute
import com.goldenratio.onepic.AllinJPEGModule.Contents.ContentType
import com.goldenratio.onepic.AllinJPEGModule.Contents.Picture
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
        // 헤더 따로 프레임 따로 저장
        imageContent.setBasicContent(sourceByteArray)
    }


    fun getJpegMetaBytes() : ByteArray{
        if(imageContent.jpegMetaData.size == 0){
            //Log.e("user error", "JpegMetaData size가 0입니다.")
        }
        return imageContent.jpegMetaData
    }
    fun setJpegMetaBytes(_jpegMetaData : ByteArray){
        imageContent.jpegMetaData = _jpegMetaData
    }


}

