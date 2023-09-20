package com.goldenratio.camerax.PictureModule.Info

import com.goldenratio.onepic.AllinJPEGModule.Content.AudioContent
import com.goldenratio.onepic.AllinJPEGModule.Content.ContentAttribute
import java.nio.ByteBuffer

class AudioContentInfo(audioContent: AudioContent, audioDataStartOffset : Int) {
    var contentInfoSize : Int = 0
    var dataStartOffset : Int = 0
    var datasize : Int = 0
    var attribute : Int

    companion object{
        val FIELD_SIZE = 4
        val XOA_MARKER_SIZE = 2
    }

    init{
        // contentInfo size
        contentInfoSize = FIELD_SIZE * 4
        dataStartOffset = audioDataStartOffset
        // size
        if(audioContent.audio != null){
            attribute = audioContent.audio!!.attribute.code
            datasize = XOA_MARKER_SIZE + audioContent.audio!!._audioByteArray!!.size
        }else {
            attribute = ContentAttribute.basic.code
            datasize = 0
        }
    }
    fun converBinaryData(): ByteArray {
        val buffer: ByteBuffer = ByteBuffer.allocate(contentInfoSize)
        //Audio Content
        buffer.putInt(contentInfoSize)
        buffer.putInt(dataStartOffset)
        buffer.putInt(attribute)
        buffer.putInt(datasize)
        return buffer.array()
    }
    fun getLength() : Int {
        return contentInfoSize
    }
}