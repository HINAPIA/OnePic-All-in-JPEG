package com.goldenratio.onepic.EditModule.Fragment

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.goldenratio.onepic.AudioModule.AudioResolver
import com.goldenratio.onepic.ConfirmDialogInterface
import com.goldenratio.onepic.ImageToolModule
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.AllinJPEGModule.Contents.ContentAttribute
import com.goldenratio.onepic.AllinJPEGModule.ImageContent
import com.goldenratio.onepic.R
import com.goldenratio.onepic.ViewerModule.ViewerEditorActivity
import com.goldenratio.onepic.databinding.FragmentAudioAddBinding
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.util.*


class AudioAddFragment : Fragment(), ConfirmDialogInterface {


    private lateinit var activity: ViewerEditorActivity
    private lateinit var binding: FragmentAudioAddBinding
    private lateinit var imageToolModule : ImageToolModule


    private val jpegViewModel by activityViewModels<JpegViewModel>()
    private lateinit var imageContent : ImageContent

    // audio
    var isAudioOn : Boolean = false
    var isPlayingMode : Boolean = true
    // 녹음 중
    var isRecordingMode : Boolean = false
    // 녹음이 완료 되었을 때
    var isRecordedMode : Boolean = false
    var isPlaying : Boolean = false
    var isPlayingEnd : Boolean = false
    var isAbleReset : Boolean = false
    private var isDestroy : Boolean = false
    private  var tempAudioFile : File? = null
    private  var preTempAudioFile : File? = null
    private lateinit var audioResolver :AudioResolver
    private lateinit var timerTask: TimerTask
    private var playingTimerTask : TimerTask? = null
    private lateinit var mediaPlayer : MediaPlayer
    private var audioWithContent : Job = Job()



    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as ViewerEditorActivity
        audioResolver = AudioResolver(activity)
        mediaPlayer = audioResolver.mediaPlayer
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentAudioAddBinding.inflate(inflater, container, false)

        imageContent = jpegViewModel.jpegAiContainer.value?.imageContent!!
        imageToolModule = ImageToolModule()

        val mainBitmap = imageContent.getMainBitmap()

        // imageView 변환
        binding.addMainView.setImageBitmap(mainBitmap)

        // auido 재생바 설정 - 사진에 들어있던 기존 오디오로 설정
        var savedFile : File? = null
        jpegViewModel.jpegAiContainer.value!!.audioContent.audio?._audioByteArray?.let {
            savedFile = audioResolver.saveByteArrToAacFile(
                it, "original"
            )
        }
        if(savedFile != null){
            tempAudioFile = savedFile
        }else{
            tempAudioFile = null
        }


        // close btn 클릭 시
        binding.addCloseBtn.setOnClickListener {
            audioResolver.audioStop()
            findNavController().navigate(R.id.action_audioAddFragment_to_editFragment)
        }

        // info 확인
        binding.addInfoBtn.setOnClickListener {
            imageToolModule.showView(binding.infoDialogLayout, true)

        }
        // info 삭제
        binding.dialogCloseBtn.setOnClickListener {
            imageToolModule.showView(binding.infoDialogLayout, false)
        }

        /* Audio Add */
        if(!isAudioOn){
            isAudioOn = true
            binding.recordingImageView.setImageDrawable(resources.getDrawable(R.drawable.record))
            binding.audioContentLayout.visibility = View.VISIBLE
//            // 재생 바
//            if(tempAudioFile != null){
//                setSeekBar()
//            }
        }else{
            isAudioOn = false
            binding.audioContentLayout.visibility = View.GONE
        }
        binding.playBtn.setOnClickListener {
            // 정지 -> 재생
            if(jpegViewModel.isAudioPlay.value!! == 0){
                jpegViewModel.isAudioPlay.value = 1
                // 오디오 재생
                if(tempAudioFile != null){
                    setSeekBar()
                }

            } else if(jpegViewModel.isAudioPlay.value!! == 1){
                // 재생 -> 정지
                if(isPlaying){
                    jpegViewModel.isAudioPlay.value = 0
                    if (mediaPlayer != null) {
                        mediaPlayer.stop()
                    }
                // 재생 -> replay
                }else
                    jpegViewModel.isAudioPlay.value = 2
            // replay -> 재생
            } else{
                jpegViewModel.isAudioPlay.value = 1
            }
        }
        jpegViewModel.isAudioPlay.observe(viewLifecycleOwner) {
            if(it == 0){
                binding.playBtn.setImageDrawable(resources.getDrawable(R.drawable.play2))
                // 재생 중인데 정지 버튼을 누를 때
                if(isPlaying){
                    if (mediaPlayer != null) {
                        mediaPlayer.stop()
                    }
                }
                // binding.back.visibility = View.VISIBLE
            }else if(it ==1){
                binding.playBtn.setImageDrawable(resources.getDrawable(R.drawable.pause2))
            }else{
                binding.playBtn.setImageDrawable(resources.getDrawable(R.drawable.replay))
            }

        }


        // 오디오 녹음 내역 저장
        binding.audioCheckButton.setOnClickListener {
            // 녹음 내역 저장
            if(tempAudioFile != null){
                saveAudioInMCContainer(tempAudioFile!!)
                jpegViewModel.jpegAiContainer.value!!.audioContent.audio!!._audioByteArray?.let { it1 ->
                    audioResolver.saveByteArrToAacFile(
                        it1, "viewer_record")
                }
            }
            // audioResolver.audioStop()
            imageContent.checkAdded = true

            CoroutineScope(Dispatchers.Main).launch {
                binding.RecordingTextView.setText("")
                Toast.makeText(activity, "저장 되었습니다.", Toast.LENGTH_SHORT).show();
            }
        }

        binding.recordingImageView.setOnClickListener {
            if(isPlayingMode){
                /* 녹음 시작 */
                binding.playAudioBarLaydout.visibility = View.INVISIBLE
              //  binding.rawImageView.visibility = View.VISIBLE
                binding.RecordingTextView.visibility = View.VISIBLE

                //Glide.with(this).load(R.raw.giphy).into(binding.rawImageView);
                binding.recordingImageView.setImageDrawable(resources.getDrawable(R.drawable.stop))
                timerUIStart()


                // 녹음 시작
                audioResolver.audioStop()
                audioResolver.startRecording("edit_record")
                isPlayingMode = false
                isRecordingMode = true
            }
            else if(isRecordingMode) {
                /* 녹음 중단 */
                binding.playAudioBarLaydout.visibility = View.VISIBLE
                //binding.rawImageView.visibility = View.GONE
                binding.RecordingTextView.visibility = View.INVISIBLE
                binding.recordingImageView.setImageDrawable(resources.getDrawable(R.drawable.refresh))
                timerUIStop()
                // 녹음 중단
                preTempAudioFile = tempAudioFile
                tempAudioFile = audioStop()

                if(tempAudioFile!= null)
                    setSeekBar()
                isRecordingMode = false
                isRecordedMode = true

                binding.audioCheckButton.visibility = View.VISIBLE

            }

        }

        // 오디오 seek bar
        binding.seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // 사용자가 시크바를 움직이면
                if (fromUser){
                    if(tempAudioFile == null){
                        audioResolver.mediaPlayer.seekTo(0)
                        return
                    }
                    else{
                        if(mediaPlayer.isPlaying)
                            audioResolver.mediaPlayer.seekTo(progress) // 재생위치를 바꿔준다(움직인 곳에서의 음악재생)
                    }
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                if(tempAudioFile == null){
                    audioResolver.mediaPlayer.seekTo(0)
                    return
                }
                // 플레이
                if(isPlayingEnd){
                    isPlayingEnd= false
                    setSeekBar()
                }
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        return binding.root
    }

    override fun onPause() {
        super.onPause()
        if (mediaPlayer != null) {
            mediaPlayer.pause()
            isDestroy = true
        }
    }

    override fun onYesButtonClick(id: Int) {
        binding.recordingImageView.setImageDrawable(resources.getDrawable(R.drawable.record))
        binding.audioCheckButton.visibility = View.INVISIBLE
        tempAudioFile = preTempAudioFile
        isRecordedMode = false
        isPlayingMode = true

        if(tempAudioFile == null)
            audioResolver.mediaPlayer.seekTo(0)
        else{
            setSeekBar()
            audioResolver.mediaPlayer.seekTo(0)
        }

    }
    override fun onStop() {
        super.onStop()

        if (mediaPlayer != null) {
            isDestroy = true
            mediaPlayer.stop()
            mediaPlayer.release()
            //mediaPlayer = null
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        if(audioWithContent != null){
            isDestroy = true
            audioWithContent.cancel()
        }
        // MediaPlayer 객체를 해제
        if (audioResolver.mediaPlayer != null) {
            audioResolver.mediaPlayer.release()
        }
    }
    fun setSeekBar(){
        isPlaying = true
        audioWithContent = CoroutineScope(Dispatchers.IO).launch {
            mediaPlayer.reset()
            mediaPlayer.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                try {
                    setDataSource(tempAudioFile!!.path)
                    prepare()
                    start()
                } catch (e: IOException) {
                    Log.e("AudioModule", "Failed to prepare media player: ${e.message}")
                    //e.printStackTrace()
                }
            }
            // Seek bar process UI
            withContext(Dispatchers.Main) {
                binding.seekBar.max = audioResolver.mediaPlayer.duration
                // 막대 바가 끝까지 도달 시 해당 코루틴 중단
                mediaPlayer.setOnCompletionListener {
                    Log.d("AudioModule", "재생 중인 코루틴 중단")
                    Log.d("AudioModule", "====================")
                    isDestroy = true
                    isPlayingEnd = true
                    binding.seekBar.clearFocus()
                    binding.seekBar.progress = binding.seekBar.max
                    coroutineContext.cancelChildren()
                }

                playinAudioUIStart(mediaPlayer.duration)
                while (true) {
                    if (mediaPlayer != null) {
                        if(isDestroy) {
                            isDestroy = false
                            break
                        }
                        val currentPosition: Int = mediaPlayer.currentPosition
                        binding.seekBar.progress = currentPosition
                        delay(100)
                    } else {
                        break
                    }
                }
            }
        }
    }


    fun audioStop() : File? {
        // 녹음 중단, 저장
        var savedFile : File?  = audioResolver.stopRecording()
        isRecordingMode = false
        return savedFile
    }
    fun saveAudioInMCContainer(savedFile : File){
        //MC Container에 추가
        var auioBytes = audioResolver.getByteArrayInFile(savedFile!!)
        jpegViewModel.jpegAiContainer.value!!.setAudioContent(auioBytes, ContentAttribute.basic)
    }
    fun playinAudioUIStart(_time : Int){
        if(playingTimerTask != null)
            playingTimerTask!!.cancel()
        var time = _time/1000
        if(isPlaying){
            playingTimerTask = object : TimerTask() {
                var cnt = 0
                override fun run() {
                    CoroutineScope(Dispatchers.Main).launch {
                        var string : String = String.format("%02d:%02d", time/60, time)
                        Log.d("AudioModule", time.toString())
                        binding.playingTextView.setText(string)
                        time -= 1
                        if(time < 0){
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
        CoroutineScope(Dispatchers.Main).launch {
            binding.playingTextView.setText("00:00")
        }
    }

    fun timerUIStart(){
        if(!isRecordingMode){
            timerTask = object : TimerTask() {
                var cnt = 0
                override fun run() {
                    CoroutineScope(Dispatchers.Main).launch {

                        var string : String = String.format("%02d:%02d", cnt/60, cnt)
                        binding.RecordingTextView.setText(string)
                        cnt++

                        if(cnt > 30){
                            timerUIStop()
                        }
                    }
                }
            }
            val timer = Timer()
            timer.schedule(timerTask, 0, 1000)
        }
    }

    fun timerUIStop(){
        if(isRecordingMode){
            timerTask.cancel()
        }
    }
}


