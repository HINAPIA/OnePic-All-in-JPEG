package com.example.camerax.PictureModule.Info

import com.example.camerax.PictureModule.Picture

class ImageInfo(picture: Picture) {
    var offset : Int = 0
    var dataSize : Int = 0
    var attribute : Int
    var embeddedDataSize : Int = 0
    lateinit var embeddedData : ArrayList<Int>

    init {
        dataSize = picture.size
        attribute = picture.contentAttribute.code
        embeddedDataSize = picture.embeddedSize
        if(embeddedDataSize > 0)
            embeddedData = picture.embeddedData!!
    }

    fun getImageInfoSize() : Int{
        return 16 + embeddedDataSize
    }
}