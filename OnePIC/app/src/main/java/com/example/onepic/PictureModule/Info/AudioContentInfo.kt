package com.example.camerax.PictureModule.Info

import com.example.camerax.PictureModule.AudioContent
import com.example.camerax.PictureModule.Contents.ContentAttribute
import java.nio.ByteBuffer
import java.security.KeyStore.Entry.Attribute

class AudioContentInfo(audioContent: AudioContent , startOffset : Int) {
    var contentInfoSize : Int = 0
    var dataStartOffset : Int = 0
    var datasize : Int = 0
    var attribute : Int

    init{
        // contentInfo size
        contentInfoSize = 16
        dataStartOffset = startOffset
        // size
        if(audioContent.audio != null){
            attribute = audioContent.audio!!.attribute.code
            datasize = audioContent.audio!!.size
        }else {
            attribute = ContentAttribute.none.code
            datasize = 0
        }
    }
    fun converBinaryData(): ByteArray {
        val buffer: ByteBuffer = ByteBuffer.allocate(contentInfoSize)
        //Image Content
        buffer.putInt(contentInfoSize)
        buffer.putInt(dataStartOffset)
        buffer.putInt(datasize)
        buffer.putInt(attribute)
        return buffer.array()
    }
    fun getLength() : Int {

        return contentInfoSize
    }
}