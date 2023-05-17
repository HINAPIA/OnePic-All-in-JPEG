package com.goldenratio.onepic

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.goldenratio.onepic.LoadModule.LoadResolver
import com.goldenratio.onepic.PictureModule.MCContainer
import com.goldenratio.onepicdiary.DiaryCellData
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

class JpegViewModel(private val context:Context) : ViewModel() {

    var jpegMCContainer = MutableLiveData<MCContainer>()

    var diaryCellArrayList = arrayListOf<DiaryCellData>()
    var currentUri : Uri? = null

    private var loadResolver : LoadResolver = LoadResolver()

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


    /** MCContainer 변경 */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun setCurrentMCContainer(){
        CoroutineScope(Dispatchers.IO).launch {

            val uri = currentUri

            if(uri != null) {
                val iStream: InputStream? = context.contentResolver.openInputStream(uri)
                var sourceByteArray = getBytes(iStream!!)
                var jop = async {
                    loadResolver.createMCContainer(jpegMCContainer.value!!, sourceByteArray )
                }
                jop.await()
//                jpegViewModel.setCurrentImageFilePath(position) // edit 위한 처리
            }
        }
    }

    @Throws(IOException::class)
    fun getBytes(inputStream: InputStream): ByteArray {
        val byteBuffer = ByteArrayOutputStream()
        val bufferSize = 1024
        val buffer = ByteArray(bufferSize)
        var len = 0
        while (inputStream.read(buffer).also { len = it } != -1) {
            byteBuffer.write(buffer, 0, len)
        }
        byteBuffer.close()
        inputStream.close()
        return byteBuffer.toByteArray()
    }


}