package com.goldenratio.onepicdiary.DiaryModule

import android.net.Uri
import android.util.Log

class DiaryCellData(var currentUri: Uri, var year: Int, var month: Int, var day: Int) {
    var titleText: String = ""
    var contentText: String = ""

    override fun toString(): String {
        return "<date>$year/$month/$day</date>" +
                "<contentText>$contentText</contentText>"
    }
}