package com.goldenratio.onepic.PictureModule

import android.app.Activity
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import com.goldenratio.onepic.AllinJPEGModule.Content.ImageContent
import com.goldenratio.onepic.AllinJPEGModule.Content.Picture
import com.goldenratio.onepic.AllinJPEGModule.Contents.ContentAttribute
import com.goldenratio.onepic.AllinJPEGModule.Header
import com.goldenratio.onepic.AllinJPEGModule.TextContent
import com.goldenratio.onepic.AudioModule.AudioResolver
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.SaveModule.SaveResolver
import com.goldenratio.onepicdiary.PictureModule.AiSaveResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext


class AiContainer(_activity: Activity? = null) {
    private lateinit var activity : Activity
    var audioResolver : AudioResolver? = null
    var header : Header
    
    var imageContent : ImageContent = ImageContent()
    var audioContent : AudioContent = AudioContent()
    var textContent: TextContent = TextContent()

    var aiSaveResolver : AiSaveResolver
    var saveResolver : SaveResolver
    
    var isBurst : Boolean = true // 연속 촬영 이미지 플래그
    var isAllinJPEG : Boolean = true  // 현재 All-in JPEG 인지 플래그

    init {
        if(_activity != null){
            activity = _activity
            audioResolver = AudioResolver(_activity)
        }
        
        aiSaveResolver = AiSaveResolver(this)
        saveResolver = SaveResolver(activity)
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

    suspend fun save() : String {
        val resultByteArray = withContext(Dispatchers.Default) {
            aiSaveResolver.AiContainerToBytes(isBurst)
        }
        Log.d("성능 평가", "All-in JPEG 사진 : "+(resultByteArray.size).toString()+" kb")
        return saveResolver.save(resultByteArray, null)
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

    /**
     * TODO  Text Content를 갱신
     *
     * @param textList 텍스트 데이터가 담긴 String List
     * @param contentAttribute 텍스트 속성
     */
    fun setTextConent(textList : ArrayList<String>, contentAttribute: ContentAttribute){
        textContent.setContent(contentAttribute, textList)
    }


    fun setAudioContent(audioBytes : ByteArray, contentAttribute: ContentAttribute){
        audioContent.setContent(audioBytes, contentAttribute)
    }

    /**
     * TODO Ai Container 데이터를 통해 Content Info(image, text, audio) 객체 업데이트
     */
    fun settingHeaderInfo(){
        header.settingHeaderInfo()
    }

    /**
     * TODO  객체로 존재하는 APP3 데이터를 APP3 'All-in' 구조에 따라  바이너리 데이터로 변환 후 리턴
     *
     * @return APP3 'All-in' 구조의  APP3 바이너리 데이터
     */
    fun convertHeaderToBinaryData() : ByteArray{
        return header.convertBinaryData(isBurst)
    }

}

