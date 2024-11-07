package com.goldenratio.onepic.CameraModule.Camera2Module

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.hardware.camera2.*
import android.hardware.camera2.params.MeteringRectangle
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.ImageView
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
import kotlin.math.max


class Camera2Module(
    private val activity: CameraEditorActivity,
    private val context: Context,
    private val textureView: AutoFitTexutreView,
    private val imageView: ImageView,
    var previewByteArrayList : MutableLiveData<ArrayList<ByteArray>>
) {

    var detectionBitmap :Bitmap? = null

    /**
     * TextureView의 사용 가능 여부와 이와 관련된 surface에 관해 호출되는 리스너
     */
    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {

        /**
         * TextureView의 SurfaceTexture가 사용 가능할 때 호출된다.
         *
         * @param texture TextureView의 SurfaceTexture
         * @param width SurfaceTexture의 너비
         * @param height SurfaceTexture의 높이
         */
        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            openCamera(width, height)
        }

        /**
         * SurfaceTexture의 버퍼 크기가 변경되었을 때 호출된다.
         *
         * @param texture TextureView의 SurfaceTexture
         * @param width SurfaceTexture의 너비
         * @param height SurfaceTexture의 높이
         */
        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
            configureTransform(width, height)
        }

        // surfaceTexture가 소멸되려 함
        /**
         * SurfaceTexture가 소멸 될 때 호출된다.
         *
         * @param texture TextureView의 SurfaceTexture
         * @return
         */
        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
            return true
        }

        /**
         * SurfaceTexture가 업데이트 될 때 호출된다.
         *
         * @param texture TextureView의 SurfaceTexture
         */
        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {
            if (isDetectionChecked) {
                setShowObjectPreView()
            }
        }

        /**
         * preview 이미지를 얻어와 객체 인식한 후 결과를 화면에 표시한다.
         */
        private fun setShowObjectPreView() {

            // 객체 인식 코드 호출
            val newBitmap = textureView.bitmap?.let {
                objectDetectionModule.runObjectDetection(it)
            }
            imageView.setImageBitmap(newBitmap)

            detectionBitmap?.recycle()
            detectionBitmap = newBitmap
        }
    }

    /**
     * 카메라 장치의 상태에 대한 업데이트를 수신할 때 호출되는 콜백 클래스
     */
    private val stateCallback = object : CameraDevice.StateCallback() {

        /**
         * 카메라 장치가 열렸을 때 호출된다.
         *
         * @param cameraDevice 열린 카메라 장치 정보
         */
        override fun onOpened(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            this@Camera2Module.cameraDevice = cameraDevice
            createCameraPreviewSession()
        }

        /**
         * 카메라 장치가 더 이상 사용할 수 없을 때 호출된다.
         *
         * @param cameraDevice 사용할 수 없는 카메라 장치 정보
         */
        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice.close()
            this@Camera2Module.cameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) { }
    }

    /**
     * 카메라 장치에 제출된 CaptureRequest의 진행률을 추척하기 위한 콜백 클래스
     */
    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {

        /**
         * 이미지 캡처가 부분적으로 진행되었울 때 호출된다.
         * 이미지 캡처의 일부 결과 사용할 수 있다.
         *
         * @param partialResult 일부 이미지 캡처 결과
         */
        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult,
        ) {
            process(partialResult)
        }

        /**
         * 이미지 캡처가 완전히 완료되고 모든 결과 메타데이터를 사용할 수 있을 때 호출된다.
         *
         * @param result 전체 이미지 캡처 결과
         */
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult,
        ) {
            process(result)
        }

        /**
         * 이미지 캡처 설정 결과로 호출되는 함수로, state 변수에 따른 이후 동작을 설정한다.
         *
         * STATE_PREVIEW : 아무것도 안함
         * STATE_WAITING_LOCK : capturePicture() 호출
         * STATE_WAITING_PRECAPTURE : 노출 상태를 확인하고, 현재 노출이 잡히고 있는 상태라면, state 변경
         * STATE_WAITING_NON_PRECAPTURE : 노출 상태를 확인하고, 현재 노출이 잡힌 상태라면, state 변경 후 captureStillPicture() 함수 호출
         *
         * @param result 이미지 캡처 결과
         */
        private fun process(result: CaptureResult) {
            when (state) {
                // 프리뷰 상태
                STATE_PREVIEW -> {
//                    Log.d("check preview","${previewRequestBuilder.get(CaptureRequest.CONTROL_AF_MODE)}")
                } // Do nothing when the camera preview is working normally.
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

//                  // CONTROL_AE_STATE can be null on some devices
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)

                    Log.d(
                        "detectionResult",
                        "4. process : STATE_WAITING_NON_PRECAPTURE - ${aeState}"
                    )
                    if (aeState == null || isDetectionChecked || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
//                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        state = STATE_PICTURE_TAKEN
                        captureStillPicture(null)
                    }
                }
            }
        }

        /**
         * 이미지 캡처가 가능한지 초점 상태와 노출 상태를 확인 후 이후 동작을 설정한다.
         *
         * 노출과 초점 상태가 캡처 가능한 상태인 경우, captureStillPicture() 함수 호출
         * 노출 상태가 캡처 가능하지 않은 상태인 경우, runPrecaptureSequence() 함수 호출
         *
         * @param result 이미지 캡처 결과
         */
        private fun capturePicture(result: CaptureResult) {
            Log.d("detectionResult", "4. capturePicture")

//            Log.d("렌즈 초점 결과", "capturePicture 1")
            val afState = result.get(CaptureResult.CONTROL_AF_STATE)
            // CONTROL_AF_STATE 키에 해당되는 것이 없다
            Log.d("detectionResult", "capturePicture : $afState")

            if (afState == null) {
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
     * 사진이 촬영돼 촬영 이미지 결과가 전달되는 함수로, 사진 저장을 도와주는 ImageSaver 제작한다.
     */
    private val onImageAvailableListener = ImageReader.OnImageAvailableListener {
        imageSaver = ImageSaver(it.acquireNextImage(), previewByteArrayList)
        backgroundHandler?.post(imageSaver!!)
    }

    /** 변수 **/
    var objectDetectionModule: ObjectDetectionModule = ObjectDetectionModule(context)

    private var state = STATE_PREVIEW
    private var PICTURE_SIZE = 1
    private lateinit var previewSize: Size

    private lateinit var previewRequestBuilder: CaptureRequest.Builder
    private lateinit var previewRequest: CaptureRequest

    private lateinit var cameraId: String
    private var captureSession: CameraCaptureSession? = null
    private var cameraDevice: CameraDevice? = null

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var imageReader: ImageReader? = null
    private var imageSaver: ImageSaver? = null

    private val cameraOpenCloseLock = Semaphore(1)
    private var flashSupported = false

    private var sensorOrientation = 0

    // 최대 초점 거리 값
    private var minimumFocusDistance: Float = 0f

    // 객체 인식
    var isDetectionChecked = false

    // 원하는 카메라 방향 (전면, 후면)
    var wantCameraDirection = 1

    companion object {
        /**
         * 화면 회전에서 JPEG 방향으로 변환
         */
        private val ORIENTATIONS = SparseIntArray()

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
         * Camera state: 카메라 프리뷰를 보여주고 있는 상태
         */
        private val STATE_PREVIEW = 0

        /**
         * Camera state: 포커스가 잠기기를 기다리고 있는 상태
         */
        private val STATE_WAITING_LOCK = 1

        /**
         * Camera state: 노출이 PreCaputure가 되길 기다리는 상태
         */
        private val STATE_WAITING_PRECAPTURE = 2

        /**
         * Camera state: 노출이 PreCaputure가 아닌 다른 상태가 되길 기다리는 상태
         */
        private val STATE_WAITING_NON_PRECAPTURE = 3

        /**
         * Camera state: 사진이 찍힌 상태
         */
        private val STATE_PICTURE_TAKEN = 4

        /**
         * Camera2 API가 보장하는 최대 프리뷰 너비
         */
        private val MAX_PREVIEW_WIDTH = 1920

        /**
         * Camera2 API가 보장하는 최대 프리뷰 높이
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

    /**
     * 메인 스레드를 방해하지 않기 위해 카메라 작업을 위한 새로운 스레드 제작 후 해당 스레드에서 실행하도록 설정한다.
     */
    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = backgroundThread?.looper?.let { Handler(it) }
    }

    private fun stopBackgroundThread() {
        // 백그라운드 스레드를 중지하고 해제합니다.
        backgroundThread?.quitSafely()
        backgroundThread = null

        // 백그라운드 핸들러를 해제합니다.
        backgroundHandler?.removeCallbacksAndMessages(null)
        backgroundHandler = null
    }

    /**
     * 카메라를 사용하기 위해 필요한 설정(카메라 열기 및 리스너 설정)을 한다.
     */
    fun startCamera() {
        // texture 사용 가능한지 확인
        if (textureView.isAvailable) {
            // 가능하면, 카메라 열기
            openCamera(textureView.width, textureView.height)
        } else {
            // 가능하지 않으면, 리스너 설정
            textureView.surfaceTextureListener = surfaceTextureListener
        }
    }

    /**
     * 카메라를 열고, 너비 높이에 맞춰서 카메라 화면을 설정([CameraDevice] 설정)한다.
     *
     * @param width 카메라 너비
     * @param height 카메라 높이
     */
    private fun openCamera(width: Int, height: Int) {
        // 메인 스레드를 방해하지 않기 위해 카메라 작업은 새로운 스레드 제작 후 해당 스레드에서 실행
        startBackgroundThread()

        // 카메라 권한 확인
        val permission = ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
        val permissionAudio =
            ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)

        if (permission != PackageManager.PERMISSION_GRANTED || permissionAudio != PackageManager.PERMISSION_GRANTED) {
            checkPermissions()
            return
        }

        // 카메라 얻어오기 및 설정
        setUpCameraOutputs(width, height)
        // 카메라 회전 설정
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
     * TextureView의 변환 매트릭스를 구성하여 카메라 프리뷰의 회전 및 크기를 조정한다.
     *
     * @param viewWidth TextureView의 너비
     * @param viewHeight TextureView의 높이
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
     * 카메라 권한을 확인한다.
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
     * 현재 사용중인 카메라를 닫는다.
     */
    fun closeCamera() {
        stopBackgroundThread()

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
     * 사용할 [CameraDevice]를 알아내고, 카메라 및 카메라 관련 멤버 변수를 설정한다.
     *
     * @param width 카메라 너비
     * @param height 카메라 높이
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
     * 카메라 회전을 화면에 맞춰 설정한다.
     *
     * @param displayRotation 화면 회전 정보
     * @return 화면 회전 설정 여부
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


    /**
     * 카메라 프리뷰의 최적 해상도를 선택한다.
     *
     * @param choices 지원하는 해상도 목록
     * @param textureViewWidth TextureView의 너비
     * @param textureViewHeight TextureView의 높이
     * @param maxWidth 선택 가능한 최대 너비
     * @param maxHeight 선택 가능한 최대 높이
     * @param aspectRatio 원하는 화면 비율
     * @return 선택한 최적 해상도
     */
    private fun chooseOptimalSize(
        choices: Array<Size>,
        textureViewWidth: Int, textureViewHeight: Int,
        maxWidth: Int, maxHeight: Int, aspectRatio: Size,
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
        // largest of those not big eugh.
        return if (bigEnough.size > 0) {
            Collections.min(bigEnough, CompareSizesByArea())
        } else if (notBigEnough.size > 0) {
            Collections.max(notBigEnough, CompareSizesByArea())
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size")
            choices[0]
        }
    }

    /**
     * 카메라 프리뷰 세션을 생성하고 설정한다.
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
//                            setAutoFlash(previewRequestBuilder)

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
     * 사진 촬영 전, 초점을 고정시키기 위해 카메라 잠금을 설정한다.
     *
     * @param pictureSize 촬영될 사진 개수
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
     * 사진 촬영 후, 초점 고정시켰던 카메라 잠금을 해제한다.
     */
    private fun unlockFocus() {
        try {
            // Reset the auto-focus trigger
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL)
//            setAutoFlash(previewRequestBuilder)
            captureSession?.capture(previewRequestBuilder.build(), captureCallback, backgroundHandler)
            // After this, the camera will go back to the normal state of preview.
            state = STATE_PREVIEW
            captureSession?.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler)
        } catch (e: CameraAccessException) {

        }
    }

    /**
     * 사진을 촬영한다. (이미지 캡처)
     *
     * @param value 설정하고자하는 카메라 초점거리들(거리별 다초점 촬영에서만 사용)
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
            }
//            }?.also { setAutoFlash(it) }

            val captureCallback = object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult,
                ) {

                    val distanceResult = result.get(CaptureResult.LENS_FOCUS_DISTANCE)

//                    Toast.makeText(requireContext(), "렌즈 초점 결과: " +distanceResult.toString(), Toast.LENGTH_SHORT).show()
                    Log.d("렌즈 초점 결과", distanceResult.toString())
                    if (value != null && distanceResult!! >= 0f && distanceResult <= 0.3f) {
                        setAutoFocus()
                    } else {
                        unlockFocus()
                        Log.d("detectionResult", "5. isDetectionChecked : ${isDetectionChecked}")
                        if (isDetectionChecked) {
                            focusObjectDetectionPictures()
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
     * 사진 촬영 준비하는 함수로, PreCapture를 실행하여 미터링 및 초점을 설정합니다.
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
     * 프리뷰 초점을 자동 초점으로 설정한다.
     */
    fun setAutoFocus() {
        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        previewRequest = previewRequestBuilder.build()

        Log.d("check preview", "setAutoFocus")

        state = STATE_PREVIEW

        captureSession?.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler)
    }

    /**
     *  프리뷰에서 원하는 위치에 초점을 맞춰준다.
     *
     * @param x 초점이 맞길 원하는 중심점 x
     * @param y 초점이 맞길 원하는 중심점 y
     * @param halfTouchWidth 중심점으로부터 초점이 맞길 원하는 너비
     * @param halfTouchHeight 중심점으로부터 초점이 맞길 원하는 높이
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
                    // 렌즈 현재 상태 알아낼 수 있음
//                    val lensState = result.get(CaptureResult.LENS_STATE)

                    // 렌즈가 정지된 상태입니다. 초점이 안정되어 있을 가능성이 높습니다.
//                    if (lensState != null && lensState == CaptureResult.LENS_STATE_STATIONARY) {
                    previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, null)
                    captureSession?.setRepeatingRequest(previewRequestBuilder.build(), null, null)
                    if(objectDetectionModule.getIsDetectionStop() && !isCaptured) {
                        isCaptured = true
                        Log.d("detectionResult", "3. setTouchPointDistanceChange onCaptureCompleted")
                        lockFocus(1)
                    }
//                    }
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

    /**
     * (플래시가 지원되는 경우) 카메라 자동 플래시 모드를 설정한다.
     *
     * @param requestBuilder CaptureRequest.Builder 인스턴스
     */
    private fun setAutoFlash(requestBuilder: CaptureRequest.Builder) {
        if (flashSupported) {
            requestBuilder.set(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
        }
    }

    /**
     * 거리별 다초점 촬영 함수로, 입력된 크기만큼 초점거리 값을 제작해 설정된 초점 값으로 초점을 맞춘다.
     *
     * @param pictureSize 거리별 다초점 촬영으로 촬영될 사진 개수
     */
    fun distanceFocusPictures(pictureSize: Int) {
        PICTURE_SIZE = pictureSize

        Log.d("check preview", "distanceBurstBtn on Click")

        // 사이즈 맞춰서 초점거리 값 제작
        val distanceUnit = minimumFocusDistance / (pictureSize-1)
        val queue = ArrayDeque<Float>()
        for (i in (pictureSize-1) downTo 0) {
            queue.add(distanceUnit * i)
        }

        // 설정된 초점 거리 값으로 초점 맞추기
        setFocusDistance(queue)
    }

    /**
     * 카메라의 초점을 수동 초점으로 변경하고, 전달받은 값의 첫번째 초점거리로 초점거리를 설정한다.
     *
     * @param value 설정하고자하는 카메라 초점거리들 (거리별 다초점 촬영에서만 사용)
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

        Log.d("check preview", "1")

        val captureCallback = object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult,
            ) {
                // 렌즈 현재 상태 알아낼 수 있음
                val lensState = result.get(CaptureResult.LENS_STATE)

                Log.d("check preview", "1 lensState")

                // 렌즈가 정지된 상태입니다. 초점이 안정되어 있을 가능성이 높습니다.
                if (lensState != null && lensState == CaptureResult.LENS_STATE_STATIONARY) {
                    val distanceValue = result.get(CaptureResult.LENS_FOCUS_DISTANCE)


                    Log.d("check preview", "distanceValue = $distanceValue")

                    // 내가 지정한 바운더리 안에 있는지 확인
                    if (distanceValue != null && value.isNotEmpty()
//                        && distanceValue > value.peek() - 0.1f && distanceValue < value.peek() + 0.1f
                    ) {
                        Log.d("check preview", "렌즈 초점거리 ${result.get(CaptureResult.LENS_FOCUS_DISTANCE)}")
                        captureStillPicture(value)
                    }
                }
            }
        }

        previewRequest = previewRequestBuilder.build()
        captureSession?.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler)
    }

    /**
     *  객체별 초점 촬영에서 호출하는 함수로, 현재 객체 감지를 정지 시키고 감지된 객체 정보를 하나씩 얻어와 초점을 맞춘 뒤 촬영한다.
     *  더 이상 촬영할 객체가 없다면 중지한다.
     */
    fun focusObjectDetectionPictures() {
        Log.d("detectionResult", "2. focusDetectionPictures")
        val detectionResult = objectDetectionModule.getDetectionResult()

        // 한 장씩 촬영
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
//            150, 150
            )
        }
    }

}