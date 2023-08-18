package com.goldenratio.onepic.AllinJPEGModule.Contents

enum class ContentType {
    Image,
    Audio,
    Text
}

enum class ContentAttribute(val code: Int ) {
    none(0),
    Basic(1),
    Burst(2),
    object_focus(3),
    distance_focus(4),
    magic(5),
    edited(6);

    companion object {
        fun fromCode(code: Int) = values().firstOrNull { it.code == code } ?: none
    }
}
