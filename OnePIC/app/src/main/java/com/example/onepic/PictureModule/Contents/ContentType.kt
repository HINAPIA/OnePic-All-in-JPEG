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
    magic(3),
    modified(4),
    edited(5);

    companion object {
        fun fromCode(code: Int) = values().firstOrNull { it.code == code } ?: none
    }
}
