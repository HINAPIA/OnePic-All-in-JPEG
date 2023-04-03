package com.example.camerax.PictureModule

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.camerax.PictureModule.Contents.ContentAttribute


class Picture(_byteArray: ByteArray, _Content_attribute: ContentAttribute) {
    private lateinit var bitmap : Bitmap
    var pictureByteArray : ByteArray
    var size : Int
    var contentAttribute : ContentAttribute
    var embeddedSize = 0
    var embeddedData : ArrayList<Int>? = null
    var offset = 0
    init {
            pictureByteArray = _byteArray
            size = pictureByteArray.size
            contentAttribute = _Content_attribute
    }
    constructor(
        _offset: Int,
        _byteArray: ByteArray, _Content_attribute: ContentAttribute, _embeddedSize: Int, _embeddedData: ArrayList<Int>?
    ) : this(_byteArray,_Content_attribute){
        offset = _offset
        embeddedSize = _embeddedSize
        embeddedData = _embeddedData
    }

    //추가 데이터를 셋팅하는 함수
    fun insertEmbeddedData(data : ArrayList<Int>){
        this.embeddedData = data
        this.embeddedSize = data.size * 4
    }
    // Byte를 Bitmap으로 변환
    fun byteArrayToBitmap(_byteArray: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(_byteArray, 0, _byteArray.size)
    }
    fun getBitmap():Bitmap{
        return bitmap
    }

}