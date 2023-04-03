package com.example.camerax.PictureModule

import com.example.camerax.PictureModule.Contents.ContentAttribute
import com.example.camerax.PictureModule.Contents.Text

class TextContent {
    var textList : ArrayList<Text> = arrayListOf()
    var textCount = 0

    fun init(){
        textList.clear()
        textCount = 0
    }

    fun setContent(byteArrayList: ArrayList<ByteArray>, contentAttribute : ContentAttribute){
        init()
        for(i in 0..byteArrayList.size-1){
            var text = Text(byteArrayList.get(i), contentAttribute)
            insertText(text)
        }
    }
    fun addContent(byteArrayList: ArrayList<ByteArray>, contentAttribute : ContentAttribute){
        for(i in 0..byteArrayList.size-1){
            var text = Text(byteArrayList.get(i), contentAttribute)
            insertText(text)
        }
    }
    fun insertText(text : Text){
        textList.add(text)
        textCount = textCount + 1
    }
    fun getTextAtIndex(index : Int): Text?{
        return textList.get(index) ?: null
    }
}