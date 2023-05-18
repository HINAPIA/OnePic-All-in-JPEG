package com.goldenratio.onepic.PictureModule

import android.util.Log
import com.goldenratio.onepic.PictureModule.Contents.ContentAttribute
import com.goldenratio.onepic.PictureModule.Contents.Text

class TextContent {
    var textList : ArrayList<Text> = arrayListOf()
    var textCount = 0
    var currentDate: List<String> = listOf()

    fun init(){
        textList.clear()
        textCount = 0
    }
    fun setContent(contentAttribute: ContentAttribute, textList : ArrayList<String>){
        init()
        for(i in 0 until textList.size){
            val text = Text(textList.get(i), contentAttribute)
            //
            insertText(text)
        }
        setDate()
    }

    fun setContent(_textList : ArrayList<Text>){
        init()
        textList = _textList
        textCount = _textList.size
        setDate()
    }

    fun insertText(text : Text){
        textList.add(text)
        textCount = textCount + 1
    }
    fun getTextAtIndex(index : Int): Text?{
        return textList.get(index) ?: null
    }

    fun getAllText():ArrayList<Text>{
        return textList
    }

    private fun setDate() {

        Log.d("Cell Text","setDate Call")
        if(textList.size !=0) {
            var date = textList[0].data.split("<date>")
            if (date.isNotEmpty())
                date = date[1].split("</date>")
            if (date.isNotEmpty())
                currentDate = date[0].split("/")
        }
    }

    fun getMonth() : Int {
        if(currentDate.isNotEmpty()){
            return Integer.parseInt(currentDate[1])
        }

        return 0
    }

    fun getDay() : Int {
        if(currentDate.isNotEmpty()){
            return Integer.parseInt(currentDate[2])
        }
        return 0
    }

    fun getTitle() : String {
        if(textList.size !=0){
            val title = textList[0].data.split("<title>")
            val finalTitle = title[1].split("</title>")
            return finalTitle[0]
        }
        return ""
    }

    fun getContent() :String {
        if(textList.size !=0) {
            val contentText = textList[0].data.split("<contentText>")
            val finalContentText = contentText[1].split("</contentText>")
            return finalContentText[0]
        }
        return ""
    }
}