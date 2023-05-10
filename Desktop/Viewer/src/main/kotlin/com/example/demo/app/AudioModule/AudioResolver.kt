package com.goldenratio.onepic.AudioModule

import com.goldenratio.onepic.PictureModule.Contents.Audio
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import javafx.util.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.*


class AudioResolver() {
    //private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    var savedFile: File?  = null
   // private var inputStream : ByteArrayInputStream? = null
   private lateinit var mediaPlayer : MediaPlayer
    //private val media = Media(filePath)
    private lateinit var media : Media
    var mediaView : MediaView = MediaView()
    private var isPlaying = false

    companion object{
        var isOn = false
    }

    fun prepare(){
        println("audio : prepare()")
        var filePath: String? =  null
        if(savedFile == null){
            savedFile = getOutputMediaFilePath("record")
        }
        val file = File("src/audio/record.aac")
        val uri = file.toURI().toString()
//        val mediaPlayer = MediaPlayer()
//        filePath = savedFile!!.path
       // println("filePath : ${filePath}")
        media = Media(uri)
        mediaPlayer = MediaPlayer(media)
        mediaView.mediaPlayer = mediaPlayer


        println("audio : 오디오 준비 끝")
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

    fun getOutputMediaFilePath(fileName : String): File {
        //val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val mediaDir = "audio"

        val file = File("src/$mediaDir/$fileName.${"aac"}")
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        return file
    }


    /** byteArray로 나타낸 오디오 데이터를 파일로 저장하는 함수**/
    fun saveByteArrToAacFile(byteArr: ByteArray) : File{
        var fileName = "record"
        savedFile = getOutputMediaFilePath(fileName)
        val outputStream = FileOutputStream(savedFile)
        println("Audio : ${savedFile!!.path.toString()}에 오디오 저장 완료")
        outputStream.write(byteArr)
        outputStream.flush()
        outputStream.close()
        return savedFile as File
    }

    fun play() {
        //prepare()
        if (!isPlaying) {
            println("audio : 오디오 재생 시작")
            mediaPlayer.run { play() }
            //isPlaying = true
        }
    }

    fun pause() {
        if (isPlaying) {
            mediaPlayer.pause()
            isPlaying = false
        }
    }

    fun stop() {
        mediaPlayer.stop()
        isPlaying = false
    }

    fun seekTo(time: Duration) {
        mediaPlayer.seek(time)
    }

    //fun getMediaView() = mediaView


//    fun audioPlay(_audio : Audio){
//        var audio = _audio
//        //mediaPlayer.
//        var byteData = audio._audioByteArray
//        if (byteData == null || byteData.isEmpty()) {
//            // _audioByteArray가 null이거나 비어있는 경우에 대한 예외 처리
//           // Log.e("AudioModule", "Failed to play audio: _audioByteArray is null or empty.")
//            return
//        }
//         CoroutineScope(Dispatchers.IO).launch {
//             // MediaPlayer 인스턴스를 생성하고 오디오 데이터를 설정
//             mediaPlayer.apply {
//                 setAudioAttributes(
//                     AudioAttributes.Builder()
//                         .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
//                         .build())
//            try {
//                 setDataSource(savedFile!!.path)
//                 prepare()
//                 // MediaPlayer를 시작하여 오디오를 재생한다.
//                 start()
//                 } catch (e: IOException) {
//                     // IOException을 처리하는 코드를 추가한다.
//                     Log.e("AudioModule", "Failed to prepare media player: ${e.message}")
//                     e.printStackTrace()
//                 }
//             }
//         }
//    }



//    fun audioStop(){
//        mediaPlayer.stop()
//        mediaPlayer.reset()
//    }


}