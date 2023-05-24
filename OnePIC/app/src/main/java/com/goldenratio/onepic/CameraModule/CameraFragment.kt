package com.goldenratio.onepic.CameraModule

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.hardware.camera2.*
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.widget.*
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.*
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.goldenratio.onepic.AudioModule.AudioResolver
import com.goldenratio.onepic.EditModule.RewindModule
import com.goldenratio.onepic.ImageToolModule
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.PictureModule.Contents.ActivityType
import com.goldenratio.onepic.PictureModule.Contents.ContentAttribute
import com.goldenratio.onepic.PictureModule.Contents.ContentType
import com.goldenratio.onepic.PictureModule.ImageContent
import com.goldenratio.onepic.R
import com.goldenratio.onepic.ViewerModule.ViewerEditorActivity
import com.goldenratio.onepic.databinding.FragmentCameraBinding
import kotlinx.coroutines.*
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.lang.reflect.InvocationTargetException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


@OptIn(ExperimentalCamera2Interop::class)
class CameraFragment : Fragment(), ObjectDetectorHelper.DetectorListener {

    // binding 변수
    private lateinit var viewFinder : PreviewView
    private lateinit var shutterBtn : ImageView
    private lateinit var basicRadioBtn : RadioButton
    private lateinit var objectFocusRadioBtn : RadioButton
    private lateinit var distanceFocusRadioBtn : RadioButton
    private lateinit var autoRewindRadioBtn : RadioButton
    private lateinit var basicToggleBtn : ToggleButton
    private lateinit var overlay : OverlayView
    private lateinit var seekBarLinearLayout : LinearLayout
    private lateinit var  distanceOptionLinearLayout : LinearLayout
    private lateinit var bottomMenu : ConstraintLayout
    private lateinit var galleryBtn : ImageView
    private lateinit var convertBtn : ImageView
    private lateinit var modeRadioGroup : RadioGroup

    data class pointData(var x: Float, var y: Float)
    data class DetectionResult(val boundingBox: RectF, val text: String)

    private var pointArrayList: ArrayList<pointData> = arrayListOf() // Object Focus
    private var previewByteArrayList: ArrayList<ByteArray> = arrayListOf()

    private lateinit var activity: CameraEditorActivity
    private lateinit var binding : FragmentCameraBinding
    private var isToggleChecked : Boolean? = false
    private lateinit var mediaPlayer : MediaPlayer
    private lateinit var rotation: ObjectAnimator
    private var selectedRadioIndex : Int? = 0

    // Camera
    private lateinit var camera: Camera
    private lateinit var cameraController: CameraControl
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var camera2CameraInfo: Camera2CameraInfo
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraSelector: CameraSelector
    private lateinit var imageCapture: ImageCapture

    // TFLite
    private lateinit var customObjectDetector: ObjectDetector
    private lateinit var detectedList: List<DetectionResult>

    // Distance Focus
    private var lensDistanceSteps: Float = 0F
    private var minFocusDistance: Float = 0F
    private var focusDistance : Float = 0F

    // Object Focus
    private lateinit var factory: MeteringPointFactory
    private var isFocusSuccess: Boolean? = null

    private val jpegViewModel by activityViewModels<JpegViewModel>()

    // audio
    private lateinit var audioResolver : AudioResolver

    // imageContent
    private lateinit var imageContent : ImageContent
    private lateinit var imageToolModule: ImageToolModule
    private lateinit var rewindModule: RewindModule

    private val burstSize = 5

    private var imageAnalyzer: ImageAnalysis? = null
    private lateinit var bitmapBuffer: Bitmap


    private var analyzeImageWidth : Int = 0

    // Lens Flag
    private var isBackLens : Boolean? = true


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
        val window: Window = activity?.window
            ?: throw IllegalStateException("Fragment is not attached to an activity")
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.setStatusBarColor(ContextCompat.getColor(requireContext(), android.R.color.white))

        binding = FragmentCameraBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Binding 초기화
        viewFinder = binding.viewFinder
        shutterBtn = binding.shutterBtn
        basicRadioBtn = binding.basicRadioBtn
        objectFocusRadioBtn = binding.objectFocusRadioBtn
        distanceFocusRadioBtn = binding.distanceFocusRadioBtn
        autoRewindRadioBtn = binding.autoRewindRadioBtn
        basicToggleBtn = binding.basicToggleBtn
        overlay = binding.overlay
        seekBarLinearLayout = binding.seekBarLinearLayout
        distanceOptionLinearLayout = binding.distanceOptionLinearLayout
        bottomMenu = binding.bottomMenu
        galleryBtn = binding.galleryBtn
        convertBtn = binding.convertBtn
        modeRadioGroup = binding.modeRadioGroup

        factory = viewFinder.meteringPointFactory
        mediaPlayer = MediaPlayer.create(context, R.raw.end_sound)

        // imageContent 설정
        imageContent = jpegViewModel.jpegMCContainer.value!!.imageContent
        imageContent.activityType = ActivityType.Camera

        //rewind
        imageToolModule = ImageToolModule()
        rewindModule = RewindModule()

        // Initialize the detector object
        setDetecter()



        // distance 수동, 자동 Btn
        binding.distanceOptionRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            // TODO : Manual과 Auto에 따라 seekbar 유무 + tv 색상 변경
            when(checkedId){
                // 수동
                R.id.distanceManualRadioBtn -> {
                    binding.seekBarLinearLayout.visibility = View.VISIBLE
                    binding.distanceManualTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    binding.distanceAutoTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.middle_gray_80))
                }
                // 자동
                R.id.distanceAutoRadioBtn -> {
                    binding.seekBarLinearLayout.visibility = View.GONE
                    binding.distanceManualTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.middle_gray_80))
                    binding.distanceAutoTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                }
            }
        }

        // distance 수동 Seek Bar 조절
        binding.distanceFocusSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                focusDistance = (minFocusDistance/binding.distanceFocusSeekBar.max.toFloat())*progress.toFloat()
                Log.v("chaewon", "focusDistance : $focusDistance")
                if(fromUser)
                    turnOffAFMode(focusDistance)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })


        // shutter Btn 애니메이션 설정
        shutterBtn.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                shutterBtn.viewTreeObserver.removeOnGlobalLayoutListener(this)

                // 뷰를 회전시키는 애니메이션을 생성합니다.
                rotation = ObjectAnimator.ofFloat(shutterBtn, View.ROTATION, 0f, 360f)
                rotation.apply {
                    duration = 1000 // 애니메이션 시간 (밀리초)
                    interpolator = AccelerateDecelerateInterpolator() // 가속도 감속도 애니메이션 인터폴레이터
                    repeatCount = ObjectAnimator.INFINITE // 애니메이션 반복 횟수 (INFINITE: 무한반복)
                    repeatMode = ObjectAnimator.RESTART // 애니메이션 반복 모드 (RESTART: 처음부터 다시 시작)

                }
            }
        })

        convertBtn.setOnClickListener {

            isBackLens = !isBackLens!!
            startCamera(isBackLens)

        }

        galleryBtn.setOnClickListener{

            val intent =
                Intent(activity, ViewerEditorActivity::class.java) //fragment라서 activity intent와는 다른 방식

            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            activity?.supportFragmentManager?.beginTransaction()?.addToBackStack(null)?.commit()

            startActivity(intent)
        }

        // 터치로 초점 맞추기
        viewFinder.setOnTouchListener { v: View, event: MotionEvent ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performClick()
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_UP -> {

                    // Get the MeteringPointFactory from PreviewView
                    val factory = viewFinder.meteringPointFactory

                    // Create a MeteringPoint from the tap coordinates
                    val point = factory.createPoint(event.x, event.y)

                    Log.v("chaewon", "point: ${event.x}, ${event.y}")

                    // Create a MeteringAction from the MeteringPoint, you can configure it to specify the metering mode
                    val action = FocusMeteringAction.Builder(point)
                        .build()

                    // Trigger the focus and metering. The method returns a ListenableFuture since the operation
                    // is asynchronous. You can use it get notified when the focus is successful or if it fails.
                    var result = cameraController?.startFocusAndMetering(action)!!

                    v.performClick()

                    return@setOnTouchListener true
                }
                else -> return@setOnTouchListener false
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

    }


    override fun onResume() {
        super.onResume()

        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)

        isBackLens = sharedPref?.getBoolean("isBackLens", true)

        Log.v("chaewon", "$isBackLens")

        // 카메라 권한 확인 후 카메라 시작하기
        if(allPermissionsGranted()){
            startCamera(isBackLens)
        } else {
            ActivityCompat.requestPermissions(
                activity, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // 저장된 값을 가져옵니다.
        selectedRadioIndex = sharedPref?.getInt("selectedRadioIndex", 0)

        Log.v("chaewon", "$selectedRadioIndex")

        // 가져온 값을 사용합니다.
        if (selectedRadioIndex != null && selectedRadioIndex!! >= 0) {
            // 저장된 라디오 버튼 인덱스를 사용하여 라디오 버튼을 선택합니다.
            when (selectedRadioIndex) {
                0 -> {
                    basicRadioBtn.isChecked = true
                    basicRadioBtn.setTypeface(null, Typeface.BOLD)
                    basicToggleBtn.visibility = View.VISIBLE

                    overlay.visibility = View.GONE

                    distanceOptionLinearLayout.visibility = View.GONE
                    seekBarLinearLayout.visibility = View.GONE
                }
                1 -> {
                    objectFocusRadioBtn.isChecked = true
                    objectFocusRadioBtn.setTypeface(null, Typeface.BOLD)
                    basicToggleBtn.visibility = View.INVISIBLE

                    overlay.visibility = View.VISIBLE

                    distanceOptionLinearLayout.visibility = View.GONE
                    seekBarLinearLayout.visibility = View.GONE
                }
                2 -> {
                    distanceFocusRadioBtn.isChecked = true
                    distanceFocusRadioBtn.setTypeface(null, Typeface.BOLD)
                    basicToggleBtn.visibility = View.INVISIBLE

                    overlay.visibility = View.GONE

                    distanceOptionLinearLayout.visibility = View.VISIBLE
                    seekBarLinearLayout.visibility = View.VISIBLE
                }
                3 -> {
                    autoRewindRadioBtn.isChecked = true
                    autoRewindRadioBtn.setTypeface(null, Typeface.BOLD)
                    basicToggleBtn.visibility = View.INVISIBLE

                    overlay.visibility = View.GONE

                    distanceOptionLinearLayout.visibility = View.GONE
                    seekBarLinearLayout.visibility = View.GONE
                }
            }
        }

        val isToggleChecked = sharedPref?.getBoolean("isToggleChecked", false)
        if(isToggleChecked != null){
            when(isToggleChecked){
                false -> basicToggleBtn.isChecked = false
                true -> basicToggleBtn.isChecked = true
            }
        }


        /**
         * radioGroup.setOnCheckedChangeListener
         *      1. Basic 버튼 눌렸을 때, Single Mode나 Burst Mode 선택 버튼이 나타나게 하기
         *      2. Basic 버튼 안 누르면 사라지게 하기
         *      3. Option에 따른 카메라 설정
         */
        modeRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId){
                basicRadioBtn.id -> {
                    selectedRadioIndex = 0
                    basicToggleBtn.visibility = View.VISIBLE

                    basicRadioBtn.setTypeface(null, Typeface.BOLD)
                    objectFocusRadioBtn.setTypeface(null, Typeface.NORMAL)
                    distanceFocusRadioBtn.setTypeface(null, Typeface.NORMAL)
                    autoRewindRadioBtn.setTypeface(null, Typeface.NORMAL)

                    overlay.visibility = View.GONE

                    distanceOptionLinearLayout.visibility = View.GONE
                    seekBarLinearLayout.visibility = View.GONE

                    viewFinder.isEnabled = true
                }

                objectFocusRadioBtn.id -> {
                    selectedRadioIndex = 1
                    basicToggleBtn.visibility = View.INVISIBLE

                    basicRadioBtn.setTypeface(null, Typeface.NORMAL)
                    objectFocusRadioBtn.setTypeface(null, Typeface.BOLD)
                    distanceFocusRadioBtn.setTypeface(null, Typeface.NORMAL)
                    autoRewindRadioBtn.setTypeface(null, Typeface.NORMAL)

                    overlay.visibility = View.VISIBLE

                    distanceOptionLinearLayout.visibility = View.GONE
                    seekBarLinearLayout.visibility = View.GONE

                    viewFinder.isEnabled = true
                }

                distanceFocusRadioBtn.id -> {
                    selectedRadioIndex = 2
                    basicToggleBtn.visibility = View.INVISIBLE

                    basicRadioBtn.setTypeface(null, Typeface.NORMAL)
                    objectFocusRadioBtn.setTypeface(null, Typeface.NORMAL)
                    distanceFocusRadioBtn.setTypeface(null, Typeface.BOLD)
                    autoRewindRadioBtn.setTypeface(null, Typeface.NORMAL)

                    overlay.visibility = View.GONE

                    distanceOptionLinearLayout.visibility = View.VISIBLE
                    seekBarLinearLayout.visibility = View.VISIBLE

                    viewFinder.isEnabled = false
                }

                autoRewindRadioBtn.id -> {
                    selectedRadioIndex = 3
                    basicToggleBtn.visibility = View.INVISIBLE

                    basicRadioBtn.setTypeface(null, Typeface.NORMAL)
                    objectFocusRadioBtn.setTypeface(null, Typeface.NORMAL)
                    distanceFocusRadioBtn.setTypeface(null, Typeface.NORMAL)
                    autoRewindRadioBtn.setTypeface(null, Typeface.BOLD)

                    overlay.visibility = View.GONE

                    distanceOptionLinearLayout.visibility = View.GONE
                    seekBarLinearLayout.visibility = View.GONE

                    viewFinder.isEnabled = true

                }
            }
        }

// shutter Btn 클릭
        shutterBtn.setOnClickListener {

            rotation.start()

            shutterBtn.isEnabled = false
            galleryBtn.isEnabled = false
            convertBtn.isEnabled = false
            basicRadioBtn.isEnabled = false
            objectFocusRadioBtn.isEnabled = false
            distanceFocusRadioBtn.isEnabled = false
            autoRewindRadioBtn.isEnabled = false
            basicToggleBtn.isEnabled = false
            binding.distanceManualRadioBtn.isEnabled = false
            binding.distanceAutoRadioBtn.isEnabled = false


            // previewByteArrayList 초기화
            previewByteArrayList.clear()

            //Basic 모드
            if(basicRadioBtn.isChecked){

                // Single 모드
                if(!(basicToggleBtn.isChecked)){

                    turnOnAFMode()

                    CoroutineScope(Dispatchers.IO).launch {
                        mediaPlayer.start()
                        val result = takePicture(0)

                        // 녹음 중단
                        val savedFile = audioResolver.stopRecording()
                        if (savedFile != null) {
                            val audioBytes = audioResolver.getByteArrayInFile(savedFile)
                            jpegViewModel.jpegMCContainer.value!!.setAudioContent(
                                audioBytes,
                                ContentAttribute.basic
                            )
                            Log.d("AudioModule", "녹음된 오디오 사이즈 : ${audioBytes.size.toString()}")
                        }

                        jpegViewModel.jpegMCContainer.value!!.setImageContent(
                            previewByteArrayList,
                            ContentType.Image,
                            ContentAttribute.basic
                        )

                        withContext(Dispatchers.Main) {

                            shutterBtn.isEnabled = true
                            galleryBtn.isEnabled = true
                            convertBtn.isEnabled = true
                            basicRadioBtn.isEnabled = true
                            objectFocusRadioBtn.isEnabled = true
                            distanceFocusRadioBtn.isEnabled = true
                            autoRewindRadioBtn.isEnabled = true
                            basicToggleBtn.isEnabled = true
                            binding.distanceManualRadioBtn.isEnabled = true
                            binding.distanceAutoRadioBtn.isEnabled = true

                            rotation.cancel()
                        }

                        imageContent.activityType = ActivityType.Camera
                        CoroutineScope(Dispatchers.Default).launch {
                            // RewindFragment로 이동
                            withContext(Dispatchers.Main) {
                                findNavController().navigate(R.id.action_cameraFragment_to_basicModeEditFragment)
                            }
                        }
                    }
                }

                // Burst 모드
                else{
                    turnOnBurstMode()
                    audioResolver.startRecording("camera_record")
                    for (i in 1..burstSize) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val result = takePicture(i)
                        }
                    } // end of the for ...

                    CoroutineScope(Dispatchers.IO).launch {

                        while (previewByteArrayList.size < burstSize) { }

                        if (previewByteArrayList.size == burstSize) {
                            mediaPlayer.start()
                            // 녹음 중단
                            val savedFile = audioResolver.stopRecording()
                            if (savedFile != null) {
                                val audioBytes = audioResolver.getByteArrayInFile(savedFile)
                                jpegViewModel.jpegMCContainer.value!!.setAudioContent(
                                    audioBytes,
                                    ContentAttribute.basic
                                )
                                Log.d("AudioModule", "녹음된 오디오 사이즈 : ${audioBytes.size.toString()}")
                            }
                            Log.d("burst", "setImageContent 호출 전")

                            withContext(Dispatchers.Main) {

                                shutterBtn.isEnabled = true
                                galleryBtn.isEnabled = true
                                convertBtn.isEnabled = true
                                basicRadioBtn.isEnabled = true
                                objectFocusRadioBtn.isEnabled = true
                                distanceFocusRadioBtn.isEnabled = true
                                autoRewindRadioBtn.isEnabled = true
                                basicToggleBtn.isEnabled = true
                                binding.distanceManualRadioBtn.isEnabled = true
                                binding.distanceAutoRadioBtn.isEnabled = true

                                rotation.cancel()
                            }

                            imageContent.activityType = ActivityType.Camera
                            CoroutineScope(Dispatchers.Default).launch {
                                // RewindFragment로 이동
                                withContext(Dispatchers.Main) {
                                    var jop = async {
                                        jpegViewModel.jpegMCContainer.value!!.setImageContent(
                                            previewByteArrayList,
                                            ContentType.Image,
                                            ContentAttribute.burst
                                        )
                                    }
                                    jop.await()
                                    Log.d("error 잡기", "넘어가기 전")
                                    imageContent.activityType = ActivityType.Camera
                                    findNavController().navigate(R.id.action_cameraFragment_to_burstModeEditFragment)
                                }
                            }

                        }
                    } // end of Coroutine ...

                } // end of else ...
            }

            // Object Focus 모드
            else if(objectFocusRadioBtn.isChecked){
                turnOnAFMode()
                pointArrayList.clear()
                isFocusSuccess = false
                saveObjectCenterPoint()
                audioResolver.startRecording("camera_record")
            }

            // Distance Focus 모드
            else if(distanceFocusRadioBtn.isChecked){

                audioResolver.startRecording("camera_record")

                if(binding.distanceManualRadioBtn.isChecked){
                    turnOffAFMode(focusDistance)
                    CoroutineScope(Dispatchers.IO).launch {
                        mediaPlayer.start()
                        val result = takePicture(0)

                        // 녹음 중단
                        val savedFile = audioResolver.stopRecording()
                        if (savedFile != null) {
                            val audioBytes = audioResolver.getByteArrayInFile(savedFile)
                            jpegViewModel.jpegMCContainer.value!!.setAudioContent(
                                audioBytes,
                                ContentAttribute.basic
                            )
                            Log.d("AudioModule", "녹음된 오디오 사이즈 : ${audioBytes.size.toString()}")
                        }

                        jpegViewModel.jpegMCContainer.value!!.setImageContent(
                            previewByteArrayList,
                            ContentType.Image,
                            ContentAttribute.basic
                        )

                        withContext(Dispatchers.Main) {

                            shutterBtn.isEnabled = true
                            galleryBtn.isEnabled = true
                            convertBtn.isEnabled = true
                            basicRadioBtn.isEnabled = true
                            objectFocusRadioBtn.isEnabled = true
                            distanceFocusRadioBtn.isEnabled = true
                            autoRewindRadioBtn.isEnabled = true
                            basicToggleBtn.isEnabled = true
                            binding.distanceManualRadioBtn.isEnabled = true
                            binding.distanceAutoRadioBtn.isEnabled = true

                            rotation.cancel()
                        }

                        imageContent.activityType = ActivityType.Camera
                        CoroutineScope(Dispatchers.Default).launch {
                            // RewindFragment로 이동
                            withContext(Dispatchers.Main) {
                                findNavController().navigate(R.id.action_cameraFragment_to_focusChangeFragment)
                            }
                        }
                    }
                }
                else if(binding.distanceAutoRadioBtn.isChecked) {
                    turnOffAFMode(0f)
                    controlLensFocusDistance(0)
                }
            }

            // Auto Rewind 모드
            else if(autoRewindRadioBtn.isChecked){
                turnOnBurstMode()
                audioResolver.startRecording("camera_record")

                for (i in 1..burstSize) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val result = takePicture(i)
                    }
                } // end of the for ...

                CoroutineScope(Dispatchers.IO).launch {
                    while(previewByteArrayList.size < burstSize) { }
                    if (previewByteArrayList.size == burstSize) {
                        // 녹음 중단
                        val savedFile = audioResolver.stopRecording()
                        if (savedFile != null) {
                            var audioBytes = audioResolver.getByteArrayInFile(savedFile)
                            jpegViewModel.jpegMCContainer.value!!.setAudioContent(
                                audioBytes,
                                ContentAttribute.basic
                            )
                            Log.d("AudioModule", "녹음된 오디오 사이즈 : ${audioBytes.size.toString()}")
                        }

                        jpegViewModel.jpegMCContainer.value!!.setImageContent(
                            previewByteArrayList,
                            ContentType.Image,
                            ContentAttribute.burst
                        )

//                        imageContent.getBitmapList()

                        //jpegViewModel.jpegMCContainer.value?.save()

                        withContext(Dispatchers.Main) {

                            shutterBtn.isEnabled = true
                            galleryBtn.isEnabled = true
                            convertBtn.isEnabled = true
                            basicRadioBtn.isEnabled = true
                            objectFocusRadioBtn.isEnabled = true
                            distanceFocusRadioBtn.isEnabled = true
                            autoRewindRadioBtn.isEnabled = true
                            basicToggleBtn.isEnabled = true
                            binding.distanceManualRadioBtn.isEnabled = true
                            binding.distanceAutoRadioBtn.isEnabled = true

                            rotation.cancel()
                        }

                        imageContent.activityType = ActivityType.Camera
                        CoroutineScope(Dispatchers.Default).launch {
                            // RewindFragment로 이동
                            withContext(Dispatchers.Main) {
                                findNavController().navigate(R.id.action_cameraFragment_to_rewindFragment)
                            }
                        }
                    }
                } // end of Coroutine ...
            } // end of auto rewind ...
        }


        /**
         * ToggleButton 눌렸을 떄
         */

//        // AlphaAnimation 객체를 미리 생성합니다.
//        val fadeOutAnimation = AlphaAnimation(1f, 0f).apply {
//            duration = 500 // 0.5초 동안 서서히 사라지게 합니다.
//            setAnimationListener(object : Animation.AnimationListener {
//                override fun onAnimationStart(animation: Animation?) {
//                    // 애니메이션이 시작될 때 호출됩니다.
//                }
//                override fun onAnimationEnd(animation: Animation?) {
//                    // 애니메이션이 종료될 때 호출됩니다.
//                    if(isToggleChecked) {
//                        Log.v("basicToggle", "fadeOutAnimation : onAnimationEnd()")
//                        binding.burstToast.visibility = View.GONE
//                    } else{
//                        Log.v("basicToggle", "fadeOutAnimation : onAnimationEnd()")
//                        binding.singleToast.visibility = View.GONE
//                    }
//                }
//                override fun onAnimationRepeat(animation: Animation?) {
//                    // 애니메이션이 반복될 때 호출됩니다.
//                }
//            })
//        }
//        val fadeInAnimation = AlphaAnimation(0f, 1f).apply {
//            duration = 500 // 0.5초 동안 서서히 나타나게 합니다.
//            setAnimationListener(object : Animation.AnimationListener {
//                override fun onAnimationStart(animation: Animation?) {
//                    // 애니메이션이 시작될 때 호출됩니다.
//                }
//                override fun onAnimationEnd(animation: Animation?) {
//                    Log.v("basicToggle", "fadeInAnimation : onAnimationEnd()")
//                    // 애니메이션이 종료될 때 호출됩니다.
//                    if(isToggleChecked) {
//                        binding.burstToast.postDelayed({
//                            // 1초 후에 textView1을 서서히 사라지게 합니다.
//                            binding.burstToast.startAnimation(fadeOutAnimation)
//                        }, 1000)
//                    } else {
//                        binding.singleToast.postDelayed({
//                            // 1초 후에 textView1을 서서히 사라지게 합니다.
//                            binding.singleToast.startAnimation(fadeOutAnimation)
//                        }, 1000)
//                    }
//                }
//                override fun onAnimationRepeat(animation: Animation?) {
//                    // 애니메이션이 반복될 때 호출됩니다.
//                }
//            })
//        }

//        binding.basicToggle.setOnCheckedChangeListener { buttonView, isChecked ->
//            isToggleChecked = isChecked
//            if (isChecked) {
//                binding.burstToast.clearAnimation()
//                binding.singleToast.clearAnimation()
//                binding.burstToast.visibility = View.VISIBLE
//                binding.burstToast.startAnimation(fadeInAnimation)
//            } else {
//                binding.singleToast.clearAnimation()
//                binding.burstToast.clearAnimation()
//                binding.singleToast.visibility = View.VISIBLE
//                binding.singleToast.startAnimation(fadeInAnimation)
//            }
//        }

    }

    // A 프래그먼트의 onPause() 메서드에서 호출됩니다.
    override fun onPause() {
        super.onPause()

        // SharedPreferences 객체를 가져옵니다.
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)

        // SharedPreferences.Editor를 사용하여 상태 값을 저장합니다.
        with (sharedPref?.edit()) {
            this?.putInt("selectedRadioIndex", selectedRadioIndex!!)
            this?.putBoolean("isToggleChecked", isToggleChecked!!)
            this?.putBoolean("isBackLens", isBackLens!!)
            this?.apply()
        }
    }


    private fun showTextViewWithFadeInAnimation(view: View) {
        Log.v("basicToggle", "showTextViewWithFadeInAnimation View : ${view}")
        view.visibility = View.VISIBLE
        val fadeInAnimation = AlphaAnimation(0f, 1f)
        fadeInAnimation.duration = 500
        view.startAnimation(fadeInAnimation)
    }

    private fun showTextViewWithFadeOutAnimation(view: View) {
        Log.v("basicToggle", "showTextViewWithFadeOutAnimation View : ${view}")
        val fadeOutAnimation = AlphaAnimation(1f, 0f)
        fadeOutAnimation.duration = 500
        view.startAnimation(fadeOutAnimation)
        view.visibility = View.GONE
    }


    suspend fun takePicture(i : Int) : Int {
        return suspendCoroutine { continuation ->

            imageCapture.takePicture(cameraExecutor, object :
                ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val buffer = image.planes[0].buffer
                    buffer.rewind()
                    val bytes = ByteArray(buffer.capacity())
                    buffer.get(bytes)
                    previewByteArrayList.add(bytes)
                    image.close()

                    continuation.resume(1)
                    super.onCaptureSuccess(image)
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                }
            })
        }
    }


    private fun setDetecter() {
        // Step 2: Initialize the detector object
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(5)          // 최대 결과 (모델에서 감지해야 하는 최대 객체 수)
            .setScoreThreshold(0.4f)    // 점수 임계값 (감지된 객체를 반환하는 객체 감지기의 신뢰도)
            .build()
        customObjectDetector = ObjectDetector.createFromFileAndOptions(
            activity,
            "lite-model_efficientdet_lite0_detection_metadata_1.tflite",
            options
        )
    }

    private fun turnOffAFMode(distance : Float){
        Camera2CameraControl.from(camera.cameraControl).captureRequestOptions =
            CaptureRequestOptions.Builder()
                .apply {
                    setCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_MODE,
                        CameraMetadata.CONTROL_AF_MODE_OFF
                    )
                    // Fix focus lens distance to infinity to get focus far away (avoid to get a close focus)
                    setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, distance)
                }.build()
    }

    private fun turnOnAFMode(){
        Camera2CameraControl.from(camera.cameraControl).captureRequestOptions =
            CaptureRequestOptions.Builder()
                .apply {
                    setCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_MODE,
                        CameraMetadata.CONTROL_AF_MODE_AUTO
                    )
                }.build()
    }

    private fun turnOnBurstMode(){
        Camera2CameraControl.from(camera.cameraControl).captureRequestOptions =
            CaptureRequestOptions.Builder()
                .apply {
                    setCaptureRequestOption(
                        CaptureRequest.CONTROL_CAPTURE_INTENT,
                        CaptureRequest.CONTROL_CAPTURE_INTENT_STILL_CAPTURE
                    )
                }.build()
    }

    private fun previewToByteArray(){

        val imageCapture = imageCapture

        imageCapture!!.takePicture(cameraExecutor, object :
            ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {

                val buffer = image.planes[0].buffer
                buffer.rewind()
                val bytes = ByteArray(buffer.capacity())
                buffer.get(bytes)
                previewByteArrayList.add(bytes)

                image.close()
                super.onCaptureSuccess(image)
            }
        })
    }

    private fun saveObjectCenterPoint(){
        val detectObjectList = detectedList

        val scaleFactor = (viewFinder.width.toFloat()) / analyzeImageWidth.toFloat()

        for (obj in detectObjectList) {
            try {

                var pointX: Float =
                    (obj.boundingBox.left + ((obj.boundingBox.right - obj.boundingBox.left) * 0.5f)) * scaleFactor
                var pointY: Float =
                    (obj.boundingBox.top + ((obj.boundingBox.bottom - obj.boundingBox.top) * 0.5f)) * scaleFactor

                pointArrayList.add(pointData(pointX, pointY))

            } catch (e: IllegalAccessException) {
                e.printStackTrace();
            } catch (e: InvocationTargetException) {
                e.targetException.printStackTrace(); //getTargetException
            }
        }

        takeObjectFocusMode(0, detectObjectList)
    }

    /**
     * takeObjectFocusMode(index: Int)
     *      - 감지된 객체 별로 초점을 맞추고
     *          Preview를 ByteArray로 저장
     */
    private fun takeObjectFocusMode(index: Int, detectedObjectList : List<DetectionResult>) {
        if(index >= detectedObjectList.size){
            Log.v("chaewon","previewByteArrayList :: ${previewByteArrayList.size}, detectedObjectList :: ${detectedObjectList.size} ")
            CoroutineScope(Dispatchers.IO).launch {
                while (previewByteArrayList.size < detectedObjectList.size) { }

                if (previewByteArrayList.size == detectedObjectList.size) {
                    mediaPlayer.start()
                    // 녹음 중단
                    val savedFile = audioResolver.stopRecording()
                    if (savedFile != null) {
                        val audioBytes = audioResolver.getByteArrayInFile(savedFile)
                        jpegViewModel.jpegMCContainer.value!!.setAudioContent(
                            audioBytes,
                            ContentAttribute.basic
                        )
                    }
                    jpegViewModel.jpegMCContainer.value!!.setImageContent(
                        previewByteArrayList,
                        ContentType.Image,
                        ContentAttribute.object_focus
                    )

                    withContext(Dispatchers.Main) {

                        shutterBtn.isEnabled = true
                        galleryBtn.isEnabled = true
                        convertBtn.isEnabled = true
                        basicRadioBtn.isEnabled = true
                        objectFocusRadioBtn.isEnabled = true
                        distanceFocusRadioBtn.isEnabled = true
                        autoRewindRadioBtn.isEnabled = true
                        basicToggleBtn.isEnabled = true
                        binding.distanceManualRadioBtn.isEnabled = true
                        binding.distanceAutoRadioBtn.isEnabled = true

                        rotation.cancel()
                    }

//                    jpegViewModel.jpegMCContainer.value!!.save()
                    imageContent.activityType = ActivityType.Camera
                    CoroutineScope(Dispatchers.Default).launch {
                        withContext(Dispatchers.Main) {
                            findNavController().navigate(R.id.action_cameraFragment_to_focusChangeFragment)
                        }
                    }
                }
            }
            return
        } // end of if ...

        val point = factory.createPoint(pointArrayList[index].x, pointArrayList[index].y)

        val action = FocusMeteringAction.Builder(point)
            .build()

        val result = cameraController?.startFocusAndMetering(action)

        result?.addListener({
            try {
                isFocusSuccess = result.get().isFocusSuccessful
                Log.v("chaewon", "hi")
            } catch (e: IllegalAccessException) {
                Log.e("Error", "IllegalAccessException")
            } catch (e: InvocationTargetException) {
                Log.e("Error", "InvocationTargetException")
            }

            if (isFocusSuccess == true) {
                mediaPlayer.start()
                Log.v("chaewon", "success")
                previewToByteArray()
                isFocusSuccess = false
                Log.v("list size", "${previewByteArrayList.size}")
                takeObjectFocusMode(index + 1, detectedObjectList)
            } else {
                // 초점이 안잡혔다면 다시 그 부분에 초점을 맞춰라
                Log.v("Focus", "false")
                takeObjectFocusMode(index, detectedObjectList)
            }
        }, ContextCompat.getMainExecutor(activity))

    } // end of takeObjectFocusMode()...


    /**
     * Lens Focus Distance 바꾸면서 사진 찍기
     */
    private fun controlLensFocusDistance(photoCnt: Int) {
        if (photoCnt >= DISTANCE_FOCUS_PHOTO_COUNT){
            CoroutineScope(Dispatchers.IO).launch {
                while (previewByteArrayList.size < DISTANCE_FOCUS_PHOTO_COUNT) { }

                if (previewByteArrayList.size == DISTANCE_FOCUS_PHOTO_COUNT) {
                    mediaPlayer.start()
                    // 녹음 중단
                    val savedFile = audioResolver.stopRecording()
                    if (savedFile != null) {
                        val audioBytes = audioResolver.getByteArrayInFile(savedFile)
                        jpegViewModel.jpegMCContainer.value!!.setAudioContent(
                            audioBytes,
                            ContentAttribute.basic
                        )
                        Log.d("AudioModule", "녹음된 오디오 사이즈 : ${audioBytes.size.toString()}")
                    }
                    Log.d("burst", "setImageContent 호출 전")
                    jpegViewModel.jpegMCContainer.value!!.setImageContent(
                        previewByteArrayList,
                        ContentType.Image,
                        ContentAttribute.distance_focus
                    )

                    withContext(Dispatchers.Main) {

                        shutterBtn.isEnabled = true
                        galleryBtn.isEnabled = true
                        convertBtn.isEnabled = true
                        basicRadioBtn.isEnabled = true
                        objectFocusRadioBtn.isEnabled = true
                        distanceFocusRadioBtn.isEnabled = true
                        autoRewindRadioBtn.isEnabled = true
                        basicToggleBtn.isEnabled = true
                        binding.distanceManualRadioBtn.isEnabled = true
                        binding.distanceAutoRadioBtn.isEnabled = true

                        rotation.cancel()
                    }

//                    jpegViewModel.jpegMCContainer.value!!.save()
                    imageContent.activityType = ActivityType.Camera
                    CoroutineScope(Dispatchers.Default).launch {
                        withContext(Dispatchers.Main) {
                            findNavController().navigate(R.id.action_cameraFragment_to_focusChangeFragment)
                        }
                    }
                }
            }
            return
        }

        val distance: Float? = 0F + lensDistanceSteps * photoCnt
        turnOffAFMode(distance!!)

        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(cameraExecutor, object :
            ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                controlLensFocusDistance(photoCnt + 1)
                val buffer = image.planes[0].buffer
                buffer.rewind()
                val bytes = ByteArray(buffer.capacity())
                buffer.get(bytes)
                previewByteArrayList.add(bytes)
                image.close()
                super.onCaptureSuccess(image)
            }
        })
    }

    /**
     * takePhoto()
     *      - 사진 촬영 후 저장
     *          오로지 저장이 잘 되는지 확인하는 용도
     */
    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                activity.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(activity),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun
                        onImageSaved(output: ImageCapture.OutputFileResults) {
//                    val msg = "Photo capture succeeded: ${output.savedUri}"
//                    Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
//                    mediaPlayer.start()
                }
            }
        )
    }

    /**
     * startCamera()
     *      - 카메라 세팅하기
     */
    private fun startCamera(isBackLens : Boolean?) {
        // 1. CameraProvider 요청
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)

        cameraProviderFuture.addListener({

            // 2. CameraProvider 사용 가능 여부 확인
            // 생명주기에 binding 할 수 있는 ProcessCameraProvider 객체 가져옴
            val cameraProvider = cameraProviderFuture.get()

            // 3. 카메라를 선택하고 use case를 같이 생명주기에 binding

            // 3-1. Preview를 생성 → Preview를 통해서 카메라 미리보기 화면을 구현.
            // surfaceProvider는 데이터를 받을 준비가 되었다는 신호를 카메라에게 보내준다.
            // setSurfaceProvider는 PreviewView에 SurfaceProvider를 제공해준다.
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3) // Preview 4:3 비율
                .build()


            imageAnalyzer =
                ImageAnalysis.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                    // The analyzer can then be assigned to the instance
                    .also {
                        it.setAnalyzer(cameraExecutor) { image ->

                            analyzeImageWidth = image.height
//                            analyzeImageHeight = image.height

                            if (!::bitmapBuffer.isInitialized) {
                                // The image rotation and RGB image buffer are initialized only once
                                // the analyzer has started running
                                bitmapBuffer = Bitmap.createBitmap(
                                    image.width,
                                    image.height,
                                    Bitmap.Config.ARGB_8888
                                )
                            }

                            val imageRotation = image.imageInfo.rotationDegrees
                            image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

                            val imageProcessor =
                                ImageProcessor.Builder()
                                    .add(Rot90Op(-imageRotation / 90))
                                    .build()

                            // Preprocess the image and convert it into a TensorImage for detection.
                            val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmapBuffer))

                            val results = customObjectDetector?.detect(tensorImage)

                            detectedList = results!!.map {
                                // Get the top-1 category and craft the display text
                                val category = it.categories.first()
                                val text = "${category.label}"

                                // Create a data object to display the detection result
                                DetectionResult(it.boundingBox, text)
                            }

                            var inferenceTime = SystemClock.uptimeMillis()
                            inferenceTime = SystemClock.uptimeMillis() - inferenceTime

                            onResults(
                                results,
                                inferenceTime,
                                tensorImage.height,
                                tensorImage.width)
                        }
                    }


            imageCapture = ImageCapture.Builder()
                .setTargetRotation(Surface.ROTATION_0)
                .build()

            val orientationEventListener = object : OrientationEventListener(context) {
                override fun onOrientationChanged(orientation: Int) {
                    when (orientation) {
                        0 -> imageCapture.setTargetRotation(Surface.ROTATION_0)
                        90 -> imageCapture.setTargetRotation(Surface.ROTATION_270)
                        180 -> imageCapture.setTargetRotation(Surface.ROTATION_180)
                        270 -> imageCapture.setTargetRotation(Surface.ROTATION_90)
                    }
                }
            }
            orientationEventListener.enable()

            if(isBackLens!!){
                // 3-2. 카메라 세팅
                // CameraSelector는 카메라 세팅을 맡는다.(전면, 후면 카메라)
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            }

            try{
                // binding 전에 binding 초기화
                cameraProvider.unbindAll()

                // 3-3. use case와 카메라를 생명 주기에 binding
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer
                )

                cameraController = camera!!.cameraControl
                camera2CameraInfo = Camera2CameraInfo.from(camera.cameraInfo)

                preview.setSurfaceProvider(viewFinder.surfaceProvider)

                // 지원되는 MeteringPoints 가져오기
                val meteringPoints = camera2CameraInfo.getCameraCharacteristic(CameraCharacteristics.CONTROL_MAX_REGIONS_AF)

                // 스마트폰 기기 별 min Focus Distance 알아내기 ( 가장 `가까운` 곳에 초점을 맞추기 위한 렌즈 초점 거리 )
                // 대부분 10f
                minFocusDistance =
                    camera2CameraInfo.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)!!
                // 연속 사진 촬영 장수에 따른 Step 거리
                lensDistanceSteps = minFocusDistance / (DISTANCE_FOCUS_PHOTO_COUNT.toFloat())

                setBottomMenuHeight()

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(activity))
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = viewFinder.display.rotation
    }

    // Update UI after objects have been detected. Extracts original image height/width
    // to scale and place bounding boxes properly through OverlayView
    override fun onResults(
        results: MutableList<Detection>?,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {
        activity?.runOnUiThread {
            // Pass necessary information to OverlayView for drawing on the canvas
            overlay.setResults(
                results ?: LinkedList<Detection>(),
                imageHeight,
                imageWidth
            )

            // Force a redraw
            overlay.invalidate()
        }
    }

    override fun onError(error: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }

    fun getStatusBarHeightDP(context: Context): Int {
        var result = 0
        val resourceId: Int = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimension(resourceId).toInt()
        }
        return result
    }

    fun setBottomMenuHeight(){
        val statusBarHeight = getStatusBarHeightDP(requireContext())
        val params: ViewGroup.LayoutParams = binding.bottomMenu.getLayoutParams()
        val topMenuParams: ViewGroup.LayoutParams = binding.topMenu.getLayoutParams()
        val displaySize = Point()
        activity.windowManager.defaultDisplay.getSize(displaySize)

        if(viewFinder.height == 0){
            params.height =
                displaySize.y - topMenuParams.height - (displaySize.x.toFloat() * (4f / 3f)).toInt() - statusBarHeight
        }else {
            Log.v("chaewon", "viewFinder ${viewFinder.width} x ${binding.viewFinder.height}")
            params.height =
                displaySize.y - topMenuParams.height - viewFinder.height - statusBarHeight
        }
        binding.bottomMenu.setLayoutParams(params)
    }

    /**
     * allPermissionsGranted()
     *      - 카메라 권한 확인하기
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "OnePIC"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val DISTANCE_FOCUS_PHOTO_COUNT = 10
        private val REQUIRED_PERMISSIONS = // Array<String>
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).toTypedArray()

        private val REQUEST_CAMERA_PERMISSION_CODE = 1001
        private val REQUEST_RECORD_AUDIO_PERMISSION_CODE = 1002
    }
}