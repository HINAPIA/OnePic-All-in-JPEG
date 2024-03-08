package com.goldenratio.onepic.AllinJPEGModule.Content

import android.graphics.Bitmap
import android.graphics.BitmapFactory

class Picture(
    var contentAttribute: ContentAttribute,
    var metaData : ByteArray? = null,
    var pictureByteArray: ByteArray? = null) {
    var _mataData : ByteArray? = null
    var _pictureByteArray: ByteArray? = null
    var imageSize: Int = pictureByteArray?.size ?: 0
    var embeddedSize = 0
    var embeddedData: ArrayList<Int>? = null
    var offset = 0

    init {
        if (pictureByteArray != null) {
            _pictureByteArray = pictureByteArray
            imageSize = pictureByteArray!!.size
            pictureByteArray = null
        }
        if(metaData != null){
            _mataData = metaData!!
        }
    }

    constructor(
        offset: Int,
        metaData: ByteArray?,
        byteArray: ByteArray,
        contentAttribute: ContentAttribute,
        embeddedSize: Int,
        embeddedData: ArrayList<Int>?
    ) : this(contentAttribute) {
        this.offset = offset
        this._mataData = metaData
        this.embeddedSize = embeddedSize
        this.embeddedData = embeddedData
        this._pictureByteArray = byteArray
        imageSize = _pictureByteArray!!.size
    }

    override fun toString(): String {
        return "[Picture] offset : ${offset}, Attribute : ${contentAttribute},"+
                " meta Data Size : ${_mataData?.size}, image data size : ${imageSize}," +
                "embbeded Size : ${embeddedSize}, embedded Data : ${embeddedData}"
    }

    // 추가 데이터를 셋팅하는 함수
    fun insertEmbeddedData(data: ArrayList<Int>) {
        this.embeddedData = data
        this.embeddedSize = data.size * 4
    }

    // Byte를 Bitmap으로 변환
    fun byteArrayToBitmap(_byteArray: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(_byteArray, 0, _byteArray.size)
    }

    fun waitForByteArrayInitialized() {
        while (!isByteArrayInitialized()) {
            Thread.sleep(100)
        }
    }

    fun isByteArrayInitialized(): Boolean {
        return _pictureByteArray != null
    }
}