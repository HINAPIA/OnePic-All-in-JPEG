package com.goldenratio.onepic.EditModule.Fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.Context
import android.content.DialogInterface
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
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.get
import androidx.core.view.setPadding
import androidx.core.view.size
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.goldenratio.onepic.EditModule.MagicPictureModule
import com.goldenratio.onepic.*
import com.goldenratio.onepic.AudioModule.AudioResolver
import com.goldenratio.onepic.EditModule.RewindModule
import com.goldenratio.onepic.EditModule.ShakeLevelModule
import com.goldenratio.onepic.LoadModule.LoadResolver
import com.goldenratio.onepic.PictureModule.AudioContent
import com.goldenratio.onepic.PictureModule.Contents.ContentAttribute
import com.goldenratio.onepic.PictureModule.Contents.Picture
import com.goldenratio.onepic.PictureModule.ImageContent
import com.goldenratio.onepic.PictureModule.MCContainer
import com.goldenratio.onepic.PictureModule.TextContent
import com.goldenratio.onepic.ViewerModule.Fragment.ViewerFragment
import com.goldenratio.onepic.ViewerModule.ViewerEditorActivity
import com.goldenratio.onepic.databinding.AudioDialogBinding
import com.goldenratio.onepic.databinding.FragmentAudioAddBinding
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
import kotlin.IllegalStateException
import kotlin.Int
import kotlin.Long
import kotlin.Unit
import kotlin.collections.ArrayList
import kotlin.getValue


class EditFragment : Fragment(R.layout.fragment_edit), ConfirmDialogInterface {

    private lateinit var binding: FragmentEditBinding
    private lateinit var activity: ViewerEditorActivity
    private val jpegViewModel by activityViewModels<JpegViewModel>()

    private lateinit var imageToolModule: ImageToolModule
    private lateinit var magicPictureModule: MagicPictureModule
    private lateinit var loadResolver: LoadResolver

    private lateinit var imageContent: ImageContent
    private lateinit var textContent: TextContent
    private lateinit var audioContent: AudioContent

    private var imageTool = ImageToolModule()

    private var isFinished = MutableLiveData<Boolean>()

    private var mainSubView: View? = null
    private var mainTextView: TextView? = null

    private var pictureList = arrayListOf<Picture>()
    //    private var pictureByteList = mutableListOf<ByteArray>()
    private lateinit var mainPicture: Picture

    private var isSaving = false
    private var isAudioPlay = false
    private var isTextView = false
    private var isPictureChanged : MutableLiveData<Boolean> = MutableLiveData(false)

    private var isAudio = false
    private var isText = false
    private var isDeleting = false
    private var isMagicPlay: Boolean = false

    // Audio 없다가 추가, 수정
    private  var isAudioAddMode = false

    private var bestImageIndex: Int? = null

    private val PICK_IMAGE_REQUEST = 1

    private var bitmapList :ArrayList<Bitmap> = arrayListOf<Bitmap>()
    private var overlayBitmap = arrayListOf<Bitmap>()
    val handler = Handler()
    var magicPlaySpeed: Long = 100


    private var infoLevel = MutableLiveData<InfoLevel>()
    private var isInfoViewed = true

    private enum class InfoLevel {
        BeforeMainSelect
    }

    private enum class LoadingText {
        BestImageRecommend,
        Save,
        MagicPlay,
        EditReady
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as ViewerEditorActivity
        audioResolver = AudioResolver(activity)
        mediaPlayer = audioResolver.mediaPlayer
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
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        bundle: Bundle?
    ): View {

        // 상태바 색상 변경
        val window: Window = activity.window
            ?: throw IllegalStateException("Fragment is not attached to an activity")
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.setStatusBarColor(ContextCompat.getColor(requireContext(), android.R.color.black))

        // isFinished가 ture가 되면 viewerFragment로 전환
        isFinished.observe(requireActivity()) { value ->
            if (value == true) {
                CoroutineScope(Dispatchers.Main).launch {
                    findNavController().navigate(R.id.action_editFragment_to_viewerFragment)
                }
                isFinished.value = false // 초기화
            }
        }

        // 뷰 바인딩 설정
        binding = FragmentEditBinding.inflate(inflater, container, false)

        // imageToolModule 설정
        imageToolModule = ImageToolModule()

//        imageToolModule.showView(binding.progressBar, true)
        showProgressBar(true, LoadingText.EditReady)

        CoroutineScope(Dispatchers.Default).launch {
            loadResolver = LoadResolver()

            // Content 설정
            imageContent = jpegViewModel.jpegMCContainer.value?.imageContent!!

            val selected = jpegViewModel.selectedSubImage
            if(selected != null)
                magicPictureModule = MagicPictureModule(imageContent, selected)
//        imageContent.setMainBitmap(null)
            textContent = jpegViewModel.jpegMCContainer.value!!.textContent
            audioContent = jpegViewModel.jpegMCContainer.value!!.audioContent


            // picture 리스트 만들어질때까지
            while (!imageContent.checkPictureList) { }

            // picture 설정
            mainPicture = imageContent.mainPicture
            pictureList = imageContent.pictureList
            CoroutineScope(Dispatchers.IO).launch {
                val newBitmapList = imageContent.getBitmapList()
                if (newBitmapList != null) {
                    bitmapList = newBitmapList
                }
            }

            // 만약 편집을 했다면 저장 버튼이 나타나게 설정
            if (imageContent.checkMainChangeAttribute || imageContent.checkRewindAttribute ||
                imageContent.checkMagicAttribute || imageContent.checkAddAttribute
            ) {
                imageToolModule.showView(binding.saveBtn, true)
            }
            setViewDetailMenu()

            // 메인 이미지 설정
            CoroutineScope(Dispatchers.Main).launch {
                val index = jpegViewModel.getSelectedSubImageIndex()
                if (pictureList[index].contentAttribute == ContentAttribute.magic) {
                    imageToolModule.showView(binding.magicPlayBtn, true)
//                    imageToolModule.showView(binding.progressBar, true)
                    showProgressBar(true, LoadingText.MagicPlay)
                    setMagicPlay()
//            Toast.makeText(requireContext(), "매직 사진입니다", Toast.LENGTH_LONG).show()
                }
                Glide.with(binding.mainImageView)
                    .load(imageContent.getJpegBytes(pictureList[index]))
                    .into(binding.mainImageView)

                // distance focus일 경우 seekbar 보이게
                if (imageContent.checkAttribute(ContentAttribute.distance_focus)) {
                    imageToolModule.showView(binding.seekBar, true)
                    setDistanceSeekBar()
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


//            imageToolModule.showView(binding.progressBar, false)
            showProgressBar(false, null)
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
        return binding.root
    }

    private fun setViewDetailMenu() {
        if(pictureList.size <= 1) {
            CoroutineScope(Dispatchers.Main).launch {
                binding.bestMainBtn.visibility = View.GONE
                binding.rewindBtn.visibility = View.GONE
                binding.magicBtn.visibility = View.GONE
            }
        } else {
            CoroutineScope(Dispatchers.Main).launch {
                binding.bestMainBtn.visibility = View.VISIBLE
                binding.rewindBtn.visibility = View.VISIBLE
                binding.magicBtn.visibility = View.VISIBLE
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Rewind 버튼 클릭 이벤트 리스너 등록
        binding.rewindBtn.setOnClickListener {
            // RewindFragment로 이동
            findNavController().navigate(R.id.action_editFragment_to_rewindFragment)
        }
        // 움직이는 Magic 버튼 클릭 이벤트 리스너 등록
        binding.magicBtn.setOnClickListener {
            // MagicPictureFragment로 이동
            findNavController().navigate(R.id.action_editFragment_to_magicPictureFragment)
        }

        // 얼굴 추천 버튼 클릭 이벤트 리스너 등록
        binding.bestMainBtn.setOnClickListener {
//            imageToolModule.showView(binding.progressBar, true)
            viewBestImage()
        }

        /* 텍스트 추가 */
        addTextModuale()


        /* 오디오 */


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

        addAudioModule()

//        // audio ADD
//        binding.audioAddBtn.setOnClickListener {
//            // 일반 사진이면 안 넘어가도록
//            if (checkAdd) {
//                // MagicPictureFragment로 이동
//                findNavController().navigate(R.id.action_editFragment_to_audioAddFragment)
//            }
//        }
//
//        // text ADD
//        binding.textAddBtn.setOnClickListener {
//            findNavController().navigate(R.id.action_editFragment_to_addFragment)
//        }


        // 메인 변경
        binding.mainChangeBtn.setOnClickListener {
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
            imageContent.checkMainChangeAttribute = true

            // 저장 버튼 표시 | 메인 변경 버튼 없애기
            imageToolModule.showView(binding.saveBtn, true)
            imageToolModule.showView(binding.mainChangeBtn, false)
        }

        // 컨텐츠 삭제
        binding.contentDeleteBtn.setOnClickListener {
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

        binding.magicPlayBtn.setOnClickListener {
            if (!isMagicPlay) {
                showProgressBar(true, LoadingText.MagicPlay)
                CoroutineScope(Dispatchers.Main).launch {
                    binding.magicPlayBtn.setImageResource(R.drawable.edit_magic_ing_icon)
                }
//                    imageToolModule.showView(binding.magicPlayBtn, true)

                    isMagicPlay = true

                CoroutineScope(Dispatchers.Default).launch {
                    if (overlayBitmap.isEmpty() ) {
                        while (!magicPictureModule.isInit) {}
                        showProgressBar(false, null)
                        overlayBitmap = magicPictureModule.magicPictureProcessing()
                    }

                    Log.d("magic", "magicPictureProcessing end ${overlayBitmap.size}")
                    showProgressBar(false, null)
                    Log.d("magic", "magicPucture run ${overlayBitmap.size}")
                    magicPictureRun(overlayBitmap)
                }
            } else {
                handler.removeCallbacksAndMessages(null)

                CoroutineScope(Dispatchers.Main).launch {
                    binding.magicPlayBtn.setImageResource(R.drawable.edit_magic_icon)

//                        binding.progressBar.visibility = View.GONE
                    showProgressBar(false, null)
                }
                isMagicPlay = false
            }
        }

        // 취소 버튼 (viewer로 이동)
        binding.backBtn.setOnClickListener {
            // 변경된 편집이 있을 경우 확인 창 띄우기
            if (imageContent.checkMainChangeAttribute || imageContent.checkRewindAttribute ||
                imageContent.checkMagicAttribute || imageContent.checkAddAttribute
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
                                loadResolver.createMCContainer(jpegViewModel.jpegMCContainer.value!!,sourceByteArray) }
                            job.await()

                            while(!imageContent.checkPictureList) {}
                            CoroutineScope(Dispatchers.Default).launch {
                                imageContent.setBitmapList()
                            }
                            findNavController().navigate(R.id.action_editFragment_to_viewerFragment)
                        }

                    }.show()
            } else {
                findNavController().navigate(R.id.action_editFragment_to_viewerFragment)
            }
        }

        // 저장 버튼 (viewer로 이동)
        binding.saveBtn.setOnClickListener {
            ViewerFragment.isEditStoraged = true
            // 저장 중인지 확인하는 flag가 false일 경우만 저장 단계 실행 --> 두번 실행될 경우 오류를 예외처리하기 위해
            if (!isSaving) {
//                imageTool.showView(binding.progressBar, true)
                showProgressBar(true, LoadingText.Save)
                isSaving = true

                imageContent.pictureList = pictureList
                imageContent.pictureCount = pictureList.size

                // 1. 메인으로 하고자하는 picture를 기존의 pictureList에서 제거
                val removeResult = imageContent.removePicture(mainPicture)
                if (removeResult) {
                    // 2. main 사진을 첫번 째로 삽입
                    imageContent.insertPicture(0, mainPicture)
                    imageContent.mainPicture = mainPicture
                }

               // textMessageSave()

                // 덮어쓰기
                val currentFilePath = jpegViewModel.currentImageUri

                var fileName = ""
                // 파일 이름 얻어내기

                // 13버전 이상일 경우는 uri를 받아 옴 --> 전처리 거쳐서 파일 이름 얻어내기
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    fileName = jpegViewModel.getFileNameFromUri(currentFilePath!!.toUri())
                }
                // 13버전 이하일 경우는 file path를 받아 옴
                else {
                    fileName = currentFilePath!!.substring(currentFilePath.lastIndexOf("/") + 1);
                }

                CoroutineScope(Dispatchers.IO).launch {
                    jpegViewModel.currentFileName = fileName
                    // 기존 파일 삭제
                    jpegViewModel.jpegMCContainer.value?.saveResolver?.deleteImage(fileName)
                    var i =0
                    while (JpegViewModel.isUserInentFinish == false){
                        Log.d("save_test", "${i++}")
                        delay(500)
                    }
                    JpegViewModel.isUserInentFinish = false
                    jpegViewModel.jpegMCContainer.value?.overwiteSave(fileName)
                    Thread.sleep(2000)
                    Log.d("save_test", "뷰어로 넘어가기")
                    setButtonDeactivation()
                    setCurrentPictureByteArrList()
                }

                // 우리 앱의 사진이 아닐 때
//                if (result == "another") {
//
//                    // 권한을 요청하고 다시 save
//                    singleSave()
//                    CoroutineScope(Dispatchers.Default).launch {
//                        Thread.sleep(1000)
//                        setButtonDeactivation()
//                        setCurrentPictureByteArrList()
//                    }
//                } else {
//                    CoroutineScope(Dispatchers.Default).launch {
//                        setButtonDeactivation()
//                        Thread.sleep(2000)
//                        setCurrentPictureByteArrList()
//                    }
//                }
                imageContent.setCheckAttribute()
            }
        }

        /* 오디오 리스너 */
        // 오디오 취소
        binding.audioCancleBtn.setOnClickListener {
            isAudioAddMode = false
            fitAudioModeUI()
            jpegViewModel.isAudioPlay.value = 0
            isPlayingMode = true
            isRecordedMode = false
            isRecordingMode = false
        }

        // 오디오 추가가
        addCurrentAudioBarListener()
        binding.audioCheckBtn.setOnClickListener {
            // 녹음 내역 저장
            if(tempAudioFile != null){
                saveAudioInMCContainer(tempAudioFile!!)
                jpegViewModel.jpegMCContainer.value!!.audioContent.audio!!._audioByteArray?.let { it1 ->
                    audioResolver.saveByteArrToAacFile(
                        it1, "viewer_record")
                }
            }
            // audioResolver.audioStop()
            imageContent.checkAddAttribute = true

            CoroutineScope(Dispatchers.Main).launch {
                binding.RecordingTextView.setText("")
                Toast.makeText(activity, "추가 되었습니다.", Toast.LENGTH_SHORT).show();

                isPlayingMode = true
                isRecordedMode = false
                isRecordingMode = false

                isAudioAddMode = false
                fitAudioModeUI()
            }
        }
    }

    fun fitAudioModeUI(){
        // 오디오 활성화
        if(isAudioAddMode) {
            binding.audioContentLayout.visibility = View.VISIBLE
            binding.mainChangeLayout.visibility = View.GONE
            binding.containerLayout.visibility = View.GONE

            binding.playAudioBarLaydout.visibility = View.GONE
            binding.recordingImageView.setImageDrawable(resources.getDrawable(R.drawable.record))
            jpegViewModel.isAudioPlay.value = 0
           // binding.constraintLayout.visibility = View.INVISIBLE

//            binding.bestMainBtn.visibility = View.GONE
//            binding.rewindBtn.visibility = View.GONE
//            binding.magicBtn.visibility = View.GONE

            // 오디오 비활성화
        }else{
            binding.audioContentLayout.visibility = View.GONE
            binding.mainChangeLayout.visibility = View.VISIBLE
            binding.containerLayout.visibility = View.VISIBLE
            //binding.constraintLayout.visibility = View.VISIBLE


        }

    }

    fun setButtonDeactivation() {
        imageContent.checkAddAttribute = false
        imageContent.checkRewindAttribute = false
        imageContent.checkMagicAttribute = false
        imageContent.checkMainChangeAttribute = false
        jpegViewModel.mainSubImage = null
    }

    fun setCurrentPictureByteArrList() {
        var pictureList = jpegViewModel.jpegMCContainer.value?.getPictureList()

        if (pictureList != null) {

            CoroutineScope(Dispatchers.IO).launch {
                val pictureByteArrayList = mutableListOf<ByteArray>()
                for (picture in pictureList) {
                    val pictureByteArr =
                        jpegViewModel.jpegMCContainer.value?.imageContent?.getJpegBytes(picture)
                    pictureByteArrayList.add(pictureByteArr!!)
                } // end of for..
                jpegViewModel.setpictureByteArrList(pictureByteArrayList)
                CoroutineScope(Dispatchers.Main).launch {
                    isFinished.value = true
                }
            }
        }
    }

    fun remainOriginalPictureSave() {
        val oDialog: AlertDialog.Builder = AlertDialog.Builder(
            activity,
            android.R.style.Theme_DeviceDefault_Light_Dialog
        )
//        imageTool.showView(binding.progressBar, false)

        oDialog.setMessage("편집된 이미지만 저장하시겠습니까? 원본 이미지는 사라지지 않습니다.")
            .setPositiveButton(
                "모두 저장",
                DialogInterface.OnClickListener { dialog, which ->
                    imageTool.showView(binding.progressBar, true)
                    if (!imageContent.checkMagicAttribute || !imageContent.checkRewindAttribute) {
                        val mainPicture = imageContent.mainPicture
                        // 바뀐 비트맵을 Main(맨 앞)으로 하는 새로운 Jpeg 저장
                        imageContent.insertPicture(0, mainPicture)
                    }
                    jpegViewModel.jpegMCContainer.value?.save()
                    CoroutineScope(Dispatchers.Default).launch {
                        setButtonDeactivation()
                        Thread.sleep(2000)
                        withContext(Dispatchers.Main) {
//                        imageTool.showView(binding.progressBar , false)
                            findNavController().navigate(R.id.action_editFragment_to_viewerFragment)
                        }
                    }
                })
            .setNeutralButton("예",
                DialogInterface.OnClickListener { dialog, which ->
                    try {
                        singleSave()
                    } catch (e: IOException) {
                        Toast.makeText(activity, "저장에 실패 했습니다.", Toast.LENGTH_SHORT)
                            .show()
                    }
                })
            .show()
    }

    fun singleSave() {
        try {
//            imageTool.showView(binding.progressBar, true)
            showProgressBar(true, LoadingText.Save)
            val newImageContent =
                jpegViewModel.jpegMCContainer.value?.imageContent!!
            val singlePictureList: ArrayList<Picture> =
                ArrayList<Picture>(1)
            singlePictureList.add(newImageContent.mainPicture)
            newImageContent.setContent(singlePictureList)

            var savedFilePath = jpegViewModel.jpegMCContainer.value?.save()
            //ViewerFragment.currentFilePath = savedFilePath.toString()

        } catch (e: IOException) {
            Toast.makeText(activity, "저장에 실패 했습니다.", Toast.LENGTH_SHORT)
                .show()
        }

    }

    /**
     * setContainer()
     *      : 아래 서브 이미지를 설정
     */
    @SuppressLint("MissingInflatedId")
    fun setContainer() {
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

        val view = setContainerSubItem(R.drawable.edit_image_add_icon, clickedFunc = ::AddImage, deleteFunc = fun() {})
        binding.linear.addView(view)
    }

    fun viewBestImage() {
        showProgressBar(true, LoadingText.BestImageRecommend)
        val newBitmapList = imageContent.getBitmapList()
        if (newBitmapList != null) {
            bitmapList = newBitmapList
            val rewindModule = RewindModule()
            CoroutineScope(Dispatchers.IO).launch {

                if (bestImageIndex == null ) {
                    rewindModule.allFaceDetection(bitmapList)
                    val faceDetectionResult = rewindModule.choiceBestImage(bitmapList)
                    Log.d("anaylsis", "end faceDetection")

                    val shakeDetectionResult =
                        ShakeLevelModule().shakeLevelDetection(bitmapList)

                    val analysisResults = arrayListOf<Double>()

                    var preBestImageIndex = 0

                    for (i in 0 until bitmapList.size) {

                        Log.d("anaylsis", "[$i] =  faceDetectio ${faceDetectionResult[i]} ")
                        Log.d("anaylsis", "[$i] =  shake ${shakeDetectionResult[i]}")

                        analysisResults.add(faceDetectionResult[i] + shakeDetectionResult[i])
                        if (analysisResults[preBestImageIndex] < analysisResults[i] ||
                            analysisResults[preBestImageIndex] == analysisResults[i] && faceDetectionResult[preBestImageIndex] < faceDetectionResult[i]
                        ) {
                            preBestImageIndex = i
                        }
                    }

                    Log.d("anaylsis", "=== ${analysisResults[preBestImageIndex]}")
                    println("bestImageIndex = $preBestImageIndex")

                    bestImageIndex = preBestImageIndex
                }
                withContext(Dispatchers.Main) {
                    Glide.with(binding.mainImageView)
                        .load(imageContent.getJpegBytes(pictureList[bestImageIndex!!]))
                        .into(binding.mainImageView)

                    changeViewImage(bestImageIndex!!, binding.linear.getChildAt(bestImageIndex!!)
                        .findViewById<ImageView>(R.id.scrollImageView))
//                    mainSubView =
//                    mainSubView.setBackgroundResource(R.drawable.chosen_image_border)
//                     mainSubView.setPadding(6)
//
////                    jpegViewModel.selectPictureIndex = bestImageIndex!!
//                    jpegViewModel.selectedSubImage = pictureList[bestImageIndex!!]
//
//                    if (bestImageIndex != jpegViewModel.getMainSubImageIndex()) {
//                        imageToolModule.showView(binding.mainChangeBtn, true)
//                    } else {
//                        imageToolModule.showView(binding.mainChangeBtn, false)
//                    }
//
//                    imageToolModule.showView(binding.progressBar, false)
                    showProgressBar(false, null)
//                    setMoveScrollView(mainSubView, bestImageIndex!!)

                    Log.d("mainChange", "bestImage null")
                }
            }
        }
    }

    /**
     * setMoveScrollView(subLayout: View, index: Int)
     *      : 해당 뷰를 스크롤의 가운데로 가게 해주는 함수
     */
    fun setMoveScrollView(subLayout: View, index: Int) {
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

    fun changeViewImage(index: Int, imageView: ImageView) {
            mainSubView?.background = null
            mainSubView?.setPadding(0)

        if(pictureList[index].contentAttribute == ContentAttribute.magic) {
            imageToolModule.showView(binding.magicPlayBtn, true)
        }
        else {
            imageToolModule.showView(binding.magicPlayBtn, false)
        }

        CoroutineScope(Dispatchers.Main).launch {
            // 메인 이미지 설정
            Glide.with(binding.mainImageView)
                .load(imageContent.getJpegBytes(pictureList[index]))
                .into(binding.mainImageView)
        }
        if (jpegViewModel.getMainSubImageIndex() != index) {
            imageToolModule.showView(binding.mainChangeBtn, true)
        } else {
            imageToolModule.showView(binding.mainChangeBtn, false)
        }
//        jpegViewModel.selectPictureIndex = index

        imageView.setBackgroundResource(R.drawable.chosen_image_border)
        imageView.setPadding(6)

        mainSubView = imageView
    }

    private fun openGallery() {
//        val intent = Intent(Intent.ACTION_PICK)
//        intent.type = "image/*"
//        startActivityForResult(intent, PICK_IMAGE_REQUEST)

        val intent = Intent(Intent.ACTION_PICK)
        intent.type = MediaStore.Images.Media.CONTENT_TYPE
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

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

            } else {      // 이미지를 여러장 선택한 경우
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
            if(uriList.size > 0) {
                for(i in 0 until uriList.size) {
                    val iStream: InputStream? = requireContext().contentResolver.openInputStream(uriList[i])
                    val sourceByteArray = imageToolModule.getBytes(iStream!!)
                    val isContainerChanged = MutableLiveData<Boolean>()

                    CoroutineScope(Dispatchers.Main).launch {
                        val jpegMCContainer = MCContainer(requireActivity())
                        loadResolver.createMCContainer(jpegMCContainer, sourceByteArray)
                        while (!jpegMCContainer.imageContent.checkPictureList) {
                        }

                        val newPictureList = jpegMCContainer.imageContent.pictureList
//                        pictureList.addAll(newPictureList)

//                        pictureByteList.add(sourceByteArray)
                        for (j in 0 until newPictureList.size) {
//                            newImageByteArrayList.add(
//                                jpegMCContainer.imageContent.getJpegBytes(
//                                    newPictureList[j]
//                                )
//                            )
                            pictureList.add(newPictureList[j])
                            val subLayout = setSubImage(pictureList[pictureList.size - 1])
                            binding.linear.addView(subLayout, pictureList.size - 1)
                            val imageView = subLayout?.findViewById<ImageView>(R.id.scrollImageView)

                            CoroutineScope(Dispatchers.Default).launch {
                                imageContent.addBitmapList(
                                    imageToolModule.byteArrayToBitmap(
                                        jpegMCContainer.imageContent.getJpegBytes(newPictureList[j])
                                    )
                                )
                            }

                            if (j == newPictureList.size - 1) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    Glide.with(binding.mainImageView)
                                        .load(imageContent.getJpegBytes(pictureList[pictureList.size - 1]))
                                        .into(binding.mainImageView)

                                    mainSubView?.background = null
                                    mainSubView?.setPadding(0)

                                    mainSubView = imageView!!

                                    mainSubView?.setBackgroundResource(R.drawable.chosen_image_border)
                                    mainSubView?.setPadding(6)

                                    if(mainSubView != null)
                                        setMoveScrollView(mainSubView!!, pictureList.size)
                                }
                            }
                            imageContent.pictureList = pictureList
                            imageContent.checkMainChangeAttribute = true
                            imageToolModule.showView(binding.saveBtn, true)
                        }
                        setViewDetailMenu()
                    }


                    //TODO: 유진아 여기 --> uriList: 방금 불러온 이미지들의 uri / newImageByteArrayList: 지금까지 불러온 이미지들의 bytearray
                }
            }
        }
    }

    fun setSubImage(picture: Picture): View? {
        // 넣고자 하는 layout 불러오기
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

        if (picture == jpegViewModel.selectedSubImage) {
            CoroutineScope(Dispatchers.Main).launch {
                imageView.setBackgroundResource(R.drawable.chosen_image_border)
                imageView.setPadding(6)
                mainSubView = imageView

                if (picture != jpegViewModel.mainSubImage) {
                    imageToolModule.showView(binding.mainChangeBtn, true)
                }

                Glide.with(binding.mainImageView)
                    .load(imageContent.getJpegBytes(picture))
                    .into(binding.mainImageView)


                setMoveScrollView(subLayout, pictureList.indexOf(picture))
            }
        }

        if (picture == jpegViewModel.mainSubImage ||
            jpegViewModel.mainSubImage == null && pictureList.indexOf(picture) == 0) {
//                imageToolModule.showView(subLayout.findViewById(R.id.checkMainIcon), true)

            jpegViewModel.mainSubImage = picture

            CoroutineScope(Dispatchers.Main).launch {
                mainTextView = subLayout.findViewById<TextView>(R.id.mainMark)
                if (mainTextView != null)
                    imageToolModule.showView(mainTextView!!, true)
            }
        }

        subLayout.setOnClickListener {

            if(isMagicPlay) {
                handler.removeCallbacksAndMessages(null)
                binding.magicPlayBtn.setImageResource(R.drawable.edit_magic_icon)
                isMagicPlay = false
            }
            // 이미지인 경우
            jpegViewModel.selectedSubImage = picture
            val index = pictureList.indexOf(picture)
            Log.d("main Change", "onClickListener : $index" )
            changeViewImage(index, imageView)

            if(picture.contentAttribute == ContentAttribute.distance_focus) {
                binding.seekBar.progress = jpegViewModel.getSelectedSubImageIndex()
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

            oDialog.setMessage("삭제 하시겠습니까?")
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
                        imageToolModule.showView(binding.linear[0].findViewById<ImageView>(R.id.deleteIcon), false)
                        jpegViewModel.mainSubImage = pictureList[0]

                        setViewDetailMenu()
                    }

                    if(picture.contentAttribute == ContentAttribute.distance_focus) {
                        binding.seekBar.max -= 1
                    }
                    setContainerTextSetting()

                    CoroutineScope(Dispatchers.Main).launch {
                        // 메인 이미지 설정
                        Glide.with(binding.mainImageView)
                            .load(imageContent.getJpegBytes(jpegViewModel.selectedSubImage!!))
                            .into(binding.mainImageView)


                        imageContent.checkMainChangeAttribute = true
                        imageToolModule.showView(binding.saveBtn, true)
                    }

                    if (jpegViewModel.mainSubImage == picture) {
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

//        CoroutineScope(Dispatchers.Main).launch {
//            // main activity에 만들어둔 scrollbar 속 layout의 아이디를 통해 해당 layout에 넣기
//            val index = pictureList.indexOf(picture)
//            if (binding.linear.size - 3 == index) {
//                binding.linear.addView(subLayout, binding.linear.size - 3)
//                Log.d("main Change","sublayout index add : $index")
//            } else {
//                binding.linear.addView(subLayout)
//                Log.d("main Change","sublayout add : $index")
//            }
//
//        }
        return subLayout
    }

    fun setContainerSubItem(drawable_image: Int, clickedFunc: (imageView: ImageView) -> Unit, deleteFunc: () -> Unit): View? {
        // 넣고자 하는 layout 불러오기
        val subLayout =
            layoutInflater.inflate(R.layout.scroll_item_layout_edit, null)

        // 위 불러온 layout에서 변경을 할 view가져오기
        val imageView: ImageView =
            subLayout.findViewById(R.id.scrollImageView)

        imageView.setImageResource(drawable_image)

        subLayout.setOnClickListener {
            clickedFunc(imageView)
        }

        // 삭제
        subLayout.findViewById<ImageView>(R.id.deleteIcon).setOnClickListener {

            val oDialog: AlertDialog.Builder = AlertDialog.Builder(
                activity,
                android.R.style.Theme_DeviceDefault_Light_Dialog
            )

            oDialog.setMessage("삭제 하시겠습니까?")
                .setPositiveButton(
                    "아니요"
                ) { _, _ -> }
                .setNeutralButton("네") { _, _ ->
                    binding.linear.removeView(subLayout)
                    setContainerTextSetting()
                    deleteFunc()
                }.show()
        }

        return subLayout
    }


    //
//    fun ShowingText(imageView: ImageView) {
//        handler.removeCallbacksAndMessages(null)
////        changeRound(imageView)
//        isTextView = if(isTextView) {
//            imageView.setImageResource(R.drawable.edit_text_icon)
//            false
//        } else {
//            imageView.setImageResource(R.drawable.edit_text_click_icon)
//            true
//        }
//    }

//    fun AddText(imageView: ImageView) {
//        handler.removeCallbacksAndMessages(null)
////        changeRound(imageView)
//    }

//    fun DeleteText() {
//        textContent.textList.clear()
//        imageContent.checkMainChangeAttribute = true
//        imageToolModule.showView(binding.saveBtn, true)
//        setContainerTextSetting()
//        val view = setContainerSubItem(R.drawable.edit_text_add_icon, clickedFunc = ::AddText, deleteFunc = ::DeleteText)
//        binding.linear.addView(view, binding.linear.size - 2)
//    }

//    fun ShowingAudio(imageView: ImageView) {
//        handler.removeCallbacksAndMessages(null)
////        changeRound(imageView)
//        isAudioPlay = if(isAudioPlay) {
//            imageView.setImageResource(R.drawable.edit_audio_icon)
//            false
//        } else {
//            imageView.setImageResource(R.drawable.edit_audio_click_icon)
//            true
//        }
//    }

//    fun AddAudio(imageView: ImageView) {
//        handler.removeCallbacksAndMessages(null)
////        changeRound(imageView)
//    }

//    fun DeleteAudio() {
//        audioContent.audio = null
//        setContainerTextSetting()
//        imageContent.checkMainChangeAttribute = true
//        imageToolModule.showView(binding.saveBtn, true)
//        val view = setContainerSubItem(R.drawable.edit_audio_add_icon, clickedFunc = ::AddAudio, deleteFunc = ::DeleteAudio)
//        binding.linear.addView(view, binding.linear.size - 1)
//    }

    fun AddImage(imageView: ImageView) {
        handler.removeCallbacksAndMessages(null)
        openGallery()
    }

    fun changeRound(imageView: ImageView) {
        if (imageView.background == null) {
            imageView.setBackgroundResource(R.drawable.chosen_image_border)
            imageView.setPadding(6)
        } else {
            imageView.background = null
            imageView.setPadding(0)
        }
    }

    fun setContainerTextSetting() {
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

    fun setMagicPlay() {
        CoroutineScope(Dispatchers.IO).launch {
            isMagicPlay = false
            val selected = jpegViewModel.selectedSubImage
            if(selected != null)
                magicPictureModule = MagicPictureModule(imageContent, selected)
        }
    }


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

    fun ShowingAudio(imageView: ImageView) {
//        changeRound(imageView)
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
        if(audioContentLayout.visibility == View.GONE){
            binding.currentAudioBarLaydout.visibility = View.VISIBLE
            // 오디오 활성화
        }else{
            binding.currentAudioBarLaydout.visibility = View.GONE
        }
    }

    fun AddAudio(imageView: ImageView) {
//        changeRound(imageView)
        handler.removeCallbacksAndMessages(null)
        Log.d("add_test", " AddAudio() 호출")
        val dialog = ConfirmDialog( "Live 레코드를 추가 하시겠습니까?", GoAudioAddMode())
        // 알림창이 띄워져있는 동안 배경 클릭 막기
        dialog.isCancelable = false
        dialog.show(activity.supportFragmentManager, "ConfirmDialog")
        //isAudioAddMode = true
        // fitAudioModeUI()

    }

    fun DeleteAudio() {
        audioContent.audio = null
        setContainerTextSetting()
        imageContent.checkMainChangeAttribute = true
        imageToolModule.showView(binding.saveBtn, true)
        val view = setContainerSubItem(R.drawable.edit_audio_add_icon, clickedFunc = ::AddAudio, deleteFunc = ::DeleteAudio)
        binding.linear.addView(view, binding.linear.size - 1)
    }

    fun addCurrentAudioBarListener(){
        val currentAudioBarLaydout = binding.currentAudioBarLaydout
        binding.currentPlayBtn.setOnClickListener {
            // 정지 -> 재생
            if(jpegViewModel.isAudioPlay.value!! == 0){
                // 오디오 재생
                if(tempAudioFile != null){
                    jpegViewModel.isAudioPlay.value = 1
                    setCurrentSeekBar()
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
                    setCurrentSeekBar()
                }
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
                        binding.mainImageView.setImageBitmap(ovelapBitmap[currentImageIndex])
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

    private fun showProgressBar(boolean: Boolean, loadingText: LoadingText?) {
//        if(boolean && isInfoViewed) {
//            imageToolModule.showView(binding.infoDialogLayout, false)
//        }
//        else if (isInfoViewed) {
//            imageToolModule.showView(binding.infoDialogLayout, true)
//        }

        CoroutineScope(Dispatchers.Main).launch {
            binding.loadingText.text = when (loadingText) {
//                LoadingText.FaceDetection -> {
//                    "자동 Face Blending 중"
//                }
                LoadingText.Save -> {
                    "편집을 저장 중.."
                }
                LoadingText.BestImageRecommend -> {
                    "Best 사진 추천 중.."
                }
                LoadingText.MagicPlay -> {
                    "매직 사진 준비 중.."
                }
                LoadingText.EditReady -> {
                    "편집 준비 중.."
                }
                else -> {
                    ""
                }
            }
        }

        imageToolModule.showView(binding.progressBar, boolean)
        imageToolModule.showView(binding.loadingText, boolean)
    }


    fun setDistanceSeekBar(){
        while(!imageContent.checkPictureList) {}
        Log.d("seekBar","#####")
        imageToolModule.showView(binding.seekBar, true)

        binding.seekBar.max = pictureList.size - 1

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // SeekBar의 값이 변경될 때 호출되는 메서드입니다.
                // progress 변수는 현재 SeekBar의 값입니다.
                // fromUser 변수는 사용자에 의해 변경된 값인지 여부를 나타냅니다.
                if (fromUser) {
                    val index = progress % pictureList.size
                    mainPicture = pictureList[index]

                    // 글라이드로만 seekbar 사진 변화 하면 좀 끊겨 보이길래

                    if (bitmapList.size > index) {
                        // 만들어 졌으면 비트맵으로 띄웠어
                        CoroutineScope(Dispatchers.Main).launch {
                            binding.mainImageView.setImageBitmap(bitmapList[index])
                        }
                    } else {
                        // 비트맵은 따로 만들고 있고 해당 index의 비트맵이 안만들어졌음명 글라이드로
                        CoroutineScope(Dispatchers.Main).launch {
                            Log.d("error 잡기", "$progress 번째 이미지 띄우기")
                            Glide.with(binding.mainImageView)
                                .load(imageContent.getJpegBytes(pictureList[index]))
                                .into(binding.mainImageView)
                        }
                    }
                    val subLayout = binding.linear.getChildAt(index)
                    CoroutineScope(Dispatchers.Main).launch {
                        changeViewImage(index, subLayout.findViewById(R.id.scrollImageView))
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    fun addAudioModule(){

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
                    setCurrentSeekBar()
                }
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
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

    fun setCurrentSeekBar(){
      //  isPlaying = true
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
                        binding.currentSeekbar.progress = currentPosition
                        delay(100)
                    } else {
                        break
                    }
                }
            }
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

                playinAudioUIStart(mediaPlayer.duration)
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
                        var string : kotlin.String = kotlin.String.format("%02d:%02d", time/60, time)
                        Log.d("AudioModule", time.toString())
                        binding.playingTextView.setText(string)

                        binding.currentPlayTime.setText(string)
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
            binding.currentPlayTime.setText("00:00")
        }
    }

    fun timerUIStart(){
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
//            CoroutineScope(Dispatchers.Main).launch {
//                binding.RecordingTextView.setText("")
//                Toast.makeText(activity, "녹음이 완료 되었습니다.", Toast.LENGTH_SHORT).show();
//            }
        }
    }

    inner class GoAudioAddMode : ConfirmDialogInterface{
        override fun onYesButtonClick(id: Int) {
            Log.d("add_test", " AddAudio() 호출")
            isAudioAddMode = true
            fitAudioModeUI()
        }

    }

    /* Text */
    // text
    var isTextOn : Boolean = false
    var textList : java.util.ArrayList<kotlin.String> = arrayListOf()

    fun addTextModuale(){
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
        }
        // text 입력 UI에 기존의 텍스트 메시지 띄우기
        var textList = jpegViewModel.jpegMCContainer.value!!.textContent.textList
        if(textList != null && textList.size !=0){
            val _text = textList.get(0).data.toString()
            binding.editText.setText(textList.get(0).data)
        }
    }


    fun textMessageSave(){
        var textMessage: kotlin.String = binding.editText.text.toString()
        var textList: java.util.ArrayList<kotlin.String> = arrayListOf()

        textList.add(textMessage)
        if (textMessage != "") {
            jpegViewModel.jpegMCContainer.value!!.setTextConent(
                ContentAttribute.basic,
                textList
            )
            imageContent.checkAddAttribute = true
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
        }
    }
    fun ShowingText(imageView: ImageView) {
        handler.removeCallbacksAndMessages(null)
//        changeRound(imageView)
        isTextView = if(isTextView) {
            imageView.setImageResource(R.drawable.edit_text_icon)
            false
        } else {
            imageView.setImageResource(R.drawable.edit_text_click_icon)
            true
        }
        if(binding.textContentLayout.visibility == View.GONE){
            binding.textContentLayout.visibility = View.VISIBLE
        } else{
            binding.textContentLayout.visibility = View.GONE
        }
    }

    fun AddText(imageView: ImageView) {
        handler.removeCallbacksAndMessages(null)
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

    fun DeleteText() {

        textContent.textList.clear()
        imageContent.checkMainChangeAttribute = true
        imageToolModule.showView(binding.saveBtn, true)
        setContainerTextSetting()
        val view = setContainerSubItem(R.drawable.edit_text_add_icon, clickedFunc = ::AddText, deleteFunc = ::DeleteText)
        binding.linear.addView(view, binding.linear.size - 2)
    }
}

