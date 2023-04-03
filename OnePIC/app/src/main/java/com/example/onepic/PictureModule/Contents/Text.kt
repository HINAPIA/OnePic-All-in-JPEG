package com.example.camerax.PictureModule.Contents

import android.util.Log


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