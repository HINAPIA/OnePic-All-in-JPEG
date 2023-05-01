package com.example.onepic

import android.content.Context
import android.database.ContentObserver
import com.example.onepic.PictureModule.Contents.Picture
import android.os.Looper

import android.provider.MediaStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.onepic.PictureModule.MCContainer


class JpegViewModel(private val context:Context) : ViewModel() {

    val GALLERY_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    var jpegMCContainer = MutableLiveData<MCContainer>()

    private var imageUriList = mutableListOf<String>() // 임시 데이터(원본이 사라져도)
    private val _imageUriLiveData = MutableLiveData<List<String>>() // 내부처리 데이터
    val imageUriLiveData: LiveData<List<String>> get() = _imageUriLiveData //읽기 전용

    private lateinit var urlHashMap:HashMap<String, Int>

    /* TODO: Edit 창에서 Main 바꿀 때 필요한 property
        - 동기처리 문제 아직 남아있어서, 만약 갤러리(사진)에 변경 있으면 테스트 할 때는 갤러리뷰 한번 갔다가 들어와주세요! */

    var currentImageFilePath:String? = null // 현재 메인 이미지 filePath
    var selectedSubImage: Picture? = null // 선택된 서브 이미지 picture 객체
    fun setCurrentImageFilePath(position:Int){ // 현재 메인 이미지 filePath 설정
        if (currentImageFilePath != null) // 초기화
            currentImageFilePath = null
        this.currentImageFilePath = imageUriLiveData.value!!.get(position)
    }

    fun setselectedSubImage(picture:Picture?){ // 선택된 서브 이미지 picture 객체 설정
        if (selectedSubImage != null) // 초기화
            selectedSubImage = null
        this.selectedSubImage = picture
    }

    private val galleryObserver = object : ContentObserver(android.os.Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            // 갤러리 변화가 있을 때마다 호출됩니다.
            // 이곳에서 LiveData 값을 변경합니다.
            getAllPhotos()
        }
    }

    init {
        // ContentObserver를 등록합니다.
        context.contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            galleryObserver
        )
    }

    fun setContainer(MCContainer: MCContainer) {
        jpegMCContainer.value = MCContainer
    }

    fun updateImageUriData(uriList: MutableList<String>) {
        this.imageUriList = uriList
        _imageUriLiveData.value = imageUriList
        urlHashMap = HashMap<String,Int>()
        for (i in 0 until uriList.size) {
            urlHashMap.put(uriList[i],i)
        }
    }

    fun getAllPhotos(){
        val cursor = context.contentResolver.query(
            GALLERY_URI,
            null,
            null,
            null,
            MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC")
        val uriList = mutableListOf<String>()
        if(cursor!=null){
            while(cursor.moveToNext()){
                // 사진 경로 FilePath 가져오기
                val uri = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                uriList.add(uri)
            }
            cursor.close()
            updateImageUriData(uriList)
        }
    }

    fun getFilePathIdx(key:String):Int?{
        if (urlHashMap.containsKey(key)){
            return urlHashMap.get(key)
        }
        return null
    }
}