package com.example.camerax.PictureModule.Info

import com.example.onepic.PictureModule.Contents.Text

class TextInfo(text: Text) {
    var dataSize : Int = 0
    var data : String
    var attribute : Int

    init {
        dataSize = text.data.length
        data = text.data
        attribute = text.contentAttribute.code
    }

    fun getTextInfoSize() : Int{
        // Int(4) X 3
        return 8 + dataSize*2
    }
}