package com.example.onepic.PictureModule

import android.app.Activity
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.onepic.AudioModule.AudioResolver
import com.example.onepic.PictureModule.Contents.ContentAttribute
import com.example.onepic.PictureModule.Contents.ContentType
import com.example.onepic.PictureModule.Contents.Picture
import com.example.onepic.SaveModule.SaveResolver


class MCContainer(_activity: Activity) {
     var saveResolver : SaveResolver
     var audioResolver : AudioResolver = AudioResolver(_activity)
    private lateinit var activity : Activity
    var header : Header



    var imageContent : ImageContent = ImageContent()
    var audioContent : AudioContent = AudioContent()
    var textContent: TextContent = TextContent()
    // 수정을 하거나 새로운 사진 그룹이 추가되면 +1
    private var groupCount : Int = 0
    init {
        activity = _activity
        saveResolver = SaveResolver(activity ,this)
        header = Header(this)

    }

    fun init(){
        imageContent.init()
        audioContent.init()
        textContent.init()

    }

    /*Edit modules에서 호출하는 함수*/
    //해당 그룹에 존재하는 picture 모두를 list로 제공
    fun getPictureList() : ArrayList<Picture>{
        return imageContent.pictureList
    }
    // 해당 그룹에 존재하는 picture 중 해당 attribute 속성인 것들만 list로 제공
    fun getPictureList(attribute: ContentAttribute) : ArrayList<Picture>{
        var pictureList = imageContent.pictureList
        var resultPictureList :ArrayList<Picture> = arrayListOf()
        for(i in 0..pictureList.size -1){
            var picture = pictureList.get(i)
            if(picture.contentAttribute == attribute)
                resultPictureList.add(picture)
        }
        return resultPictureList
    }
    //해당 그룹에 존재하는 modifiedPicture 제공
    fun getMainPicture(): Picture {
        return imageContent.mainPicture
    }

    // 해당 그룹의 Modified Picture 변경
//    fun setModifiedPicture(groupID: Int, modifyPicture: ByteArray, attribute: ContentAttribute): Picture {
//        var picture = Picture(modifyPicture, attribute)
//        groupContentList.get(groupID).imageContent.modifiedPicture = picture
//    }

    /*Edit modules에서 호출하는 함수 끝 */


    // 사진을 찍은 후에 호출되는 함수로 MC Container를 초기화하고 찍은 사진 내용으로 MC Container를 채운다
    fun setImageContent(byteArrayList: ArrayList<ByteArray>, type: ContentType, contentAttribute : ContentAttribute){
        imageContent.init()
        imageContent.setContent(byteArrayList, contentAttribute)
        var testString : ArrayList<String> = arrayListOf("안녕하세요", "2071231 김유진")
        textContent.setContent(ContentAttribute.basic, testString)
        //save()
    }

    fun setAudioContent(audioBytes : ByteArray, contentAttribute: ContentAttribute){
        audioContent.setContent(audioBytes, contentAttribute)
    }
    // Text Content를 초기화. 뷰어에서 텍스트를 추가 후 Container에게 넣기

    fun setTextConent(contentAttribute: ContentAttribute, textList : ArrayList<String>){
        textContent.setContent(contentAttribute, textList)
    }
    fun setBasicJepg(sourceByteArray: ByteArray) {
        init()
        // 헤더 따로 프레임 따로 저장
        imageContent.setBasicContent(sourceByteArray)
    }

    fun settingHeaderInfo(){
        header.settingHeaderInfo()
    }
    fun convertHeaderToBinaryData() : ByteArray{
        return header.convertBinaryData()
    }
    //Container의 데이터를 파일로 저장
    fun save(){
        saveResolver.save()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun audioPlay(){
        var audio = audioContent.audio
        if(audio != null){
            audioResolver.audioPlay(audio)
        }

    }
    fun getJpegMetaBytes() : ByteArray{
        if(imageContent.jpegMetaData.size == 0){
            Log.e("user error", "JpegMetaData size가 0입니다.")
        }
        return imageContent.jpegMetaData
    }
    fun setJpegMetaBytes(_jpegMetaData : ByteArray){
        imageContent.jpegMetaData = _jpegMetaData
    }


}

