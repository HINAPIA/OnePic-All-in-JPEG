package com.goldenratio.onepic.CameraModule

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.hardware.camera2.*
import android.media.MediaPlayer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.*
import androidx.camera.core.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.Glide
import com.goldenratio.onepic.AudioModule.AudioResolver
import com.goldenratio.onepic.CameraModule.Camera2Module.Camera2Module
import com.goldenratio.onepic.ImageToolModule
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.AllinJPEGModule.Contents.ContentAttribute
import com.goldenratio.onepic.AllinJPEGModule.Contents.ContentType
import com.goldenratio.onepic.AllinJPEGModule.ImageContent
import com.goldenratio.onepic.R
import com.goldenratio.onepic.ViewerModule.ViewerEditorActivity
import com.goldenratio.onepic.databinding.FragmentCameraBinding
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*


@OptIn(ExperimentalCamera2Interop::class)
class CameraFragment : Fragment() {

    private val jpegViewModel by activityViewModels<JpegViewModel>()

    private lateinit var camera2Module: Camera2Module

    private lateinit var activity: CameraEditorActivity
    private lateinit var binding: FragmentCameraBinding
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var rotation: ObjectAnimator
    private var selectedRadioIndex: Int? = 0

    // audio
    private lateinit var audioResolver: AudioResolver

    // imageContent
    private lateinit var imageContent: ImageContent
    private lateinit var imageToolModule: ImageToolModule

    var previewByteArrayList = MutableLiveData<ArrayList<ByteArray>>(arrayListOf())

    var isSaved = MutableLiveData<Uri>()
    var saveByteArray = MutableLiveData<ByteArray>()

    private var PICTURE_SIZE = 1
    private var BURST_SIZE = 3

    private var contentAttribute = ContentAttribute.basic

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as CameraEditorActivity
        audioResolver = AudioResolver(activity)
    }

    @SuppressLint("SuspiciousIndentation")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 상태바 색상 변경
        val window: Window = activity.window
            ?: throw IllegalStateException("Fragment is not attached to an activity")
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.white))

        binding = FragmentCameraBinding.inflate(inflater, container, false)

        previewByteArrayList.observe(viewLifecycleOwner) {
            var isObjectPictureClear = false
            if(binding.objectFocusRadioBtn.isChecked) {
                val objectDetectionModule = camera2Module.objectDetectionModule

                if(it.size >= objectDetectionModule.getDetectionSize()) {
                    isObjectPictureClear = true
                }
            }
            if(it.size >= PICTURE_SIZE || isObjectPictureClear) {
                mediaPlayer.start()
//                Toast.makeText(requireContext(), "촬영 완료", Toast.LENGTH_SHORT).show()

                // 한 장일 경우 저장
                if(binding.basicRadioBtn.isChecked) {
                    saveJPEG()
                }
                // 여러 장일 경우 저장
                else {
                    saveAllinJPEG()
                }
            }
        }

//        saveByteArray.observe(viewLifecycleOwner) {
//            System.gc()
//            if(it != null) {
//                CoroutineScope(Dispatchers.Main).launch {
////                    imageToolModule.showView(binding.objectDetectionImageView, true)
//                    Glide.with(binding.testView2)
//                        .load(it)
//                        .into(binding.testView2)
//                }
//            }
//        }

        isSaved.observe(viewLifecycleOwner) {
            if (it != null) {
                CoroutineScope(Dispatchers.Main).launch {
//                    binding.testView.setImageURI(it)

                    binding.shutterBtn.isEnabled = true
                    binding.galleryBtn.isEnabled = true
                    binding.convertBtn.isEnabled = true
                    binding.basicRadioBtn.isEnabled = true
                    binding.burstRadioBtn.isEnabled = true
                    binding.objectFocusRadioBtn.isEnabled = true
                    binding.distanceFocusRadioBtn.isEnabled = true
                    binding.successInfoTextView.text = getText(R.string.camera_success_info)
                    imageToolModule.showView(binding.successInfoConstraintLayout, true)

                    imageToolModule.fadeIn.start()
                    rotation.cancel()
                }
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Camera2 모듈 생성
        camera2Module = Camera2Module(
            activity,
            requireContext(),
            binding.textureView,
            binding.objectDetectionImageView,
            previewByteArrayList
        )

        // imageContent 설정
        imageContent = jpegViewModel.jpegAiContainer.value!!.imageContent

        // 촬영 완료음 설정
        mediaPlayer = MediaPlayer.create(context, R.raw.end_sound)

        imageToolModule = ImageToolModule()

        // warning Gif (Object Focus 촬영 중 gif)
        imageToolModule.settingLoopGif(binding.warningLoadingImageView, R.raw.flower_loading)

        // 서서히 나타나기/없어지기 애니메이션 설정
        imageToolModule.settingAnimation(binding.successInfoConstraintLayout)

        // shutter Btn 애니메이션 설정
        binding.shutterBtn.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.shutterBtn.viewTreeObserver.removeOnGlobalLayoutListener(this)

                // 뷰를 회전시키는 애니메이션을 생성합니다.
                rotation = ObjectAnimator.ofFloat(binding.shutterBtn, View.ROTATION, 0f, 360f)
                rotation.apply {
                    duration = 1000 // 애니메이션 시간 (밀리초)
                    interpolator = AccelerateDecelerateInterpolator() // 가속도 감속도 애니메이션 인터폴레이터
                    repeatCount = ObjectAnimator.INFINITE // 애니메이션 반복 횟수 (INFINITE: 무한반복)
                    repeatMode = ObjectAnimator.RESTART // 애니메이션 반복 모드 (RESTART: 처음부터 다시 시작)

                }
            }
        })

        // 카메라 전환 (전면<>후면)
        binding.convertBtn.setOnClickListener {

            if (camera2Module.wantCameraDirection == CameraCharacteristics.LENS_FACING_BACK) {
                camera2Module.wantCameraDirection = CameraCharacteristics.LENS_FACING_FRONT // 전면
            } else {
                camera2Module.wantCameraDirection = CameraCharacteristics.LENS_FACING_BACK // 후면
            }

            camera2Module.closeCamera()
            camera2Module.startCamera()
        }

        // 갤러리 버튼
        binding.galleryBtn.setOnClickListener {
            val intent =
                Intent(
                    activity,
                    ViewerEditorActivity::class.java
                ) //fragment라서 activity intent와는 다른 방식

            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            activity.supportFragmentManager.beginTransaction().addToBackStack(null).commit()

            startActivity(intent)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onResume() {
        super.onResume()

        // 앱을 나갔다와도 변수 값 기억하게 하는 SharedPreference
        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE)
        /**
         * lensFacing : 카메라 렌즈 전면 / 후면
         * selectedRadioIndex : 선택된 카메라 촬영 모드
         * BURST_SIZE : 연속 촬영 장 수
         */
        val newLensFacing = sharedPref?.getInt("lensFacing", CameraCharacteristics.LENS_FACING_BACK)
        if(newLensFacing != null) {
            camera2Module.wantCameraDirection = newLensFacing
        }
        selectedRadioIndex = sharedPref?.getInt("selectedRadioIndex", binding.basicRadioBtn.id)
        BURST_SIZE = sharedPref?.getInt("selectedBurstSize", BURST_SIZE)!!

        // 카메라 시작하기
        camera2Module.startCamera()

        /**
         * 앱을 나갔다 들어와도 촬영 모드 기억하기
         *      - 카메라 모드에 따른 UI 적용
         */
        if (selectedRadioIndex != null && selectedRadioIndex!! >= 0) {
            settingChangeRadioButton(selectedRadioIndex!!)
        }

        // burst size 기억하기
        if (BURST_SIZE >= 0 && selectedRadioIndex == binding.burstRadioBtn.id) {
            updateBurstSize()
        }

        /**
         * radioGroup.setOnCheckedChangeListener
         *      - 촬영 모드 선택(라디오 버튼)했을 때 UI 변경
         */
        binding.modeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            settingChangeRadioButton(checkedId)
        }

        binding.burstSizeSettingRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            setBusrtSize(checkedId)
        }

        // preview 이벤트 리스너 등록
        binding.textureView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    camera2Module.setTouchPointDistanceChange(event.x, event.y, 150, 150)
                    return@setOnTouchListener true
                }
                else -> return@setOnTouchListener false
            }
        }

        // shutter Btn 클릭
        binding.shutterBtn.setOnClickListener {

            rotation.start()
            binding.shutterBtn.isEnabled = false
            binding.galleryBtn.isEnabled = false
            binding.convertBtn.isEnabled = false
            binding.basicRadioBtn.isEnabled = false
            binding.burstRadioBtn.isEnabled = false
            binding.objectFocusRadioBtn.isEnabled = false
            binding.distanceFocusRadioBtn.isEnabled = false

            // previewByteArrayList 초기화
            previewByteArrayList.value?.clear()

            /**
             * Basic 모드
             */
            if(binding.basicRadioBtn.isChecked) {
                PICTURE_SIZE = 1
                contentAttribute = ContentAttribute.basic
                camera2Module.lockFocus(PICTURE_SIZE)
            }

            /**
             * Burst 모드
             */
            if (binding.burstRadioBtn.isChecked) {
                audioResolver.startRecording("camera_record")

                contentAttribute = ContentAttribute.burst
                PICTURE_SIZE = BURST_SIZE
                camera2Module.lockFocus(BURST_SIZE)
            }

            /**
             * ObjectFocus 모드 (아무것도 잡힌게 없을 때 처리 해줘야 함)
             */
            if(binding.objectFocusRadioBtn.isChecked) {
                Log.d("detectionResult", "1. shutter click" )
                audioResolver.startRecording("camera_record")

                PICTURE_SIZE = camera2Module.objectDetectionModule.getDetectionSize()
                if(PICTURE_SIZE > 0) {
                    contentAttribute = ContentAttribute.object_focus
                    imageToolModule.showView(binding.objectWarningConstraintLayout, true)
                    camera2Module.focusDetectionPictures()
                }
                else {
                    camera2Module.objectDetectionModule.resetDetectionResult()
                    CoroutineScope(Dispatchers.Main).launch {
                        binding.shutterBtn.isEnabled = true
                        binding.galleryBtn.isEnabled = true
                        binding.convertBtn.isEnabled = true
                        binding.basicRadioBtn.isEnabled = true
                        binding.burstRadioBtn.isEnabled = true
                        binding.objectFocusRadioBtn.isEnabled = true
                        binding.distanceFocusRadioBtn.isEnabled = true

                        binding.successInfoTextView.text = getText(R.string.camera_object_detection_failed)
                        binding.successInfoConstraintLayout.visibility = View.VISIBLE

                        imageToolModule.fadeIn.start()
                        rotation.cancel()
                    }
                }
            }

            /**
             * DistanceFocus 모드
             */
            if(binding.distanceFocusRadioBtn.isChecked) {
                audioResolver.startRecording("camera_record")

                PICTURE_SIZE = 10
                contentAttribute = ContentAttribute.distance_focus
                camera2Module.distanceFocusPictures(PICTURE_SIZE)
            }
        }
    }

    // A 프래그먼트의 onPause() 메서드에서 호출됩니다.
    override fun onPause() {
        super.onPause()

        // 값 기억하기 (프래그먼트 이동 후 다시 돌아왔을 때도 유지하기 위한 기억)
        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE)
        with(sharedPref?.edit()) {
            this?.putInt("selectedRadioIndex", selectedRadioIndex!!)
            this?.putInt("lensFacing", camera2Module.wantCameraDirection)
            this?.putInt("selectedBurstSize", BURST_SIZE)
            this?.apply()
        }

        // 카메라 멈추기
        camera2Module.closeCamera()
    }

    private fun settingChangeRadioButton(checkedId: Int) {

        when (checkedId) {
            /**
             * basic 모드
             */
            binding.basicRadioBtn.id -> {
                camera2Module.isDetectionChecked = false
                binding.objectDetectionImageView.setImageBitmap(null)

                selectedRadioIndex = binding.basicRadioBtn.id

                imageToolModule.showView(binding.infoConstraintLayout, false)
                imageToolModule.showView(binding.burstSizeConstraintLayout, false)

                binding.basicRadioBtn.isChecked = true

                setBold(binding.basicRadioBtn, true)
                setBold(binding.burstRadioBtn, false)
                setBold(binding.objectFocusRadioBtn, false)
                setBold(binding.distanceFocusRadioBtn, false)
            }
            /**
             * burst 모드
             */
            binding.burstRadioBtn.id -> {
                camera2Module.isDetectionChecked = false
                binding.objectDetectionImageView.setImageBitmap(null)

                selectedRadioIndex = binding.burstRadioBtn.id

                imageToolModule.showView(binding.infoConstraintLayout, true)
                imageToolModule.showView(binding.burstSizeConstraintLayout, true)

                binding.burstRadioBtn.isChecked = true

                setBold(binding.basicRadioBtn, false)
                setBold(binding.burstRadioBtn, true)
                setBold(binding.objectFocusRadioBtn, false)
                setBold(binding.distanceFocusRadioBtn, false)

                updateBurstSize()
            }
            /**
             * object 모드
             */
            binding.objectFocusRadioBtn.id -> {
                selectedRadioIndex = binding.objectFocusRadioBtn.id

                imageToolModule.showView(binding.infoConstraintLayout, true)
                imageToolModule.showView(binding.burstSizeConstraintLayout, false)

                binding.objectFocusRadioBtn.isChecked = true

                setBold(binding.basicRadioBtn, false)
                setBold(binding.burstRadioBtn, false)
                setBold(binding.objectFocusRadioBtn, true)
                setBold(binding.distanceFocusRadioBtn, false)

                setText(binding.infoTextView, resources.getString(R.string.camera_object_info))

                camera2Module.isDetectionChecked = true
            }
            /**
             * distance 모드
             */
            binding.distanceFocusRadioBtn.id -> {
                camera2Module.isDetectionChecked = false
                binding.objectDetectionImageView.setImageBitmap(null)

                selectedRadioIndex = binding.distanceFocusRadioBtn.id

                imageToolModule.showView(binding.infoConstraintLayout, true)
                imageToolModule.showView(binding.burstSizeConstraintLayout, false)

                binding.distanceFocusRadioBtn.isChecked = true

                setBold(binding.basicRadioBtn, false)
                setBold(binding.burstRadioBtn, false)
                setBold(binding.objectFocusRadioBtn, false)
                setBold(binding.distanceFocusRadioBtn, true)

                setText(
                    binding.infoTextView,
                    resources.getString(R.string.camera_distance_info)
                )
            }
        }
    }

    /**
     * All-in JPEG으로 저장
     */
    private fun saveAllinJPEG() {
        CoroutineScope(Dispatchers.Default).launch {
            withContext(Dispatchers.Main) {
                // 녹음 중단
                val savedFile = audioResolver.stopRecording()
                if (savedFile != null) {
                    val audioBytes = audioResolver.getByteArrayInFile(savedFile)
                    jpegViewModel.jpegAiContainer.value!!.setAudioContent(
                        audioBytes,
                        contentAttribute
                    )
                    Log.d("AudioModule", "녹음된 오디오 사이즈 : ${audioBytes.size.toString()}")
                }

                //  renew ImageContent
                val jop = async {
                    jpegViewModel.jpegAiContainer.value!!.setImageContent(
                        previewByteArrayList.value!!,
                        contentAttribute
                    )
                }
                jop.await()
                Log.d("error 잡기", "넘어가기 전")

                // objectFocus일 경우, 관련 데이터 EmbeddedData에 추가
                if (binding.objectFocusRadioBtn.isChecked) {
                    val objectDetectionModule = camera2Module.objectDetectionModule

                    val detectionResult = objectDetectionModule.getDetectionResults()
                    val pictureList = jpegViewModel.jpegAiContainer.value!!.imageContent.pictureList

                    for (i in 0 until pictureList.size) {
                        val boundingBox = detectionResult[i].boundingBox
                        pictureList[i].insertEmbeddedData(
                            arrayListOf(
                                objectDetectionModule.bitmapWidth,
                                boundingBox.left.toInt(), boundingBox.top.toInt(),
                                boundingBox.right.toInt(), boundingBox.bottom.toInt()
                            )
                        )
                    }
                    imageToolModule.showView(binding.objectWarningConstraintLayout, false)
                    objectDetectionModule.resetDetectionResult()
                }

                JpegViewModel.AllInJPEG = true
                // All-in JPEG 저장
                jpegViewModel.jpegAiContainer.value?.save(isSaved)
            }

//                binding.shutterBtn.isEnabled = true
//                binding.galleryBtn.isEnabled = true
//                binding.convertBtn.isEnabled = true
//                binding.basicRadioBtn.isEnabled = true
//                binding.burstRadioBtn.isEnabled = true
//                binding.objectFocusRadioBtn.isEnabled = true
//                binding.distanceFocusRadioBtn.isEnabled = true
//                binding.successInfoTextView.text = getText(R.string.camera_success_info)
//                binding.successInfoConstraintLayout.visibility = View.VISIBLE
//
//                imageToolModule.fadeIn.start()
//                rotation.cancel()
        }
    }

    /**
     * 일반 JPEG으로 저장
     */
    private fun saveJPEG() {
        val fileName = System.currentTimeMillis().toString() + ".jpg"
        val externalStorage = Environment.getExternalStorageDirectory().absolutePath
        val path = "$externalStorage/DCIM/imageSave"
        val dir = File(path)
        if (dir.exists().not()) {
            dir.mkdirs() // 폴더 없을경우 폴더 생성
        }
        try {
            val fos = FileOutputStream("$dir/$fileName")
            fos.write(previewByteArrayList.value!![0]) // ByteArray의 이미지 데이터를 파일에 쓰기
            fos.close()

            // 미디어 스캐닝을 통해 갤러리에 이미지를 등록
            MediaScannerConnection.scanFile(context, arrayOf("$dir/$fileName"), null, null)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        CoroutineScope(Dispatchers.Main).launch {
            binding.shutterBtn.isEnabled = true
            binding.galleryBtn.isEnabled = true
            binding.convertBtn.isEnabled = true
            binding.basicRadioBtn.isEnabled = true
            binding.burstRadioBtn.isEnabled = true
            binding.objectFocusRadioBtn.isEnabled = true
            binding.distanceFocusRadioBtn.isEnabled = true
            binding.successInfoTextView.text = getText(R.string.camera_success_info)
            binding.successInfoConstraintLayout.visibility = View.VISIBLE

            imageToolModule.fadeIn.start()
            rotation.cancel()
        }
    }

    // 라디오 버튼 볼드 처리
    fun setBold(radioBtn: RadioButton, isBold: Boolean) {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.Main) {
                if (isBold) radioBtn.setTypeface(null, Typeface.BOLD)
                else radioBtn.setTypeface(null, Typeface.NORMAL)
            }
        }
    }

    fun setText(textView: TextView, string: String) {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.Main) {
                textView.text = string
            }
        }
    }

    fun setBusrtSize(checkedId: Int) {
        when (checkedId) {
            binding.burst1RadioBtn.id -> {
                BURST_SIZE = BURST_OPTION1
                setText(binding.infoTextView, resources.getString(R.string.burst1_info))
            }
            binding.burst2RadioBtn.id -> {
                BURST_SIZE = BURST_OPTION2
                setText(binding.infoTextView, resources.getString(R.string.burst2_info))
            }
            binding.burst3RadioBtn.id -> {
                BURST_SIZE = BURST_OPTION3
                setText(binding.infoTextView, resources.getString(R.string.burst3_info))
            }
        }
    }

    fun updateBurstSize() {
        when (BURST_SIZE) {
            BURST_OPTION1 -> {
                binding.burst1RadioBtn.isChecked = true
                setText(binding.infoTextView, resources.getString(R.string.burst1_info))
            }
            BURST_OPTION2 -> {
                binding.burst2RadioBtn.isChecked = true
                setText(binding.infoTextView, resources.getString(R.string.burst2_info))
            }
            BURST_OPTION3 -> {
                binding.burst3RadioBtn.isChecked = true
                setText(binding.infoTextView, resources.getString(R.string.burst3_info))
            }
        }
    }

    fun getStatusBarHeightDP(context: Context): Int {
        var result = 0
        val resourceId: Int =
            context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimension(resourceId).toInt()
        }
        return result
    }

    companion object {
        private val BURST_OPTION1 = 3
        private val BURST_OPTION2 = 5
        private val BURST_OPTION3 = 7
    }
}