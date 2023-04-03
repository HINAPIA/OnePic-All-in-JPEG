package com.example.onepic

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.camerax.PictureModule.MCContainer
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class JpegViewModel : ViewModel() {

    var jpegMCContainer = MutableLiveData<MCContainer>()

    private var imageUriList = mutableListOf<String>() // 임시 데이터(원본이 사라져도)
    private var imageDrawableList = mutableListOf<Drawable>() // 임시 데이터(원본이 사라져도)

    private val _imageUriLiveData = MutableLiveData<List<String>>() // 내부처리 데이터
    private val _imageDrawableLiveData = MutableLiveData<List<Drawable>>() // 내부처리 데이터

    val imageUriLiveData: LiveData<List<String>> get() = _imageUriLiveData // client 읽기 전용
    val imageDrawableLiveData: LiveData<List<Drawable>> get() = _imageDrawableLiveData // client 읽기 전용


    fun setContainer(MCContainer: MCContainer) {
        jpegMCContainer.value = MCContainer
    }

    fun updateImageUriData(uriList: MutableList<String>) {
        this.imageUriList = uriList
        _imageUriLiveData.value = imageUriList
    }

}