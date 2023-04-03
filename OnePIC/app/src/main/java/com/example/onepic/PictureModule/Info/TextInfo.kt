package com.example.camerax.PictureModule.Info

import com.example.camerax.PictureModule.Contents.Text

class TextInfo(text: Text) {
    var offset : Int = 0
    var dataSize : Int = 0
    var attribute : Int

    init {
        dataSize = text.size
        attribute = text.contentAttribute.code
    }

    fun getImageInfoSize() : Int{
        // Int(4) X 3
        return 12
    }
}