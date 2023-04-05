package com.example.onepic.PictureModule.Contents

import android.util.Log
import com.example.onepic.PictureModule.Contents.ContentAttribute


class Text (_data : String, _Content_attribute: ContentAttribute) {

    var data : String
    var contentAttribute : ContentAttribute
    init {
        data = _data
        contentAttribute = _Content_attribute
        Log.d("Picture Module",
            "[create Text]")
    }


}