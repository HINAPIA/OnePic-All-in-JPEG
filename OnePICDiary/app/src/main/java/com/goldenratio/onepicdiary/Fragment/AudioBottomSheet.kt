package com.goldenratio.onepicdiary.Fragment

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.goldenratio.onepic.AllinJPEGModule.Contents.ContentAttribute
import com.goldenratio.onepic.AudioModule.AudioResolver
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepicdiary.MainActivity
import com.goldenratio.onepicdiary.R
import com.goldenratio.onepicdiary.databinding.AudioDialogBinding
import com.goldenratio.onepicdiary.databinding.BottomSheetLayoutBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.util.*
class AudioBottomSheet : BottomSheetDialogFragment() , ConfirmDialogInterface{

    private val jpegViewModel by activityViewModels<JpegViewModel>()

    // audio
    var isAudioOn : Boolean = false
    var isPlayingMode : Boolean = true

    // 녹음 중
    var isRecordingMode : Boolean = false
    // 녹음이 완료 되었을 때
    var isRecordedMode : Boolean = false
    var isPlaying : Boolean = false
    var isPlayingEnd : Boolean = false
    private var isDestroy : Boolean = false
    private  var tempAudioFile : File? = null
    private  var preTempAudioFile : File? = null
    private lateinit var audioResolver : AudioResolver
    private lateinit var timerTask: TimerTask
    private var playingTimerTask : TimerTask? = null
    private lateinit var mediaPlayer : MediaPlayer
    private var audioWithContent : Job = Job()

    private lateinit var binding: BottomSheetLayoutBinding
    private lateinit var activity: MainActivity

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as MainActivity
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = BottomSheetLayoutBinding.inflate(inflater, container, false)
        audioResolver = AudioResolver(activity)
        mediaPlayer = audioResolver.mediaPlayer

        if(!isAudioOn){
            isAudioOn = true
            binding.recordingImageView.setImageDrawable(resources.getDrawable(R.drawable.record))

            if(tempAudioFile != null){
                setSeekBar()
            }
        }else{
            isAudioOn = false
        }

        // auido 재생바 설정 - 사진에 들어있던 기존 오디오로 설정
        var savedFile : File? = null
        jpegViewModel.jpegMCContainer.value!!.audioContent.audio?._audioByteArray?.let {
            savedFile = audioResolver.saveByteArrToAacFile(
                it, "original"
            )
        }
        if(savedFile != null){
            tempAudioFile = savedFile
            setSeekBar()
        }else{
            tempAudioFile = null
        }

        // 확인 버튼 클릭시
        binding.checkBtn.setOnClickListener {
            if(tempAudioFile != null)
                saveAudioInMCContainer(tempAudioFile!!)
            audioResolver.audioStop()
        }

        // 백 버튼 클릭 시
        binding.backBtn.setOnClickListener {
            dialog?.dismiss() // 현재 다이얼로그를 닫음
            // findNavController().navigate(R.id.action_audioAddFragment_to_addDiaryFragment)
        }

        binding.recordingImageView.setOnClickListener {
            if(isPlayingMode){
                /* 녹음 시작 */
                binding.playAudioBarLaydout.visibility = View.INVISIBLE
                audioResolver.mediaPlayer.seekTo(0)
                tempAudioFile = null

                binding.recordingImageView.setImageDrawable(resources.getDrawable(R.drawable.stopbutton))

                binding.recordingView.visibility = View.VISIBLE
                Glide.with(this).load(R.raw.audio_wave2).into(binding.recordingView);
                timerUIStart()

                // 녹음 시작
                audioResolver.audioStop()
                audioResolver.startRecording("edit_record")
                isPlayingMode = false
                isRecordingMode = true
            }
            else if(isRecordingMode) {
                /* 녹음 중단 */
                // UI
                binding.recordingView.visibility = View.INVISIBLE

                binding.playAudioBarLaydout.visibility = View.VISIBLE
                binding.recordingImageView.setImageDrawable(resources.getDrawable(R.drawable.refresh_icon3))
                timerUIStop()
                // 녹음 중단
                preTempAudioFile = tempAudioFile
                tempAudioFile = audioStop()

                if(tempAudioFile!= null)
                    setSeekBar()
                isRecordingMode = false
                isRecordedMode = true
            }
            else if(isRecordedMode){
                // dialog
                val dialog = ConfirmDialog(this)
                // 알림창이 띄워져있는 동안 배경 클릭 막기
                dialog.isCancelable = false
                dialog.show(activity.supportFragmentManager, "ConfirmDialog")
            }
        }

        // 오디오 seek bar
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
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


    override fun onYesButtonClick(id: Int) {
        binding.recordingImageView.setImageDrawable(resources.getDrawable(R.drawable.record))
        tempAudioFile = preTempAudioFile
        isRecordedMode = false
        isPlayingMode = true
        if(tempAudioFile == null)
            audioResolver.mediaPlayer.seekTo(0)
        else
            setSeekBar()
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

    fun saveAudioInMCContainer(savedFile : File){
        jpegViewModel.isAddedAudio.value = true
        Log.d("save_audio", "isAddedAudio True")
        //MC Container에 추가
        var auioBytes = audioResolver.getByteArrayInFile(savedFile!!)
        jpegViewModel.jpegMCContainer.value!!.setAudioContent(auioBytes, ContentAttribute.Basic)
        CoroutineScope(Dispatchers.Main).launch {
            binding.RecordingTextView.setText("")
            Toast.makeText(activity, "저장 되었습니다.", Toast.LENGTH_SHORT).show();
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
            binding.RecordingTextView.setTextColor(Color.BLACK)
            binding.RecordingTextView.setTypeface(null, Typeface.BOLD)
            timerTask = object : TimerTask() {
                var cnt = 0
                override fun run() {
                    CoroutineScope(Dispatchers.Main).launch {
                        var string : String = String.format("%02d:%02d", cnt/60, cnt)
                        Log.d("AudioModule", string)
                        binding.RecordingTextView.setText(string)
                        cnt++
                        if(cnt > 30){
                            timerUIStop()
                            audioStop()
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
            CoroutineScope(Dispatchers.Main).launch {
                binding.RecordingTextView.setText("")
                //  Toast.makeText(activity, "녹음이 완료 되었습니다.", Toast.LENGTH_SHORT).show();

            }
        }
    }
}


class ConfirmDialog(confirmDialogInterface: ConfirmDialogInterface) : DialogFragment() {

    // 뷰 바인딩 정의
    private var _binding: AudioDialogBinding? = null
    private val binding get() = _binding!!
    private var confirmDialogInterface: ConfirmDialogInterface? = null

    init {
        this.confirmDialogInterface = confirmDialogInterface
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AudioDialogBinding.inflate(inflater, container, false)
        val view = binding.root

        // 취소 버튼 클릭
        binding.noButton.setOnClickListener {
            dismiss()
        }

        // 확인 버튼 클릭
        binding.yesButton.setOnClickListener {
            this.confirmDialogInterface?.onYesButtonClick(id!!)
            dismiss()
        }
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
interface ConfirmDialogInterface {
    fun onYesButtonClick(id: Int)
}