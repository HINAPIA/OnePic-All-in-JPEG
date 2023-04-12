package com.example.onepic

import android.content.Context
import android.database.ContentObserver
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
    val imageUriLiveData: LiveData<List<String>> get() = _imageUriLiveData // client 읽기 전용


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

}