package com.goldenratio.onepic.CameraModule.Camera2Module

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.hardware.camera2.*
import android.hardware.camera2.params.MeteringRectangle
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.example.test_camera2.CameraHelper.AutoFitTexutreView
import com.example.test_camera2.CameraHelper.CompareSizesByArea
import com.example.test_camera2.CameraHelper.ImageSaver
import com.goldenratio.onepic.CameraModule.CameraEditorActivity
import com.goldenratio.onepic.CameraModule.ObjectDetectionModule
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.math.PI
import kotlin.math.max

class Camera2Module(
    private val activity: CameraEditorActivity,
    private val context: Context,
    private val textureView: AutoFitTexutreView,
    private val imageView: ImageView,
    var previewByteArrayList : MutableLiveData<ArrayList<ByteArray>>
) {
    /**
     * TextureView를 사용 가능 한 지와 이와 관련된 surface에 관해 호출되는 리스너
     */
    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {

        // TextureView가 surfaceTexture를 사용할 준비가 됨
        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            openCamera(width, height)
        }

        // surfaceTexture의 버퍼크기가 변했음
        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
            configureTransform(width, height)
        }

        // surfaceTexture가 소멸되려 함
        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
            return true
        }

        // surfaceTexture가 업데이트 됨
        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {
            if (isDetectionChecked) {
                // 객체 인식 코드 호출
                val newBitmap = textureView.bitmap?.let {
                    objectDetectionModule.runObjectDetection(it)
                }
                imageView.setImageBitmap(newBitmap)
            }
        }
    }

    /**
     * 카메라 캡처 세션의 상태에 대한 업데이트를 수신하기 위한 콜백
     */
    private val stateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            this@Camera2Module.cameraDevice = cameraDevice
            createCameraPreviewSession()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice.close()
            this@Camera2Module.cameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {

        }

    }

    /**
     * 카메라 장치에 capture Request의 진행률을 추천하기 위한 콜백
     */
    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {

        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult,
        ) {
            process(partialResult)
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult,
        ) {
            process(result)
        }

//        var preState = 0;
//        var preAfState = 0;

        private fun process(result: CaptureResult) {
            when (state) {
                // 프리뷰 상태
                STATE_PREVIEW -> Unit // Do nothing when the camera preview is working normally.
                // 촬영 위해 focus lock한 상태
                STATE_WAITING_LOCK -> capturePicture(result)
                // 촬영 위해 precaputure 대기 상태
                STATE_WAITING_PRECAPTURE -> {
                    // CONTROL_AE_STATE can be null on some devices
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    Log.d("detectionResult", "4. process : STATE_WAITING_PRECAPTURE - ${aeState}")

                    if (aeState == null ||
                        aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                        aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED
                    ) {
                        state = STATE_WAITING_NON_PRECAPTURE
                    }
                }
                // 촬영을 위한 precapture 완료한 상태
                STATE_WAITING_NON_PRECAPTURE -> {

//                    Log.d("렌즈 초점 결과", "process: STATE_WAITING_NON_PRECAPTURE")
                    // CONTROL_AE_STATE can be null on some devices
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)

                    Log.d("detectionResult", "4. process : STATE_WAITING_NON_PRECAPTURE - ${aeState}")
                    if (aeState == null || isDetectionChecked || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE ) {
                        state = STATE_PICTURE_TAKEN
                        captureStillPicture(null)
                    }
                }
            }
        }

        // 캡처가 준비됬는지 확인하고 준비됬으면 바로 캡처 함수 호출 | 안됐으면 준비 함수 호출
        private fun capturePicture(result: CaptureResult) {
            Log.d("detectionResult", "4. capturePicture")

//            Log.d("렌즈 초점 결과", "capturePicture 1")
            val afState = result.get(CaptureResult.CONTROL_AF_STATE)
            // CONTROL_AF_STATE 키에 해당되는 것이 없다
            Log.d("detectionResult", "capturePicture : $afState")

            if (afState == null ) {
//                Log.d("렌즈 초점 결과", "capturePicture 2")
                // 캡처 함수
                captureStillPicture(null)
            } else if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
                || afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED || afState == CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN
                || wantCameraDirection == CaptureRequest.LENS_FACING_FRONT && afState == CaptureRequest.CONTROL_AF_STATE_INACTIVE
            ) {
                // CONTROL_AE_STATE can be null on some devices
                val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                    state = STATE_PICTURE_TAKEN
                    // 캡처 함수
                    captureStillPicture(null)
                } else {
                    // 준비 함수
                    runPrecaptureSequence()
                }
            }
        }
    }

    /**
     * ImageReader.OnImageAvailableListener
     */
    private val onImageAvailableListener = ImageReader.OnImageAvailableListener {
        imageSaver = ImageSaver(it.acquireNextImage(), previewByteArrayList)
        backgroundHandler?.post(imageSaver!!)
    }

    /**
     * 변수 모음
     */

    private var PICTURE_SIZE = 1

    var objectDetectionModule: ObjectDetectionModule

    private lateinit var cameraId: String

    private var captureSession: CameraCaptureSession? = null

    private var cameraDevice: CameraDevice? = null

    private lateinit var previewSize: Size

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var imageReader: ImageReader? = null
    private var imageSaver: ImageSaver? = null

    private lateinit var previewRequestBuilder: CaptureRequest.Builder
    private lateinit var previewRequest: CaptureRequest

    private var state = STATE_PREVIEW

    private val cameraOpenCloseLock = Semaphore(1)

    private var flashSupported = false

    private var sensorOrientation = 0

    private var minimumFocusDistance: Float = 0f

    var isDetectionChecked = false

    var wantCameraDirection = 0


    companion object {

        /**
         * Conversion from screen rotation to JPEG orientation.
         */
        private val ORIENTATIONS = SparseIntArray()
        private val FRAGMENT_DIALOG = "dialog"

        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }

        /**
         * Tag for the [Log].
         */
        private val TAG = "Camera2BasicFragment"

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

        /**
         * Max preview width that is guaranteed by Camera2 API
         */
        private val MAX_PREVIEW_WIDTH = 1920

        /**
         * Max preview height that is guaranteed by Camera2 API
         */
        private val MAX_PREVIEW_HEIGHT = 1080

        /**
         * 권한 체크
         */
        private const val REQUEST_PERMISSION = 123
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }

    init {
        objectDetectionModule = ObjectDetectionModule(context)

        // 메인 스레드를 방해하지 않기 위해 카메라 작업은 새로운 스레드 제작 후 해당 스레드에서 실행
        startBackgroundThread()

        wantCameraDirection = CameraCharacteristics.LENS_FACING_BACK
    }

    fun startCamera() {
        // texture 사용 가능한지 확인
        if (textureView.isAvailable) {
            // 카메라 열기
            openCamera(textureView.width, textureView.height)
        } else {
            textureView.surfaceTextureListener = surfaceTextureListener
        }
    }

    /**
     * distanceFocusPictures(pictureSize: Int)
     */
    fun distanceFocusPictures(pictureSize: Int) {
        PICTURE_SIZE = pictureSize

        Log.d("렌즈 초점 결과", "distanceBurstBtn on Click")
        val distanceUnit = minimumFocusDistance / 10f

        val queue = ArrayDeque<Float>()
        for (i in 10 downTo 1) {
            queue.add(distanceUnit * i)
        }

        setFocusDistance(queue)
    }

    /**
     * focusDetectionPictures()
     *      객체별 초점 촬영을 하고자할때 호출하는 함수로
     *      현재 객체 감지를 정지 시키고, 감지된 객체 정보를 하나씩 얻어와 초점을 맞춘뒤 촬영한다.
     *      더 이상 촬영할 객체가 없다면 중지한다.
     */



    fun focusDetectionPictures() {
        Log.d("detectionResult", "2. focusDetectionPictures")
        val detectionResult = objectDetectionModule.getDetectionResult()

        PICTURE_SIZE = 1
        Log.d("detectionResult", "2. ${detectionResult}")
        if (detectionResult != null) {
            val boundingBox = detectionResult.boundingBox
            val halfTouchWidth = (boundingBox.right - max(boundingBox.left, 0f)) / 2
            val halfTouchHeight = (boundingBox.bottom - max(boundingBox.top, 0f)) / 2

            // 해당 객체에 초점 맞추기
            setTouchPointDistanceChange(
                max(max(boundingBox.left, 0f) + halfTouchWidth, 0F),
                max(max(boundingBox.top, 0f) + halfTouchHeight, 0F),
                halfTouchWidth.toInt(), halfTouchHeight.toInt()
            )

//            Toast.makeText(context, "${detectionResult.text}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 메인 스레드를 방해하지 않기 위해 카메라 작업은 새로운 스레드 제작 후 해당 스레드에서 실행
     */
    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = backgroundThread?.looper?.let { Handler(it) }
    }


    /**
     * 카메라 열기
     */
    private fun openCamera(width: Int, height: Int) {

        val permission = ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
        val permissionAudio =
            ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)

        if (permission != PackageManager.PERMISSION_GRANTED || permissionAudio != PackageManager.PERMISSION_GRANTED) {
            checkPermissions()
            return
        }

        setUpCameraOutputs(width, height)
        configureTransform(width, height)

        val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            // Wait for camera to open - 2.5 seconds is sufficient
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            manager.openCamera(cameraId, stateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.", e)
        }

    }

    /**
     * 권한 체크
     */
    private fun checkPermissions() {
        //거절되었거나 아직 수락하지 않은 권한(퍼미션)을 저장할 문자열 배열 리스트
        var rejectedPermissionList = ArrayList<String>()

        //필요한 퍼미션들을 하나씩 끄집어내서 현재 권한을 받았는지 체크
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                //만약 권한이 없다면 rejectedPermissionList에 추가
                rejectedPermissionList.add(permission)
            }
        }
        //거절된 퍼미션이 있다면...
        if (rejectedPermissionList.isNotEmpty()) {
            //권한 요청!
            val array = arrayOfNulls<String>(rejectedPermissionList.size)
            ActivityCompat.requestPermissions(
                activity, rejectedPermissionList.toArray(array),
                REQUEST_PERMISSION
            )
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
     * 카메라 설정 및 카메라 관련 멤버 변수 초기화
     */
    private fun setUpCameraOutputs(width: Int, height: Int) {
        // 카메라 매니저 얻기 (사용가능한 카메라 장치들 얻어오기)
        val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            // 카메라 중에 조건에 맞는 거 하나를 찾고 return (첫번째부터 확인 후, 조건이 맞지 않을 continue)
            for (cameraId in manager.cameraIdList) {
                // 카메라 정보 알아내기
                val characteristics = manager.getCameraCharacteristics(cameraId)

                // 렌즈 정보 알아낸 후, 후면 카메라가 아닐 시 continue (이를 통해 처음 켜지는 카메라는 무조건 적으로 후면 카메라)
                val cameraDirection = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (cameraDirection == null || cameraDirection != wantCameraDirection) {
                    continue
                }

                // 스트림 구성 맵이 null인 경우 continue =>
                val map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                ) ?: continue

                // 이미지 해상도 및 포맷 설정 -> 가장 높은(좋은) 해상도 선택
                val largest = Collections.max(
                    Arrays.asList(*map.getOutputSizes(ImageFormat.JPEG)), CompareSizesByArea()
                )
                // 선택된 해상도로 ImageReader 설정 -> JPEG으로, width, height는 가장 크게
                //TODO: MaxImages : 최대 저장 개수 알아보기
                imageReader = ImageReader.newInstance(
                    largest.width, largest.height,
                    ImageFormat.JPEG, /*maxImages*/ 10
                ).apply {
                    setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)
                }

                // 디바이스에 따른 센서 방향 고려해서 preview 크기 결정
                val displayRotation = activity.windowManager?.defaultDisplay?.rotation

                sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
                val swappedDimensions = areDimensionsSwapped(displayRotation!!)

                val displaySize = Point()
                activity.windowManager?.defaultDisplay?.getSize(displaySize)
                val rotatedPreviewWidth = if (swappedDimensions) height else width
                val rotatedPreviewHeight = if (swappedDimensions) width else height
                var maxPreviewWidth = if (swappedDimensions) displaySize.y else displaySize.x
                var maxPreviewHeight = if (swappedDimensions) displaySize.x else displaySize.y

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) maxPreviewWidth = MAX_PREVIEW_WIDTH
                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) maxPreviewHeight = MAX_PREVIEW_HEIGHT

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                previewSize = chooseOptimalSize(
                    map.getOutputSizes(SurfaceTexture::class.java),
                    rotatedPreviewWidth, rotatedPreviewHeight,
                    maxPreviewWidth, maxPreviewHeight,
                    largest
                )

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                if (activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    textureView.setAspectRatio(previewSize.width, previewSize.height)
                } else {
                    textureView.setAspectRatio(previewSize.height, previewSize.width)
                }

                // Check if the flash is supported.
                flashSupported =
                    characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true

                this.cameraId = cameraId

                // 렌즈 LENS_INFO_MINIMUM_FOCUS_DISTANCE 값 알아오기 (최대값 = 변수 등록) : 최대 초점 거리
                minimumFocusDistance =
                    characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)!!

                return
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        } catch (e: NullPointerException) {
//            ErrorDialog.newInstance(getString(R.string.camera_error))
//                .show(childFragmentManager, FRAGMENT_DIALOG)
        }
    }

    /**
     * 카메라 회전에 관한 함수
     */
    private fun areDimensionsSwapped(displayRotation: Int): Boolean {
        var swappedDimensions = false
        when (displayRotation) {
            Surface.ROTATION_0, Surface.ROTATION_180 -> {
                if (sensorOrientation == 90 || sensorOrientation == 270) {
                    swappedDimensions = true
                }
            }
            Surface.ROTATION_90, Surface.ROTATION_270 -> {
                if (sensorOrientation == 0 || sensorOrientation == 180) {
                    swappedDimensions = true
                }
            }
            else -> {
                Log.e(TAG, "Display rotation is invalid: $displayRotation")
            }
        }
        return swappedDimensions
    }

    private fun chooseOptimalSize(
        choices: Array<Size>,
        textureViewWidth: Int,
        textureViewHeight: Int,
        maxWidth: Int,
        maxHeight: Int,
        aspectRatio: Size,
    ): Size {

        // Collect the supported resolutions that are at least as big as the preview Surface
        val bigEnough = ArrayList<Size>()
        // Collect the supported resolutions that are smaller than the preview Surface
        val notBigEnough = ArrayList<Size>()
        val w = aspectRatio.width
        val h = aspectRatio.height
        for (option in choices) {
            if (option.width <= maxWidth && option.height <= maxHeight &&
                option.height == option.width * h / w
            ) {
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

    /**
     * 카메라 회전 설정 및 textureView 설정
     */
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
            val scale = max(
                viewHeight.toFloat() / previewSize.height,
                viewWidth.toFloat() / previewSize.width
            )
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

    /**
     * 카메라 프리뷰 생성
     */
    private fun createCameraPreviewSession() {
        try {
            val texture = textureView.surfaceTexture

            // We configure the size of default buffer to be the size of camera preview we want.
            texture?.setDefaultBufferSize(previewSize.width, previewSize.height)

            // This is the output Surface we need to start preview.
            val surface = Surface(texture)

            // We set up a CaptureRequest.Builder with the output Surface.
            previewRequestBuilder = cameraDevice!!.createCaptureRequest(
                CameraDevice.TEMPLATE_PREVIEW
            )
            previewRequestBuilder.addTarget(surface)

            // Here, we create a CameraCaptureSession for camera preview.
            cameraDevice?.createCaptureSession(
                Arrays.asList(surface, imageReader?.surface),
                object : CameraCaptureSession.StateCallback() {

                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        // The camera is already closed
                        if (cameraDevice == null) return

                        // When the session is ready, we start displaying the preview.
                        captureSession = cameraCaptureSession
                        try {
                            // Auto focus should be continuous for camera preview.
                            previewRequestBuilder.set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                            )
                            // Flash is automatically enabled when necessary.
                            setAutoFlash(previewRequestBuilder)

                            // Finally, we start displaying the camera preview.
                            previewRequest = previewRequestBuilder.build()
                            captureSession?.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler)
                        } catch (e: CameraAccessException) {
                            Log.e(TAG, e.toString())
                        }

                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
//                        activity.showToast("Failed")
                    }
                }, null
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }

    }

    /**
     * 사진 촬영 전, 초점 고정시키기 위해 lock 거는 함수
     */
    fun lockFocus(pictureSize: Int) {
        Log.d("detectionResult", "4. lockFocus")

        PICTURE_SIZE = pictureSize

        try {
            Log.d(
                "렌즈 초점 결과",
                "lockFocus : " + previewRequestBuilder.get(CaptureRequest.CONTROL_AF_MODE)
                    .toString()
            )
            if (previewRequestBuilder.get(CaptureRequest.CONTROL_AF_MODE) != CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE) {
                previewRequestBuilder.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
            }
            // This is how to tell the camera to lock focus.
            previewRequestBuilder.set(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_START
            )
            // Tell #captureCallback to wait for the lock.
            state = STATE_WAITING_LOCK
            captureSession?.capture(previewRequestBuilder.build(), captureCallback, backgroundHandler)


        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }
    }

    /**
     * 사진 촬영 후, 초점 고정시켰던 lock 푸는 함수
     */
    private fun unlockFocus() {
        try {
            // Reset the auto-focus trigger
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL)
            setAutoFlash(previewRequestBuilder)
            captureSession?.capture(previewRequestBuilder.build(), captureCallback, backgroundHandler)
            // After this, the camera will go back to the normal state of preview.
            state = STATE_PREVIEW
            captureSession?.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler)
        } catch (e: CameraAccessException) {

        }
    }

    /**
     * 실제 사진 촬영을 하는 함수 (이미지 캡처)
     */
    private fun captureStillPicture(value: ArrayDeque<Float>?) {
        Log.d("detectionResult", "5. captureStillPicture")
        try {
            if (activity == null || cameraDevice == null) return
            val rotation = activity.windowManager?.defaultDisplay?.rotation

            // This is the CaptureRequest.Builder that we use to take a picture.
            val captureBuilder = cameraDevice?.createCaptureRequest(
                CameraDevice.TEMPLATE_STILL_CAPTURE
            )?.apply {
                addTarget(imageReader?.surface!!)

                set(
                    CaptureRequest.JPEG_ORIENTATION,
                    (ORIENTATIONS.get(rotation!!) + sensorOrientation + 270) % 360
                )

                // Use the same AE and AF modes as the preview.
                if (value != null) {
                    set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                } else {
                    set(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                    )
                }
            }?.also { setAutoFlash(it) }

            val captureCallback = object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult,
                ) {

                    val distanceResult = result.get(CaptureResult.LENS_FOCUS_DISTANCE)

//                    Toast.makeText(requireContext(), "렌즈 초점 결과: " +distanceResult.toString(), Toast.LENGTH_SHORT).show()
                    Log.d("렌즈 초점 결과", distanceResult.toString())
                    if (value != null && distanceResult == 10f) {
                        setAutoFocus()
                    } else {
                        unlockFocus()
                        Log.d("detectionResult", "5. isDetectionChecked : ${isDetectionChecked}")
                        if (isDetectionChecked) {
                            focusDetectionPictures()
                        }
                    }
                }
            }

            captureSession?.apply {
                stopRepeating()
                abortCaptures()

                val captureRequestList = mutableListOf<CaptureRequest>()

                // 자동 초점
                if (value == null) {
                    for (i in 0 until PICTURE_SIZE) {
                        captureBuilder?.let { captureRequestList.add(it.build()) }
                    }
                }

                // 수동 초점 (디스턴스 촬영)
                else {
                    while (value.isNotEmpty()) {
                        captureBuilder?.set(CaptureRequest.LENS_FOCUS_DISTANCE, value.poll())
                        captureBuilder?.let { captureRequestList.add(it.build()) }
                    }
                }

                // Capture the burst of images
                captureBurst(captureRequestList, captureCallback, null)
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }

    }

    /**
     * 사진 촬영 준비하는 함수 (precaputre 대기 위한 설정)
     */
    private fun runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            previewRequestBuilder.set(
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START
            )
            // Tell #captureCallback to wait for the precapture sequence to be set.
            state = STATE_WAITING_PRECAPTURE
            captureSession?.capture(
                previewRequestBuilder.build(), captureCallback,
                backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }
    }

    /**
     * 프리뷰 초점 수동으로 변경했을 때 마지막에 자동 초점 촬영으로 변경
     */
    fun setAutoFocus() {
        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        previewRequest = previewRequestBuilder.build()

        state = STATE_PREVIEW

        captureSession?.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler)
    }

    /**
     * 프리뷰 초점 수동으로 변경하고 초점 설정해서 촬영
     */
    private fun setFocusDistance(value: ArrayDeque<Float>) {

        state = STATE_PICTURE_TAKEN

        var focusDistanceValue = value.peek()
        if (focusDistanceValue > minimumFocusDistance) {
            focusDistanceValue = minimumFocusDistance
        }

        previewRequestBuilder.set(
            CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AF_MODE_OFF
        )
        previewRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, focusDistanceValue)

        val captureCallback = object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult,
            ) {

                // 렌즈 현재 상태 알아낼 수 있음
                val lensState = result.get(CaptureResult.LENS_STATE)

                // 렌즈가 정지된 상태입니다. 초점이 안정되어 있을 가능성이 높습니다.
                if (lensState != null && lensState == CaptureResult.LENS_STATE_STATIONARY) {
                    var distanceValue = result.get(CaptureResult.LENS_FOCUS_DISTANCE)
                    // 내가 지정한 바운더리 안에 있는지 확인
                    if (distanceValue != null && value.isNotEmpty() && distanceValue > value.peek() - 0.1f
                        && distanceValue < value.peek() + 0.1f
                    ) {
                        Log.d(
                            "렌즈 초점 거리",
                            "렌즈 초점거리 ${result.get(CaptureResult.LENS_FOCUS_DISTANCE)}"
                        )
                        captureStillPicture(value)
                    }
                }
            }
        }

        previewRequest = previewRequestBuilder.build()
        captureSession?.setRepeatingRequest(
            previewRequest,
            captureCallback, backgroundHandler
        )
    }

    /**
     * 해당 위치에 초점을 맞춰주는 함수
     */
    fun setTouchPointDistanceChange(x: Float, y: Float, halfTouchWidth: Int, halfTouchHeight: Int) {
        var isCaptured = false

        val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        // 카메라 정보 알아내기
        val characteristics = manager.getCameraCharacteristics(cameraId)

        val sensorArraySize: Rect? = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)

        val newX = max((y * (sensorArraySize!!.width() / textureView.height) - halfTouchWidth).toInt(), 0)
        val newY = max(((textureView.width - x) * (sensorArraySize.height() / textureView.width) - halfTouchHeight).toInt(), 0)

        val focusAreaTouch = MeteringRectangle(
            newX, newY,
            halfTouchWidth * 2,
            halfTouchHeight * 2,
            MeteringRectangle.METERING_WEIGHT_MAX - 1
        )

        val captureCallback = object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult,
            ) {
                //the focus trigger is complete -
                //resume repeating (preview surface will get frames), clear AF trigger
                if (request.tag == "FOCUS_TAG" && !isCaptured)  {
                    isCaptured = true
                    previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, null)
                    captureSession?.setRepeatingRequest(previewRequestBuilder.build(), null, null)
                    if(objectDetectionModule.getIsDetectionStop()) {
                        Log.d("detectionResult", "3. setTouchPointDistanceChange onCaptureCompleted")
                        lockFocus(1)
                    }
                }
            }
        }

        // 모든 캡처 정보를 삭제
        captureSession?.stopRepeating()

        // 초점 변경을 위한 AF 모드 off
        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL)
        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
        captureSession?.capture(previewRequestBuilder.build(), captureCallback, backgroundHandler)

        // 터치 위치로 초점 변경
        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(focusAreaTouch))
        // 터치 위치에 맞춰서 노출 정도 변경
        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, arrayOf(focusAreaTouch))


        // AF 모드 다시 설정 - 안하면 초점 변경 안됨
        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)
        previewRequestBuilder.setTag("FOCUS_TAG"); //we'll capture this later for resuming the preview

        //then we ask for a single request (not repeating!)
        captureSession?.capture(previewRequestBuilder.build(), captureCallback, backgroundHandler)

    }

    private fun setAutoFlash(requestBuilder: CaptureRequest.Builder) {
        if (flashSupported) {
            requestBuilder.set(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
        }
    }

}


