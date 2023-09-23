package com.goldenratio.onepic.EditModule.Fragment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.graphics.toRectF
import androidx.core.view.isEmpty
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.goldenratio.onepic.*
import com.goldenratio.onepic.AllinJPEGModule.Content.ContentAttribute
import com.goldenratio.onepic.AllinJPEGModule.Content.ImageContent
import com.goldenratio.onepic.AllinJPEGModule.Content.Picture
import com.goldenratio.onepic.EditModule.FaceDetectionModule
import com.goldenratio.onepic.databinding.FragmentFaceBlendingBinding
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

open class FaceBlendingFragment : Fragment(R.layout.fragment_face_blending) {

    private lateinit var binding: FragmentFaceBlendingBinding

    protected var imageToolModule: ImageToolModule = ImageToolModule()
    protected lateinit var faceDetectionModule: FaceDetectionModule

    protected lateinit var selectPicture: Picture

    private lateinit var originalSelectBitmap: Bitmap
    protected lateinit var selectBitmap: Bitmap
    private var preSelectBitmap: Bitmap? = null
    protected var newImage: Bitmap? = null

    protected var changeFaceStartX = 0
    protected var changeFaceStartY = 0

    protected var bitmapList: ArrayList<Bitmap> = arrayListOf()

    protected val cropBitmapList: ArrayList<Bitmap> = arrayListOf()

    protected val jpegViewModel by activityViewModels<JpegViewModel>()
    protected lateinit var imageContent: ImageContent
    lateinit var fragment: Fragment

    private var infoLevel = MutableLiveData<InfoLevel>()
    var isInfoViewed = true

    protected var selectFaceRect: Rect? = null
    protected var isSelected = false

    private var mainSubView: View? = null

    private enum class InfoLevel {
        EditFaceSelect,
        ChangeFaceSelect,
        ArrowCheck,
        BasicLevelEnd
    }

    private enum class LoadingText {
        Save,
        Change,
        AutoBlending
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        bundle: Bundle?
    ): View {
        // 뷰 바인딩 설정
        binding = FragmentFaceBlendingBinding.inflate(inflater, container, false)
        showProgressBar(true, LoadingText.AutoBlending)

        settingFaceBlendingFragment()

        setClickEvent()

        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.post {
            val joystickView = binding.joystick
            joystickView.setOnMoveListener({ angle, strength ->
                Log.d("joystick", "angle : ${angle}" + "strength : ${strength}")
                moveCropFace(angle, strength) }, 200)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        faceDetectionModule.deleteModelCoroutine()
    }

    override fun onStop() {
        super.onStop()
        faceDetectionModule.deleteModelCoroutine()
    }


    /**
     *  변수를 초기화한다.
     */
    private fun settingFaceBlendingFragment() {
        infoLevel.observe(viewLifecycleOwner) { _ ->
            infoTextView()
        }

        faceDetectionModule = jpegViewModel.faceDetectionModule
        imageContent = jpegViewModel.jpegAiContainer.value?.imageContent!!

        while(!imageContent.checkPictureList) {}

        // main Picture의 byteArray를 bitmap 제작
        selectPicture = jpegViewModel.selectedSubImage!!

        CoroutineScope(Dispatchers.IO).launch {
            // 메인 이미지 임시 설정
            withContext(Dispatchers.Main) {
                Glide.with(binding.mainView)
                    .load(imageContent.getJpegBytes(selectPicture))
                    .into(binding.mainView)
            }
            if(faceDetectionModule.checkFaceDetectionCall && faceDetectionModule.getCheckFaceDetection()) {
                Thread.sleep(3500)
            }

            // Blending 가능한 연속 사진 속성의 picture list 얻음
            bitmapList = imageContent.getBitmapList(ContentAttribute.edited)

            faceDetectionModule.allFaceDetection(bitmapList)
            selectBitmap = faceDetectionModule.autoBestFaceChange(bitmapList, jpegViewModel.getSelectedSubImageIndex())

            // faceDetection 하고 결과가 표시된 사진을 받아 imaveView에 띄우기
            setMainImageBoundingBox()
            withContext(Dispatchers.Main) {
                imageToolModule.fadeIn.start()
            }
        }

        CoroutineScope(Dispatchers.Default).launch {
            val allBitmapList = imageContent.getBitmapList()
            val index = jpegViewModel.getSelectedSubImageIndex()
            if(allBitmapList.size <= index) {
                imageContent.resetBitmap()
                imageContent.setBitmapList()
                bitmapList = imageContent.getBitmapList()
            }
            val newMainBitmap = allBitmapList[index]
            if (newMainBitmap != null) {
                selectBitmap = newMainBitmap
            }
            originalSelectBitmap = selectBitmap

        }
        imageToolModule.settingAnimation(binding.successInfoConstraintLayout)
    }

    /**
     * 이벤트 처리를 설정한다.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setClickEvent() {
        // save btn 클릭 시
        binding.blendingSaveBtn.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                imageToolModule.showView(binding.infoDialogLayout, false)
                infoLevel.value = InfoLevel.EditFaceSelect
                showProgressBar(true, LoadingText.Save)
            }
            saveNewImage()
        }

        // close btn 클릭 시
        binding.blendingCloseBtn.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_fregemnt_to_editFragment)
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
        }

        // autoBlending 클릭시
        binding.autoBlendingBtn.setOnClickListener {

            CoroutineScope(Dispatchers.Main).launch {
                imageToolModule.showView(binding.infoDialogLayout, false)
                infoLevel.value = InfoLevel.EditFaceSelect
                isInfoViewed = false
            }

//            imageToolModule.showView(binding.progressBar, true)
            imageToolModule.showView(binding.arrowBar, false)
            imageToolModule.showView(binding.blendingMenuLayout, false)
            showProgressBar(true, LoadingText.AutoBlending)
            binding.candidateLayout.removeAllViews()

            if (bitmapList.size != 0) {
                CoroutineScope(Dispatchers.Default).launch {
                    faceDetectionModule.allFaceDetection(bitmapList)
                    Log.d("autoBlending", "1. selectedImage = ${jpegViewModel.getSelectedSubImageIndex()}")
                    selectBitmap = faceDetectionModule.autoBestFaceChange(bitmapList, jpegViewModel.getSelectedSubImageIndex())

                    setMainImageBoundingBox()
                    newImage = null
                    withContext(Dispatchers.Main) {
                        imageToolModule.fadeIn.start()
                    }
                    imageToolModule.showView(binding.blendingMenuLayout, true)
                    showProgressBar(false, null)

                }
            }
            else {
                imageToolModule.showView(binding.blendingMenuLayout, true)
                showProgressBar(false, null)
            }
        }

        // compare 버튼 클릭시
        binding.imageCompareBtn.setOnTouchListener { _, event ->
            CoroutineScope(Dispatchers.Main).launch {
                imageToolModule.showView(binding.infoDialogLayout, false)
            }
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    binding.mainView.setImageBitmap(originalSelectBitmap)
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_UP -> {
                    if (newImage != null)
                        binding.mainView.setImageBitmap(preSelectBitmap)
                    else
                        binding.mainView.setImageBitmap(selectBitmap)
                    return@setOnTouchListener true
                }
                else -> return@setOnTouchListener false
            }
        }

        // reset 버튼 클릭시
        binding.imageResetBtn.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                imageToolModule.showView(binding.infoDialogLayout, false)
            }
            binding.mainView.setImageBitmap(originalSelectBitmap)
            selectBitmap = originalSelectBitmap
            preSelectBitmap = null
            newImage = null
        }

        // info 확인
        binding.blendingInfoBtn.setOnClickListener {
            isInfoViewed = true
            imageToolModule.showView(binding.infoDialogLayout, true)
        }

        // info 삭제
        binding.dialogCloseBtn.setOnClickListener {
            isInfoViewed = false
            imageToolModule.showView(binding.infoDialogLayout, false)
        }

        infoLevel.value = InfoLevel.EditFaceSelect

        // 이미지 뷰 클릭 시
        binding.mainView.setOnTouchListener { _, event ->
            if (event!!.action == MotionEvent.ACTION_UP) {
                if (!isSelected) {
//                imageToolModule.showView(binding.progressBar, true)
                    showProgressBar(true, LoadingText.Change)
                    imageToolModule.showView(binding.blendingMenuLayout, false)

//                    if (preSelectBitmap != null) {
//                        selectBitmap = preSelectBitmap!!
//                        newImage = null
//                    }
                    // click 좌표를 bitmap에 해당하는 좌표로 변환
                    val touchPoint = imageToolModule.getBitmapClickPoint(
                        PointF(event.x, event.y),
                        binding.mainView
                    )
                    println("------- click point:$touchPoint")

                    if (touchPoint != null) {

                        CoroutineScope(Dispatchers.Default).launch {
                            // Click 좌표가 포함된 Bounding Box 얻음
                            while (!faceDetectionModule.getCheckFaceDetection()) {
                            }
                            val boundingBox = getBoundingBox(touchPoint)

                            if (boundingBox.size > 0) {
                                // Bounding Box로 이미지를 Crop한 후 보여줌
                                withContext(Dispatchers.Main) {
                                    cropImgAndView(boundingBox)
                                }
                            }
                        }
                    } else {
//                    imageToolModule.showView(binding.progressBar, false)
                        showProgressBar(false, null)
                    }
                }
            }
            return@setOnTouchListener true
        }

        binding.faceSaveBtn.setOnClickListener {
            isSelected = false

            if (preSelectBitmap != null) {
                selectBitmap = preSelectBitmap!!
                newImage = null
                preSelectBitmap = null
            }

            binding.mainView.setImageBitmap(selectBitmap)
            imageToolModule.showView(binding.faceBlendingMenuLayout, false)
            imageToolModule.showView(binding.blendingSaveBtn, true)
            imageToolModule.showView(binding.blendingCloseBtn, true)
            setMainImageBoundingBox()
        }

        binding.faceCancleBtn.setOnClickListener {
            isSelected = false

            newImage = null
            preSelectBitmap = null

            binding.mainView.setImageBitmap(selectBitmap)
            imageToolModule.showView(binding.faceBlendingMenuLayout, false)
            imageToolModule.showView(binding.blendingSaveBtn, true)
            imageToolModule.showView(binding.blendingCloseBtn, true)
            setMainImageBoundingBox()
        }
    }

    /**
     * 이미지를 저장한다.
     */
    private fun saveNewImage() {
        CoroutineScope(Dispatchers.Default).launch {
            if (preSelectBitmap != null) {
                selectBitmap = preSelectBitmap!!
                newImage = null
            }
            val allBytes = imageToolModule.bitmapToByteArray(selectBitmap, imageContent.getJpegBytes(selectPicture))

            val selectIndex = imageContent.pictureList.size

            // Picture 생성
            val newPicture = jpegViewModel.jpegAiContainer.value?.getPictureFromEditedBytes(allBytes)
            if (newPicture != null) {
                // 추가
                jpegViewModel.jpegAiContainer.value?.addPictureToImageContent(selectIndex, newPicture)
            }

            imageContent.addBitmapList(selectIndex, selectBitmap)
            jpegViewModel.setPictureByteList(imageContent.getJpegBytes(imageContent.pictureList[selectIndex]), selectIndex)

            jpegViewModel.selectedSubImage = imageContent.pictureList[selectIndex]

            withContext(Dispatchers.Main) {
                imageContent.checkBlending = true
                findNavController().navigate(R.id.action_fregemnt_to_editFragment)
            }
//            showProgressBar(false, null)
        }
    }

    /**
     * 선택된 이미지로 얼굴 감지 모델 실행 후, 감지된 얼굴이 표시한 후 화면에 출력한다.
     */
    open fun setMainImageBoundingBox() {

        if(infoLevel.value != InfoLevel.EditFaceSelect) {
            CoroutineScope(Dispatchers.Main).launch {
                infoLevel.value = InfoLevel.EditFaceSelect
                isInfoViewed = false

                imageToolModule.showView(binding.infoDialogLayout, false)
            }
        }

        imageToolModule.showView(binding.arrowBar, false)
        if (!binding.candidateLayout.isEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                withContext(Dispatchers.Main) {
                    binding.candidateLayout.removeAllViews()
                }
            }
        }

        CoroutineScope(Dispatchers.Default).launch {
            Log.d("checkPictureList", "!!!!!!!!!!!!!!!!!!! setMainImageBoundingBox")
            val faceResult = faceDetectionModule.getFaces(jpegViewModel.getSelectedSubImageIndex())
            Log.d("checkPictureList", "!!!!!!!!!!!!!!!!!!! end runFaceDetection")

            if (faceResult.size == 0) {
                withContext(Dispatchers.Main) {
                    try {
                        Toast.makeText(requireContext(), "사진에 얼굴이 존재하지 않습니다.", Toast.LENGTH_SHORT)
                            .show()
                        showProgressBar(false, null)
                        imageToolModule.showView(binding.blendingMenuLayout, true)
                    } catch (e: IllegalStateException) {
                        println(e.message)
                    }
                }
            } else {
                try {
                    val resultBitmap =
                        imageToolModule.drawDetectionResult(selectBitmap, faceResult, requireContext().resources.getColor(R.color.white))

                    // imageView 변환
                    withContext(Dispatchers.Main) {
                        binding.mainView.setImageBitmap(resultBitmap)
                    }
                } catch (e: IllegalStateException) {
                    println(e.message)
                }
            }
//            imageToolModule.showView(binding.progressBar, false)
            showProgressBar(false, null)

            imageToolModule.showView(binding.blendingMenuLayout, true)

        }
    }

    /**
     * 터치된 좌표가 객체 감지 결과 중 객체 위치 정보(bounding Box) 속에 포함되는지 알아낸 후,
     * 터치 좌표가 포함되는 boundingBox를 모아 리스트로 반환한다.
     *
     * @param touchPoint 터치된 좌표
     * @return 터치 좌표가 포함되는 boundingBox를 모아 리스트로 반환
     */
    suspend fun getBoundingBox(touchPoint: Point): ArrayList<ArrayList<Int>> = suspendCoroutine { box ->
        val boundingBox: ArrayList<ArrayList<Int>> = arrayListOf()

        val checkFinish = BooleanArray(bitmapList.size)
        for (i in 0 until bitmapList.size) {
            checkFinish[i] = false
//            boundingBox.add(arrayListOf(0,0,0,0,0,0,0,0,0))
        }

        CoroutineScope(Dispatchers.IO).launch {
            if (bitmapList.size == 0) {
//                imageToolModule.showView(binding.progressBar, false)
                showProgressBar(false, null)
                return@launch
            }

            val basicRect =
                faceDetectionModule.getClickPointBoundingBox(0, touchPoint)

            if (basicRect == null) {
                withContext(Dispatchers.Main) {
                    try {
                        Toast.makeText(
                            requireContext(),
                            "해당 좌표에 얼굴이 존재 하지 않습니다.",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    } catch (e: IllegalStateException) {
                        println(e.message)
                    }
                }
                // 메인 사진의 boundingBox에 인지된 얼굴이 없을 때
                // faceDetection하고 결과가 표시된 사진을 받아 imaveView에 띄우기
                setMainImageBoundingBox()

                checkFinish.fill(true) // 배열의 모든 요소를 true로 설정
            } else {

                selectFaceRect = Rect(basicRect[0], basicRect[1], basicRect[2], basicRect[3])

                // 메인 사진일 경우 나중에 다른 사진을 겹칠 위치 지정
                changeFaceStartX = basicRect[4]
                changeFaceStartY = basicRect[5]

                val arrayBounding = arrayListOf(
                    0,
                    basicRect[0], basicRect[1], basicRect[2], basicRect[3],
                    basicRect[4], basicRect[5], basicRect[6], basicRect[7]
                )
                boundingBox.add(arrayBounding)
                checkFinish[0] = true
                for (i in 1 until bitmapList.size) {
//                    CoroutineScope(Dispatchers.Default).launch {
                    // clickPoint와 사진을 비교하여 클릭된 좌표에 감지된 얼굴이 있는지 확인 후 해당 얼굴 boundingBox 받기
                    val rect =
                        faceDetectionModule.getClickPointBoundingBox( i, touchPoint)

                    if (rect != null) {
                        val arrayBounding = arrayListOf(
                            i,
                            rect[0], rect[1], rect[2], rect[3],
                            rect[4], rect[5], rect[6], rect[7]
                        )
                        boundingBox.add(arrayBounding)
                    }
                    checkFinish[i] = true
                }
//                }
            }
            while (!checkFinish.all { it }) {
            }
            box.resume(boundingBox)
        }
    }

    /**
     * 자를 위치정보에 맞게 이미지를 자르고 화면에 띄어준다.
     *
     * @param boundingBox 자를 위치 정보
     */
    private fun cropImgAndView(boundingBox: ArrayList<ArrayList<Int>>) {

        imageToolModule.showView(binding.faceBlendingMenuLayout, true)
        imageToolModule.showView(binding.blendingSaveBtn, false)
        imageToolModule.showView(binding.blendingCloseBtn, false)
        isSelected = true
        changeSelectedView(selectBitmap)

        // 감지된 모든 boundingBox 출력
        println("=======================================================")
        binding.candidateLayout.removeAllViews()
        cropBitmapList.clear()

        if (bitmapList.size == 0) {
//            imageToolModule.showView(binding.progressBar , false)
            showProgressBar(false, null)
            return
        }

        for (i in 0 until boundingBox.size) {
            println(i.toString() + " || " + boundingBox[i])

            // bounding rect 알아내기
            val rect = boundingBox[i]

            // bitmap를 자르기
            val cropImage = imageToolModule.cropBitmap(
                bitmapList[rect[0]], Rect(rect[1], rect[2], rect[3], rect[4])
            )

            try {
                // 넣고자 하는 layout 불러오기
                val candidateLayout = layoutInflater.inflate(R.layout.candidate_image_array, null)

                // 위 불러온 layout에서 변경을 할 view가져오기
                val cropImageView: ImageView =
                    candidateLayout.findViewById(R.id.cropImageView)

                // 자른 사진 이미지뷰에 붙이기
                cropImageView.setImageBitmap(cropImage)
                // crop 된 후보 이미지 클릭시 해당 이미지로 얼굴 변환 (face Blending)
                cropImageView.setOnClickListener {

                    mainSubView?.background = null
                    mainSubView?.setPadding(0)

                    newImage = imageToolModule.cropBitmap(
                        bitmapList[rect[0]],
                        //bitmapList[rect[0]].copy(Bitmap.Config.ARGB_8888, true),
                        Rect(rect[5], rect[6], rect[7], rect[8])
                    )
                    newImage = imageToolModule.circleCropBitmap(newImage!!)

                    // 크롭이미지 배열에 값 추가
                    cropBitmapList.add(newImage!!)

                    preSelectBitmap = imageToolModule.overlayBitmap(
                        selectBitmap,
                        newImage!!,
                        changeFaceStartX,
                        changeFaceStartY
                    )

//                    binding.mainView.setImageBitmap(preSelectBitmap)
                    if(preSelectBitmap != null)
                        changeSelectedView(preSelectBitmap!!)

                    mainSubView = cropImageView
                    mainSubView?.setBackgroundResource(R.drawable.chosen_image_border)
                    mainSubView?.setPadding(6)

                    imageToolModule.showView(binding.arrowBar, true)
                    infoLevel.value = InfoLevel.ArrowCheck
                }

                // main activity에 만들어둔 scrollbar 속 layout의 아이디를 통해 해당 layout에 넣기
                binding.candidateLayout.addView(candidateLayout)
            } catch (e: IllegalStateException) {
                println(e.message)
            }
        }
        showProgressBar(false, null)
        imageToolModule.showView(binding.arrowBar, false)
        infoLevel.value = InfoLevel.ChangeFaceSelect
    }

    /**
     * 잘라진 이미지를 방향과 강도에 따라 이동한다.
     *
     * @param angle 이동할 방향
     * @param strength 강도
     */
    private fun moveCropFace(angle: Int, strength: Int) {

        val newStrength = strength / 10

        val angleRadians = angle * (PI / 180.0)

        // x 좌표 계산
        val moveX = (newStrength * cos(angleRadians)).toInt()
        // y 좌표 계산
        val moveY = - (newStrength * sin(angleRadians)).toInt()

        if(infoLevel.value != InfoLevel.BasicLevelEnd)
            infoLevel.value = InfoLevel.BasicLevelEnd
        if (newImage != null) {

            changeFaceStartX += moveX
            changeFaceStartY += moveY

            println("!!!!!!!!!! change point (${changeFaceStartX}, ${changeFaceStartY})")
            if(changeFaceStartX < 0)
                changeFaceStartX = 0
            else if(changeFaceStartX > selectBitmap.width - newImage!!.width)
                changeFaceStartX = selectBitmap.width - newImage!!.width
            if(changeFaceStartY < 0)
                changeFaceStartY = 0
            else if(changeFaceStartY > selectBitmap.height - newImage!!.height)
                changeFaceStartY = selectBitmap.height - newImage!!.height

            println("==== change point (${changeFaceStartX}, ${changeFaceStartY})")
            preSelectBitmap = imageToolModule.overlayBitmap(
                selectBitmap,
                newImage!!,
                changeFaceStartX,
                changeFaceStartY
            )

            binding.mainView.setImageBitmap(preSelectBitmap)
        }
    }

    /**
     * 도움말을 알맞게 띄운다.
     */
    open fun infoTextView() {
        Log.d("infoTextView","infoTextView call")
        when (infoLevel.value) {
            InfoLevel.EditFaceSelect -> {
                binding.infoText.text = "변경을 원하는 얼굴을 누릅니다."
            }
            InfoLevel.ChangeFaceSelect -> {
                binding.infoText.text = "아래 사진을 보고\n마음에 드는 얼굴을 선택합니다."
            }
            InfoLevel.ArrowCheck -> {
                binding.infoText.text = "변경된 얼굴의 위치는\n조정 바를 통해 수정할 수 있습니다."
            }
            InfoLevel.BasicLevelEnd -> {
                binding.infoText.text = "버튼을 누르면 모든 사람이\n 잘나온 얼굴로 자동 변경됩니다."
            }
            else -> {}
        }
    }

    /**
     * 선택한 이미지임을 표시한다.
     *
     * @param bitmap 선택한 이미지
     */
    open fun changeSelectedView(bitmap: Bitmap) {
        if(selectFaceRect != null) {
            val newBitmap = imageToolModule.drawDetectionResult(bitmap, selectFaceRect!!.toRectF(), requireContext().resources.getColor(R.color.select_face))
            binding.mainView.setImageBitmap(newBitmap)
        }
    }

    /**
     * 로딩바를 설정한다.
     *
     * @param boolean 로딩바 보여줄지 여부
     * @param loadingText 로딩과 함께 보여질 텍스트
     */
    private fun showProgressBar(boolean: Boolean, loadingText: LoadingText?){
        setEnable(!boolean)

        if(boolean && isInfoViewed) {
            imageToolModule.showView(binding.infoDialogLayout, false)
        }
        else if (isInfoViewed) {
            imageToolModule.showView(binding.infoDialogLayout, true)
        }

        CoroutineScope(Dispatchers.Main).launch {
            binding.loadingText.text = when (loadingText) {
                LoadingText.Save -> {
                    "편집 저장 중.."
                }
                LoadingText.AutoBlending -> {
                    "최적의 블렌딩 사진 제작 중.."
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
     * 버튼들의 터치 가능 여부를 조정한다.
     *
     * @param boolean 터치 가능 여부
     */
    private fun setEnable(boolean: Boolean) {
        CoroutineScope(Dispatchers.Main).launch {
            binding.blendingCloseBtn.isEnabled = boolean
            binding.blendingSaveBtn.isEnabled = boolean

            binding.autoBlendingBtn.isEnabled = boolean
            binding.imageResetBtn.isEnabled = boolean
            binding.imageCompareBtn.isEnabled = boolean
            binding.blendingInfoBtn.isEnabled = boolean
//            binding.circleArrowBtn.isEnabled = boolean

            binding.dialogCloseBtn.isEnabled = boolean
            binding.faceCancleBtn.isEnabled = boolean
        }
    }


}
