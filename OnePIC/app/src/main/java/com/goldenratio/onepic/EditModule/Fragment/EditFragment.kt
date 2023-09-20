package com.goldenratio.onepic.EditModule.Fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.get
import androidx.core.view.setPadding
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.goldenratio.onepic.*
import com.goldenratio.onepic.AudioModule.AudioResolver
import com.goldenratio.onepic.EditModule.MagicPictureModule
import com.goldenratio.onepic.EditModule.FaceDetectionModule
import com.goldenratio.onepic.EditModule.ShakeLevelModule
import com.goldenratio.onepic.AllinJPEGModule.AiContainer
import com.goldenratio.onepic.AllinJPEGModule.AiLoadResolver
import com.goldenratio.onepic.AllinJPEGModule.Content.*
import com.goldenratio.onepic.ViewerModule.Fragment.ViewerFragment
import com.goldenratio.onepic.ViewerModule.ViewerEditorActivity
import com.goldenratio.onepic.databinding.FragmentEditBinding
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.lang.String
import java.util.*
import kotlin.Boolean
import kotlin.ByteArray
import kotlin.Double
import kotlin.Exception
import kotlin.IllegalArgumentException
import kotlin.IllegalStateException
import kotlin.Int
import kotlin.Long
import kotlin.Unit
import kotlin.apply
import kotlin.getValue
import kotlin.let

class EditFragment : Fragment(R.layout.fragment_edit), ConfirmDialogInterface {

    private lateinit var binding: FragmentEditBinding
    private lateinit var activity: ViewerEditorActivity
    private val jpegViewModel by activityViewModels<JpegViewModel>()

    private val imageToolModule: ImageToolModule = ImageToolModule()
    private val faceDetectionModule: FaceDetectionModule =  FaceDetectionModule()
    private var magicPictureModule: MagicPictureModule? = null
    private lateinit var aiLoadResolver: AiLoadResolver

    private lateinit var imageContent: ImageContent
    private lateinit var textContent: TextContent
    private lateinit var audioContent: AudioContent

    private var isFinished = MutableLiveData<Boolean>()

    private var mainSubView: View? = null
    private var mainTextView: TextView? = null

    private var pictureList = arrayListOf<Picture>()
    private lateinit var mainPicture: Picture

    private var isSaving = false
    private var isAudioPlay = false
    private var isTextView = false
    private var isPictureChanged : MutableLiveData<Boolean> = MutableLiveData(false)

    private var isDeleting = false
    private var isMagicPlay: Boolean = false

    // Audio 없다가 추가, 수정
    private  var isAudioAddMode = false

    private var bestImageIndex: Int? = null

    private val PICK_IMAGE_REQUEST = 1

    private var bitmapList :ArrayList<Bitmap> = arrayListOf()
    private var overlayBitmap = arrayListOf<Bitmap>()
    val handler = Handler()
    var magicPlaySpeed: Long = 100

    var isAllInJPEG : Boolean = false
    var isButtonEnable = true

    /* 오디오 추가 관련 */
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
    private lateinit var audioResolver : AudioResolver
    private lateinit var timerTask: TimerTask
    private var playingTimerTask : TimerTask? = null
    private lateinit var mediaPlayer : MediaPlayer
    private var audioWithContent : Job = Job()

    /* 텍스트 추가 관련 */
    // text
    var isTextOn : Boolean = false
    var textList : java.util.ArrayList<kotlin.String> = arrayListOf()

    private enum class LoadingText {
        BestImageRecommend,
        Save,
        MagicPlay,
        EditReady,
        Adding
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as ViewerEditorActivity
        audioResolver = AudioResolver(activity)
        mediaPlayer = audioResolver.mediaPlayer
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        bundle: Bundle?
    ): View {
        // 상태바 색상 변경
        val window: Window = activity.window
            ?: throw IllegalStateException("Fragment is not attached to an activity")
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.setStatusBarColor(ContextCompat.getColor(requireContext(), android.R.color.black))

        // 뷰 바인딩 설정
        binding = FragmentEditBinding.inflate(inflater, container, false)

        settingEditFragment()

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setClickEvent()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        imageAdd(data)
    }

    override fun onPause() {
        super.onPause()
        if (mediaPlayer != null) {
            try {
                mediaPlayer.pause()
                isDestroy = true
            }catch (e : IllegalStateException){
                Log.e("AudioModule", "Failed to media pause: ${e.message}")

            }
        }
    }

    override fun onYesButtonClick(id: Int) {
        binding.recordingImageView.setImageDrawable(resources.getDrawable(R.drawable.record))
        binding.playAudioBarLaydout.visibility = View.GONE
        jpegViewModel.isAudioPlay.value = 1
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

    /**
     *  변수를 초기화한다.
     */
    private fun settingEditFragment() {
        //        imageToolModule.showView(binding.progressBar, true)
        showProgressBar(true, LoadingText.EditReady)

        // isFinished가 ture가 되면 viewerFragment로 전환
        isFinished.observe(requireActivity()) { value ->
            if (value == true) {
                CoroutineScope(Dispatchers.Main).launch {
                    findNavController().navigate(R.id.action_editFragment_to_viewerFragment)
                }
                isFinished.value = false // 초기화
            }
        }

        isPictureChanged.observe(viewLifecycleOwner) {
            if (isPictureChanged.value == true) {
                CoroutineScope(Dispatchers.IO).launch {
                    bestImageIndex = null
                    overlayBitmap.clear()
                    withContext(Dispatchers.Main) {
                        isPictureChanged.value = false
                    }
                }
            }
        }

        CoroutineScope(Dispatchers.Default).launch {
            aiLoadResolver = AiLoadResolver()
            // Content 설정
            imageContent = jpegViewModel.jpegAiContainer.value?.imageContent!!

            val selected = jpegViewModel.selectedSubImage
            if(selected != null)
                magicPictureModule = MagicPictureModule(imageContent, selected)
//        imageContent.setMainBitmap(null)
            textContent = jpegViewModel.jpegAiContainer.value!!.textContent
            audioContent = jpegViewModel.jpegAiContainer.value!!.audioContent

            CoroutineScope(Dispatchers.IO).launch {
                while (!imageContent.checkPictureList) {
                }

                // picture 설정
                mainPicture = imageContent.mainPicture
                pictureList = imageContent.pictureList

                val bitmap = imageContent.getBitmapList()
                if (bitmap != null)
                    bitmapList = bitmap
            }

            // 만약 편집을 했다면 save 버튼이 나타나게 설정
            if (imageContent.checkMainChanged || imageContent.checkBlending ||
                imageContent.checkMagicCreated || imageContent.checkAdded || imageContent.checkEditChanged
            ) {
                imageToolModule.showView(binding.saveBtn, true)
            }
            setViewDetailMenu()

            // 메인 이미지 설정
            CoroutineScope(Dispatchers.Main).launch {
                val index = jpegViewModel.getSelectedSubImageIndex()
                if (pictureList[index].contentAttribute == ContentAttribute.magic) {
                    imageToolModule.showView(binding.magicPlayBtn, true)
                    setMagicPlay()
                }
                Glide.with(binding.mainImageView)
                    .load(imageContent.getJpegBytes(pictureList[index]))
                    .into(binding.mainImageView)

                // distance focus일 경우 seekbar 보이게
                if (imageContent.checkAttribute(ContentAttribute.distance_focus)) {
                    imageToolModule.showView(binding.seekBar, true)
                    setDistanceSeekBar()
                }
                if(imageContent.checkAttribute(ContentAttribute.object_focus)) {
                    imageToolModule.showView(binding.focusBtn, true)
                }
            }
            setContainerTextSetting()

            // 컨테이너 이미지 설정 (오디오 텍스트 이미지도 포함)
            withContext(Dispatchers.Main) {
                setContainer()
            }

            /* TODO: ViewrFragment to EditorFragment - currentImageFilePath와 selectedImageFilePath 확인 */
            // ViewerFragment에서 스크롤뷰 이미지 중 아무것도 선택하지 않은 상태에서 edit 누르면 picturelist의 맨 앞 객체(메인)이 선택된 것으로 했음
            Log.d("currentImageUri: ", "" + jpegViewModel.currentImageUri)
            Log.d("selectedImageFilePath: ", "" + jpegViewModel.selectedSubImage)

            showProgressBar(false, null)
        }

        /* Format - JPEG, Ai JPEG*/
        if(JpegViewModel.AllInJPEG) {
            Log.d("format_test", "뷰어 로드할 때 AllInJPEG = true")
            binding.formatTextView.text = "ALL In JPEG"
            JpegViewModel.AllInJPEG = true
        }
        else{
            Log.d("format_test", "뷰어 로드할 때 AllInJPEG = false")
            binding.formatTextView.text = "일반 JPEG"
            JpegViewModel.AllInJPEG = false
        }

        /* 텍스트 추가 */
        addTextModual()

        /* 오디오 */
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

        /* 오디오 모듈 추가 */
        addAudioModule()

        imageToolModule.settingAnimation(binding.successInfoConstraintLayout)
    }

    /**
     * 이벤트 처리를 설정한다.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun setClickEvent() {
        // Blending 버튼 클릭 이벤트 리스너 등록
        binding.blendingBtn.setOnClickListener {
            // FaceBlendingFragment로 이동
            findNavController().navigate(R.id.action_editFragment_to_FaceBlendingFragment)
        }
        // 움직이는 Magic 버튼 클릭 이벤트 리스너 등록
        binding.magicBtn.setOnClickListener {
            // MagicPictureFragment로 이동
            findNavController().navigate(R.id.action_editFragment_to_magicPictureFragment)
        }
        // 초점 업그레이드 버튼 클릭 이벤트 리스너 등록
        binding.focusBtn.setOnClickListener {
            //FocusChangeFragment로 이동
            findNavController().navigate(R.id.action_editFragment_to_focusChangeFragment)
        }

        // 얼굴 추천 버튼 클릭 이벤트 리스너 등록
        binding.bestMainBtn.setOnClickListener {
            AlgorithmBestPictureRanking()
        }

        // 단일 사진 추출
        binding.extractJpegBtn.setOnClickListener {
            // 한 장으로 저장 추출
            val currentImage = jpegViewModel.selectedSubImage
            CoroutineScope(Dispatchers.Main).launch {
                jpegViewModel.jpegAiContainer.value!!.singleImageSave(currentImage!!)
                //jpegViewModel.jpegAiContainer.value!!.saveResolver.singleImageSave(currentImage!!)
                Toast.makeText(activity, "사진이 저장 되었습니다.", Toast.LENGTH_SHORT).show();

            }

        }

        // 대표 사진 변경
        binding.mainChangeBtn.setOnClickListener {
            changeMainImage()
        }

        // 컨텐츠 삭제
        binding.contentDeleteBtn.setOnClickListener {
            removeContent()
        }

        // 매직픽처 재생 버튼
        binding.magicPlayBtn.setOnClickListener {
            playMagicPicture()
        }

        // 취소 버튼 (viewer로 이동)
        binding.backBtn.setOnClickListener {
            cancleEditImage()
        }

        // 저장 버튼 (viewer로 이동)
        binding.saveBtn.setOnClickListener {
            saveNewImage()
        }

        /* 오디오 리스너 */
        // 오디오 취소
        binding.audioCancleBtn.setOnClickListener {

            if(audioWithContent != null){
                isDestroy = true
                audioWithContent.cancel()
                playinAudioUIStop()
            }
            if(playingTimerTask != null)
                playingTimerTask!!.cancel()

            isAudioAddMode = false
            fitAudioModeUI()
            setCurrentSeekBar()
            jpegViewModel.isAudioPlay.value = 0
            isPlayingMode = true
            isRecordedMode = false
            isRecordingMode = false
        }

        // 오디오 추가 버튼
        addCurrentAudioBarListener()
        binding.audioCheckBtn.setOnClickListener {
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
                binding.linear.removeViewAt(binding.linear.size - 2)
                val view = setContainerSubItem(R.drawable.edit_audio_icon, clickedFunc = ::ShowingAudio, deleteFunc = ::DeleteAudio)
                setContainerTextSetting()

                binding.audioContentLayout.visibility = View.GONE
                imageContent.checkEditChanged = true
                binding.linear.addView(view, binding.linear.size - 1)
                // 저장 버튼 표시 | 메인 변경 버튼 없애기
                imageToolModule.showView(binding.saveBtn, true)
                if(audioWithContent != null){
                    isDestroy = true
                    audioWithContent.cancel()
                }
                playingTimerTask!!.cancel()
                binding.RecordingTextView.setText("")
                Toast.makeText(activity, "추가 되었습니다.", Toast.LENGTH_SHORT).show()

                jpegViewModel.isAudioPlay.value = 0
                isPlayingMode = true
                isRecordedMode = false
                isRecordingMode = false

                isAudioAddMode = false

                setCurrentSeekBar()
                fitAudioModeUI()
                checkAllInJPEG()
            }
        }
    }

    /**
     * 메인 이미지(대표 이미지)를 현재 선택한 사진[jpegViewModel.selectedSubImage]으로 변경한다.
     */
    private fun changeMainImage() {
        // 기존 메인에 관한 설정 제거
        mainTextView?.visibility = View.INVISIBLE

        // 선택한 picture Index 알아내기
        val selectPictureIndex = pictureList.indexOf(jpegViewModel.selectedSubImage)
        jpegViewModel.mainSubImage = jpegViewModel.selectedSubImage
        Log.d("main Change", "selectPictureIndex: $selectPictureIndex")

        // 해당 뷰를 index를 통해 알아내 mainMark 표시 띄우기
        val newMainSubView = binding.linear.getChildAt(selectPictureIndex)
            .findViewById<TextView>(R.id.mainMark)
        newMainSubView.visibility = View.VISIBLE

        // 메인에 관한 설정
        mainTextView = newMainSubView
        mainPicture = pictureList[selectPictureIndex]

        // 메인 변경 유무 flag true로 변경
        imageContent.checkMainChanged = true

        //save 표시 | 메인 변경 버튼 없애기
        imageToolModule.showView(binding.saveBtn, true)
        imageToolModule.showView(binding.mainChangeBtn, false)
    }

    /**
     * All-in JPEG에 담겨져 있는 콘텐츠를 삭제할 수 있게 설정한다.
     */
    private fun removeContent() {
        val size = binding.linear.size
        if (!isDeleting) {
            CoroutineScope(Dispatchers.Main).launch {
                binding.contentDeleteBtn.setImageResource(R.drawable.container_deleting_icon)
            }
            if (pictureList.size > 1) {
                for (i in 0 until size - 3) {
                    val view = binding.linear.getChildAt(i)
                    imageToolModule.showView(
                        view.findViewById<ImageView>(R.id.deleteIcon),
                        true
                    )
                }
            }
            for (i in size-3 until size - 1 ) {
                val view = binding.linear.getChildAt(i)
                imageToolModule.showView(
                    view.findViewById<ImageView>(R.id.deleteIcon),
                    true
                )
            }
            if (textContent.textList.size == 0) {
                val view = binding.linear.getChildAt(binding.linear.size - 3)
                imageToolModule.showView(view, false)
                Log.d("delete edit", "text add")
            }
            if (audioContent.audio == null) {
                val view = binding.linear.getChildAt(binding.linear.size - 2)
                imageToolModule.showView(view, false)
                Log.d("delete edit", "audio add")
            }

            val view = binding.linear.getChildAt(binding.linear.size - 1)
            imageToolModule.showView(view, false)

            isDeleting = true
        } else {
            CoroutineScope(Dispatchers.Main).launch {
                binding.contentDeleteBtn.setImageResource(R.drawable.container_delete_icon)
            }
            for (i in 0 until size - 1) {
                val view = binding.linear.getChildAt(i)
                imageToolModule.showView(
                    view.findViewById<ImageView>(R.id.deleteIcon),
                    false
                )
            }
            if (textContent.textList.isEmpty()) {
                val view = binding.linear.getChildAt(binding.linear.size - 3)
                imageToolModule.showView(view, true)
            }
            if (audioContent.audio == null) {
                val view = binding.linear.getChildAt(binding.linear.size - 2)
                imageToolModule.showView(view, true)
            }

            val view = binding.linear.getChildAt(binding.linear.size - 1)
            imageToolModule.showView(view, true)
            isDeleting = false
        }
    }

    /**
     * 매직픽처일 경우, 화면에 매직픽처 아이콘 띄우기
     */
    private fun setMagicPlay() {
        CoroutineScope(Dispatchers.IO).launch {
            isMagicPlay = false
            val selected = jpegViewModel.selectedSubImage
            if(selected != null)
                magicPictureModule = MagicPictureModule(imageContent, selected)

            Log.d("magic!!!", "seleted index = $selected | ${jpegViewModel.getSelectedSubImageIndex()}" )
        }
    }

    /**
     * 매직픽처를 재생한다.
     */
    private fun playMagicPicture() {
        if (!isMagicPlay) {
            showProgressBar(true, LoadingText.MagicPlay)
            CoroutineScope(Dispatchers.Main).launch {
                binding.magicPlayBtn.setImageResource(R.drawable.edit_magic_ing_icon)
            }

            isMagicPlay = true

            CoroutineScope(Dispatchers.Default).launch {
                if (overlayBitmap.isEmpty() ) {
                    while(magicPictureModule == null ) {}
                    overlayBitmap = magicPictureModule!!.magicPictureProcessing()
                }
                Log.d("magic", "magicPictureProcessing end ${overlayBitmap.size}")
                showProgressBar(false, null)
                Log.d("magic", "magicPucture run ${overlayBitmap.size}")
                magicPictureRun(overlayBitmap)
            }
        }
        else {
            handler.removeCallbacksAndMessages(null)
            CoroutineScope(Dispatchers.Main).launch {
                binding.magicPlayBtn.setImageResource(R.drawable.edit_magic_icon)
                showProgressBar(false, null)
            }
            isMagicPlay = false
        }
    }

    /**
     * 매직픽처를 재생한다.
     *
     * @param overlapBitmap 재생에 필요한 [Bitmap]
     */
    private fun magicPictureRun(overlapBitmap: ArrayList<Bitmap>) {
        CoroutineScope(Dispatchers.Default).launch {

            var currentImageIndex = 0
            var increaseIndex = 1

            val runnable = object : java.lang.Runnable {
                override fun run() {
                    if (overlapBitmap.size > 0) {
                        binding.mainImageView.setImageBitmap(overlapBitmap[currentImageIndex])
                        //currentImageIndex++

                        currentImageIndex += increaseIndex

                        if (currentImageIndex >= overlapBitmap.size - 1) {
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

    /**
     * 편집을 취소하고 뷰어로 돌아간다.
     */
    private fun cancleEditImage() {
        // 변경된 편집이 있을 경우 확인 창 띄우기
        if (imageContent.checkMainChanged || imageContent.checkBlending ||
            imageContent.checkMagicCreated || imageContent.checkAdded || imageContent.checkEditChanged
        ) {
            val oDialog: AlertDialog.Builder = AlertDialog.Builder(
                activity,
                android.R.style.Theme_DeviceDefault_Light_Dialog
            )

            oDialog.setMessage("편집한 내용을 저장하지 않고 나가시겠습니까?")
                .setPositiveButton(
                    "취소"
                ) { _, _ -> }
                .setNeutralButton("확인") { _, _ ->
                    setButtonDeactivation()

                    val uri:Uri
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                        uri = Uri.parse(jpegViewModel.currentImageUri)
                    }
                    else {
                        uri = imageToolModule.getUriFromPath(requireContext(), jpegViewModel.currentImageUri!!)
                    }

                    val iStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
                    val sourceByteArray = imageToolModule.getBytes(iStream!!)

                    val isContainerChanged = MutableLiveData<Boolean>()
                    CoroutineScope(Dispatchers.Main).launch {
                        imageContent.checkPictureList = false
                        val job = async {
                            aiLoadResolver.createAiContainer(jpegViewModel.jpegAiContainer.value!!,sourceByteArray) }
                        job.await()

                        while(!imageContent.checkPictureList) {}
                        CoroutineScope(Dispatchers.Default).launch {
                            imageContent.setBitmapList()
                        }
                        setCurrentPictureByteArrList()
//                            findNavController().navigate(R.id.action_editFragment_to_viewerFragment)
                    }

                }.show()
        } else {
            setButtonDeactivation()
            findNavController().navigate(R.id.action_editFragment_to_viewerFragment)
        }
    }

    /**
     * 이미지를 저장하고 뷰어로 돌아간다.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveNewImage() {
        ViewerFragment.isEditStoraged = true
        jpegViewModel.mainSubImage = null
        // 저장 중인지 확인하는 flag가 false일 경우만 저장 단계 실행 --> 두번 실행될 경우 오류를 예외처리하기 위해
        if (!isSaving) {
            showProgressBar(true, LoadingText.Save)
            isSaving = true

            imageContent.pictureList = pictureList
            imageContent.pictureCount = pictureList.size

            if(imageContent.checkMainChanged) {
                // 1. 메인으로 하고자하는 picture를 기존의 pictureList에서 제거
                val removeResult = imageContent.removePicture(mainPicture)
                if (removeResult) {
                    // 2. main 사진을 첫번 째로 삽입
                    imageContent.insertPicture(0, mainPicture)
                    imageContent.mainPicture = mainPicture

                    // 3. meta data 변경
                    imageContent.jpegHeader = mainPicture._mataData!!
                }
            }
            // 덮어쓰기
            val currentFilePath = jpegViewModel.currentImageUri
            var fileName = ""

            // 13버전 이상일 경우는 uri를 받아 옴 --> 전처리 거쳐서 파일 이름 얻어내기
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                fileName = jpegViewModel.getFileNameFromUri(currentFilePath!!.toUri())
            }
            // 13버전 이하일 경우는 file path를 받아 옴
            else {
                fileName = currentFilePath!!.substring(currentFilePath.lastIndexOf("/") + 1);
            }

            // Format 설정
            if(isAllInJPEG) {
                JpegViewModel.AllInJPEG = true
            }
            else {
                JpegViewModel.AllInJPEG = false
            }

            CoroutineScope(Dispatchers.IO).launch {
                jpegViewModel.currentFileName = fileName
                jpegViewModel.jpegAiContainer.value?.saveAfterEdit(fileName)
                Thread.sleep(3000)
                setButtonDeactivation()
                setCurrentPictureByteArrList()
            }

            imageContent.setCheckAttribute()
        }
    }

    private fun setCurrentPictureByteArrList() {
        var pictureList = jpegViewModel.jpegAiContainer.value?.getPictureList()

        if (pictureList != null) {

            CoroutineScope(Dispatchers.IO).launch {
                val pictureByteArrayList = mutableListOf<ByteArray>()
                for (picture in pictureList) {
                    val pictureByteArr =
                        jpegViewModel.jpegAiContainer.value?.imageContent?.getJpegBytes(picture)
                    pictureByteArrayList.add(pictureByteArr!!)
                } // end of for..
                jpegViewModel.setpictureByteArrList(pictureByteArrayList)
                CoroutineScope(Dispatchers.Main).launch {
                    isFinished.value = true
                }
            }
        }
    }

    /**
     * 현재 이미지 상태에 따라, 편집 메뉴를 설정한다.
     */
    private fun setViewDetailMenu() {
        checkAllInJPEG()
        CoroutineScope(Dispatchers.Main).launch {
            if (pictureList.size <= 1) {
                Log.d("checkMenu", " 1 : pictureList.size = ${pictureList.size}")
                binding.bestMainBtn.visibility = View.GONE
                binding.blendingBtn.visibility = View.GONE
                binding.magicBtn.visibility = View.GONE
            } else {
                Log.d("checkMenu", " 2 : pictureList.size = ${pictureList.size}")
                binding.bestMainBtn.visibility = View.VISIBLE
                binding.blendingBtn.visibility = View.VISIBLE
                binding.magicBtn.visibility = View.VISIBLE
                if (imageContent.checkAttribute(ContentAttribute.distance_focus)) {
                    Log.d("checkMenu", " 3 : pictureList.size = ${pictureList.size}")
                    binding.bestMainBtn.visibility = View.GONE
                    binding.blendingBtn.visibility = View.GONE
                    binding.magicBtn.visibility = View.GONE
                }
            }
        }
    }

    /**
     * 현재 편집을 했는지 여부를 알려주는 변수들을 초기화한다.
     */
    private fun setButtonDeactivation() {
        imageContent.checkAdded = false
        imageContent.checkBlending = false
        imageContent.checkMagicCreated = false
        imageContent.checkMainChanged = false
        imageContent.checkEditChanged = false
        jpegViewModel.mainSubImage = null
    }

    /**
     * 동적을 실행할 때 꽤 시간이 소요되어 progressBar를 화면에 표시한다.
     *
     * @param boolean progressBar를 표시할지 말지 여부
     * @param loadingText progressBar가 표시될때 같이 띄어질 텍스트
     */
    private fun showProgressBar(boolean: Boolean, loadingText: LoadingText?) {
        setEnable(!boolean)

        CoroutineScope(Dispatchers.Main).launch {
            binding.loadingText.text = when (loadingText) {
                LoadingText.Save -> {
                    "편집을 저장 중.."
                }
                LoadingText.BestImageRecommend -> {
                    "베스트 사진 추천 중.."
                }
                LoadingText.MagicPlay -> {
                    "매직 사진 준비 중.."
                }
                LoadingText.EditReady -> {
                    "편집 준비 중.."
                }
                LoadingText.Adding -> {
                    "사진 추가 중.."
                }

                else -> {
                    ""
                }
            }
        }

        imageToolModule.showView(binding.progressBar, boolean)
        imageToolModule.showView(binding.loadingText, boolean)
    }

    /**
     * All-in JPEG인지 일반 JPEG인지 판별해 화면에 표시한다.
     */
    private fun checkAllInJPEG() {
        CoroutineScope(Dispatchers.Main).launch {
            if (imageContent.pictureList.size > 1 || textContent.textCount > 0 || audioContent.audio != null) {
                isAllInJPEG = true
                binding.formatTextView.text = "ALL In JPEG"
                binding.allInJpegTextView.text = "ALL In JPEG"
                jpegViewModel.jpegAiContainer.value!!.isAllinJPEG = true
            } else {
                isAllInJPEG = false
                jpegViewModel.jpegAiContainer.value!!.isAllinJPEG = false
                Log.d("format_test", "textContent.textCount : ${textContent.textCount}")
                Log.d(
                    "format_test",
                    "audioContent.audio : ${audioContent.audio?._audioByteArray?.size}"
                )
                binding.formatTextView.text = "일반 JPEG"
                binding.allInJpegTextView.text = "일반 JPEG"
            }
        }
    }

    /**
     * 로딩 중에 화면에 버튼들을 비활성화 시킨다.
     *
     * @param boolean 비활성화 여부
     */
    private fun setEnable(boolean: Boolean) {
        isButtonEnable = boolean
        CoroutineScope(Dispatchers.Main).launch {
            binding.bestMainBtn.isEnabled = boolean
            binding.blendingBtn.isEnabled = boolean
            binding.magicBtn.isEnabled = boolean
            binding.focusBtn.isEnabled = boolean

            binding.backBtn.isEnabled = boolean
            binding.saveBtn.isEnabled = boolean

            binding.mainChangeBtn.isEnabled = boolean
            binding.extractJpegBtn.isEnabled = boolean
            binding.contentDeleteBtn.isEnabled = boolean
            binding.linear.isEnabled = boolean
        }
    }

    /**
     * 거리별 다초점 촬영으로 찍은 사진인 경우 호출되는 함수로, 사진을 보기 편리하게 SeekBar를 생성한다.
     */
    private fun setDistanceSeekBar() {
        while(!imageContent.checkPictureList) {}
        Log.d("seekBar","#####")
        imageToolModule.showView(binding.seekBar, true)

        binding.seekBar.max = pictureList.size - 1
        binding.seekBar.progress = jpegViewModel.getSelectedSubImageIndex()

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // SeekBar의 값이 변경될 때 호출되는 메서드입니다.
                // progress 변수는 현재 SeekBar의 값입니다.
                // fromUser 변수는 사용자에 의해 변경된 값인지 여부를 나타냅니다.
                val index = progress % pictureList.size
                jpegViewModel.setselectedSubImage(pictureList[index])

                // 글라이드로만 seekbar 사진 변화 하면 좀 끊겨 보이길래
                CoroutineScope(Dispatchers.Main).launch {
                    val subLayout = binding.linear.getChildAt(index)
                    changeViewImage(index, subLayout.findViewById(R.id.scrollImageView))
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    /**
     * 하단 All-in JPEG 컨테이너에 들어있는 이미지와 오디오, 텍스트, 이미지 추가 아이콘을 설정한다.
     * 이미지 클릭시 메인 화면에 보이는 이미지 변환되게, 아이콘 클릭 시 해당 아이콘이 하는 동작 실행 등을 설정한다.
     */
    @SuppressLint("MissingInflatedId")
    fun setContainer() {
        try {
            for (i in 0 until pictureList.size) {
                val view = setSubImage(pictureList[i])
                binding.linear.addView(view)
            }

            if (textContent.textList.size > 0) {
                val view = setContainerSubItem(R.drawable.edit_text_icon, clickedFunc = ::ShowingText, deleteFunc = ::DeleteText)
                binding.linear.addView(view)
            } else {
                val view = setContainerSubItem(R.drawable.edit_text_add_icon, clickedFunc = ::AddText, deleteFunc = ::DeleteText)
                binding.linear.addView(view)
            }

            if (audioContent.audio != null) {
                val view = setContainerSubItem(R.drawable.edit_audio_icon, clickedFunc = ::ShowingAudio, deleteFunc = ::DeleteAudio)
                binding.linear.addView(view)
            } else {
                val view = setContainerSubItem(R.drawable.edit_audio_add_icon, clickedFunc = ::AddAudio, deleteFunc = ::DeleteAudio)
                binding.linear.addView(view)
            }

            val view = setContainerSubItem(R.drawable.edit_image_add_icon, clickedFunc = ::AddImage, deleteFunc = fun(_ : FrameLayout) {})
            binding.linear.addView(view)
        }catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    /**
     * All-in JPEG 사진 속 이미지들 중 베스트 사진을 추천해준다.
     */
    private fun AlgorithmBestPictureRanking() {
        showProgressBar(true, LoadingText.BestImageRecommend)
        val newBitmapList = imageContent.getBitmapList()
        if (newBitmapList != null) {
            bitmapList = newBitmapList
            CoroutineScope(Dispatchers.IO).launch {
                if (bestImageIndex == null ) {
                    // 인공지능 모델을 통해 〖image〗_i  에 있는 모든 얼굴 f에 대한
                    // re(오른쪽 눈을 뜬 정도), le(왼쪽 눈을 뜬 정도), sm(웃고 있는 정도)를 알아낸다.
                    faceDetectionModule.allFaceDetection(bitmapList)

                    val eyesDetectionResult = faceDetectionModule.getEyesAnalysisResults(bitmapList)
                    val smilingDetectionResult = faceDetectionModule.getSmilingAnalysisResults()
                    Log.d("anaylsis", "end faceDetection")

                    val shakeDetectionResult =
                        ShakeLevelModule().shakeLevelDetection(bitmapList)

                    val analysisResults = arrayListOf<Double>()

                    var preBestImageIndex = 0

                    for (i in 0 until bitmapList.size) {
                        analysisResults.add(eyesDetectionResult[i] * 0.3 + smilingDetectionResult[i] * 0.2 + shakeDetectionResult[i] * 0.5)
                        if (analysisResults[preBestImageIndex] < analysisResults[i]) {
                            preBestImageIndex = i
                        }
                    }

                    Log.d("anaylsis", "=== ${analysisResults[preBestImageIndex]}")
                    println("bestImageIndex = $preBestImageIndex")

                    bestImageIndex = preBestImageIndex
                }
                jpegViewModel.selectedSubImage = pictureList[bestImageIndex!!]

                withContext(Dispatchers.Main) {
                    Glide.with(binding.mainImageView)
                        .load(imageContent.getJpegBytes(pictureList[bestImageIndex!!]))
                        .into(binding.mainImageView)

                    val view =  binding.linear.getChildAt(bestImageIndex!!)

                    changeViewImage(bestImageIndex!!, view.findViewById<ImageView>(R.id.scrollImageView))

                    setMoveScrollView(view, bestImageIndex!!)

                    showProgressBar(false, null)

                    Log.d("mainChange", "bestImage null")
                    try {
                        binding.successInfoTextView.text = getString(R.string.best_choice_animation)
                        imageToolModule.fadeIn.start()
                    }
                    catch (e: IllegalStateException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    /**
     * 하단 All-in JPEG 컨테이너에 스크롤을 선택한 이미지가 가운데가 되도록 설정한다.
     *
     * @param subLayout 하단 뷰에 있는 아이콘 혹은 이미지 뷰
     * @param index 선택한 index
     */
    private fun setMoveScrollView(subLayout: View, index: Int) {
        val viewTreeObserver = subLayout.viewTreeObserver

        viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // 동적으로 추가된 뷰가 다 그려진 후에 실행되어야 할 코드 작성
                val x = subLayout.width * (index - 2)

                CoroutineScope(Dispatchers.Main).launch {
                    binding.scrollView.scrollTo(x, 0)
                }
                subLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    /**
     * 하단 All-in JPEG 컨테이너에서 다른 이미지를 클릭했을 시 호출되는 함수로, 선택된 이미지를 메인 화면에 보여준다.
     *
     * @param index 선택한 index
     * @param imageView 선택한 이미지 뷰
     */
    fun changeViewImage(index: Int, imageView: ImageView) {
        mainSubView?.background = null
        mainSubView?.setPadding(0)

        if(pictureList[index].contentAttribute == ContentAttribute.magic) {
            imageToolModule.showView(binding.magicPlayBtn, true)
            setMagicPlay()
        }
        else {
            imageToolModule.showView(binding.magicPlayBtn, false)
            magicPictureModule = null
        }

        CoroutineScope(Dispatchers.Main).launch {
            // 메인 이미지 설정 (선택된 이미지로 변경)
            if (bitmapList.size > index) {
                // 만들어 졌으면 비트맵으로 띄웠어
                binding.mainImageView.setImageBitmap(bitmapList[index])
            } else {
                // 비트맵은 따로 만들고 있고 해당 index의 비트맵이 안만들어졌음명 글라이드로
                Glide.with(binding.mainImageView)
                    .load(imageContent.getJpegBytes(pictureList[index]))
                    .into(binding.mainImageView)
            }
        }
        if (jpegViewModel.getMainSubImageIndex() != index) {
            imageToolModule.showView(binding.mainChangeBtn, true)
//            imageToolModule.showView(binding.extractJpegBtn, true)
        } else {
            imageToolModule.showView(binding.mainChangeBtn, false)
//            imageToolModule.showView(binding.extractJpegBtn, false)
        }
//        jpegViewModel.selectPictureIndex = index

        imageView.setBackgroundResource(R.drawable.chosen_image_border)
        imageView.setPadding(6)

        mainSubView = imageView
    }

    /**
     * 사진을 선택할 갤러리 창을 보여준다.
     */
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = MediaStore.Images.Media.CONTENT_TYPE
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    /**
     * 추가할 이미지를 선택하면 호출될 함수로, 선택한 이미지들을 All-in JPEG에 추가한다.
     *
     * @param data 선택한 이미지 데이터
     */
    private fun imageAdd(data: Intent?) {
        val uriList = arrayListOf<Uri>()

        if (data == null) {   // 어떤 이미지도 선택하지 않은 경우
            Toast.makeText(requireContext(), "이미지를 선택하지 않았습니다.", Toast.LENGTH_SHORT).show()
        } else {   // 이미지를 하나라도 선택한 경우
            if (data.clipData == null) {     // 이미지를 하나만 선택한 경우
                Log.e("single choice: ", String.valueOf(data.data))
                val imageUri: Uri? = data.data
                if (imageUri != null) {
                    uriList.add(imageUri)
                }
            } else {  // 이미지를 여러장 선택한 경우
                val clipData: ClipData? = data.clipData
                Log.e("clipData", String.valueOf(clipData?.itemCount))
                if (clipData != null) {
                    if (clipData.itemCount > 10) {   // 선택한 이미지가 11장 이상인 경우
                        Toast.makeText(requireContext(), "사진은 10장까지 선택 가능합니다.", Toast.LENGTH_LONG)
                            .show()
                    } else {   // 선택한 이미지가 1장 이상 10장 이하인 경우
                        Log.e("image불러오기", "multiple choice")

                        for (i in 0 until clipData.itemCount) {
                            val imageUri: Uri =
                                clipData.getItemAt(i).uri // 선택한 이미지들의 uri를 가져온다.
                            try {
                                uriList.add(imageUri) //uri를 list에 담는다.
                            } catch (e: Exception) {
                                Log.e("image불러오기", "File select error", e)
                            }
                        }
                    }
                }
            }

            if (uriList.size > 0) {
//                imageToolModule.showView(binding.progressBar, true)
                showProgressBar(true, LoadingText.Adding)

                binding.mainImageView.setImageBitmap(null)

                CoroutineScope(Dispatchers.Default).launch {
                    var imageView: ImageView? = null
                    for (i in 0 until uriList.size) {
                        val iStream: InputStream? = requireContext().contentResolver.openInputStream(uriList[i])
                        val sourceByteArray = imageToolModule.getBytes(iStream!!)

                        var isPossibleAdd = true

                        val currentImageContent = imageContent

                        val addedAiContainer = AiContainer(requireActivity())
                        val jop = async {
                            aiLoadResolver.createAiContainer(addedAiContainer, sourceByteArray)
                        }
                        jop.await()



                        if (isPossibleAdd) {
                            while (!addedAiContainer.imageContent.checkPictureList) {
                            }

                            val newPictureList = addedAiContainer.imageContent.pictureList
//
                            for (j in 0 until newPictureList.size) {
                                newPictureList[j].embeddedData = null
                                newPictureList[j].embeddedSize = 0
                                newPictureList[j].contentAttribute = ContentAttribute.basic
                                pictureList.add(newPictureList[j])

                                // subLayer 동적 추가
                                val subLayout = setSubImage(pictureList[pictureList.size - 1])
                                withContext(Dispatchers.Main) {
                                    binding.linear.addView(subLayout, pictureList.size - 1)
                                }
                                imageView =
                                    subLayout?.findViewById<ImageView>(R.id.scrollImageView)
                            }

                            // picture 변경
                            imageContent.pictureList = pictureList

                            // 이제 사진이 수정됨
                            imageContent.checkEditChanged = true
                            imageToolModule.showView(binding.saveBtn, true)
                        }
                        else{
                            withContext(Dispatchers.Main) {
                                Toast.makeText(requireContext(), "호환 되지 않는 사진은 추가할 수 없습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }


                    if(imageView != null) {
                        // 추가된 이미지 중에 마지막 이미지를 메인 뷰로 설정
                        withContext(Dispatchers.Main) {
                            jpegViewModel.selectedSubImage = pictureList[pictureList.size - 1]
                            val index = pictureList.indexOf(pictureList[pictureList.size - 1])
                            Log.d("main Change", "onClickListener : $index")
                            changeViewImage(index, imageView)
                            if (mainSubView != null)
                                setMoveScrollView(mainSubView!!, pictureList.size)

                            // 마무리 설정 (편집 메뉴설정 및 컨테이너에 표시되는 담긴 사진 n장 변경
                            setViewDetailMenu()
                            setContainerTextSetting()
                            checkAllInJPEG()
                        }
                    }
                    else {
                        CoroutineScope(Dispatchers.Main).launch {
                            Glide.with(binding.mainImageView)
                                .load(imageContent.getJpegBytes(jpegViewModel.selectedSubImage!!))
                                .into(binding.mainImageView)
                        }
                    }

                    // TODO: 코드 확인 필요
                    CoroutineScope(Dispatchers.Default).launch {
                        val bitmapSize = bitmapList.size
                        for(i in bitmapSize until pictureList.size)
                            imageContent.addBitmapList(
                                try {
                                    val byteArray = imageContent.getJpegBytes(pictureList[i])
                                    imageToolModule.byteArrayToBitmap(byteArray)
                                } catch (e: IndexOutOfBoundsException) { e.printStackTrace() } as Bitmap
                            )
                        bitmapList = imageContent.getBitmapList()!!
                    }

                    showProgressBar(false, null)
                }
            }
        }
    }

    /**
     * 하단 All-in JPEG 컨테이너에 이미지를 토대로 뷰(아이콘)을 제작하고 이미지를 눌렀을 때 이벤트 동작도 설정한 다음 뷰를 반환한다.
     *
     * @param picture 보여줄 이미지
     * @return 제작된 뷰 반환
     */
    private fun setSubImage(picture: Picture): View? {
        // 넣고자 하는 layout 불러오기
        try {
            val subLayout =
                layoutInflater.inflate(R.layout.scroll_item_layout_edit, null)

            // 위 불러온 layout에서 변경을 할 view가져오기
            val imageView: ImageView =
                subLayout.findViewById(R.id.scrollImageView)

            // 이미지뷰에 붙이기
            CoroutineScope(Dispatchers.Main).launch {
                Glide.with(imageView)
                    .load(imageContent.getJpegBytes(picture))
                    .into(imageView)
            }

            // 처음부터 선택한 이미지 보여주기
            if (picture == jpegViewModel.selectedSubImage) {
                CoroutineScope(Dispatchers.Main).launch {
                    imageView.setBackgroundResource(R.drawable.chosen_image_border)
                    imageView.setPadding(6)
                    mainSubView = imageView

                    if (picture != jpegViewModel.mainSubImage) {
                        imageToolModule.showView(binding.mainChangeBtn, true)
//                    imageToolModule.showView(binding.extractJpegBtn, true)
                    }

                    Glide.with(binding.mainImageView)
                        .load(imageContent.getJpegBytes(picture))
                        .into(binding.mainImageView)

                    setMoveScrollView(subLayout, pictureList.indexOf(picture))
                }
            }

            if (jpegViewModel.getMainSubImageIndex() == pictureList.indexOf(picture)) {
//                imageToolModule.showView(subLayout.findViewById(R.id.checkMainIcon), true)
                jpegViewModel.mainSubImage = picture

                CoroutineScope(Dispatchers.Main).launch {
                    mainTextView = subLayout.findViewById<TextView>(R.id.mainMark)
                    if (mainTextView != null)
                        imageToolModule.showView(mainTextView!!, true)
                }
            }

            subLayout.setOnClickListener {
                if(isButtonEnable) {
                    if (isMagicPlay) {
                        handler.removeCallbacksAndMessages(null)
                        binding.magicPlayBtn.setImageResource(R.drawable.edit_magic_icon)
                        isMagicPlay = false
                    }
                    // 이미지인 경우
                    jpegViewModel.selectedSubImage = picture
                    val index = pictureList.indexOf(picture)
                    Log.d("main Change", "onClickListener : $index")
                    changeViewImage(index, imageView)

                    if (picture.contentAttribute == ContentAttribute.distance_focus) {
                        binding.seekBar.progress = jpegViewModel.getSelectedSubImageIndex()
                    }
                }
            }

            // 삭제
            subLayout.findViewById<ImageView>(R.id.deleteIcon).setOnClickListener {
                // 이미지인 경우
//            changeViewImage(index, imageView)

                CoroutineScope(Dispatchers.Main).launch {
                    // 메인 이미지 설정
                    Glide.with(binding.mainImageView)
                        .load(imageContent.getJpegBytes(picture))
                        .into(binding.mainImageView)
                }

                val oDialog: AlertDialog.Builder = AlertDialog.Builder(
                    activity,
                    android.R.style.Theme_DeviceDefault_Light_Dialog
                )

                oDialog.setMessage("이미지를 삭제 하시겠습니까?")
                    .setPositiveButton(
                        "아니요"
                    ) { _, _ ->
                        CoroutineScope(Dispatchers.Main).launch {
                            // 메인 이미지 설정
                            Glide.with(binding.mainImageView)
                                .load(imageContent.getJpegBytes(jpegViewModel.selectedSubImage!!))
                                .into(binding.mainImageView)
                        }
                    }
                    .setNeutralButton("네") { _, _ ->
                        CoroutineScope(Dispatchers.Main).launch {
                            isPictureChanged.value = true
                        }
                        binding.linear.removeView(subLayout)
                        imageContent.removePicture(picture)

                        if (pictureList.size == 1) {
                            imageToolModule.showView(
                                binding.linear[0].findViewById<ImageView>(R.id.deleteIcon),
                                false
                            )
                            jpegViewModel.mainSubImage = pictureList[0]
                            mainSubView = binding.linear[0].findViewById<ImageView>(R.id.mainMark)
                            imageToolModule.showView(mainSubView!!, true)
                            setViewDetailMenu()
                        }

                        if (picture.contentAttribute == ContentAttribute.distance_focus) {
                            binding.seekBar.max -= 1
                        }
                        setContainerTextSetting()

                        CoroutineScope(Dispatchers.Main).launch {
                            // 메인 이미지 설정
                            Glide.with(binding.mainImageView)
                                .load(imageContent.getJpegBytes(jpegViewModel.selectedSubImage!!))
                                .into(binding.mainImageView)
                            imageContent.checkEditChanged = true
                            imageToolModule.showView(binding.saveBtn, true)
                        }
                        if (jpegViewModel.mainSubImage == picture && pictureList.size > 0) {
                            jpegViewModel.mainSubImage = pictureList[0]

                            CoroutineScope(Dispatchers.Main).launch {
                                mainTextView = binding.linear.getChildAt(0)
                                    .findViewById<TextView>(R.id.mainMark)
                                if (mainTextView != null)
                                    imageToolModule.showView(mainTextView!!, true)
                            }
                            mainPicture = pictureList[jpegViewModel.getSelectedSubImageIndex()]
                        }
                    }.show()
            }
            return subLayout
        } catch (e: IllegalStateException) {
            return null
        }
    }

    /**
     * 하단 All-in JPEG 컨테이너에 아이콘을 제작하고, 클릭시 이벤트를 설정한 다음 뷰를 반환한다.
     *
     * @param drawable_image 아이콘 이미지
     * @param clickedFunc 클릭 시 불러질 함수
     * @param deleteFunc 삭제 시 불러질 함수
     * @return 제작된 뷰 반환
     */
    private fun setContainerSubItem(drawable_image: Int, clickedFunc: (imageView: ImageView) -> Unit, deleteFunc: (subLayout: FrameLayout) -> Unit): View? {
        // 넣고자 하는 layout 불러오기
        val subLayout =
            layoutInflater.inflate(R.layout.scroll_item_layout_edit, null)

        // 위 불러온 layout에서 변경을 할 view가져오기
        val imageView: ImageView =
            subLayout.findViewById(R.id.scrollImageView)

        imageView.setImageResource(drawable_image)

        // 아이콘 클릭
        subLayout.setOnClickListener {
            if(isButtonEnable)
                clickedFunc(imageView)
        }

        // 삭제
        subLayout.findViewById<ImageView>(R.id.deleteIcon).setOnClickListener {
            deleteFunc(subLayout as FrameLayout)
        }

        return subLayout
    }

    /**
     * 이미지 추가 아이콘 클릭시 호출되는 함수로, 사진 선택을 위한 갤러리를 연다.
     */
    private fun AddImage(imageView: ImageView) {
        handler.removeCallbacksAndMessages(null)
        openGallery()
    }

    /**
     * 하단 All-in JPEG 컨테이너에 적히는 텍스트를 설정한다.
     * All-in JPEG에 몇 개의 이미지와 텍스트, 오디오 유무를 적는다.
     */
    private fun setContainerTextSetting() {
        // jpeg container의 텍스트 설정
        var imageCntText = "담긴 사진 ${imageContent.pictureList.size}장 "
        if (textContent.textList.size > 0) {
            imageCntText += "+ 텍스트"
        }
        if (audioContent.audio != null) {
            imageCntText += "+ 오디오"
        }
        CoroutineScope(Dispatchers.Main).launch {
            binding.imageCntTextView.text = imageCntText
        }
    }

    /* 오디오 추가 관련 */
    private fun ShowingAudio(imageView: ImageView) {
//        changeRound(imageView)
        Log.d("current_audio", " AddAudio() 호출")

        handler.removeCallbacksAndMessages(null)
        isAudioPlay = if(isAudioPlay) {
            imageView.setImageResource(R.drawable.edit_audio_icon)
            false
        } else {
            imageView.setImageResource(R.drawable.edit_audio_click_icon)
            true
        }
        val audioContentLayout = binding.audioContentLayout
        // 오디오 활성화
        if(binding.currentAudioBarLaydout.visibility == View.GONE){
            binding.currentAudioBarLaydout.visibility = View.VISIBLE
            jpegViewModel.isAudioPlay.value = 0
            setCurrentSeekBar()
            // 오디오 비활성화
        }else{
            binding.currentAudioBarLaydout.visibility = View.GONE
        }
    }

    private fun AddAudio(imageView: ImageView) {
//        changeRound(imageView)
        checkAllInJPEG()
        handler.removeCallbacksAndMessages(null)

        Log.d("add_test", " AddAudio() 호출")
        val dialog = ConfirmDialog( "Live 레코드를 추가 하시겠습니까?", GoAudioAddMode())
        // 알림창이 띄워져있는 동안 배경 클릭 막기
        dialog.isCancelable = false
        dialog.show(activity.supportFragmentManager, "ConfirmDialog")
        //isAudioAddMode = true
        // fitAudioModeUI()

    }

    private fun DeleteAudio(subLayout: FrameLayout) {
        val oDialog: AlertDialog.Builder = AlertDialog.Builder(
            activity,
            android.R.style.Theme_DeviceDefault_Light_Dialog
        )

        oDialog.setMessage("오디오를 삭제 하시겠습니까?")
            .setPositiveButton(
                "아니요"
            ) { _, _ -> }
            .setNeutralButton("네") { _, _ ->
                binding.linear.removeView(subLayout)
                setContainerTextSetting()

                // 삭제
                audioContent.init()
                binding.audioContentLayout.visibility = View.GONE
                setContainerTextSetting()
                imageContent.checkEditChanged = true
                imageToolModule.showView(binding.saveBtn, true)
                val view = setContainerSubItem(R.drawable.edit_audio_add_icon, clickedFunc = ::AddAudio, deleteFunc = ::DeleteAudio)
                binding.linear.addView(view, binding.linear.size - 1)
                if (view != null) {
                    imageToolModule.showView(view, false)
                }
                checkAllInJPEG()
                setContainerTextSetting()
            }.show()

    }

    private fun addCurrentAudioBarListener(){
        binding.currentPlayBtn.setOnClickListener {
            var num = jpegViewModel.isAudioPlay.value
            Log.d("current_audio", "${num}")
            // 정지 -> 재생
            if(jpegViewModel.isAudioPlay.value!! == 0){
                // 오디오 재생
                if(tempAudioFile != null){
                    jpegViewModel.isAudioPlay.value = 1
                    currentSeekBarPlay()
                    Log.d("current_audio", "플레이")
                }
            } else if(jpegViewModel.isAudioPlay.value!! == 1){
                // 재생 -> 정지
                Log.d("current_audio", "플레이")
            } else if(jpegViewModel.isAudioPlay.value!! == 2){
                jpegViewModel.isAudioPlay.value = 1
                Log.d("current_audio", "다시 재생1")
                if(tempAudioFile != null){
                    currentSeekBarPlay()
                    Log.d("current_audio", "다시 재생2")
                }
            }
        }
    }

    private fun addAudioModule() {

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

        // 수정 버튼
        binding.audioEditBtn.setOnClickListener{
            isAudioAddMode = true
            playinAudioUIStop()
            fitAudioModeUI()
        }

        binding.playBtn.setOnClickListener {
            // 정지 -> 재생
            if(jpegViewModel.isAudioPlay.value!! == 0){
                // 오디오 재생
                if(tempAudioFile != null){
                    jpegViewModel.isAudioPlay.value = 1
                    setSeekBar()
                    //mediaPlayer.start()
                }

            } else if(jpegViewModel.isAudioPlay.value!! == 1){
                // 재생 -> 정지
//                if(isPlaying){
//                    jpegViewModel.isAudioPlay.value = 0
//                    if (mediaPlayer != null) {
//                        mediaPlayer.stop()
//                    }
//                    // 재생 -> replay
//                }else
                //   jpegViewModel.isAudioPlay.value = 2
                // replay -> 재생
            } else if(jpegViewModel.isAudioPlay.value!! == 2){
                jpegViewModel.isAudioPlay.value = 1
                if(tempAudioFile != null){
                    setSeekBar()
                }
            }
        }
        jpegViewModel.isAudioPlay.observe(viewLifecycleOwner) {
            if(it == 0){
                binding.playBtn.setImageDrawable(resources.getDrawable(R.drawable.play2))
                binding.currentPlayBtn.setImageDrawable(resources.getDrawable(R.drawable.play2))
                // binding.back.visibility = View.VISIBLE
            }else if(it ==1){
                binding.playBtn.setImageDrawable(resources.getDrawable(R.drawable.pause2))
                binding.currentPlayBtn.setImageDrawable(resources.getDrawable(R.drawable.pause2))
            }else{
                binding.playBtn.setImageDrawable(resources.getDrawable(R.drawable.replay))
                binding.currentPlayBtn.setImageDrawable(resources.getDrawable(R.drawable.replay))
            }

        }


        binding.recordingImageView.setOnClickListener {
            if(isPlayingMode){
                /* 녹음 시작 */
                binding.playAudioBarLaydout.visibility = View.GONE
                binding.rawImageView2.visibility = View.VISIBLE
                binding.RecordingTextView.visibility = View.VISIBLE

                Glide.with(this).load(R.raw.giphy).into(binding.rawImageView2);
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
                binding.rawImageView2.visibility = View.GONE
                binding.RecordingTextView.visibility = View.INVISIBLE
                binding.recordingImageView.setImageDrawable(resources.getDrawable(R.drawable.refresh))
                timerUIStop()
                // 녹음 중단
                preTempAudioFile = tempAudioFile
                tempAudioFile = audioStop()

                if(tempAudioFile!= null){
                    setSeekBar()
                    jpegViewModel.isAudioPlay.value = 1
                }

                isRecordingMode = false
                isRecordedMode = true

            }
            else if(isRecordedMode){
                // dialog
                val dialog = ConfirmDialog("녹음된 내용을 지우고  다시 녹음하시겠습니까?",this)
                // 알림창이 띄워져있는 동안 배경 클릭 막기
                dialog.isCancelable = false
                dialog.show(activity.supportFragmentManager, "ConfirmDialog")
            }
        }
        if(tempAudioFile != null)
            setCurrentSeekBar()

        // 오디오 추가 창의 seek bar
        binding.seekBar2.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
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

        // 편집창의 오디오 재생 seek bar
        binding.currentSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
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
                    currentSeekBarPlay()
                }
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    fun fitAudioModeUI(){
        // 오디오 활성화
        if(isAudioAddMode) {
            binding.audioContentLayout.visibility = View.VISIBLE
            binding.audioButtonLinearLayout.visibility = View.VISIBLE

            binding.mainChangeLayout.visibility = View.GONE
            binding.containerLayout.visibility = View.GONE
            binding.seekBarLinearLayout.visibility = View.GONE
            // 편집 창 오디오 재생 바
            binding.currentAudioBarLaydout.visibility = View.GONE

            binding.playAudioBarLaydout.visibility = View.GONE
            binding.recordingImageView.setImageDrawable(resources.getDrawable(R.drawable.record))
            jpegViewModel.isAudioPlay.value = 0

            // 오디오 비활성화
        }else{
            binding.audioContentLayout.visibility = View.GONE
            binding.audioButtonLinearLayout.visibility = View.GONE
            binding.mainChangeLayout.visibility = View.VISIBLE
            binding.containerLayout.visibility = View.VISIBLE
            binding.seekBarLinearLayout.visibility = View.VISIBLE

            binding.currentAudioBarLaydout.visibility = View.GONE
            binding.constraintLayout.visibility = View.VISIBLE
        }

    }

    fun currentSeekBarPlay() {
        if(audioWithContent != null)
            audioWithContent.cancel()
        isPlaying = true
        Log.d("current_audio","currentSeekBarPlay 호출: 오디오 재생" )

        audioWithContent = CoroutineScope(Dispatchers.IO).launch {
            mediaPlayer.apply {
                try {
                    start()
                } catch (e: IOException) {
                    Log.e("AudioModule", "Failed to prepare media player: ${e.message}")
                }
            }
            // Seek bar process UI
            withContext(Dispatchers.Main) {
                binding.currentSeekbar.max = audioResolver.mediaPlayer.duration
                // 막대 바가 끝까지 도달 시 해당 코루틴 중단
                mediaPlayer.setOnCompletionListener {
                    isDestroy = true
                    isPlayingEnd = true
                    binding.currentSeekbar.clearFocus()
                    binding.currentSeekbar.progress = binding.currentSeekbar.max
                    binding.currentPlayBtn.setImageDrawable(resources.getDrawable(R.drawable.replay))
                    jpegViewModel.isAudioPlay.value = 2
                    coroutineContext.cancelChildren()
                }
                playinAudioUIStart(mediaPlayer.duration+1)
                while (true) {
                    if (mediaPlayer != null) {
                        if(isDestroy) {
                            isDestroy = false
                            break
                        }
                        val currentPosition: Int = mediaPlayer.currentPosition
                        binding.currentSeekbar.progress = currentPosition
                        delay(400)
                    } else {
                        break
                    }
                }
            }
        }
    }

    // 재생 바의 적절한 파일 로드
    private fun setCurrentSeekBar() {
        //  audioWithContent = CoroutineScope(Dispatchers.IO).launch {
        if(audioWithContent != null)
            audioWithContent.cancel()
        isDestroy = false
        Log.d("current_audio","setCurrentSeekBar 호출: 오디오 파일 로드" )
        mediaPlayer.reset()
        mediaPlayer.apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
            try {
                if(tempAudioFile!!.path != null){
                    setDataSource(tempAudioFile!!.path)
                    prepare()
                    Log.d("current_audio","setCurrentSeekBar 호출: ${mediaPlayer.duration}" )
                    var time = mediaPlayer.duration/1000
                    var string : kotlin.String = kotlin.String.format("%02d:%02d", time/60, time)
                    binding.currentPlayTime.setText(string)
                    binding.currentSeekbar.progress = 0
                }
            } catch (e: IOException) {
                Log.e("AudioModule", "Failed to prepare media player: ${e.message}")
            }
        }
    }

    fun setSeekBar() {
        isPlaying = true
        isDestroy = false
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
                binding.seekBar2.max = audioResolver.mediaPlayer.duration
                // 막대 바가 끝까지 도달 시 해당 코루틴 중단
                mediaPlayer.setOnCompletionListener {
                    isDestroy = true
                    isPlayingEnd = true
                    binding.seekBar2.clearFocus()
                    binding.seekBar2.progress = binding.seekBar2.max
                    jpegViewModel.isAudioPlay.value = 2
                    coroutineContext.cancelChildren()
                }

                playinAudioUIStart(mediaPlayer.duration+1)
                while (true) {
                    if (mediaPlayer != null) {
                        if(isDestroy) {
                            isDestroy = false
                            break
                        }
                        val currentPosition: Int = mediaPlayer.currentPosition
                        binding.seekBar2.progress = currentPosition
                        delay(100)
                    } else {
                        break
                    }
                }
            }
        }
    }

    private fun audioStop() : File? {
        // 녹음 중단, 저장
        var savedFile : File?  = audioResolver.stopRecording()
        isRecordingMode = false
        return savedFile
    }

    private fun saveAudioInMCContainer(savedFile : File){
        //MC Container에 추가
        var auioBytes = audioResolver.getByteArrayInFile(savedFile!!)
        jpegViewModel.jpegAiContainer.value!!.setAudioContent(auioBytes, ContentAttribute.basic)
    }

    private fun playinAudioUIStart(_time : Int, ){
        if(playingTimerTask != null)
            playingTimerTask!!.cancel()
        var time = _time/1000
        if(isPlaying){
            playingTimerTask = object : TimerTask() {
                var cnt = 0
                override fun run() {
                    CoroutineScope(Dispatchers.Main).launch {
                        // var time = _time/1000
                        var string : kotlin.String = kotlin.String.format("%02d:%02d", time/60, time)
                        // binding.currentPlayTime.setText(string)
                        Log.d("AudioModule", time.toString())
                        if(isAudioAddMode){
                            binding.playingTextView.setText(string)
                        } else {
                            binding.currentPlayTime.setText(string)
                        }
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

    fun playinAudioUIStop() {
        if(playingTimerTask != null)
            playingTimerTask!!.cancel()
        CoroutineScope(Dispatchers.Main).launch {
            binding.playingTextView.setText("00:00")
            binding.currentPlayTime.setText("00:00")
        }
    }

    private fun timerUIStart(){
        if(!isRecordingMode){
            timerTask = object : TimerTask() {
                var cnt = 0
                override fun run() {
                    CoroutineScope(Dispatchers.Main).launch {
                        var string : kotlin.String = kotlin.String.format("%02d:%02d", cnt/60, cnt)
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

    inner class GoAudioAddMode : ConfirmDialogInterface{
        override fun onYesButtonClick(id: Int) {
            Log.d("add_test", " AddAudio() 호출")
            isAudioAddMode = true
            fitAudioModeUI()
        }

    }

    /* 텍스트 추가 관련 */
    private fun addTextModual() {

        binding.editText.apply {
            // 그림자 효과 추가
            val shadowColor = Color.parseColor("#818181") // 그림자 색상
            val shadowDx = 5f // 그림자의 X 방향 오프셋
            val shadowDy = 0f // 그림자의 Y 방향 오프셋
            val shadowRadius = 3f // 그림자의 반경
            setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)

            textSize = 20.0F
        }
        // 포커스를 잃으면 수정하는 그림이 뜨도록
        binding.editText.onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                binding.imageView6.visibility = View.GONE
                binding.textCheckButton .visibility = View.VISIBLE
                binding.editText.apply {
                    // 그림자 효과 제거
                    setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
                    textSize = 17.0F

                }
            } else {
                if(textContent.textList.size > 0)
                    binding.imageView6.visibility = View.VISIBLE
                else
                    binding.editText.setText("")
                binding.textCheckButton.visibility = View.INVISIBLE
                binding.editText.apply {
                    // 그림자 효과 추가
                    val shadowColor = Color.parseColor("#818181") // 그림자 색상
                    val shadowDx = 5f // 그림자의 X 방향 오프셋
                    val shadowDy = 0f // 그림자의 Y 방향 오프셋
                    val shadowRadius = 3f // 그림자의 반경
                    setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)

                    textSize = 20.0F
                }
            }
        }
        binding.mainImageView.setOnClickListener {
            // 다른 영역 클릭 시 포커스를 잃도록 처리
            binding.editText.clearFocus()
            // 키보드 내리기
            val imm: InputMethodManager? =
                activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
            if (imm != null) {
                imm.hideSoftInputFromWindow(binding.editText.windowToken, 0)
            }
        }

        // text의 "확인" 클릭 시
        binding.textCheckButton.setOnClickListener {
            textMessageSave()
            checkAllInJPEG()
        }
        // text 입력 UI에 기존의 텍스트 메시지 띄우기
        var textList = jpegViewModel.jpegAiContainer.value!!.textContent.textList
        if(textList != null && textList.size !=0){
            val _text = textList.get(0).data.toString()
            binding.editText.setText(textList.get(0).data)
        }
    }

    private fun textMessageSave() {
        var textMessage: kotlin.String = binding.editText.text.toString()
        var textList: java.util.ArrayList<kotlin.String> = arrayListOf()

        textList.add(textMessage)
        if (textMessage != "") {
            jpegViewModel.jpegAiContainer.value!!.setTextConent(
                textList,
                ContentAttribute.basic,
            )
            imageContent.checkAdded = true
            CoroutineScope(Dispatchers.Main).launch {
                // 키보드 내리기
                val imm: InputMethodManager? =
                    activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
                if (imm != null) {
                    imm.hideSoftInputFromWindow(binding.editText.getWindowToken(), 0)
                }
                binding.editText.clearFocus()
                Toast.makeText(activity, "${binding.textCheckButton.text} 되었습니다.", Toast.LENGTH_SHORT).show();
                binding.textCheckButton.text = "수정"
            }

            binding.linear.removeViewAt(binding.linear.size - 3)
            val view = setContainerSubItem(R.drawable.edit_text_icon, clickedFunc = ::ShowingText, deleteFunc = ::DeleteText)
            setContainerTextSetting()

            imageContent.checkEditChanged = true

            CoroutineScope(Dispatchers.Main).launch{
                binding.linear.addView(view, binding.linear.size - 2)
                if(view != null) {
                    ShowingText(view.findViewById(R.id.scrollImageView))
                }

                // save 버튼 표시 | 메인 변경 버튼 없애기
                imageToolModule.showView(binding.saveBtn, true)
            }
        }
    }

    private fun ShowingText(imageView: ImageView) {
        handler.removeCallbacksAndMessages(null)
//        changeRound(imageView)
        isTextView = if(isTextView) {
            imageView.setImageResource(R.drawable.edit_text_icon)
            false
        } else {
            imageView.setImageResource(R.drawable.edit_text_click_icon)
            true
        }
        // 텍스트 활성화
        if(binding.textContentLayout.visibility == View.GONE){
            binding.textContentLayout.visibility = View.VISIBLE
            binding.editText.clearFocus()
            // 텍스트 비활성화
        } else{
            binding.textContentLayout.visibility = View.GONE
        }
    }

    private fun AddText(imageView: ImageView) {
        handler.removeCallbacksAndMessages(null)
        isTextView = if(isTextView) {
            imageView.setImageResource(R.drawable.edit_text_add_icon)
            false
        } else {
            imageView.setImageResource(R.drawable.edit_text_add_click_icon)
            true
        }
//        changeRound(imageView)
        if(binding.textContentLayout.visibility == View.GONE){
            binding.textContentLayout.visibility = View.VISIBLE
            binding.editText.requestFocus()
            binding.textCheckButton.text = "추가"
        } else{
            binding.textContentLayout.visibility = View.GONE
        }
        Log.d("add_test", " ADDText() 호출")
    }

    private fun DeleteText(subLayout: FrameLayout) {
        val oDialog: AlertDialog.Builder = AlertDialog.Builder(
            activity,
            android.R.style.Theme_DeviceDefault_Light_Dialog
        )

        oDialog.setMessage("텍스트를 삭제 하시겠습니까?")
            .setPositiveButton(
                "아니요"
            ) { _, _ -> }
            .setNeutralButton("네") { _, _ ->
                binding.linear.removeView(subLayout)
                setContainerTextSetting()

                // 삭제
                textContent.init()
                binding.textContentLayout.visibility = View.GONE
                binding.editText.setText("")
                imageContent.checkEditChanged = true
                imageToolModule.showView(binding.saveBtn, true)
                setContainerTextSetting()
                val view = setContainerSubItem(R.drawable.edit_text_add_icon, clickedFunc = ::AddText, deleteFunc = ::DeleteText)
                binding.linear.addView(view, binding.linear.size - 2)

                if (view != null) {
                    imageToolModule.showView(view, false)
                }
                checkAllInJPEG()
                setContainerTextSetting()

            }.show()

        // 데이터 삭제

    }

}