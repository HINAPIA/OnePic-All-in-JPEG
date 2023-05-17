package com.goldenratio.onepic

import android.content.Context
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.goldenratio.onepic.PictureModule.MCContainer

class JpegViewModel(private val context:Context) : ViewModel() {

    var jpegMCContainer = MutableLiveData<MCContainer>()
    var currentUri : Uri? = null

    var year = 0
    var month = 0
    var day = 0

    private lateinit var pictureByteArrayList: MutableList<ByteArray> // pictureByteArrayList

    fun setpictureByteArrList(byteArrayList: MutableList<ByteArray>) {
        pictureByteArrayList = byteArrayList
    }

    fun getPictureByteArrList(): MutableList<ByteArray> {
        return pictureByteArrayList
    }

    fun setContainer(MCContainer: MCContainer) {
        jpegMCContainer.value = MCContainer
    }

    fun resetDate(){
        year = 0
        month = 0
        day = 0
    }

}