package com.goldenratio.onepic.AllinJPEGModule

import android.util.Log
import com.goldenratio.onepic.AllinJPEGModule.Contents.ContentAttribute
import com.goldenratio.onepic.AllinJPEGModule.Contents.Audio

class AudioContent {
    var audio : Audio? = null

    fun init() {
        audio = null
    }
    fun setContent(byteArray:ByteArray, contentAttribute: ContentAttribute){
        init()
        Log.d("audio_test", "audio SetContent, audio 객체 갱신")
        // audio 객체 생성
        audio = Audio(byteArray, contentAttribute)
        audio!!.waitForByteArrayInitialized()
        Log.d("audio_test", "setContent : 오디오 크기 ${audio!!.size}")
    }
    fun setContent(_audio:Audio){
        init()
        audio = _audio
        audio!!.waitForByteArrayInitialized()
    }

}