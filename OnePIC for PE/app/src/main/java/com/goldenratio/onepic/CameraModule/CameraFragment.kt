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
import com.goldenratio.onepic.AllinJPEGModule.Content.ContentAttribute
import com.goldenratio.onepic.AllinJPEGModule.Content.ImageContent
import com.goldenratio.onepic.AudioModule.AudioResolver
import com.goldenratio.onepic.CameraModule.Camera2Module.Camera2Module
import com.goldenratio.onepic.ImageToolModule
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.R
import com.goldenratio.onepic.ViewerModule.ViewerEditorActivity
import com.goldenratio.onepic.databinding.FragmentCameraBinding
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
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

    // 카메라로 촬영된 이미지들
    var previewByteArrayList = MutableLiveData<ArrayList<ByteArray>>(arrayListOf())

    // 이미지가 저장됬는지 확인
    var isSaved = MutableLiveData<Uri>()

    private var PICTURE_SIZE = 1
    private var BURST_SIZE = 3

    // 어떤 모드로 촬영됬는지 (저장할 때 사용)
    private var contentAttribute = ContentAttribute.basic

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as CameraEditorActivity
        audioResolver = AudioResolver(activity)
    }

    @SuppressLint("SuspiciousIndentation")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // 상태바 색상 변경
        val window: Window = activity.window
            ?: throw IllegalStateException("Fragment is not attached to an activity")
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.white))

        binding = FragmentCameraBinding.inflate(inflater, container, false)

        // 촬영된 이미지가 추가됬을 때 호출
        previewByteArrayList.observe(viewLifecycleOwner) {
            incrementProgressBar()
            imageByteArrayUpdate()
        }

        isSaved.observe(viewLifecycleOwner) {
            imageSaved()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 카메라 프레그먼트 세팅
        settingCameraFragment()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onResume() {
        super.onResume()

        // preference에 저장된 설정 값 가져오기
//       getPreferences()

        // radioGroup.setOnCheckedChangeListener - 촬영 모드 선택(라디오 버튼)했을 때 UI 변경
        binding.modeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            settingChangeRadioButton(checkedId)
        }

        // 카메라 시작하기
        camera2Module.startCamera()

        // burst size 버튼 리스너 등록
        binding.burstSizeSettingRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            setBusrtSize(checkedId)
        }

        // preview 이벤트 리스너 등록
        binding.textureView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // 터치된 곳으로 초점 변경
                    camera2Module.setTouchPointDistanceChange(event.x, event.y, 150, 150)
                    return@setOnTouchListener true
                }
                else -> return@setOnTouchListener false
            }
        }

        // shutter Btn 클릭
        binding.shutterBtn.setOnClickListener {
            shutterBtnClicked()
        }
    }

    // A 프래그먼트의 onPause() 메서드에서 호출됩니다.
    override fun onPause() {
        super.onPause()

        // 값 기억하기 (프래그먼트 이동 후 다시 돌아왔을 때도 유지하기 위한 기억)
//        setPreferences()

        // 카메라 멈추기
        camera2Module.closeCamera()
    }

    /**
     * 필요한 변수 설정 및 이벤트 처리를 설정한다.
     */
    private fun settingCameraFragment() {
        // Camera2 모듈 생성
        camera2Module = Camera2Module(activity, requireContext(), binding.textureView, binding.objectDetectionImageView, previewByteArrayList)

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

            // 값 기억하기 (프래그먼트 이동 후 다시 돌아왔을 때도 유지하기 위한 기억)
//            setPreferences()

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

            // 객체 감지 비트맵 초기화
            camera2Module.detectionBitmap?.recycle()
        }
    }

    /**
     * [previewByteArrayList]가 업데이트될 때 호출되는 함수로,
     * 현재 [previewByteArrayList]의 개수가 촬영해야할 개수를 충족했으면, 촬영된 이미지를 한 장으로 저장한다.
     */
    private fun imageByteArrayUpdate() {
        val byteArrays = previewByteArrayList.value!!

        var isObjectPictureClear = false
        // 현재 객체별 다초점 촬영일 경우
        if(binding.objectFocusRadioBtn.isChecked) {
            // 현재 촬영된 사진이 감지된 객체 수보다 많거나 같을 경우 촬영 완료 상태로 설정
            val objectDetectionModule = camera2Module.objectDetectionModule
            if(byteArrays.size >= objectDetectionModule.getDetectionSize()) {
                isObjectPictureClear = true
            }
        }

        //  촬영해야할 개수를 충족 확인
        if(byteArrays.size >= PICTURE_SIZE || isObjectPictureClear) {
            imageToolModule.showView(binding.distanceWarningConstraintLayout, false)
            imageToolModule.showView(binding.objectWarningConstraintLayout, false)
            mediaPlayer.start()
            rotation.cancel()

            // 저장 중 화면
            imageToolModule.showView(binding.loadingLayout, true)

            // 한 장일 경우 저장
            if (binding.basicRadioBtn.isChecked) saveJPEG()
            // 여러 장일 경우 저장
            else saveAllinJPEG()
        }
    }

    /**
     * 이미지 저장이 완료됬을 때 호출되는 함수로, 촬영으로 인해 막아놨던 버튼들을 활성화 및 화면 설정을 한다.
     */
    private fun imageSaved() {
        // previewByteArrayList 초기화
        previewByteArrayList.value?.clear()

        val uri = isSaved.value

        if (uri != null) {
            CoroutineScope(Dispatchers.Main).launch {

                binding.shutterBtn.isEnabled = true
                binding.galleryBtn.isEnabled = true
                binding.convertBtn.isEnabled = true
                binding.basicRadioBtn.isEnabled = true
                binding.burstRadioBtn.isEnabled = true
                binding.objectFocusRadioBtn.isEnabled = true
                binding.distanceFocusRadioBtn.isEnabled = true
                binding.burstSizeSettingRadioGroup.isEnabled = true

                imageToolModule.showView(binding.loadingLayout, false)

                binding.successInfoTextView.text = getText(R.string.camera_success_info)
                imageToolModule.showView(binding.successInfoConstraintLayout, true)

                imageToolModule.fadeIn.start()
            }
        }
    }

    /**
     * 앱을 나갔다와도 변수 값을 기억하는 SharedPreference를 얻어와 촬영 상태를 설정한다.
     *
     * lensFacing : 카메라 렌즈 전면 / 후면
     * selectedRadioIndex : 선택된 카메라 촬영 모드
     * BURST_SIZE : 연속 촬영 장 수
     */
    private fun getPreferences() {
        // 앱을 나갔다와도 변수 값 기억하게 하는 SharedPreference
        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE)

        val newLensFacing = sharedPref?.getInt("lensFacing", CameraCharacteristics.LENS_FACING_BACK)
        if(newLensFacing != null) {
            camera2Module.wantCameraDirection = newLensFacing
        }
        selectedRadioIndex = sharedPref?.getInt("selectedRadioIndex", binding.basicRadioBtn.id)
        BURST_SIZE = sharedPref?.getInt("selectedBurstSize", BURST_SIZE)!!

        // 앱을 나갔다 들어와도 촬영 모드 기억하기 - 카메라 모드에 따른 UI 적용
        if (selectedRadioIndex != null && selectedRadioIndex!! >= 0) {
            settingChangeRadioButton(selectedRadioIndex!!)
        }

        // burst size 기억하기
        if (BURST_SIZE >= 0 && selectedRadioIndex == binding.burstRadioBtn.id) {
            updateBurstSize()
        }

        // radioGroup.setOnCheckedChangeListener - 촬영 모드 선택(라디오 버튼)했을 때 UI 변경
        binding.modeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            settingChangeRadioButton(checkedId)
        }
    }

    /**
     * SharedPreference으로 촬영 상태를 기록한다.
     * SharedPreference 설정으로 앱을 나갔다왔을 때 동일한 촬영 상태가 되도록한다.
     */
    private fun setPreferences() {
        // 값 기억하기 (프래그먼트 이동 후 다시 돌아왔을 때도 유지하기 위한 기억)
        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE)
        with(sharedPref?.edit()) {
            this?.putInt("selectedRadioIndex", selectedRadioIndex!!)
            this?.putInt("lensFacing", camera2Module.wantCameraDirection)
            this?.putInt("selectedBurstSize", BURST_SIZE)
            this?.apply()
        }
    }

    /**
     * 카메라 모드 버튼의 값 변경 시 호출되는 함수로,
     * 변경된 값에 따라 화면 UI, 카메라 및 변수를 재설정한다.
     *
     * @param checkedId 현재 변경된 카메라 모드 버튼 ID
     */
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
                camera2Module.detectionBitmap?.recycle()

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

        // 값 기억하기 (프래그먼트 이동 후 다시 돌아왔을 때도 유지하기 위한 기억)
//        setPreferences()

    }

    /**
     * 촬영 버튼을 클릭하면 호출되는 함수로, 현재 모드에 맞는 촬영을 실행한다.
     *
     * 일반 촬영 : 한 장의 사진 촬영
     * 연속 촬영 : 3, 5, 7장의 연속 사진 촬영
     * 객체별 다초점 촬영 : 카메라 프리뷰에 감지된 객체별로 초점이 맞게 사진 촬영
     * 거리별 다초점 촬영 : 0부터 N까지 10개의 초점 거리에 맞는 10장의 연속 사진 촬영
     *
     */
    private fun shutterBtnClicked() {
        System.gc()
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
         * 일반 모드
         */
        if (binding.basicRadioBtn.isChecked) {
            PICTURE_SIZE = 1
            contentAttribute = ContentAttribute.basic
            camera2Module.lockFocus(PICTURE_SIZE)
        }

        /**
         * 연속 모드
         */
        if (binding.burstRadioBtn.isChecked) {
            audioResolver.startRecording("camera_record")

            contentAttribute = ContentAttribute.burst
            PICTURE_SIZE = BURST_SIZE
            camera2Module.lockFocus(BURST_SIZE)
        }

        /**
         * 객체별 다초점 모드
         */
        if (binding.objectFocusRadioBtn.isChecked) {
            Log.d("detectionResult", "1. shutter click")
            audioResolver.startRecording("camera_record")

            PICTURE_SIZE = camera2Module.objectDetectionModule.getDetectionSize()
            if (PICTURE_SIZE > 0) {
                contentAttribute = ContentAttribute.object_focus
                imageToolModule.showView(binding.objectWarningConstraintLayout, true)
                camera2Module.focusObjectDetectionPictures()
            } else {
                camera2Module.objectDetectionModule.resetDetectionResult()
                CoroutineScope(Dispatchers.Main).launch {
                    binding.shutterBtn.isEnabled = true
                    binding.galleryBtn.isEnabled = true
                    binding.convertBtn.isEnabled = true
                    binding.basicRadioBtn.isEnabled = true
                    binding.burstRadioBtn.isEnabled = true
                    binding.objectFocusRadioBtn.isEnabled = true
                    binding.distanceFocusRadioBtn.isEnabled = true

                    binding.successInfoTextView.text =
                        getText(R.string.camera_object_detection_failed)
                    binding.successInfoConstraintLayout.visibility = View.VISIBLE

                    imageToolModule.fadeIn.start()
                    rotation.cancel()
                }
            }
        }

        /**
         * 거리별 다초점 촬영 모드
         */
        if (binding.distanceFocusRadioBtn.isChecked) {
            audioResolver.startRecording("camera_record")

            initProgressBar()

            PICTURE_SIZE = 10
            contentAttribute = ContentAttribute.distance_focus
            camera2Module.distanceFocusPictures(PICTURE_SIZE)
        }
    }

    /**
     * All-in JPEG으로 저장
     */
    private fun saveAllinJPEG() {
        var isSafeSaved = true
        CoroutineScope(Dispatchers.Default).launch {
            withContext(Dispatchers.Main) {
                // 녹음 중단
                val savedFile = audioResolver.stopRecording()
                if (savedFile != null) {
                    val audioBytes = audioResolver.getByteArrayInFile(savedFile)
                    jpegViewModel.jpegAiContainer.value!!.setAudioContent(audioBytes, contentAttribute)
                    Log.d("AudioModule", "녹음된 오디오 사이즈 : ${audioBytes.size}")
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
                        if(pictureList.size != detectionResult.size) {
                            isSafeSaved = false
                            break
                        }
                        var boundingBox = detectionResult[i].boundingBox

                        // 전면 카메라일 경우 객체 위치 좌우반전
                        if(camera2Module.wantCameraDirection == 0) {

                            val left = objectDetectionModule.bitmapWidth - boundingBox.right
                            val right = objectDetectionModule.bitmapWidth - boundingBox.left

                            boundingBox = RectF(left, boundingBox.top, right, boundingBox.bottom)

                            Log.d("전면" , "객체 좌우반전")
                        }
                        pictureList[i].insertEmbeddedData(
                            arrayListOf(
                                objectDetectionModule.bitmapWidth,
                                boundingBox.left.toInt(), boundingBox.top.toInt(),
                                boundingBox.right.toInt(), boundingBox.bottom.toInt()
                            )
                        )
                    }
//                    imageToolModule.showView(binding.objectWarningConstraintLayout, false)
                    objectDetectionModule.resetDetectionResult()
                }

                if(isSafeSaved) {
                    JpegViewModel.AllInJPEG = true
                    // All-in JPEG 저장
                    jpegViewModel.jpegAiContainer.value?.saveAfterCapture(isSaved)
                }
            }
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

            imageToolModule.showView(binding.loadingLayout, false)

            binding.successInfoTextView.text = getText(R.string.camera_success_info)
            binding.successInfoConstraintLayout.visibility = View.VISIBLE

            imageToolModule.fadeIn.start()
//            rotation.cancel()
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

    // 텍스트뷰 텍스트 설정
    fun setText(textView: TextView, string: String) {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.Main) {
                textView.text = string
            }
        }
    }

    // 연속 촬영 개수에 따른 변수 및 텍스트 설정
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

        // 값 기억하기 (프래그먼트 이동 후 다시 돌아왔을 때도 유지하기 위한 기억)
//        setPreferences()
    }

    // 연속 촬영 개수 설정
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

    private fun initProgressBar() {
        binding.distanceProgressBar.progress = 0
        imageToolModule.showView(binding.distanceWarningConstraintLayout, true)
    }

    private fun incrementProgressBar() {
        var currentProgress = binding.distanceProgressBar.progress
        currentProgress += 7
        binding.distanceProgressBar.progress = currentProgress
    }

    companion object {
        private val BURST_OPTION1 = 3
        private val BURST_OPTION2 = 5
        private val BURST_OPTION3 = 7
    }
}