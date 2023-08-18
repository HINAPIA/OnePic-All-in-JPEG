package com.goldenratio.onepic.AllinJPEGModule.Contents

class Picture(
    var contentAttribute: ContentAttribute,
    var app1Segment : ByteArray? = null,
    var pictureByteArray: ByteArray? = null) {
    var _app1Segment : ByteArray? = null
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
        if(app1Segment != null){
            _app1Segment = app1Segment!!
        }
    }

    constructor(
        offset: Int,
        app1Segment: ByteArray?,
        byteArray: ByteArray,
        contentAttribute: ContentAttribute,
        embeddedSize: Int,
        embeddedData: ArrayList<Int>?
    ) : this(contentAttribute) {
        this.offset = offset
        this._app1Segment = app1Segment
        this.embeddedSize = embeddedSize
        this.embeddedData = embeddedData
        this._pictureByteArray = byteArray
        imageSize = _pictureByteArray!!.size
    }


    override fun toString(): String {
        return "[Picture] offset : ${offset}, Attribute : ${contentAttribute},"+
                " app1 Data Size : ${_app1Segment?.size}, image data size : ${imageSize}," +
                "embbeded Size : ${embeddedSize}, embedded Data : ${embeddedData}"
    }

    // 추가 데이터를 셋팅하는 함수
    fun insertEmbeddedData(data: ArrayList<Int>) {
        this.embeddedData = data
        this.embeddedSize = data.size * 4
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