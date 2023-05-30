package com.goldenratio.onepic.CameraModule.Camera2Module

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.hardware.camera2.*
import android.media.ImageReader
import android.media.MediaPlayer
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.test_camera2.CameraHelper.AutoFitTexutreView
import com.example.test_camera2.CameraHelper.CompareSizesByArea
import com.example.test_camera2.CameraHelper.ImageSaver
import com.goldenratio.onepic.CameraModule.CameraEditorActivity
import com.goldenratio.onepic.CameraModule.CameraFragment
import com.goldenratio.onepic.R
import com.goldenratio.onepic.databinding.FragmentCameraBinding
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class Camera2(activity: CameraEditorActivity, context: Context, binding: FragmentCameraBinding){
    private val activity : CameraEditorActivity
    private val binding : FragmentCameraBinding
    private val context : Context

    private val textureView : AutoFitTexutreView
    private var sensorOrientation = 0
    private lateinit var previewSize: Size
    private var flashSupported = false
    var cameraId: String = CAMERA_BACK
    private val cameraOpenCloseLock = Semaphore(1)
    private var imageReader: ImageReader? = null
    private var cameraDevice: CameraDevice? = null
    private lateinit var previewRequestBuilder: CaptureRequest.Builder
    var captureSession: CameraCaptureSession? = null
    private lateinit var previewRequest: CaptureRequest

    var state = STATE_PREVIEW

    private var BURST_SIZE = 5

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    var previewByteArrayList: ArrayList<ByteArray> = arrayListOf()

    var dummyWidth : Int? = null
    var dummyHeight : Int? = null

    init {
        this.activity = activity
        this.binding = binding
        this.context = context

        textureView = binding.textureView
    }

    /**
     * [TextureView.SurfaceTextureListener] handles several lifecycle events on a [TextureView].
     */
    val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        // SurfaceTexture가 생성되고 준비 되었을 때
        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            openCamera(width, height)
        }
        // i) 사용자가 화면 방향을 바꿨을 때
        // ii) TextureView 크기가 변경됐을 때
        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture) = true

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
    }

    private val onImageAvailableListener = ImageReader.OnImageAvailableListener {
        backgroundHandler?.post(ImageSaver(it.acquireNextImage(), context, this@Camera2))
    }

    private val stateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            this@Camera2.cameraDevice = cameraDevice
            createCameraPreviewSession()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice.close()
            this@Camera2.cameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            onDisconnected(cameraDevice)
            this@Camera2.activity?.finish()
        }

    }

    /**
     * A [CameraCaptureSession.CaptureCallback] that handles events related to JPEG capture.
     */
    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {

        private fun process(result: CaptureResult) {
            when (state) {
                STATE_PREVIEW -> Unit // Do nothing when the camera preview is working normally.
                STATE_WAITING_LOCK -> capturePicture(result)
                STATE_WAITING_PRECAPTURE -> {
                    // CONTROL_AE_STATE can be null on some devices
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null ||
                        aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                        aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        state = STATE_WAITING_NON_PRECAPTURE
                    }
                }
                STATE_WAITING_NON_PRECAPTURE -> {
                    // CONTROL_AE_STATE can be null on some devices
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        state = STATE_PICTURE_TAKEN
                        captureStillPicture()
                    }
                }
            }
        }

        private fun capturePicture(result: CaptureResult) {
            val afState = result.get(CaptureResult.CONTROL_AF_STATE)
            if (afState == null) {
                captureStillPicture()
            } else if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
                || afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED
                || afState == CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED) {

                // CONTROL_AE_STATE can be null on some devices
                val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                    state = STATE_PICTURE_TAKEN
                    captureStillPicture()
                } else {
                    runPrecaptureSequence()
                }
            } else if (afState == CaptureResult.CONTROL_AF_STATE_INACTIVE){
                captureStillPicture()
            }
        }

        override fun onCaptureProgressed(session: CameraCaptureSession,
                                         request: CaptureRequest,
                                         partialResult: CaptureResult) {
            process(partialResult)
        }

        override fun onCaptureCompleted(session: CameraCaptureSession,
                                        request: CaptureRequest,
                                        result: TotalCaptureResult) {
            process(result)
        }

    }

    /**
     * openCamera(width: Int, height: Int)
     */
    @SuppressLint("MissingPermission")
    fun openCamera(width: Int, height: Int){

        val permission = ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
        val permissionAudio =
            ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)

        if (permission != PackageManager.PERMISSION_GRANTED
            || permissionAudio != PackageManager.PERMISSION_GRANTED) {
            checkPermissions()
            return
        }

        setUpCameraOutputs(width, height)
        configureTransform(width, height)

        val statusBarHeight = getStatusBarHeightDP(context)
        val params: ViewGroup.LayoutParams = binding.bottomMenu.getLayoutParams()
        val topMenuParams: ViewGroup.LayoutParams = binding.topMenu.getLayoutParams()
        val displaySize = Point()
        activity.windowManager.defaultDisplay.getSize(displaySize)
//        params.height = displaySize.y - topMenuParams.height - textureView.height - statusBarHeight
        params.height = displaySize.y - topMenuParams.height - textureView.height

        if(statusBarHeight <= 75)
            params.height -= statusBarHeight

        binding.bottomMenu.setLayoutParams(params)

        val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try{
            if(!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MICROSECONDS)){
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            manager.openCamera(cameraId, stateCallback, backgroundHandler)
        }catch (e: CameraAccessException) {

        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.", e)
        }
    }

    /**
     * Closes the current [CameraDevice].
     */
    fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            imageReader?.close()
            imageReader = null
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    /**
     * startBackgroundThread()
     *          - 카메라 관련 작업을 하기 위한 스레드와 핸들러 생성
     */
    fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper!!)
    }

    /**
     * stopBackgroundThread()
     */
    fun stopBackgroundThread(){
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException){
            Log.e(TAG, e.toString())
        }
    }

    private fun checkPermissions() {
        //거절되었거나 아직 수락하지 않은 권한(퍼미션)을 저장할 문자열 배열 리스트
        var rejectedPermissionList = ArrayList<String>()

        //필요한 퍼미션들을 하나씩 끄집어내서 현재 권한을 받았는지 체크
        for(permission in REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                //만약 권한이 없다면 rejectedPermissionList에 추가
                rejectedPermissionList.add(permission)
            }
        }
        //거절된 퍼미션이 있다면...
        if(rejectedPermissionList.isNotEmpty()){
            //권한 요청!
            val array = arrayOfNulls<String>(rejectedPermissionList.size)
            ActivityCompat.requestPermissions(activity, rejectedPermissionList.toArray(array), REQUEST_PERMISSION)
        }
    }

    private fun setUpCameraOutputs(width: Int, height: Int){
        val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try{
//            for(cameraId in manager.cameraIdList){
                // CameraCharacteristics : 카메라 정보를 가짐
//                cameraId = CAMERA_FRONT
                val characteristics = manager.getCameraCharacteristics(cameraId)

                val cameraDirection = characteristics.get(CameraCharacteristics.LENS_FACING)
//                if(cameraDirection != null &&
//                    cameraDirection == CameraCharacteristics.LENS_FACING_FRONT)
//                    return

//            if(cameraDirection != null ) return

                // 프리뷰 설정하는데 사용
                // 현재 카메라의 스트림 구성 맵 가져오기
                val map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return

                // map.getOutputSizes(format) : 현재 카메라에서 지원하는 JPEG 이미지 출력 크기 가져오기
                // CompareSizesByArea() : 지원하는 출력 크기 목록에서 가장 큰 크기 찾기
                val largest = Collections.max(
                    Arrays.asList(*map.getOutputSizes(ImageFormat.JPEG)),
                    CompareSizesByArea())

            dummyWidth = largest.width
            dummyHeight = largest.height

                imageReader = ImageReader.newInstance(largest.width, largest.height,
                    ImageFormat.JPEG, /*maxImages*/40).apply{
                    setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)
                }

                // 카메라 이미지 센서와 디스플레이 회전 방향에 따른 가로 세로 길이 결정
                val displayRotation = activity.windowManager.defaultDisplay.rotation

                sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
                val swappedDimensions = areDimensionsSwapped(displayRotation)

                val displaySize = Point()

                activity.windowManager.defaultDisplay.getSize(displaySize)
                val rotatedPreviewWidth = if (swappedDimensions) height else width
                val rotatedPreviewHeight = if (swappedDimensions) width else height

                var maxPreviewWidth = if (swappedDimensions) displaySize.y else displaySize.x
                var maxPreviewHeight = if (swappedDimensions) displaySize.x else displaySize.y

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) maxPreviewWidth = MAX_PREVIEW_WIDTH
                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) maxPreviewHeight = MAX_PREVIEW_HEIGHT

                val activeArraySize = characteristics[CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE]

            Log.v("chaewon", "previewSize 설정 안해?")

                // 미리보기 크기를 결정하는 부분
                previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture::class.java),
                    rotatedPreviewWidth, rotatedPreviewHeight,
                    maxPreviewWidth, maxPreviewHeight,
                    largest)
            Log.v("chaewon", "previewSize width : ${previewSize.width}")
                // We fit the aspect ratio of TextureView to the size of preview we picked.
                if (activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    textureView.setAspectRatio(previewSize.width, previewSize.height)

                } else {
                    textureView.setAspectRatio(previewSize.height, previewSize.width)
                }

                flashSupported =
                    characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true

                this.cameraId = cameraId

                return
//            }
        }catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        } catch (e: NullPointerException) {
            Log.e(TAG, "This device doesn't support Camera2 API.")
        }
    }

    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        activity ?: return
        val rotation = activity.windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, previewSize.height.toFloat(), previewSize.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            val scale = Math.max(
                viewHeight.toFloat() / previewSize.height,
                viewWidth.toFloat() / previewSize.width)
            with(matrix) {
                setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
                postScale(scale, scale, centerX, centerY)
                postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
            }
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        textureView.setTransform(matrix)
    }

    private fun createCameraPreviewSession(){
        try{
            val texture = textureView.surfaceTexture
            texture?.setDefaultBufferSize(previewSize.width, previewSize.height)
            Log.v(TAG, "[createCameraPreviewSession] previewSize width x height = ${previewSize.width} x ${previewSize.height}")
            val surface = Surface(texture)

            previewRequestBuilder = cameraDevice!!.createCaptureRequest(
                CameraDevice.TEMPLATE_PREVIEW
            )
            previewRequestBuilder.addTarget(surface)

            cameraDevice?.createCaptureSession(Arrays.asList(surface, imageReader?.surface),
                object : CameraCaptureSession.StateCallback(){
                    override fun onConfigured(session: CameraCaptureSession) {
                        if(cameraDevice == null) return

                        captureSession = session
                        try{
                            previewRequestBuilder.set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)

                            setAutoFlash(previewRequestBuilder)

                            previewRequest = previewRequestBuilder.build()

                            captureSession?.setRepeatingRequest(previewRequest,
                                captureCallback, backgroundHandler)

                        }catch (e: CameraAccessException) {
                            Log.e(TAG, e.toString())
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Failed onConfigured")
                    }
                }, null)

        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }
    }

    private fun captureStillPicture() {
        try {
            if (activity == null || cameraDevice == null) return
            val rotation = activity.windowManager.defaultDisplay.rotation

            // This is the CaptureRequest.Builder that we use to take a picture.
            val captureBuilder = cameraDevice?.createCaptureRequest(
                CameraDevice.TEMPLATE_STILL_CAPTURE)?.apply {
                addTarget(imageReader?.surface!!)

                // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
                // We have to take that into account and rotate JPEG properly.
                // For devices with orientation of 90, we return our mapping from ORIENTATIONS.
                // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
                set(CaptureRequest.JPEG_ORIENTATION,
                    ((ORIENTATIONS.get(rotation) + sensorOrientation + 270) % 360 + 90) % 360)

                // Use the same AE and AF modes as the preview.
                set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
//                set(CaptureRequest.CONTROL_AF_MODE,
//                    CaptureRequest.CONTROL_AF_MODE_OFF)
            }?.also { setAutoFlash(it) }

            val captureCallback = object : CameraCaptureSession.CaptureCallback() {

                override fun onCaptureCompleted(session: CameraCaptureSession,
                                                request: CaptureRequest,
                                                result: TotalCaptureResult) {
                    unlockFocus()
                    val mediaPlayer = MediaPlayer.create(context, R.raw.end_sound)
                    mediaPlayer.start()
                }
            }

            captureSession?.apply {
                stopRepeating()
                abortCaptures()

                // Set up a list of capture requests to be captured in a burst
                val captureRequestList = mutableListOf<CaptureRequest>()
                for (i in 0 until BURST_SIZE) {
                    captureBuilder?.let { captureRequestList.add(it.build()) }
                }

                // Capture the burst of images
                captureBurst(captureRequestList, captureCallback, null)
            }

        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }

    }

    /**
     * Run the precapture sequence for capturing a still image. This method should be called when
     * we get a response in [.captureCallback] from [.lockFocus].
     */
    private fun runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START)
            // Tell #captureCallback to wait for the precapture sequence to be set.
            state = STATE_WAITING_PRECAPTURE
            captureSession?.capture(previewRequestBuilder.build(), captureCallback,
                backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }

    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
    fun lockFocus() {
        try {
            // This is how to tell the camera to lock focus.
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_START)
            // Tell #captureCallback to wait for the lock.
            state = STATE_WAITING_LOCK
            captureSession?.capture(previewRequestBuilder.build(), captureCallback,
                backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }

    }

    private fun unlockFocus() {
        try {
            // Reset the auto-focus trigger
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_CANCEL)
            setAutoFlash(previewRequestBuilder)
            captureSession?.capture(previewRequestBuilder.build(), captureCallback,
                backgroundHandler)
            // After this, the camera will go back to the normal state of preview.
            state = STATE_PREVIEW
            captureSession?.setRepeatingRequest(previewRequest, captureCallback,
                backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }

    }

    private fun setAutoFlash(requestBuilder: CaptureRequest.Builder) {
        if (flashSupported) {
            requestBuilder.set(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
        }
    }

    private fun areDimensionsSwapped(displayRotation : Int) : Boolean {
        var swappedDimensions = false
        when (displayRotation) {
            Surface.ROTATION_0, Surface.ROTATION_180 -> {
                if (sensorOrientation == 90 || sensorOrientation == 270){
                    swappedDimensions = true
                }
            }
            Surface.ROTATION_90, Surface.ROTATION_270 ->{
                if(sensorOrientation == 0 || sensorOrientation == 180){
                    swappedDimensions = true
                }
            }
            else -> {
                Log.e(TAG, "Display rotation is invalid: $displayRotation")
            }
        }
        return swappedDimensions
    }

    fun getStatusBarHeightDP(context: Context): Int {
        var result = 0
        val resourceId: Int = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimension(resourceId).toInt()
        }
        return result
    }

    fun startCamera2(){
        Log.v("burst error", "startCamera2()")
//        startBackgroundThread()

        // textureView가 사용 가능 여부
        if(textureView.isAvailable){ // textureView 적절히 생성 O
            openCamera(textureView.width, textureView.height)
        } else { // textureView 아직 생성 X
            textureView.surfaceTextureListener = surfaceTextureListener
        }
    }

    fun stopCamera2(){
        stopBackgroundThread()
        closeCamera()
    }

    fun setBurstSize ( size : Int ) {
        BURST_SIZE = size
    }
    companion object{
        private val TAG = "CameraFragment"
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO)
        private const val REQUEST_CAMERA_PERMISSION = 1
        private const val REQUEST_PERMISSION = 123
        private val ORIENTATIONS = SparseIntArray()

        private val MAX_PREVIEW_WIDTH = 1920
        private val MAX_PREVIEW_HEIGHT = 1080

        private val INPUT_SIZE = 320
        private val NUM_DETECTIONS = 5
        private val MIN_CONFIDENCE = 0.2f

        /**
         * Camera state: Showing camera preview.
         */
        private val STATE_PREVIEW = 0

        /**
         * Camera state: Waiting for the focus to be locked.
         */
        private val STATE_WAITING_LOCK = 1

        /**
         * Camera state: Waiting for the exposure to be precapture state.
         */
        private val STATE_WAITING_PRECAPTURE = 2

        /**
         * Camera state: Waiting for the exposure state to be something other than precapture.
         */
        private val STATE_WAITING_NON_PRECAPTURE = 3

        /**
         * Camera state: Picture was taken.
         */
        private val STATE_PICTURE_TAKEN = 4

        private val OBJECT_DETECT_SIZE = 5

        private val OBJECT_DETECT_PERCENTAGE = 0.45

        private val DISTANCE_COUNT = 5

        val CAMERA_FRONT = "1"

        val CAMERA_BACK = "0"

        @JvmStatic private fun chooseOptimalSize(
            choices: Array<Size>,
            textureViewWidth: Int,
            textureViewHeight: Int,
            maxWidth: Int,
            maxHeight: Int,
            aspectRatio: Size
        ): Size {

            // Collect the supported resolutions that are at least as big as the preview Surface
            val bigEnough = ArrayList<Size>()
            // Collect the supported resolutions that are smaller than the preview Surface
            val notBigEnough = ArrayList<Size>()
            val w = aspectRatio.width
            val h = aspectRatio.height
            for (option in choices) {
                if (option.width <= maxWidth && option.height <= maxHeight &&
                    option.height == option.width * h / w) {
                    if (option.width >= textureViewWidth && option.height >= textureViewHeight) {
                        bigEnough.add(option)
                    } else {
                        notBigEnough.add(option)
                    }
                }
            }

            // Pick the smallest of those big enough. If there is no one big enough, pick the
            // largest of those not big enough.
            if (bigEnough.size > 0) {
                return Collections.min(bigEnough, CompareSizesByArea())
            } else if (notBigEnough.size > 0) {
                return Collections.max(notBigEnough, CompareSizesByArea())
            } else {
                Log.e(TAG, "Couldn't find any suitable preview size")
                return choices[0]
            }
        }
    }

} // end of class...