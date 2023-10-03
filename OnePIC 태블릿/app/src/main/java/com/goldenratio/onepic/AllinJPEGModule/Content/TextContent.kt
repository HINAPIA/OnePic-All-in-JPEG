package com.goldenratio.onepic.AllinJPEGModule.Content

import com.goldenratio.onepic.AllinJPEGModule.Content.ContentAttribute
import com.goldenratio.onepic.AllinJPEGModule.Content.Text

class TextContent {
    var textList : ArrayList<Text> = arrayListOf()
    var textCount = 0

    fun init(){
        textList.clear()
        textCount = 0
    }

    fun setContent(contentAttribute: ContentAttribute, textList : ArrayList<String>){
        init()
        for(i in 0..textList.size-1){
            var text = Text(textList.get(i), contentAttribute)
            insertText(text)
        }
    }

    fun setContent(_textList : ArrayList<Text>){
        init()
        textList = _textList
        textCount = _textList.size
    }

    fun insertText(text : Text){
        textList.add(text)
        textCount = textCount + 1
    }
    fun getTextAtIndex(index : Int): Text?{
        return textList.get(index) ?: null
    }

}