package com.example.camerax.PictureModule

import com.example.camerax.PictureModule.Contents.ContentAttribute
import com.example.camerax.PictureModule.Contents.Audio

class AudioContent {
    var audio : Audio? = null
    var length = 0
    init {

    }

    fun init() {
        audio = null
        length = 0
    }
    fun setContent(byteArray: ArrayList<ByteArray>, contentAttribute: ContentAttribute){
        // audio 객체 생성
        var audio = Audio(byteArray.get(0),contentAttribute)
        this.audio = audio
        length = audio.audioByteArray.size
    }

}