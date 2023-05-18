package com.goldenratio.onepic.PictureModule.Contents

import com.goldenratio.onepic.PictureModule.Contents.ContentAttribute


class Text (_data : String, _Content_attribute: ContentAttribute) {

    var data : String
    var contentAttribute : ContentAttribute
    init {
        data = _data
        contentAttribute = _Content_attribute
        println("Picture Module, [create Text]")
    }


}