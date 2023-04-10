package com.example.onepic

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.onepic.PictureModule.MCContainer

class JpegViewModel(private val context: Context) : ViewModel() {

    var jpegMCContainer = MutableLiveData<MCContainer>()

    private var imageUriList = mutableListOf<Uri>() // 임시 데이터(원본이 사라져도)
    private val _imageUriLiveData = MutableLiveData<List<Uri>>() // 내부처리 데이터
    val imageUriLiveData: LiveData<List<Uri>> get() = _imageUriLiveData // client 읽기 전용


    fun setContainer(MCContainer: MCContainer) {
        jpegMCContainer.value = MCContainer
    }

    fun updateImageUriData(uriList: MutableList<Uri>) {
        this.imageUriList = uriList
        _imageUriLiveData.value = imageUriList
    }

    fun getAllPhotos(){ // 이미지는 glider 써서 불러와야함
        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            null,
            null,
            null,
            MediaStore.Images.ImageColumns.DATE_ADDED + " DESC")
        val uriList = mutableListOf<Uri>()
        if(cursor!=null){
            while(cursor.moveToNext()){
                // 사진 경로 Uri 가져오기
                val uri = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                //uriList.add(getUriFromPath(uri))
                uriList.add(getUriFromPath(uri))
            }
            cursor.close()
            updateImageUriData(uriList)
        }
    }


    /** FilePath String 을 Uri로 변환 */
    @SuppressLint("Range")
    fun getUriFromPath(filePath: String): Uri { // filePath String to Uri
        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            null, "_data = '$filePath'", null, null)
        var uri: Uri
        if(cursor!=null) {
            cursor!!.moveToNext()
            val id = cursor.getInt(cursor.getColumnIndex("_id"))
            uri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id.toLong()
            )
            cursor.close()
        }
        else {
            return Uri.parse("Invalid path")
        }
        return uri
    }



}