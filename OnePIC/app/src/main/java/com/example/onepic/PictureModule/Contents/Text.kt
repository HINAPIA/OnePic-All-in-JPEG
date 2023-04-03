package com.example.camerax.PictureModule.Contents

import android.util.Log


class Text (_byteArray : ByteArray, _Content_attribute: ContentAttribute) {

    var textByteArray : ByteArray
    var contentAttribute : ContentAttribute
    var size : Int
    private var length : Int = 0
    init {
        textByteArray = _byteArray
        contentAttribute = _Content_attribute
        size = textByteArray.size
        Log.d("Picture Module",
            "[create Text]size :${textByteArray.size}")
    }


}