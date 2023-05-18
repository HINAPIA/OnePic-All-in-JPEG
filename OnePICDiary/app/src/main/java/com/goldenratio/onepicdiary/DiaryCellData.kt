package com.goldenratio.onepicdiary

import android.net.Uri
import android.util.Log

class DiaryCellData(var currentUri: Uri, var year: Int, var month: Int, var day: Int) {
    var titleText: String = ""
    var contentText: String = ""

    override fun toString(): String {

        return "<date>$year/$month/$day</date>" +
                "<title>$titleText</title>" +
                "<contentText>$contentText</contentText>"
    }
}