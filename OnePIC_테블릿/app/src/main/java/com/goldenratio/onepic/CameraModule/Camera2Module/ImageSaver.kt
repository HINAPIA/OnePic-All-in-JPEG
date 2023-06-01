package com.example.test_camera2.CameraHelper

import android.content.ContentValues
import android.content.Context
import android.media.Image
import android.media.MediaPlayer
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.goldenratio.onepic.CameraModule.Camera2Module.Camera2
import com.goldenratio.onepic.CameraModule.CameraFragment
import com.goldenratio.onepic.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

internal class ImageSaver(
    private val image: Image,

    private val context: Context,

    private val camera2: Camera2
) : Runnable {

    override fun run() {

        val mediaPlayer = MediaPlayer.create(context, R.raw.end_sound)

        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        camera2.previewByteArrayList.add(bytes)

        try {
//            mediaPlayer.start()
        } catch (e: IOException) {
            Log.e(TAG, e.toString())
        } finally {
            image.close()
        }

    }

    companion object {

        private val TAG = "ImageSaver"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}
