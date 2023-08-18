package com.goldenratio.onepic.EditModule.Fragment

import android.annotation.SuppressLint
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.graphics.toRectF
import androidx.core.view.isEmpty
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.goldenratio.onepic.*
import com.goldenratio.onepic.EditModule.ArrowMoveClickListener
import com.goldenratio.onepic.EditModule.FaceDetectionModule
import com.goldenratio.onepic.AllinJPEGModule.Contents.ContentAttribute
import com.goldenratio.onepic.AllinJPEGModule.Contents.Picture
import com.goldenratio.onepic.AllinJPEGModule.ImageContent
import com.goldenratio.onepic.databinding.FragmentFaceBlendingBinding
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


open class FaceBlendingFragment : Fragment(R.layout.fragment_face_blending) {

    private lateinit var binding: FragmentFaceBlendingBinding

    protected lateinit var imageToolModule: ImageToolModule
    protected lateinit var faceDetectionModule: FaceDetectionModule

    protected lateinit var selectPicture: Picture

    private lateinit var originalSelectBitmap: Bitmap
    protected lateinit var selectBitmap: Bitmap
    private var PreSelectBitmap: Bitmap? = null
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

    // Animation
//    private lateinit var fadeIn: ObjectAnimator
//    private lateinit var fadeOut: ObjectAnimator

    private enum class InfoLevel {
        EditFaceSelect,
        ChangeFaceSelect,
        ArrowCheck,
        BasicLevelEnd
    }

    private enum class LoadingText {
        FaceDetection,
        Save,
        Change,
        AutoBlending
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        bundle: Bundle?
    ): View {
        // 상태바 색상 변경
        val window: Window = activity?.window
            ?: throw IllegalStateException("Fragment is not attached to an activity")
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.setStatusBarColor(ContextCompat.getColor(requireContext(), android.R.color.black))

        // 뷰 바인딩 설정
        binding = FragmentFaceBlendingBinding.inflate(inflater, container, false)

        imageContent = jpegViewModel.jpegAiContainer.value?.imageContent!!

        imageToolModule = ImageToolModule()
        faceDetectionModule = FaceDetectionModule()

//        imageToolModule.showView(binding.progressBar, true)
//        imageToolModule.showView(binding.loadingText, true)
//        showProgressBar(true, LoadingText.FaceDetection)
        showProgressBar(true, LoadingText.AutoBlending)
        imageToolModule.showView(binding.blendingMenuLayout, false)

        while(!imageContent.checkPictureList) {}

        // main Picture의 byteArray를 bitmap 제작
        selectPicture = jpegViewModel.selectedSubImage!!
        
        // 메인 이미지 임시 설정
        CoroutineScope(Dispatchers.Default).launch {
            withContext(Dispatchers.Main) {
                Glide.with(binding.mainView)
                    .load(imageContent.getJpegBytes(selectPicture))
                    .into(binding.mainView)
            }
        }

        CoroutineScope(Dispatchers.Default).launch {
            val allBitmapList = imageContent.getBitmapList()
            val index = jpegViewModel.getSelectedSubImageIndex()
            val newMainBitmap = allBitmapList?.get(index)
            if (newMainBitmap != null) {
                selectBitmap = newMainBitmap
            }
            originalSelectBitmap = selectBitmap

        }
        CoroutineScope(Dispatchers.IO).launch {
            // Blending 가능한 연속 사진 속성의 picture list 얻음
            Log.d("faceBlending", "newBitmapList call before")
            val newBitmapList = imageContent.getBitmapList(ContentAttribute.edited)
            Log.d("faceBlending", "newBitmapList $newBitmapList")
            if (newBitmapList != null) {
                bitmapList = newBitmapList

                faceDetectionModule.allFaceDetection(bitmapList)

                selectBitmap = faceDetectionModule.autoBestFaceChange(bitmapList, jpegViewModel.getSelectedSubImageIndex())
                
                // faceDetection 하고 결과가 표시된 사진을 받아 imaveView에 띄우기
                setMainImageBoundingBox()
                withContext(Dispatchers.Main) {
                    imageToolModule.fadeIn.start()
                }
            }
        }

        imageToolModule.settingAnimation(binding.successInfoConstraintLayout)

        SetClickEvent()

        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.post {
            binding.circleArrowBtn.setOnTouchListener(ArrowMoveClickListener(::moveCropFace, binding.maxArrowBtn, binding.circleArrowBtn))
            imageToolModule.showView(binding.arrowBar, false)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun SetClickEvent() {
        // save btn 클릭 시
        binding.blendingSaveBtn.setOnClickListener {

            CoroutineScope(Dispatchers.Main).launch {
                imageToolModule.showView(binding.infoDialogLayout, false)
                infoLevel.value = InfoLevel.EditFaceSelect
            }

            CoroutineScope(Dispatchers.Default).launch {

//                imageToolModule.showView(binding.progressBar, true)
//                imageToolModule.showView(binding.loadingText, true)
                showProgressBar(true, LoadingText.Save)

                if (PreSelectBitmap != null) {
                    selectBitmap = PreSelectBitmap!!
                    newImage = null
                }

                val allBytes = imageToolModule.bitmapToByteArray(
                    selectBitmap,
                    imageContent.getJpegBytes(selectPicture)
                )

                val selectIndex = jpegViewModel.getSelectedSubImageIndex()

                imageContent.pictureList.add(selectIndex,
                    Picture(ContentAttribute.edited, imageContent.extractSOI(allBytes)))
                imageContent.addBitmapList(selectIndex, selectBitmap)

                imageContent.pictureList[selectIndex].waitForByteArrayInitialized()

                jpegViewModel.setPictureByteList(imageContent.getJpegBytes(imageContent.pictureList[selectIndex]), selectIndex)

                jpegViewModel.selectedSubImage = imageContent.pictureList[selectIndex]

//                imageContent.setMainBitmap(selectBitmap)
                withContext(Dispatchers.Main) {
                    //jpegViewModel.jpegMCContainer.value?.save()
                    imageContent.checkBlending = true
                    findNavController().navigate(R.id.action_fregemnt_to_editFragment)
                }

//                imageToolModule.showView(binding.progressBar, false)
                showProgressBar(false, null)
            }
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
                        binding.mainView.setImageBitmap(PreSelectBitmap)
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
            PreSelectBitmap = null
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
        infoLevel.observe(viewLifecycleOwner) { _ ->
            infoTextView()
        }

        // 이미지 뷰 클릭 시
        binding.mainView.setOnTouchListener { _, event ->
            if (event!!.action == MotionEvent.ACTION_UP) {
                if (!isSelected) {
//                imageToolModule.showView(binding.progressBar, true)
                    showProgressBar(true, LoadingText.Change)
                    imageToolModule.showView(binding.blendingMenuLayout, false)

//                    if (PreSelectBitmap != null) {
//                        selectBitmap = PreSelectBitmap!!
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

            if (PreSelectBitmap != null) {
                selectBitmap = PreSelectBitmap!!
                newImage = null
                PreSelectBitmap = null
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
            PreSelectBitmap = null

            binding.mainView.setImageBitmap(selectBitmap)
            imageToolModule.showView(binding.faceBlendingMenuLayout, false)
            imageToolModule.showView(binding.blendingSaveBtn, true)
            imageToolModule.showView(binding.blendingCloseBtn, true)
            setMainImageBoundingBox()
        }
    }

    /**
     * setMainImageBoundingBox()
     *      - mainImage를 faceDetection 실행 후,
     *        감지된 얼굴의 사각형 표시된 사진으로 imageView 변환
     */
    open fun setMainImageBoundingBox() {

        if(infoLevel.value != InfoLevel.EditFaceSelect) {
            CoroutineScope(Dispatchers.Main).launch {
                infoLevel.value = InfoLevel.EditFaceSelect
                isInfoViewed = false

                imageToolModule.showView(binding.infoDialogLayout, false)
            }
        }

        //showView(binding.faceListView, false)
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
            val faceResult = faceDetectionModule.runFaceDetection(0)
//            val faceResult = FaceDetectionModule.runMainFaceDetection(selectBitmap)
            Log.d("checkPictureList", "!!!!!!!!!!!!!!!!!!! end runFaceDetection")

            if (faceResult.size == 0) {
                withContext(Dispatchers.Main) {
                    try {
                    Toast.makeText(requireContext(), "사진에 얼굴이 존재하지 않습니다.", Toast.LENGTH_SHORT)
                        .show()
//                        imageToolModule.showView(binding.progressBar, false)
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
     * getBoundingBox(touchPoint: Point): ArrayList<List<Int>>
     *     - click된 포인트를 알려주면,
     *       해당 포인트가 객체 감지 결과 bounding Box 속에 존재하는지 찾아서
     *       만약 포인트를 포함하는 boundingBox를 찾으면 모아 return
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
                faceDetectionModule.getClickPointBoundingBox(bitmapList[0], 0, touchPoint)

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
                            faceDetectionModule.getClickPointBoundingBox(bitmapList[i], i, touchPoint)

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
     *  cropImgAndView(boundingBox: ArrayList<List<Int>>)
     *         - 이미지를 자르고 화면에 띄어줌
     */
    private fun cropImgAndView(boundingBox: ArrayList<ArrayList<Int>>) {

        imageToolModule.showView(binding.faceBlendingMenuLayout, true)
        imageToolModule.showView(binding.blendingSaveBtn, false)
        imageToolModule.showView(binding.blendingCloseBtn, false)
        isSelected = true
        changeMainView(selectBitmap)

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
                bitmapList[rect[0]],
                //bitmapList[rect[0]].copy(Bitmap.Config.ARGB_8888, true),
                Rect(rect[1], rect[2], rect[3], rect[4])
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

                    PreSelectBitmap = imageToolModule.overlayBitmap(
                        selectBitmap,
                        newImage!!,
                        changeFaceStartX,
                        changeFaceStartY
                    )

//                    binding.mainView.setImageBitmap(PreSelectBitmap)
                    if(PreSelectBitmap != null)
                        changeMainView(PreSelectBitmap!!)

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
//        imageToolModule.showView(binding.progressBar , false)
        showProgressBar(false, null)
        imageToolModule.showView(binding.arrowBar, false)
        infoLevel.value = InfoLevel.ChangeFaceSelect
    }

    private fun moveCropFace(moveX:Int, moveY:Int) {
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
            PreSelectBitmap = imageToolModule.overlayBitmap(
                selectBitmap,
                newImage!!,
                changeFaceStartX,
                changeFaceStartY
            )

            binding.mainView.setImageBitmap(PreSelectBitmap)
        }
    }

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

    open fun changeMainView(bitmap: Bitmap) {
        if(selectFaceRect != null) {
            val newBitmap = imageToolModule.drawDetectionResult(bitmap, selectFaceRect!!.toRectF(), requireContext().resources.getColor(R.color.select_face))
            binding.mainView.setImageBitmap(newBitmap)
        }
    }

    private fun showProgressBar(boolean: Boolean, loadingText: LoadingText?){
        if(boolean && isInfoViewed) {
            imageToolModule.showView(binding.infoDialogLayout, false)
        }
        else if (isInfoViewed) {
            imageToolModule.showView(binding.infoDialogLayout, true)
        }

        CoroutineScope(Dispatchers.Main).launch {
            binding.loadingText.text = when (loadingText) {
//                LoadingText.FaceDetection -> {
//                    "자동 Face Blending 중"
//                }
                LoadingText.Save -> {
                    "편집 저장 중.."
                }
                LoadingText.AutoBlending -> {
                    "최적의 Blending 사진 제작 중.."
                }
                else -> {
                    ""
                }
            }
        }

        imageToolModule.showView(binding.progressBar, boolean)
        imageToolModule.showView(binding.loadingText, boolean)
    }


    override fun onDestroy() {
        super.onDestroy()
        faceDetectionModule.deleteModelCoroutine()
    }
    override fun onStop() {
        super.onStop()
        faceDetectionModule.deleteModelCoroutine()
    }


}
