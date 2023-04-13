package com.example.onepic.AudioModule

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaDataSource
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.service.controls.ControlsProviderService.TAG
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.onepic.PictureModule.Contents.Audio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class AudioResolver(val context : Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var savedFile: File?  = null
    private var inputStream : ByteArrayInputStream? = null



    fun startRecording() : File? {
        if(isRecording){
            return null
        }
        isRecording = true
        Log.d("AudioModule", "녹음 start")
       
        savedFile = getOutputMediaFilePath()

        // 녹음 시작
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            // 해당 파일에 write
            setOutputFile(savedFile!!.path)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioChannels(1)
            try {
                prepare()
                start()
            } catch (e: Exception) {
                Log.e(TAG, "startRecording: ${e.message}")
            }
        }
        return savedFile

    }

    fun stopRecording() : File?{
        if(!isRecording){
            return null
        }
        isRecording = false
        Log.d("AudioModule", "녹음 stop")
        mediaRecorder?.apply {
            stop()
            reset()
            release()
        }
        return savedFile
    }

    fun getByteArrayInFile(audioFile : File) : ByteArray{
        var audioBytes : ByteArray = audioFile.readBytes()
        while (!(audioBytes != null)) {
            Thread.sleep(100)
        }
        return audioBytes
    }

    private fun getOutputMediaFilePath(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val mediaStorageDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val mediaDir = "audio"

        val file = File("${mediaStorageDir?.absolutePath}/$mediaDir/$timeStamp.${"aac"}")
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        return file
    }

    private fun getOutputMediaFilePath2(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val mediaStorageDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val mediaDir = "audio"

        val file = File("${mediaStorageDir?.absolutePath}/$mediaDir/PARSING_$timeStamp.${"aac"}")
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        return file
    }

    fun saveByteArrToAacFile(byteArr: ByteArray) {
        savedFile = getOutputMediaFilePath2()
        val outputStream = FileOutputStream(savedFile)
        Log.d("AudioModule", "파싱 후 저장 완료")
        outputStream.write(byteArr)
        outputStream.flush()
        outputStream.close()
    }


    @RequiresApi(Build.VERSION_CODES.M)
    fun audioPlay(audio : Audio){
        val mediaPlayer = MediaPlayer()
        mediaPlayer.reset()
        var byteData = audio._audioByteArray
        if (byteData == null || byteData.isEmpty()) {
            // _audioByteArray가 null이거나 비어있는 경우에 대한 예외 처리
            Log.e("AudioModule", "Failed to play audio: _audioByteArray is null or empty.")
            return
        }
         CoroutineScope(Dispatchers.Main).launch {
             // MediaDataSource를 구현하는 클래스를 작성한다.
//             val dataSource = @RequiresApi(Build.VERSION_CODES.M)
//             object : MediaDataSource() {
//                 var byteData = audio._audioByteArray
//                 // ByteArray를 ByteArrayInputStream으로 변환한다.
//                 var inputStream = ByteArrayInputStream(byteData)
//                 override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
//                     return inputStream!!.read(buffer, offset, size)
//                 }
//
//                 override fun getSize(): Long {
//                     return byteData?.size!!.toLong()
//                 }
//
//                 override fun close() {
//                     inputStream!!.close()
//                 }
//             }
             // MediaPlayer 인스턴스를 생성하고 오디오 데이터를 설정한다.
             mediaPlayer.apply {
                 setAudioAttributes(
                     AudioAttributes.Builder()
                         .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                         .build())

           // try {
                 setDataSource(savedFile!!.path)
                 prepare()
                     // MediaPlayer를 시작하여 오디오를 재생한다.
                 start()
           //      } catch (e: IOException) {
                     // IOException을 처리하는 코드를 추가한다.
                 //    Log.e("AudioModule", "Failed to prepare media player: ${e.message}")
                //     e.printStackTrace()
             //    }
             }

         }

    }

}