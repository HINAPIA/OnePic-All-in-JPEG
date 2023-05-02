package com.example.onepic.EditModule.Fragment

import android.app.Activity
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.onepic.AudioModule.AudioResolver
import com.example.onepic.ImageToolModule
import com.example.onepic.JpegViewModel
import com.example.onepic.PictureModule.Contents.ContentAttribute
import com.example.onepic.PictureModule.Contents.Picture
import com.example.onepic.PictureModule.ImageContent
import com.example.onepic.R
import com.example.onepic.ViewerModule.ViewerEditorActivity
import com.example.onepic.databinding.AudioDialogBinding
import com.example.onepic.databinding.FragmentAddBinding
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.util.*


class AddFragment : Fragment(), ConfirmDialogInterface {
    var testCount = 0

    private lateinit var activity: ViewerEditorActivity
    private lateinit var binding: FragmentAddBinding
    private lateinit var imageToolModule : ImageToolModule
    private lateinit var mainPicture : Picture

    protected val jpegViewModel by activityViewModels<JpegViewModel>()
    protected lateinit var imageContent : ImageContent

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


    // text
    var isTextOn : Boolean = false
    var textList : ArrayList<String> = arrayListOf()


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
        binding = FragmentAddBinding.inflate(inflater, container, false)

        imageContent = jpegViewModel.jpegMCContainer.value?.imageContent!!
        imageToolModule = ImageToolModule()
        textInit()
        // main Picture의 byteArray를 bitmap 제작
//        mainPicture = imageContent.mainPicture
//        var mainBitmap = ImageToolModule().byteArrayToBitmap(imageContent.getJpegBytes(mainPicture))
        val mainBitmap = imageContent.getMainBitmap()

        // imageView 변환
        binding.addMainView.setImageBitmap(mainBitmap)

        // auido 재생바 설정 - 사진에 들어있던 기존 오디오로 설정
        var savedFile : File? = null
        jpegViewModel.jpegMCContainer.value!!.audioContent.audio?._audioByteArray?.let {
            savedFile = audioResolver.saveByteArrToAacFile(
                it, "original"
            )
        }
        if(savedFile != null){
            tempAudioFile = savedFile
        }else{
            tempAudioFile = null
        }

        // save btn 클릭 시
        binding.addSaveBtn.setOnClickListener {
           // if(isRecording){
            if(tempAudioFile != null)
                saveAudioInMCContainer(tempAudioFile!!)
           // }
            audioResolver.audioStop()
            imageContent.checkAddAttribute = true
            findNavController().navigate(R.id.action_addFragment_to_editFragment)
        }

        // close btn 클릭 시
        binding.addCloseBtn.setOnClickListener {
            audioResolver.audioStop()
            findNavController().navigate(R.id.action_addFragment_to_editFragment)
        }

        // text btn 클릭 시
        binding.textAddBtn.setOnClickListener {
            if(!isTextOn){
                audioResolver.audioStop()
                if(isAudioOn){
                    if(isRecordingMode) {
                        /* 녹음 중단 후 저장*/
                        timerUIStop()
                        tempAudioFile = audioStop()
                        isRecordingMode = false
                        isRecordedMode = true
                    }
                    isAudioOn = false

                    binding.contentLayout.visibility = View.GONE
                }
                isTextOn = true
                //binding.recordingImageView.setImageDrawable(resources.getDrawable(R.drawable.record))
                binding.textContentLayout.visibility = View.VISIBLE
                // text 입력 UI에 기존의 텍스트 메시지 띄우기
                var textList = jpegViewModel.jpegMCContainer.value!!.textContent.textList
                if(textList != null && textList.size !=0){
                    binding.editText.setText(textList.get(0).data)
                }
            }else{
                textInit()
                binding.textContentLayout.visibility = View.GONE
            }

        }
        // Edit Text에 포커스가 갔을 시
        binding.editText.onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                //method
                binding.checkButton.visibility = View.VISIBLE
            } else{
                binding.checkButton.visibility = View.INVISIBLE
            }
        }

        // text의 수정 클릭 시
        binding.checkButton.setOnClickListener {
            var textMessage: String = binding.editText.text.toString()
            var textList: ArrayList<String> = arrayListOf()
            textList.add(textMessage)
            if (textMessage != "") {
                jpegViewModel.jpegMCContainer.value!!.setTextConent(
                    ContentAttribute.basic,
                    textList
                )
                CoroutineScope(Dispatchers.Main).launch {
                    // 키보드 내리기
                    val imm: InputMethodManager? =
                        activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(binding.editText.getWindowToken(), 0)
                    }
                    binding.editText.clearFocus()
                    Toast.makeText(activity, "수정 되었습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        }
        /* Audio Add */

        // audio reset btn 클릭 시
        binding.audioResetBtn.setOnClickListener {
            Log.d("AudioModule", "reset btn click")
            if(isAbleReset){
                Log.d("AudioModule", "reset in btn click")
                tempAudioFile =  audioResolver.getOutputMediaFilePath("original")
                audioWithContent.cancel()
                setSeekBar()

            }
        }
        // audio btn 클릭 시
        binding.audioAddBtn.setOnClickListener {
            if(!isAudioOn){
                if(isTextOn){
                    textInit()
                    binding.textContentLayout.visibility = View.GONE
                }
                isAudioOn = true

                binding.recordingImageView.setImageDrawable(resources.getDrawable(R.drawable.record))
                binding.contentLayout.visibility = View.VISIBLE
                if(!isAbleReset){
                    binding.audioResetBtn.visibility = View.GONE
                } else{
                    binding.audioResetBtn.visibility = View.VISIBLE
                }

                //binding.seekBar.visibility = View.VISIBLE
                // 재생 바
                if(tempAudioFile != null){
                    setSeekBar()
                }
            }else{
                isAudioOn = false
                binding.contentLayout.visibility = View.GONE
            }

        }

        binding.recordingImageView.setOnClickListener {
            if(isPlayingMode){
                /* 녹음 시작 */
                // UI
//                binding.seekBar.visibility = View.GONE
//                binding.playingTextView.visibility = View.GONE
                binding.playAudioBarLaydout.visibility = View.GONE
                binding.audioResetBtn.visibility = View.GONE
                Glide.with(this).load(R.raw.giphy).into(binding.recordingImageView);
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
                binding.playAudioBarLaydout.visibility = View.VISIBLE
                binding.recordingImageView.setImageDrawable(resources.getDrawable(R.drawable.refresh_icon))
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
        binding.seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // 사용자가 시크바를 움직이면
                if (fromUser){
                    if(tempAudioFile == null){
                        audioResolver.mediaPlayer.seekTo(0)
                        return
                    }
                  if(mediaPlayer.isPlaying)
                      audioResolver.mediaPlayer.seekTo(progress) // 재생위치를 바꿔준다(움직인 곳에서의 음악재생)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                if(tempAudioFile == null){
                    audioResolver.mediaPlayer.seekTo(0)
                    return
                }
                Log.d("AudioModule", "onStartTrackingTouch 호출")
                // 플레이
               // if(binding.seekBar.progress >= binding.seekBar.max){
                if(isPlayingEnd){
                    isPlayingEnd= false
                    Log.d("AudioModule", "모두 재생 후 setSeekBar 호출")
                    setSeekBar()
                }
                //}
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
    fun setSeekBar(){
        isPlaying = true
        Log.d("AudioModule", "setSeekBar(${++testCount})")
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

    fun textInit(){
        isTextOn = false
        textList.clear()
        CoroutineScope(Dispatchers.Main).launch{
            binding.editText.setText("")
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
        jpegViewModel.jpegMCContainer.value!!.setAudioContent(auioBytes, ContentAttribute.basic)
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
                        if(time <=0){
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
            CoroutineScope(Dispatchers.Main).launch {
                binding.RecordingTextView.setText("")
                Toast.makeText(activity, "녹음이 완료 되었습니다.", Toast.LENGTH_SHORT).show();
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
