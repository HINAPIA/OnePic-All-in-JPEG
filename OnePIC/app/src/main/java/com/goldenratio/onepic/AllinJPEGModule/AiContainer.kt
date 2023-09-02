package com.goldenratio.onepic.AllinJPEGModule

import android.app.Activity
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import com.goldenratio.onepic.AudioModule.AudioResolver
import com.goldenratio.onepic.AllinJPEGModule.Contents.ContentAttribute
import com.goldenratio.onepic.AllinJPEGModule.Contents.ContentType
import com.goldenratio.onepic.AllinJPEGModule.Contents.Picture
import com.goldenratio.onepic.SaveModule.SaveResolver
import kotlinx.coroutines.*


class AiContainer(_activity: Activity) {
    private lateinit var activity : Activity
    var header : Header

    var imageContent : ImageContent = ImageContent()
    var audioContent : AudioContent = AudioContent()
    var textContent: TextContent = TextContent()

    var saveResolver : SaveResolver
    var audioResolver : AudioResolver = AudioResolver(_activity)
    // 수정을 하거나 새로운 사진 그룹이 추가되면 +1
    private var groupCount : Int = 0
    var jpegConstant : JpegConstant = JpegConstant()

    var isBurstImage : Boolean = true

    init {
        activity = _activity
        saveResolver = SaveResolver(activity ,this)
        header = Header(this)

    }

    /**
     * byteArray에 있는 마커 이름과 마커의 위치를 출력하는 함수
     */
    fun exploreMarkers(byteArray: ByteArray){
        var pos = 0
        var marker : String =""
        while (pos < byteArray.size-2){
            if(byteArray[pos] == 0xFF.toByte()){
                val value =  (byteArray[pos].toInt() + 256) + (byteArray[pos + 1].toInt() + 256)
                if(jpegConstant.nameHashMap.containsKey(value)){
                    marker = jpegConstant.nameHashMap.get(value).toString()
                    Log.d("Marker_List", "[${marker}] : ${pos}")
                }
            }
            pos++
        }
    }


    fun init(){
        imageContent.init()
        audioContent.init()
        textContent.init()
    }

    /*Edit modules에서 호출하는 함수*/
    //해당 그룹에 존재하는 picture 모두를 list로 제공
    fun getPictureList() : ArrayList<Picture>{
        while (!imageContent.checkPictureList) {
        }
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
    suspend fun overwiteSave(fileName : String) : String{
       return saveResolver.overwriteSave(fileName)
    }

    //Container의 데이터를 파일로 저장
    suspend fun save(isSaved: MutableLiveData<Uri>): String {
        return saveResolver.save(isSaved)
    }

    /**
     * TODO 사진을 찍은 후에 호출되는 함수로 찍은 사진 데이터로 imageContent 갱신
     *
     * @param byteArrayList 촬영된 사진들의 바이너리 데이터 리스트
     * @param contentAttribute 촬영 모드
     * @return 작업 완료 결과
     */
    suspend fun setImageContent(byteArrayList: ArrayList<ByteArray>, type: ContentType, contentAttribute : ContentAttribute) : Boolean= withContext(Dispatchers.Default){
        var jop = async {
            imageContent.setContent(byteArrayList, contentAttribute)
        }
        jop.await()
        return@withContext true
    }

    fun setAudioContent(audioBytes : ByteArray, contentAttribute: ContentAttribute){
        audioContent.setContent(audioBytes, contentAttribute)
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

    fun setBasicJepg(sourceByteArray: ByteArray) {
        init()
        // 헤더 따로 프레임 따로 저장
        imageContent.setBasicContent(sourceByteArray)
    }


    /**
     * TODO Ai Container 데이터를 통해 Content Info(image, text, audio) 객체 갱신
     *
     */
    fun settingHeaderInfo(){
        header.settingHeaderInfo()
    }

    /**
     * TODO  객체로 존재하는 APP3 데이터를 APP3 'All-in' 구조에 따라  바이너리 데이터로 변환 후 리턴
     *
     *
     * @return APP3 'All-in' 구조의  APP3 바이너리 데이터
     */
    fun convertHeaderToBinaryData() : ByteArray{
        return header.convertBinaryData()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun audioPlay(){
        var audio = audioContent.audio
        if(audio != null){
            audioResolver.audioPlay(audio)
        }
    }

    fun audioStop(){
        var audio = audioContent.audio
        if (audio != null){
            audioResolver.audioStop()
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

