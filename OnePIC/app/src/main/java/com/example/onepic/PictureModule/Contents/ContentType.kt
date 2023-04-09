package com.example.onepic.PictureModule.Contents

enum class ContentType {
    Image,
    Audio,
    Text

}
enum class ContentAttribute(val code: Int ) {
    none(0),
    basic(1),
    focus(2),
    modified(3),
    edited(4);
    companion object {
        fun fromCode(code: Int) = values().firstOrNull { it.code == code } ?: none
    }
}
