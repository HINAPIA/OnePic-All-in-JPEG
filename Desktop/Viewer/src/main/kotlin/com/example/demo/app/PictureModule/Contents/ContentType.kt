package com.goldenratio.onepic.PictureModule.Contents

enum class ContentType {
    Image,
    Audio,
    Text
}

enum class ActivityType {
    Camera,
    Viewer
}

enum class ContentAttribute(val code: Int ) {
    none(0),
    Basic(1),
    Burst(2),
    object_focus(3),
    Focus(4),
    magic(5),
    edited(6);

    companion object {
        fun fromCode(code: Int) = values().firstOrNull { it.code == code } ?: none
    }
}
