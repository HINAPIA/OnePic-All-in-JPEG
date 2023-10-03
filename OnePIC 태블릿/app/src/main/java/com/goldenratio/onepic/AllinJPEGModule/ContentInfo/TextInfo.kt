package com.goldenratio.camerax.PictureModule.Info

import com.goldenratio.onepic.AllinJPEGModule.Content.Text

class TextInfo(text: Text) {
    var offset : Int = 0
    var dataSize : Int = 0
    var data : String
    var attribute : Int

    init {
        dataSize = (text.data.length)*2
        data = text.data
        attribute = text.contentAttribute.code
    }

    fun getTextInfoSize() : Int{
        // Int(4) X 3
        return 4*3
    }
}