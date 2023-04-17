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
   // private var inputStream : ByteArrayInputStream? = null
    val mediaPlayer = MediaPlayer()
    
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
            setAudioSamplingRate(48000) // 샘플링 레이트를 48kHz로 설정
            setAudioEncodingBitRate(192000) // 비트 레이트를 192kbps로 설정
            setAudioChannels(2)
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

    fun savedFileDelete(){
        savedFile?.delete()
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

        val file = File("${mediaStorageDir?.absolutePath}/$mediaDir/create.${"aac"}")
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        return file
    }

    private fun getOutputMediaFilePath2(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val mediaStorageDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val mediaDir = "audio"

        val file = File("${mediaStorageDir?.absolutePath}/$mediaDir/playing.${"aac"}")
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

        mediaPlayer.reset()
        var byteData = audio._audioByteArray
        if (byteData == null || byteData.isEmpty()) {
            // _audioByteArray가 null이거나 비어있는 경우에 대한 예외 처리
            Log.e("AudioModule", "Failed to play audio: _audioByteArray is null or empty.")
            return
        }
         CoroutineScope(Dispatchers.IO).launch {
             // MediaPlayer 인스턴스를 생성하고 오디오 데이터를 설정
             mediaPlayer.apply {
                 setAudioAttributes(
                     AudioAttributes.Builder()
                         .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                         .build())

            try {
                 setDataSource(savedFile!!.path)
                 prepare()
                 // MediaPlayer를 시작하여 오디오를 재생한다.
                 start()
                 } catch (e: IOException) {
                     // IOException을 처리하는 코드를 추가한다.
                     Log.e("AudioModule", "Failed to prepare media player: ${e.message}")
                     e.printStackTrace()
                 }
             }

         }

    }

    fun audioStop(){
        mediaPlayer.stop()
        mediaPlayer.reset()
    }

}