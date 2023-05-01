package com.example.onepic.CameraModule

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.hardware.camera2.*
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.*
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.onepic.AudioModule.AudioResolver
import com.example.onepic.EditModule.RewindModule
import com.example.onepic.ImageToolModule
import com.example.onepic.JpegViewModel
import com.example.onepic.PictureModule.Contents.ActivityType
import com.example.onepic.PictureModule.Contents.ContentAttribute
import com.example.onepic.PictureModule.Contents.ContentType
import com.example.onepic.PictureModule.ImageContent
import com.example.onepic.R
import com.example.onepic.ViewerModule.ViewerEditorActivity
import com.example.onepic.databinding.FragmentCameraBinding
import kotlinx.coroutines.*
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.lang.reflect.InvocationTargetException
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


@OptIn(ExperimentalCamera2Interop::class)
class CameraFragment : Fragment() {
    data class pointData(var x: Float, var y: Float)
    data class DetectionResult(val boundingBox: RectF, val text: String)

    private var pointArrayList: ArrayList<pointData> = arrayListOf() // Object Focus
    private var previewByteArrayList: ArrayList<ByteArray> = arrayListOf()

    private lateinit var activity: CameraEditorActivity
    private lateinit var binding : FragmentCameraBinding
    private var isShutterClick : Boolean = false
    private lateinit var mediaPlayer : MediaPlayer

    // Camera
    private lateinit var analyzer: ImageAnalysis.Analyzer
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as CameraEditorActivity
        audioResolver = AudioResolver(activity)
        // 카메라 권한 확인 후 카메라 시작하기
        if(allPermissionsGranted()){
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                activity, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentCameraBinding.inflate(inflater, container, false)

        factory = binding.viewFinder.meteringPointFactory
        mediaPlayer = MediaPlayer.create(context, R.raw.end_sound)

        // imageContent 설정
        imageContent = jpegViewModel.jpegMCContainer.value!!.imageContent
        imageContent.activityType = ActivityType.Camera

        //rewind
        imageToolModule = ImageToolModule()
        rewindModule = RewindModule()

        // Initialize the detector object
        setDetecter()

//        // 카메라 권한 확인 후 카메라 시작하기
//        if(allPermissionsGranted()){
//            startCamera()
//        } else {
//            ActivityCompat.requestPermissions(
//                activity, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
//            )
//        }

        /**
         * radioGroup.setOnCheckedChangeListener
         *      1. Basic 버튼 눌렸을 때, Single Mode나 Burst Mode 선택 버튼이 나타나게 하기
         *      2. Basic 버튼 안 누르면 사라지게 하기
         *      3. Option에 따른 카메라 설정
         */
        binding.radioGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId){
                binding.basicRadio.id -> {
                    binding.basicToggle.visibility = View.VISIBLE
                    turnOnAEMode()
                }

                binding.distanceFocusRadio.id -> {
                    binding.basicToggle.visibility = View.INVISIBLE
                    turnOffAFMode(0F)
                }

                else -> {
                    binding.basicToggle.visibility = View.INVISIBLE
                    turnOnAEMode()
                }
            }
        }

        /**
         * shutterButton.setOnClickListener{ }
         *      - 셔터 버튼 눌렀을 때 Option에 따른 촬영
         */
        binding.shutterButton.setOnClickListener {

            previewByteArrayList.clear() // previewByteArrayList 초기화

            isShutterClick = true
            binding.shutterButton.isEnabled = !isShutterClick
            binding.shutterButton.setImageDrawable(resources.getDrawable(R.drawable.shutter_block))

            /**
             * Basic Mode
             */
            if(binding.basicRadio.isChecked){
                /**
                 * Single Mode
                 */
                if(!(binding.basicToggle.isChecked)){
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
                            isShutterClick = false
                            binding.shutterButton.isEnabled = !isShutterClick
                            binding.shutterButton.setImageDrawable(resources.getDrawable(R.drawable.shutter))
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
                /**
                 * Burst Mode
                 */
                else{
                    turnOnBurstMode()

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
                            jpegViewModel.jpegMCContainer.value!!.setImageContent(
                                previewByteArrayList,
                                ContentType.Image,
                                ContentAttribute.burst
                            )

                            withContext(Dispatchers.Main) {
                                isShutterClick = false
                                binding.shutterButton.isEnabled = !isShutterClick
                                binding.shutterButton.setImageDrawable(resources.getDrawable(R.drawable.shutter))
                            }

                            imageContent.activityType = ActivityType.Camera
                            CoroutineScope(Dispatchers.Default).launch {
                                // RewindFragment로 이동
                                withContext(Dispatchers.Main) {
                                    findNavController().navigate(R.id.action_cameraFragment_to_burstModeEditFragment)
                                }
                            }
                        }
                    } // end of Coroutine ...

                } // end of else ...
            }
            /**
             * Object Focus Mode
             */
            else if(binding.objectFocusRadio.isChecked){
                pointArrayList.clear()
                isFocusSuccess = false
                startObjectFocusMode()
                audioResolver.startRecording("camera_record")
            }
            /**
             * Distance Focus Mode
             */
            else if(binding.distanceFocusRadio.isChecked){
                controlLensFocusDistance(0)
            }
            /**
             * Auto Rewind Mode
             */
            else if(binding.autoRewindRadio.isChecked){
                turnOnBurstMode()

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

                        imageContent.getBitmapList()

                        //jpegViewModel.jpegMCContainer.value?.save()

                        withContext(Dispatchers.Main) {
                            isShutterClick = false
                            binding.shutterButton.isEnabled = !isShutterClick
                            binding.shutterButton.setImageDrawable(resources.getDrawable(R.drawable.shutter))
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

        binding.galleryButton.setOnClickListener{

            val intent =
                Intent(activity, ViewerEditorActivity::class.java) //fragment라서 activity intent와는 다른 방식

            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            activity?.supportFragmentManager?.beginTransaction()?.addToBackStack(null)?.commit()

            startActivity(intent)
        }

        /**
         * 화면 터치
         * 터치한 좌표로 초점 맞추기
         */
        binding.viewFinder.setOnTouchListener { v: View, event: MotionEvent ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performClick()
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_UP -> {

                    // Get the MeteringPointFactory from PreviewView
                    val factory = binding.viewFinder.meteringPointFactory

                    // Create a MeteringPoint from the tap coordinates
                    val point = factory.createPoint(event.x, event.y)

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

        return binding.root
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
            .setScoreThreshold(0.2f)    // 점수 임계값 (감지된 객체를 반환하는 객체 감지기의 신뢰도)
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

    private fun turnOnAEMode(){
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
//                    setCaptureRequestOption(
//                        CaptureRequest.CONTROL_CAPTURE_INTENT,
//                        CameraMetadata.CONTROL_CAPTURE_INTENT_ZERO_SHUTTER_LAG
//                    )
                    setCaptureRequestOption(
                        CaptureRequest.CONTROL_CAPTURE_INTENT,
                        CaptureRequest.CONTROL_CAPTURE_INTENT_STILL_CAPTURE
                    )
                }.build()
    }

//    /**
//     * < TEST >
//     * takePhotoIndex(index : Int, maxIndex : Int)
//     *      - 사진 촬영 시, 저장
//     */
//    private fun takePhotoIndex(index : Int, maxIndex : Int) {
//        if(index >= maxIndex){
////            mediaPlayer.start()
//            return
//        }
//        // Get a stable reference of the modifiable image capture use case
//        val imageCapture = imageCapture ?: return
//
//        // Create time stamped name and MediaStore entry.
//        val name = System.currentTimeMillis().toString() + ".jpg" // 파일이름 현재시간.jpg
//
//        val contentValues = ContentValues().apply {
//            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
//            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
//            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
//                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/ImageSave")
//            }
//        }
//
//        // Create output options object which contains file + metadata
//        val outputOptions = ImageCapture.OutputFileOptions
//            .Builder(
//                activity.contentResolver,
//                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//                contentValues
//            )
//            .build()
//
//        // Set up image capture listener, which is triggered after photo has
//        // been taken
//        imageCapture.takePicture(
//            outputOptions,
//            ContextCompat.getMainExecutor(activity),
//            object : ImageCapture.OnImageSavedCallback {
//                override fun onError(exc: ImageCaptureException) {
//                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
//                }
//
//                override fun
//                        onImageSaved(output: ImageCapture.OutputFileResults) {
//                    val msg = "Photo capture succeeded: ${output.savedUri}"
//                    Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
//                    takePhotoIndex(index + 1, maxIndex)
//                }
//            }
//        )
//    }

//    /**
//     * burstMode(index : Int, maxIndex : Int)
//     *      - 연속 촬영 모드
//     *          재귀로 돌면서 preview를 ByteArray로 변환
//     *          previewByteArrayList에 저장
//     */
//    private fun takeBurstMode(index : Int, maxIndex : Int){
//        if(index >= maxIndex){
//            mediaPlayer.start()
//            return
//        }
//
//
//        val imageCapture = imageCapture ?: return
//
//        imageCapture.takePicture(cameraExecutor, object :
//            ImageCapture.OnImageCapturedCallback() {
//            override fun onCaptureSuccess(image: ImageProxy) {
//
//                val buffer = image.planes[0].buffer
//                buffer.rewind()
//                val bytes = ByteArray(buffer.capacity())
//                buffer.get(bytes)
//                previewByteArrayList.add(bytes)
//
//                // 다시 호출
//                takeBurstMode(index + 1, maxIndex)
//
//                image.close()
//
//                super.onCaptureSuccess(image)
//            }
//
//            override fun onError(exception: ImageCaptureException) {
//                super.onError(exception)
//            }
//        })
//    }

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

            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
            }
        })
    }

    /**
     * startObjectFocusMode()
     *      - Preview에서 Bitmap 가져오고 runObjectDetection에 넘겨주기
     */
    private fun startObjectFocusMode() {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(cameraExecutor, object :
            ImageCapture.OnImageCapturedCallback() {
            @SuppressLint("RestrictedApi")
            override fun onCaptureSuccess(image: ImageProxy) {
                //get bitmap from image
                val bitmap = imageProxyToBitmap(image)
                val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 1080, 1440, false)

                // resizedBitmap 에서 객체 인식하기
                runObjectDetection(resizedBitmap)

                // 객체 별로 초점 맞춰서 저장
                takeObjectFocusMode(0)

                image.close()
                super.onCaptureSuccess(image)
            }

            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
            }
        })
    }

    /**
     * imageProxyToBitmap(image: ImageProxy): Bitmap
     *      - ImageProxy를 Bitmap으로 변환
     */
    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val planeProxy = image.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    /**
     * runObjectDetection(bitmap: Bitmap)
     *      - Tensorflow Lite 객체 인식
     *          객체 별 가운데 좌표 계산 > 초점 > 촬영
     */
    private fun runObjectDetection(bitmap: Bitmap) {
        // 객체 인지 후 객체 정보 받기
        detectedList = getObjectDetection(bitmap)

        for (obj in detectedList) {
            try {
                var pointX: Float =
                    (obj.boundingBox.left + ((obj.boundingBox.right - obj.boundingBox.left) / 2))
                var pointY: Float =
                    (obj.boundingBox.top + ((obj.boundingBox.bottom - obj.boundingBox.top) / 2))

                pointArrayList.add(pointData(pointX, pointY))

            } catch (e: IllegalAccessException) {
                e.printStackTrace();
            } catch (e: InvocationTargetException) {
                e.targetException.printStackTrace(); //getTargetException
            }
        }
    }

    /**
     * getObjectDetection(bitmap: Bitmap):
     *         ObjectDetection 결과(bindingBox) 및 category 그리기
     */
    private fun getObjectDetection(bitmap: Bitmap): List<DetectionResult> {
        // Step 1: Create TFLite's TensorImage object
        val image = TensorImage.fromBitmap(bitmap)

        // Step 3: Feed given image to the detector
        val results = customObjectDetector.detect(image)

        // Step 4: Parse the detection result and show it
        val resultToDisplay = results.map {
            // Get the top-1 category and craft the display text
            val category = it.categories.first()
            val text = "${category.label}"

            // Create a data object to display the detection result
            DetectionResult(it.boundingBox, text)
        }
        return resultToDisplay
    }

    /**
     * takeObjectFocusMode(index: Int)
     *      - 감지된 객체 별로 초점을 맞추고
     *          Preview를 ByteArray로 저장
     */
    private fun takeObjectFocusMode(index: Int) {
        if(index >= detectedList.size){
            CoroutineScope(Dispatchers.IO).launch {
                while (previewByteArrayList.size < detectedList.size) { }

                if (previewByteArrayList.size == detectedList.size) {
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
                        ContentAttribute.focus
                    )

                    withContext(Dispatchers.Main) {
                        isShutterClick = false
                        binding.shutterButton.isEnabled = !isShutterClick
                        binding.shutterButton.setImageDrawable(resources.getDrawable(R.drawable.shutter))
                    }

//                    jpegViewModel.jpegMCContainer.value!!.save()
                    imageContent.activityType = ActivityType.Camera
                    CoroutineScope(Dispatchers.Default).launch {
                        withContext(Dispatchers.Main) {
                            findNavController().navigate(R.id.action_cameraFragment_to_burstModeEditFragment)
                        }
                    }
                }
            }

            return
        }
        Log.v("index cnt", "index : ${index}, detectedList.size : ${detectedList.size}, pointArrayList.size : ${pointArrayList.size}")


        GlobalScope.launch(Dispatchers.Default){
            withContext(Dispatchers.Main){
//                val factory = binding.viewFinder.meteringPointFactory
                val point = factory.createPoint(pointArrayList[index].x, pointArrayList[index].y)
                val action = FocusMeteringAction.Builder(point)
                    .build()

                val result = cameraController?.startFocusAndMetering(action)
                result?.addListener({
                    try {
                        isFocusSuccess = result.get().isFocusSuccessful
                    } catch (e: IllegalAccessException) {
                        Log.e("Error", "IllegalAccessException")
                    } catch (e: InvocationTargetException) {
                        Log.e("Error", "InvocationTargetException")
                    }

                    if (isFocusSuccess == true) {
//                takePhoto()
                        previewToByteArray()
                        Log.v("list size", "${previewByteArrayList.size}")
                        takeObjectFocusMode(index + 1)
                        isFocusSuccess = false
                    } else {
                        // 초점이 안잡혔다면 다시 그 부분에 초점을 맞춰라
                        Log.v("Focus", "false")
                        takeObjectFocusMode(index)
                    }
                }, ContextCompat.getMainExecutor(activity))
            }
        }

//        val factory = binding.viewFinder.meteringPointFactory
//        val point = factory.createPoint(pointArrayList[index].x, pointArrayList[index].y)
//        val action = FocusMeteringAction.Builder(point)
//            .build()
//
//        val result = cameraController?.startFocusAndMetering(action)
//        result?.addListener({
//            try {
//                isFocusSuccess = result.get().isFocusSuccessful
//            } catch (e: IllegalAccessException) {
//                Log.e("Error", "IllegalAccessException")
//            } catch (e: InvocationTargetException) {
//                Log.e("Error", "InvocationTargetException")
//            }
//
//            if (isFocusSuccess == true) {
////                takePhoto()
//                previewToByteArray()
//                Log.v("list size", "${previewByteArrayList.size}")
//                takeObjectFocusMode(index + 1)
//                isFocusSuccess = false
//            } else {
//                // 초점이 안잡혔다면 다시 그 부분에 초점을 맞춰라
//                Log.v("Focus", "false")
//                takeObjectFocusMode(index)
//            }
//        }, ContextCompat.getMainExecutor(activity))
    }


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
                        ContentAttribute.focus
                    )

                    withContext(Dispatchers.Main) {
                        isShutterClick = false
                        binding.shutterButton.isEnabled = !isShutterClick
                        binding.shutterButton.setImageDrawable(resources.getDrawable(R.drawable.shutter))
                    }

//                    jpegViewModel.jpegMCContainer.value!!.save()
                    imageContent.activityType = ActivityType.Camera
                    CoroutineScope(Dispatchers.Default).launch {
                        withContext(Dispatchers.Main) {
                            findNavController().navigate(R.id.action_cameraFragment_to_burstModeEditFragment)
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

            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
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
    private fun startCamera() {
        // 1. CameraProvider 요청
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)

        cameraProviderFuture.addListener({

            // 2. CameraProvider 사용 가능 여부 확인
            // 생명주기에 binding 할 수 있는 ProcessCameraProvider 객체 가져옴
            cameraProvider = cameraProviderFuture.get()

            // 3. 카메라를 선택하고 use case를 같이 생명주기에 binding

            // 3-1. Preview를 생성 → Preview를 통해서 카메라 미리보기 화면을 구현.
            // surfaceProvider는 데이터를 받을 준비가 되었다는 신호를 카메라에게 보내준다.
            // setSurfaceProvider는 PreviewView에 SurfaceProvider를 제공해준다.
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3) // Preview 4:3 비율
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()


            // 3-2. 카메라 세팅
            // CameraSelector는 카메라 세팅을 맡는다.(전면, 후면 카메라)
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try{
                // binding 전에 binding 초기화
                cameraProvider.unbindAll()

                // 3-3. use case와 카메라를 생명 주기에 binding
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

                cameraController = camera!!.cameraControl
                camera2CameraInfo = Camera2CameraInfo.from(camera.cameraInfo)


                // 지원되는 MeteringPoints 가져오기
                val meteringPoints = camera2CameraInfo.getCameraCharacteristic(CameraCharacteristics.CONTROL_MAX_REGIONS_AF)
                if (meteringPoints == null || meteringPoints == 0) {
                    // 사용자에게 에러 메시지를 표시하거나 다른 처리를 수행합니다.
                    Log.v("meteringPoints", "해당 기기는 meteringPoints를 지원하지 않습니다.")
                } else {
                    // 지원되는 MeteringPoints를 사용하여 카메라 기능을 사용합니다.
                    Log.v("meteringPoints", "해당 기기는 meteringPoints를 지원합니다.")
                }

                // 스마트폰 기기 별 min Focus Distance 알아내기 ( 가장 `가까운` 곳에 초점을 맞추기 위한 렌즈 초점 거리 )
                // 대부분 10f
                minFocusDistance =
                    camera2CameraInfo.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)!!
                // 연속 사진 촬영 장수에 따른 Step 거리
                lensDistanceSteps = minFocusDistance / (DISTANCE_FOCUS_PHOTO_COUNT.toFloat())


            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(activity))
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
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}