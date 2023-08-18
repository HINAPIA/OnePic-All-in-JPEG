package com.goldenratio.onepic.AudioModule

import com.example.demo.view.SubImagesView
import javafx.scene.layout.StackPane
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tornadofx.*
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
    var isPlaying = false

    private var playingTimerTask : TimerTask? = null
    var subImagesView: SubImagesView? = null

    var currentTime = 0

    val audioSourcePath = "src/main/kotlin/com/example/demo/resource/audio/"

    var audioView : StackPane = StackPane()

    companion object{
        var isOn = false
    }

    fun prepare(){
        CoroutineScope(Dispatchers.Default).launch {
            println("audio : prepare()")
            var filePath: String? =  null
            if(savedFile == null){
                savedFile = getOutputMediaFilePath("record")
            }
            val file = File(audioSourcePath + "record.wav")
            val uri = file.toURI().toString()
            media = Media(uri)
            mediaPlayer = MediaPlayer(media)
            mediaPlayer.setOnEndOfMedia {
                CoroutineScope(Dispatchers.Default).launch {
                    println("재생 끝")
                    isPlaying = false
                    mediaPlayer.stop()
                    audioView.apply{
                        audioView.style{
                            borderWidth += box(0.px)
                            borderColor += box(c("#EA2424"))
                        }

                    }
                }
            }
            mediaPlayer?.setOnReady {
                println("Audio: Ready to play")
                currentTime = (mediaPlayer.totalDuration.toMillis()).toInt()/1000
                var string : String = String.format("%02d:%02d", currentTime/60, currentTime)
                if(subImagesView != null){
                    subImagesView!!.setAudioTextLabel(string)
                }

            }
        }

    }

    fun play() {
        if (!isPlaying) {
            println("audio : 오디오 재생 시작")
            isPlaying = true
            playinAudioUIStart(currentTime)
            mediaPlayer?.play()

        }
    }

    fun playinAudioUIStart(_time : Int){
        if(playingTimerTask != null)
            playingTimerTask!!.cancel()
        var time = _time
        if(isPlaying){
            var string : String = String.format("%02d:%02d", time/60, time)
            if(subImagesView != null)
                subImagesView!!.setAudioTextLabel(string)
            playingTimerTask = object : TimerTask() {
                var cnt = 0
                override fun run() {
                    CoroutineScope(Dispatchers.Default).launch {
                        var string : String = String.format("%02d:%02d", time/60, time)
                        subImagesView!!.setAudioTextLabel(string)
                        time -= 1
                        if(time <-1){
                            playinAudioUIStop()
                        }
                    }
                }
            }
            val timer = Timer()
            timer.schedule(playingTimerTask, 0, 1000)
        }
    }

    fun playinAudioUIStop(){
        if(playingTimerTask != null)
            playingTimerTask!!.cancel()
        CoroutineScope(Dispatchers.Default).launch {
            var time = currentTime
            var string : String = String.format("%02d:%02d", time/60, time)
            subImagesView!!.setAudioTextLabel(string)
        }
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

        val file = File(audioSourcePath+"$fileName.${"wav"}")
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


//




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