package com.example.camerax.PictureModule.Info

import com.example.camerax.PictureModule.GroupContent
import java.nio.ByteBuffer

class GroupContentInfo (_groupContent : GroupContent, _groupStartOffset : Int){
    var groupStartOffset : Int

    var groupInfoLength : Int = 0
    var imageContentInfo : ImageContentInfo
    var audioContentInfo : AudioContentInfo
    var textContentInfo: TextContentInfo
    var groupContent : GroupContent

    init {
        groupStartOffset = _groupStartOffset
        groupContent = _groupContent

        imageContentInfo = ImageContentInfo(groupContent.imageContent,0)
        textContentInfo = TextContentInfo(groupContent.textContent,imageContentInfo.getEndOffset() +1)
        audioContentInfo = AudioContentInfo(groupContent.audioContent,imageContentInfo.getEndOffset()+1)
    }

    fun getInfoLength() : Int{
        groupInfoLength = 8 + imageContentInfo.getLength() + audioContentInfo.getLength() +textContentInfo.getLength()
        return 8 + imageContentInfo.getLength() + audioContentInfo.getLength() +textContentInfo.getLength()
    }

    fun convertBinaryData() : ByteArray {
        val buffer: ByteBuffer = ByteBuffer.allocate(getInfoLength())
        buffer.putInt(groupStartOffset)
        buffer.putInt(groupInfoLength)
        buffer.put(imageContentInfo.converBinaryData())
        buffer.put(textContentInfo.converBinaryData())
        buffer.put(audioContentInfo.converBinaryData())
        return buffer.array()
    }


}