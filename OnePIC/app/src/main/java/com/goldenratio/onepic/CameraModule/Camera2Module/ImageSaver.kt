package com.example.test_camera2.CameraHelper

import android.content.Context
import android.media.Image
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.goldenratio.onepic.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

internal class ImageSaver(
    private val image: Image,

    private val previewByteArrayList: MutableLiveData<ArrayList<ByteArray>>
) : Runnable {

    /**
     * 받은 이미지를 ByteArray로 변환해, previewByteArrayList에 추가한다.
     */
    override fun run() {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        CoroutineScope(Dispatchers.Main).launch {
            previewByteArrayList.value?.add(bytes)
            previewByteArrayList.value = previewByteArrayList.value
        }

        image.close()
    }

}
