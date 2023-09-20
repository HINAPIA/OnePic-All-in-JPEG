package com.goldenratio.onepic.AllinJPEGModule.Content


class Text (_data : String, _Content_attribute: ContentAttribute) {

    var data : String
    var contentAttribute : ContentAttribute
    init {
        data = _data
        contentAttribute = _Content_attribute
    }

    override fun toString(): String {
        return "[Text] data : ${data}"
    }
}