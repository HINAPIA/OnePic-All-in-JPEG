package com.goldenratio.camerax.PictureModule.Info

import com.goldenratio.onepic.AllinJPEGModule.Content.Picture

class ImageInfo(picture: Picture) {
    var metaDataSize : Int = 0
    var offset : Int = 0
    var imageDataSize : Int = 0
    var attribute : Int
    var embeddedDataSize : Int = 0
    lateinit var embeddedData : ArrayList<Int>

    init {
        if(picture._mataData != null){
            metaDataSize = picture._mataData!!.size
        }
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