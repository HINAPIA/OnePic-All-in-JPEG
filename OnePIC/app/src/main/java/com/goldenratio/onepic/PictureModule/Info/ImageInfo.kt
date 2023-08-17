package com.goldenratio.camerax.PictureModule.Info

import com.goldenratio.onepic.PictureModule.Contents.Picture

class ImageInfo(picture: Picture) {
    var app1DataSize : Int = 0
    var offset : Int = 0
    var imageDataSize : Int = 0
    var attribute : Int
    var embeddedDataSize : Int = 0
    lateinit var embeddedData : ArrayList<Int>

    init {
        app1DataSize = picture._app1Segment!!.size
        imageDataSize = picture.imageSize
        attribute = picture.contentAttribute.code
        embeddedDataSize = picture.embeddedSize
        if(embeddedDataSize > 0)
            embeddedData = picture.embeddedData!!
    }

    fun getImageInfoSize() : Int{
        return 20 + embeddedDataSize
    }
}