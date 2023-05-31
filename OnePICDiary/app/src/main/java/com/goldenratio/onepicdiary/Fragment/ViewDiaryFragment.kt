package com.goldenratio.onepicdiary.Fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.goldenratio.onepic.AudioModule.AudioResolver
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.PictureModule.Contents.ContentAttribute
import com.goldenratio.onepic.PictureModule.ImageContent
import com.goldenratio.onepic.PictureModule.TextContent
import com.goldenratio.onepicdiary.DiaryModule.LayoutToolModule
import com.goldenratio.onepicdiary.DiaryModule.ViewPagerAdapter
import com.goldenratio.onepicdiary.MagicPictureModule.MagicPictureModule
import com.goldenratio.onepicdiary.MainActivity
import com.goldenratio.onepicdiary.R
import com.goldenratio.onepicdiary.databinding.FragmentViewDiaryBinding
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class ViewDiaryFragment : Fragment() {

    private lateinit var binding: FragmentViewDiaryBinding
    private val jpegViewModel by activityViewModels<JpegViewModel>()
    private lateinit var imageContent : ImageContent

    private lateinit var textContent: TextContent
    private lateinit var layoutToolModule: LayoutToolModule

    private var month = MutableLiveData<Int>()
    private var day =  MutableLiveData<Int>()

    private lateinit var viewPagerAdapter:ViewPagerAdapter

    private lateinit var magicPictureModule: MagicPictureModule

    val handler = Handler()

    var magicPlaySpeed: Long = 100

    private var isMagicPlay = false
    private var isViewUnder = false

    private var overlayBitmap = arrayListOf<Bitmap>()

    /* Audio */
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

    private lateinit var activity: MainActivity

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as MainActivity
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment

        binding = FragmentViewDiaryBinding.inflate(inflater, container, false)
        audioResolver = AudioResolver(activity)
        mediaPlayer = audioResolver.mediaPlayer

        layoutToolModule = LayoutToolModule()

        jpegViewModel.isAudioPlay.observe(viewLifecycleOwner) {
            if(it == 0){
                binding.playBtn.setImageDrawable(resources.getDrawable(R.drawable.play))
               // binding.back.visibility = View.VISIBLE

            }else if(it == 1){
                binding.playBtn.setImageDrawable(resources.getDrawable(R.drawable.pause))

            } else{
                binding.playBtn.setImageDrawable(resources.getDrawable(R.drawable.re))
            }
        }


        binding.playBtn.setOnClickListener {
            binding.playAudioBarLaydout.visibility = View.VISIBLE
            binding.playBtn.setImageDrawable(resources.getDrawable(R.drawable.play))


            if(jpegViewModel.isAudioPlay.value!! == 0){
                jpegViewModel.isAudioPlay.value = 1
            } else if(jpegViewModel.isAudioPlay.value!! == 1){
                jpegViewModel.isAudioPlay.value = 2
            } else{
                jpegViewModel.isAudioPlay.value = 1
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

        }

        if(jpegViewModel.jpegMCContainer.value!!.audioContent.audio?._audioByteArray?.size ==0 ){
            binding.playBtn.visibility = View.GONE
        }
        /* audio */


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
                        if(mediaPlayer.isPlaying){
                            jpegViewModel.isAudioPlay.value = 1
                            audioResolver.mediaPlayer.seekTo(progress) // 재생위치를 바꿔준다(움직인 곳에서의 음악재생)
                        }

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



    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CoroutineScope(Dispatchers.Default).launch {

            binding.progressBar.visibility = View.VISIBLE

            imageContent = jpegViewModel.jpegMCContainer.value!!.imageContent
            textContent = jpegViewModel.jpegMCContainer.value!!.textContent

            while (!imageContent.checkPictureList) {
                delay(300)
            }

            setDiary()

            withContext(Dispatchers.Main) {

                month.value = textContent.getMonth() + 1
                day.value = textContent.getDay()
                Log.d("Cell Text", "create : ${month.value!!} || ${day.value!!}")

                layoutToolModule.month = month.value!!
                try {
                    layoutToolModule.setMonthLayer(layoutInflater, binding.monthLayout, jpegViewModel.currentMonth, month.value!!, ::month)
                }catch (e: IllegalStateException){
                    e.printStackTrace()
                }
                month.observe(viewLifecycleOwner) { _ ->
                    dateChange()
                    setDayView()
                }
                day.observe(viewLifecycleOwner) { _ ->
                    dateChange()
                }
            }
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
                    jpegViewModel.isAudioPlay.value = 2
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

//    fun timerUIStart(){
//        if(!isRecordingMode){
//            binding.RecordingTextView.setTextColor(Color.BLACK)
//            binding.RecordingTextView.setTypeface(null, Typeface.BOLD)
//            timerTask = object : TimerTask() {
//                var cnt = 0
//                override fun run() {
//                    CoroutineScope(Dispatchers.Main).launch {
//                        var string : String = String.format("%02d:%02d", cnt/60, cnt)
//                        Log.d("AudioModule", string)
//                        binding.RecordingTextView.setText(string)
//                        cnt++
//                        if(cnt > 30){
//                            timerUIStop()
//                            audioStop()
//                        }
//                    }
//                }
//            }
//            val timer = Timer()
//            timer.schedule(timerTask, 0, 1000)
//        }
//    }
//
//    fun timerUIStop(){
//        if(isRecordingMode){
//            timerTask.cancel()
//            CoroutineScope(Dispatchers.Main).launch {
//                binding.RecordingTextView.setText("")
//                //  Toast.makeText(activity, "녹음이 완료 되었습니다.", Toast.LENGTH_SHORT).show();
//
//            }
//        }
//    }

    fun setDayView() {
        val cellList = jpegViewModel.diaryCellArrayList
        val dayList = arrayListOf<Int>()

        val calendar = Calendar.getInstance()
        calendar.set(2023, month.value!! -1, 1)

        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (i in 0 until cellList.size) {
            val cell = cellList[i]
            if (cell.month == month.value!! - 1) {
                dayList.add(cell.day)
            }
        }
        CoroutineScope(Dispatchers.Main).launch {
            binding.dayLayout.removeAllViews()
        }
        CoroutineScope(Dispatchers.Default).launch {
            if(month.value == jpegViewModel.currentMonth) {
                layoutToolModule.setSubImage(layoutInflater, binding.dayLayout, jpegViewModel.currentDay, day.value!!, dayList, ::day)
            }
            else  {
                layoutToolModule.setSubImage(layoutInflater, binding.dayLayout, jpegViewModel.daysInMonth, 1, dayList, ::day)
            }
        }
    }


    fun setDiary() {
        val pictureList = imageContent.pictureList
        val byteArrayList = arrayListOf<ByteArray>()
        for (i in 0 until pictureList.size) {
            try {
                byteArrayList.add(imageContent.getJpegBytes(pictureList[i]))
            } catch (e: IndexOutOfBoundsException) {
                e.printStackTrace()
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                viewPagerAdapter = ViewPagerAdapter(requireContext())
                viewPagerAdapter.setImageList(byteArrayList)
                binding.viewPager.adapter = viewPagerAdapter
            }catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }

        binding.viewUnberBtn.setOnClickListener {
            isViewUnder = if(isViewUnder) {
                viewOnImageLayout()
                binding.viewUnberBtn.setImageResource(R.drawable.underview_unview)
                false
            } else {
                viewUnderLayout()
                binding.viewUnberBtn.setImageResource(R.drawable.underview_view)
                true
            }
        }

        viewOnImageLayout()

    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun dateChange() {
        handler.removeCallbacksAndMessages(null)

//        for(i in 0 until overlayBitmap.size) {
//            overlayBitmap[i].recycle()
//        }
        overlayBitmap.clear()


        val cellList = jpegViewModel.diaryCellArrayList
        var isCell = false

        Log.d("Cell Text", "current Day 22: ${month.value!! -1} | ${day.value}")
        for (i in 0 until cellList.size) {
            val cell = cellList[i]

            Log.d("Cell Text", "dateChange: ${cell.month} | ${cell.day}")
            if (cell.month == month.value!! - 1 && cell.day == day.value) {
                isCell = true
                binding.addBtn.visibility = View.GONE
//                binding.viewUnberBtn.visibility = View.VISIBLE
                jpegViewModel.jpegMCContainer.value!!.init()
                jpegViewModel.setCurrentMCContainer(cell.currentUri)

                while (!imageContent.checkPictureList) {

                }
                setDiary()
                break
            }
        }
        Log.d("Cell Text","isCell : $isCell")
        if(!isCell) {
            binding.viewPager.adapter = ViewPagerAdapter(requireContext())

            binding.OnImageLayout.visibility = View.INVISIBLE
//            binding.magicBtn.visibility = View.GONE
            binding.viewUnberBtn.visibility = View.GONE

            binding.addBtn.visibility = View.VISIBLE
            binding.addBtn.setOnClickListener {
                jpegViewModel.selectMonth = month.value!! - 1
                jpegViewModel.selectDay = day.value!!

                findNavController().navigate(R.id.action_viewDiaryFragment_to_addDiaryFragment)
            }
        }
    }

    private fun magicPictureRun(ovelapBitmap: ArrayList<Bitmap>) {
        CoroutineScope(Dispatchers.Default).launch {

            var currentImageIndex = 0
            var increaseIndex = 1

            val runnable = object : java.lang.Runnable {
                override fun run() {
                    if (ovelapBitmap.size > 0) {
                        binding.mainView.setImageBitmap(ovelapBitmap[currentImageIndex])
                        //currentImageIndex++

                        currentImageIndex += increaseIndex

                        if (currentImageIndex >= ovelapBitmap.size - 1) {
                            //currentImageIndex = 0
                            increaseIndex = -1
                        } else if (currentImageIndex <= 0) {
                            increaseIndex = 1
                        }
                        handler.postDelayed(this, magicPlaySpeed)
                    }
                }
            }
            handler.postDelayed(runnable, magicPlaySpeed)
        }
    }

    fun viewUnderLayout() {
        CoroutineScope(Dispatchers.Main).launch {
            binding.OnImageLayout.visibility = View.GONE
        }
    }

    fun viewOnImageLayout() {
        CoroutineScope(Dispatchers.Main).launch {
            binding.OnImageLayout.visibility = View.VISIBLE
            binding.contentTextValueOnImage.text = textContent.getContent()

            binding.progressBar.visibility = View.GONE
        }
    }

//    fun setMasetMagigic() {
//        CoroutineScope(Dispatchers.IO).launch {
//            if (imageContent.checkAttribute(ContentAttribute.magic)) {
//
//                withContext(Dispatchers.Main) {
//                    binding.magicBtn.visibility = View.VISIBLE
//                    binding.magicBtn.setImageResource(R.drawable.play)
//                }
//
//                isMagicPlay = false
//                magicPictureModule = MagicPictureModule(imageContent)
//
//                binding.magicBtn.setOnClickListener {
//                    if (!isMagicPlay) {
//                        CoroutineScope(Dispatchers.Main).launch {
//                            binding.magicBtn.setImageResource(R.drawable.parse)
//
//                            binding.progressBar.visibility = View.VISIBLE
//                            binding.viewPager.visibility = View.GONE
//                            binding.mainView.visibility = View.VISIBLE
//
//                            if(overlayBitmap.isEmpty()) {
//                                Glide.with(binding.mainView)
//                                    .load(imageContent.getJpegBytes(imageContent.mainPicture))
//                                    .into(binding.mainView)
//                            }
//                        }
//                        isMagicPlay = true
//
//                        CoroutineScope(Dispatchers.Default).launch {
//                            if(overlayBitmap.isEmpty()) {
//                                overlayBitmap = magicPictureModule.magicPictureProcessing()
//                            }
//
//                            Log.d("magic","magicPictureProcessing end ${overlayBitmap.size}")
//                            withContext(Dispatchers.Main) {
//                                binding.progressBar.visibility = View.GONE
//                            }
//                            Log.d("magic","magicPucture run ${overlayBitmap.size}")
//                            magicPictureRun(overlayBitmap)
//                        }
//                    } else {
//                        handler.removeCallbacksAndMessages(null)
//
//                        CoroutineScope(Dispatchers.Main).launch {
//                            binding.magicBtn.setImageResource(R.drawable.play)
//
//                            binding.progressBar.visibility = View.GONE
//                            binding.viewPager.visibility = View.VISIBLE
//                            binding.mainView.visibility = View.GONE
//                        }
//                        isMagicPlay = false
//                    }
//                }
//            } else {
//                CoroutineScope(Dispatchers.Main).launch {
//                    binding.magicBtn.visibility = View.INVISIBLE
//                }
//            }
//        }
//    }
}
