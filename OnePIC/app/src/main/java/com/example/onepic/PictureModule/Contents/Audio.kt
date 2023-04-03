package com.example.camerax.PictureModule.Contents

import android.util.Log

class Audio (_audioByteArray : ByteArray, _Content_attribute: ContentAttribute){
    var audioByteArray : ByteArray
    var attribute = _Content_attribute
    var size : Int = 0
    init {
        audioByteArray = _audioByteArray
        attribute = _Content_attribute
        size = audioByteArray.size
        Log.d("Picture Module",
            "[create Audio]size :${audioByteArray.size}")
    }
    fun getInfoLength() : Int{
        // offset(4) + attribute(4) + size(4)
        return 12
    }
}